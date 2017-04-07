package com.hp.extracredit;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.hp.extracredit.ui.AvailableStoreActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class CameraActivity extends AppCompatActivity {

    private static final int TAKE_PICTURE = 1;
    private Uri imageUri;
    private Button printButton;
    public static String Flag = "flag";
    private String fileName;


    public void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = null;
        try {
            photo = Utility.createImageFile();
            fileName = photo.getAbsolutePath();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("ssss", fileName).apply();
        } catch (Exception e) {

        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        final Context context = this;
        printButton = (Button) findViewById(R.id.print_button);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PrintActivity.class);
                intent.putExtra("filename", fileName);
                startActivity(intent);
            }
        });
        findViewById(R.id.link_to_merchand).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, AvailableStoreActivity.class);
//                intent.putExtra("filename", fileName);
                startActivity(intent);
            }
        });
        if (!getIntent().getBooleanExtra(Flag, false)) {
            takePhoto();
        } else {
            Toast.makeText(this, "Watermark added to photo.", Toast.LENGTH_SHORT).show();

            fileName = PreferenceManager.getDefaultSharedPreferences(this).getString("ssss", "");
//            Bitmap bmp = BitmapFactory.decodeFile(fileName);
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            bmp.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
//            ((ImageView)findViewById(R.id.imageView)).setImageBitmap(bmp);
//            Utility.displayPic(((ImageView)findViewById(R.id.imageView)), fileName);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);
                    ImageView imageView = (ImageView) findViewById(R.id.imageview);
                    ContentResolver cr = getContentResolver();
                    Bitmap bitmap;
                    try {
                        bitmap = android.provider.MediaStore.Images.Media
                                .getBitmap(cr, selectedImage);

                        imageView.setImageBitmap(bitmap);
                     //   Toast.makeText(this, selectedImage.toString(),
                       //         Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        //Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT)
                          //      .show();
                        //Log.e("Camera", e.toString());
                    }
                }
        }
    }

}
