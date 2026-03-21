package com.njst.gaming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class OpenWorldTerrainState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public long seed;
    public int chunkSize;
    public int renderDistance;
    public float noiseScale;
    public float heightScale;

    public OpenWorldTerrainState() {
    }

    public static OpenWorldTerrainState loadOrCreate(String path) {
        File file = new File(path);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                OpenWorldTerrainState state = GSON.fromJson(reader, OpenWorldTerrainState.class);
                if (state != null) {
                    state.applyDefaults();
                    return state;
                }
            } catch (IOException ignored) {
            }
        }

        OpenWorldTerrainState state = defaultState();
        state.save(path);
        return state;
    }

    public void save(String path) {
        applyDefaults();
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save open world state to " + path, e);
        }
    }

    private static OpenWorldTerrainState defaultState() {
        OpenWorldTerrainState state = new OpenWorldTerrainState();
        state.seed = new Random().nextLong();
        state.chunkSize = 32;
        state.renderDistance = 2;
        state.noiseScale = 48f;
        state.heightScale = 12f;
        return state;
    }

    private void applyDefaults() {
        if (chunkSize <= 0) {
            chunkSize = 32;
        }
        if (renderDistance <= 0) {
            renderDistance = 2;
        }
        if (noiseScale <= 0) {
            noiseScale = 48f;
        }
        if (heightScale <= 0) {
            heightScale = 12f;
        }
        if (seed == 0L) {
            seed = new Random().nextLong();
        }
    }
}
