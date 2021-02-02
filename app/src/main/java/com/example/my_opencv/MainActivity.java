package com.example.my_opencv;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.dnn.Dnn;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity{

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    // Initialize OpenCV manager.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //launch opencv manager
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);

        setContentView(R.layout.activity_main);

        // Set up camera listener.
        //mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.CameraView);
       // mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        //mOpenCvCameraView.setCvCameraViewListener(this);

        //setup image and text view
        imagev = (ImageView) findViewById(R.id.opencvImageView);
       // networkstatus = (TextView) findViewById(R.id.statusText);

        //create ddn network
       // createDDNNetwork();

        //run socket
        connect.start();

    }

    Thread connect = new Thread(new Runnable() {

        @Override
        public void run() {
            // establish a connection
            try
            {
                socket = new Socket(IP, PORT);
                System.out.println("Connected");
               // networkstatus.setText("Connected!");
                input   = new DataInputStream(socket.getInputStream());

                //String mssg = input.readUTF();

                // System.out.println("server reply: " + mssg);
            }
            catch(UnknownHostException u)
            {
                System.out.println(u);
            }
            catch(IOException i)
            {
                System.out.println(i);
            }

            String data = "";
            myFrame = new Mat();

            byte[] fr;
            int bytesToRead;
            int bytesRead;
            Mat fanalyze;
            byte[] size_buff;

            //ByteBuffer byteBuffer;
            // keep reading until "Over" is input
            while (!data.equals("over"))
            {
                try
                {

                    // Get size of packet
                    size_buff = new byte[4];
                    input.read(size_buff);
                    bytesToRead = ByteBuffer.wrap(size_buff).asIntBuffer().get();

                    //byte frame
                    fr = new byte[bytesToRead];
                    //bytesRead = 0;

                    //get whole frame x bytes at a time
//                    while(true){
//                        bytesRead += input.read(fr);
//                        if(bytesRead >= bytesToRead){
//                            break;
//                        }
//                    }

                    //get frame
                    input.readFully(fr);

                    //convert binary to MAT
                    myFrame =  Imgcodecs.imdecode(new MatOfByte(fr), Imgcodecs.IMREAD_COLOR);

                    //show packet info
                    String info = ", w:" + myFrame.width() + ", h: " + myFrame.height();
                    System.out.println("Packet: " + fr + ", packet size: " + bytesToRead + info);

                   // myFrame = identify(myFrame);

                    //update image view
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            //convert to bitmap
                            bitmap = convertMatToBitMap(myFrame);

                            imagev.setImageBitmap(bitmap);

                        }
                    });

                }
                catch(IOException i)
                {
                    System.out.println(i);
                }
            }

            // close the connection
            try
            {
                // out.writeUTF("Goodbye!");
                input.close();
                // out.close();
                socket.close();
                System.out.println("successfully closed");
            }
            catch(IOException i)
            {
                System.out.println(i);
            }

        }
    });

    //convert mat to bitmap
    private static Bitmap convertMatToBitMap(Mat input){
        Bitmap bmp = null;
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB);

        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, bmp);
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        }
        return bmp;
    }

    //Load a network.
    public void createDDNNetwork() {
        String proto = getPath("MobileNetSSD_deploy.prototxt", this);
        String weights = getPath("MobileNetSSD_deploy.caffemodel", this);
        net = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Network loaded successfully");
        //networkstatus.setText("DDN Network loaded successfully");
    }

    //identify objects in frame
    public Mat identify(Mat frame) {
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
        return frame;
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


    private static final String TAG = "OpenCV/Sample/MobileNet";
    private static final String[] classNames = {"background","plane","bicycle","bird","boat","bottle",
            "bus","car","cat","chair","cow","dinningtable","dog","horse","motorbike","person",
            "pottedplant","sheep","sofa","train","monitor"};

    private Net net;
    private Socket socket;
    //private DataInputStream  input   = null;
    //private DataOutputStream out     = null;
    private DataInputStream input     = null;
    private ImageView imagev;
   // private TextView networkstatus;

    //network info
    private String IP = "10.0.0.41";
    private int PORT = 9999;

    private Bitmap bitmap;
    private Mat myFrame;
}