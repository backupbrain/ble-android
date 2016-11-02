package tonyg.example.com.exampleblescan.views;

import android.bluetooth.BluetoothDevice;
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


public class BleDevicesListAdapter extends BaseAdapter {

    private ArrayList<BleDeviceListItem> mBluetoothDeviceListItems; // list of Peripherals

    /**
     * How many items are in the ListView
     * @return the number of items in this ListView
     */
    public int getCount() {
        if (mBluetoothDeviceListItems.size()<=0)  return 1;
        return mBluetoothDeviceListItems.size();
    }

    /**
     * Add a new Peripheral to the ListView
     *
     * @param device Periheral device information
     * @param rssi Periheral's RSSI, indicating its radio signal quality
     */
    public void addBluetoothDevice(BluetoothDevice device, int rssi) {
        // update UI stuff
        int listItemId = mBluetoothDeviceListItems.size();
        BleDeviceListItem listItem = new BleDeviceListItem();
        listItem.setDevice(device);
        listItem.setItemId(listItemId);
        listItem.setPowerLevel(rssi);

        // add to list
        mBluetoothDeviceListItems.add(listItem);
        notifyDataSetChanged();
    }

    /**
     * Get current state of ListView
     * @return ArrayList of BleDeviceListItems
     */
    public ArrayList<BleDeviceListItem> getItems() {
        return mBluetoothDeviceListItems;
    }

    /**
     * Clear all items from the ListView
     */
    public void clear() {
        mBluetoothDeviceListItems.clear();
    }

    /**
     * Get the BleDeviceListItem held at some position in the ListView
     *
     * @param position the position of a desired item in the list
     * @return the BleDeviceListItem at some position
     */
    public BleDeviceListItem getItem(int position) {
        return mBluetoothDeviceListItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * This ViewHolder represents what UI components are in each List Item in the ListView
     */
    public static class ViewHolder{
        public TextView mName;
        public TextView mAddress;
        public TextView mPowerLevel;
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
            // convert list_item_device_device.xml to a View
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            v = inflater.inflate(R.layout.list_item_device, null);

            // match the UI stuff in the list Item to what's in the xml file
            holder = new ViewHolder();
            holder.mName = (TextView) v.findViewById(R.id.name);
            holder.mAddress = (TextView) v.findViewById(R.id.address);
            holder.mPowerLevel = (TextView) v.findViewById(R.id.power_level);

            v.setTag( holder );
        } else {
            holder = (ViewHolder) v.getTag();
        }

        // if there are known Peripherals, create a ListItem that says so
        // otherwise, display a ListItem with Bluetooth Periheral information
        if (mBluetoothDeviceListItems.size() <= 0) {
            holder.mName.setText(R.string.no_data);
        } else {
            BleDeviceListItem item = mBluetoothDeviceListItems.get(position);

            holder.mName.setText(item.getName());
            holder.mAddress.setText(item.getAddress());
            holder.mPowerLevel.setText(String.valueOf(item.getRssi()));
        }
        return v;
    }

}