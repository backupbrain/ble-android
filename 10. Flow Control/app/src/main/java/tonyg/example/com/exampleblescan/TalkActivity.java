

package tonyg.example.com.exampleblescan;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.BlePeripheral;

/**
 * Connect to a BLE Peripheral, list its GATT services
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class TalkActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = TalkActivity.class.getSimpleName();

    public static final String CHARACTER_ENCODING = "ASCII";
    public static final String PERIPHERAL_MAC_ADDRESS_KEY = "peripheralMacAddress";
    public static final String CHARACTERISTIC_KEY = "characteristicUUID";
    public static final String SERVICE_KEY = "serviceUUID";

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;
    private BlePeripheral mBlePeripheral;

    private BluetoothGattCharacteristic mCharacteristic;

    /** Functional stuff **/
    private String mPeripheralMacAddress;
    private UUID mCharacteristicUUID, mServiceUUID;

    /** UI Stuff **/
    private MenuItem mProgressSpinner, mDisconnectItem;
    private TextView mResponseText, mSendText, mPeripheralBroadcastNameTV, mPeripheralAddressTV, mServiceUUIDTV;
    private Button mSendButton, mReadButton;
    private CheckBox mSubscribeCheckbox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab a Characteristic from the savedInstanceState,
        // passed when a user clicked on a Characteristic in the Connect Activity
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mPeripheralMacAddress = extras.getString(PERIPHERAL_MAC_ADDRESS_KEY);
                mCharacteristicUUID = UUID.fromString(extras.getString(CHARACTERISTIC_KEY));
                mServiceUUID = UUID.fromString(extras.getString(SERVICE_KEY));
            }
        } else {
            mPeripheralMacAddress = savedInstanceState.getString(PERIPHERAL_MAC_ADDRESS_KEY);
            mCharacteristicUUID = UUID.fromString(savedInstanceState.getString(CHARACTERISTIC_KEY));
            mServiceUUID = UUID.fromString(savedInstanceState.getString(SERVICE_KEY));
        }

        Log.v(TAG, "Incoming mac address: "+ mPeripheralMacAddress);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mBleCommManager = new BleCommManager();
        mBlePeripheral = new BlePeripheral();

        loadUI();


    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Load UI components
     */
    public void loadUI() {
        mResponseText = (TextView) findViewById(R.id.response_text);
        mSendText = (TextView) findViewById(R.id.write_text);
        mPeripheralBroadcastNameTV = (TextView)findViewById(R.id.name);
        mPeripheralAddressTV = (TextView)findViewById(R.id.address);
        mServiceUUIDTV = (TextView)findViewById(R.id.service_uuid);

        mSendButton = (Button) findViewById(R.id.write_button);
        mReadButton = (Button) findViewById(R.id.read_button);

        mPeripheralBroadcastNameTV.setText(R.string.connecting);

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

        initializeBluetooth();
        connect();

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


    public void initializeBluetooth() {
        try {
            mBleCommManager.initBluetooth(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
    }


    public void connect() {
        // grab the Peripheral Device address and attempt to connect
        BluetoothDevice bluetoothDevice = mBleCommManager.getBluetoothAdapter().getRemoteDevice(mPeripheralMacAddress);
        mProgressSpinner.setVisible(true);
        try {
            mBlePeripheral.connect(bluetoothDevice, mGattCallback, getApplicationContext());
        } catch (Exception e) {
            mProgressSpinner.setVisible(false);
            Log.e(TAG, "Error connecting to peripheral");
        }
    }

    /**
     * Peripheral has connected.  Update UI
     */
    public void onBleConnected() {
        BluetoothDevice bluetoothDevice = mBlePeripheral.getBluetoothDevice();
        mPeripheralBroadcastNameTV.setText(bluetoothDevice.getName());
        mPeripheralAddressTV.setText(bluetoothDevice.getAddress());
        mProgressSpinner.setVisible(false);
    }

    /**
     * characteristic supports writes.  Update UI
     */
    public void onCharacteristicWritable() {
        Log.v(TAG, "Characteristic is writable");
        // send features

        // attach callbacks to the button and other stuff
        mSendButton.setVisibility(View.VISIBLE);
        mSendText.setVisibility(View.VISIBLE);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // write to the charactersitic when the send button is clicked
                Log.v(TAG, "Send button clicked");
                String message = mSendText.getText().toString();
                try {
                    mBlePeripheral.writeValueToCharacteristic(message, mCharacteristic);

                } catch (Exception e) {
                    Log.e(TAG, "problem sending message through bluetooth");
                }
            }
        });

    }

    /**
     * Charactersitic supports reads.  Update UI
     */
    public void onCharacteristicReadable() {
        Log.v(TAG, "Characteristic is readable");

        mReadButton.setVisibility(View.VISIBLE);
        mResponseText.setVisibility(View.VISIBLE);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Read button clicked");
                mBlePeripheral.readValueFromCharacteristic(mCharacteristic);
            }
        });


    }

    /**
     * Characteristic supports notifications.  Update UI
     */
    public void onCharacteristicNotifiable() {
        mSubscribeCheckbox.setVisibility(View.VISIBLE);
        mSubscribeCheckbox.setChecked(true);
        mSubscribeCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // subscribe to notifications from this channel
                        mBlePeripheral.setCharacteristicNotification(mCharacteristic, isChecked);

                    }
                }
        );
    }

    /**
     * Update TextView when a new message is read from a Charactersitic
     * Also scroll to the bottom so that new messages are always in view
     *
     * @param message the Characterstic value to display in the UI as text
     */
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

    /**
     * Clear the input TextView when a Characteristic is successfully written to.
     */
    public void onBleCharacteristicValueWritten() {
        mSendText.setText("");
    }

    /**
     * On a multi-part message, send the next packet of a message when a write operation is successful
     *
     * @param characteristic the Charactersitic being written to
     */
    public void onBleCharacteristicReady(final BluetoothGattCharacteristic characteristic) {
        Log.v(TAG, "Flow control message received by server");
        if (mBlePeripheral.morePacketsAvailableInQueue()) {
            try {
                mBlePeripheral.writePartialValueToCharacteristic(mBlePeripheral.getCurrentMessage(), mBlePeripheral.getCurrentOffset(), characteristic);
            } catch (Exception e) {
                Log.e(TAG, "Unable to send next chunk of message");
            }
        }
    }

    /**
     * convert bytes to hexadecimal for debugging purposes
     *
     * @param bytes
     * @return Hexadecimal String representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes.length <=0) return "";
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

    /**
     * convert bytes to an integer in Little Endian for debugging purposes
     *
     * @param bytes a byte array
     * @return integer integer representation of byte array
     */
    //
    public static String bytesToInt(byte[] bytes) {
        if (bytes.length <=0) return "";
        char[] decArray = "0123456789".toCharArray();
        char[] decChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            decChars[j * 2] = decArray[v >>> 4];
            decChars[j * 2 + 1] = decArray[v & 0x0F];
        }

        return new String(decChars);
    }

    /**
     * BluetoothGattCallback handles connections, state changes, reads, writes, and GATT profile listings to a Peripheral
     *
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Charactersitic successfuly read
         *
         * @param gatt connection to GATT
         * @param characteristic The charactersitic that was read
         * @param status the status of the operation
         */
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {
            // characteristic was read.  Convert the data to something usable
            // on Android and display it in the UI
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // read more at http://developer.android.com/guide/topics/connectivity/bluetooth-le.html#notification
                final byte[] data = characteristic.getValue();
                String m = "";
                try {
                    m = new String(data, CHARACTER_ENCODING);
                } catch (Exception e) {
                    Log.e(TAG, "Could not convert message byte array to String");
                }
                final String message = m;

                if (message.equals(BlePeripheral.FLOW_CONROL_VALUE)) {
                    onBleCharacteristicReady(characteristic);
                }

                Log.v(TAG, "Characteristic read hex value: "+bytesToHex(data));
                Log.v(TAG, "Characteristic read int value: "+bytesToInt(data));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateResponseText(message);
                    }
                });
            }

        }

        /**
         * Characteristic was written successfully.  update the UI
         *
         * @param gatt Connection to the GATT
         * @param characteristic The Characteristic that was written
         * @param status write status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "characteristic written");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleCharacteristicValueWritten();
                    }
                });
            }
        }

        /**
         * Charactersitic value changed.  Read new value.
         * @param gatt Connection to the GATT
         * @param characteristic The Characterstic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            mBlePeripheral.readValueFromCharacteristic(characteristic);
        }

        /**
         * Peripheral connected or disconnected.  Update UI
         * @param bluetoothGatt Connection to GATT
         * @param status status of the operation
         * @param newState new connection state
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to peripheral");


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleConnected();
                    }
                });

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(TAG, "Disconnected from peripheral");

                disconnect();
                mBlePeripheral.close();
            }
        }

        /**
         * GATT Profile discovered.  Update UI
         * @param bluetoothGatt connection to GATT
         * @param status status of operation
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {

            // if services were discovered, then let's iterate through them and display them on screen
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // connect to a specific service

                BluetoothGattService gattService = bluetoothGatt.getService(mServiceUUID);
                // while we are here, let's ask for this service's characteristics:
                List<BluetoothGattCharacteristic> characteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic != null) {
                        Log.v(TAG, "found characteristic: "+characteristic.getUuid().toString());

                    }
                }

                // determine the read/write/notify permissions of the Characterstic
                Log.v(TAG, "desired service is: "+mServiceUUID.toString());
                Log.v(TAG, "desired charactersitic is: "+mCharacteristicUUID.toString());
                Log.v(TAG, "this service: "+bluetoothGatt.getService(mServiceUUID).getUuid().toString());
                Log.v(TAG, "this characteristic: "+bluetoothGatt.getService(mServiceUUID).getCharacteristic(mCharacteristicUUID).getUuid().toString());

                mCharacteristic = bluetoothGatt.getService(mServiceUUID).getCharacteristic(mCharacteristicUUID);
                if (BlePeripheral.isCharacteristicReadable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCharacteristicReadable();
                        }
                    });
                }

                if (BlePeripheral.isCharacteristicWritable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCharacteristicWritable();
                        }
                    });
                }


                if (BlePeripheral.isCharacteristicNotifiable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBlePeripheral.setCharacteristicNotification(mCharacteristic, true);
                            onCharacteristicNotifiable();

                        }
                    });
                }



            } else {
                Log.e(TAG, "Something went wrong while discovering GATT services from this peripheral");
            }


        }
    };


    /**
     * Disconnect
     */
    private void disconnect() {
        // close the Activity when disconnecting.  No actions can be done without a connection
        mBlePeripheral.disconnect();
        finish();
    }



}
