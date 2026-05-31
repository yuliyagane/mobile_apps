package com.example.dogbreedclassifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView ivImage;
    private TextView tvBreed;
    private TextView tvProbability;
    private DogBreedClassifier classifier;

    // ВАЖНО: photoUri должен быть объявлен ПЕРЕД cameraLauncher
    private Uri photoUri;

    // Выбор из галереи
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadAndClassify(uri);
                } else {
                    tvBreed.setText("Фото не выбрано");
                }
            });

    // Камера - теперь photoUri уже существует
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && photoUri != null) {
                    loadAndClassify(photoUri);
                } else {
                    tvBreed.setText("Фото не сделано");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivImage = findViewById(R.id.ivImage);
        tvBreed = findViewById(R.id.tvBreed);
        tvProbability = findViewById(R.id.tvProbability);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);

        // Загрузка модели
        try {
            classifier = new DogBreedClassifier(this);
            tvBreed.setText("Выберите фото");
        } catch (Exception e) {
            tvBreed.setText("Ошибка: " + e.getMessage());
        }

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            } else {
                openCamera();
            }
        });

        btnGallery.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });
    }

    private void openCamera() {
        try {
            java.io.File photoFile = java.io.File.createTempFile("dog_photo", ".jpg", getCacheDir());
            photoUri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (Exception e) {
            tvBreed.setText("Ошибка: " + e.getMessage());
        }
    }

    private void loadAndClassify(Uri uri) {
        try {
            String filePath = getRealPathFromURI(uri);

            Bitmap bitmap;
            if (filePath != null) {
                bitmap = BitmapFactory.decodeFile(filePath);
            } else {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(inputStream);
            }

            if (bitmap == null) {
                tvBreed.setText("Не удалось загрузить изображение");
                return;
            }

            ivImage.setImageBitmap(bitmap);

            String result = classifier.classify(bitmap);
            String[] parts = result.split("\n");

            if (parts.length >= 2) {
                tvBreed.setText(parts[0]);
                tvProbability.setText(parts[1]);
            } else {
                tvBreed.setText(result);
                tvProbability.setText("");
            }

        } catch (Exception e) {
            tvBreed.setText("Ошибка: " + e.getMessage());
            tvProbability.setText("");
            e.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Нужно разрешение на камеру", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) classifier.close();
    }
}