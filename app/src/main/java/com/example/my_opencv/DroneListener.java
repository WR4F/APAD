package com.example.my_opencv;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

public interface DroneListener {

    void onUpdateImageView(Mat mat);

    void onOnlineStatus(boolean online);

    void onSetAppData(int [] data);

    void onGetAppData();

}
