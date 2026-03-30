package com.njst.gaming.assets.skinned;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class SkinnedModelAssetIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SkinnedModelAssetIO() {
    }

    public static SkinnedModelAsset load(String path) {
        try (FileReader reader = new FileReader(path)) {
            return fromJson(readerToString(reader), path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load skinned model asset from " + path, e);
        }
    }

    public static SkinnedModelAsset fromJson(String json, String sourceName) {
        SkinnedModelAsset asset = GSON.fromJson(json, SkinnedModelAsset.class);
        if (asset == null) {
            throw new IllegalArgumentException("No skinned model asset data found in " + sourceName);
        }
        return asset;
    }

    public static void save(SkinnedModelAsset asset, String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(asset, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save skinned model asset to " + path, e);
        }
    }

    private static String readerToString(FileReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[4096];
        int readCount;
        while ((readCount = reader.read(buffer)) >= 0) {
            content.append(buffer, 0, readCount);
        }
        return content.toString();
    }
}
