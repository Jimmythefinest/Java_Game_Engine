package com.njst.gaming.assets.skinned;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.objects.Weighted_GameObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class WeightedMeshAssetIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WeightedMeshAssetIO() {
    }

    public static SkinnedMeshAsset fromGameObject(Weighted_GameObject gameObject) {
        if (gameObject == null) {
            throw new IllegalArgumentException("Weighted game object must not be null.");
        }
        return fromGeometry(gameObject.geo, gameObject.name);
    }

    public static SkinnedMeshAsset fromGeometry(WeightedGeometry geometry, String name) {
        if (geometry == null) {
            throw new IllegalArgumentException("Weighted geometry must not be null.");
        }
        SkinnedMeshAsset asset = new SkinnedMeshAsset();
        asset.name = name;
        asset.vertices = geometry.getVertices();
        asset.normals = geometry.getNormals();
        asset.textureCoordinates = geometry.getTextureCoordinates();
        asset.weights = geometry.getWeightss();
        asset.indices = geometry.getIndices();
        asset.boneIndices = geometry.getBoness();
        return asset;
    }

    public static WeightedGeometry toGeometry(SkinnedMeshAsset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Weighted mesh asset must not be null.");
        }
        return new WeightedGeometry(
                asset.vertices,
                asset.normals,
                asset.textureCoordinates,
                asset.weights,
                asset.indices,
                asset.boneIndices);
    }

    public static Weighted_GameObject loadObject(String path, int textureId) {
        return new Weighted_GameObject(loadGeometry(path), textureId);
    }

    public static WeightedGeometry loadGeometry(String path) {
        return toGeometry(load(path));
    }

    public static SkinnedMeshAsset load(String path) {
        try (FileReader reader = new FileReader(path)) {
            SkinnedMeshAsset asset = GSON.fromJson(reader, SkinnedMeshAsset.class);
            if (asset == null) {
                throw new IllegalArgumentException("No weighted mesh asset data found in " + path);
            }
            return asset;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load weighted mesh asset from " + path, e);
        }
    }

    public static void save(Weighted_GameObject gameObject, String path) {
        save(fromGameObject(gameObject), path);
    }

    public static void save(WeightedGeometry geometry, String name, String path) {
        save(fromGeometry(geometry, name), path);
    }

    public static void save(SkinnedMeshAsset asset, String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(asset, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save weighted mesh asset to " + path, e);
        }
    }
}
