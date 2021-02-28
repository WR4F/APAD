package com.example.my_opencv;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DroneConnect implements Runnable {

    //network info
    private final String IP;
    private final int PORT;

    //sockets
    protected Socket socket;
    protected DataInputStream input;
    protected DataOutputStream output;
    protected boolean online;

    protected double[] dataToDrone;
    protected int[] dataFromDrone;
    protected DroneListener listener;
    protected MainActivity main;

    protected VideoWriter videoWriter;
    protected int record; //0 nothing, 1 record, 2 pause, 3 stop (and save)
    protected Size size;

    public DroneConnect(String IP, int PORT, MainActivity main) {
        this.IP = IP;
        this.PORT = PORT;
        this.main = main;
        listener = null;
        dataToDrone = new double[]{0, 0, 0, 0, 0};  //button pressed, flight mode, velocity, lat, long
        dataFromDrone = new int[]{0, 0, 0, 0, 0};  //status, battery, velocity, altitude, error code
        online = false;
        record = 0;
        size = new Size(640, 360);


        main.setAppListener(new AppListener() {

            @Override
            public void onDisconnectDrone() {
                online = false;
            }

            @Override
            public void onUpdateDrone(double[] data) {

                dataToDrone = data;
            }

            @Override
            public void onRecordUpdate(int status) {

                record = status;

            }
        });
    }

    //listener setter
    public void setDroneListener(DroneListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        connect();
    }

    //try to establish a connection
    private void connect() {
        // establish a connection
        try {
            socket = new Socket(IP, PORT);
            System.out.println("Connected to " + socket.toString());

            online = true;
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

        } catch (UnknownHostException u) {
            u.printStackTrace();
            online = false;
        } catch (IOException i) {
            online = false;
            i.printStackTrace();
        }

        listener.onOnlineStatus(online);

        //if online continue to update gui and start communication
        if (online) {

            if (PORT == 9999) {
                video_comms();

            } else {

                nav_comms();
            }

        } else {
            System.out.println("Failed to establish a connection to drone.");
        }
    }

    protected void nav_comms() {

        System.out.println("Established nav comms.");

        //String reply = "";

        int intData; // int
        byte[] intDataBytes; // int in bytes

        while (online) {

            try {

                //get latest data from app
                if (listener != null) {
                    listener.onGetAppData();
                }

                //System.out.printf("sent: %s %s %s", dataToDrone[0], dataToDrone[1], dataToDrone[2]);

                //get drone info
                for (int x = 0; x < dataFromDrone.length; x++) {

                    //send app info
                    output.writeDouble(dataToDrone[x]);

                    //get 4 new bytes from drone and convert to int
                    intDataBytes = new byte[4];
                    input.read(intDataBytes);
                    intData = ByteBuffer.wrap(intDataBytes).asIntBuffer().get();

                    //update drone info
                    dataFromDrone[x] = intData;

                    // reply += String.valueOf(intData);

                }

                //set latest data from drone to app
                if (listener != null) {
                    listener.onSetAppData(dataFromDrone);
                }


            } catch (IOException e) {
                e.printStackTrace();
                online = false;
            }
        }


        if (listener != null) {
            listener.onOnlineStatus(false);
        }

        try {

            input.close();
            output.close();
            socket.close();
            System.out.println("successfully closed nav socket");

        } catch (
                IOException i) {
            i.printStackTrace();
        }

    }

    protected void video_comms() {

        System.out.println("Established video comms.");

        new Mat();
        Mat myFrame;       //frame converted from bytes
        byte[] fr;         //frame in bytes
        int bytesToRead;   //size of packet
        byte[] size_buff;  //size of packet in bytes

        while (online) {
            try {

                // Get size of packet
                size_buff = new byte[4];
                input.read(size_buff);
                bytesToRead = ByteBuffer.wrap(size_buff).asIntBuffer().get();

                if (bytesToRead > 0) {

                    //byte frame
                    fr = new byte[bytesToRead];


                    //get frame
                    input.readFully(fr);

                    //convert binary to MAT
                    myFrame = Imgcodecs.imdecode(new MatOfByte(fr), Imgcodecs.IMREAD_COLOR);

                    if (record == 1 ){
                       if(videoWriter == null){
                           videoWriter = new VideoWriter(recordFilePath(),VideoWriter.fourcc('M', 'J', 'P', 'G'),30.0, size);

                       }

                       if(!videoWriter.isOpened()){

                           videoWriter.open(recordFilePath(),VideoWriter.fourcc('M', 'J', 'P', 'G'),30.0, size);
                       }

                        System.out.println("Writing "+ myFrame.toString());
                        videoWriter.write(myFrame);

                    } else if (record == 3) {
                        System.out.println("released");
                        videoWriter.release();
                    }

                    //convert and update image view
                    //Bitmap bmp = convertMatToBitMap();

                    //update image view
                    if (listener != null) {
                        listener.onUpdateImageView(myFrame);
                    }

                } else {
                    fr = new byte[1];
                    //get frame
                    input.readFully(fr);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (listener != null) {
            listener.onOnlineStatus(false);
        }

        try {

            input.close();
            output.close();
            socket.close();
            System.out.println("successfully closed video socket");

        } catch (
                IOException i) {
            i.printStackTrace();
        }

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

    public void disconnect() {
        online = false;

    }

    //return current date and time
    private static String getDateTime() {

        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

    }

    private String recordFilePath() {
        File sddir = Environment.getExternalStorageDirectory();
        File vrdir = new File(sddir, "Drone");
        String mTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(vrdir, "KLI_" + mTimeStamp + ".avi");
        String filepath = file.getAbsolutePath();

        return filepath;

    }
}
