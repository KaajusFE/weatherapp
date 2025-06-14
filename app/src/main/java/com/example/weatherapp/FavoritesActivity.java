package com.example.weatherapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


public class FavoritesActivity extends AppCompatActivity implements FavoriteCityAdapter.OnItemClickListener {

    private RecyclerView recyclerViewFavorites;
    private FavoriteCityAdapter adapter;
    private ArrayList<String> favoriteCitiesList;
    private TextView textViewNoFavorites;
    private Button buttonBackToMain;


    public static final String PREFS_NAME = "FavoriteCitiesPrefs";
    public static final String FAVORITE_PREFIX = "favorite_";
    public static final String EXTRA_CITY_NAME_FROM_FAVORITES = "com.example.weatherapp.CITY_NAME_FROM_FAVORITES";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Favorite Cities");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        textViewNoFavorites = findViewById(R.id.textViewNoFavorites);
        favoriteCitiesList = new ArrayList<>();
        buttonBackToMain = findViewById(R.id.buttonBackToMain);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoriteCityAdapter(this, favoriteCitiesList, this);
        recyclerViewFavorites.setAdapter(adapter);
    }

    private void loadFavoriteCities() {
        Log.d("FavoritesActivity", "Loading favorite cities...");
        favoriteCitiesList.clear();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        if (allEntries.isEmpty()) {
            Log.d("FavoritesActivity", "SharedPreferences is empty.");
        } else {
            Log.d("FavoritesActivity", "SharedPreferences has " + allEntries.size() + " entries.");
        }


        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("FavoritesActivity", "Checking entry: " + entry.getKey() + " = " + entry.getValue());
            if (entry.getKey().startsWith(FAVORITE_PREFIX)) {

                if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                    String cityNameWithPrefix = entry.getKey();
                    String cityName = cityNameWithPrefix.substring(FAVORITE_PREFIX.length());


                    if (!cityName.isEmpty()) {
                        cityName = cityName.substring(0, 1).toUpperCase() + cityName.substring(1).toLowerCase();
                    }
                    Log.d("FavoritesActivity", "Adding favorite city: " + cityName);
                    favoriteCitiesList.add(cityName);
                } else {
                    Log.d("FavoritesActivity", "Entry " + entry.getKey() + " is not a true boolean favorite.");
                }
            }
        }
        buttonBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(FavoritesActivity.this, MainActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        Collections.sort(favoriteCitiesList);

        updateUIBasedOnFavorites();
        adapter.notifyDataSetChanged();
        Log.d("FavoritesActivity", "Favorite cities loaded. Count: " + favoriteCitiesList.size());
    }

    private void updateUIBasedOnFavorites() {
        if (favoriteCitiesList.isEmpty()) {
            textViewNoFavorites.setVisibility(View.VISIBLE);
            recyclerViewFavorites.setVisibility(View.GONE);
            Log.d("FavoritesActivity", "No favorites to show.");
        } else {
            textViewNoFavorites.setVisibility(View.GONE);
            recyclerViewFavorites.setVisibility(View.VISIBLE);
            Log.d("FavoritesActivity", "Showing " + favoriteCitiesList.size() + " favorite cities.");
        }
    }


    @Override
    public void onItemClick(String cityName) {
        Log.d("FavoritesActivity", "Clicked favorite city: " + cityName);


        Intent intent = new Intent(this, MainActivity.class);


        intent.putExtra(EXTRA_CITY_NAME_FROM_FAVORITES, cityName);


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteCities();
    }
}