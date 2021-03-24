package com.example.my_opencv;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemViewModel extends ViewModel {

    private final MutableLiveData<Bitmap> imageViewBitMap = new MutableLiveData<>();
    private final MutableLiveData<Boolean> status = new MutableLiveData<>();

    public void selectBitmap(Bitmap item) {
        imageViewBitMap.setValue(item);
    }

    public void selectStatus(boolean item) {
        status.setValue(item);
    }

    public LiveData<Bitmap> getImageViewBitMap() {
        return imageViewBitMap;
    }

    public MutableLiveData<Boolean> getStatus() {
        return status;
    }
}

