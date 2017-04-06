package com.hp.extracredit;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.MaltaGATTAttributes;
import com.hp.impulselib.MaltaGATTService;
import com.hp.impulselib.MaltaService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class LeDeviceControlActivity extends Activity {
    private final static String TAG = LeDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView connectionState;
    private TextView dataField;
    private EditText editDataField;
    private Button changeDataButton;
    private String deviceName;
    private String deviceAddress;
    private String deviceColor;
    private String printerStatus;
    private ExpandableListView gattServicesList;
    private MaltaGATTService maltaGATTService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> gattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean connected = false;
    private BluetoothGattCharacteristic notifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            maltaGATTService = ((MaltaGATTService.LocalBinder) service).getService();
            if (!maltaGATTService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            maltaGATTService.connect(deviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            maltaGATTService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MaltaGATTService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (MaltaGATTService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (MaltaGATTService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(maltaGATTService.getSupportedGattServices());
            } else if (MaltaGATTService.ACTION_DATA_AVAILABLE.equals(action)) {
                String value = intent.getStringExtra(MaltaGATTService.CHARACTERISTIC_VALUE);
                boolean writeable = intent.getBooleanExtra(MaltaGATTService.CHARACTERISTIC_PROPERTY_WRITEABLE, false);
                displayData(value, writeable);
            } else if (MaltaGATTService.ACTION_WRITE_FAILED.equals(action)) {
                Toast.makeText(context, "Failed to write to characteristic.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {

                    if (gattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                gattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (notifyCharacteristic != null) {
                                maltaGATTService.setCharacteristicNotification(
                                        notifyCharacteristic, false);
                                notifyCharacteristic = null;
                            }
                            maltaGATTService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            notifyCharacteristic = characteristic;
                            maltaGATTService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        gattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        displayData(getString(R.string.no_data), false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        ImpulseDevice impulseDevice = intent.getParcelableExtra(MaltaService.EXTRA_DEVICE_TAG);
        deviceName = impulseDevice.getDevice().getName();
        deviceAddress = impulseDevice.getDevice().getAddress();
        deviceColor = impulseDevice.getColor().toString();
        printerStatus = impulseDevice.getPrinterStatus().toString();

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(deviceAddress);
        ((TextView) findViewById(R.id.device_color)).setText(deviceColor);
        ((TextView) findViewById(R.id.printer_status)).setText(printerStatus);
        gattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        gattServicesList.setOnChildClickListener(servicesListClickListner);
        connectionState = (TextView) findViewById(R.id.connection_state);
        dataField = (TextView) findViewById(R.id.data_value);
        editDataField = (EditText) findViewById(R.id.edit_data_value);
        changeDataButton = (Button) findViewById(R.id.change_button);

        getActionBar().setTitle(getString(R.string.device_info));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, MaltaGATTService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        changeDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String btnString = ((Button) v).getText().toString();
                if ( btnString.equals(getString(R.string.change)) ) {
                    if (dataField != null) dataField.setVisibility(View.GONE);
                    if (editDataField != null) editDataField.setVisibility(View.VISIBLE);
                    if (changeDataButton != null) changeDataButton.setText(getString(R.string.done));
                } else if ( btnString.equals(getString(R.string.done)) ) {
                    //theres a malta defect where you need to set name twice before ble invokes the
                    //write callback.
                    maltaGATTService.setName(editDataField.getText().toString());
                 }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (maltaGATTService != null) {
            final boolean result = maltaGATTService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        maltaGATTService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (connected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                maltaGATTService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                maltaGATTService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data, boolean writeable) {
        if (data == null) return;

        dataField.setVisibility(View.VISIBLE);
        dataField.setText(data);
        editDataField.setVisibility(View.GONE);
        editDataField.setText("");
        changeDataButton.setVisibility(writeable? View.VISIBLE : View.GONE);
        changeDataButton.setText(getString(R.string.change));
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        gattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, MaltaGATTAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                //maltaGATTService
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, MaltaGATTAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            this.gattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        gattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MaltaGATTService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MaltaGATTService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(MaltaGATTService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(MaltaGATTService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(MaltaGATTService.ACTION_WRITE_FAILED);
        return intentFilter;
    }
}
