package tonyg.example.com.exampleblescan.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Timer;
import java.util.TimerTask;

import tonyg.example.com.exampleblescan.MainActivity;

/**
 * This class helps us manage Bluetooth Low Energy scanning functions.
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-12
 */
public class BleCommManager {
    private static final String TAG = BleCommManager.class.getSimpleName();
    private static final long SCAN_PERIOD = 5000; // 5 seconds of scanning time

    private BluetoothAdapter mBluetoothAdapter; // Andrdoid's Bluetooth Adapter


    private Timer mTimer = new Timer();

    public BleCommManager() {

    }

    public void initBluetooth(final Context context) throws Exception {
        // make sure Android device supports Bluetooth Low Energy
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("Bluetooth Not Supported");
        }

        // get a reference to the Bluetooth Manager class, which allows us to talk to talk to the BLE radio
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    public void scanForDevices(final CustomScanCallback scanCallback) throws Exception {
        // Throw an exception if Bluetooth is not supported by this Android Device
        if (mBluetoothAdapter == null) {
            throw new Exception("Bluetooth Not Supported");
        }

        // Don't proceed if there is already a scan in progress
        if(mTimer != null) {
            mTimer.cancel();
        }

        // Scan for SCAN_PERIOD milliseconds.
        // at the end of that time, stop the scan.
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(scanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(scanCallback);
            }
        }.start();
        // alert the sytem that BLE scanning has stopped after SCAN_PERIOD milliseconds
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopScanning(scanCallback);
            }
        }, SCAN_PERIOD);

    }


    public void stopScanning(final CustomScanCallback scanCallback) {
        // close the timer if necessary
        if(mTimer != null) {
            mTimer.cancel();
        }
        // propogate the onScanComplete through the system
        scanCallback.onScanComplete();

    }


}

