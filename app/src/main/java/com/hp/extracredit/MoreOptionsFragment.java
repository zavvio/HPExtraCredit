package com.hp.extracredit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.print.PrintJobInfo;
import android.printservice.PrintService;
import android.text.TextUtils;
import android.util.Log;

import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseBinding;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.ImpulseDeviceOptions;
import com.hp.impulselib.ImpulseDeviceState;
import com.hp.impulselib.TrackListener;
import com.hp.impulselib.util.Bytes;
import com.hp.impulselib.util.Maps;
import com.pavelsikun.seekbarpreference.SeekBarPreference;

import java.util.HashMap;
import java.util.Map;

public class MoreOptionsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_TAG = "ImpulsePreferences";

    @SuppressLint("UseSparseArrays")
    private final static Map<Integer, String> AutoPowerOffValues = new HashMap<Integer, String>() {{
        put(0x00, "0"); // None
        put(0x04, "3"); // 3 minutes
        put(0x08, "5"); // 5 minutes
        put(0x0C, "10"); // 10 minutes
    }};

    @SuppressLint("UseSparseArrays")
    private final static Map<Integer, String> PrintModeValues = new HashMap<Integer, String>() {{
        put(0x01, "1"); // Page Full
        put(0x02, "2"); // Image Full
    }};

    private ImpulseDevice mDevice;
    private ImpulseBinding mImpulse;
    private Preference mDeviceStatusPref, mBatteryPref, mHardwareVersionPref, mTotalPrintsPref;
    private ListPreference mAutoPowerOffPref, mPrintModePref;
    private SwitchPreference mAutoExposurePref;
    private SeekBarPreference mScalePref;
    private PreferenceCategory mDeviceSettingsPref, mDeviceInfoPref;
    private AutoCloseable mTracking;
    private Context mContext;
    private boolean mChanging = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        addPreferencesFromResource(R.xml.more_options);

        // And do some stuff now
        // Get information about the print job and selected printer from intent.
        PrintJobInfo printJobInfo = getActivity().getIntent()
                .getParcelableExtra(PrintService.EXTRA_PRINT_JOB_INFO);
        //noinspection ConstantConditions
        mDevice = Impulse.toDevice(printJobInfo.getPrinterId().getLocalId());

        mImpulse = Impulse.bind(getActivity());

        mDeviceSettingsPref = (PreferenceCategory) findPreference(getString(R.string.device_settings_key));
        mDeviceInfoPref = (PreferenceCategory) findPreference(getString(R.string.device_info_key));

        mDeviceStatusPref = findPreference(getString(R.string.device_status_key));
        mDeviceStatusPref.setTitle(getString(R.string.device_status, getString(R.string.unknown)));

        mBatteryPref = findPreference(getString(R.string.battery_key));
        mHardwareVersionPref = findPreference(getString(R.string.hardware_version_key));
        mAutoPowerOffPref = (ListPreference)findPreference(getString(R.string.auto_power_off_key));
        mAutoExposurePref = (SwitchPreference)findPreference(getString(R.string.auto_exposure_key));
        mPrintModePref = (ListPreference)findPreference(getString(R.string.print_mode_key));
        mTotalPrintsPref = findPreference(getString(R.string.total_prints_key));

        mScalePref = (SeekBarPreference) findPreference(getString(R.string.scale_image_key));

        mDeviceStatusPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getDeviceStatus();
                return true;
            }
        });

        getDeviceStatus();

        registerListeners();
    }


    void registerListeners() {
        // This does not work, remove when https://github.com/MrBIMC/MaterialSeekBarPreference/issues/27
        // is closed
        mScalePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(LOG_TAG, "New mScalePref value " + newValue);
                updateJobOptions();
                return true;
            }
        });

        // See above. When this bug isn't around any more, we can remove this
        // (and the corresponding unregister in onDestroy)
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        mAutoExposurePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // If it was us doing the changing, ignore.
                if (mChanging) return true;

                Boolean value = (Boolean) newValue;
                ImpulseDeviceOptions options = new ImpulseDeviceOptions.Builder()
                        .setAutoExposure(value ? 0x01 : 0x00)
                        .build();

                mDeviceSettingsPref.setEnabled(false);
                setOptions(options);
                return true;
            }
        });

        mAutoPowerOffPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Ignore if we are setting this from device
                if (mChanging) return true;

                String value = (String)newValue;
                ImpulseDeviceOptions options = new ImpulseDeviceOptions.Builder()
                        .setAutoPowerOff(Maps.inverse(AutoPowerOffValues).get(value))
                        .build();
                mDeviceSettingsPref.setEnabled(false);
                setOptions(options);
                return true;
            }
        });

        mPrintModePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Ignore if we are setting this from device
                if (mChanging) return true;

                String value = (String)newValue;
                ImpulseDeviceOptions options = new ImpulseDeviceOptions.Builder()
                        .setPrintMode(Maps.inverse(PrintModeValues).get(value))
                        .build();
                mDeviceSettingsPref.setEnabled(false);
                setOptions(options);
                return true;
            }
        });
    }

    private void updateJobOptions() {
        JobOptions options = new JobOptions.Builder()
                .setScale(mScalePref.getCurrentValue())
                .build();
        ((MoreOptionsActivity)getActivity()).onJobOptionsChanged(options);
    }

    private void stopTracking() {
        if (mTracking == null) return;
        try {
            mTracking.close();
        } catch (Exception ignore) {
        }
        mTracking = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        mImpulse.close();
    }

    private void getDeviceStatus() {
        Log.d(LOG_TAG, "getDeviceStatus()");
        stopTracking();

        // Disable the whole category while we are getting status
        mDeviceSettingsPref.setEnabled(false);
        mDeviceInfoPref.setEnabled(false);

        mTracking = mImpulse.track(mDevice, new TrackListener() {
            @Override
            public void onState(ImpulseDeviceState state) {
                updateState(state);
                stopTracking();
            }

            @Override
            public void onError(int errorCode) {
                badState(errorCode);
                mTracking = null;
            }
        });
    }

    private void badState(int errorCode) {
        Log.d(LOG_TAG, "badState() " + errorCode);
        mDeviceSettingsPref.setEnabled(true);
        mDeviceStatusPref.setTitle(getString(R.string.device_status, ImpulsePrintService.getErrorString(mContext, errorCode)));
        // Disable everything else because it's old/unknown. But keep device status.
    }

    private void updateState(ImpulseDeviceState state) {
        Log.d(LOG_TAG, "updateState() " + state);
        ImpulseDeviceState.AccessoryInfo info = state.getInfo();

        mChanging = true;

        mDeviceSettingsPref.setEnabled(true);
        mDeviceStatusPref.setTitle(getString(R.string.device_status, ImpulsePrintService.getErrorString(mContext, state.getError())));

        // Settings (things the user could change)
        String autoPowerOffValue = AutoPowerOffValues.get(info.autoPowerOff);
        if (TextUtils.isEmpty(autoPowerOffValue)) {
            mAutoPowerOffPref.setSummary(getString(R.string.unknown));
        } else {
            mAutoPowerOffPref.setValue(autoPowerOffValue);
            mAutoPowerOffPref.setSummary(mAutoPowerOffPref.getEntry());
        }

        mAutoExposurePref.setChecked(info.autoExposure == 0x01);

        String printModeValue = PrintModeValues.get(info.printMode);
        if (TextUtils.isEmpty(printModeValue)) {
            mPrintModePref.setSummary(getString(R.string.unknown));
        } else {
            mPrintModePref.setValue(printModeValue);
            mPrintModePref.setSummary(mPrintModePref.getEntry());
        }

        // Device info (things the user cannot change)
        mDeviceInfoPref.setEnabled(true);

        mBatteryPref.setTitle(getString(R.string.battery_level, info.batteryStatus));
        mHardwareVersionPref.setSummary(getString(R.string.hardware_versions_summary,
                Bytes.toHex(info.firmwareVersion),
                Bytes.toHex(info.hardwareVersion)));
        mTotalPrintsPref.setTitle(getString(R.string.total_prints, info.totalPrints));

        mChanging = false;
    }

    public void setOptions(ImpulseDeviceOptions options) {
        // Disable the whole category while we are getting status
        mDeviceSettingsPref.setEnabled(false);

        mImpulse.setOptions(mDevice, options, new TrackListener() {
            @Override
            public void onState(ImpulseDeviceState state) {
                updateState(state);
            }

            @Override
            public void onError(int errorCode) {
                stopTracking();
                badState(errorCode);
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(key, mScalePref.getKey())) {
            updateJobOptions();
        }
    }
}
