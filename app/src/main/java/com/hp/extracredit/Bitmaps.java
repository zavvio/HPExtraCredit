package com.hp.extracredit;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class Bitmaps {
    private static final String LOG_TAG = "Bitmaps";

    public static void toExternalFile(Context context, Bitmap source) {
        String root = Environment.getExternalStorageDirectory().toString();
        File targetDirectory = new File(root, context.getString(context.getApplicationInfo().labelRes));
        targetDirectory.mkdirs();
        File targetFile;
        int n = 0;
        do {
            n++;
            targetFile = new File(targetDirectory, String.format(Locale.getDefault(), "image_%d.jpg", n));
        } while(targetFile.exists());

        try {
            FileOutputStream out = new FileOutputStream(targetFile);
            source.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.w(LOG_TAG, "Failed to write", e);
        }
    }
}
