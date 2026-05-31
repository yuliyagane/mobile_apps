package com.example.drivedetector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;

    private TextView statusLabel;
    private TextView vibrationLabel;
    private TextView counterLabel;
    private ProgressBar progressBar;
    private Button startButton;
    private Button stopButton;

    private int vibrationCounter = 0;

    private boolean isDetecting = false;
    private boolean isReady = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusLabel = findViewById(R.id.statusLabel);
        vibrationLabel = findViewById(R.id.vibrationLabel);
        counterLabel = findViewById(R.id.counterLabel);
        progressBar = findViewById(R.id.progressBar);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        checkAccelerometerAvailability();
    }

    private void checkAccelerometerAvailability() {
        if (accelerometer == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Ошибка")
                    .setMessage("Акселерометр не поддерживается на этом устройстве.\n\nПриложение не может работать без акселерометра.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();

            startButton.setEnabled(false);
        }
    }

    public void startDetection(android.view.View view) {
        if (accelerometer == null) return;

        if (!isDetecting) {
            vibrationCounter = 0;
            isReady = false;
            updateUI();

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            isDetecting = true;

            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            statusLabel.setText("⏳ ПОДГОТОВКА...");
            statusLabel.setTextColor(getColor(R.color.orange));

            handler.postDelayed(() -> {
                if (isDetecting) {
                    isReady = true;
                    statusLabel.setText("🚀 СКАНИРОВАНИЕ...");
                    statusLabel.setTextColor(getColor(R.color.yellow));
                }
            }, 3000);
        }
    }

    public void stopDetection(android.view.View view) {
        if (isDetecting) {
            sensorManager.unregisterListener(this);

            isDetecting = false;
            isReady = false;
            vibrationCounter = 0;

            updateUI();

            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            statusLabel.setText("⏹ ОСТАНОВЛЕН");
            statusLabel.setTextColor(getColor(R.color.red));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isDetecting && isReady) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double total = Math.sqrt(x * x + y * y + z * z);

            double vibration = Math.abs(total - SensorManager.GRAVITY_EARTH);

            runOnUiThread(() -> vibrationLabel.setText(String.format("%.3f", vibration)));

            double THRESHOLD = 1.5;
            if (vibration > THRESHOLD) {
                vibrationCounter++;
            } else {
                vibrationCounter = Math.max(0, vibrationCounter - 2);
            }

            runOnUiThread(this::updateUI);

            if (vibrationCounter >= 25) {
                runOnUiThread(() -> {

                    statusLabel.setText("🚗 ПОЕЗДКА ОБНАРУЖЕНА!");
                    statusLabel.setTextColor(getColor(R.color.green));

                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                            500,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                            );
                        }
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("🚗 Поездка началась!")
                            .setMessage("Детектор зафиксировал устойчивые вибрации, соответствующие движению.")
                            .setPositiveButton("OK", (dialog, which) -> stopDetection(null))
                            .show();
                });
            }
        }
    }

    private void updateUI() {
        counterLabel.setText(vibrationCounter + "/25");

        progressBar.setMax(25);
        progressBar.setProgress(vibrationCounter);

        if (vibrationCounter >= 25) {
            progressBar.setProgressTintList(getColorStateList(R.color.green));
        } else if (vibrationCounter > 15) {
            progressBar.setProgressTintList(getColorStateList(R.color.orange));
        } else {
            progressBar.setProgressTintList(getColorStateList(R.color.cyan));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isDetecting) {
            sensorManager.unregisterListener(this);
        }

        handler.removeCallbacksAndMessages(null);
    }
}