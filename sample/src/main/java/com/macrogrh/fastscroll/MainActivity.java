package com.macrogrh.fastscroll;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_android_fastscroll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AndroidFastScrollActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.button_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ConfigurableHeightActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.button_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ConfigurableHeightActivity.class);
                intent.putExtra("type","image");
                startActivity(intent);
            }
        });

        getSupportActionBar().setTitle("Sample");
    }
}