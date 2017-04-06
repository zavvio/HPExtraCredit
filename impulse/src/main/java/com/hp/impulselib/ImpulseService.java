package com.hp.impulselib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.hp.impulselib.bt.ImpulseClient;
import com.hp.impulselib.util.BoundServiceConnection;
import com.hp.impulselib.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A service to manage access to Impulse devices.
 */
public class ImpulseService extends Service {
    private static final String LOG_TAG = "ImpulseService";
    private static final int TIMEOUT_IN_MILSECOND = 1000;

    private List<DiscoverListener> mDiscoverListeners = new ArrayList<>();
    private BroadcastReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private Map<String, ImpulseDevice> mDevices = new HashMap<>();
    private Map<ImpulseDevice, TrackInfo> mTrackInfos = new HashMap<>();
    private List<ImpulseJob> mJobs = new ArrayList<>();
    private List<BluetoothDevice> mUuidQueue = new ArrayList<>();
    private boolean printing = false;

    private class TrackInfo {
        List<TrackListener> listeners = new ArrayList<>();
        ImpulseClient client;
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate()");
        init();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind() " + intent);
        return new BoundServiceConnection.Binder<ImpulseService>() {
            @Override
            protected ImpulseService getService() {
                return ImpulseService.this;
            }
        };
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind() " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mBluetoothAdapter = null;
            mDevices.clear();
            mReceiver = null;
        }
    }

    /** Set everything up */
    int init() {
        // Already done?
        if (mBluetoothAdapter != null) return Impulse.ErrorNone;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /**
         * If the device does not have bluetooth interface, mBluetoothAdapter will be
         * null. This code will allow us to run our code in devices where there is no
         * bluetooth. E.g. emulators
         */

        if (mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "ERROR: Bluetooth not found",
                    Toast.LENGTH_SHORT).show();
            return Impulse.ErrorBluetoothNotPresent;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter = null;
            return Impulse.ErrorBluetoothDisabled;
        }

        // Preload bonded devices
        Set<BluetoothDevice> bonded = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice: bonded) {
            ImpulseDevice device = new ImpulseDevice.Builder(bluetoothDevice)
                                .setBonded(true)
                                .build();
                   if (ImpulseDevice.isImpulseClass(bluetoothDevice)) {
                       Log.d(LOG_TAG, "Preload bonded devices: " + device.getAddress() + ", rssi: " + device.getRssi());
                       mDevices.put(device.getAddress(), device);
                    }
                }

        mReceiver = new ImpulseBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(mReceiver, filter);

        return Impulse.ErrorNone;
    }

    /** Start continuous discovery of devices, reporting them to the callback */
    void discover(DiscoverListener discoverListener) {
        Log.d(LOG_TAG, "discover() " + discoverListener);
        // Signal back the current device set
        discoverListener.onDevices(
                Collections.unmodifiableList(new ArrayList<>(mDevices.values())));
        mDiscoverListeners.add(discoverListener);
        updateDiscovery();
    }

    /** Stop discovery of devices for the specified callback */
    void stopDiscovery(DiscoverListener discoverListener) {
        Log.d(LOG_TAG, "stopDiscovery()");
        mDiscoverListeners.remove(discoverListener);
        updateDiscovery();
    }

    private void updateDiscovery() {
        // Could be null if shutting down or if Bluetooth is disabled
        if (mBluetoothAdapter == null) return;

        if (mBluetoothAdapter.isDiscovering() &&
            (mDiscoverListeners.isEmpty() || !canDiscover())) {
            /**
             * We will not cancel discovery EVER!
             * this will be dead when the class is down.
             */
            Log.d(LOG_TAG, "Stopping discovery");
            mBluetoothAdapter.cancelDiscovery();
        } else if (!mBluetoothAdapter.isDiscovering() && !mDiscoverListeners.isEmpty() && canDiscover()) {
            Log.d(LOG_TAG, "Starting discovery");
            boolean result = mBluetoothAdapter.startDiscovery();
            if (!result) {
                Log.w(LOG_TAG, "Attempt to launch bluetooth discovery failed");
                // TODO: Consider if this happens to keep listeners around and try again later if Bluetooth becomes enabled.
                for (DiscoverListener listener: mDiscoverListeners) {
                    listener.onError(Impulse.ErrorBluetoothDiscovery);
                }
                mDiscoverListeners.clear();
            }
        }
    }

    /** Used by printing clients to indicate printing
     *  is occuring and discovery should halt
     */
    public void setPrintingStatus(boolean printing){
        Log.d(LOG_TAG, "Setting is printing to "+ printing);
        this.printing = printing;
        updateDiscovery();
    }

    /** If printing using OPP or SPP , do not discover */
    private boolean canDiscover() {
        return mTrackInfos.isEmpty() && !printing;
    }

    public void track(final ImpulseDevice device, TrackListener listener) {
        Log.d(LOG_TAG, "track() " + device);
        TrackInfo info = mTrackInfos.get(device);
        if (info == null) {
            info = new TrackInfo();
            final TrackInfo trackInfo = info;
            mTrackInfos.put(device, info);
            info.client = new ImpulseClient(device, new ImpulseClient.ImpulseListener() {
                @Override
                public void onInfo(ImpulseDeviceState info) {
                    for (TrackListener listener : new ArrayList<>(trackInfo.listeners)) {
                        listener.onState(info);
                    }
                }

                @Override
                public void onError(IOException e) {
                    for (TrackListener listener : new ArrayList<>(trackInfo.listeners)) {
                        listener.onError(Impulse.ErrorConnectionFailed);
                    }
                    mTrackInfos.remove(device);
                }

                @Override
                public void onError(int errorCode) {
                    for (TrackListener listener : new ArrayList<>(trackInfo.listeners)) {
                        listener.onError(errorCode);
                    }
                    mTrackInfos.remove(device);
                }
            });
        }
        info.listeners.add(listener);
        updateDiscovery();
    }


    /**
     * Set options to the device. TrackListener will be called once with results and the operation
     * will be considered complete.
     */
    void setOptions(final ImpulseDevice device, final ImpulseDeviceOptions options, final TrackListener listener) {
        // Track
        track(device, new TrackListener() {
            boolean mSent = false;
            @Override
            public void onError(int errorCode) {
                listener.onError(errorCode);
            }

            @Override
            public void onState(ImpulseDeviceState info) {
                if (!mSent) {
                    // Read any missing state
                    ImpulseDeviceOptions.Builder builder = new ImpulseDeviceOptions.Builder(options);
                    if (options.getAutoExposure() == null) {
                        builder.setAutoExposure(info.getInfo().autoExposure);
                    }
                    if (options.getAutoPowerOff() == null) {
                        builder.setAutoPowerOff(info.getInfo().autoPowerOff);
                    }
                    if (options.getPrintMode() == null) {
                        builder.setPrintMode(info.getInfo().printMode);
                    }
                    mTrackInfos.get(device).client.setAccessoryInfo(builder.build());
                    mSent = true;
                } else {
                    if (info.getCommand() == ImpulseClient.CommandAccessoryInfo) {
                        listener.onState(info);
                        untrack(device, this);
                    } else {
                        Log.d(LOG_TAG, "Got command " + Bytes.toHex(info.getCommand()) + " so waiting...");
                    }
                }
            }
        });
    }

    public void untrack(ImpulseDevice device, TrackListener listener) {
        Log.d(LOG_TAG, "untrack() " + device);
        TrackInfo info = mTrackInfos.get(device);
        if (info != null) {
            info.listeners.remove(listener);
            if (info.listeners.size() == 0) {
                mTrackInfos.remove(device);
                info.client.close();
            }
        }
        updateDiscovery();
    }

    ImpulseJob send(ImpulseDevice device, Bitmap bitmap, final SendListener listener) {
        Log.d(LOG_TAG, "sendBitmap() width=" + bitmap.getWidth() + " height=" + bitmap.getHeight());

        ImpulseJob job = new ImpulseJob(this, device, bitmap, listener);
        mJobs.add(job);
        job.start();
        return job;
    }

    void onJobEnd(ImpulseJob job) {
        mJobs.remove(job);
    }

    private class ImpulseBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, action);
            if (TextUtils.equals(action, BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(LOG_TAG, "device: " + device.getName());
                if (device != null && ImpulseDevice.isImpulseClass(device)) {
                    ImpulseDevice impulseDevice = new ImpulseDevice.Builder(intent)
                            .setBonded(mBluetoothAdapter.getBondedDevices())
                            .build();
                    impulseDevice.checkIfConnected(TIMEOUT_IN_MILSECOND);
                    discoverDevice(impulseDevice);
                }
            } else if (TextUtils.equals(action, BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Log.d(LOG_TAG, action + " device=" + device + " bondState=" + bondState);
                reloadBonded();
            } else if (TextUtils.equals(action, BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ImpulseDevice.isImpulseClass(device)) {
                    Log.d(LOG_TAG, "Observed pairing request for an Impulse device; approving");
                    try {
                        device.setPairingConfirmation(true);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "caught exception for setPairingConfirmation");
                        e.printStackTrace();
                    }
                }
            } else if (TextUtils.equals(action, BluetoothDevice.ACTION_ACL_CONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ImpulseDevice.isImpulseClass(device) && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    boolean bonding = device.createBond();
                    Log.d(LOG_TAG, "creating bond started " + bonding);
                }
            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                updateDiscovery();
            }
        }
    }

    private void reloadBonded() {
        Set<BluetoothDevice> allBonded = mBluetoothAdapter.getBondedDevices();

        // Build a hashset of bonded addresses
        Set<String> bondedAddresses = new HashSet<>();
        for (BluetoothDevice bondedDevice: allBonded) {
            bondedAddresses.add(bondedDevice.getAddress());
        }

        for (ImpulseDevice device: new ArrayList<>(mDevices.values())) {
            boolean currentlyBonded = bondedAddresses.contains(device.getAddress());
            if (device.getBonded() != currentlyBonded) {
                // Bonding state has changed so update
                ImpulseDevice updated = new ImpulseDevice.Builder(device)
                        .setBonded(currentlyBonded)
                        .build();
                discoverDevice(updated);
            }
        }
    }

    private void discoverDevice(ImpulseDevice device) {
        mDevices.put(device.getAddress(), device);
        notifyDevices();
    }

    private void notifyDevices() {
        List<ImpulseDevice> devices = Collections.unmodifiableList(new ArrayList<>(mDevices.values()));
        for (DiscoverListener listener: mDiscoverListeners) {
            Log.d(LOG_TAG, "Notifying listener about new devices: " + listener);
            listener.onDevices(devices);
        }
    }
}
