package com.example.weatherapp; // Ensure this matches your package name

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoriteCityAdapter extends RecyclerView.Adapter<FavoriteCityAdapter.ViewHolder> {

    private List<String> favoriteCities;
    private Context context;
    private OnItemClickListener listener; // Your custom click listener interface

    // Define the interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(String cityName);
    }

    public FavoriteCityAdapter(Context context, List<String> favoriteCities, OnItemClickListener listener) {
        this.context = context;
        this.favoriteCities = favoriteCities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Correctly inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite_city, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String cityName = favoriteCities.get(position);
        holder.textViewFavoriteCityName.setText(cityName); // This was the line from the previous error

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(cityName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteCities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewFavoriteCityName; // Make sure this ID is correct

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure R.id.textViewFavoriteCityName matches the ID in item_favorite_city.xml
            textViewFavoriteCityName = itemView.findViewById(R.id.textViewFavoriteCityName);
        }
    }

    // Helper to update data
    public void updateData(List<String> newFavoriteCities) {
        this.favoriteCities.clear();
        this.favoriteCities.addAll(newFavoriteCities);
        notifyDataSetChanged();
    }
}