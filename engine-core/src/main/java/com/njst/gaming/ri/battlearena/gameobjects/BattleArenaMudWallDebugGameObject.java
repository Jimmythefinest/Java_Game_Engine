package com.njst.gaming.ri.battlearena.gameobjects;

import com.njst.gaming.Geometries.CollisionBoxGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.Bounds3;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.ri.battlearena.BattleArenaMudWallCollider;

public final class BattleArenaMudWallDebugGameObject extends GameObject {
    private final BattleArenaMudWallCollider collider;
    private final CollisionBoxGeometry boxGeometry;
    private boolean enabled;

    public BattleArenaMudWallDebugGameObject(BattleArenaMudWallCollider collider) {
        super(new CollisionBoxGeometry(new Vector3(-0.5f), new Vector3(0.5f)), 0);
        this.collider = collider;
        this.boxGeometry = (CollisionBoxGeometry) geometry;
        this.enabled = false;
        this.name = "MudWall_Debug";
        this.modelMatrix.identity();
        this.shininess = 0f;
        this.ambientlight_multiplier = 2.2f;
        this.castsShadows = false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void updateModelMatrix() {
        modelMatrix.identity();
    }

    @Override
    public void render(ShaderHandle shader, int textureHandle) {
        if (!enabled) {
            return;
        }

        if (vaoIds[0] == 0) {
            generateBuffers();
        }

        Bounds3 bounds = collider.getWorldBounds();
        boxGeometry.setBounds(bounds.getMin(), bounds.getMax());
        if (vboIds[0] != 0) {
            graphicsDevice.updateArrayBufferFloat(vboIds[0], boxGeometry.getVertices());
        }

        if (shaderprogram == null) {
            shaderprogram = shader;
        }
        shaderprogram.setUniformVector3("properties", new Vector3(shininess, ambientlight_multiplier, 0));
        shaderprogram.setUniformMatrix4fv("uMMatrix", modelMatrix);
        shaderprogram.activateTexture(textureHandle, texture);

        graphicsDevice.bindVertexArray(vaoIds[0]);
        graphicsDevice.drawElementsLines(geometry.getIndices().length);
        graphicsDevice.bindVertexArray(0);
    }
}
