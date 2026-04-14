package com.njst.gaming.objects;

import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.ShaderHandle;

public class Weighted_GameObject extends GameObject {
    private static final String SKINNED_VERTEX_SHADER = "shaders/vert111.glsl";
    private static final String SKINNED_FRAGMENT_SHADER = "shaders/frag111.glsl";

    public WeightedGeometry geo;
    ShaderHandle program1;
    public int boneBufferStartIndex = 0;

    public Weighted_GameObject(WeightedGeometry geo, int t) {
        super(geo, t);
        this.geo = geo;
    }

    public void generateBuffers() {
        int vaoId = graphicsDevice.createVertexArray();
        int[] vbos = graphicsDevice.createBuffers(6);
        graphicsDevice.bindVertexArray(vaoId);
        int vboId = vbos[0];
        int vboId1 = vbos[1];
        graphicsDevice.uploadArrayBufferFloat(vboId, geo.getVertices());
        graphicsDevice.uploadArrayBufferFloat(vboId1, geo.getNormals());
        graphicsDevice.uploadArrayBufferFloat(vbos[2], geo.getTextureCoordinates());
        graphicsDevice.uploadArrayBufferFloat(vbos[3], geo.getWeightss());
        graphicsDevice.uploadArrayBufferInt(vbos[4], geo.getBoness());

        graphicsDevice.setVertexAttribPointer(vboId, 0, 3);
        graphicsDevice.setVertexAttribPointer(vboId1, 1, 3);
        graphicsDevice.setVertexAttribPointer(vbos[2], 2, 2);
        graphicsDevice.setVertexAttribPointer(vbos[3], 3, 4);
        graphicsDevice.setVertexAttribIPointer(vbos[4], 4, 4);

        int eboId = vbos[5];
        graphicsDevice.uploadElementArrayBufferInt(eboId, geo.getIndices());

        graphicsDevice.bindVertexArray(0);
        vaoIds[0] = vaoId;
        program1 = graphicsDevice.createShaderProgram(
                graphicsDevice.loadShaderSource(SKINNED_VERTEX_SHADER),
                graphicsDevice.loadShaderSource(SKINNED_FRAGMENT_SHADER));
    }

    @Override
    public void render(ShaderHandle shaderprogram, int textureHandle) {
        program1.use();
        program1.setUniformVector3("properties", new Vector3(shininess, ambientlight_multiplier, 0));
        program1.setUniformMatrix4fv("uMMatrix", modelMatrix);
        program1.setUniformMatrix4fv("uLightSpaceMatrix", lightSpaceMatrix);
        program1.setUniformInt("uShadowEnabled", shadowsEnabled ? 1 : 0);
        program1.setUniformInt("boneStartIndex", boneBufferStartIndex);
        if (shadowsEnabled) {
            program1.activateTexture("uShadowMap", 5, shadowMapTexture);
        }
        program1.activateTexture(textureHandle, texture);

        graphicsDevice.bindVertexArray(vaoIds[0]);
        graphicsDevice.drawElementsTriangles(geo.getIndices().length);
        graphicsDevice.bindVertexArray(0);
        shaderprogram.use();
    }
}
