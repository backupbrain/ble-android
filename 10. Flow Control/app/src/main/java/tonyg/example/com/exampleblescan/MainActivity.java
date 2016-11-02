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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.CustomScanCallback;
import tonyg.example.com.exampleblescan.views.BleDeviceListItem;
import tonyg.example.com.exampleblescan.views.BleDevicesListAdapter;


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


    private List<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();

    /** UI Stuff **/
    private MenuItem mProgressSpinner;
    private MenuItem mStartScanItem, mStopScanItem;
    private ListView mDevicesList;
    private BleDevicesListAdapter mDevicesListAdapter;


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

    public void loadUI() {
        // load UI components, set up the Peripheral list
        mDevicesList = (ListView) findViewById(R.id.devices_list);
        mDevicesListAdapter = new BleDevicesListAdapter();
        mDevicesList.setAdapter(mDevicesListAdapter);

    }

    public void attachCallbacks() {
        // when a user clicks on a Peripheral in the list, open that Peripheral in the Connect Activity
        mDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mBluetoothDevices.size() > 0) {
                    Log.v(TAG, "List View click: position: " + position + ", id: " + id);
                    BleDeviceListItem listItem = mDevicesListAdapter.getItem(position);
                    BluetoothDevice device = mBluetoothDevices.get(listItem.getItemId());

                    // start the Connect Activity and connect to this Bluetooth Device
                    Intent intent = new Intent(getBaseContext(), ConnectActivity.class);
                    intent.putExtra(ConnectActivity.MAC_ADDRESS_KEY, device.getAddress());

                    Log.v(TAG, "Setting intent: " + ConnectActivity.MAC_ADDRESS_KEY + ": " + device.getAddress());
                    startActivity(intent);
                }

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mStartScanItem = menu.findItem(R.id.action_start_scan);
        mStopScanItem =  menu.findItem(R.id.action_stop_scan);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);

        mStartScanItem.setVisible(true);
        mStopScanItem.setVisible(false);
        mProgressSpinner.setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }
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


    public void initializeBluetooth() {
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

    public void startScan() {
        // update UI components
        mStartScanItem.setVisible(false);
        mStopScanItem.setVisible(true);
        mProgressSpinner.setVisible(true);

        // clear the list of Peripherals and start scanning
        mDevicesListAdapter.clear();
        try {
            mBleCommManager.scanForDevices(mScanCallback);
        } catch (Exception e) {
            Log.e(TAG, "Could not open Ble Device Scanner");
        }

    }


    public void stopScan() {
        mBleCommManager.stopScanning(mScanCallback);
    }

    public void onBleScanStopped() {
        // update UI compenents to reflect that a BLE scan has stopped
        mStopScanItem.setVisible(false);
        mProgressSpinner.setVisible(false);
        mStartScanItem.setVisible(true);
    }


    // FIXME: make separate class
    // aynctaskloader
    private final CustomScanCallback mScanCallback = new CustomScanCallback() {
        // when a Peripheral is found, process it
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.v(TAG, "Found "+device.getName()+", "+device.getAddress());
            // only add the device if
            // - it has a name, on
            // - doesn't already exist in our list, or
            // - is transmitting at a higher power (is closer) than an existing device
            boolean addDevice = true;
            if (device.getName() == null) {
                addDevice = false;
            }
            for(BleDeviceListItem listItem : mDevicesListAdapter.getItems()) {
                if ( listItem.getName().equals(device.getName()) ) {
                    addDevice = false;
                }
            }

            if (addDevice) {
                mDevicesListAdapter.addBluetoothDevice(device, rssi);
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
