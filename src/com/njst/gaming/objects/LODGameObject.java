package com.njst.gaming.objects;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Geometries.ImposterGeometry;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Renderer;

/**
 * A GameObject that automatically swaps between a high-poly 3D geometry
 * and a 2D imposter based on distance from the camera.
 */
public class LODGameObject extends GameObject {

    private final Geometry highPolyGeometry;
    private final ImposterGeometry imposterGeometry;
    private final int imposterTexture;
    private final float lodDistance;

    private boolean isFar = false;

    public LODGameObject(Geometry highPoly, int meshTexture, 
                         ImposterGeometry imposter, int imposterTexture, 
                         float lodDistance) {
        super(highPoly, meshTexture);
        this.highPolyGeometry = highPoly;
        this.imposterGeometry = imposter;
        this.imposterTexture = imposterTexture;
        this.lodDistance = lodDistance;
    }

    @Override
    public void render(ShaderProgram shader, int textureHandle) {
        // Calculate distance to camera
        // (Accessing renderer's camera which is accessible from shader context or passed)
        // Since we don't have easy access to the exact camera ref here, 
        // we'll assume the distance can be calculated from the shader's eyepos1 uniform
        // or we just use the renderer's static camera position if we can.
        
        // However, it's better to let the Renderer or Scene handle the LOD state updates.
        // For now, we'll implement a simple check.
        
        // We'll need a reference to the camera/renderer. 
        // Let's add a static or passed reference.
        
        // Actually, we can check a static global camera position if one exists,
        // or just calculate it in the scene loop.
        
        // Minimal approach: The renderer sets `eyepos1` uniform.
        // We can't easily read it back here.
        
        // Let's assume we can get the distance from a provided camera position.
        // (I'll modify this if the engine has a better way).
        
        if (isFar) {
            this.geometry = imposterGeometry;
            super.render(shader, imposterTexture); // Overrides the objects texture locally
        } else {
            this.geometry = highPolyGeometry;
            super.render(shader, texture);
        }
    }

    public void updateLOD(com.njst.gaming.Math.Vector3 cameraPos) {
        float dist = position.distance(cameraPos);
        isFar = (dist > lodDistance);

        if (isFar) {
            // Billboard: rotate to face camera on Y axis
            // Calculate direction from object to camera
            float dx = cameraPos.x - position.x;
            float dz = cameraPos.z - position.z;
            
            // Calculate angle in radians
            float angle = (float) Math.atan2(dx, dz);
            
            // Set rotation (Rotating around Y axis)
            // Note: Now using the public setRotation in the base GameObject class.
            this.setRotation(0, (float) Math.toDegrees(angle), 0);
        }
    }
}
