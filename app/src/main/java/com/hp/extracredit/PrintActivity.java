package com.hp.extracredit;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.impulselib.DiscoverListener;
import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseBinding;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.MaltaService;
import com.hp.impulselib.OperationListener;
import com.hp.impulselib.bt.ImpulsePrintClient;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
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

    private Button getBleInfoButton;


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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // request permissin explicityly
        ActivityCompat.requestPermissions(this, permissions, REQUEST_ID_PERMISSIONS);

        mImpulse = Impulse.bind(this);
        viewTrackingDevice = (TextView) findViewById(R.id.view_selected_device);
        getBleInfoButton = (Button) findViewById(R.id.ble_device_info_button);
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

    public void onClickSPP(View view) {


        final Activity thisActivity = this;
        int printCount = 1;  // 1 copy
        int printMode = 2;  // image full
        int post = 0;  // 1 post

        if (mTrackingDevice == null)  {
            snack("No device found");
            return;
        }

        setPrintStatus(true);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.seaturtle_sample_6str_96wpi_96ppi);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
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
        impulsePrintClient.print();
        bmp.recycle();

    }

    public void onClickFindBLEDevices(View view) {
        Toast.makeText(getApplicationContext(), "onClickFindBLEDevices", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
        startActivityForResult(intent, SELECT_DEVICE_REQUEST);
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
        findViewById(R.id.print_SPP_button).setEnabled(!isPrinting);
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
}
