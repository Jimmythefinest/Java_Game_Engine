package com.njst.gaming.android;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class AndroidAssetLoader {
    private static final String TAG = "NJST";
    private static String externalRoot;
    private AndroidAssetLoader() {
    }

    static void setExternalRoot(String externalRoot) {
        if (externalRoot == null || externalRoot.trim().isEmpty()) {
            AndroidAssetLoader.externalRoot = null;
            return;
        }
        AndroidAssetLoader.externalRoot = externalRoot.replace('\\', '/');
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

    static InputStream openExternalStream(String filePath) throws IOException {
        File file = resolveExternalFile(filePath);
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        return new FileInputStream(file);
    }

    private static File resolveExternalFile(String filePath) {
        if (externalRoot == null || externalRoot.isEmpty() || filePath == null || filePath.isEmpty()) {
            return null;
        }
        String normalized = normalizeResourcePath(filePath);
        File root = new File(externalRoot);
        if (!root.isAbsolute()) {
            return new File(root, normalized);
        }
        return new File(root, normalized);
    }

    static byte[] readBytes(AssetManager assetManager, String filePath) {
        String normalized = normalizeResourcePath(filePath);
        Log.i(TAG, "Reading binary asset: " + normalized);
        try (InputStream externalStream = openExternalStream(filePath)) {
            if (externalStream != null) {
                byte[] value = readAllBytes(externalStream);
                Log.i(TAG, "Loaded external binary asset: " + normalized + " (" + value.length + " bytes)");
                return value;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load external binary asset: " + normalized, e);
        }
        try (InputStream inputStream = assetManager.open(normalized);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            byte[] value = outputStream.toByteArray();
            Log.i(TAG, "Loaded binary asset: " + normalized + " (" + value.length + " bytes)");
            return value;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load Android binary asset: " + normalized, e);
            throw new IllegalStateException("Unable to load Android binary asset: " + normalized, e);
        }
    }

    static String readText(AssetManager assetManager, String filePath) {
        String normalized = normalizeResourcePath(filePath);
        Log.i(TAG, "Reading text asset: " + normalized);
        try (InputStream externalStream = openExternalStream(filePath)) {
            if (externalStream != null) {
                String value = readAllText(externalStream);
                Log.i(TAG, "Loaded external text asset: " + normalized + " (" + value.length() + " chars)");
                return value;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load external text asset: " + normalized, e);
        }
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

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private static String readAllText(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
            return text.toString();
        }
    }
}
