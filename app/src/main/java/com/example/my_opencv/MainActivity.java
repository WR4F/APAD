package com.example.my_opencv;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;

import android.os.Bundle;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity{

    //network info
    private String IP = "10.0.0.41";
    private int PORT = 9999;
    private ConnectionThread myConnection;

    private static final String TAG = "MainActivity";

    private Button discb;

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
       // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        setContentView(R.layout.activity_main);

        //setup gui objects
        ImageView imagev = (ImageView) findViewById(R.id.opencvImageView);
        TextView networkstatus = (TextView) findViewById(R.id.status_text);
        Button connect_b = (Button) findViewById(R.id.connect_b);
        connect_b.setVisibility(View.VISIBLE);
        discb = (Button) findViewById(R.id.disconnect_b);
        discb.setVisibility(View.INVISIBLE);

        //get raulito
        File r = new File(this.getFilesDir(), "raulito.bmp");
        Bitmap raulito = BitmapFactory.decodeFile(r.getAbsolutePath());
        imagev.setImageBitmap(raulito);


        //make network class
        try {
            myConnection = new ConnectionThread(IP, PORT, networkstatus, imagev, connect_b, discb, getApplicationContext(), r.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //connect button
    public void button_onclick(View view) {

        //start new connection
        new Thread(myConnection).start();


    }

    //disconnect button
    public void disconnect(View view) throws IOException {
        //disconnect communication
        ConnectionThread.disconnect();
        discb.setVisibility(View.INVISIBLE);
    }

}