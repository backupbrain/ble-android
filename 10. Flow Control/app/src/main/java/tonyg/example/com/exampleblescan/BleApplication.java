package tonyg.example.com.exampleblescan;

import android.app.Application;

import tonyg.example.com.exampleblescan.ble.BleDevice;

/**
 * This class holds the Bluetooth device connections between activities
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-05
 */
public class BleApplication extends Application {

    private BleDevice mBleDevice; // FIXME: bad idea: should pass bledevice between activities.  could cause errors

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setBleDevice(BleDevice bleDevice) {
        mBleDevice = bleDevice;
    }
    public BleDevice getBleDevice() {
        return mBleDevice;
    }
}
