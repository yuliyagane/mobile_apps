package com.example.incidentmap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private AppDatabase database;
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private TextView tvCount;
    private RecyclerView recyclerView;
    private IncidentAdapter adapter;
    private LinearLayout bottomCard;
    private FloatingActionButton fabList;
    private Button btnCloseList;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String MY_HOME_ADDRESS = "Шота Руставели 27, Уфа";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        database = androidx.room.Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "incident_db"
        ).allowMainThreadQueries().build();

        initMap();
        initViews();
        loadIncidentsAndUpdateCounter();
        checkPermission();

        new Handler().postDelayed(() -> moveToMyHome(), 1000);
    }

    private void moveToMyHome() {
        Toast.makeText(this, "🔍 Ищем ваше местоположение...", Toast.LENGTH_LONG).show();

        executor.execute(() -> {
            try {
                String encodedAddress = URLEncoder.encode(MY_HOME_ADDRESS, "UTF-8");
                String urlString = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress +
                        "&format=json&limit=1";

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

                    org.json.JSONArray jsonArray = new org.json.JSONArray(jsonResponse);
                    if (jsonArray.length() > 0) {
                        JSONObject firstResult = jsonArray.getJSONObject(0);
                        double lat = firstResult.getDouble("lat");
                        double lon = firstResult.getDouble("lon");
                        String displayName = firstResult.getString("display_name");

                        GeoPoint homePoint = new GeoPoint(lat, lon);

                        mainHandler.post(() -> {
                            mapView.getController().animateTo(homePoint);
                            mapView.getController().setZoom(18.0);

                            Toast.makeText(MainActivity.this,
                                    "🏠 Ваше местоположение найдено!",
                                    Toast.LENGTH_LONG).show();

                            addHomeMarker(homePoint, displayName);
                        });
                        return;
                    }
                }

                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "❌ Местоположение не найдено, остаёмся в центре", Toast.LENGTH_LONG).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка поиска адреса: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void addHomeMarker(GeoPoint point, String address) {
        for (int i = 0; i < mapView.getOverlays().size(); i++) {
            org.osmdroid.views.overlay.Overlay overlay = mapView.getOverlays().get(i);
            if (overlay instanceof Marker && ((Marker) overlay).getTitle() != null
                    && ((Marker) overlay).getTitle().equals("🏠 Мой дом")) {
                mapView.getOverlays().remove(i);
                break;
            }
        }

        Marker homeMarker = new Marker(mapView);
        homeMarker.setPosition(point);
        homeMarker.setTitle("🏠 Мой дом");
        homeMarker.setSnippet(address.split(",")[0]);
        homeMarker.setIcon(getResources().getDrawable(android.R.drawable.btn_star_big_on, null));
        homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(homeMarker);
        mapView.invalidate();
    }

    private void initViews() {
        tvCount = findViewById(R.id.tvCount);
        bottomCard = findViewById(R.id.bottomCard);
        recyclerView = findViewById(R.id.recyclerView);
        btnCloseList = findViewById(R.id.btnCloseList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidentAdapter(incident -> {
            GeoPoint point = new GeoPoint(incident.latitude, incident.longitude);
            mapView.getController().animateTo(point);
            mapView.getController().setZoom(18.0);
            bottomCard.setVisibility(android.view.View.GONE);
        });
        recyclerView.setAdapter(adapter);

        btnCloseList.setOnClickListener(v -> bottomCard.setVisibility(android.view.View.GONE));

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            showAddIncidentDialog(center);
        });

        fabList = findViewById(R.id.fabList);
        fabList.setOnClickListener(v -> {
            loadIncidentsToList();
            bottomCard.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void loadIncidentsToList() {
        List<IncidentEntity> incidents = database.incidentDao().getAllIncidents();
        adapter.setIncidents(incidents);
    }

    private void initMap() {
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(14.0);

        GeoPoint ufa = new GeoPoint(54.7349, 55.9579);
        mapView.getController().setCenter(ufa);

        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        locationOverlay = new MyLocationNewOverlay(provider, mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);

        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint point) {
                showAddIncidentDialog(point);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint point) {
                return false;
            }
        };

        MapEventsOverlay overlay = new MapEventsOverlay(receiver);
        mapView.getOverlays().add(overlay);
    }

    private void showAddIncidentDialog(GeoPoint selectedPoint) {
        String[] types = {"🚗 ДТП / Авария", "🚧 Ремонт дороги", "🕳 Яма / Выбоина", "⚠ Другое"};
        String[] typeKeys = {"accident", "repair", "hole", "other"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите тип");
        builder.setItems(types, (dialog, which) -> {
            String type = typeKeys[which];
            String typeName = types[which];

            android.widget.EditText editText = new android.widget.EditText(this);
            editText.setHint("Описание");

            new AlertDialog.Builder(this)
                    .setTitle(typeName)
                    .setView(editText)
                    .setPositiveButton("Добавить", (d, w) -> {
                        String description = editText.getText().toString();
                        if (description.isEmpty()) description = "Без описания";
                        addIncident(type, typeName, description, selectedPoint);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        builder.show();
    }

    private void addIncident(String type, String typeName, String description, GeoPoint point) {
        IncidentEntity entity = new IncidentEntity(
                type, typeName, description, point.getLatitude(),
                point.getLongitude(), System.currentTimeMillis()
        );

        database.incidentDao().insert(entity);

        addMarkerToMap(type, typeName, description, point);

        int newCount = database.incidentDao().getCount();
        tvCount.setText(newCount + " происшествий");

        Toast.makeText(this, "✅ Добавлено", Toast.LENGTH_SHORT).show();
    }

    private void addMarkerToMap(String type, String typeName, String description, GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(typeName);
        marker.setSnippet(description);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        switch (type) {
            case "accident":
                marker.setIcon(getResources().getDrawable(R.drawable.ic_accident, null));
                break;
            case "repair":
                marker.setIcon(getResources().getDrawable(R.drawable.ic_remont, null));
                break;
            case "hole":
                marker.setIcon(getResources().getDrawable(R.drawable.ic_warning, null));
                break;
            default:
                marker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation, null));
        }

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void checkPermission() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationOverlay.enableMyLocation();
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Нужно разрешение GPS", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void loadIncidentsAndUpdateCounter() {
        List<IncidentEntity> savedIncidents = database.incidentDao().getAllIncidents();
        int count = database.incidentDao().getCount();
        tvCount.setText(count + " происшествий");

        for (int i = mapView.getOverlays().size() - 1; i >= 0; i--) {
            org.osmdroid.views.overlay.Overlay overlay = mapView.getOverlays().get(i);
            if (overlay instanceof Marker && overlay != locationOverlay) {
                Marker m = (Marker) overlay;
                if (m.getTitle() == null || !m.getTitle().equals("🏠 Мой дом")) {
                    mapView.getOverlays().remove(i);
                }
            }
        }
        
        for (IncidentEntity incident : savedIncidents) {
            GeoPoint point = new GeoPoint(incident.latitude, incident.longitude);
            addMarkerToMap(incident.type, incident.title, incident.description, point);
        }

        mapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        loadIncidentsAndUpdateCounter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}