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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    //network info
    private String IP = "10.0.0.41";
    private int PORT = 9999;
    private DroneConnect myConnection;

    private static final String TAG = "MainActivity";

    //gui
    private Button[] buttons;
    private Switch connect;
    private ImageView imagev;
    private TextView networkstatus;
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

        //launch opencv manager or static link
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        setContentView(R.layout.activity_main);

        //setup image view and text
        imagev = (ImageView) findViewById(R.id.opencvImageView);
        networkstatus = (TextView) findViewById(R.id.status_text);

        //setup switch button and listener
        connect = (Switch) findViewById(R.id.connect_s);
        connect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread(myConnection).start();
                    System.out.println("Started new thread");
                } else {
                    myConnection.disconnect();
                    System.out.println("disconnected");
                }
            }
        });

        //setup buttons
        //land, emergency, up, down, left, right, forward, backward, rot left, rot right
        buttons = new Button[]{(Button) findViewById(R.id.takeoff_b), (Button) findViewById(R.id.emergency_b),
                (Button) findViewById(R.id.up_b), (Button) findViewById(R.id.down_b), (Button) findViewById(R.id.left_b),
                (Button) findViewById(R.id.right_b), (Button) findViewById(R.id.forward_b), (Button) findViewById(R.id.back_b),
                (Button) findViewById(R.id.rotate_left_b), (Button) findViewById(R.id.rotate_right_b)};

        //get raulito image
        File r = new File(this.getFilesDir(), "raulito.bmp");
        raulito = BitmapFactory.decodeFile(r.getAbsolutePath());
        imagev.setImageBitmap(raulito);

        //make network class and listener
        myConnection = new DroneConnect(IP, PORT);
        myConnection.setDroneListiner(new DroneConnect.DroneListener() {

            @Override
            public void onUpdateImageView(Bitmap bmp) {
                updateImageView(bmp);
            }

            @Override
            public void onOnlineStatus(boolean online){
                updateGUI(online);
            }
        });

        //AI class
        //ai = new AI(getApplicationContext());
        //ai.createDDNNetwork();
    }


    //update gui buttons and text based on drone online status
    public void updateGUI(boolean status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                connect.setChecked(status);

                if (status) {

                    //toast
                    Toast.makeText( getApplicationContext(),"Connected!",Toast.LENGTH_SHORT).show();

                    //update status text to online and green
                    networkstatus.setText("Online");
                    networkstatus.setTextColor(Color.GREEN);

                    //show buttons
                    for (Button button : buttons) {
                        button.setVisibility(View.VISIBLE);
                    }

                } else {
                    //toast
                    Toast.makeText( getApplicationContext(),"No Connection!",Toast.LENGTH_SHORT).show();

                    //hide buttons
                    for (Button button : buttons) {
                        button.setVisibility(View.INVISIBLE);
                    }

                    //update status text to offline and red
                    imagev.setImageBitmap(raulito);
                    networkstatus.setText("Offline");
                    networkstatus.setTextColor(Color.RED);

                }

            }
        });

    }

    //function to update image view with latest video feed
    public void updateImageView(Bitmap bmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imagev.setImageBitmap(bmp);
            }
        });

    }

    //handle button press
    public void onButtonPressed(View view) {
        //land/takeoff, emergency, up, down, left, right, forward, backward, rot left, rot right
        //1-10
        int type = 0;

        for(int x = 0; x < buttons.length ; x++){
            if(view.getId() == buttons[x].getId()){
                type = x + 1 ;
            }
        }
    }
}