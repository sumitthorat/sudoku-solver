package com.example.sudokusolver;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sumit on 17/09/17.
 */

public class CustomCropActivity extends Activity {
    /*Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Inside Handler");
           cropped =  cropImageView.getCroppedImage();
            Log.i(TAG, "Inside Handler2");
        }
    };*/
    Bitmap inputBitmap, cropped;
    CropImageView cropImageView;
    private String mImageFileLocation, TAG = "OpenCVT";
    private final int SELECT_FROM_GALLERY = 1, REQUEST_EXT_STORAGE = 2, ACTIVITY_START_CAMERA_APP = 3;
    Thread thread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_crop);
        cropImageView = (CropImageView) findViewById(R.id.cropImageView);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SELECT SOURCE");
        builder.setPositiveButton("GALLERY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                loadImageFromGallery();
            }
        });

        builder.setNegativeButton("CAMERA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                takePhoto();
            }
        });
        builder.show();
    }

    public void loadImageFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, SELECT_FROM_GALLERY);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void takePhoto() {
        Log.i("OpenCVT", "inside takephoto");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            callCameraApp();
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Need external storage permission", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_EXT_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("OpenCVT", "inside req permiss");
        if (requestCode == REQUEST_EXT_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                callCameraApp();
            } else {
                Toast.makeText(this, "External write permission not granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public void callCameraApp() {
        Log.i("OpenCVT", "inside callcam");
        Intent callCameraApplicationIntent = new Intent();
        callCameraApplicationIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String authorities = getApplicationContext().getPackageName() + ".fileprovider";
        Uri imageUri = FileProvider.getUriForFile(this, authorities, photoFile);
        callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(callCameraApplicationIntent, ACTIVITY_START_CAMERA_APP);
    }

    File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE" + timestamp + "_";
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDirectory);
        mImageFileLocation = image.getAbsolutePath();
        return image;
    }

    private void rotateBitmap(Bitmap bitmap) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(mImageFileLocation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;

            default:
        }
        Bitmap rawInputBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        inputBitmap = Bitmap.createScaledBitmap(rawInputBitmap, (int)(bitmap.getWidth() * 0.9), (int) (bitmap.getHeight() * 0.9), false);
        rawInputBitmap.recycle();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_FROM_GALLERY && resultCode == RESULT_OK) {
            SuperActivityToast.create(this, new Style(), Style.TYPE_STANDARD)
                    .setProgressBarColor(Color.WHITE)
                    .setText("Picture loaded from gallery!")
                    .setDuration(Style.DURATION_SHORT)
                    .setFrame(Style.FRAME_LOLLIPOP)
                    .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_BLUE_GREY))
                    .setAnimations(Style.ANIMATIONS_POP).show();
            try {
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap rawInputBitmap = BitmapFactory.decodeStream(imageStream);
                inputBitmap = Bitmap.createScaledBitmap(rawInputBitmap, (int)(rawInputBitmap.getWidth() * 0.9), (int) (rawInputBitmap.getHeight() * 0.9), false);
                if (inputBitmap == null)
                    Log.i("OpenCVT", "Null bitmap");
                cropImageView.setImageBitmap(inputBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == ACTIVITY_START_CAMERA_APP && resultCode == RESULT_OK) {
            SuperActivityToast.create(this, new Style(), Style.TYPE_STANDARD)
                    .setProgressBarColor(Color.WHITE)
                    .setText("Picture taken from camera!")
                    .setDuration(Style.DURATION_SHORT)
                    .setFrame(Style.FRAME_LOLLIPOP)
                    .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_BLUE_GREY))
                    .setAnimations(Style.ANIMATIONS_POP).show();
            Bitmap rawInputBitmap = BitmapFactory.decodeFile(mImageFileLocation);
            rotateBitmap(rawInputBitmap);
            cropImageView.setImageBitmap(inputBitmap);
        }
    }


    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName = "MI_" + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    public void cropImage(View view) {
        cropped = cropImageView.getCroppedImage();
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            cropped.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        Uri imageUri = Uri.fromFile(pictureFile);
        Intent resultIntent = new Intent();
        resultIntent.setData(imageUri);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
