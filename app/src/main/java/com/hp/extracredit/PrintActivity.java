package com.hp.extracredit;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.impulselib.DiscoverListener;
import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseBinding;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.ImpulseDeviceState;
import com.hp.impulselib.MaltaService;
import com.hp.impulselib.OperationListener;
import com.hp.impulselib.SendListener;
import com.hp.impulselib.TrackListener;
import com.hp.impulselib.bt.ImpulseBulkTransferClient;
import com.hp.impulselib.bt.ImpulsePrintClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintActivity extends AppCompatActivity {

    private static final String LOG_TAG = "PrintActivity";
    private static int PRINT_MODE_DEFAULT_PRINTER_SETTING = 0;
    private static int PRINT_MODE_PAPER_FULL = 1;
    private static int PRINT_MODE_IMAGE_FULL = 2;

    private static int SELECT_DEVICE_REQUEST = 1000;
    public static String DEVICE_KEY = "DEVICE";

    private static int REQUEST_ID_PERMISSIONS = 1;
    String[] permissions = new String[]{
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.ACCESS_COARSE_LOCATION"
    };

    private ImpulseBinding mImpulse;
    private List<ImpulseDevice> mDevices = new ArrayList<>();
    private ImpulseDevice mTrackingDevice;
    private AutoCloseable autoCloseable;
    private TextView viewTrackingDevice;
    private TextView versionTextView;
    private ProgressBar progressBar;
    private ImageView mImageView;

    private Button getBleInfoButton;

    private String fileName = "";

    private TextView file;


    private final static Map<Integer, String> AutoPowerOffValues = new HashMap<Integer, String>() {{
        put(0x00, "None");
        put(0x04, "3 minutes");
        put(0x08, "5 minutes");
        put(0x0C, "10 minutes");
    }};

    private final static Map<Integer, String> PrintModeValues = new HashMap<Integer, String>() {{
        put(0x01, "Page Full");
        put(0x02, "Image Full");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
       // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // request permissin explicityly
        ActivityCompat.requestPermissions(this, permissions, REQUEST_ID_PERMISSIONS);

        /*Button btnGoToSettings = (Button) findViewById(R.id.btn_gotosettings);
        btnGoToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintActivity.this.startActivityForResult(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), 0);
            }
        });*/


        this.progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mImpulse = Impulse.bind(this);
        viewTrackingDevice = (TextView) findViewById(R.id.view_selected_device);
        // versionTextView = (TextView) findViewById(R.id.view_device_state);
        getBleInfoButton = (Button) findViewById(R.id.ble_device_info_button);

        Utility.createRootDirectory();
        mImageView = (ImageView) findViewById(R.id.imageViewPreview);

        Intent i= getIntent();
        fileName = i.getStringExtra("filename");

        file= (TextView) findViewById(R.id.editText_filename);
        file.setText(fileName);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_DEVICE_REQUEST && resultCode == RESULT_OK) {
            mTrackingDevice = data.getExtras().getParcelable(DEVICE_KEY);
            getBleInfoButton.setVisibility(View.VISIBLE);
        }
    }

    public void onClickGetBLEDeviceInfo(View view) {
        final Intent intent = new Intent(this, LeDeviceControlActivity.class);
        intent.putExtra(MaltaService.EXTRA_DEVICE_TAG, mTrackingDevice);
        startActivity(intent);
    }

    public void onClickDisplayDevice(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Device List");

        ListView modeList = new ListView(this);
        String[] stringArray = new String[mDevices.size()];
        Log.d(LOG_TAG, "device size: " + mDevices.size());

        for (int i = 0; i < mDevices.size(); i++) {
            String connected = (mDevices.get(i).isConnected ? "ON" : "OFF");
            stringArray[i] = mDevices.get(i).getDevice().getName() + " : " + connected;
        }

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
        modeList.setAdapter(modeAdapter);
        modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mTrackingDevice = mDevices.get(position);
                viewTrackingDevice.setText(mTrackingDevice.getDevice().getName() + " selected");
            }
        });
        builder.setView(modeList);
        builder.setCancelable(true);
        builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        final Dialog dialog = builder.create();

        dialog.show();
    }

    /*public void onClickDisplayVersion(View view) {
        if (mTrackingDevice == null) {
            Toast.makeText(this, "No Device Selected", Toast.LENGTH_LONG).show();
            return;
        }
        getDeviceStatus(mTrackingDevice);
    }

    public void onClickFirmwareUpdate(View view) {
        if (mTrackingDevice == null) {
            Toast.makeText(this, "No device selected", Toast.LENGTH_LONG).show();
            return;
        }
        sendFirmwareUpdate(mTrackingDevice);
    }

    public void onClickOPP(View view) {

        if (mTrackingDevice == null) {
            snack("No device found");
        } else {
            put(mTrackingDevice);
        }
    }*/
    public void onClickSPP(View view) {


        final Activity thisActivity = this;
        int printCount = getPrintCount();
        int printMode = getPrintMode();
        int post = getPost();

        if (mTrackingDevice == null)  {
            snack("No device found");
            return;
        }

        setPrintStatus(true);

        EditText editText = (EditText) findViewById(R.id.editText_filename);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.seaturtle_sample_6str_96wpi_96ppi);
        bmp = BitmapFactory.decodeFile(editText.getText().toString());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);

        Utility.displayPic(mImageView, editText.getText().toString());

        ImpulsePrintClient impulsePrintClient = new ImpulsePrintClient.Builder(mTrackingDevice, byteArrayOutputStream.toByteArray(), new com.hp.impulselib.SendListenerSPP() {

            @Override
            public void onError(int errorCode) {
                Log.d(LOG_TAG, "onClickImpulsePrint: onError");
                Toast.makeText(thisActivity, ImpulsePrintService.getErrorString(thisActivity, errorCode), Toast.LENGTH_LONG).show();
                setPrintStatus(false);
            }

            @Override
            public void onProgress(int total, int sent, boolean completed ) {
                snack("Sent " + sent + " of " + total);
                Log.d(LOG_TAG, "onClickImpulsePrint: onProgress");
            }

            @Override
            public void onDone() {
                Log.d(LOG_TAG, "onClickImpulsePrint: onDone");
                Toast.makeText(thisActivity, "SPP Printing Done", Toast.LENGTH_LONG).show();
                setPrintStatus(false);
            }

        }, mImpulse)
                .setPostCount(post)
                .setPrintCount(printCount)
                .setPrintMode(printMode)
                .build();
        //TODO:
        impulsePrintClient.print();
        bmp.recycle();

    }
    /*private void put(ImpulseDevice device) {
        final Activity thisActivity = this;
        if (device == null) {
            Toast.makeText(this, "No Device Selected", Toast.LENGTH_LONG).show();
            return;
        }
        setPrintStatus(true);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.seaturtle_sample_6str_96wpi_96ppi);
        mImpulse.send(device, bmp, new SendListener() {
            @Override
            public void onProgress(int total, int sent) {
                snack("Sent " + sent + " of " + total);
            }

            @Override
            public void onDone() {
                Toast.makeText(thisActivity, "OPP Printing Done", Toast.LENGTH_LONG).show();
                setPrintStatus(false);
            }

            @Override
            public void onError(int code) {
                Toast.makeText(thisActivity, ImpulsePrintService.getErrorString(thisActivity, code), Toast.LENGTH_LONG).show();
                setPrintStatus(false);
            }
        });

        bmp.recycle();
    }*/

    public void onClickFindBLEDevices(View view) {
        Toast.makeText(getApplicationContext(), "onClickFindBLEDevices", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
        startActivityForResult(intent, SELECT_DEVICE_REQUEST);
    }

    public void getDeviceStatus(ImpulseDevice device) {
        final Activity activity = this;
        Log.d(LOG_TAG, "getDeviceStatus()");
        final ImpulseDevice theDevice = device;
        updateState(null);
        this.progressBar.setVisibility(View.VISIBLE);
        autoCloseable = mImpulse.track(device, new TrackListener() {
            @Override
            public void onState(ImpulseDeviceState state) {
                progressBar.setVisibility(View.GONE);
                updateState(state);
                stopTracking();
            }

            @Override
            public void onError(int errorCode) {
                String errorString = ImpulsePrintService.getErrorString(activity, errorCode);
                versionTextView.setText(errorString);
                Log.d(LOG_TAG, "badState() " + errorString);
                autoCloseable = null;
            }
        });
    }

    private void stopTracking() {
        if (autoCloseable == null) return;
        try {
            autoCloseable.close();
        } catch (Exception ignore) {
            Log.d(LOG_TAG, "badState() " + ignore.getMessage());
        }
        autoCloseable = null;
    }

    @Override
    protected void onResume() {
        final Activity activity = this;
        Log.d(LOG_TAG, "onResume()");
        super.onResume();

        mImpulse.init(new OperationListener() {
            @Override
            public void onError(int errorCode) {
                Log.d(LOG_TAG, "onError() code=" + ImpulsePrintService.getErrorString(activity, errorCode));
                // TODO: Uh oh. Fix it if we can.
            }

            @Override
            public void onDone() {
                Log.d(LOG_TAG, "(init) onSuccess()");
                discover();
            }
        });

        if (mTrackingDevice == null)
            viewTrackingDevice.setText(" No Device Selected");
        else
            viewTrackingDevice.setText(mTrackingDevice.getDevice().getName() + " selected");
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause()");
        super.onPause();
    }

    private void discover() {
        final Activity activity = this;
        mImpulse.discover(new DiscoverListener() {

            @Override
            public void onDevices(List<ImpulseDevice> devices) {
                PrintActivity.this.onDevices(devices);
            }

            @Override
            public void onError(int error) {
                Log.d(LOG_TAG, "Discovery issue: " + ImpulsePrintService.getErrorString(activity, error));
                Toast.makeText(activity, ImpulsePrintService.getErrorString(activity, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onDevices(List<ImpulseDevice> devices) {
        Log.d(LOG_TAG, "onDevices(" + devices.size() + ")");
        mDevices = devices;

        int inRange = 0;
        for (ImpulseDevice device : mDevices) {
            if (device.getRssi() != 0) {
                inRange++;
            }
        }

        snack("Found " + mDevices.size() + " devices, " + inRange + " in range");
    }


    private void setPrintStatus(boolean isPrinting){
        mImpulse.getService().setPrintingStatus(isPrinting);
        findViewById(R.id.print_button).setEnabled(!isPrinting);
        if(isPrinting){
            progressBar.setVisibility(View.VISIBLE);
        }else{
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();
        mImpulse.close();
    }

    private void snack(String text) {
        View view = findViewById(android.R.id.content);
        if (view != null) {
            Snackbar.make(view, text, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null)
                    .show();
        }
    }

    /*private void sendFirmwareUpdate(final ImpulseDevice device) {
        AssetManager assetManager = getAssets();

        final Button sendFirmwareUpdateBT = (Button) findViewById(R.id.send_firmware_update);
        final TextView firmwareUpgradeTextView = (TextView) findViewById(R.id.firmware_update_message);
        final TextView firmwareUpgradeErrorTextView = (TextView) findViewById(R.id.firmware_update_error_message);

        firmwareUpgradeErrorTextView.setText("");
        firmwareUpgradeErrorTextView.setText("");

        try {
            InputStream inputStream = assetManager.open(getFWFilePath());
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            final long currentTime = System.currentTimeMillis();
            sendFirmwareUpdateBT.setEnabled(false);
            new ImpulseBulkTransferClient(device, buffer,
                    new ImpulseBulkTransferClient.ImpulseBulkTransferListener() {

                        @Override
                        public void onDone() {
                            long duration = System.currentTimeMillis() - currentTime;
                            firmwareUpgradeTextView.setText("Device: " + device.getDevice().getName() + " Finished FW upgrade, duration: " + duration / (1000 * 60) + "sec");
                            Log.d(LOG_TAG, "SendFirmwareUpdate::Done - " + duration / (1000 * 60) + " sec");
                            sendFirmwareUpdateBT.setEnabled(true);
                        }

                        @Override
                        public void onProgress(int progressInPercentage) {
                            firmwareUpgradeTextView.setText("Device: " + device.getDevice().getName() + " firmware upgrade in progress");
                            Log.d(LOG_TAG, "onProgress - " + progressInPercentage);
                        }

                        @Override
                        public void onError(Exception e) {
                            firmwareUpgradeErrorTextView.setText("Device: " + device.getDevice().getName() + ", failed to upgrde, please reset your printer and try again. error: " + e.getMessage());
                            Log.d(LOG_TAG, "SendFirmwareUpdate::e " + e.toString());
                            sendFirmwareUpdateBT.setEnabled(true);
                        }
                    });
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "can't open firmware update file");
        } finally {

        }
    }*/
    private int getPrintMode(){
        /*RadioButton pm1 = (RadioButton) findViewById(R.id.radio_00);
        RadioButton pm2 = (RadioButton) findViewById(R.id.radio_01);
        RadioButton pm3 = (RadioButton) findViewById(R.id.radio_02);
        if(pm1.isChecked()){
            return 0;
        }else if(pm2.isChecked()){
            return 1;
        }else{
            return 2;
        }*/

        return 2;
    }
    private int getPrintCount(){
        /*String pageString = ((Spinner)findViewById(R.id.copy_spinner)).getSelectedItem().toString();
        return parsePrintOption(pageString);*/
        return 1;
    }
    private int getPost(){
        /*String pageString = ((Spinner)findViewById(R.id.post_spinner)).getSelectedItem().toString();
        int post = parsePrintOption(pageString);
        if(post == 1){
            return 0;
        }
        return post;*/
        return 0;
    }

    /*private int parsePrintOption(String selectionString){
        String optionValue = selectionString.substring(0, selectionString.indexOf(" "));
        return Integer.parseInt(optionValue);
    }

    private String getFWFilePath() {

        String[][] FW_FILES = {
                {"HP_v101_R1962_MP.rbn", "HP_v102_R1969_MP.rbn", "HP_v110_R1969_MP.rbn"},
                {"Polaroid_v200.rbn", "Polaroid_v300.rbn"}
        };

        RadioButton upgradeRB101 = (RadioButton) findViewById(R.id.radio_101);
        RadioButton upgradeRB102 = (RadioButton) findViewById(R.id.radio_102);

        int i = mTrackingDevice.getDevice()
                .getName()
                .toLowerCase()
                .contains(ImpulseDevice.PREFIX_HP_DEVICE_NAME.toLowerCase()) ? 0 : 1;

        int j;
        if (upgradeRB101.isChecked()) {
            j = 0;
        } else if (upgradeRB102.isChecked()) {
            j = 1;
        } else {
            j = 2;
        }

        return "firmware_updates/" + FW_FILES[i][j];
    }*/

    /**
     * @param state if null, then will clean params
     */
    private void updateState(ImpulseDeviceState state) {
        Log.d(LOG_TAG, "updateState() " + state);

        if (state == null) {
            versionTextView.setVisibility(View.GONE);
            return;
        }

        versionTextView.setVisibility(View.VISIBLE);
        ImpulseDeviceState.AccessoryInfo info = state.getInfo();
        if (info == null) {
            versionTextView.setText("No state");
            return;
        }
        versionTextView.setText(info.toString());
    }
}
