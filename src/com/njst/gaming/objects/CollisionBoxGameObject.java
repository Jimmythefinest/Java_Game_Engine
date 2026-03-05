package com.njst.gaming.objects;

import com.njst.gaming.Geometries.CollisionBoxGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.GlUtils;
import com.njst.gaming.graphics.ShaderHandle;

public class CollisionBoxGameObject extends GameObject {
    private final GameObject target;
    private final CollisionBoxGeometry boxGeometry;

    public CollisionBoxGameObject(GameObject target, int texture) {
        super(new CollisionBoxGeometry(target.min, target.max), texture);
        this.target = target;
        this.boxGeometry = (CollisionBoxGeometry) this.geometry;
        this.name = target.name + "_CollisionBox";
        this.modelMatrix.identity();
        this.shininess = 0;
        this.ambientlight_multiplier = 1;
    }

    @Override
    public void updateModelMatrix() {
        modelMatrix.identity();
    }

    @Override
    public void render(ShaderHandle shader, int textureHandle) {
        if (target == null) {
            return;
        }

        if (vaoIds[0] == 0) {
            generateBuffers();
        }

        boxGeometry.setBounds(new Vector3(target.min), new Vector3(target.max));

        if (vboIds[0] != 0) {
            GlUtils.set_VBO_Float(vboIds[0], boxGeometry.getVertices());
        }

        if (shaderprogram == null) {
            shaderprogram = shader;
        }
        shaderprogram.setUniformVector3("properties", new Vector3(shininess, ambientlight_multiplier, 0));
        shaderprogram.setUniformMatrix4fv("uMMatrix", modelMatrix);
        shaderprogram.activateTexture(textureHandle, texture);

        GlUtils.bind_vertex_array(vaoIds[0]);
        GlUtils.DrawElements(GlUtils.GL_LINES, geometry.getIndices().length, GlUtils.GL_UNSIGNED_INT, 0);
        GlUtils.bind_vertex_array(0);
    }
}
