package tonyg.example.com.exampleblescan.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

/**
 * This class represents a generic Bluetooth Peripheral
 * and allows us to share Bluetooth resources
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class BlePeripheral {
    private static final String TAG = BlePeripheral.class.getSimpleName();

    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    /** Flow control stuff **/
    private int mNumPacketsTotal;
    private int mNumPacketsSent;
    private int mCharacteristicLength = 20;
    private String mQueuedCharactersticValue;
    public static final String FLOW_CONROL_VALUE = "ready";

    public BlePeripheral() {
    }

    /**
     * Connect to a Peripheral
     *
     * @param bluetoothDevice the Bluetooth Device
     * @param callback The connection callback
     * @param context The Activity that initialized the connection
     * @return a connection to the BluetoothGatt
     * @throws Exception if no device is given
     */
    public BluetoothGatt connect(BluetoothDevice bluetoothDevice, BluetoothGattCallback callback, final Context context) throws Exception {
        if (bluetoothDevice == null) {
            throw new Exception("No bluetooth device provided");
        }
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = bluetoothDevice.connectGatt(context, false, callback);
        refreshDeviceCache();
        return mBluetoothGatt;
    }

    /**
     * Disconnect from a Peripheral
     */
    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * A connection can only close after a successful disconnect.
     * Be sure to use the BluetoothGattCallback.onConnectionStateChanged event
     * to notify of a successful disconnect
     */
    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }
    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }


    // Android caches BLE Peripheral GATT Profiles.  This is ok when the Peripheral GATT Profile is
    // fixed, but since we are developing the Peripheral along-side the Central, we need to clear
    // the cache so that we don't see old GATT Profiles
    // http://stackoverflow.com/a/22709467
    public boolean refreshDeviceCache() throws Exception {
        Method localMethod = mBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
        if (localMethod != null) {
            boolean bool = ((Boolean) localMethod.invoke(mBluetoothGatt, new Object[0])).booleanValue();
            return bool;
        }

        return false;
    }

    /**
     * Request a data/value read from a Ble Characteristic
     *
     * @param characteristic
     */
    public void readValueFromCharacteristic(final BluetoothGattCharacteristic characteristic) {
        // Reading a characteristic requires both requesting the read and handling the callback that is
        // sent when the read is successful
        // http://stackoverflow.com/a/20020279
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Write a value to the Characteristic
     *
     * @param value
     * @param characteristic
     * @throws Exception
     */
    public void writeValueToCharacteristic(String value, BluetoothGattCharacteristic characteristic) throws Exception {
        // reset the queue counters, prepare the message to be sent, and send the value to the Characteristic
        mQueuedCharactersticValue = value;
        mNumPacketsSent = 0;
        byte[] byteValue = value.getBytes();
        mNumPacketsTotal = (int) Math.ceil((float) byteValue.length / mCharacteristicLength);
        writePartialValueToCharacteristic(value, mNumPacketsSent, characteristic);

    }


    /**
     * Subscribe or unsubscribe from Characteristic Notifications
     *
     * @param characteristic
     * @param enabled <b>true</b> for "subscribe" <b>false</b> for "unsubscribe"
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, final boolean enabled) {
        // modified from http://stackoverflow.com/a/18011901/5671180
        // This is a 2-step process
        // Step 1: set the Characteristic Notification parameter locally
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        final List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        // Step 2: Write a descriptor to the Bluetooth GATT enabling the subscription on the Perpiheral
        // turns out you need to implement a delay between setCharacteristicNotification and setvalue.
        // maybe it can be handled with a callback, but this is an easy way to implement
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }, 10);
    }


    /**
     * Write a portion of a larger message to a Characteristic
     *
     * @param message The message being written
     * @param offset The current packet index in queue to be written
     * @param characteristic The Characteristic being written to
     * @throws Exception
     */
    public void writePartialValueToCharacteristic(String message, int offset, BluetoothGattCharacteristic characteristic) throws Exception {
        byte[] temp = message.getBytes();

        mNumPacketsTotal = (int) Math.ceil((float) temp.length / mCharacteristicLength);
        int remainder = temp.length % mCharacteristicLength;

        int dataLength = mCharacteristicLength;
        if (offset >= mNumPacketsTotal) {
            dataLength = remainder;
        }

        byte[] chunk = new byte[dataLength];
        for (int localIndex = 0; localIndex < chunk.length; localIndex++) {
            int index = (offset * dataLength) + localIndex;
            if (index < temp.length) {
                chunk[localIndex] = temp[index];
            } else {
                chunk[localIndex] = 0x00;
            }
        }

        // a simpler way to write this might be:
        //System.arraycopy(getCurrentMessage().getBytes(), getCurrentOffset()*mCharacteristicLength, chunk, 0, mCharacteristicLength);
        //chunk[dataLength] = 0x00;


        Log.v(TAG, "Writing message: '" + new String(chunk, "ASCII") + "' to " + characteristic.getUuid().toString());
        characteristic.setValue(chunk);
        mBluetoothGatt.writeCharacteristic(characteristic);
        mNumPacketsSent++;
    }

    /**
     * Determine if a message has been completely wirtten to a Characteristic or if more data is in queue
     *
     * @return <b>false</b> if all of a message is has been written to a Characteristic, <b>true</b> otherwise
     */
    public boolean morePacketsAvailableInQueue() {
        boolean morePacketsAvailable = mNumPacketsSent < mNumPacketsTotal;
        Log.v(TAG, mNumPacketsSent + " of " + mNumPacketsTotal + " packets sent: "+morePacketsAvailable);
        return morePacketsAvailable;
    }

    /**
     * Determine how much of a message has been written to a Characteristic
     *
     * @return integer representing how many packets have been written so far to Characteristic
     */
    public int getCurrentOffset() {
        return mNumPacketsSent;
    }

    /**
     * Get the current message being written to a Characterstic
     *
     * @return the message in queue for writing to a Characteristic
     */
    public String getCurrentMessage() {
        return mQueuedCharactersticValue;
    }



    // http://stackoverflow.com/a/21300916/5671180
    // more options available at:
    // http://www.programcreek.com/java-api-examples/index.php?class=android.bluetooth.BluetoothGattCharacteristic&method=PROPERTY_NOTIFY

    /**
     * Check if a Characetristic supports write permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * Check if a Characetristic has read permissions
     *
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * Check if a Characteristic supports Notifications
     *
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }



}
