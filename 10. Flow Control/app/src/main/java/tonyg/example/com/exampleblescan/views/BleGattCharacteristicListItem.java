package tonyg.example.com.exampleblescan.views;


import android.bluetooth.BluetoothGattCharacteristic;

/**
 * A visual representation of the characteristics available in a BLE service
 * This is paired with a list_item_ble_characteristic.xml that lists the services found by the BleCommManager
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class BleGattCharacteristicListItem {
    private int mItemId;
    private BluetoothGattCharacteristic mCharacteristic;
    // here we handle sub-lists?

    public BleGattCharacteristicListItem(BluetoothGattCharacteristic characteristic, int itemId) {
        mCharacteristic = characteristic;
        mItemId = itemId;
    }

    public BluetoothGattCharacteristic getCharacteristic() { return mCharacteristic; }
}
