package com.example.my_opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ConnectionThread implements Runnable {


    //network info
    private String IP;
    private int PORT;

    //sockets
    private Net net;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;
    private static boolean online;

    private ImageView imagev;
    private TextView networkstatus;
    private Button connect_b;
    private Bitmap raulito;
    private Context myContext;

    private Handler myHandler;

    //Constructor
    public ConnectionThread(String ip, int port, TextView text, ImageView view, Button b, Context c, String path) throws IOException {
        networkstatus = text;
        imagev = view;
        connect_b = b;
        online = false;
        raulito = BitmapFactory.decodeFile(path);
        myHandler = new Handler();
        IP = ip;
        PORT = port;
        myContext = c;

    }

    //Runnable function
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

        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        }

        //if online continue to update gui and start communication
        if (online){
            toasted("Connected!");
            updateGUI();
            communicate();
        }

        //else just display no connection
        else{
            toasted("No connection response!");
        }

    }

    private void toasted(String t){
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(myContext,t,Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void communicate() {
        if (online) {

            Mat myFrame = new Mat();
            byte[] fr;
            int bytesToRead;
            Mat fanalyze;
            byte[] size_buff;

            //ByteBuffer byteBuffer;
            // keep reading until "Over" is input

            while (online) {
                try {

                    // Get size of packet
                    size_buff = new byte[4];
                    input.read(size_buff);
                    bytesToRead = ByteBuffer.wrap(size_buff).asIntBuffer().get();

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
                    String info = ", w:" + myFrame.width() + ", h: " + myFrame.height();
                    System.out.println("Packet: " + fr + ", packet size: " + bytesToRead + info);

                    //convert and update image view
                    Bitmap bmp = convertMatToBitMap(myFrame);
                    updateImageView(bmp);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                // out.writeUTF("Goodbye!");
                input.close();
                // out.close();
                socket.close();
                System.out.println("successfully closed");

                updateGUI();

            } catch (
                    IOException i) {
                System.out.println(i);
            }
        }
    }

    //image image view with frames
    private void updateImageView(Bitmap bmp) {

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                imagev.setImageBitmap(bmp);
            }
        });

    }

    //update network status GUI
    private void updateGUI() {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                if (online) {
                    networkstatus.setText("Online");
                    networkstatus.setTextColor(Color.GREEN);
                    connect_b.setVisibility(View.INVISIBLE);
                } else {
                    networkstatus.setText("Offline");
                    networkstatus.setTextColor(Color.RED);
                    connect_b.setVisibility(View.VISIBLE);
                    imagev.setImageBitmap(raulito);
                }
            }
        });

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

    public static void disconnect(){
        online = false;
    }

}
