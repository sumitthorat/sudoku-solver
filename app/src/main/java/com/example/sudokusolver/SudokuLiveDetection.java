package com.example.sudokusolver;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sumit on 08/10/17.
 */

public class SudokuLiveDetection extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public SudokuLiveDetection() {
        this.getClass();
    }

    Button captureButton, flashButton;
    //ImageView imageView;
    Mat cropped;
    boolean flashEnabled = false;

    private PortraitCameraView mOpenCvCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_detection);
        //ImagingUtils.getVersion();
        //Log.i(TAG, "After OpenCV loading");
        //checkPermissions();
        //imageView = findViewById(R.id.imageView);
        captureButton = findViewById(R.id.captureButton);
        mOpenCvCameraView = findViewById(R.id.javaCamera);
        flashButton = findViewById(R.id.flashButton);

        mOpenCvCameraView.enableView();

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Bitmap bitmap = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);
                //Utils.matToBitmap(cropped, bitmap);
                //imageView.setImageBitmap(bitmap);
                MatDataHolder.setData(cropped);
                //Intent data = new Intent();
                //data.setData(null);
                /*long addr = cropped.getNativeObjAddr();
                Intent data = new Intent();
                data.putExtra("nativeObjAddr", addr);
                setResult(RESULT_OK, data);*/
                setResult(RESULT_OK);
                finish();
            }
        });

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(flashEnabled) {
                    mOpenCvCameraView.flashToggleOFF();
                    flashEnabled = false;
                } else {
                    mOpenCvCameraView.flashToggleON();
                    flashEnabled = true;
                }
            }
        });




    }
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat grayMat= inputFrame.gray();
        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(grayMat, blurMat, new Size(5,5), 0);
        Mat thresh = new Mat();
        //Imgproc.adaptiveThreshold(blurMat, thresh, 255,1,1,11,2);
        Imgproc.Canny(blurMat, thresh,10, 100);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hier = new Mat();
        Imgproc.findContours(thresh, contours, hier, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        hier.release();

        MatOfPoint2f biggest = new MatOfPoint2f();
        double max_area = 0;
        for (MatOfPoint i : contours) {
            double area = Imgproc.contourArea(i);
            if (area > 100) {
                MatOfPoint2f m = new MatOfPoint2f(i.toArray());
                double peri = Imgproc.arcLength(m, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(m, approx, 0.02 * peri, true);
                if (area > max_area && approx.total() == 4) {
                    biggest = approx;
                    max_area = area;
                }
            }
        }

        // find the outer box
        Mat displayMat = inputFrame.rgba();
        Mat displayMatCopy = displayMat.clone();
        Point[] points = biggest.toArray();
        cropped = new Mat();
        int t, padding = 15;
        if (points.length >= 4) {
            double smallestX = 5000, smallestY = 5000, largestX = 0, largestY = 0;
                for(int q = 0; q < 4; q++) {
                    if(points[q].x < smallestX) {
                        smallestX = points[q].x;
                    }
                    if(points[q].x > largestX) {
                        largestX = points[q].x;
                    }
                    if(points[q].y < smallestY) {
                        smallestY = points[q].y;
                    }
                    if(points[q].y > largestX) {
                        largestY = points[q].y;
                    }
                }
            // draw the outer box
            Imgproc.line(displayMat, new Point(points[0].x, points[0].y), new Point(points[1].x, points[1].y), new Scalar(255, 0, 0), 2);
            Imgproc.line(displayMat, new Point(points[1].x, points[1].y), new Point(points[2].x, points[2].y), new Scalar(255, 0, 0), 2);
            Imgproc.line(displayMat, new Point(points[2].x, points[2].y), new Point(points[3].x, points[3].y), new Scalar(255, 0, 0), 2);
            Imgproc.line(displayMat, new Point(points[3].x, points[3].y), new Point(points[0].x, points[0].y), new Scalar(255, 0, 0), 2);
            // crop the image
            //Rect R = new Rect(new Point(points[0].x - t, points[0].y - t), new Point(points[2].x + t, points[2].y + t));
            //Rect R = new Rect(new Point(points[0].x - padding, points[0].y - padding), new Point(points[2].x + padding, points[2].y + padding));
            Rect R = new Rect(new Point(smallestX - padding, smallestY - padding), new Point(largestX + padding, largestY + padding));
            if (displayMat.width() > 1 && displayMat.height() > 1) {
                try {
                    cropped = new Mat(displayMatCopy, R);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        return displayMat;
    }
}
