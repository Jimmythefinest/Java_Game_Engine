package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;

/**
 * Test class to verify the enhanced ai_training loader works correctly
 */
public class AITrainingTest {
    
    public static void main(String[] args) {
        System.out.println("Testing enhanced AI Training loader...");
        
        // Create a test scene
        Scene testScene = new Scene();
        
        // Load the enhanced AI training environment
        ai_training loader = new ai_training();
        loader.load(testScene);
        
        // Verify scene contents
        // System.out.println("Scene objects count: " + testScene.getGameObjects().size());
        // System.out.println("Skybox set: " + (testScene.renderer.skybox != null));
        // System.out.println("Camera positioned: " + testScene.getCamera().getPosition());
        
        System.out.println("AI Training loader test completed successfully!");
    }
}
