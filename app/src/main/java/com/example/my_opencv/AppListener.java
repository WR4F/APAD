package com.example.my_opencv;

public interface AppListener {

    void onDisconnectDrone();

    void onUpdateDrone(int [] data);
}
