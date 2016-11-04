package tonyg.example.com.exampleblescan.views;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tonyg.example.com.exampleblescan.R;
import tonyg.example.com.exampleblescan.ble.BlePeripheral;


/**
 * Manages the BleGattServiceListItem so that we can populate the GATT Profile List
 * Uses a BaseExpandableListAdapter to create a tree-like structure
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class BleGattProfileListAdapter extends BaseExpandableListAdapter {
    private final static String TAG = BleGattProfileListAdapter.class.getSimpleName();

    private ArrayList<BleGattServiceListItem> mBleGattServiceListItems = new ArrayList<BleGattServiceListItem>(); // list of Services
    private Map<Integer, ArrayList<BleGattCharacteristicListItem>> mBleCharacteristicListItems = new HashMap<Integer, ArrayList<BleGattCharacteristicListItem>>(); // list of Characteristics

    /**
     * Instantiate the class
     */
    public BleGattProfileListAdapter() {

    }

    /** PARENT (SERVICE) METHODS FOR COLLAPSIBLE TREE STRUCTURE **/

    /**
     * Get the Service ListItem listed in a groupPosition
     * @param groupPosition the position of the ListItem
     * @return the ServiceListItem
     */
    public BleGattServiceListItem getGroup(int groupPosition) {
        return mBleGattServiceListItems.get(groupPosition);
    }

    /**
     * How many Services are listed
     * @return number of Services
     */
    public int getGroupCount() {
        return mBleGattServiceListItems.size();
    }

    /**
     * Add a new Service to be listed in the ListView
     * @param service the GATT Service to add
     */
    public void addService(BluetoothGattService service) {
        int serviceItemID = mBleGattServiceListItems.size();
        BleGattServiceListItem serviceListItem = new BleGattServiceListItem(service, serviceItemID);
        mBleGattServiceListItems.add(serviceListItem);
        mBleCharacteristicListItems.put(serviceItemID, new ArrayList<BleGattCharacteristicListItem>());
    }

    /**
     * Add a new Characteristic to be listed in the ListView
     *
     * @param service the Service that this Characteristic belongs to
     * @param characteristic the Gatt Characteristic to add
     * @throws Exception if such a service does not exist
     */
    public void addCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic) throws Exception {
        // find the Service in this listView with matching UUID from the input service
        int serviceItemId = -1;
        for (BleGattServiceListItem bleGattServiceListItem : mBleGattServiceListItems) {
            //serviceItemId++;
            if (bleGattServiceListItem.getService().getUuid().equals(service.getUuid())) {
                Log.v(TAG, "Service found with UUID: "+service.getUuid().toString());
                serviceItemId = bleGattServiceListItem.getItemId();
                //break;
            }
        }

        // Throw an exception if no such service exists
        if (serviceItemId < 0) throw new Exception("Service not found with UUID: "+service.getUuid().toString());

        // add characterstic to the end of the sub-list for the parent service
        int characteristicItemId = mBleCharacteristicListItems.size();
        if (mBleCharacteristicListItems.get(serviceItemId) == null) {
            mBleCharacteristicListItems.put(serviceItemId, new ArrayList<BleGattCharacteristicListItem>());
        }
        BleGattCharacteristicListItem characteristicListItem = new BleGattCharacteristicListItem(characteristic, characteristicItemId);
        mBleCharacteristicListItems.get(serviceItemId).add(characteristicListItem);
    }

    /**
     * Clear all ListItems from ListView
     */
    public void clear() {
        mBleGattServiceListItems.clear();
        mBleCharacteristicListItems.clear();
    }

    /**
     * Get the Service ListItem at some position
     *
     * @param position the position of the ListItem
     * @return BleGattServiceListItem at position
     */
    public long getGroupId(int position) {
        return mBleGattServiceListItems.get(position).getItemId();
    }

    /**
     * This GroupViewHolder represents what UI components are in each Service ListItem in the ListView
     */
    public static class GroupViewHolder{
        public TextView mUuid;
        public TextView mType;
    }


    /**
     * Generate a new Service ListItem for some known position in the ListView
     *
     * @param position the position of the Service ListItem
     * @param convertView An existing List Item
     * @param parent The Parent ViewGroup
     * @return The Service List Item
     */
    public View getGroupView(int position, boolean isExpanded, View convertView, ViewGroup parent) {
        View v = convertView;
        GroupViewHolder holder;

        // if this ListItem does not exist yet, generate it
        // otherwise, use it
        if(convertView == null) {
            // convert list_item_peripheral.xml.xml to a View
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

        // if there are known Services, create a ListItem that says so
        // otherwise, display a ListItem with Bluetooth Service information
        if (getGroupCount() <= 0) {
            holder.mUuid.setText(R.string.peripheral_list_empty);
        } else {
            BleGattServiceListItem item = getGroup(position);

            holder.mUuid.setText(item.getUUIDString());
            int type = item.getType();
            // Is this a primary or secondary service
            if (type == BluetoothGattService.SERVICE_TYPE_PRIMARY) {
                holder.mType.setText(R.string.service_type_primary);
            } else {
                holder.mType.setText(R.string.service_type_secondary);
            }
        }
        return v;
    }

    /**
     * The IDs for this ListView do not change
     * @return <b>true</b>
     */
    public boolean hasStableIds() {
        return true;
    }



    /** CHILD (CHARACTERISTIC) METHODS FOR COLLAPSIBLE TREE STRUCTURE **/

    /**
     * Get the Characteristic ListItem at some position in the ListView
     * @param groupPosition the position of the Service ListItem in the ListView
     * @param childPosition the sub-position of the Characteristic ListItem under the Service
     * @return BleGattCharactersiticListItem at some groupPosition, childPosition
     */
    public BleGattCharacteristicListItem getChild(int groupPosition, int childPosition) {
        return mBleCharacteristicListItems.get(groupPosition).get(childPosition);
    }

    /**
     * Get the ID of a Charactersitic ListItem
     *
     * @param groupPosition the position of a Service ListItem
     * @param childPosition The sub-position of a Characteristic ListItem
     * @return the ID of the Characteristic ListItem
     */
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }


    /**
     * How many Characteristics exist under a Service
     *
     * @param groupPosition The position of the Service ListItem in the ListView
     * @return the number of Characteristics in this Service
     */
    public int getChildrenCount(int groupPosition) {
        return mBleCharacteristicListItems.get(groupPosition).size();
    }

    /**
     * Charactersitics are selectable because the user can click on them to open the TalkActivity
     *
     * @param groupPosition The Service ListItem position
     * @param childPosition The Charecteristic ListItem sub-position
     * @return <b>true</b>
     */
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }



    /**
     * This ChildViewHolder represents what UI components are in each Characteristic List Item in the ListView
     */
    public static class ChildViewHolder{
        public TextView mUuid;
    }

    /**
     * Generate a new Characteristic ListItem for some known position in the ListView
     *
     * @param groupPosition the position of the Service ListItem
     * @param childPosition the position of the Characterstic ListItem
     * @param convertView An existing List Item
     * @param parent The Parent ViewGroup
     * @return The Characteristic ListItem
     */
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        BleGattCharacteristicListItem item = getChild(groupPosition, childPosition);
        BluetoothGattCharacteristic characteristic = item.getCharacteristic();

        // if this ListItem does not exist yet, generate it
        // otherwise, use it
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.list_item_ble_characteristic, null);
        }

        // prep the View
        TextView uuidTV = (TextView) convertView.findViewById(R.id.uuid);
        TextView mPropertyReadable = (TextView)convertView.findViewById(R.id.property_read);
        TextView mPropertyWritable = (TextView)convertView.findViewById(R.id.property_write);
        TextView mPropertyNotifiable = (TextView)convertView.findViewById(R.id.property_notify);
        TextView mPropertyNone = (TextView)convertView.findViewById(R.id.property_none);

        // display the UUID of the characteristic
        uuidTV.setText(characteristic.getUuid().toString());

        // Display the read/write/notify attributes of the Characteristic
        if (BlePeripheral.isCharacteristicReadable(characteristic)) {
            mPropertyReadable.setVisibility(View.VISIBLE);
        } else {
            mPropertyReadable.setVisibility(View.GONE);
        }
        if (BlePeripheral.isCharacteristicWritable(characteristic)) {
            mPropertyWritable.setVisibility(View.VISIBLE);
        } else {
            mPropertyWritable.setVisibility(View.GONE);
        }
        if (BlePeripheral.isCharacteristicNotifiable(characteristic)) {
            mPropertyNotifiable.setVisibility(View.VISIBLE);
        } else {
            mPropertyNotifiable.setVisibility(View.GONE);
        }
        if (!BlePeripheral.isCharacteristicNotifiable(characteristic) &&
                !BlePeripheral.isCharacteristicWritable(characteristic) &&
                !BlePeripheral.isCharacteristicReadable(characteristic)) {
            mPropertyNone.setVisibility(View.VISIBLE);
        } else {
            mPropertyNone.setVisibility(View.GONE);

        }


        return convertView;
    }

}