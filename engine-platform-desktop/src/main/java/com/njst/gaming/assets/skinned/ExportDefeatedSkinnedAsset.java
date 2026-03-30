package com.njst.gaming.assets.skinned;

import com.njst.gaming.data;

public final class ExportDefeatedSkinnedAsset {
    private static final String DEFAULT_SOURCE = data.rootDirectory + "/Defeated.fbx";
    private static final String DEFAULT_TEXTURE = "j.jpg";
    private static final String DEFAULT_OUTPUT = data.rootDirectory + "/skinned/defeated.model.json";

    private ExportDefeatedSkinnedAsset() {
    }

    public static void main(String[] args) {
        String source = args.length > 0 ? args[0] : DEFAULT_SOURCE;
        String texture = args.length > 1 ? args[1] : DEFAULT_TEXTURE;
        String output = args.length > 2 ? args[2] : DEFAULT_OUTPUT;
        float scale = args.length > 3 ? Float.parseFloat(args[3]) : 100.0f;

        FbxSkinnedModelExporter.exportToFile(source, texture, output, scale);
        System.out.println("Exported skinned model asset to " + output);
    }
}
