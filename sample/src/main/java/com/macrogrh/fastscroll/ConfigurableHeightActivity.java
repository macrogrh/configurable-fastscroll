package com.macrogrh.fastscroll;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.macrogrh.configurablefastscroll.ConfigurableFastScroll;

import java.util.ArrayList;

public class ConfigurableHeightActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurable_height);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        ArrayList<Item> items = Item.createList(100, true);
        ItemAdapter adapter = new ItemAdapter(items);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.addItemDecoration(new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if ("image".equals(getIntent().getStringExtra("type"))) {
            recyclerView.addItemDecoration((new ConfigurableFastScroll.Builder(recyclerView)
                    .setThumbDrawable(R.drawable.sample_thumb)
                    .setHeight(R.dimen.fastscroll_thumb_height)
                    .setTopBottomMargin(R.dimen.fastscroll_thumb_top_bottom_margin)
                    .setRightMargin(R.dimen.fastscroll_thumb_right_margin)
//                    .setThumbTint(R.color.colorPrimary)
                    .build()));

            getSupportActionBar().setTitle("Configurable (image)");
        } else {
            // default
            recyclerView.addItemDecoration((new ConfigurableFastScroll.Builder(recyclerView)
//                    .setRightMargin(R.dimen.fastscroll_thumb_right_margin)
//                    .setTopBottomMargin(R.dimen.fastscroll_thumb_top_bottom_margin)
//                    .setThumbTint(R.color.colorPrimary)
                    .build()));

            getSupportActionBar().setTitle("Configurable (default)");
        }

    }
}