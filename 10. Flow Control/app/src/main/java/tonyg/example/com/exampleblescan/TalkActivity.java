

package tonyg.example.com.exampleblescan;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.BleDevice;

/**
 * Connect to a BLE Device, list its GATT services
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class TalkActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = TalkActivity.class.getSimpleName();
    public static final String MAC_ADDRESS_KEY = "bluetoothMacAddress"; // FIXME: give a better name
    public static final String CHARACTERISTIC_KEY = "characteristicUUID"; // FIXME: give a better name
    public static final String SERVICE_KEY = "serviceUUID"; // FIXME: give a better name

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;
    private BleDevice mBleDevice;

    private BluetoothGattCharacteristic mCharacteristic;

    /** Functional stuff **/
    private String mDeviceAddress;
    private UUID mCharacteristicUUID, mServiceUUID;

    /** UI Stuff **/
    private MenuItem mProgressSpinner, mDisconnectItem;
    private TextView mResponseText, mSendText, mDeviceNameTV, mDeviceAddressTV, mServiceUUIDTV;
    private Button mSendButton, mReadButton;
    private CheckBox mSubscribeCheckbox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab a device from the savedInstanceState
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mDeviceAddress = extras.getString(MAC_ADDRESS_KEY);
                mCharacteristicUUID = UUID.fromString(extras.getString(CHARACTERISTIC_KEY));
                mServiceUUID = UUID.fromString(extras.getString(SERVICE_KEY));
            }
        } else {
            mDeviceAddress = savedInstanceState.getString(MAC_ADDRESS_KEY);
            mCharacteristicUUID = UUID.fromString(savedInstanceState.getString(CHARACTERISTIC_KEY));
            mServiceUUID = UUID.fromString(savedInstanceState.getString(SERVICE_KEY));
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mBleCommManager = new BleCommManager();
        mBleDevice = new BleDevice();

        loadUI();


    }


    @Override
    public void onResume() {
        super.onResume();

        BluetoothDevice bluetoothDevice = mBleCommManager.getBluetoothAdapter().getRemoteDevice(mDeviceAddress);
        mProgressSpinner.setVisible(true);
        try {
            mBleDevice.connect(bluetoothDevice, mGattCallback, getApplicationContext());
        } catch (Exception e) {
            mProgressSpinner.setVisible(false);
            Log.e(TAG, "Error connecting to device");
        }
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    public void loadUI() {
        mResponseText = (TextView) findViewById(R.id.response_text);
        mSendText = (TextView) findViewById(R.id.send_text);
        mDeviceNameTV = (TextView)findViewById(R.id.name);
        mDeviceAddressTV = (TextView)findViewById(R.id.address);
        mServiceUUIDTV = (TextView)findViewById(R.id.service_uuid);

        mSendButton = (Button) findViewById(R.id.send_button);
        mReadButton = (Button) findViewById(R.id.read_button);

        BluetoothDevice device = mBleDevice.getBluetoothDevice();
        mDeviceNameTV.setText(device.getName());
        mDeviceAddressTV.setText(device.getAddress());
        Log.v(TAG, "Incoming Service UUID: " + mServiceUUID.toString());
        Log.v(TAG, "Incoming Characteristic UUID: " + mCharacteristicUUID.toString());
        mServiceUUIDTV.setText(mCharacteristicUUID.toString());

        mSubscribeCheckbox = (CheckBox) findViewById(R.id.subscribe_checkbox);

        mSendButton.setVisibility(View.GONE);
        mSendText.setVisibility(View.GONE);
        mReadButton.setVisibility(View.GONE);
        mResponseText.setVisibility(View.GONE);
        mSubscribeCheckbox.setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_talk, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mDisconnectItem =  menu.findItem(R.id.action_disconnect);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);

        mDisconnectItem.setVisible(true);
        mProgressSpinner.setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                // User chose the "Stop" item
                disconnect();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    public void onCharacteristicWritable() {
        Log.v(TAG, "Characteristic is writable");
        // send features

        // attach callbacks to the buttons and stuff
        mSendButton.setVisibility(View.VISIBLE);
        mSendText.setVisibility(View.VISIBLE);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Send button clicked");
                String message = mSendText.getText().toString();
                try {
                    mBleDevice.sendMessage(message, mCharacteristic);

                } catch (Exception e) {
                    Log.e(TAG, "problem sending message through bluetooth");
                }
            }
        });

    }
    public void onCharacteristicReadable() {
        Log.v(TAG, "Characteristic is readable");

        mReadButton.setVisibility(View.VISIBLE);
        mResponseText.setVisibility(View.VISIBLE);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Read button clicked");
                mBleDevice.readMessage(mCharacteristic);
            }
        });


    }

    public void onCharacteristicNotifiable() {
        mSubscribeCheckbox.setVisibility(View.VISIBLE);
        mSubscribeCheckbox.setChecked(true);
        mSubscribeCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // subscribe to notifications from this channel
                        mBleDevice.setCharacteristicNotification(mCharacteristic, isChecked);

                    }
                }
        );
    }

    public void updateResponseText(String message) {
        mResponseText.append(message + "\n");
        final int scrollAmount = mResponseText.getLayout().getLineTop(mResponseText.getLineCount()) - mResponseText.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0) {
            mResponseText.scrollTo(0, scrollAmount);
        } else {
            mResponseText.scrollTo(0, 0);
        }
    }

    public void onBleMessageSent() {
        mSendText.setText("");
    }

    public void onBleSentMessageReceived(final BluetoothGattCharacteristic characteristic) {
        Log.v(TAG, "Flow control message received by server");
        if (mBleDevice.hasMoreChunks()) {
            try {
                mBleDevice.sendNextChunk(mBleDevice.getCurrentMessage(), mBleDevice.getCurrentOffset(), characteristic);
            } catch (Exception e) {
                Log.e(TAG, "Unable to send next chunk of message");
            }
        }
    }
    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = 0x20; // space
        }
        return new String(hexChars);
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes); // big-endian by default
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int result = byteBuffer.getInt();
        return result;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // read more at http://developer.android.com/guide/topics/connectivity/bluetooth-le.html#notification
                final byte[] data = characteristic.getValue();
                String m = "";
                try {
                    m = new String(data, "ASCII"); // FIXME: set as a constant
                } catch (Exception e) {
                    Log.e(TAG, "Could not convert message byte array to String");
                }
                final String message = m;

                if (message.equals(BleDevice.FLOW_CONROL_MESSAGE)) {
                    onBleSentMessageReceived(characteristic);
                }

                Log.v(TAG, "Characteristic read: "+bytesToHex(data));
                Log.v(TAG, "Characteristic read: "+bytesToInt(data));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateResponseText(message);
                    }
                });
            }

        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "characteristic written");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleMessageSent();
                    }
                });
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            mBleDevice.readMessage(characteristic);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to device");

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(TAG, "Disconnected from device");

                disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {

            // if services were discovered, then let's iterate through them and display them on screen
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // connect to a specific service

                mCharacteristic = bluetoothGatt.getService(mServiceUUID).getCharacteristic(mCharacteristicUUID);
                if (BleDevice.isCharacteristicReadable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCharacteristicReadable();
                        }
                    });
                }

                if (BleDevice.isCharacteristicWritable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCharacteristicWritable();
                        }
                    });
                }


                if (BleDevice.isCharacteristicNotifiable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBleDevice.setCharacteristicNotification(mCharacteristic, true);
                            onCharacteristicNotifiable();

                        }
                    });
                }



            } else {
                Log.e(TAG, "Something went wrong while discovering GATT services from this device");
            }


        }
    };


    private void disconnect() {
        mBleDevice.disconnect();
        finish();
    }



}
