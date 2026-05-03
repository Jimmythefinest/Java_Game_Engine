package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Geometries.CustomGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.objects.GameObject;

public final class BattleArenaGpuExperimentLoader implements Scene.SceneLoader {
    private static final String VERTEX_SHADER = "shaders/fullscreen_experiment_vert.glsl";
    private static final String FRAGMENT_SHADER = "shaders/fullscreen_experiment_frag.glsl";
    private static final float[] POSITIONS = {
            -1f, -1f, 0f,
             1f, -1f, 0f,
             1f,  1f, 0f,
            -1f,  1f, 0f
    };
    private static final float[] NORMALS = {
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
    };
    private static final float[] UVS = {
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
    };
    private static final int[] INDICES = {
            0, 1, 2,
            2, 3, 0
    };

    private float timeSeconds;
    private int resolutionWidth = 800;
    private int resolutionHeight = 600;

    public void setTimeSeconds(float timeSeconds) {
        this.timeSeconds = Math.max(0f, timeSeconds);
    }

    public void setResolution(int width, int height) {
        this.resolutionWidth = Math.max(1, width);
        this.resolutionHeight = Math.max(1, height);
    }

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        ShaderHandle shader = graphicsDevice.createShaderProgram(
                graphicsDevice.loadShaderSource(VERTEX_SHADER),
                graphicsDevice.loadShaderSource(FRAGMENT_SHADER));
        if (shader == null || !shader.compiled()) {
            throw new IllegalStateException("Failed to compile fullscreen GPU experiment shader");
        }
        scene.addGameObject(new FullscreenExperimentQuad(shader));
        scene.renderer.setShadowMapEnabled(false);
        scene.renderer.clearLights();
    }

    private final class FullscreenExperimentQuad extends GameObject {
        private final ShaderHandle experimentShader;

        FullscreenExperimentQuad(ShaderHandle experimentShader) {
            super(new CustomGeometry(POSITIONS, INDICES, NORMALS, UVS), 0);
            this.experimentShader = experimentShader;
            this.name = "GpuExperiment_FullscreenQuad";
            this.castsShadows = false;
        }

        @Override
        public void render(ShaderHandle ignoredShader, int ignoredTextureHandle) {
            if (experimentShader == null) {
                return;
            }
            if (vaoIds[0] == 0) {
                generateBuffers();
            }
            experimentShader.use();
            experimentShader.setUniformVector3("uResolution", new Vector3(
                    resolutionWidth,
                    resolutionHeight,
                    1f));
            experimentShader.setUniformVector3("uTimeMouse", new Vector3(
                    timeSeconds,
                    0f,
                    0f));
            graphicsDevice.bindVertexArray(vaoIds[0]);
            graphicsDevice.drawElementsTriangles(getIndexCount());
            graphicsDevice.bindVertexArray(0);
        }

        @Override
        public void cleanup() {
            super.cleanup();
            if (experimentShader != null) {
                experimentShader.cleanup();
            }
        }
    }
}
