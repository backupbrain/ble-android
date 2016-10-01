package tonyg.example.com.exampleblescan.views;

import android.bluetooth.BluetoothDevice;

/**
 * A visual representation of a Bluetooth Low Energy Device.
 * This is paired with a ble_list_item.xml that lets us list all the devices found by the BLECommManager
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-17
 */
public class BleDeviceListItem {
    private int mItemId;
    private String mName;
    private int mRssi;
    private BluetoothDevice mBluetoothDevice;


    public void setItemId(int id) {  mItemId = id; }
    public void setName(String name) {  mName = name; }
    public void setPowerLevel(int rssi) {
        mRssi = rssi;
    }
    public void setDevice(BluetoothDevice device) { mBluetoothDevice = device; }

    public int getItemId() { return mItemId; }
    public String getName() {
        String name = mName;
        if (mBluetoothDevice != null) {
            name = mBluetoothDevice.getName();
        }
        return name;
    }
    public String getAddress() {
        return mBluetoothDevice.getAddress();
    }
    public int getRssi() {
        return mRssi;
    }
}
