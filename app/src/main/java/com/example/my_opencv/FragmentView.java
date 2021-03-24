package com.example.my_opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class FragmentView extends Fragment {

    private Context mContext;
    private ItemViewModel viewModel;
    private boolean status;
    private Bitmap raulito;

    public FragmentView() {
        super(R.layout.image_fragment);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);

        mContext = context;

    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = false;

        try {
            InputStream r = mContext.getAssets().open("APAD.bmp");
            this.raulito = BitmapFactory.decodeStream(r);
            r.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        ImageView imageView = (ImageView) getView().findViewById(R.id.imageView);

        viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);

        viewModel.getStatus().observe(getViewLifecycleOwner(), item -> {
            // Perform an action with the latest item data
            if (item != null) {
                if (status != item) {

                    status = item;

                    if(! status){
                        imageView.setImageBitmap(raulito);
                    }
                }
            }
        });

        viewModel.getImageViewBitMap().observe(getViewLifecycleOwner(), item -> {
            // Perform an action with the latest item data
            if (item != null && status) {

                imageView.setImageBitmap(item);

            }
        });

    }
}
