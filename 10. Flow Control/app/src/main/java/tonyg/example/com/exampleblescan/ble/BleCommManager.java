package tonyg.example.com.exampleblescan.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
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
    private static final long SCAN_PERIOD = 5000; //5 seconds

    private BluetoothAdapter mBluetoothAdapter; // Andrdoid's Bluetooth Adapter


    private Timer mTimer = new Timer();

    public BleCommManager() {

    }

    public void initBluetooth(final Context context) throws Exception {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("Bluetooth Not Supported");
        }


        //get a reference to the Bluetooth Manager
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    public void scanForDevices(final CustomScanCallback scanCallback) throws Exception {
        if (mBluetoothAdapter == null) {
            throw new Exception("Bluetooth Not Supported");
        }

        if(mTimer != null) {
            mTimer.cancel();
        }

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

        /*
        // FIXME: lighter timer method
        mScanHandler = new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning(scanCallback);
            }
        }, SCAN_PERIOD);
        */

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopScanning(scanCallback);
            }
        }, SCAN_PERIOD);

    }


    public void stopScanning(final CustomScanCallback scanCallback) {
        if(mTimer != null) {
            mTimer.cancel();
        }
        scanCallback.onScanComplete();

    }


}

