package com.njst.gaming.assets.skinned;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Loaders.FBXAnimationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public final class FbxAnimationExporter {
    private FbxAnimationExporter() {
    }

    public static Map<String, KeyframeAnimation> export(String fbxPath, int animationIndex, float scale) {
        return FBXAnimationLoader.extractAnimation(fbxPath, animationIndex, scale);
    }

    public static void exportToFile(String fbxPath, String outputPath, int animationIndex, float scale) {
        Map<String, KeyframeAnimation> animations = export(fbxPath, animationIndex, scale);
        File outputFile = new File(outputPath);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            outputStream.writeObject(new HashMap<>(animations));
        } catch (IOException e) {
            throw new RuntimeException("Failed to export animation file: " + outputPath, e);
        }
    }
}
