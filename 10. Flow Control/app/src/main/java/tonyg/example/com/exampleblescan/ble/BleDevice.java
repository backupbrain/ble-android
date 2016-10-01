package tonyg.example.com.exampleblescan.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * This class allows us to share Bluetooth resources
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class BleDevice {
    private static final String TAG = BleDevice.class.getSimpleName();

    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    /** Flow control stuff **/
    private int numChunksTotal;
    private int numChunksSent;
    private int chunkSize = 20;
    private String outboundMessage;
    public static final String FLOW_CONROL_MESSAGE = "ready";

    public BleDevice() {
    }


    public BluetoothGatt connect(BluetoothDevice device, BluetoothGattCallback callback, final Context context) throws Exception {
        if (device == null) {
            throw new Exception("No device found");
        }
        mBluetoothDevice = device;
        mBluetoothGatt = device.connectGatt(context, false, callback);
        refreshDeviceCache();
        return mBluetoothGatt;
    }
    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }
    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public BluetoothGattService getService(UUID uuid) {
        return mBluetoothGatt.getService(uuid);
    }


    // When we are developing a bluetooth device, we need a current list of services
    // by default, Android caches these services and leaves us with an out-of-date list
    // http://stackoverflow.com/a/22709467
    public boolean refreshDeviceCache() throws Exception {
        Method localMethod = mBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
        if (localMethod != null) {
            boolean bool = ((Boolean) localMethod.invoke(mBluetoothGatt, new Object[0])).booleanValue();
            return bool;
        }

        return false;
    }

    // http://stackoverflow.com/a/20020279
    public void readMessage(final BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    public void sendMessage(String message, BluetoothGattCharacteristic characteristic) throws Exception {
        outboundMessage = message;
        numChunksSent = 0;
        byte[] messageBytes = message.getBytes();
        numChunksTotal = (int) Math.ceil((float) messageBytes.length / chunkSize);
        sendNextChunk(message, numChunksSent, characteristic);

    }



    // modified fromhttp://stackoverflow.com/a/18011901/5671180
    //public boolean setCharacteristicNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicUuid, boolean enable) {
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, final boolean enabled) {
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        final List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        // turns out you need to implement a delay between setCharacteristicNotification and setvalue.
        // maybe it can be handled with a callback?
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x00, 0x00 });
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }, 10);
    }



    public void sendNextChunk(String message, int offset, BluetoothGattCharacteristic characteristic) throws Exception {
        byte[] temp = message.getBytes();

        numChunksTotal = (int) Math.ceil((float) temp.length / chunkSize);
        int remainder = temp.length % chunkSize;

        int dataLength = chunkSize;
        if (offset >= numChunksTotal) {
            dataLength = remainder;
        }

        byte[] chunk = new byte[dataLength];
        //System.arraycopy(getCurrentMessage().getBytes(), getCurrentOffset()*chunkSize, chunk, 0, chunkSize);
        //chunk[dataLength] = 0x00;
        for (int localIndex = 0; localIndex < chunk.length; localIndex++) {
            int index = (offset * dataLength) + localIndex;
            if (index < temp.length) {
                chunk[localIndex] = temp[index];
            } else {
                chunk[localIndex] = 0x00;
            }
        }


        Log.v(TAG, "Writing message: '" + new String(chunk, "ASCII") + "' to " + characteristic.getUuid().toString());
        characteristic.setValue(chunk);
        mBluetoothGatt.writeCharacteristic(characteristic);
        numChunksSent++;
    }
    public boolean hasMoreChunks() {
        boolean hasMoreChunks = numChunksSent < numChunksTotal;
        Log.v(TAG, numChunksSent + " of " + numChunksTotal + " chunks sent: "+hasMoreChunks);
        return hasMoreChunks;
    }
    public int getCurrentOffset() {
        return numChunksSent;
    }
    public int getTotalChunks() {
        return numChunksTotal;
    }
    public String getCurrentMessage() {
        return outboundMessage;
    }



    // http://stackoverflow.com/a/21300916/5671180
    // more options available at:
    // http://www.programcreek.com/java-api-examples/index.php?class=android.bluetooth.BluetoothGattCharacteristic&method=PROPERTY_NOTIFY
    /**
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }



}
