package amiin.bazouk.application.com.p2pproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ToyVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = ToyVpnService.class.getSimpleName();

    public static final String ACTION_CONNECT = "com.example.android.toyvpn.START";
    public static final String ACTION_DISCONNECT = "com.example.android.toyvpn.STOP";

    private Handler mHandler;

    private static class Connection extends Pair<Thread, ParcelFileDescriptor> {
        public Connection(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }

    private final AtomicReference<Thread> mConnectingThread = new AtomicReference<>();
    private final AtomicReference<Connection> mConnection = new AtomicReference<>();

    private AtomicInteger mNextConnectionId = new AtomicInteger(1);

    private PendingIntent mConfigureIntent;

    @Override
    public void onCreate() {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Create the intent to "configure" the connection (just start ToyVpnClient).
        mConfigureIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            connect();
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        disconnect();
    }

    @Override
    public boolean handleMessage(Message message) {
        Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        if (message.what != R.string.disconnected) {
            updateForegroundNotification(message.what);
        }
        return true;
    }

    private void connect() {
        // Become a foreground service. Background services can be VPN services too, but they can
        // be killed by background check before getting a chance to receive onRevoke().
        updateForegroundNotification(R.string.connecting);
        mHandler.sendEmptyMessage(R.string.connecting);

        // Extract information from the shared preferences.
        final String server = "192.168.49.1";
        final int port;
        port = 8282;

        // Kick off a connection.
        startConnection(new ToyVpnConnection(
                this, mNextConnectionId.getAndIncrement(), server, port));
    }

    private void startConnection(final ToyVpnConnection connection) {
        // Replace any existing connecting thread with the  new one.
        final Thread thread = new Thread(connection, "ToyVpnThread");
        setConnectingThread(thread);

        // Handler to mark as connected once onEstablish is called.
        connection.setConfigureIntent(mConfigureIntent);
        connection.setOnEstablishListener(new ToyVpnConnection.OnEstablishListener() {
            public void onEstablish(ParcelFileDescriptor tunInterface) {
                mHandler.sendEmptyMessage(R.string.connected);

                mConnectingThread.compareAndSet(thread, null);
                setConnection(new Connection(thread, tunInterface));
            }
        });
        thread.start();
    }

    private void setConnectingThread(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet(thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setConnection(final Connection connection) {
        final Connection oldConnection = mConnection.getAndSet(connection);
        if (oldConnection != null) {
            try {
                oldConnection.first.interrupt();
                oldConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing VPN interface", e);
            }
        }
    }

    private void disconnect() {
        mHandler.sendEmptyMessage(R.string.disconnected);
        setConnectingThread(null);
        setConnection(null);
        stopForeground(true);
    }

    private void updateForegroundNotification(final int message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = createNotificationChannel("default_channel_id","default_channel_name");
            startForeground(1, new Notification.Builder(this,channelId)
                    .setContentText(getString(message))
                    .setContentIntent(mConfigureIntent)
                    .build());
        }
        else {
            startForeground(1, new Notification.Builder(this)
                    .setContentText(getString(message))
                    .setContentIntent(mConfigureIntent)
                    .build());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel chan = null;
        chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);

        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;

    }
}