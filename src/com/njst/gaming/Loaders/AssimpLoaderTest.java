package com.njst.gaming.Loaders;

import com.njst.gaming.Bone;

public class AssimpLoaderTest {
    public static void main(String[] args) {
        // Replace with the actual path to your FBX file
        // String filePath = data.rootDirectory+"/test.fbx"; // Update this path
        // accordingly

        // Load the FBX file
        // Bone rootBone =FBXBoneLoader.loadBones(filePath);

        // Print the loaded bone hierarchy
        // printBoneHierarchy(rootBone, 0);
    }

    public static void printBoneHierarchy(Bone bone, int depth) {
        // Print the bone name with indentation based on depth
        String indent = "  ".repeat(depth);
        System.out.println(indent + bone.name);

        // Recursively print child bones
        for (Bone child : bone.Children) {
            printBoneHierarchy(child, depth + 1);
        }
    }
}
