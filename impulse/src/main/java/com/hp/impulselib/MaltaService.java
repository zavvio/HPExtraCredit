package com.hp.impulselib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.hp.impulselib.util.ScanRecord;

/**
 * A service to manage access to Impulse devices.
 */
public class MaltaService extends Service {
    private static final String LOG_TAG = "MaltaService";
    public final static String ACTION_LE_DEVICE_FOUND = "com.hp.impulse.ACTION_LE_DEVICE_FOUND";
    public final static String ACTION_LE_SCANNING_STOPPED = "com.hp.impulse.ACTION_LE_DEVICE_SCANNING_STOPPED";
    public final static String EXTRA_DEVICE_TAG = "device";

    public final static String EXTRA_DEVICE_COLOR_TAG = "color";
    public final static String EXTRA_PRINTER_STATUS_TAG = "printer_status";
    private static int DEFAULT_SCAN_PERIOD = 10000;
    private static int HP_IDENTIFIER = 0x65;
    // Stops scanning after 10 seconds
    private long scanPeriod = DEFAULT_SCAN_PERIOD;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean scanning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate()");
        init();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind() " + intent);
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");

    }
    public void setScanPeriod(int milliseconds){
        if(milliseconds > 0)
            scanPeriod = milliseconds;
    }

    public class LocalBinder extends Binder {
       public MaltaService getService() {
            return MaltaService.this;
        }
    }

    /** Set everything up */
    public int init() {
        // Already done?
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return Impulse.ErrorBluetoothLENotSupported;
        }
        if (mBluetoothAdapter != null) return Impulse.ErrorNone;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /**
         * If the device does not have bluetooth interface, mBluetoothAdapter will be
         * null. This code will allow us to run our code in devices where there is no
         * bluetooth. E.g. emulators
         */
        if (mBluetoothAdapter == null){
            return Impulse.ErrorBluetoothNotPresent;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter = null;
            return Impulse.ErrorBluetoothDisabled;
        }

        return Impulse.ErrorNone;
    }

    public void startScan(){
        scanLeDevice(true);
    }

    public void stopScan(){
        scanLeDevice(false);
    }

    public boolean isScanning(){
        return scanning;
    }

    private void scanLeDevice(final boolean enable) {
        Handler handler = new Handler();
        if(enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    mBluetoothAdapter.stopLeScan(leScanCallback);
                    broadcastScanUpdate(ACTION_LE_SCANNING_STOPPED);
                }
            }, scanPeriod);

            scanning = true;
            mBluetoothAdapter.startLeScan(leScanCallback);
        } else {
            scanning = false;
            mBluetoothAdapter.stopLeScan(leScanCallback);
            broadcastScanUpdate(ACTION_LE_SCANNING_STOPPED);
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi,
                                 byte[] scanRecord) {
                ScanRecord scanRecordObject = null;
                byte[] manufacturerData;
                int i = 0;

                if (isMaltaDevice(device))  {
                    ImpulseDevice impulseDevice = new ImpulseDevice.Builder(device).build();
                    Log.d(LOG_TAG, device.getName() + " " + device.getAddress());
//                    scanRecordObject = ScanRecord.parseFromBytes(scanRecord);
//                    if (scanRecordObject != null) {
//                        manufacturerData = scanRecordObject.getManufacturerSpecificData(HP_IDENTIFIER);
//                        if (manufacturerData != null && manufacturerData.length >= 3) {
//                            impulseDevice.setCalibratedRssi(manufacturerData[i++]);
//                            impulseDevice.setColor(manufacturerData[i++]);
//                            impulseDevice.setPrinterStatus(manufacturerData[i++]);
//                        }
//                    }
                    broadcastUpdate(ACTION_LE_DEVICE_FOUND, impulseDevice);
                }

            }
        };

    private boolean isMaltaDevice(BluetoothDevice bluetoothDevice) {
            return bluetoothDevice != null
                    && bluetoothDevice.getName() != null
                    && (bluetoothDevice.getName().toLowerCase().startsWith(ImpulseDevice.HP_LE_DEVICE_NAME.toLowerCase())
                    || bluetoothDevice.getName().equalsIgnoreCase(ImpulseDevice.HP_MALTA_NAME))
                    ;
    }

    private void broadcastScanUpdate(final String action){
        final Intent intent = new Intent(action);
        intent.setAction(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final ImpulseDevice device) {
        final Intent intent = new Intent(action);
        intent.setAction(action);
        intent.putExtra(EXTRA_DEVICE_TAG, device);
        sendBroadcast(intent);
    }


}
