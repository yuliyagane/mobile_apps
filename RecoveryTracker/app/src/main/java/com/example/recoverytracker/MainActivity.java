package com.example.recoverytracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.*;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView tvAvgPain, tvAvgActivity, tvTotalRecords;
    private RecyclerView recyclerView;
    private RecordAdapter adapter;
    private RecoveryDatabase database;
    private List<RecoveryRecord> records = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = RecoveryDatabase.getInstance(this);

        tvAvgPain = findViewById(R.id.tvAvgPain);
        tvAvgActivity = findViewById(R.id.tvAvgActivity);
        tvTotalRecords = findViewById(R.id.tvTotalRecords);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        MaterialButton btnAdd = findViewById(R.id.btnAdd);
        MaterialButton btnHistory = findViewById(R.id.btnHistory);
        MaterialButton btnReminders = findViewById(R.id.btnReminders);

        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AddRecordActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnReminders.setOnClickListener(v -> setupReminders());

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            records = database.recoveryDao().getLast5Records();
            int count = database.recoveryDao().getCount();

            // Считаем средние значения по ВСЕМ записям
            List<RecoveryRecord> allRecords = database.recoveryDao().getAllRecords();
            int sumPain = 0, sumActivity = 0;
            for (RecoveryRecord r : allRecords) {
                sumPain += r.getPainLevel();
                sumActivity += r.getActivityLevel();
            }

            final int avgPain = allRecords.isEmpty() ? 0 : sumPain / allRecords.size();
            final int avgActivity = allRecords.isEmpty() ? 0 : sumActivity / allRecords.size();
            final int totalCount = count;

            runOnUiThread(() -> {
                tvAvgPain.setText(String.valueOf(avgPain));
                tvAvgActivity.setText(String.valueOf(avgActivity));
                tvTotalRecords.setText(String.valueOf(totalCount));

                adapter = new RecordAdapter(records, this::deleteRecord);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    private void deleteRecord(RecoveryRecord record) {
        new Thread(() -> {
            database.recoveryDao().deleteById(record.getId());
            runOnUiThread(() -> {
                loadData();
                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void setupReminders() {
        // Запрос разрешения для Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        // Напоминание о лекарствах в 20:00
        long delayMedication = getDelayForTime(20, 0);
        WorkRequest medicationWork = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delayMedication, TimeUnit.MILLISECONDS)
                .setInputData(new androidx.work.Data.Builder().putString("type", "medication").build())
                .build();

        // Напоминание о записи в 21:00
        long delayRecord = getDelayForTime(21, 0);
        WorkRequest recordWork = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delayRecord, TimeUnit.MILLISECONDS)
                .setInputData(new androidx.work.Data.Builder().putString("type", "record").build())
                .build();

        WorkManager.getInstance(this).enqueue(medicationWork);
        WorkManager.getInstance(this).enqueue(recordWork);

        Toast.makeText(this, "🔔 Напоминания включены!\nЛекарства: 20:00\nЗапись: 21:00", Toast.LENGTH_LONG).show();
    }

    private long getDelayForTime(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis() - System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}