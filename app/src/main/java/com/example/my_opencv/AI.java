package com.example.my_opencv;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AI {

    //DNN
    private Net net;
    private Context c;

    public AI(Context c){
        this.c = c;
    }

    //Load a network.
    public void createDDNNetwork() {
        String proto = getPath("MobileNetSSD_deploy.prototxt", c);
        String weights = getPath("MobileNetSSD_deploy.caffemodel", c);
        net = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Network loaded successfully");
        //networkstatus.setText("DDN Network loaded successfully");
    }

    //identify objects in frame
    public Bitmap identify(Mat frame) {
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.7;

        // Get a new frame
        // Mat frame = inputFrame;
        //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

        // Forward image through network.
        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), /*swapRB*/false, /*crop*/false);

        net.setInput(blob);
        Mat detections = net.forward();

        int cols = frame.cols();
        int rows = frame.rows();

        detections = detections.reshape(1, (int)detections.total() / 7);

        for (int i = 0; i < detections.rows(); ++i) {

            double confidence = detections.get(i, 2)[0];
            int classId = (int)detections.get(i, 1)[0];

            if (confidence > THRESHOLD && classId == 15) {


                int left   = (int)(detections.get(i, 3)[0] * cols);
                int top    = (int)(detections.get(i, 4)[0] * rows);
                int right  = (int)(detections.get(i, 5)[0] * cols);
                int bottom = (int)(detections.get(i, 6)[0] * rows);

                // Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom),
                        new Scalar(0, 255, 0));

                String label = classNames[classId] + ": " + confidence;
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);

                // Draw background for label.
                Imgproc.rectangle(frame, new Point(left, top - labelSize.height),
                        new Point(left + labelSize.width, top + baseLine[0]),
                        new Scalar(255, 255, 255));

                // Write class name and confidence.
                Imgproc.putText(frame, label, new Point(left, top),
                        Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
            }
        }
        return convertMatToBitMap(frame);
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

    //convert MAT to bmp
    private static Bitmap convertMatToBitMap(Mat input) {
        Bitmap bmp = null;
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB);

        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, bmp);
        } catch (CvException e) {
            e.printStackTrace();
            System.out.println("failed to convert mat to bmp");
        }
        return bmp;

    }

    private static final String TAG = "OpenCV/Sample/MobileNet";
    private static final String[] classNames = {"background","plane","bicycle","bird","boat","bottle",
            "bus","car","cat","chair","cow","dinningtable","dog","horse","motorbike","person",
            "pottedplant","sheep","sofa","train","monitor"};
}
