package com.example.my_opencv;

import android.graphics.Bitmap;
import android.os.Handler;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class DroneConnect implements Runnable {

    //network info
    private final String IP;
    private final int PORT;

    //sockets
    protected Socket socket;
    protected DataInputStream input;
    protected DataOutputStream output;
    protected boolean online;

    protected DroneListener listener;
    protected MainActivity main;
    protected int button;

    public DroneConnect(String IP, int PORT, MainActivity main) {
        this.IP = IP;
        this.PORT = PORT;
        this.main = main;
        listener = null;
        button = 0;

        main.setAppListener(new AppListener() {
            @Override
            public void onNavButtonPress(int type) {
                button = type;
            }

            @Override
            public void onDisconnectDrone() {
                online = false;
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
            System.out.println(u);
            online = false;
        } catch (IOException i) {
            online = false;
            System.out.println(i);
            System.out.println("Failed to connect video comms.");
        }

        //update online status to app
        if (listener != null) {
            listener.onOnlineStatus(online);
        }

        //if online continue to update gui and start communication
        if (online) {

            if (PORT == 9999) {
                video_comms();
            } else {
                nav_comms();
            }

        } else {

            System.out.println("Failed to connect video comms.");
        }
    }

    protected void nav_comms() {
        String string;

        System.out.println("Established nav comms.");

        try {
            while (online) {

                //send button as string
                output.writeUTF(Integer.toString(button));

                //reset button back to 0
                button = 0;

                //read reply
                string = input.readUTF();

                //show reply if its not 0
                if (!string.equals("0")) {
                    System.out.println("Nav reply: " + string);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();

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
            System.out.println(i);
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
                    myFrame = Imgcodecs.imdecode(new MatOfByte(fr), Imgcodecs.IMREAD_COLOR);

                    //show packet info
                    // String info = ", w:" + myFrame.width() + ", h: " + myFrame.height();
                    //System.out.println("Packet: " + fr + ", packet size: " + bytesToRead + info);

                    //convert and update image view
                    Bitmap bmp = convertMatToBitMap(myFrame);

                    //update image view
                    if (listener != null) {
                        listener.onUpdateImageView(bmp);
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
            System.out.println(i);
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
            System.out.println("failed to convert mat to bmp");
        }
        return bmp;

    }

    public void disconnect() {
        online = false;

    }

}
