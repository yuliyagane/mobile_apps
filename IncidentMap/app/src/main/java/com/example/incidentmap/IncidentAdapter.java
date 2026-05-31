package com.example.incidentmap;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {
    private List<IncidentEntity> incidents = new ArrayList<>();
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());

    // Кэш для адресов
    private Map<Integer, String> addressCache = new HashMap<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnItemClickListener {
        void onItemClick(IncidentEntity incident);
    }

    public IncidentAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setIncidents(List<IncidentEntity> incidents) {
        this.incidents = incidents;
        notifyDataSetChanged();
        loadAllAddresses(); // Загружаем адреса для всех происшествий
    }

    private void loadAllAddresses() {
        for (IncidentEntity incident : incidents) {
            // Если адрес ещё не загружен
            if (!addressCache.containsKey(incident.id)) {
                loadAddressForIncident(incident);
            }
        }
    }

    private void loadAddressForIncident(IncidentEntity incident) {
        executor.execute(() -> {
            try {
                String address = getAddressFromCoordinates(incident.latitude, incident.longitude);
                addressCache.put(incident.id, address);
                mainHandler.post(() -> {
                    // Обновляем только конкретный элемент
                    int position = incidents.indexOf(incident);
                    if (position != -1) {
                        notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                addressCache.put(incident.id, "📍 Координаты: " +
                        String.format("%.4f", incident.latitude) + ", " +
                        String.format("%.4f", incident.longitude));
                mainHandler.post(() -> {
                    int position = incidents.indexOf(incident);
                    if (position != -1) {
                        notifyItemChanged(position);
                    }
                });
            }
        });
    }

    private String getAddressFromCoordinates(double lat, double lon) throws Exception {
        String urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon + "&zoom=18&addressdetails=1";

        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "IncidentMapApp/1.0");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        String jsonResponse = response.toString();

        if (jsonResponse != null && jsonResponse.length() > 2) {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject address = json.optJSONObject("address");

            if (address != null) {
                String road = address.optString("road", "");
                String houseNumber = address.optString("house_number", "");
                String pedestrian = address.optString("pedestrian", "");
                String suburb = address.optString("suburb", "");
                String city = address.optString("city", "");
                String town = address.optString("town", "");
                String village = address.optString("village", "");

                StringBuilder result = new StringBuilder();

                if (!road.isEmpty()) {
                    result.append(road);
                } else if (!pedestrian.isEmpty()) {
                    result.append(pedestrian);
                }

                if (!houseNumber.isEmpty()) {
                    if (result.length() > 0) result.append(", ");
                    result.append(houseNumber);
                }

                if (result.length() == 0) {
                    if (!suburb.isEmpty()) {
                        result.append(suburb);
                    } else if (!city.isEmpty()) {
                        result.append(city);
                    } else if (!town.isEmpty()) {
                        result.append(town);
                    } else if (!village.isEmpty()) {
                        result.append(village);
                    }
                }

                if (result.length() > 0) {
                    return "📍 " + result.toString();
                }
            }
        }

        return "📍 " + String.format("%.4f", lat) + ", " + String.format("%.4f", lon);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incident, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IncidentEntity incident = incidents.get(position);

        holder.tvTypeIcon.setText(getTypeIcon(incident.type));
        holder.tvTitle.setText(incident.title);

        String address = addressCache.get(incident.id);
            holder.tvAddress.setText(address);
            holder.tvAddress.setVisibility(View.VISIBLE);

        holder.tvDescription.setText(incident.description);
        holder.tvDate.setText(dateFormat.format(new Date(incident.createAt)));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(incident));
    }

    private String getTypeIcon(String type) {
        switch (type) {
            case "accident": return "🚗";
            case "repair": return "🚧";
            case "hole": return "🕳";
            default: return "⚠️";
        }
    }

    @Override
    public int getItemCount() {
        return incidents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTypeIcon, tvTitle, tvAddress, tvDescription, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTypeIcon = itemView.findViewById(R.id.tvTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}