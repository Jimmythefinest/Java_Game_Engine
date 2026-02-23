package com.njst.gaming.objects;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Geometries.ImposterGeometry;
import com.njst.gaming.Natives.ShaderProgram;

/**
 * A GameObject that automatically swaps between a high-poly 3D geometry
 * and a 2D imposter based on distance from the camera.
 */
public class LODGameObject extends GameObject {

    private final Geometry highPolyGeometry;
    private final ImposterGeometry imposterGeometry;
    private final int imposterTexture;
    private final float lodDistance;

    public static boolean forceImposter = false;

    private boolean isFar = false;
    private boolean buffersMatchFarState = false;

    public LODGameObject(Geometry highPoly, int meshTexture,
            ImposterGeometry imposter, int imposterTexture,
            float lodDistance) {
        super(highPoly, meshTexture);
        this.highPolyGeometry = highPoly;
        this.imposterGeometry = imposter;
        this.imposterTexture = imposterTexture;
        this.lodDistance = lodDistance;
        this.geometry = highPolyGeometry;
        this.buffersMatchFarState = false;
    }

    @Override
    public void render(ShaderProgram shader, int textureHandle) {
        if (isFar != buffersMatchFarState) {
            cleanup();
            this.geometry = isFar ? imposterGeometry : highPolyGeometry;
            generateBuffers();
            buffersMatchFarState = isFar;
        }

        if (isFar) {
            int originalTexture = this.texture;
            this.texture = imposterTexture;
            super.render(shader, textureHandle);
            this.texture = originalTexture;
        } else {
            super.render(shader, textureHandle);
        }
    }

    public void updateLOD(com.njst.gaming.Math.Vector3 cameraPos) {
        if (forceImposter) {
            isFar = true;
        } else {
            float dist = position.distance(cameraPos);
            isFar = (dist > lodDistance);
        }

        if (isFar) {
            float dx = cameraPos.x - position.x;
            float dz = cameraPos.z - position.z;
            float angle = (float) Math.atan2(dx, dz);
            this.setRotation(0, (float) Math.toDegrees(angle), 0);
        }
    }
}
