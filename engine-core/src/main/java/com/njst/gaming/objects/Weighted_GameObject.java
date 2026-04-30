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
    private boolean renderStateLogged = false;

    public Weighted_GameObject(WeightedGeometry geo, int t) {
        super(geo, t);
        this.geo = geo;
    }

    public void generateBuffers() {
        log("generateBuffers start name=" + name
                + " graphicsDevice=" + graphicsDevice.getClass().getName()
                + " texture=" + texture
                + " vertices=" + count(geo != null ? geo.getVertices() : null, 3)
                + " normals=" + count(geo != null ? geo.getNormals() : null, 3)
                + " uvs=" + count(geo != null ? geo.getTextureCoordinates() : null, 2)
                + " weights=" + count(geo != null ? geo.getWeightss() : null, 4)
                + " bones=" + count(geo != null ? geo.getBoness() : null, 4)
                + " indices=" + (geo != null && geo.getIndices() != null ? geo.getIndices().length : 0));
        int vaoId = graphicsDevice.createVertexArray();
        int[] vbos = graphicsDevice.createBuffers(6);
        vboIds = vbos;
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
        String vertexShader = graphicsDevice.loadShaderSource(SKINNED_VERTEX_SHADER);
        String fragmentShader = graphicsDevice.loadShaderSource(SKINNED_FRAGMENT_SHADER);
        log("skinned shader sources name=" + name
                + " vertexPath=" + SKINNED_VERTEX_SHADER
                + " vertexChars=" + (vertexShader != null ? vertexShader.length() : -1)
                + " fragmentPath=" + SKINNED_FRAGMENT_SHADER
                + " fragmentChars=" + (fragmentShader != null ? fragmentShader.length() : -1));
        program1 = graphicsDevice.createShaderProgram(
                vertexShader,
                fragmentShader);
        log("generateBuffers complete name=" + name
                + " vao=" + vaoIds[0]
                + " shader=" + program1
                + " shaderCompiled=" + (program1 != null && program1.compiled()));
    }

    public void ensureRenderResources() {
        if (vaoIds[0] == 0 || program1 == null) {
            generateBuffers();
        }
    }

    public ShaderHandle getSkinnedShaderProgram() {
        return program1;
    }

    @Override
    public void render(ShaderHandle shaderprogram, int textureHandle) {
        ensureRenderResources();
        if (!renderStateLogged) {
            renderStateLogged = true;
            log("first render name=" + name
                    + " position=" + position.x + "," + position.y + "," + position.z
                    + " scale=" + scale[0] + "," + scale[1] + "," + scale[2]
                    + " texture=" + texture
                    + " vao=" + vaoIds[0]
                    + " skinnedShader=" + program1
                    + " fallbackShader=" + shaderprogram
                    + " boneBufferStartIndex=" + boneBufferStartIndex
                    + " indexCount=" + (geo != null && geo.getIndices() != null ? geo.getIndices().length : 0));
        }
        if (program1 == null) {
            log("ERROR skinned shader is null name=" + name
                    + " vao=" + vaoIds[0]
                    + " texture=" + texture
                    + " graphicsDevice=" + graphicsDevice.getClass().getName());
            return;
        }
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

    private int count(float[] values, int stride) {
        return values != null && stride > 0 ? values.length / stride : 0;
    }

    private int count(int[] values, int stride) {
        return values != null && stride > 0 ? values.length / stride : 0;
    }

    private void log(String message) {
        System.out.println("[WeightedGameObject] " + message);
    }
}
