package com.njst.gaming.android;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class AndroidAssetLoader {
    private static final String TAG = "NJST";
    private AndroidAssetLoader() {
    }

    static String normalizeResourcePath(String filePath) {
        String normalized = filePath.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("resources/")) {
            normalized = normalized.substring("resources/".length());
        }
        return normalized;
    }

    static String readText(AssetManager assetManager, String filePath) {
        String normalized = normalizeResourcePath(filePath);
        Log.i(TAG, "Reading text asset: " + normalized);
        try (InputStream inputStream = assetManager.open(normalized);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
            String value = text.toString();
            Log.i(TAG, "Loaded text asset: " + normalized + " (" + value.length() + " chars)");
            return value;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load Android text asset: " + normalized, e);
            throw new IllegalStateException("Unable to load Android asset: " + normalized, e);
        }
    }
}
