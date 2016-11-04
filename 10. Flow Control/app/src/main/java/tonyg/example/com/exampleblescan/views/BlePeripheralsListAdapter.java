package tonyg.example.com.exampleblescan.views;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import tonyg.example.com.exampleblescan.R;

/**
 * Manages the BLEDeviceListItems so that we can populate the list
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-17
 */


public class BlePeripheralsListAdapter extends BaseAdapter {
    private static String TAG = BlePeripheralsListAdapter.class.getSimpleName();

    private ArrayList<BlePeripheralListItem> mBluetoothPeripheralListItems = new ArrayList<BlePeripheralListItem>(); // list of Peripherals

    /**
     * How many items are in the ListView
     * @return the number of items in this ListView
     */
    public int getCount() {
        return mBluetoothPeripheralListItems.size();
    }

    /**
     * Add a new Peripheral to the ListView
     *
     * @param device Periheral device information
     * @param rssi Periheral's RSSI, indicating its radio signal quality
     */
    public void addBluetoothPeripheral(BluetoothDevice device, int rssi) {
        // update UI stuff
        int listItemId = mBluetoothPeripheralListItems.size();
        BlePeripheralListItem listItem = new BlePeripheralListItem();
        listItem.setDevice(device);
        listItem.setItemId(listItemId);
        listItem.setRssi(rssi);

        // add to list
        mBluetoothPeripheralListItems.add(listItem);
    }

    /**
     * Get current state of ListView
     * @return ArrayList of BlePeripheralListItems
     */
    public ArrayList<BlePeripheralListItem> getItems() {
        return mBluetoothPeripheralListItems;
    }

    /**
     * Clear all items from the ListView
     */
    public void clear() {
        mBluetoothPeripheralListItems.clear();
    }

    /**
     * Get the BlePeripheralListItem held at some position in the ListView
     *
     * @param position the position of a desired item in the list
     * @return the BlePeripheralListItem at some position
     */
    public BlePeripheralListItem getItem(int position) {
        return mBluetoothPeripheralListItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * This ViewHolder represents what UI components are in each List Item in the ListView
     */
    public static class ViewHolder{
        public TextView mBroadcastName;
        public TextView mMacAddress;
        public TextView mRssi;
    }

    /**
     * Generate a new ListItem for some known position in the ListView
     *
     * @param position the position of the ListItem
     * @param convertView An existing List Item
     * @param parent The Parent ViewGroup
     * @return The List Item
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        // if this ListItem does not exist yet, generate it
        // otherwise, use it
        if(convertView == null) {
            // convert list_item_peripheral.xml to a View
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            v = inflater.inflate(R.layout.list_item_peripheral, null);

            // match the UI stuff in the list Item to what's in the xml file
            holder = new ViewHolder();
            holder.mBroadcastName = (TextView) v.findViewById(R.id.name);
            holder.mMacAddress = (TextView) v.findViewById(R.id.address);
            holder.mRssi = (TextView) v.findViewById(R.id.power_level);

            v.setTag( holder );
        } else {
            holder = (ViewHolder) v.getTag();
        }
        Log.v(TAG, "ListItem size: "+ mBluetoothPeripheralListItems.size());
        // if there are known Peripherals, create a ListItem that says so
        // otherwise, display a ListItem with Bluetooth Periheral information
        if (mBluetoothPeripheralListItems.size() <= 0) {
            holder.mBroadcastName.setText(R.string.no_peripherals);
        } else {
            BlePeripheralListItem item = mBluetoothPeripheralListItems.get(position);

            holder.mBroadcastName.setText(item.getBroadcastName());
            holder.mMacAddress.setText(item.getMacAddress());
            holder.mRssi.setText(String.valueOf(item.getRssi()));
        }
        return v;
    }

}