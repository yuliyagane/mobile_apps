package com.example.dogbreedclassifier;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class DogBreedClassifier {

    private Interpreter tflite;
    private List<String> labels;

    public DogBreedClassifier(Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
        labels = loadLabels(context);
    }

    private ByteBuffer loadModelFile(Context context) throws IOException {
        byte[] buffer = new byte[Math.toIntExact(context.getAssets().openFd("dog_breed_classifier.tflite").getLength())];
        context.getAssets().open("dog_breed_classifier.tflite").read(buffer);
        ByteBuffer model = ByteBuffer.allocateDirect(buffer.length);
        model.order(ByteOrder.nativeOrder());
        model.put(buffer);
        model.rewind();
        return model;
    }

    private List<String> loadLabels(Context context) throws IOException {
        List<String> labels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }

    public String classify(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        ByteBuffer input = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        input.order(ByteOrder.nativeOrder());

        int[] pixels = new int[224 * 224];
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224);

        for (int pixel : pixels) {
            input.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            input.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            input.putFloat((pixel & 0xFF) / 255.0f);
        }

        float[][] output = new float[1][1000];
        tflite.run(input, output);

        int bestIndex = 0;
        float bestProb = output[0][0];
        for (int i = 1; i < labels.size(); i++) {
            if (output[0][i] > bestProb) {
                bestProb = output[0][i];
                bestIndex = i;
            }
        }

        return labels.get(bestIndex) + "\n" + String.format("%.1f%%", bestProb * 100);
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}