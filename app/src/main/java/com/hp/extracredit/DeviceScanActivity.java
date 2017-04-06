package com.hp.extracredit;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.MaltaService;

import java.util.ArrayList;

import static com.hp.impulselib.MaltaService.EXTRA_DEVICE_TAG;

public class DeviceScanActivity extends ListActivity {
    private BluetoothAdapter bluetoothAdapter;

    private LeDeviceListAdapter leDeviceListAdapter;

    private static final String UNKNOWN_DEVICE = "unknown device";
    private static final String UNNAMED_MALTA_DEVICE = "unnamed malta device";

    private static final String BLUETOOTH_LE_NOT_SUPPORT = "bluetooth le is not supported";
    private static final int REQUEST_ENABLE_BT = 1;

    private MaltaService maltaService;

    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            maltaService = ((MaltaService.LocalBinder)service).getService();
            if (maltaService.init()!= Impulse.ErrorNone) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            scanForDevices(true);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            maltaService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_LE_DEVICE_FOUND: BT LE device found.
    // ACTION_LE_SCANNING_STOPPED: BT LE scanning stopped(currently scans for 10 secs)
    private final BroadcastReceiver maltaUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MaltaService.ACTION_LE_DEVICE_FOUND.equals(action)) {
                ImpulseDevice impulseDevice = intent.getParcelableExtra(EXTRA_DEVICE_TAG);
                leDeviceListAdapter.addDevice(impulseDevice);
                leDeviceListAdapter.notifyDataSetChanged();
            }
            else if(MaltaService.ACTION_LE_SCANNING_STOPPED.equals(action)){
                invalidateOptionsMenu();
            }
        }
    };

    private static IntentFilter makeMaltaUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MaltaService.ACTION_LE_DEVICE_FOUND);
        intentFilter.addAction(MaltaService.ACTION_LE_SCANNING_STOPPED);
        return intentFilter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, BLUETOOTH_LE_NOT_SUPPORT, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "bluetooth is not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Start and bind to BTLE service
        Intent maltaServiceIntent = new Intent(this, MaltaService.class);
        bindService(maltaServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        registerReceiver(maltaUpdateReceiver, makeMaltaUpdateIntentFilter());
        // Initializes list view adapter.
        leDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(leDeviceListAdapter);
        scanForDevices(true);

    }


    @Override
    protected void onPause() {
        super.onPause();
        scanForDevices(false);
        leDeviceListAdapter.clear();
        unregisterReceiver(maltaUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        maltaService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_device_scan, menu);
        if (maltaService == null || !maltaService.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                leDeviceListAdapter.clear();
                scanForDevices(true);
                break;
            case R.id.menu_stop:
                scanForDevices(false);
                break;
        }
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ImpulseDevice device = leDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent();
        intent.putExtra(PrintActivity.DEVICE_KEY, device);
        setResult(RESULT_OK, intent);
        scanForDevices(false);
        finish();
    }


    private void scanForDevices(boolean enable){
        if(maltaService == null)
            return;
        else if(enable){
            maltaService.startScan();
        }
        else{
            maltaService.stopScan();
        }
        invalidateOptionsMenu();
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<ImpulseDevice> leDevices;
        private LayoutInflater inflator;

        public LeDeviceListAdapter() {
            super();
            leDevices = new ArrayList<ImpulseDevice>();
            inflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(ImpulseDevice device) {
            for (int i = 0; i < leDevices.size(); i++){
                if ( leDevices.get(i).getDevice().getAddress().equals(device.getAddress()) )
                    return;
            }
            leDevices.add(device);
        }

        public ImpulseDevice getDevice(int position) {
            return leDevices.get(position);
        }

        public void clear() {
            leDevices.clear();
        }

        @Override
        public int getCount() {
            return leDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return leDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = inflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ImpulseDevice device = leDevices.get(i);
            final String deviceName = device.getDevice().getName().replace(ImpulseDevice.HP_LE_DEVICE_NAME, "");
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else if (device.getDevice().getName().toLowerCase().startsWith(ImpulseDevice.HP_LE_DEVICE_NAME.toLowerCase()))
                viewHolder.deviceName.setText(UNNAMED_MALTA_DEVICE);
            else
                viewHolder.deviceName.setText(UNKNOWN_DEVICE);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }


}


