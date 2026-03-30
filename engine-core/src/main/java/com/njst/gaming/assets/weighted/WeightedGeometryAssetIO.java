package com.njst.gaming.assets.weighted;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.njst.gaming.Geometries.WeightedGeometry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class WeightedGeometryAssetIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WeightedGeometryAssetIO() {
    }

    public static WeightedGeometryAsset fromGeometry(WeightedGeometry geometry, String name) {
        if (geometry == null) {
            throw new IllegalArgumentException("Weighted geometry must not be null.");
        }
        WeightedGeometryAsset asset = new WeightedGeometryAsset();
        asset.name = name;
        asset.vertices = geometry.getVertices();
        asset.normals = geometry.getNormals();
        asset.textureCoordinates = geometry.getTextureCoordinates();
        asset.weights = geometry.getWeightss();
        asset.indices = geometry.getIndices();
        asset.boneIds = geometry.getBoness();
        return asset;
    }

    public static WeightedGeometry toGeometry(WeightedGeometryAsset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Weighted geometry asset must not be null.");
        }
        return new WeightedGeometry(
                asset.vertices,
                asset.normals,
                asset.textureCoordinates,
                asset.weights,
                asset.indices,
                asset.boneIds);
    }

    public static WeightedGeometryAsset fromJson(String json, String sourceName) {
        WeightedGeometryAsset asset = GSON.fromJson(json, WeightedGeometryAsset.class);
        if (asset == null) {
            throw new IllegalArgumentException("No weighted geometry asset data found in " + sourceName);
        }
        return asset;
    }

    public static void save(WeightedGeometry geometry, String name, String path) {
        save(fromGeometry(geometry, name), path);
    }

    public static void save(WeightedGeometryAsset asset, String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(asset, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save weighted geometry asset to " + path, e);
        }
    }

    public static WeightedGeometryAsset load(String path) {
        try (FileReader reader = new FileReader(path)) {
            return fromJson(readerToString(reader), path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load weighted geometry asset from " + path, e);
        }
    }

    public static WeightedGeometry loadGeometry(String path) {
        return toGeometry(load(path));
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
