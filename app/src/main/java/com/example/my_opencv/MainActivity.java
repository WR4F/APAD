package com.example.my_opencv;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import android.view.MotionEvent;
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
import com.google.android.gms.maps.model.LatLng;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


public class MainActivity extends AppCompatActivity implements RecognitionListener {

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
    private boolean recording;
    private boolean paused;
    private double latitude;
    private double longitude;
    private long UPDATE_INTERVAL = 1000;
    private long FASTEST_INTERVAL = 1000;
    private boolean run;

    private MediaPlayer mp;

    private AI ai;

    private SpeechRecognizer recognizer;
    private static final String KEYPHRASE = "apad";
    private static final String KWS_SEARCH = "wakeup";
    private static final String MENU_SEARCH = "menu";

    private AppListener appListener;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

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


    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //launch opencv manager or static link
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        setContentView(R.layout.activity_main);

        //hide PHONE UI
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new SetupTask(this).execute();

        mp = MediaPlayer.create(this, R.raw.chime);

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
        startLocationUpdates();
        recording = false;
        paused = false;
        run = false;

        //AI
        // ai = new AI(getApplicationContext());
        //ai.createDDNNetwork();

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
                findViewById(R.id.baseland_b), findViewById(R.id.record_button)};

        //setup ontouch listeners for buttons
        for (Button button : buttons) {

            button.setOnTouchListener((v, event) -> {

                //get time between touch
                //long eventDuration = event.getEventTime() - event.getDownTime();

                //first touch handle
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    run = true;
                    v.setPressed(true);
                    Thread buttonThread = new Thread(new Runnable() {
                        public void run() {
                            while (run) {
                                buttonPress(v);

                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    });

                    buttonThread.start();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    run = false;
                    v.setPressed(false);
                }

                return true;
            });
        }

        //get raulito image
        try {
            InputStream r = getAssets().open("APAD.bmp");
            raulito = BitmapFactory.decodeStream(r);
            imageView.setImageBitmap(raulito);
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            public void onUpdateImageView(Mat mat) {
                updateImageView(mat);
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
            public void onUpdateImageView(Mat mat) {

            }

            //update online status
            @Override
            public void onOnlineStatus(boolean online) {
                handleOnlineChange(online);
            }

            //set new drone info from drone to app
            @Override
            public void onSetAppData(int[] data) {

                //update error code
                errorCode = data[4];

                //update battery
                battery = data[1];

                handleDroneData(data);

            }

            //send new drone info from app to drone
            @Override
            public void onGetAppData() {

                if (appListener != null) {
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

        //======================================END OF ONCREAT===================================
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    //===============================SPEECH RECOGNITION============================================

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;

        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                System.out.println("Failed to start pocketshpinx ");
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search
        File commandsGrammar = new File(assetsDir, "commands.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, commandsGrammar);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);

        else
            recognizer.startListening(searchName, 10000);

        //System.out.println("=========Switched to " + searchName);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            mp.start();
            switchSearch(MENU_SEARCH);
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {

            String text = hypothesis.getHypstr();
            System.out.println("============On Result " + text);
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

            switch (text) {
                case "land":
                case "take off":
                case "launch":
                    button = 1;
                    break;
                case "take photo":
                    saveImage(((BitmapDrawable) imageView.getDrawable()).getBitmap(), getDateTime());
                    break;
                case "take video":
                case "stop video":
                case "pause video":
                    onRecordButton(findViewById(R.id.record_button));
                    break;
                default:
                    System.out.println("Other voice text:" + text);

            }
        }
    }

    @Override
    public void onError(Exception e) {
        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    //=================================handle online change=======================================
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

    //==================================update gui buttons and text based on drone online status
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void handleDroneData(int[] data) {

        boolean statusChanged = false;

        //check if there was a status change
        if (data[0] != status) {
            statusChanged = true;
        }

        //======================================update launch/land button text and flying status
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

    //===============================update nav button visibility================================
    private void updateNavButtons(int visible) {
        runOnUiThread(() -> {
            //update buttons
            for (int x = 2; x < 10; x++) {
                buttons[x].setVisibility(visible);
            }
        });
    }

    //=================================update lanch/land button text
    private void updateLaunchButton() {

        runOnUiThread(() -> {
            if (flying) {
                buttons[0].setText("Land");
            } else {
                buttons[0].setText("Take Off");
            }
        });
    }

    //=========================function to update image view with latest video feed
    public void updateImageView(Mat mat) {

        //Bitmap bmp = ai.identify(mat);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(convertMatToBitMap(mat));
            }
        });

    }


    //==============================handle button press====================================
    public void buttonPress(View view) {
        //land/takeoff, emergency, up, down, left, right, forward, backward, rot left, rot right
        //1-10

        if (online) {
            for (int x = 0; x < buttons.length; x++) {

                if (view.getId() == buttons[x].getId()) {

                    button = x + 1;

                    System.out.println(button);
                    break;

                }
            }
        }

    }

    //==================================update status text====================================
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
                statusText += getFlightModeString();
                color = flyMode == 3 ? Color.BLUE : Color.MAGENTA;

                break;
            case 4:
                statusText = "Landing";
                color = Color.YELLOW;
                break;

            case 5:
                statusText = "Error: ";
                statusText += getErrorString();
                color = Color.RED;
                break;

            default:
                statusText = String.valueOf(status);
                color = Color.RED;
                break;
        }

        String finalStatusText = statusText;
        int finalColor = color;

        runOnUiThread(() -> {
            networkStatusText.setText(finalStatusText);
            networkStatusText.setTextColor(finalColor);
        });

    }

    //==========================convert MAT to bmp=========================================
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

    //==============================update status mode text====================================
    private String getFlightModeString() {

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
                    statusText += String.valueOf(flyMode);
                    break;
            }
        }

        return statusText;
    }

    private String getErrorString() {
        String errorCodeString;

        switch (errorCode) {
            case 1:
                errorCodeString = "Low battery";
                break;

            case 2:
                errorCodeString = "FL Engine";
                break;

            case 3:
                errorCodeString = "FR Engine";
                break;

            case 4:
                errorCodeString = "BL Engine";
                break;

            case 5:
                errorCodeString = "BR Engine";
                break;

            case 6:
                errorCodeString = "Camera Failure";
                break;

            case 7:
                errorCodeString = "Power Failure";
                break;

            case 8:
                errorCodeString = "GPS Failure";
                break;

            default:
                errorCodeString = String.valueOf(errorCode);
                break;
        }

        return errorCodeString;
    }

    //=================================return app data for drone
    private double[] getAppInfo() {

        return new double[]{button, flyMode, velocity, latitude, longitude};

    }

    //=================================save image to phone
    public void onPhotoTake(View view) {
        saveImage(((BitmapDrawable) imageView.getDrawable()).getBitmap(), getDateTime());
        System.out.println(getDateTime());
    }

    //================================return current date and time
    private static String getDateTime() {
        //SimpleDateFormat day = new SimpleDateFormat("yyyy MM dd hh-mm-ss", Locale.getDefault());

        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

    }

    //===============================do the actual saving wink wink
    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root, "Drone");
        myDir.mkdirs();
        String fname = "" + image_name + ".jpg";
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

    // =========================Trigger new location updates at interval
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

    //======================================get new location
    public void onLocationChanged(Location location) {
        // New location has now been determined
//        String msg = "Updated Location: " +
//             Double.toString(location.getLatitude()) + "," +
//              Double.toString(location.getLongitude());
//        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        //You can now create a LatLng Object for use with maps
        //LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    //================== Get last known recent location using new Google Play Services SDK (v11+)
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

    //=================================request permission for location services
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
    }


    //====================================record button
    public void onRecordButton(View view) {

        if (online) {
            int id;
            String status;

            //switch recording and text status
            if (!recording) {
                id = 1;
                recording = true;
                status = "Stop";
            } else {
                id = 3;
                recording = false;
                status = "Rec";
            }

            //notify drone connect and update button text
            appListener.onRecordUpdate(id);

            ((Button) view).setText(status);
        }

    }

    //===========================record button
    public void onPauseRecButton(View view) {

        if (online) {
            int id;
            String status;

            //switch recording and text status
            if (paused) {
                id = 1;
                paused = false;
                status = "pause";
            } else {
                id = 2;
                paused = true;
                status = "play";
            }

            //notify drone connect and update button text
            appListener.onRecordUpdate(id);
            ((Button) view).setText(status);
        }

    }
}