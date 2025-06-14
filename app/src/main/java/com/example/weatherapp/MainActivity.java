package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;

import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView cityNameText, temperatureText, humidityText, descriptionText, windText;
    private ImageView weatherIcon;
    private Button refreshButton;
    private EditText cityNameInput;
    private ImageView favoriteStarIcon;
    private boolean isCurrentCityFavorite = false;
    private String currentCityNameForFavorite = "";
    private static final String PREFS_NAME = "FavoriteCitiesPrefs";
    private static final String FAVORITE_PREFIX = "favorite_";
    private static final String API_KEY = "e4c23158ebedad33bf396578161b6123";
    private static final String DEFAULT_CITY = "Warsaw";
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private Button buttonGoToFavorites;
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchLocationAndWeather();
                } else {
                    Toast.makeText(this, "Location permission denied. Showing default city.", Toast.LENGTH_LONG).show();
                    FetchWeatherData(DEFAULT_CITY);
                }
            });
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cityNameText = findViewById(R.id.cityNameText);
        temperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        windText = findViewById(R.id.windText);
        descriptionText = findViewById(R.id.descriptionText);
        weatherIcon = findViewById(R.id.weatherIcon);
        refreshButton = findViewById(R.id.fetchWeatherButton);
        cityNameInput = findViewById(R.id.cityNameInput);
        favoriteStarIcon = findViewById(R.id.favoriteStarIcon);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        buttonGoToFavorites = findViewById(R.id.buttonGoToFavorites);

        buttonGoToFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start FavoritesActivity
                Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                startActivity(intent);
            }
        });
        refreshButton.setOnClickListener(v -> {
            String cityName = cityNameInput.getText().toString();
            if (!cityName.isEmpty()) {
                FetchWeatherData(cityName);
            } else {
                cityNameInput.setError("Please enter city name");
            }
        });

        favoriteStarIcon.setOnClickListener(v -> toggleFavoriteStatus());
        checkLocationPermissionAndFetchInitialWeather();


    }


    private void checkLocationPermissionAndFetchInitialWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(this, "Location permission is needed to show local weather.", Toast.LENGTH_LONG).show();
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void fetchLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted. Cannot fetch local weather.", Toast.LENGTH_LONG).show();
            FetchWeatherData(DEFAULT_CITY);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        androidx.media3.common.util.Log.d("Location", "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        getCityNameAndFetchWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        androidx.media3.common.util.Log.w("Location", "Last known location is null.");
                        Toast.makeText(MainActivity.this, "Could not get current location. Showing default city.", Toast.LENGTH_SHORT).show();
                        FetchWeatherData(DEFAULT_CITY);
                    }
                })
                .addOnFailureListener(this, e -> {
                    androidx.media3.common.util.Log.e("Location", "Error getting location", e);
                    Toast.makeText(MainActivity.this, "Error getting location. Showing default city.", Toast.LENGTH_SHORT).show();
                    FetchWeatherData(DEFAULT_CITY);
                });
    }


    @OptIn(markerClass = UnstableApi.class)
    private void getCityNameAndFetchWeather(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        String cityName = addresses.get(0).getLocality();
                        if (cityName != null && !cityName.isEmpty()) {
                            androidx.media3.common.util.Log.d("Geocoder", "City found: " + cityName);
                            FetchWeatherData(cityName);
                        } else {
                            String adminArea = addresses.get(0).getSubAdminArea();
                            if (adminArea != null && !adminArea.isEmpty()) {
                                androidx.media3.common.util.Log.d("Geocoder", "City (SubAdminArea) found: " + adminArea);
                                FetchWeatherData(adminArea);
                            } else {
                                androidx.media3.common.util.Log.w("Geocoder", "City name not found via Geocoder.");
                                Toast.makeText(MainActivity.this, "Could not determine city name. Showing default.", Toast.LENGTH_SHORT).show();
                                FetchWeatherData(DEFAULT_CITY);
                            }
                        }
                    } else {
                        androidx.media3.common.util.Log.w("Geocoder", "No address found for coordinates.");
                        Toast.makeText(MainActivity.this, "Could not determine city from location. Showing default.", Toast.LENGTH_SHORT).show();
                        FetchWeatherData(DEFAULT_CITY);
                    }
                });
            } else {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        final String[] determinedCityName = {null};
                        if (addresses != null && !addresses.isEmpty()) {
                            determinedCityName[0] = addresses.get(0).getLocality();
                            if (determinedCityName[0] == null || determinedCityName[0].isEmpty()) {
                                determinedCityName[0] = addresses.get(0).getSubAdminArea();
                            }
                        }
                        runOnUiThread(() -> {
                            if (determinedCityName[0] != null && !determinedCityName[0].isEmpty()) {
                                androidx.media3.common.util.Log.d("Geocoder", "City found: " + determinedCityName[0]);
                                FetchWeatherData(determinedCityName[0]);
                            } else {
                                androidx.media3.common.util.Log.w("Geocoder", "City name not found via Geocoder.");
                                Toast.makeText(MainActivity.this, "Could not determine city name. Showing default.", Toast.LENGTH_SHORT).show();
                                FetchWeatherData(DEFAULT_CITY);
                            }
                        });
                    } catch (IOException ioException) {
                        androidx.media3.common.util.Log.e("Geocoder", "Geocoder service not available or error", ioException);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Geocoder error. Showing default city.", Toast.LENGTH_SHORT).show();
                            FetchWeatherData(DEFAULT_CITY);
                        });
                    }
                });
            }
        } catch (Exception e) {
            androidx.media3.common.util.Log.e("Geocoder", "Error using Geocoder", e);
            Toast.makeText(MainActivity.this, "Error determining location. Showing default city.", Toast.LENGTH_SHORT).show();
            FetchWeatherData(DEFAULT_CITY);
        }
    }

    private void FetchWeatherData(String cityName) {
        currentCityNameForFavorite = cityName;
        isCurrentCityFavorite = checkIsFavorite(cityName);
        updateFavoriteStarIcon();
        String url = "https:api.openweathermap.org/data/2.5/weather?q="+cityName + "&appid="+ API_KEY + "&units=metric";
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() ->
                {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    try{
                        Response response = client.newCall(request).execute();
                        String result = response.body().string();
                        runOnUiThread(() -> updateUI(result));
                    } catch (IOException e) {
                        e.printStackTrace();;
                    }
                }
                );
    }
    private void toggleFavoriteStatus() {
        if (currentCityNameForFavorite.isEmpty()) {
            Toast.makeText(this, "Please search for a city first", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        boolean isFavorite = checkIsFavorite(currentCityNameForFavorite);

        if (isFavorite) {
            // It was a favorite, so remove it
            editor.remove(FAVORITE_PREFIX + currentCityNameForFavorite);
            Toast.makeText(this, currentCityNameForFavorite + " removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            // It was not a favorite, so add it
            editor.putBoolean(FAVORITE_PREFIX + currentCityNameForFavorite, true);
            Toast.makeText(this, currentCityNameForFavorite + " added to favorites", Toast.LENGTH_SHORT).show();
        }
        editor.apply();
        updateFavoriteStarIcon();
    }

    private void updateFavoriteStarIcon() {
        if (currentCityNameForFavorite.isEmpty()) {
            favoriteStarIcon.setVisibility(View.INVISIBLE);
            return;
        }
        favoriteStarIcon.setVisibility(View.VISIBLE);

        if (checkIsFavorite(currentCityNameForFavorite)) {
            favoriteStarIcon.setImageResource(R.drawable.ic_star_favorite2);
        } else {
            favoriteStarIcon.setImageResource(R.drawable.ic_star_favorite1);
        }
    }


        private boolean checkIsFavorite(java.lang.String cityName) {
            if (cityName == null || cityName.isEmpty()) {
                return false;
            }
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            return prefs.getBoolean(FAVORITE_PREFIX + cityName.trim().toLowerCase(), false);
        }
    private void updateUI(String result) {
        if(result != null){
            try {


                JSONObject jsonObject = new JSONObject(result);
                String fetchedCityName = jsonObject.getString("name");
                currentCityNameForFavorite = fetchedCityName.trim().toLowerCase();


                updateFavoriteStarIcon();

                JSONObject main = jsonObject.getJSONObject("main");
                double temperature = main.getDouble("temp");
                double humidity = main.getDouble("humidity");
                double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");

                String description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                String iconCode = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
                String resourceName = "ic_" + iconCode;
                int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                weatherIcon.setImageResource(resId);

                cityNameText.setText(jsonObject.getString("name"));
                if (temperature > 20) {
                    temperatureText.setTextColor(getResources().getColor(R.color.red, getTheme()));
                } else if (temperature > 10) {
                    temperatureText.setTextColor(getResources().getColor(R.color.yellow, getTheme()));
                }else {
                    temperatureText.setTextColor(getResources().getColor(R.color.default_text_color, getTheme()));
                }

                temperatureText.setText(String.format("%.0f°", temperature));
                humidityText.setText(String.format("%.0f%%", humidity));
                windText.setText(String.format("%.0f km/h", windSpeed));
                descriptionText.setText(description);
            } catch (JSONException e)
            {
                e.printStackTrace();
                Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                favoriteStarIcon.setVisibility(View.INVISIBLE);
            }
        }
    }

}