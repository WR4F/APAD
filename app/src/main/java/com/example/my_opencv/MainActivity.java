package com.example.my_opencv;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.os.Build;
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
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private DroneConnect droneVideo;
    private DroneConnect droneNav;
    private static final String TAG = "MainActivity";

    //gui
    private Button[] buttons;
    private Switch connectSwitch;
    private ImageView imageView;
    private TextView networkStatusText;
    private Bitmap raulito;
    private Switch followMeSwitch;
    private ProgressBar batteryBar;
    private TextView batteryText;
   private int [] appInfo;

    //drone values
    private int [] droneInfo;
    private boolean online;
    private int flyMode;
    private boolean flying;
    private int velocity;
    private int savedFlyingMode;

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


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //launch opencv manager or static link
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        setContentView(R.layout.activity_main);

        //drone info
        online = false;
        droneInfo = new int[]{0,0,0,0,0};
        flyMode = 3;
        flying = false;
        velocity = 3;
        savedFlyingMode = 4;

        appInfo = new int[]{flyMode, velocity};

        //setup image view and text
        imageView = findViewById(R.id.opencvImageView);
        networkStatusText = findViewById(R.id.status_text);

        //setup battery progress bar and text
        batteryText = findViewById(R.id.battery_text);
        batteryBar = findViewById(R.id.battery_progress);
        batteryBar.setMax(100);
        batteryBar.setMin(0);

        //setup buttons
        //land, emergency, up, down, left, right, forward, backward, rot left, rot right
        buttons = new Button[]{findViewById(R.id.takeoff_b), findViewById(R.id.emergency_b),
                findViewById(R.id.up_b), findViewById(R.id.down_b), findViewById(R.id.left_b),
                findViewById(R.id.right_b), findViewById(R.id.forward_b), findViewById(R.id.back_b),
                findViewById(R.id.rotate_left_b), findViewById(R.id.rotate_right_b), findViewById(R.id.switchc_button)};

        //get raulito image
        File r = new File(this.getFilesDir(), "raulito.bmp");
        raulito = BitmapFactory.decodeFile(r.getAbsolutePath());
        imageView.setImageBitmap(raulito);

        //IP
        String IP = "10.0.0.41";

        //app listener
        appListener = null;

        //make drone video class and listener
        int V_PORT = 9999;
        droneVideo = new DroneConnect(IP, V_PORT, this);
        droneVideo.setDroneListener(new DroneListener() {

            //update image view
            @Override
            public void onUpdateImageView(Bitmap bmp) {
                updateImageView(bmp);
            }

            //update gui based on online status
            @Override
            public void onOnlineStatus(boolean online) {
                updateGUI(online);
            }

            @Override
            public void onSetAppData(int[] array) {

            }

            @Override
            public void onGetAppData() {

            }

        });

        //make drone nav and listener
        int NAV_PORT = 9998;
        droneNav = new DroneConnect(IP, NAV_PORT, this);
        droneNav.setDroneListener(new DroneListener() {

            @Override
            public void onUpdateImageView(Bitmap bmp) {

            }

            //update online status
            @Override
            public void onOnlineStatus(boolean online) {
                updateGUI(online);
            }

            //set new drone info from drone to app
            @Override
            public void onSetAppData(int[] array) {

                //update gui if drone status changed
                if (array[0] != droneInfo[0]){
                    flyMode = array[0];
                    updateGUI(online);
                }

                //set new drone info
                droneInfo = array;

            }

            //send new drone info from app to drone
            @Override
            public void onGetAppData() {

                updateAppInfo();

                if(appListener != null){
                    appListener.onUpdateDrone(appInfo);
                }
            }

        });

        //setup switch button and listener
        connectSwitch = findViewById(R.id.connect_s);
        connectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                new Thread(droneVideo).start();
                new Thread(droneNav).start();

            } else {
                droneVideo.disconnect();
                droneNav.disconnect();
                //appListener.onDisconnectDrone();
            }
        });

        //setup follow me switch button and listener
        followMeSwitch = findViewById(R.id.followme_switch);
        followMeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {

                flyMode = savedFlyingMode;

            } else {

                flyMode = 3;
            }
        });
    }

    //update gui buttons and text based on drone online status
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateGUI(boolean status) {

        online = status;

        runOnUiThread(() -> {

            updateStatusText();

            connectSwitch.setChecked(status);

            batteryBar.setProgress(droneInfo[1],true);
            batteryText.setText(String.valueOf(droneInfo[1]) + "%");

            int controls; //weather to show or hide controls

            if (status) {

                //toast
                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();

                if(flying){
                    buttons[0].setText("Land");

                    //show/hide nav buttons
                    if(flyMode == 3){
                        controls = View.VISIBLE;
                    }else{
                        controls = View.INVISIBLE;
                    }

                    for(int x = 2 ; x < 10 ; x++){
                        buttons[x].setVisibility(controls);
                    }

                }

                //just show launch button
                else{
                    buttons[0].setText("Take Off");
                }

            } else {

                //toast
                Toast.makeText(getApplicationContext(), "Offline!", Toast.LENGTH_SHORT).show();

                //update status text to offline and red
                imageView.setImageBitmap(raulito);
                networkStatusText.setText("Offline");
                networkStatusText.setTextColor(Color.RED);

            }

        });

    }

    //function to update image view with latest video feed
    public void updateImageView(Bitmap bmp) {
        runOnUiThread(() -> imageView.setImageBitmap(bmp));

    }

    //handle button press
    public void onButtonPressed(View view) {
        //land/takeoff, emergency, up, down, left, right, forward, backward, rot left, rot right
        //1-10

        if (online){
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

    private void updateStatusText(){
        String status;
        int color;

        switch(droneInfo[0]){
            case 0:
                status = "Offline";
                color = Color.RED;
                break;
            case 1:
                status = "Checking";
                color = Color.YELLOW;
                break;
            case 2:
                status = "Ready To Fly";
                color = Color.GREEN;
                break;
            case 3:
                status = "Flying Manual";
                color = Color.BLUE;
                break;
            case 4:
                status = "Following you";
                color = Color.MAGENTA;
                break;
            case 5:
                status = "Trailing you";
                color = Color.MAGENTA;
                break;
            case 6:
                status = "Above you";
                color = Color.MAGENTA;
                break;
            case 7:
                status = "Landing";
                color = Color.YELLOW;
            case 8:
                status = "Error!";
                color = Color.RED;
            default:
                status = "";
                color = Color.RED;
        }

        String finalStatus = status;
        int finalColor = color;


        networkStatusText.setText(finalStatus);
        networkStatusText.setTextColor(finalColor);

    }

    private void updateAppInfo(){
        droneInfo[0] = flyMode;
        droneInfo[0] = velocity;
    }
}