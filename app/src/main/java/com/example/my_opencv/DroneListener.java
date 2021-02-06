package com.example.my_opencv;

import android.graphics.Bitmap;

public interface DroneListener {

    void onUpdateImageView(Bitmap bmp);

    void onOnlineStatus(boolean online);

    void onDroneUpdate();



}
