package amiin.bazouk.application.com.p2pproject;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btnServerStart,btnRmvGroup,btnClientStart,btnRmvClient;
    TextView connectionStatus;

    private WifiManager mWifiManager;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDnsSdServiceInfo service;

    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private boolean serviceAlreadyCreated;
    private String ssid;
    private String password;

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText("Host");
                mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup group) {
                                Map<String, String> record = new HashMap<>();
                                record.put("ssid", group.getNetworkName());
                                record.put("password", encrypt(group.getPassphrase()));

                                WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                                        "SERVICE_INSTANCE", "SERVICE_TYPE", record);
                                MainActivity.this.service = service;
                                mManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        serviceAlreadyCreated = true;
                                        btnServerStart.setEnabled(false);
                                        btnRmvGroup.setEnabled(true);
                                    }

                                    @Override
                                    public void onFailure(int error) {
                                        deletePersistentGroups();
                                    }
                                });
                            }
                        });
            }
        }
    };

    private String encrypt(String passphrase) {
        return passphrase+"a";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }

    private void exqListener() {

        btnClientStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWifiManager.setWifiEnabled(true);
                final WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
                mManager.setDnsSdResponseListeners(mChannel,
                        new WifiP2pManager.DnsSdServiceResponseListener() {
                            @Override
                            public void onDnsSdServiceAvailable(String instanceName,String registrationType, WifiP2pDevice srcDevice) {
                                if (instanceName.equalsIgnoreCase("SERVICE_INSTANCE")) {
                                    connect(ssid, decrypt(password));
                                    btnClientStart.setEnabled(false);
                                    btnRmvClient.setEnabled(true);
                                } else {
                                    System.out.println("I AM NOT HERE");
                                }
                            }
                        }, new WifiP2pManager.DnsSdTxtRecordListener() {
                            @Override
                            public void onDnsSdTxtRecordAvailable(
                                    String fullDomainName, Map<String, String> record,
                                    WifiP2pDevice device) {
                                ssid = record.get("ssid");
                                password = record.get("password");
                                mManager.removeServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {

                                    }

                                    @Override
                                    public void onFailure(int i) {

                                    }
                                });
                            }
                        });
                mManager.addServiceRequest(mChannel, serviceRequest,
                        new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                int a = 0;
                            }

                            @Override
                            public void onFailure(int arg0) {
                                int a = 0;
                            }
                        });
                mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        int a = 0;
                    }

                    @Override
                    public void onFailure(int arg0) {
                        int a = 0;
                    }
                });
            }
        });

        btnRmvClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWifiManager.setWifiEnabled(false);
                btnClientStart.setEnabled(true);
                btnRmvClient.setEnabled(false);
            }
        });

        btnRmvGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletePersistentGroups();
            }
        });

        btnServerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager == null || !(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED) &&
                        !(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)) {
                    Toast.makeText(getApplicationContext(),"Not connected to internet!!",Toast.LENGTH_LONG).show();
                    return;
                }
                mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Group created",Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Group not created",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private String decrypt(String password) {
        return password.substring(0,password.length()-1);
    }

    private void initialWork() {
        btnClientStart = findViewById(R.id.client_start);
        btnRmvClient = findViewById(R.id.remove_client);
        connectionStatus = findViewById(R.id.connectionStatus);
        btnRmvGroup = findViewById(R.id.remove_group);
        btnServerStart = findViewById(R.id.server_start);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
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

    private void connect(String ssid, String password) {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        if (mWifiManager != null) {
            mWifiManager.setWifiEnabled(true);
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = String.format("\"%s\"", ssid);

            conf.status = WifiConfiguration.Status.ENABLED;
            conf.priority = 40;
            String capabilities = "WPA";

            if (capabilities.equals("WEP")) {
                Log.v("rht", "Configuring WEP");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                if (password.matches("^[0-9a-fA-F]+$")) {
                    conf.wepKeys[0] = password;
                } else {
                    conf.wepKeys[0] = "\"".concat(password).concat("\"");
                }

                conf.wepTxKeyIndex = 0;

            } else if (capabilities.contains("WPA")) {
                Log.v("rht", "Configuring WPA");

                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                conf.preSharedKey = "\"" + password + "\"";

            } else {
                Log.v("rht", "Configuring OPEN network");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.clear();
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }
            int netId = mWifiManager.addNetwork(conf);
            if (netId == -1) {
                netId = getExistingNetworkId(conf.SSID, mWifiManager);
            }

            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId, true);
            mWifiManager.reconnect();
        }

    }

    private int getExistingNetworkId(String ssid, WifiManager mWifiManager) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID.equals(ssid)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }

    private void deletePersistentGroups() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Group Disconnected",Toast.LENGTH_LONG).show();
                serviceAlreadyCreated = false;
                btnServerStart.setEnabled(true);
                btnRmvGroup.setEnabled(false);
                connectionStatus.setText("");
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Group Not Disconnected",Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(),"Group Disconnected",Toast.LENGTH_LONG).show();
                serviceAlreadyCreated = false;
                btnServerStart.setEnabled(true);
                btnRmvGroup.setEnabled(false);
                connectionStatus.setText("");
            }
        });
        if(service!=null)
        {
            mManager.removeLocalService(mChannel,service, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int i) {

                }
            });
        }
    }

    public boolean getServiceAlreadyCreated() {
        return serviceAlreadyCreated;
    }

    public void setServiceAlreadyCreated(boolean serviceAlreadyCreated) {
        this.serviceAlreadyCreated = serviceAlreadyCreated;
    }
}
