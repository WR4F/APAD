package com.example.my_opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;


import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity{

    //network info
    private String IP = "10.0.0.41";
    private int PORT = 9999;
    private DroneConnect myConnection;

    private static final String TAG = "MainActivity";

    //gui
    private Button discb;
    private ImageView imagev;
    private TextView networkstatus;
    private Button connect_b;
    private Bitmap raulito;
    private AI ai;
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
         imagev = (ImageView) findViewById(R.id.opencvImageView);
         networkstatus = (TextView) findViewById(R.id.status_text);
         connect_b = (Button) findViewById(R.id.connect_b);
         discb = (Button) findViewById(R.id.disconnect_b);

        connect_b.setVisibility(View.VISIBLE);
        discb.setVisibility(View.INVISIBLE);

        //get raulito image
        File r = new File(this.getFilesDir(), "raulito.bmp");
        raulito = BitmapFactory.decodeFile(r.getAbsolutePath());
        imagev.setImageBitmap(raulito);

        //make network class and listener
        myConnection = new DroneConnect(IP, PORT, getApplicationContext());
        myConnection.setDroneListiner(new DroneConnect.DroneListener() {

            @Override
            public void onUpdateGUI(Boolean status) {
                updateGUI(status);
            }

            @Override
            public void onUpdateImageView(Bitmap bmp) {
                updateImageView(bmp);
            }
        });

        //AI class
        //ai = new AI(getApplicationContext());
        //ai.createDDNNetwork();

    }

    //connect button
    public void button_onclick(View view) {

        //start new connection
        new Thread(myConnection).start();

    }

    //disconnect button
    public void disconnect(View view) throws IOException {
        //disconnect communication
        myConnection.disconnect();
        discb.setVisibility(View.INVISIBLE);
    }

    //update gui buttons and text based on drone online status
    public void updateGUI(boolean status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status) {
                    networkstatus.setText("Online");
                    networkstatus.setTextColor(Color.GREEN);
                    connect_b.setVisibility(View.INVISIBLE);
                    discb.setVisibility(View.VISIBLE);
                } else {
                    networkstatus.setText("Offline");
                    networkstatus.setTextColor(Color.RED);
                    connect_b.setVisibility(View.VISIBLE);
                    imagev.setImageBitmap(raulito);
                }

            }
        });

    }

    //function to update image view with latest video feed
    public void updateImageView( Bitmap bmp){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imagev.setImageBitmap(bmp);
            }
        });

    }

}