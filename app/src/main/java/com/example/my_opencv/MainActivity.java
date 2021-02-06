package com.example.my_opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;


import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private DroneConnect droneVideo;
    private DroneConnect droneNav;
    private static final String TAG = "MainActivity";

    //gui
    private Button[] buttons;
    private Switch connect;
    private ImageView imagev;
    private TextView networkstatus;
    private Bitmap raulito;

    //private AI ai;

    private AppListener appListener;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    // Initialize OpenCV manager.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                //mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    //listener setter
    public void setAppListener(AppListener listener) {
        this.appListener = listener;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //launch opencv manager or static link
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        setContentView(R.layout.activity_main);

        //setup image view and text
        imagev = findViewById(R.id.opencvImageView);
        networkstatus = findViewById(R.id.status_text);

        //setup buttons
        //land, emergency, up, down, left, right, forward, backward, rot left, rot right
        buttons = new Button[]{findViewById(R.id.takeoff_b), findViewById(R.id.emergency_b),
                findViewById(R.id.up_b), findViewById(R.id.down_b), findViewById(R.id.left_b),
                findViewById(R.id.right_b), findViewById(R.id.forward_b), findViewById(R.id.back_b),
                findViewById(R.id.rotate_left_b), findViewById(R.id.rotate_right_b)};

        //get raulito image
        File r = new File(this.getFilesDir(), "raulito.bmp");
        raulito = BitmapFactory.decodeFile(r.getAbsolutePath());
        imagev.setImageBitmap(raulito);

        //IP
        String IP = "10.0.0.41";

        appListener = null;

        //make drone video class and listener
        int V_PORT = 9999;
        droneVideo = new DroneConnect(IP, V_PORT, MainActivity.this);
        droneVideo.setDroneListener(new DroneListener() {

            @Override
            public void onUpdateImageView(Bitmap bmp) {
                updateImageView(bmp);
            }

            @Override
            public void onOnlineStatus(boolean online) {
                updateGUI(online);
            }

            @Override
            public void onDroneUpdate() {

            }

        });

        //make drone nav and listener
        int NAV_PORT = 9998;
        droneNav = new DroneConnect(IP, NAV_PORT, MainActivity.this);
        droneNav.setDroneListener(new DroneListener() {
            @Override
            public void onUpdateImageView(Bitmap bmp) {

            }

            @Override
            public void onOnlineStatus(boolean online) {
                updateGUI(online);
            }


            @Override
            public void onDroneUpdate() {

            }

        });

        //setup switch button and listener
        connect = findViewById(R.id.connect_s);
        connect.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                new Thread(droneVideo).start();
                new Thread(droneNav).start();

            } else {
                droneVideo.disconnect();
                droneNav.disconnect();
                //appListener.onDisconnectDrone();
            }
        });
    }

    //update gui buttons and text based on drone online status
    public void updateGUI(boolean status) {
        runOnUiThread(() -> {

            connect.setChecked(status);

            if (status) {

                //toast
                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();

                //update status text to online and green
                networkstatus.setText("Online");
                networkstatus.setTextColor(Color.GREEN);

                //show buttons
                for (Button button : buttons) {
                    button.setVisibility(View.VISIBLE);
                }

            } else {
                //toast
                Toast.makeText(getApplicationContext(), "Offline!", Toast.LENGTH_SHORT).show();

                //hide buttons
                for (Button button : buttons) {
                    button.setVisibility(View.INVISIBLE);
                }

                //update status text to offline and red
                imagev.setImageBitmap(raulito);
                networkstatus.setText("Offline");
                networkstatus.setTextColor(Color.RED);

            }

        });

    }

    //function to update image view with latest video feed
    public void updateImageView(Bitmap bmp) {
        runOnUiThread(() -> imagev.setImageBitmap(bmp));

    }

    //handle button press
    public void onButtonPressed(View view) {
        //land/takeoff, emergency, up, down, left, right, forward, backward, rot left, rot right
        //1-10
        int type;

        for (int x = 0; x < buttons.length; x++) {

            if (view.getId() == buttons[x].getId()) {
                type = x + 1;
                //update online status to app
                if (appListener != null) {
                    appListener.onNavButtonPress(type);
                }
                break;
            }
        }
    }
}