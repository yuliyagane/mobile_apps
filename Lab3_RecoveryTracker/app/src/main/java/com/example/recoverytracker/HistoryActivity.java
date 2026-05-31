package com.example.recoverytracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyHistory;
    private RecordAdapter adapter;
    private RecoveryDatabase database;
    private List<RecoveryRecord> records = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        database = RecoveryDatabase.getInstance(this);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadRecords();
    }

    private void loadRecords() {
        new Thread(() -> {
            records = database.recoveryDao().getAllRecords();
            runOnUiThread(() -> {
                if (records.isEmpty()) {
                    tvEmptyHistory.setVisibility(android.view.View.VISIBLE);
                    recyclerView.setVisibility(android.view.View.GONE);
                } else {
                    tvEmptyHistory.setVisibility(android.view.View.GONE);
                    recyclerView.setVisibility(android.view.View.VISIBLE);
                    adapter = new RecordAdapter(records, this::deleteRecord);
                    recyclerView.setAdapter(adapter);
                }
            });
        }).start();
    }

    private void deleteRecord(RecoveryRecord record) {
        new Thread(() -> {
            database.recoveryDao().deleteById(record.getId());
            runOnUiThread(() -> {
                loadRecords();
                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }
}