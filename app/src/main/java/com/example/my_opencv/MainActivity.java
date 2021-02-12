package com.example.my_opencv;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;


import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


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

    //drone
    private int status;
    private int flyMode;
    private int battery;
    private int velocity;
    private int altitude;
    private int errorCode;

    //app
    private boolean online;
    private int savedFlyingMode;
    private boolean flying;
    private int button;
    private LocationRequest mLocationRequest;
    private double latitude;
    private double longitude;
    private long UPDATE_INTERVAL = 1000;
    private long FASTEST_INTERVAL = 1000;

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
        status = 0;
        flyMode = 3;
        battery = 100;
        velocity = 3;
        altitude = 0;
        errorCode = 0;

        //app
        button = 0;
        savedFlyingMode = 4;
        online = false;
        flying = false;
        //startLocationUpdates();

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
                findViewById(R.id.rotate_left_b), findViewById(R.id.rotate_right_b), findViewById(R.id.switchc_button),
                findViewById(R.id.baseland_b)};

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

            }

            @Override
            public void onSetAppData(int[] data) {

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
                handleOnlineChange(online);
            }

            //set new drone info from drone to app
            @Override
            public void onSetAppData(int[] data) {

                handleDroneData(data);
                //System.out.println("received: " + data[1]);

            }

            //send new drone info from app to drone
            @Override
            public void onGetAppData() {

                //System.out.println(button);
                //System.out.println(Arrays.toString(getAppInfo()));

                if (appListener != null) {
                    System.out.println("switched: " + flyMode);
                    appListener.onUpdateDrone(getAppInfo());
                }

                //reset
                button = 0;
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

            int controls;

            if (isChecked) {

                flyMode = savedFlyingMode;
                controls = View.INVISIBLE;

            } else {

                flyMode = 3;
                controls = View.VISIBLE;

            }

            updateNavButtons(controls);
            updateStatusText();

            System.out.println("switched: " + flyMode);

        });
    }

    //handle online change
    public void handleOnlineChange(boolean online) {

        this.online = online;

        updateStatusText();

        runOnUiThread(() -> {
            connectSwitch.setChecked(online);

            if (online) {
                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(getApplicationContext(), "Offline!", Toast.LENGTH_SHORT).show();

                //update status text to offline and red
                imageView.setImageBitmap(raulito);
                networkStatusText.setText("Offline");
                networkStatusText.setTextColor(Color.RED);
            }
        });
    }

    //update gui buttons and text based on drone online status
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void handleDroneData(int[] data) {

        boolean statusChanged = false;

        //check if there was a status change
        if (data[0] != status) {
            statusChanged = true;
        }

        //update battery
        battery = data[2];

        //update button text and flying status
        if (statusChanged) {

            //update flying status
            if (status == 2 && data[0] >= 3) {

                flying = true;

            } else if (status >= 3 && data[0] == 2) {

                flying = false;
            }

            //update status and text
            status = data[0];
            updateStatusText();

            //update button
            updateLaunchButton();

        }

        //update battery
        runOnUiThread(() -> {

            batteryBar.setProgress(battery, true);
            batteryText.setText(String.valueOf(battery) + "%");
        });

    }

    //update nav button visibility
    private void updateNavButtons(int visible) {
        runOnUiThread(() -> {
            //update buttons
            for (int x = 2; x < 10; x++) {
                buttons[x].setVisibility(visible);
            }
        });
    }

    //update lanch/land button text
    private void updateLaunchButton() {

        runOnUiThread(() -> {
            if (flying) {
                buttons[0].setText("Land");
            } else {
                buttons[0].setText("Take Off");
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

        if (online) {

            for (int x = 0; x < buttons.length; x++) {

                if (view.getId() == buttons[x].getId()) {

                    button = x + 1;

                    //System.out.println(button);

                    break;
                }
            }
        }
    }

    //update status text
    private void updateStatusText() {
        String statusText;
        int color;

        switch (status) {
            case 0:
                statusText = "Offline";
                color = Color.RED;
                break;
            case 1:
                statusText = "Checking";
                color = Color.YELLOW;
                break;
            case 2:
                statusText = "Ready To Fly";
                color = Color.GREEN;
                break;

            case 3:
                statusText = "Flying: ";
                statusText += updateFlightText();
                color = flyMode == 3 ? Color.BLUE : Color.MAGENTA;

                break;
            case 4:
                statusText = "Landing";
                color = Color.YELLOW;
            case 5:
                statusText = "Error!";
                color = Color.RED;
            default:
                statusText = "";
                color = Color.RED;
        }

        String finalStatusText = statusText;
        int finalColor = color;

        runOnUiThread(() -> {
            networkStatusText.setText(finalStatusText);
            networkStatusText.setTextColor(finalColor);
        });

    }

    //update status mode text
    private String updateFlightText() {

        String statusText = "";

        if (flying) {
            switch (flyMode) {
                case 3:
                    statusText += "Manual";

                    break;
                case 4:
                    statusText += "Following";


                    break;
                case 5:
                    statusText += "Trailing";

                    break;
                case 6:
                    statusText += "Above";

                    break;
                default:
                    statusText += "";
                    break;
            }
        }

        return statusText;
    }

    //return app data for drone
    private int[] getAppInfo() {

        return new int[]{button, flyMode, velocity};

    }

    //save image to phone
    public void onPhotoTake(View view) {
        saveImage(((BitmapDrawable) imageView.getDrawable()).getBitmap(), getDateTime());
        System.out.println(getDateTime());
    }

    //return current date and time
    private static String getDateTime() {
        SimpleDateFormat day = new SimpleDateFormat("yyyy MM dd hh-mm-ss'.tsv'", Locale.getDefault());

        return day.toString();

    }

    //do the actual saving wink wink
    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();


        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions();
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    //get new location
    public void onLocationChanged(Location location) {
        // New location has now been determined
        //String msg = "Updated Location: " +
        //     Double.toString(location.getLatitude()) + "," +
        //      Double.toString(location.getLongitude());
        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        // You can now create a LatLng Object for use with maps
        //  LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    // Get last known recent location using new Google Play Services SDK (v11+)
    public void getLastLocation() {

        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        }

        locationClient.getLastLocation()

                .addOnSuccessListener(location -> {
                    // GPS location can be null if GPS is switched off
                    if (location != null) {
                        onLocationChanged(location);
                    }
                })

                .addOnFailureListener(e -> {
                    Log.d("MapDemoActivity", "Error trying to get last GPS location");
                    e.printStackTrace();
                });
    }

    //request permission for location services
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
    }
}