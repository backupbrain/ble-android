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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.BleDevice;
import tonyg.example.com.exampleblescan.views.BleGattProfileListAdapter;

/**
 * Connect to a BLE Device, list its GATT services
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class ConnectActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = ConnectActivity.class.getSimpleName();
    public static final String MAC_ADDRESS_KEY = "bluetoothMacAddress"; // FIXME: give a good name

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;
    private BleDevice mBleDevice;
    private ArrayList<BluetoothGattService> mGattServices = new ArrayList<BluetoothGattService>();
    private LinkedHashMap<Integer, ArrayList<BluetoothGattCharacteristic>> mGattCharactaristics = new LinkedHashMap<Integer, ArrayList<BluetoothGattCharacteristic>>();

    /** Functional stuff **/
    private String mDeviceAddress;

    /** UI Stuff **/
    private MenuItem mProgressSpinner;
    private MenuItem mConnectItem, mDisconnectItem;
    private ExpandableListView mDevicesList;
    private TextView mDeviceNameTV,mDeviceAddressTV;
    private BleGattProfileListAdapter mGattProfileListAdapter;
    //private final ArrayList<BleGattServiceListItem> mServicesListItems = new ArrayList<BleGattServiceListItem>(); // FIXME: move to BleSerivceListAdapter
    //private LinkedHashMap<Integer, ArrayList<BleGattCharacteristicListItem>> mCharacteristicListItems = new LinkedHashMap<Integer, ArrayList<BleGattCharacteristicListItem>>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab a device from the savedInstanceState
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mDeviceAddress = extras.getString(MAC_ADDRESS_KEY);
            }
        } else {
            mDeviceAddress = savedInstanceState.getString(MAC_ADDRESS_KEY);
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUI();
        attachCallbacks();

        mBleCommManager = new BleCommManager();
        mBleDevice = new BleDevice();

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

    public void loadUI() {
        mDeviceNameTV = (TextView)findViewById(R.id.name);
        mDeviceAddressTV = (TextView)findViewById(R.id.address);

        mDevicesList = (ExpandableListView) findViewById(R.id.devices_list);
        //mGattProfileListAdapter = new BleGattProfileListAdapter(mServicesListItems, mCharacteristicListItems);
        mGattProfileListAdapter = new BleGattProfileListAdapter();


        mDevicesList.setAdapter(mGattProfileListAdapter);
    }
    public void attachCallbacks() {
        mDevicesList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.v(TAG, "List View click: groupPosition: " + groupPosition + ", childPosition: " + childPosition);

                BluetoothGattService service = mGattServices.get(groupPosition);
                BluetoothGattCharacteristic characteristic = mGattCharactaristics.get(groupPosition).get(childPosition);

                // start the Connect Activity and connect to this Bluetooth Device
                Intent intent = new Intent(getBaseContext(), TalkActivity.class);
                intent.putExtra(TalkActivity.MAC_ADDRESS_KEY, mDeviceAddress);
                intent.putExtra(TalkActivity.CHARACTERISTIC_KEY, characteristic.getUuid().toString());
                intent.putExtra(TalkActivity.SERVICE_KEY, service.getUuid().toString());

                Log.v(TAG, "Setting intent: " + TalkActivity.CHARACTERISTIC_KEY + ": " + characteristic.getUuid().toString());
                startActivity(intent);

                return false;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mConnectItem = menu.findItem(R.id.action_connect);
        mDisconnectItem =  menu.findItem(R.id.action_disconnect);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);

        mConnectItem.setVisible(true);
        mDisconnectItem.setVisible(false);
        mProgressSpinner.setVisible(true);



        initializeBluetooth();
        connect();

        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                // User chose the "Scan" item
                connect();
                return true;

            case R.id.action_disconnect:
                // User chose the "Stop" item
                disconnect();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    public void initializeBluetooth() {
        try {
            mBleCommManager.initBluetooth(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
    }


    public void connect() {
        BluetoothDevice bluetoothDevice = mBleCommManager.getBluetoothAdapter().getRemoteDevice(mDeviceAddress);
        mProgressSpinner.setVisible(true);
        try {
            mBleDevice.connect(bluetoothDevice, mGattCallback, getApplicationContext());
        } catch (Exception e) {
            mProgressSpinner.setVisible(false);
            Log.e(TAG, "Error connecting to device");
        }
    }
    public void disconnect() {
        mProgressSpinner.setVisible(true);
        mBleDevice.disconnect();
        finish();
    }

    public void onBleConnected() {
        BluetoothDevice bluetoothDevice = mBleDevice.getBluetoothDevice();
        mDeviceNameTV.setText(bluetoothDevice.getName());
        mDeviceAddressTV.setText(bluetoothDevice.getAddress());
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
        mProgressSpinner.setVisible(false);

    }
    public void onBleDisconnected() {
        mDeviceNameTV.setText("");
        mDeviceAddressTV.setText("");
        mProgressSpinner.setVisible(false);
        mConnectItem.setVisible(true);
        mDisconnectItem.setVisible(false);
    }
    public void onBleServiceDiscovered() {
        mProgressSpinner.setVisible(false);
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
        mGattProfileListAdapter.notifyDataSetChanged();
    }

    private static class Blah extends BluetoothGattCallback {

    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to device");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleConnected();
                    }
                });

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(TAG, "Disconnected from device");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleDisconnected();
                    }
                });

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            // if services were discovered, then let's iterate through them and display them on screen
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = gatt.getServices();
                for (BluetoothGattService gattService : gattServices) {
                    if (gattService != null) {
                        Log.v(TAG, "Service uuid: " + gattService.getUuid());
                        // add the gatt service to our list
                        //mGattServices.add(gattService);
                        //int serviceItemID = mGattServices.size() - 1;
                        //mGattCharactaristics.put(serviceItemID, new ArrayList<BluetoothGattCharacteristic>());

                        mGattProfileListAdapter.addService(gattService);

                        // build a new list item
                        //BleGattServiceListItem serviceListItem = new BleGattServiceListItem(gattService, serviceItemID);

                        //mServicesListItems.add(item);
                        //mCharacteristicListItems.put(serviceItemID, new ArrayList<BleGattCharacteristicListItem>());

                        // while we are here, let's ask for this service's characteristics:
                        List<BluetoothGattCharacteristic> characteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            if (characteristic != null) {
                                /*
                                mGattCharactaristics.get(serviceItemID).add(characteristic);

                                BleGattCharacteristicListItem characteristicListItem = new BleGattCharacteristicListItem();
                                characteristicListItem.setCharacteristic(characteristic);
                                characteristicListItem.setItemId(mGattCharactaristics.get(serviceItemID).size() - 1);

                                mCharacteristicListItems.get(serviceItemID).add(characteristicListItem);
                                */
                                try {
                                    mGattProfileListAdapter.addCharacteristic(gattService, characteristic);
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }

                            }
                        }
                    }


                }
            } else {
                Log.e(TAG, "Something went wrong while discovering GATT services from this device");
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleServiceDiscovered();
                }
            });

        }
    };



}
