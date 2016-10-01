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

    private ArrayList<BleDeviceListItem> mBluetoothDeviceListItems;

    /*
    public BleDevicesListAdapter() { //, ArrayList<BleDeviceListItem> mBluetoothDeviceListItems) {
        //mContext = context;
        //mBluetoothDeviceListItems = mBluetoothDeviceListItems;
    }
    */

    public int getCount() {
        if (mBluetoothDeviceListItems.size()<=0)  return 1;
        return mBluetoothDeviceListItems.size();
    }

    public void addBluetoothDevice(BluetoothDevice device, int rssi) {
        int listItemId = mBluetoothDeviceListItems.size();
        BleDeviceListItem listItem = new BleDeviceListItem();
        listItem.setDevice(device);
        listItem.setItemId(listItemId);
        listItem.setPowerLevel(rssi);

        mBluetoothDeviceListItems.add(listItem);
        notifyDataSetChanged();
    }

    public ArrayList<BleDeviceListItem> getItems() {
        return mBluetoothDeviceListItems;
    }

    public void clear() {
        mBluetoothDeviceListItems.clear();
    }

    public BleDeviceListItem getItem(int position) {
        return mBluetoothDeviceListItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * What UI stuff is contained in this List Item
     */
    public static class ViewHolder{
        public TextView mName;
        public TextView mAddress;
        public TextView mPowerLevel;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

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