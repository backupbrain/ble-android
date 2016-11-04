package tonyg.example.com.exampleblescan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.CustomScanCallback;
import tonyg.example.com.exampleblescan.views.BlePeripheralListItem;
import tonyg.example.com.exampleblescan.views.BlePeripheralsListAdapter;


/**
 * Scan for and list BLE Peripherals
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class MainActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;

    /** Activity State **/
    private boolean mScanningActive = false;
    private boolean mConnectToPeripheral = false;
    private String mConnectToPeripheralMacAddress = null;


    /** UI Stuff **/
    private MenuItem mScanProgressSpinner;
    private MenuItem mStartScanItem, mStopScanItem;
    private ListView mBlePeripheralsListView;
    private TextView mPeripheralsListEmptyTV;
    private BlePeripheralsListAdapter mBlePeripheralsListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUI();
        attachCallbacks();

        // initialize the BleCommManager
        mBleCommManager = new BleCommManager();
    }


    @Override
    public void onResume() {
        super.onResume();
        initializeBluetooth();
    }

    @Override
    public void onPause() {
        super.onPause();
        // stop scanning when the activity pauses
        mBleCommManager.stopScanning(mScanCallback);
    }


    /**
     * Load UI components
     */
    public void loadUI() {
        // load UI components, set up the Peripheral list
        mPeripheralsListEmptyTV = (TextView) findViewById(R.id.peripheral_list_empty);
        mBlePeripheralsListView = (ListView) findViewById(R.id.peripherals_list);
        mBlePeripheralsListAdapter = new BlePeripheralsListAdapter();
        mBlePeripheralsListView.setAdapter(mBlePeripheralsListAdapter);
        mBlePeripheralsListView.setEmptyView(mPeripheralsListEmptyTV);
    }

    /**
     * Attach callback listeners to UI elements
     */
    public void attachCallbacks() {
        // when a user clicks on a Peripheral in the list, open that Peripheral in the Connect Activity
        mBlePeripheralsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v(TAG, "Adapter count: "+parent.getCount());
                Log.v(TAG, "Adapter child count: "+parent.getChildCount());
                Log.v(TAG, "Adapter click position: "+position);
                Log.v(TAG, "Adapter view: "+view.toString());
                Log.v(TAG, "Adapter ID: "+id);

                // only click through if the selected ListItem represents a Bluetooth Peripheral
                BlePeripheralListItem selectedPeripheralListItem = (BlePeripheralListItem) mBlePeripheralsListView.getItemAtPosition(position);
                if ((mBlePeripheralsListView.getCount() > 0) && (selectedPeripheralListItem.getDevice() != null)) {
                    Log.v(TAG, "List View click: position: " + position + ", id: " + id);
                    BlePeripheralListItem listItem = mBlePeripheralsListAdapter.getItem(position);
                    connectToPeripheral(listItem.getMacAddress());
                    stopScan();

                }
            }
        });
    }

    /**
     * Create a menu
     * @param menu The menu
     * @return <b>true</b> if processed successfully
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mStartScanItem = menu.findItem(R.id.action_start_scan);
        mStopScanItem =  menu.findItem(R.id.action_stop_scan);
        mScanProgressSpinner = menu.findItem(R.id.scan_progress_item);

        mStartScanItem.setVisible(true);
        mStopScanItem.setVisible(false);
        mScanProgressSpinner.setVisible(false);

        return true;
    }

    /**
     * Handle a menu item click
     *
     * @param item the Menuitem
     * @return <b>true</b> if processed successfully
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Start a BLE scan when a user clicks the "start scanning" menu button
        // and stop a BLE scan when a user clicks the "stop scanning" menu button
        switch (item.getItemId()) {
            case R.id.action_start_scan:
                // User chose the "Scan" item
                startScan();
                return true;

            case R.id.action_stop_scan:
                // User chose the "Stop" item
                stopScan();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Initialize the Bluetooth Radio
     */
    public void initializeBluetooth() {
        // reset connection variables
        mScanningActive = false;
        mConnectToPeripheral = false;
        mConnectToPeripheralMacAddress = null;

        try {
            mBleCommManager.initBluetooth(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }

        // should prompt user to open settings if Bluetooth is not enabled.
        if (!mBleCommManager.getBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    /**
     * Start scanning for Peripherals
     */
    public void startScan() {
        // update UI components
        mStartScanItem.setVisible(false);
        mStopScanItem.setVisible(true);
        mScanProgressSpinner.setVisible(true);

        // clear the list of Peripherals and start scanning
        mBlePeripheralsListAdapter.clear();
        try {
            mScanningActive = true;
            mBleCommManager.scanForPeripherals(mScanCallback);
        } catch (Exception e) {
            Log.e(TAG, "Could not open Ble Device Scanner");
        }

    }

    /**
     * Stop scanning for Peripherals
     */
    public void stopScan() {
        mBleCommManager.stopScanning(mScanCallback);
    }

    /**
     * Event trigger when BLE Scanning has stopped
     */
    public void onBleScanStopped() {
        // update UI compenents to reflect that a BLE scan has stopped
        mScanningActive = false;
        if (mStopScanItem != null) mStopScanItem.setVisible(false);
        if (mScanProgressSpinner != null) mScanProgressSpinner.setVisible(false);
        if (mStartScanItem != null) mStartScanItem.setVisible(true);

        if (mConnectToPeripheralMacAddress != null) {
            connectToPeripheral(mConnectToPeripheralMacAddress);
        }
    }

    /**
     * Hand the Peripheral Mac Address over to the Connect Activity
     *
     * @param peripheralMacAddress the MAC address of the selected Peripheral
     */
    public void connectToPeripheral(String peripheralMacAddress) {
        // in case the system isn't ready to stop scanning, store the connection information
        mConnectToPeripheral = true;
        mConnectToPeripheralMacAddress = peripheralMacAddress;

        if (mScanningActive == false) {
            // start the Connect Activity and connect to this Bluetooth Peripheral
            Intent intent = new Intent(getBaseContext(), ConnectActivity.class);
            intent.putExtra(ConnectActivity.PERIPHERAL_MAC_ADDRESS_KEY, mConnectToPeripheralMacAddress);

            Log.v(TAG, "Setting intent: " + ConnectActivity.PERIPHERAL_MAC_ADDRESS_KEY + ": " + mConnectToPeripheralMacAddress);
            startActivity(intent);
        }
    }

    // FIXME: consider make separate class, use an asynctastloader?
    private final CustomScanCallback mScanCallback = new CustomScanCallback() {
        // when a Peripheral is found, process it
        public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            Log.v(TAG, "Found "+bluetoothDevice.getName()+", "+bluetoothDevice.getAddress());
            // only add the peripheral if
            // - it has a name, on
            // - doesn't already exist in our list, or
            // - is transmitting at a higher power (is closer) than an existing peripheral
            boolean addPeripheral = true;
            if (bluetoothDevice.getName() == null) {
                addPeripheral = false;
            }
            for(BlePeripheralListItem listItem : mBlePeripheralsListAdapter.getItems()) {
                if ( listItem.getBroadcastName().equals(bluetoothDevice.getName()) ) {
                    addPeripheral = false;
                }
            }

            if (addPeripheral) {
                mBlePeripheralsListAdapter.addBluetoothPeripheral(bluetoothDevice, rssi);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBlePeripheralsListAdapter.notifyDataSetChanged();
                    }
                });
            }

        }

        // update UI components when done scanning - push onto main thread
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }
    };


}
