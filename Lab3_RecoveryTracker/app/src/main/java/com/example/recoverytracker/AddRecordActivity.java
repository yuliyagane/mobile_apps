package com.example.recoverytracker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AddRecordActivity extends AppCompatActivity {

    private SeekBar seekBarPain, seekBarActivity;
    private TextView tvPainLabel, tvActivityLabel;
    private EditText etMedications, etNotes;
    private MaterialButton btnSave, btnCancel;
    private RecoveryDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        database = RecoveryDatabase.getInstance(this);

        seekBarPain = findViewById(R.id.seekBarPain);
        seekBarActivity = findViewById(R.id.seekBarActivity);
        tvPainLabel = findViewById(R.id.tvPainLabel);
        tvActivityLabel = findViewById(R.id.tvActivityLabel);
        etMedications = findViewById(R.id.etMedications);
        etNotes = findViewById(R.id.etNotes);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        seekBarPain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvPainLabel.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarActivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvActivityLabel.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSave.setOnClickListener(v -> saveRecord());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveRecord() {
        int pain = seekBarPain.getProgress();
        int activity = seekBarActivity.getProgress();
        String medications = etMedications.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (medications.isEmpty()) medications = "—";
        if (notes.isEmpty()) notes = "—";

        RecoveryRecord record = new RecoveryRecord(System.currentTimeMillis(), pain, activity, medications, notes);

        new Thread(() -> {
            database.recoveryDao().insert(record);
            runOnUiThread(() -> {
                Toast.makeText(this, "✅ Запись сохранена!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}