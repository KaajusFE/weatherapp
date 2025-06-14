package com.example.weatherapp; // Ensure this matches your package name

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

// Implement the adapter's click listener interface
public class FavoritesActivity extends AppCompatActivity implements FavoriteCityAdapter.OnItemClickListener {

    private RecyclerView recyclerViewFavorites;
    private FavoriteCityAdapter adapter;
    private ArrayList<String> favoriteCitiesList;
    private TextView textViewNoFavorites;

    // --- IMPORTANT: Define SharedPreferences constants CONSISTENTLY ---
    // These MUST match the constants used in MainActivity for saving favorites.
    // It's best to define these in a shared constants file or make them public static in MainActivity.
    public static final String PREFS_NAME = "FavoriteCitiesPrefs"; // Example name
    public static final String FAVORITE_PREFIX = "favorite_";   // Example prefix
    // ---

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

        setupRecyclerView();
        // loadFavoriteCities(); // Will be called in onResume
    }

    private void setupRecyclerView() {
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' as the OnItemClickListener because FavoritesActivity implements it
        adapter = new FavoriteCityAdapter(this, favoriteCitiesList, this);
        recyclerViewFavorites.setAdapter(adapter);
    }

    private void loadFavoriteCities() {
        Log.d("FavoritesActivity", "Loading favorite cities...");
        favoriteCitiesList.clear(); // Clear the list before reloading

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
                // Ensure the value is a Boolean and is true
                if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                    String cityNameWithPrefix = entry.getKey();
                    String cityName = cityNameWithPrefix.substring(FAVORITE_PREFIX.length());

                    // Optional: Format the city name (e.g., capitalize first letter)
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

        Collections.sort(favoriteCitiesList); // Optional: Sort alphabetically

        updateUIBasedOnFavorites();
        adapter.notifyDataSetChanged(); // Notify the adapter about the new data
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

    // --- Implementation of FavoriteCityAdapter.OnItemClickListener ---
    @Override
    public void onItemClick(String cityName) {
        // Handle item click here.
        // For example, you could open MainActivity and pass the city name to display its weather.
        Toast.makeText(this, "Clicked: " + cityName, Toast.LENGTH_SHORT).show();

        // Example: Navigate back to MainActivity and tell it to search for this city
        // Intent intent = new Intent(this, MainActivity.class);
        // intent.putExtra("SEARCH_CITY_FROM_FAVORITES", cityName); // Use a unique key
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Clears other activities on top and brings MainActivity to front or creates new
        // startActivity(intent);
        // finish(); // Optional: finish FavoritesActivity after navigating
    }
    // ---

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load or refresh the list every time the activity is resumed
        // This ensures that if favorites are changed in MainActivity and the user returns,
        // the list here is up-to-date.
        loadFavoriteCities();
    }
}