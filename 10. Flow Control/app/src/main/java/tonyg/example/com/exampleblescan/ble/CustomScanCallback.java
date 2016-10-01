package tonyg.example.com.exampleblescan.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import java.util.List;

/**
 * This class exists so that we can add the onScanComplete() function to the existing ScanCallback functions
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-18
 */
public abstract class CustomScanCallback implements BluetoothAdapter.LeScanCallback {


    @Override
    public abstract void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord);

    /**
     * we have finished our scan
     */
    public abstract void onScanComplete();
}
