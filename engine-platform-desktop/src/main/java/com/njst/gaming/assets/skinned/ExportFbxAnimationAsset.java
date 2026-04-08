package com.njst.gaming.assets.skinned;

import com.njst.gaming.data;

import java.io.File;

public final class ExportFbxAnimationAsset {
    private static final String DEFAULT_SOURCE = data.rootDirectory + "/jump.fbx";
    private static final float DEFAULT_SCALE = 100.0f;
    private static final int DEFAULT_ANIMATION_INDEX = 0;

    private ExportFbxAnimationAsset() {
    }

    public static void main(String[] args) {
        String source = args.length > 0 ? args[0] : DEFAULT_SOURCE;
        String output = args.length > 1 ? args[1] : defaultOutputPath(source, DEFAULT_ANIMATION_INDEX);
        int animationIndex = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_ANIMATION_INDEX;
        float scale = args.length > 3 ? Float.parseFloat(args[3]) : DEFAULT_SCALE;

        if (args.length <= 1) {
            output = defaultOutputPath(source, animationIndex);
        }

        FbxAnimationExporter.exportToFile(source, output, animationIndex, scale);
        System.out.println("Exported animation " + animationIndex + " from " + source + " to " + output);
    }

    private static String defaultOutputPath(String source, int animationIndex) {
        File sourceFile = new File(source);
        String fileName = sourceFile.getName();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            fileName = fileName.substring(0, extensionIndex);
        }
        return data.rootDirectory
                + "/weighted_geometry/"
                + fileName.toLowerCase()
                + "_animation_"
                + animationIndex
                + ".ser";
    }
}
