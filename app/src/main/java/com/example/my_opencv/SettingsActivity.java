package com.example.my_opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class SettingsActivity extends AppCompatActivity {

    private int selected;
    private Intent mainIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        selected = 0;

        View b = findViewById(R.id.confirm_b);
        Button main = (Button) b;

        mainIntent = new Intent(SettingsActivity.this, MainActivity.class);

        main.setOnClickListener(v -> {

            mainIntent.putExtra("flymode", selected);
            startActivity(mainIntent);
        });

        Spinner flymode = findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.flymode_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        flymode.setAdapter(adapter);

        flymode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println((flymode.getItemAtPosition(position)));
                System.out.println(position);

                selected = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }
}