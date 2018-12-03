package amiin.bazouk.application.com.p2pproject;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnOff, btnDiscover, btnSend;
    ListView listView;
    TextView readMsgBox, connectionStatus;
    EditText writeMsg;

    private WifiManager wifiManager;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private String[] deviceNameArray;
    private WifiP2pDevice[] deviceArray;

    private static final int READ_MESSAGE = 1;

    ClientThread clientThread;
    ServerThread serverThread;
    SendReceive sendReceive;

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peers.size()];
                deviceArray = new WifiP2pDevice[peers.size()];

                int index = 0;

                for(WifiP2pDevice device: peers){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            if(peers.size()==0){
                Toast.makeText(getApplicationContext(),"No device Found",Toast.LENGTH_LONG).show();
            }
        }
    };

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText("Host");
                serverThread = new ServerThread();
                serverThread.start();
            }
            else if(wifiP2pInfo.groupFormed){
                connectionStatus.setText("Client");
                clientThread = new ClientThread(groupOwnerAddress);
            }
        }
    };

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch(message.what){
                case READ_MESSAGE:
                    byte[] readBuffer = (byte[]) message.obj;
                    String tempMess = new String(readBuffer,0,message.arg1);
                    readMsgBox.setText(tempMess);
                    break;
            }
            return true;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }

    private void exqListener() {
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOff.setText("ON");
                }
                else{
                    wifiManager.setWifiEnabled(true);
                    btnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Starting failed");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connected to "+device.deviceName,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = writeMsg.getText().toString();
                sendReceive.write(msg.getBytes());
            }
        });
    }

    private void initialWork() {
        btnOff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        btnSend = findViewById(R.id.sendButton);
        listView = findViewById(R.id.peerListView);
        readMsgBox = findViewById(R.id.readMsg);
        connectionStatus = findViewById(R.id.connectionStatus);
        writeMsg = findViewById(R.id.writeMsg);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public WifiP2pManager.PeerListListener getPeerListListener() {
        return peerListListener;
    }

    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListener() {
        return connectionInfoListener;
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerThread extends Thread{
        ServerSocket serverSocket;
        Socket socket;

        @Override
        public void run(){
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientThread extends Thread{
        Socket socket;
        String host;

        public ClientThread(InetAddress hostAddress){
            host = hostAddress.getHostAddress();
            socket = new Socket();
            sendReceive = new SendReceive(socket);
            sendReceive.start();
        }

        @Override
        public void run(){
            try {
                socket.connect(new InetSocketAddress(host,8888),500);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{
        private  Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket =skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while(socket!=null){
                try {
                    bytes = inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(READ_MESSAGE,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
