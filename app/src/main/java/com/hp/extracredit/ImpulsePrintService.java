package com.hp.extracredit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hp.impulselib.DiscoverListener;
import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseBinding;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.ImpulseDeviceOptions;
import com.hp.impulselib.ImpulseDeviceState;
import com.hp.impulselib.SendListener;
import com.hp.impulselib.TrackListener;
import com.hp.impulselib.util.Tasks;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImpulsePrintService extends PrintService {
    private static final String LOG_TAG = "ImpulsePrintService";

    private static final String Impulse_MediaSize_2x3 = "2x3";
    private static final String Impulse_Resolution = "300dpi";
    private static final long TRACKING_DELAY = 2000; // Delay between failed attempts to track

    private ImpulseBinding mImpulse;
    private Map<String, ImpulseDevice> mDevices = new HashMap<>();
    private Map<PrintJobId, AutoCloseable> mSends = new HashMap<>();

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate()");
        super.onCreate();
        mImpulse = Impulse.bind(this);
        // Do some init thing including launching an activity to fix permissions if necessary
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();
        mImpulse.close();
    }

    @Nullable
    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        Log.d(LOG_TAG, "onCreatePrinterDiscoverySession()");
        return new DiscoverySession();
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        Log.d(LOG_TAG, "onRequestCancelPrintJob() " + printJob);
        AutoCloseable sending = mSends.remove(printJob.getId());
        if (sending != null) {
            try {
                sending.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Consider the job cancelled no matter what.
        printJob.cancel();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPrintJobQueued(final PrintJob job) {
        Log.d(LOG_TAG, "onPrintJobQueued() " + job);

        //noinspection ConstantConditions (PrintJob info is always present)
        ImpulseDevice device = mDevices.get(job.getInfo().getPrinterId().getLocalId());
        if (device == null) {
            Log.w(LOG_TAG, "Received request to print to undiscovered printer " + job.getInfo().getPrinterId().getLocalId());
            device = Impulse.toDevice(job.getInfo().getPrinterId().getLocalId());
        }

        final ImpulseDevice fDevice = device;

        // Apply auto-exposure option.
        // Note: .setPrintMode(0x02) doesn't seem to work with Polaroid ZIP
        ImpulseDeviceOptions options = new ImpulseDeviceOptions.Builder()
                .setAutoExposure(0x01)
                .build();
        mImpulse.setOptions(device, options, new TrackListener() {
            @Override
            public void onState(ImpulseDeviceState state) {
                Log.d(LOG_TAG, "(setOptions) onDone");
            }

            @Override
            public void onError(int errorCode) {
                Log.d(LOG_TAG, "(setOptions) onError" + errorCode);
            }
        });
        //noinspection ConstantConditions (Document is always present at this point)
        final FileDescriptor fd = job.getDocument().getData().getFileDescriptor();
        final String jobId = job.getId().toString();
        final JobOptions jobOptions = JobOptions.extract(this, job);
        Log.d(LOG_TAG, "onPrintJobQueued() extracted " + jobOptions);
        Tasks.run(new Tasks.Task() {
            Bitmap mBitmap;

            @Override
            public void run() throws IOException {
                mBitmap = getBitmap(fd, jobId, 0, jobOptions);
            }

            @Override
            public void onError(IOException exception) {
                Log.d(LOG_TAG, "Failed to render PDF into bitmap", exception);
                job.fail(getString(R.string.render_failed));
            }

            @Override
            public void onDone() {
                proceedPrint(fDevice, job, mBitmap);
            }
        });
    }

    private void proceedPrint(ImpulseDevice device, final PrintJob job, Bitmap bitmap) {
        job.start();
        AutoCloseable sending = mImpulse.send(device, bitmap, new SendListener() {
            @Override
            public void onProgress(int total, int sent) {
                Log.d(LOG_TAG, "onProgress() total=" + total + " sent=" + sent);
            }

            @Override
            public void onDone() {
                Log.d(LOG_TAG, "onSuccess()");
                job.complete();
                done();
            }

            @Override
            public void onError(int errorCode) {
                Log.d(LOG_TAG, "onError() code=" + errorCode);
                if (errorCode == Impulse.ErrorCancel) {
                    job.cancel();
                } else {
                    job.fail(getErrorString(ImpulsePrintService.this, errorCode));
                }
                done();
            }

            private void done() {
                mSends.remove(job.getId());
            }
        });
        mSends.put(job.getId(), sending);
    }

    /** Use IO to extract bitmap from PDF. Should be run in background */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Bitmap getBitmap(FileDescriptor pdfFile, String id, int pageNumber, JobOptions options) throws IOException {
        // Make a (seekable) file
        //noinspection ConstantConditions (The document will always have data here)
        FileInputStream in = new FileInputStream(pdfFile);
        File tempFile = File.createTempFile(id, ".pdf", getFilesDir());
        FileOutputStream out = new FileOutputStream(tempFile);
        byte buffer[] = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        out.flush();
        out.close();
        in.close();

        // Create a target bitmap
        int x = options.getScale();
        int y = x * 1258 / 818; // 2x3-ish ratio required
        Log.d(LOG_TAG, "Rendering PDF to bitmap " + x + "x" + y);
        Bitmap bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);

        // Use PdfRenderer (L+) to blast onto the bitmap
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(pfd);
        PdfRenderer.Page page = renderer.openPage(pageNumber);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

        // Save the rendered bitmap for debug purposes
        // Bitmaps.toExternalFile(this, bitmap);

        return bitmap;
    }

    private class DiscoverySession extends PrinterDiscoverySession {
        private final static String LOG_TAG = "DiscoverySession";

        private AutoCloseable mDiscover;
        private Map<String, AutoCloseable> mTrackers = new HashMap<>();

        @Override
        public void onStartPrinterDiscovery(@NonNull List<PrinterId> ignored) {
            Log.d(LOG_TAG, "onStartPrinterDiscovery()");
            // TODO: Perhaps we should consider the ignored printers as pretend-discovered?
            DiscoverListener listener = new DiscoverListener() {
                @Override
                public void onDevices(List<ImpulseDevice> devices) {
                    Log.d(LOG_TAG, "onDevices() " + devices);

                    Map<String, ImpulseDevice> old = new HashMap<>(mDevices);
                    mDevices.clear();
                    for (ImpulseDevice device: devices) {
                        // Hang on to old status if we still have it
                        ImpulseDevice oldDevice = old.get(device.getAddress());
                        if (oldDevice != null && oldDevice.getState() != null) {
                            device = new ImpulseDevice.Builder(device)
                                    .setState(oldDevice.getState())
                                    .build();
                        }
                        mDevices.put(device.getAddress(), device);
                    }
                    addPrinters(mDevices.values());
                }

                @Override
                public void onError(int errorCode) {
                    Log.d(LOG_TAG, "onError() " + errorCode);
                    // Uh oh it failed ignore it
                }
            };
            mDiscover = mImpulse.discover(listener);
        }

        @Override
        public void onStopPrinterDiscovery() {
            Log.d(LOG_TAG, "onStopPrinterDiscovery()");
            if (mDiscover != null) {
                try {
                    mDiscover.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mDiscover = null;
            }
        }

        @Override
        public void onValidatePrinters(@NonNull List<PrinterId> ignored) {
        }

        @Override
        public void onStartPrinterStateTracking(@NonNull PrinterId printerId) {
            Log.d(LOG_TAG, "onStartPrinterStateTracking() " + printerId);
            ImpulseDevice device = mDevices.get(printerId.getLocalId());
            if (device == null) {
                Log.d(LOG_TAG, "Tracking undiscovered device: " + printerId);
                // TODO: We can manufacture a device but it will not have a name
                // To get it: http://stackoverflow.com/questions/21073338/android-how-to-find-out-name-of-the-bluetooth-device-connected
                BluetoothManager bluetooth = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
                device = new ImpulseDevice.Builder(bluetooth.getAdapter().getRemoteDevice(printerId.getLocalId()))
                        .build();
            }
            track(device);
        }

        private void track(final ImpulseDevice device) {
            // TODO: Should we just track once, then go to sleep, then try again? Otherwise when
            // we are tracking, nobody else can use it.
            final String address = device.getAddress();
            AutoCloseable tracking = mImpulse.track(device, new TrackListener() {
                @Override
                public void onState(ImpulseDeviceState state) {
                    Log.d(LOG_TAG, "onState() state=" + state);
                    ImpulseDevice updated = new ImpulseDevice.Builder(device)
                            .setState(state)
                            .build();
                    mDevices.put(address, updated);
                    List<PrinterInfo> one = new ArrayList<>();
                    PrinterInfo printerInfo = toPrinterInfo(updated);
                    one.add(printerInfo);
                    addPrinters(one);
                }

                @Override
                public void onError(int errorCode) {
                    Log.d(LOG_TAG, "onError() code=" + errorCode);
                    mTrackers.remove(address);

                    // Repeat after a short delay
                    Tasks.runMainDelayed(TRACKING_DELAY, new Runnable() {
                        @Override
                        public void run() {
                            if (getTrackedPrinters().contains(generatePrinterId(address))) {
                                track(device);
                            }
                        }
                    });
                }
            });
            mTrackers.put(address, tracking);
        }

        @Override
        public void onStopPrinterStateTracking(@NonNull PrinterId printerId) {
            Log.d(LOG_TAG, "onStopPrinterStateTracking() " + printerId);
            AutoCloseable tracker = mTrackers.remove(printerId.getLocalId());
            if (tracker != null) {
                try {
                    tracker.close();
                } catch (Exception ignore) {
                }
            }
        }

        @Override
        public void onDestroy() {
            Log.d(LOG_TAG, "onDestroy()");
        }

        private void addPrinters(Collection<ImpulseDevice> devices) {
            List<PrinterInfo> infos = new ArrayList<>();
            for (ImpulseDevice device: devices) {
                PrinterInfo printerInfo = toPrinterInfo(device);
                infos.add(printerInfo);
            }
            addPrinters(infos);
        }
    }

    private PrinterInfo toPrinterInfo(ImpulseDevice device) {
        PrinterId printerId = generatePrinterId(device.getAddress());
        String name = device.getDevice().getName() + (device.getBonded() ? "" : " (new)");
        PrinterCapabilitiesInfo caps;

        // TODO: Pixels: 640x960, is 320dpi correct?. 640x(1280 or 1248 or 1258)  (or 960 x 1280)
        // Measured output: 50mm (1968.5 mils) x 76mm (2992.13 mils), 640x690 pixels
        caps = new PrinterCapabilitiesInfo.Builder(printerId)
                .addMediaSize(new PrintAttributes.MediaSize(Impulse_MediaSize_2x3, getString(R.string.media_size_2x3), 1969, 2992), true)
                .addResolution(new PrintAttributes.Resolution(Impulse_Resolution, getString(R.string.resolution), 300, 300), true)
                .setColorModes(PrintAttributes.COLOR_MODE_COLOR, PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0))
                .build();

        // TODO: We have availability information but how should we use it?
        // - If we say printer is not available sometimes, then the user can't select it and we
        //   don't get the chance to help the user fix it.
        // - If we say printer is not available then switch to available, an Android bug can leave
        //   the printer selectable but won't allow start of print. (We can work around this by
        //   withholding caps and putting them back later. Blech.)
        // if (device.isAvailable(AVAILABILITY_TIMEOUT)) { ... }
        // For now: always clam the printer is IDLE and has full capabilities even if we don't know
        // that it's around.

        return new PrinterInfo
                .Builder(printerId, name, PrinterInfo.STATUS_IDLE)
                .setCapabilities(caps)
                // Could .setDescription but this never appears anywhere interesting anyway
                .build();
    }

    @SuppressLint("UseSparseArrays")
    private static Map<Integer, Integer> ErrorToString = new HashMap<Integer, Integer>() {{
        put(Impulse.ErrorNone, R.string.device_ready);
        put(Impulse.ErrorBusy, R.string.fail_busy);
        put(Impulse.ErrorBatteryFault, R.string.fail_battery_fault);
        put(Impulse.ErrorBatteryLow, R.string.fail_battery_low);
        put(Impulse.ErrorBluetoothDisabled, R.string.fail_bluetooth_disabled);
        put(Impulse.ErrorBluetoothDiscovery, R.string.fail_bluetooth_discovery);
        put(Impulse.ErrorBluetoothPermissions, R.string.fail_bluetooth_permissions);
        put(Impulse.ErrorCooling, R.string.fail_cooling);
        put(Impulse.ErrorCoverOpen, R.string.fail_cover_open);
        put(Impulse.ErrorDataError, R.string.fail_bad_data);
        put(Impulse.ErrorHighTemperature, R.string.fail_high_temperature);
        put(Impulse.ErrorLowTemperature, R.string.fail_low_temperature);
        put(Impulse.ErrorPaperEmpty, R.string.fail_paper_empty);
        put(Impulse.ErrorPaperJam, R.string.fail_paper_jam);
        put(Impulse.ErrorPaperMismatch, R.string.fail_wrong_paper);
        put(Impulse.ErrorConnectionFailed, R.string.fail_connection_failed);
    }};

    public static String getErrorString(Context context, int errorCode) {
        Integer resourceId = ErrorToString.get(errorCode);
        if (resourceId == null) {
            resourceId = R.string.fail_unknown;
        }
        return context.getString(resourceId);
    }
}
