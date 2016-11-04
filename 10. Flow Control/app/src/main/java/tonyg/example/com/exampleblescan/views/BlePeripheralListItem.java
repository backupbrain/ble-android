package tonyg.example.com.exampleblescan.views;

import android.bluetooth.BluetoothDevice;

/**
 * A visual representation of a Bluetooth Low Energy Device.
 * This is paired with a ble_list_item.xml that lets us list all the devices found by the BleCommManager
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-17
 */
public class BlePeripheralListItem {
    private int mItemId;
    private String mBroadcastName;
    private int mRssi;
    private BluetoothDevice mBluetoothDevice;


    public void setItemId(int id) {  mItemId = id; }
    public void setBroadcastName(String broadcastName) {  mBroadcastName = broadcastName; }
    public void setRssi(int rssi) {
        mRssi = rssi;
    }
    public void setDevice(BluetoothDevice bluetoothDevice) { mBluetoothDevice = bluetoothDevice; }

    public int getItemId() { return mItemId; }
    public String getBroadcastName() {
        String name = mBroadcastName;
        if (mBluetoothDevice != null) {
            name = mBluetoothDevice.getName();
        }
        return name;
    }
    public String getMacAddress() {
        return mBluetoothDevice.getAddress();
    }
    public int getRssi() {
        return mRssi;
    }
    public BluetoothDevice getDevice() { return mBluetoothDevice; }
}
