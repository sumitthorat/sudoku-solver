package com.example.sudokusolver;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.rw.imaging.ImagingUtils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.yalantis.ucrop.UCrop;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dmax.dialog.SpotsDialog;


public class MainActivity extends Activity {

    ImageView imageView;
    boolean answerFound ;
    AlertDialog progressDialog = null;
    Bitmap inputBitmap = null;
    private static final int ACTIVITY_START_CAMERA_APP = 0;
    private static final int REQUEST_EXT_STORAGE = 1;
    private static final int SELECT_FROM_GALLERY = 2;
    private static final int LIVE_FEED = 3;
    private String mImageFileLocation;
    private TessBaseAPI mTess;
    String datapath = "";
    BoomMenuButton bmb;
    boolean fromLiveFeed = false;
    Mat croppedMatFromLiveFeed;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (progressDialog != null)
            progressDialog.dismiss();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ImagingUtils.getVersion();

        answerFound = false;
        imageView = findViewById(R.id.image_view);
        bmb = findViewById(R.id.bmb);
        bmb.setButtonEnum(ButtonEnum.TextOutsideCircle);
        bmb.setPiecePlaceEnum(PiecePlaceEnum.DOT_4_1);
        bmb.setButtonPlaceEnum(ButtonPlaceEnum.SC_4_1);
        bmb.setNormalColor(Color.parseColor("#DCE775"));

        TextOutsideCircleButton.Builder builder = new TextOutsideCircleButton.Builder()
                        .normalImageRes(R.drawable.icons8_multiple_cameras)
                        .normalText("Load Image")
                        .listener(new OnBMClickListener() {
                            @Override
                            public void onBoomButtonClick(int index) {
                                loadGeneric();
                            }
                        })
                        .rotateImage(false);
        bmb.addBuilder(builder);

        TextOutsideCircleButton.Builder builder1 = new TextOutsideCircleButton.Builder()
                        .normalImageRes(R.drawable.icons8_for_experienced)
                        .normalText("Start Processing")
                        .listener(new OnBMClickListener() {
                            @Override
                            public void onBoomButtonClick(int index) {
                                imageProcess();
                            }
                        })
                        .rotateImage(false);
        bmb.addBuilder(builder1);

        TextOutsideCircleButton.Builder builder2 = new TextOutsideCircleButton.Builder()
                        .normalImageRes(R.drawable.icons8_save)
                        .normalText("Save to Gallery")
                        .listener(new OnBMClickListener() {
                            @Override
                            public void onBoomButtonClick(int index) {
                                Log.i("OpenCVT", "Save to gallery button pressed");
                                new saveImageToGallery().execute();
                            }
                        })
                        .rotateImage(false);
        bmb.addBuilder(builder2);

        TextOutsideCircleButton.Builder builder3 = new TextOutsideCircleButton.Builder()
                .normalImageRes(R.drawable.icons8_instagram_new)
                .normalText("Live Feed(Experimental)")
                .listener(new OnBMClickListener() {
                    @Override
                    public void onBoomButtonClick(int index) {
                        Intent intent = new Intent(MainActivity.this, SudokuLiveDetection.class);
                        startActivityForResult(intent, LIVE_FEED);
                    }
                })
                .rotateImage(false);
        bmb.addBuilder(builder3);

        String language = "eng";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);
        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789");
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
        mTess.setVariable("classify_bin_numeric_mode", "1");
    }

    public void loadGeneric() {
        /*CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);*/
        final CharSequence[] items = {"Gallery", "Camera"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SELECT SOURCE");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        loadImageFromGallery();
                        break;

                    case 1:
                        takePhoto();
                        break;
                }
            }
        });
        builder.show();
    }

    public void loadImageFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, SELECT_FROM_GALLERY);
    }


    private class saveImageToGallery extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if(values[0] == 0) {
                SuperActivityToast.create(MainActivity.this, new Style(), Style.TYPE_STANDARD)
                        .setProgressBarColor(Color.WHITE)
                        .setText("Please load and solve a sudoku first")
                        .setDuration(Style.DURATION_MEDIUM)
                        .setFrame(Style.ANIMATIONS_FLY)
                        .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_LIME))
                        .setAnimations(Style.ANIMATIONS_POP).show();
            } else {
                SuperActivityToast.create(MainActivity.this, new Style(), Style.TYPE_STANDARD)
                        .setProgressBarColor(Color.WHITE)
                        .setText("Image saved to gallery")
                        .setDuration(Style.DURATION_SHORT)
                        .setFrame(Style.ANIMATIONS_FLY)
                        .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_LIME))
                        .setAnimations(Style.ANIMATIONS_POP).show();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(!answerFound) {
                publishProgress(0);
            } else {
                File root = Environment.getExternalStorageDirectory();


                File dir = new File(root.getAbsolutePath() + "/SudokuSolverAPP");
                if (!dir.exists()) dir.mkdirs();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
                File output = new File(dir, "Sudoku" + timeStamp + ".jpg");
                OutputStream os = null;

                try {
                    os = new FileOutputStream(output);
                    inputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.flush();
                    os.close();
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[]{output.toString()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("OpenCVT", "image saved in gallery and gallery is refreshed.");
                                }
                            }
                    );
                    publishProgress(1);
                } catch (Exception e) {
                    e.getStackTrace();
                }
            }
            return null;
        }
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


    private void checkFile(File dir) {
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        Log.i("OpenCVT", "copyFiles called from MainActivity");
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_FROM_GALLERY && resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    startCropActivity(data.getData());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                try {
                    Uri resultUri = result.getUri();
                    InputStream imageStream = getContentResolver().openInputStream(resultUri);
                    inputBitmap = BitmapFactory.decodeStream(imageStream);
                    imageView.setImageBitmap(inputBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.i("OpenCVT", "Error = " + error.getMessage());
            }
        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            InputStream imageStream = null;
            try {
                imageStream = getContentResolver().openInputStream(resultUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            inputBitmap = BitmapFactory.decodeStream(imageStream);
            imageView.setImageBitmap(inputBitmap);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            handleCropResult(data);
        } else if (requestCode == ACTIVITY_START_CAMERA_APP && resultCode == RESULT_OK) {
            File file = new File(mImageFileLocation);
            Uri imageUri = Uri.fromFile(file);
            startCropActivity(imageUri);
        } else if(requestCode == LIVE_FEED) {
            if(resultCode == RESULT_OK) {
                Log.i("OpenCVT", "From Live Feed");
                croppedMatFromLiveFeed = MatDataHolder.getData();
                inputBitmap = Bitmap.createBitmap(croppedMatFromLiveFeed.width(), croppedMatFromLiveFeed.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedMatFromLiveFeed, inputBitmap);
                imageView.setImageBitmap(inputBitmap);
            }
        }
    }


    private void startCropActivity(@NonNull Uri uri) {
        String destinationFileName = "tempImage.jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.start(MainActivity.this);
    }

    private void handleCropResult(@NonNull Intent result) {
        final Uri resultUri = UCrop.getOutput(result);
        try {
            if (resultUri != null) {
                InputStream imageStream = getContentResolver().openInputStream(resultUri);
                inputBitmap = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(inputBitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void imageProcess() {
        if (inputBitmap != null)
            new ImageProcessing().execute();
        else
            SuperActivityToast.create(this, new Style(), Style.TYPE_BUTTON)
                    .setButtonText("LOAD NOW")
                    .setButtonIconResource(R.drawable.icons8_ok)
                    .setOnButtonClickListener("good_tag_name", null, new SuperActivityToast.OnButtonClickListener() {
                        @Override
                        public void onClick(View view, Parcelable token) {
                            loadGeneric();
                        }
                    })
                    .setProgressBarColor(Color.WHITE)
                    .setText("You need to load an image first")
                    .setDuration(Style.DURATION_MEDIUM)
                    .setFrame(Style.ANIMATIONS_FLY)
                    .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_LIME))
                    .setAnimations(Style.ANIMATIONS_POP).show();
    }


    public class ImageProcessing extends AsyncTask<Void, Integer, Bitmap> {

        boolean verticalError = false;
        boolean nullPointerException = false;

        @Override
        protected void onPreExecute() {
            progressDialog = new SpotsDialog(MainActivity.this, R.style.CustomProgressDialog);
            progressDialog.setCancelable(false);
            try {
                progressDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (verticalError == false) {
                if (bitmap == null) {
                    answerFound = false;
                    Log.i("OpenCVT", "On post exec, bitmap is null");
                }
                answerFound = true;
                progressDialog.dismiss();
                inputBitmap = bitmap;
                imageView.setImageBitmap(inputBitmap);
                return;
            } else if(nullPointerException) {
                answerFound = false;
                progressDialog.dismiss();
                SuperActivityToast.create(MainActivity.this, new Style(), Style.TYPE_STANDARD)
                        .setProgressBarColor(Color.WHITE)
                        .setText("Unknown error occured. Please try again.")
                        .setDuration(Style.DURATION_MEDIUM)
                        .setFrame(Style.ANIMATIONS_FLY)
                        .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_LIME))
                        .setAnimations(Style.ANIMATIONS_POP).show();
                return;
            } else {
                answerFound = false;
                progressDialog.dismiss();
                SuperActivityToast.create(MainActivity.this, new Style(), Style.TYPE_STANDARD)
                        .setProgressBarColor(Color.WHITE)
                        .setText("Please correct the orientation!")
                        .setDuration(Style.DURATION_MEDIUM)
                        .setFrame(Style.ANIMATIONS_FLY)
                        .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_LIME))
                        .setAnimations(Style.ANIMATIONS_POP).show();
                return;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]) {
                case 0:
                    progressDialog.setMessage("Finding Lines");
                    break;

                case 1:
                    progressDialog.setMessage("Checking the orientation of image");
                    break;

                case 2:
                    progressDialog.setMessage("Calculating intesection points");
                    break;

                case 3:
                    progressDialog.setMessage("Performing OCR");
                    break;

                case 4:
                    progressDialog.setMessage("Finding the solution");
                    break;
            }
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Mat inputMat = new Mat(inputBitmap.getHeight(), inputBitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(inputBitmap, inputMat, false);
            Log.i("OpenCVT", "Inside Image Process");

            Mat grayMat = new Mat();
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY);

            Mat blurMat = new Mat();
            Imgproc.blur(grayMat, blurMat, new Size(1, 1));

            Mat cannyEdges = new Mat();
            Imgproc.Canny(blurMat, cannyEdges, 50, 200);

            Log.i("OpenCVT", "Before HoughLines");
            publishProgress(0);
            Mat lines = new Mat();
            Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, 150);

            List<double[]> horizontalLines = new ArrayList<>();
            List<double[]> verticalLines = new ArrayList<>();
            List<double[]> SingleHorizontalLines = new ArrayList<>();
            List<double[]> SingleVerticalLines = new ArrayList<>();

            for (int i = 0; i < lines.rows(); i++) {
                double[] line = lines.get(i, 0);
                double x1 = line[0];
                double y1 = line[1];
                double x2 = line[2];
                double y2 = line[3];


                if (Math.abs(y2 - y1) < Math.abs(x2 - x1)) {
                    horizontalLines.add(line);
                } else if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
                    verticalLines.add(line);
                }
            }

            Log.i("OpenCVT", "After Hori n vert");
            Collections.sort(verticalLines, new VerticalLinesSort());
            Collections.sort(horizontalLines, new HorizontalLinesSort());

            int i = 0, j;
            for (j = 0; j < 15 || i < verticalLines.size(); j++) {
                double smallestY = 5000, smallestX = 5000;
                double largestY = 0, largestX = 0;
                for (; i < verticalLines.size() - 1; i++) {
                    double[] vert = verticalLines.get(i);
                    double[] vertNext = verticalLines.get(i + 1);
                    if (Math.abs(vert[0] - vertNext[0]) > 80 || Math.abs(vert[2] - vertNext[2]) > 80) {
                        break;
                    }

                    if (vert[0] < smallestX)
                        smallestX = vert[0];
                    if (vert[2] < smallestX)
                        smallestX = vert[2];
                    if (vert[0] > largestX)
                        largestX = vert[0];
                    if (vert[2] > largestX)
                        largestX = vert[2];
                    if (vert[1] < smallestY)
                        smallestY = vert[1];
                    if (vert[3] < smallestY)
                        smallestY = vert[3];
                    if (vert[1] > largestY)
                        largestY = vert[1];
                    if (vert[3] > largestY)
                        largestY = vert[3];
                }
                if(smallestX != 5000 || smallestY != 5000) {
                    double[] toAdd = new double[4];
                    toAdd[0] = smallestX;
                    toAdd[1] = smallestY - 100;
                    toAdd[2] = largestX;
                    toAdd[3] = largestY + 100;
                    SingleVerticalLines.add(toAdd);
                }
                i++;
            }

            Log.i("OpenCVT", "Vert size : " + SingleVerticalLines.size());


            int ii = 0, jj;
            for (jj = 0; jj < 15 || ii < horizontalLines.size(); jj++) {
                double smallestY = 5000, smallestX = 5000;
                double largestY = 0, largestX = 0;
                for (; ii < horizontalLines.size() - 1; ii++) {
                    double[] vert = horizontalLines.get(ii);
                    double[] vertNext = horizontalLines.get(ii + 1);
                    if (Math.abs(vert[1] - vertNext[1]) > 80 || Math.abs(vert[3] - vertNext[3]) > 80)
                        break;
                    if (vert[0] < smallestX)
                        smallestX = vert[0];
                    if (vert[2] < smallestX)
                        smallestX = vert[2];
                    if (vert[0] > largestX)
                        largestX = vert[0];
                    if (vert[2] > largestX)
                        largestX = vert[2];
                    if (vert[1] < smallestY)
                        smallestY = vert[1];
                    if (vert[3] < smallestY)
                        smallestY = vert[3];
                    if (vert[1] > largestY)
                        largestY = vert[1];
                    if (vert[3] > largestY)
                        largestY = vert[3];
                }
                if(smallestX != 5000 || smallestY != 5000) {
                    double[] toAdd = new double[4];
                    toAdd[0] = smallestX - 100;
                    toAdd[1] = smallestY;
                    toAdd[2] = largestX + 100;
                    toAdd[3] = largestY;
                    SingleHorizontalLines.add(toAdd);
                }

                ii++;
            }

            publishProgress(1);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /*double[] verticalCheck = SingleVerticalLines.get(0);
            if (Math.abs(verticalCheck[2] - verticalCheck[0]) > 100) {
                Log.i("OpenCVT", "Image not vertical");
                verticalError = true;
                return null;
            }*/

            publishProgress(2);
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<Point> intersectionPoints = new ArrayList<>();
            for (double[] hort : SingleHorizontalLines)
                for (double[] vert : SingleVerticalLines) {
                    Point p = LineOperations.LineIntersection(new Point(hort[0], hort[1]), new Point(hort[2], hort[3]), new Point(vert[0], vert[1]), new Point(vert[2], vert[3]));
                    intersectionPoints.add(p);
                }

            publishProgress(3);
            Log.i("OpenCVT", "After inter.");
            List<Rect> sudokuTiles = new ArrayList<>();
            for (int a = 0; a < 89; a++) {
                if (a == 9 || a == 19 || a == 29 || a == 39 || a == 49 || a == 59 || a == 69 || a == 79)
                    continue;
                Point p = intersectionPoints.get(a);
                Point pRight = intersectionPoints.get(a + 1);
                Point pBelow = intersectionPoints.get(a + 10);
                Point p2 = new Point();
                double height, width;
                height = pBelow.y - p.y;
                width = pRight.x - p.x;
                p.x = p.x + (width * 0.18);
                p.y = p.y + (height * 0.18);

                p2.x = p.x + (width * 0.64);
                p2.y = p.y + (height * 0.64);

                Rect r = new Rect(new Point(p.x, p.y), new Point(p2.x, p2.y));
                sudokuTiles.add(r);
            }

            Log.i("OpenCVT", "After Rectangles." + sudokuTiles.size());
            String firstIt[] = new String[81];
            String secIt[] = new String[81];
            String thirdIt[] = new String[81];
            String fourthIt[] = new String[81];
            String fifthIt[] = new String[81];
            int finalNos[] = new int[81];
            int sudoku[][] = new int[9][9];


            for (int index = 0; index < sudokuTiles.size(); index++) {
                finalNos[index] = 0;
                Rect r = sudokuTiles.get(index);
                Mat sudokuTileMat = new Mat(grayMat, r);
                Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
                Mat threshSudokuTileMat = new Mat();
                Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);
                Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

                String OCRResult = null;
                mTess.setImage(sudokuTileBitmap);
                OCRResult = mTess.getUTF8Text();
                if (OCRResult.length() == 1) {
                    firstIt[index] = OCRResult;
                } else {
                    firstIt[index] = "0";
                }
                mTess.clear();
            }

            Log.i("OpenCVT", "After 1st Tess");

            for (int index = 0; index < sudokuTiles.size(); index++) {
                Rect r = sudokuTiles.get(index);
                r.x = r.x + 1;
                r.y = r.y + 1;
                r.height = r.height - 1;
                r.width = r.width - 1;
                Mat sudokuTileMat = new Mat(grayMat, r);
                Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
                Mat threshSudokuTileMat = new Mat();
                Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);
                Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

                String OCRResult;
                mTess.setImage(sudokuTileBitmap);
                OCRResult = mTess.getUTF8Text();
                if (OCRResult.length() == 1) {
                    secIt[index] = OCRResult;
                } else {
                    secIt[index] = "0";
                }

                mTess.clear();
            }

            Log.i("OpenCVT", "2nd tess");

            for (int index = 0; index < sudokuTiles.size(); index++) {
                Rect r = sudokuTiles.get(index);
                r.x = r.x - 1;
                r.y = r.y - 1;
                r.height = r.height + 1;
                r.width = r.width + 1;
                Mat sudokuTileMat = new Mat(grayMat, r);
                Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
                Mat threshSudokuTileMat = new Mat();
                Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);
                Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

                String OCRResult;
                mTess.setImage(sudokuTileBitmap);
                OCRResult = mTess.getUTF8Text();
                if (OCRResult.length() == 1) {
                    thirdIt[index] = OCRResult;
                } else {
                    thirdIt[index] = "0";
                }

                mTess.clear();
            }

            Log.i("OpenCVT", "3rd tess");

            for (int index = 0; index < sudokuTiles.size(); index++) {
                Rect r = sudokuTiles.get(index);
                r.x = r.x + 2;
                r.y = r.y + 2;
                r.height = r.height - 2;
                r.width = r.width - 2;
                Mat sudokuTileMat = new Mat(grayMat, r);
                Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
                Mat threshSudokuTileMat = new Mat();
                Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);
                Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

                String OCRResult;
                mTess.setImage(sudokuTileBitmap);
                OCRResult = mTess.getUTF8Text();
                if (OCRResult.length() == 1) {
                    fourthIt[index] = OCRResult;
                } else {
                    fourthIt[index] = "0";
                }

                mTess.clear();
            }

            Log.i("OpenCVT", "4th tess");

            for (int index = 0; index < sudokuTiles.size(); index++) {
                Rect r = sudokuTiles.get(index);
                r.x = r.x - 2;
                r.y = r.y - 2;
                r.height = r.height - 2;
                r.width = r.width - 2;
                Mat sudokuTileMat = new Mat(grayMat, r);
                Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
                Mat threshSudokuTileMat = new Mat();
                Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 5, 1);
                Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

                String OCRResult;
                mTess.setImage(sudokuTileBitmap);
                OCRResult = mTess.getUTF8Text();
                if (OCRResult.length() == 1) {
                    fifthIt[index] = OCRResult;
                } else {
                    fifthIt[index] = "0";
                }

                mTess.clear();
            }

            Log.i("OpenCVT", "5th tess");


            int s = 0, t = 0;
            for (int x = 0; x < 81; x++) {
                int count[] = new int[10];
                for (int y = 0; y < 10; y++)
                    count[y] = 0;

                count[Integer.parseInt(firstIt[x])]++;
                count[Integer.parseInt(secIt[x])]++;
                count[Integer.parseInt(thirdIt[x])]++;
                count[Integer.parseInt(fourthIt[x])]++;
                count[Integer.parseInt(fifthIt[x])]++;

                int max = 0, mNo = 0;
                for (int z = 0; z < 10; z++) {
                    if (count[z] > max) {
                        max = count[z];
                        mNo = z;
                    }
                }
                finalNos[x] = mNo;

                if (x == 9 || x == 18 || x == 27 || x == 36 || x == 45 || x == 54 || x == 63 || x == 72) {
                    t = 0;
                    s++;
                }
                sudoku[s][t] = finalNos[x];
                t++;
            }

            publishProgress(4);
            SudokuSolver sudokuSolver = new SudokuSolver();
            sudoku = sudokuSolver.printFinal(sudoku);


            try {
                Thread.sleep(300);
            } catch (Exception e) {
                e.printStackTrace();
            }


            Bitmap tempBitmap = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(inputMat, tempBitmap);

            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/font.ttf");
            tempBitmap = tempBitmap.copy(Bitmap.Config.RGB_565, true);
            Canvas canvas = new Canvas(tempBitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShadowLayer(1f, 0f, 1f, Color.BLACK);
            paint.setStrokeWidth(2);
            paint.setColor(Color.parseColor("#ef5350"));
            paint.setTypeface(tf);

            s = 0;
            t = 0;
            for (int y = 0; y < 81; y++) {
                if (y == 9 || y == 18 || y == 27 || y == 36 || y == 45 || y == 54 || y == 63 || y == 72) {
                    t = 0;
                    s++;
                }

                try {
                    if (sudoku[s][t] != finalNos[y]) {
                        Rect r = sudokuTiles.get(y);
                        paint.setTextSize(r.height);
                        canvas.drawText(Integer.toString(sudoku[s][t]), r.x + 20, r.y + r.height - 15, paint);
                    }
                } catch (Exception e) {
                    nullPointerException = true;
                    return null;
                }

                t++;
            }

            Log.i("OpenCVT", "End!");
            return tempBitmap;
        }
    }
}