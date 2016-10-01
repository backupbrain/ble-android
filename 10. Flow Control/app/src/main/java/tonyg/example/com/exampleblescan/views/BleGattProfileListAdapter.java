package tonyg.example.com.exampleblescan.views;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import tonyg.example.com.exampleblescan.R;
import tonyg.example.com.exampleblescan.ble.BleDevice;


/**
 * Manages the BleGattServiceListItem so that we can populate the list
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class BleGattProfileListAdapter extends BaseExpandableListAdapter {
    private final static String TAG = BleGattProfileListAdapter.class.getSimpleName();

    private ArrayList<BleGattServiceListItem> mBleGattServiceListItems;
    private Map<Integer, ArrayList<BleGattCharacteristicListItem>> mBleCharacteristicListItems;


    //private final ArrayList<BleGattServiceListItem> listItems = new ArrayList<BleGattServiceListItem>(); // FIXME: add this and an addService() method
    /*
    public BleGattProfileListAdapter(ArrayList<BleGattServiceListItem> mBleGattServiceListItems, Map<Integer, ArrayList<BleGattCharacteristicListItem>> mBleCharacteristicListItems) {
        mBleGattServiceListItems = mBleGattServiceListItems;
        mBleCharacteristicListItems = mBleCharacteristicListItems;
    }
    */

    public BleGattProfileListAdapter() {

    }


    /** Parent (Service) methods **/
    public BleGattServiceListItem getGroup(int groupPosition) {
        return mBleGattServiceListItems.get(groupPosition);
    }

    public int getGroupCount() {
        return mBleGattServiceListItems.size();
    }


    public void addService(BluetoothGattService service) {
        int serviceItemID = mBleGattServiceListItems.size()-1;
        BleGattServiceListItem serviceListItem = new BleGattServiceListItem(service, serviceItemID);
        mBleGattServiceListItems.add(serviceListItem);
        mBleCharacteristicListItems.put(serviceItemID, new ArrayList<BleGattCharacteristicListItem>());

        notifyDataSetChanged();
    }


    public void addCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic) throws Exception {
        // find the service with this UUID
        int serviceItemId = -1;
        for (BleGattServiceListItem bleGattServiceListItem : mBleGattServiceListItems) {
            if (bleGattServiceListItem.getService().equals(service)) {
                serviceItemId = bleGattServiceListItem.getItemId();
            }
        }

        if (serviceItemId < 0) throw new Exception("Service not found with UUID: "+service.getUuid().toString());

        int characteristicItemId = mBleCharacteristicListItems.size();

        BleGattCharacteristicListItem characteristicListItem = new BleGattCharacteristicListItem(characteristic, characteristicItemId);

        mBleCharacteristicListItems.get(serviceItemId).add(characteristicListItem);

        notifyDataSetChanged();
    }

    public void clear() {
        mBleGattServiceListItems.clear();
        mBleCharacteristicListItems.clear();
    }

    public long getGroupId(int position) {
        return mBleGattServiceListItems.get(position).getItemId();
    }

    /**
     * What UI stuff is contained in this List Item
     */
    public static class GroupViewHolder{
        public TextView mUuid;
        public TextView mType;
    }


    public View getGroupView(int position, boolean isExpanded, View convertView, ViewGroup parent) {
        View v = convertView;
        GroupViewHolder holder;

        if(convertView == null) {
            // convert list_item_device.xmlice.xml to a View
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            v = inflater.inflate(R.layout.list_item_ble_service, parent, false);

            // match the UI stuff in the list Item to what's in the xml file
            holder = new GroupViewHolder();
            holder.mUuid = (TextView) v.findViewById(R.id.uuid);
            holder.mType = (TextView) v.findViewById(R.id.type);


            v.setTag( holder );
        } else {
            holder = (GroupViewHolder) v.getTag();
        }

        if (getGroupCount() <= 0) {
            holder.mUuid.setText(R.string.no_data);
        } else {
            BleGattServiceListItem item = getGroup(position);

            holder.mUuid.setText(item.getUUIDString());
            int type = item.getType();
            if (type == BluetoothGattService.SERVICE_TYPE_PRIMARY) {
                holder.mType.setText(R.string.service_type_primary);
            } else {
                holder.mType.setText(R.string.service_type_secondary);
            }
        }
        return v;
    }


    public boolean hasStableIds() {
        return true;
    }



    /** Child methods **/

    public BleGattCharacteristicListItem getChild(int groupPosition, int childPosition) {
        return mBleCharacteristicListItems.get(groupPosition).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }


    public int getChildrenCount(int groupPosition) {
        return mBleCharacteristicListItems.get(groupPosition).size();
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }



    public static class ChildViewHolder{
        public TextView mUuid;
    }

    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        BleGattCharacteristicListItem item = getChild(groupPosition, childPosition);
        BluetoothGattCharacteristic characteristic = item.getCharacteristic();

        if (convertView == null) {

            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            convertView = inflater.inflate(R.layout.list_item_ble_characteristic, null);
        }

        TextView uuidTV = (TextView) convertView.findViewById(R.id.uuid);


        TextView mPropertyReadable = (TextView)convertView.findViewById(R.id.property_read);
        TextView mPropertyWritable = (TextView)convertView.findViewById(R.id.property_write);
        TextView mPropertyNotifiable = (TextView)convertView.findViewById(R.id.property_notify);
        TextView mPropertyNone = (TextView)convertView.findViewById(R.id.property_none);

        uuidTV.setText(characteristic.getUuid().toString());

        if (BleDevice.isCharacteristicReadable(characteristic)) {
            mPropertyReadable.setVisibility(View.VISIBLE);
        } else {
            mPropertyReadable.setVisibility(View.GONE);
        }
        if (BleDevice.isCharacteristicWritable(characteristic)) {
            mPropertyWritable.setVisibility(View.VISIBLE);
        } else {
            mPropertyWritable.setVisibility(View.GONE);
        }
        if (BleDevice.isCharacteristicNotifiable(characteristic)) {
            mPropertyNotifiable.setVisibility(View.VISIBLE);
        } else {
            mPropertyNotifiable.setVisibility(View.GONE);
        }

        if (!BleDevice.isCharacteristicNotifiable(characteristic) &&
                !BleDevice.isCharacteristicWritable(characteristic) &&
                !BleDevice.isCharacteristicReadable(characteristic)) {
            mPropertyNone.setVisibility(View.VISIBLE);
        } else {
            mPropertyNone.setVisibility(View.GONE);

        }
        return convertView;
    }

}