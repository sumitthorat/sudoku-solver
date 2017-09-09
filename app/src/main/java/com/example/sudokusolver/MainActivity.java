package com.example.sudokusolver;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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


public class MainActivity extends Activity {
    ImageView imageView;
    Bitmap inputBitmap = null;
    private static final int ACTIVITY_START_CAMERA_APP = 0, REQUEST_EXT_STORAGE = 1;
    private String mImageFileLocation;
    private TessBaseAPI mTess;
    String datapath = "";

    private LoaderCallbackInterface mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("OpenCVT", "OpenCV Loaded!");
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_13, this, mLoaderCallBack);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Button button = (Button) findViewById(R.id.take_picture);
        imageView = (ImageView) findViewById(R.id.image_view);

        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);
        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789");
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
        mTess.setVariable("classify_bin_numeric_mode", "1");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        Button button1 = (Button) findViewById(R.id.process_image);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageProcess();
            }
        });

    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
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

    public void takePhoto() {
        Log.i("OpenCVT", "inside takephoto");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            callCameraApp();
        } else {
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Need external storage permission", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_EXT_STORAGE);
        }
    }

   @Override
   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
       Log.i("OpenCVT", "inside req permiss");
       if(requestCode == REQUEST_EXT_STORAGE) {
           if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
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
        } catch ( IOException e) {
            e.printStackTrace();
        }
        String authorities = getApplicationContext().getPackageName() + ".fileprovider";
        Uri imageUri = FileProvider.getUriForFile(this, authorities, photoFile);
        callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

//        callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        startActivityForResult(callCameraApplicationIntent, ACTIVITY_START_CAMERA_APP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ACTIVITY_START_CAMERA_APP && resultCode == RESULT_OK) {
            Toast.makeText(this, "Picture taken successfully!", Toast.LENGTH_SHORT).show();
            Bitmap rawInputBitmap = BitmapFactory.decodeFile(mImageFileLocation);
            rotateBitmap(rawInputBitmap);
            imageView.setImageBitmap(inputBitmap);
        }
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

            default:
        }
        inputBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void imageProcess() {
        Log.i("OpenCVT", "Inside Image Process");
        Mat inputMat = new Mat(inputBitmap.getHeight(), inputBitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(inputBitmap, inputMat, false);

        Mat grayMat = new Mat();
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        Mat blurMat = new Mat();
        Imgproc.blur(grayMat, blurMat, new Size(1, 1));

        Mat cannyEdges = new Mat();
        Imgproc.Canny(blurMat, cannyEdges, 50, 200);

        Log.i("OpenCVT", "Before HoughLines");
        Mat lines = new Mat();
        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, 150);

        List<double[]> horizontalLines = new ArrayList<>();
        List<double[]> verticalLines = new ArrayList<>();
        List<double[]> SingleHorizontalLines = new ArrayList<>();
        List<double[]> SingleVerticalLines = new ArrayList<>();


        for (int i = 0; i < lines.cols(); i++) {
            double[] line = lines.get(0, i);
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
        for(j = 0; j < 10; j++) {
            double smallestY = 5000, smallestX = 5000;
            double largestY = 0, largestX = 0;
            for (; i < verticalLines.size() - 1; i++) {
                double[] vert = verticalLines.get(i);
                double[] vertNext = verticalLines.get(i + 1);
                if (Math.abs(vert[0] - vertNext[0]) > 80 || Math.abs(vert[2] - vertNext[2]) > 80)
                    break;
                if(vert[0] < smallestX)
                    smallestX = vert[0];
                if(vert[2] < smallestX)
                    smallestX = vert[2];
                if(vert[0] > largestX)
                    largestX = vert[0];
                if(vert[2] > largestX)
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
            double[] toAdd = new double[4];
            toAdd[0] = smallestX ;
            toAdd[1] = smallestY - 100;
            toAdd[2] = largestX;
            toAdd[3] = largestY + 100;
            SingleVerticalLines.add(toAdd);
            i++;
        }

        int ii = 0, jj;
        for(jj = 0; jj < 10; jj++) {
            double smallestY = 5000, smallestX = 5000;
            double largestY = 0, largestX = 0;
            for (; ii < horizontalLines.size() - 1; ii++) {
                double[] vert = horizontalLines.get(ii);
                double[] vertNext = horizontalLines.get(ii + 1);
                if (Math.abs(vert[1] - vertNext[1]) > 80 || Math.abs(vert[3] - vertNext[3]) > 80)
                    break;
                if(vert[0] < smallestX)
                    smallestX = vert[0];
                if(vert[2] < smallestX)
                    smallestX = vert[2];
                if(vert[0] > largestX)
                    largestX = vert[0];
                if(vert[2] > largestX)
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
            double[] toAdd = new double[4];
            toAdd[0] = smallestX - 100 ;
            toAdd[1] = smallestY;
            toAdd[2] = largestX + 100;
            toAdd[3] = largestY;
            SingleHorizontalLines.add(toAdd);
            ii++;
        }

        Log.i("OpenCVT", "After Cacl  " + SingleVerticalLines.size() + SingleHorizontalLines.size());


        List<Point> intersectionPoints = new ArrayList<>();
        for(double [] hort : SingleHorizontalLines)
            for(double [] vert : SingleVerticalLines) {
                Point p = LineOperations.LineIntersection(new Point(hort[0], hort[1]), new Point(hort[2], hort[3]), new Point(vert[0], vert[1]), new Point(vert[2], vert[3]));
                intersectionPoints.add(p);
            }

        Log.i("OpenCVT", "After circles.");
        List<Rect> sudokuTiles = new ArrayList<>();
        for(int a = 0; a < 89; a++) {
            if(a == 9 || a == 19 || a == 29 || a == 39 || a == 49 || a == 59 || a == 69 || a == 79)
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
            Core.rectangle(inputMat, p, p2, new Scalar(255, 0, 0), 6);
            sudokuTiles.add(r);
        }


        String firstIt[] = new String[81];
        String secIt[] = new String[81];
        String thirdIt[] = new String[81];
        String fourthIt[] = new String[81];
        String fifthIt[] = new String[81];
        int finalNos[] = new int[81];
        int sudoku[][] = new int[9][9];

        for(int index = 0; index < sudokuTiles.size(); index++) {
            Rect r = sudokuTiles.get(index);
            Mat sudokuTileMat = new Mat(grayMat, r);
            Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
            Mat threshSudokuTileMat = new Mat();
            Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 1);
            Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

            String OCRResult = null;
            mTess.setImage(sudokuTileBitmap);
            OCRResult = mTess.getUTF8Text();
            if (OCRResult.length() == 1) {
                firstIt[index] = OCRResult;
            }
            else {
                firstIt[index] = "0";
            }
            mTess.clear();
        }

        Log.i("OpenCVT", "After 1st Tess");

        for(int index = 0; index < sudokuTiles.size(); index++) {
            Rect r = sudokuTiles.get(index);
            r.x = r.x + 1;
            r.y = r.y + 1;
            r.height = r.height - 1;
            r.width = r.width - 1;
            Mat sudokuTileMat = new Mat(grayMat, r);
            Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
            Mat threshSudokuTileMat = new Mat();
            Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 1);
            Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

            String OCRResult = null;
            mTess.setImage(sudokuTileBitmap);
            OCRResult = mTess.getUTF8Text();
            if (OCRResult.length() == 1) {
                secIt[index] = OCRResult;
            }
            else {
                secIt[index] = "0";
            }

            mTess.clear();
        }

        Log.i("OpenCVT", "2nd tess");

        for(int index = 0; index < sudokuTiles.size(); index++) {
            Rect r = sudokuTiles.get(index);
            r.x = r.x - 1;
            r.y = r.y - 1;
            r.height = r.height + 1;
            r.width = r.width + 1;
            Mat sudokuTileMat = new Mat(grayMat, r);
            Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
            Mat threshSudokuTileMat = new Mat();
            Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 1);
            Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

            String OCRResult = null;
            mTess.setImage(sudokuTileBitmap);
            OCRResult = mTess.getUTF8Text();
            if (OCRResult.length() == 1) {
                thirdIt[index] = OCRResult;
            }
            else {
                thirdIt[index] = "0";
            }

            mTess.clear();
        }

        Log.i("OpenCVT", "3rd tess");

        for(int index = 0; index < sudokuTiles.size(); index++) {
            Rect r = sudokuTiles.get(index);
            r.x = r.x + 2;
            r.y = r.y + 2;
            r.height = r.height - 2;
            r.width = r.width - 2;
            Mat sudokuTileMat = new Mat(grayMat, r);
            Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
            Mat threshSudokuTileMat = new Mat();
            Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 1);
            Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

            String OCRResult = null;
            mTess.setImage(sudokuTileBitmap);
            OCRResult = mTess.getUTF8Text();
            if (OCRResult.length() == 1) {
                fourthIt[index] = OCRResult;
            }
            else {
                fourthIt[index] = "0";
            }

            mTess.clear();
        }

        Log.i("OpenCVT", "4th tess");

        for(int index = 0; index < sudokuTiles.size(); index++) {
            Rect r = sudokuTiles.get(index);
            r.x = r.x - 2;
            r.y = r.y - 2;
            r.height = r.height - 2;
            r.width = r.width - 2;
            Mat sudokuTileMat = new Mat(grayMat, r);
            Imgproc.GaussianBlur(sudokuTileMat, sudokuTileMat, new Size(5, 5), 0);
            Mat threshSudokuTileMat = new Mat();
            Imgproc.adaptiveThreshold(sudokuTileMat, threshSudokuTileMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 1);
            Bitmap sudokuTileBitmap = Bitmap.createBitmap(threshSudokuTileMat.cols(), threshSudokuTileMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(threshSudokuTileMat, sudokuTileBitmap);

            String OCRResult = null;
            mTess.setImage(sudokuTileBitmap);
            OCRResult = mTess.getUTF8Text();
            if (OCRResult.length() == 1) {
                fifthIt[index] = OCRResult;
            }
            else {
                fifthIt[index] = "0";
            }

            mTess.clear();
        }

        Log.i("OpenCVT", "5th tess");

        int s = 0, t = 0;
        for(int x = 0; x < 81; x++) {
            int count[] = new int[10];
            for(int y = 0; y < 10; y++)
                count[y] = 0;

            count[Integer.parseInt(firstIt[x])]++;
            count[Integer.parseInt(secIt[x])]++;
            count[Integer.parseInt(thirdIt[x])]++;
            count[Integer.parseInt(fourthIt[x])]++;
            count[Integer.parseInt(fifthIt[x])]++;

            int max = 0, mNo = 0;
            for(int z = 0; z < 10; z++) {
                if(count[z] > max) {
                    max = count[z];
                    mNo = z;
                }
            }
            finalNos[x] = mNo;

            if(x == 9 || x == 18 || x == 27|| x == 36|| x == 45 || x == 54 || x == 63 || x == 72) {
                t = 0;
                s++;
            }
            sudoku[s][t] = finalNos[x];
            t++;
        }

        int x = 0;
        for(Rect r : sudokuTiles) {
            if(finalNos[x] != 0)
                Core.putText(inputMat, Integer.toString(finalNos[x]), new Point(r.x, r.y), 1, 10.0f, new Scalar(0, 255, 0), 8);
            x++;
        }

        for(int a = 0; a < 9; a++) {
            for(int b = 0; b < 9; b++) {
                Log.i("OpenCVT", " " + sudoku[a][b]);
            }
            Log.i("OpenCVT", "next row");

        }

        Bitmap tempBitmap = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(inputMat, tempBitmap);
        imageView.setImageBitmap(tempBitmap);
        Log.i("OpenCVT", "End!");
    }

}