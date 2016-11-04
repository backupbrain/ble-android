package tonyg.example.com.exampleblescan;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.BlePeripheral;
import tonyg.example.com.exampleblescan.views.BleGattProfileListAdapter;

/**
 * Connect to a BLE Peripherals, list its GATT services
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class ConnectActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = ConnectActivity.class.getSimpleName();
    public static final String PERIPHERAL_MAC_ADDRESS_KEY = "peripheralMacAddress";

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;
    private BlePeripheral mBlePeripheral;

    /** Functional stuff **/
    private String mPeripheralMacAddress;

    /** Activity State **/
    private boolean bleDisconnected = false;
    private boolean leaveActivity = false;

    /** UI Stuff **/
    private MenuItem mProgressSpinner;
    private MenuItem mConnectItem, mDisconnectItem;
    private ExpandableListView mGattProfileListView;
    private TextView mPeripheralBroadcastNameTV, mPeripheralAddressTV, mGattProfileListEmptyTV;
    private BleGattProfileListAdapter mGattProfileListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab information passed to the savedInstanceState,
        // from when the user clicked on the list in MainActivty
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mPeripheralMacAddress = extras.getString(PERIPHERAL_MAC_ADDRESS_KEY);
            }
        } else {
            mPeripheralMacAddress = savedInstanceState.getString(PERIPHERAL_MAC_ADDRESS_KEY);
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUI();
        attachCallbacks();

        mBleCommManager = new BleCommManager();
        mBlePeripheral = new BlePeripheral();

    }

    @Override
    public void onResume() {
        super.onResume();
    }



    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onDestroy() {
        mGattCallback = null;
        super.onDestroy();
    }


    /**
     * Prepare the UI elements
     */
    public void loadUI() {
        mPeripheralBroadcastNameTV = (TextView)findViewById(R.id.name);
        mPeripheralAddressTV = (TextView)findViewById(R.id.address);
        mGattProfileListEmptyTV = (TextView)findViewById(R.id.gatt_profile_list_empty);

        mGattProfileListView = (ExpandableListView) findViewById(R.id.peripherals_list);
        mGattProfileListAdapter = new BleGattProfileListAdapter();


        mGattProfileListView.setAdapter(mGattProfileListAdapter);
        mGattProfileListView.setEmptyView(mGattProfileListEmptyTV);
    }

    /**
     * Attach callback handlers to UI elements
     */
    public void attachCallbacks() {
        // When a user clicks on a GATT Service, drop down the GATT Characteristics belonging
        // to thet Service.
        // When a user clicks on a GATT Characteristic, open in TalkActivity
        mGattProfileListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                Log.v(TAG, "List View click: groupPosition: " + groupPosition + ", childPosition: " + childPosition);

                BluetoothGattService service = mGattProfileListAdapter.getGroup(groupPosition).getService();
                BluetoothGattCharacteristic characteristic = mGattProfileListAdapter.getChild(groupPosition, childPosition).getCharacteristic();

                // start the Connect Activity and connect to this Bluetooth Peripheral
                Intent intent = new Intent(getBaseContext(), TalkActivity.class);
                intent.putExtra(TalkActivity.PERIPHERAL_MAC_ADDRESS_KEY, mPeripheralMacAddress);
                intent.putExtra(TalkActivity.CHARACTERISTIC_KEY, characteristic.getUuid().toString());
                intent.putExtra(TalkActivity.SERVICE_KEY, service.getUuid().toString());

                Log.v(TAG, "Setting intent: " + TalkActivity.CHARACTERISTIC_KEY + ": " + characteristic.getUuid().toString());
                startActivity(intent);

                return false;
            }
        });
    }

    /**
     * Create the menu
     *
     * @param menu
     * @return <b>true</b> if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);

        mConnectItem = menu.findItem(R.id.action_connect);
        mDisconnectItem =  menu.findItem(R.id.action_disconnect);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);

        mConnectItem.setVisible(true);
        mDisconnectItem.setVisible(false);
        mProgressSpinner.setVisible(true);



        initializeBluetooth();
        connect();

        return true;
    }


    /**
     * User clicked a menu button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                // User chose the "Scan" item
                connect();
                return true;

            case R.id.action_disconnect:
                // User chose the "Stop" item
                leaveActivity = true;
                quitActivity();
                return true;

            default:
                // The user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    /**
     * Turn on Bluetooth radio
     */
    public void initializeBluetooth() {
        try {
            mBleCommManager.initBluetooth(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
    }


    /**
     * Connect to Peripheral
     */
    public void connect() {
        // grab the Peripheral Device address and attempt to connect
        BluetoothDevice bluetoothDevice = mBleCommManager.getBluetoothAdapter().getRemoteDevice(mPeripheralMacAddress);
        mProgressSpinner.setVisible(true);
        try {
            mBlePeripheral.connect(bluetoothDevice, mGattCallback, getApplicationContext());
        } catch (Exception e) {
            mProgressSpinner.setVisible(false);
            Log.e(TAG, "Error connecting to peripheral");
        }
    }

    /**
     * Disconnect from Peripheral
     */
    public void disconnect() {
        // disconnect from the Peripheral.
        mProgressSpinner.setVisible(true);
        mBlePeripheral.disconnect();
    }

    /**
     * Bluetooth Peripheral connected.  Update UI
     */
    public void onBleConnected() {
        // update UI to reflect a connection
        BluetoothDevice bluetoothDevice = mBlePeripheral.getBluetoothDevice();
        mPeripheralBroadcastNameTV.setText(bluetoothDevice.getName());
        mPeripheralAddressTV.setText(bluetoothDevice.getAddress());
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
        mProgressSpinner.setVisible(false);

    }

    /**
     * Bluetooth Peripheral Disconnected.  Update UI
     */
    public void onBleDisconnected() {
        // update UI to reflect a disconnection
        mPeripheralBroadcastNameTV.setText("");
        mPeripheralAddressTV.setText("");
        mProgressSpinner.setVisible(false);
        mConnectItem.setVisible(true);
        mDisconnectItem.setVisible(false);
        bleDisconnected = true;
    }

    /**
     * Bluetooth Peripheral GATT Profile being scanned.  Update UI
     */
    public void onBleServiceDiscoveryStarted() {
        mProgressSpinner.setVisible(true);
    }

    /**
     * Bluetooth Peripheral GATT Profile discovered.  Update UI
     */
    public void onBleServiceDiscoveryStopped() {
        // update UI to reflect the GATT profile of the connected Perihperal
        mProgressSpinner.setVisible(false);
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
    }

    /**
     *  Quit the activity if the Peripheral is disconnected.  Otherwise disconnect and try again
     */
    public void quitActivity() {
        if (bleDisconnected == true) {
            finish();
        } else {
            disconnect();
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * Characteristic value changed
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // We don't care about this here as we aren't communicating with Characteristics
        }

        /**
         * Peripheral connected or disconnected
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // There has been a connection or a disconnection with a Peripheral.
            // If this is a connection, update the UI to reflect the change
            // and discover the GATT profile of the connected Peripheral
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to peripheral");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleConnected();
                        onBleServiceDiscoveryStarted();
                    }
                });

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBlePeripheral.close();
                bleDisconnected = true;
                if (leaveActivity) quitActivity();
            }
        }

        /**
         * Gatt Profile discovered
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {

            // if services were discovered, then let's iterate through them and display them on screen
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = bluetoothGatt.getServices();
                for (BluetoothGattService service : services) {
                    if (service != null) {
                        Log.v(TAG, "Service uuid: " + service.getUuid());

                        // add the gatt service to our list
                        mGattProfileListAdapter.addService(service);
                        // update the UI to reflect the new Service
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mGattProfileListAdapter.notifyDataSetChanged();
                            }
                        });

                        // while we are here, let's ask for this service's characteristics:
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            if (characteristic != null) {
                                // if there are Characteristics, add them to the Service's list
                                try {
                                    mGattProfileListAdapter.addCharacteristic(service, characteristic);
                                    // update the ListView UI
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mGattProfileListAdapter.notifyDataSetChanged();
                                        }
                                    });

                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }

                            }
                        }
                    }


                }
                disconnect(); // disconnect from the Peripheral so that a connection is possible again in TalkActivity
            } else {
                Log.e(TAG, "Something went wrong while discovering GATT services from this peripheral");
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleServiceDiscoveryStopped();
                }
            });

        }
    };



}
