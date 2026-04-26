package com.njst.gaming;

import java.util.ArrayList;
import java.util.Comparator;

import com.njst.gaming.Math.*;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.NullGraphicsDevice;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.graphics.ShadowMapHandle;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.TerrainObject;
import com.njst.gaming.objects.Weighted_GameObject;

public class Renderer {
    public static class ProfilerSnapshot {
        public final float frameMs;
        public final float updateMs;
        public final float skyboxMs;
        public final float renderMs;
        public final int objectCount;
        public final int terrainCount;

        public ProfilerSnapshot(float frameMs, float updateMs, float skyboxMs, float renderMs,
                int objectCount, int terrainCount) {
            this.frameMs = frameMs;
            this.updateMs = updateMs;
            this.skyboxMs = skyboxMs;
            this.renderMs = renderMs;
            this.objectCount = objectCount;
            this.terrainCount = terrainCount;
        }
    }

    public GameObject screen;
    public GameObject skybox;
    public Scene scene;
    public Camera camera;
    public Camera lightCamera;
    private int textureHandle;
    public boolean hasError = false;

    public ShaderHandle shaderProgram;
    public ShaderHandle terrainShaderProgram;
    public ShaderHandle shadowShaderProgram;
    public ShaderHandle skinnedShadowShaderProgram;
    public float speed = 1;
    public GameObject test;

    public RootLogger log;

    public int width = 100;
    public int height = 100;
    public float[] clearColor = { 0, 0, 0, 1 };

    int tex1;

    public float angle;
    public final float[] lightPos = { 0, 50f, 0 };
    public final float[] lightColor = { 1.0f, 1.0f, 1.0f };
    public static final int MAX_LIGHTS = 8;
    public BufferHandle ssbo;

    public long lasttym = 0;
    public int fps;
    public int frame_counter = 0;

    public float[] lightViewMatrix, lightProjectionMatrix;
    private Matrix4 lightSpaceMatrix = new Matrix4().identity();
    private ShadowMapHandle shadowMap;
    private static final int SHADOW_MAP_SIZE = 2048;
    private static final float SHADOW_ORTHO_RADIUS = 40f;
    private static final float SHADOW_NEAR = 1f;
    private static final float SHADOW_FAR = 140f;
    private boolean shadowMapEnabled = true;
    private boolean shadowMapDumpPending = true;

    private final GraphicsDevice graphicsDevice;
    private final ArrayList<GameObject> renderQueue = new ArrayList<>();
    private final ArrayList<Light> lights = new ArrayList<>();
    private final float[] cameraDataBuffer = new float[39];
    private final Vector3 mainPassLightPosition = new Vector3();
    private final Vector3 scratchLightColor = new Vector3();
    private final Vector3 scratchLightProperties = new Vector3();
    private ProfilerSnapshot profilerSnapshot = new ProfilerSnapshot(0f, 0f, 0f, 0f, 0, 0);
    private long profilerWindowStartMillis = 0L;
    private long profilerFrameNanos = 0L;
    private long profilerUpdateNanos = 0L;
    private long profilerSkyboxNanos = 0L;
    private long profilerRenderNanos = 0L;
    private int profilerFrames = 0;
    private int profilerObjects = 0;
    private int profilerTerrainObjects = 0;

    public Renderer() {
        this(new NullGraphicsDevice());
    }

    public Renderer(GraphicsDevice graphicsDevice) {
        this.graphicsDevice = graphicsDevice;
        this.ssbo = graphicsDevice.createShaderStorageBuffer();
        log = new RootLogger(data.rootDirectory + "/render.log");
        camera = new Camera(new Vector3(0f, 0f, -7f), new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f));
        lightCamera = new Camera(new Vector3(0f, 5f, 0f), new Vector3(0f, 0f, 0f), new Vector3(-1f, 10f, 0f));
        log.logToRootDirectory("Renderer initialized");
    }

    public void onSurfaceCreated() {
        try {
            lasttym = System.currentTimeMillis();
            profilerWindowStartMillis = lasttym;

            shaderProgram = graphicsDevice.createShaderProgram(
                    graphicsDevice.loadShaderSource("resources/shaders/vert11.glsl"),
                    graphicsDevice.loadShaderSource("resources/shaders/frag11.glsl"));
            terrainShaderProgram = graphicsDevice.createShaderProgram(
                    graphicsDevice.loadShaderSource("resources/shaders/terrain_vert.glsl"),
                    graphicsDevice.loadShaderSource("resources/shaders/terrain_frag.glsl"));
            shadowShaderProgram = graphicsDevice.createShaderProgram(
                    graphicsDevice.loadShaderSource("resources/shaders/shadow_depth_vert.glsl"),
                    graphicsDevice.loadShaderSource("resources/shaders/shadow_depth_frag.glsl"));
            skinnedShadowShaderProgram = graphicsDevice.createShaderProgram(
                    graphicsDevice.loadShaderSource("resources/shaders/shadow_depth_skinned_vert.glsl"),
                    graphicsDevice.loadShaderSource("resources/shaders/shadow_depth_frag.glsl"));
            if (shadowMapEnabled) {
                shadowMap = graphicsDevice.createShadowMap(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
            }
            scene.loader.load(scene);
            for (GameObject object : scene.objects) {
                object.setGraphicsDevice(graphicsDevice);
                object.generateBuffers();
            }

            graphicsDevice.enableBlendAndDepth();

        } catch (Exception e) {
            System.err.println("NJST Renderer.onSurfaceCreated failed: " + e);
            logException(e);
            e.printStackTrace();
        }
    }

    public void onDrawFrame() {
        if (hasError)
            return;
        try {
            long frameStart = System.nanoTime();
            long updateStart = System.nanoTime();
            scene.onDrawFrame();
            long updateEnd = System.nanoTime();
            scene.uploadSkeletonBuffer(graphicsDevice);

            renderQueue.clear();
            renderQueue.addAll(scene.objects);
            if (skybox != null) {
                renderQueue.remove(skybox);
            }
            updateLightMatrices();
            renderShadowPass(renderQueue);
            bindMainCameraData();

            graphicsDevice.clearColorAndDepth();
            shaderProgram.use();
            shaderProgram.setUniformVector3("eyepos1", camera.cameraPosition);
            uploadLightUniforms(shaderProgram);
            terrainShaderProgram.use();
            terrainShaderProgram.setUniformVector3("eyepos1", camera.cameraPosition);
            uploadLightUniforms(terrainShaderProgram);

            long skyboxNanos = 0L;
            if (skybox != null) {
                long skyboxStart = System.nanoTime();
                skybox.setGraphicsDevice(graphicsDevice);
                shaderProgram.use();
                skybox.updateModelMatrix();
                skybox.render(shaderProgram, textureHandle);
                skyboxNanos = System.nanoTime() - skyboxStart;
            }

            long renderStart = System.nanoTime();
            renderQueue.sort((first, second) -> Float.compare(
                    second.position.distanceSquared(camera.cameraPosition),
                    first.position.distanceSquared(camera.cameraPosition)));
            int terrainCount = 0;
            boolean shadowsActive = shadowMapEnabled && shadowMap != null;
            for (GameObject object : renderQueue) {
                object.setGraphicsDevice(graphicsDevice);
                object.setShadowContext(shadowsActive ? shadowMap.getTextureId() : 0, lightSpaceMatrix, shadowsActive);
                boolean terrainObject = object instanceof TerrainObject;
                if (terrainObject) {
                    terrainCount++;
                }
                ShaderHandle activeShader = terrainObject ? terrainShaderProgram : shaderProgram;
                if (object instanceof Weighted_GameObject
                        && ((Weighted_GameObject) object).getSkinnedShaderProgram() != null) {
                    activeShader = ((Weighted_GameObject) object).getSkinnedShaderProgram();
                }
                activeShader.use();
                activeShader.setUniformVector3("eyepos1", camera.cameraPosition);
                uploadLightUniforms(activeShader);
                object.updateModelMatrix();
                object.render(activeShader, textureHandle);
            }
            long renderNanos = System.nanoTime() - renderStart;
            long frameNanos = System.nanoTime() - frameStart;
            updateProfiler(frameNanos, updateEnd - updateStart, skyboxNanos, renderNanos, renderQueue.size(), terrainCount);

            time += frameNanos;
            if (frame == 200) {
                frame = 0;
                System.out.println(time / 200 / 1000000);
                time = 0;
            }
            frame++;

        } catch (Exception e) {
            hasError = true;
            logException(e);
            e.printStackTrace();
        }
    }

    public void renderObjectLikeMainPass(GameObject object) {
        renderObjectWithCameraAndLight(object, camera, new Vector3(0f, 0f, 100f));
    }

    public void renderObjectWithCamera(GameObject object, Camera renderCamera) {
        renderObjectWithCameraAndLight(object, renderCamera, new Vector3(0f, 0f, 100f));
    }

    public void renderObjectWithCameraAndLight(GameObject object, Camera renderCamera, Vector3 lightPosition) {
        if (hasError || object == null) {
            return;
        }
        Camera activeCamera = renderCamera != null ? renderCamera : camera;
        Vector3 activeLight = lightPosition != null ? lightPosition : new Vector3(lightPos[0], lightPos[1], lightPos[2]);
        bindCameraData(activeCamera, activeLight);
        scene.uploadSkeletonBuffer(graphicsDevice);
        ShaderHandle activeShader = (object instanceof TerrainObject) ? terrainShaderProgram : shaderProgram;
        activeShader.use();
        activeShader.setUniformVector3("eyepos1", activeCamera.cameraPosition);
        uploadLightUniforms(activeShader, activeLight);
        object.setGraphicsDevice(graphicsDevice);
        boolean shadowsActive = shadowMapEnabled && shadowMap != null;
        object.setShadowContext(shadowsActive ? shadowMap.getTextureId() : 0, lightSpaceMatrix, shadowsActive);
        object.updateModelMatrix();
        object.render(activeShader, textureHandle);
    }

    public void setShadowMapEnabled(boolean enabled) {
        shadowMapEnabled = enabled;
        if (!enabled) {
            shadowMapDumpPending = false;
        }
    }

    public boolean isShadowMapEnabled() {
        return shadowMapEnabled;
    }

    public void addLight(Light light) {
        if (light != null && lights.size() < MAX_LIGHTS - 1) {
            lights.add(light);
        }
    }

    public Light addPointLight(float x, float y, float z, float r, float g, float b, float intensity, float range) {
        Light light = Light.point(x, y, z, r, g, b, intensity, range);
        addLight(light);
        return light;
    }

    public void clearLights() {
        lights.clear();
    }

    public ArrayList<Light> getLights() {
        return lights;
    }

    private void bindMainCameraData() {
        mainPassLightPosition.set(lightPos[0], lightPos[1], lightPos[2]);
        bindCameraData(camera, mainPassLightPosition);
    }

    private void uploadLightUniforms(ShaderHandle shader) {
        uploadLightUniforms(shader, mainPassLightPosition);
    }

    private void uploadLightUniforms(ShaderHandle shader, Vector3 primaryLightPosition) {
        if (shader == null) {
            return;
        }
        Vector3 primaryPosition = primaryLightPosition != null
                ? primaryLightPosition
                : new Vector3(lightPos[0], lightPos[1], lightPos[2]);
        int count = Math.min(MAX_LIGHTS, 1 + lights.size());
        shader.setUniformInt("uLightCount", count);
        uploadLight(shader, 0, primaryPosition, lightColor[0], lightColor[1], lightColor[2], 1f, 200f);
        for (int i = 1; i < count; i++) {
            Light light = lights.get(i - 1);
            uploadLight(shader, i, light.position, light.color.x, light.color.y, light.color.z,
                    light.intensity, light.range);
        }
    }

    private void uploadLight(ShaderHandle shader, int index, Vector3 position, float red, float green, float blue,
            float intensity, float range) {
        shader.setUniformVector3("uLightPositions[" + index + "]", position);
        scratchLightColor.set(red * intensity, green * intensity, blue * intensity);
        shader.setUniformVector3("uLightColors[" + index + "]", scratchLightColor);
        scratchLightProperties.set(Math.max(0.001f, range), index == 0 ? 1f : 0f, 0f);
        shader.setUniformVector3("uLightProperties[" + index + "]", scratchLightProperties);
    }

    private void bindCameraData(Camera activeCamera, Vector3 activeLight) {
        System.arraycopy(activeCamera.getProjectionMatrix().r, 0, cameraDataBuffer, 0, 16);
        System.arraycopy(activeCamera.getViewMatrix().r, 0, cameraDataBuffer, 16, 16);
        cameraDataBuffer[32] = activeCamera.cameraPosition.x;
        cameraDataBuffer[33] = activeCamera.cameraPosition.y;
        cameraDataBuffer[34] = activeCamera.cameraPosition.z;
        cameraDataBuffer[35] = activeLight.x;
        cameraDataBuffer[36] = activeLight.y;
        cameraDataBuffer[37] = activeLight.z;
        cameraDataBuffer[38] = 0f;
        ssbo.setData(cameraDataBuffer, graphicsDevice.dynamicDrawUsage());
        ssbo.bind();
        ssbo.bindToShader(0);
    }

    private void updateLightMatrices() {
        Vector3 lightPosition = new Vector3(lightPos[0], lightPos[1], lightPos[2]);
        Vector3 lightTarget = camera != null && camera.targetPosition != null
                ? new Vector3(camera.targetPosition)
                : new Vector3(0f, 0f, 0f);
        Vector3 lightDirection = new Vector3(lightTarget).sub(lightPosition).normalize();
        Vector3 upVector = Math.abs(lightDirection.y) > 0.98f
                ? new Vector3(0f, 0f, 1f)
                : new Vector3(0f, 1f, 0f);
        Matrix4 lightView = new Matrix4().lookAt(lightPosition, lightTarget, upVector);
        Matrix4 lightProjection = new Matrix4().identity()
                .ortho(-SHADOW_ORTHO_RADIUS, SHADOW_ORTHO_RADIUS,
                        -SHADOW_ORTHO_RADIUS, SHADOW_ORTHO_RADIUS,
                        SHADOW_NEAR, SHADOW_FAR);
        lightViewMatrix = lightView.r.clone();
        lightProjectionMatrix = lightProjection.r.clone();
        lightSpaceMatrix = lightProjection.multiply(lightView);
    }

    private void renderShadowPass(ArrayList<GameObject> renderQueue) {
        if (!shadowMapEnabled || shadowMap == null || shadowShaderProgram == null || skinnedShadowShaderProgram == null) {
            return;
        }
        graphicsDevice.bindShadowMap(shadowMap);
        graphicsDevice.viewport(shadowMap.getWidth(), shadowMap.getHeight());
        graphicsDevice.clearDepth();
        for (GameObject object : renderQueue) {
            if (!object.castsShadows) {
                continue;
            }
            object.setGraphicsDevice(graphicsDevice);
            if (object.vaoIds[0] == 0) {
                object.generateBuffers();
            }
            object.updateModelMatrix();
            if (object instanceof Weighted_GameObject) {
                renderSkinnedShadow((Weighted_GameObject) object);
            } else {
                renderStaticShadow(object);
            }
        }
        graphicsDevice.bindDefaultFramebuffer();
        graphicsDevice.viewport(width, height);
        if (shadowMapDumpPending) {
            graphicsDevice.dumpShadowMap(shadowMap, data.rootDirectory + "/shadow_map_debug.png");
            shadowMapDumpPending = false;
        }
    }

    private void renderStaticShadow(GameObject object) {
        shadowShaderProgram.use();
        shadowShaderProgram.setUniformMatrix4fv("uMMatrix", object.modelMatrix);
        shadowShaderProgram.setUniformMatrix4fv("uLightSpaceMatrix", lightSpaceMatrix);
        graphicsDevice.bindVertexArray(object.vaoIds[0]);
        graphicsDevice.drawElementsTriangles(object.geometry.getIndices().length);
        graphicsDevice.bindVertexArray(0);
    }

    private void renderSkinnedShadow(Weighted_GameObject object) {
        object.ensureRenderResources();
        skinnedShadowShaderProgram.use();
        skinnedShadowShaderProgram.setUniformMatrix4fv("uMMatrix", object.modelMatrix);
        skinnedShadowShaderProgram.setUniformMatrix4fv("uLightSpaceMatrix", lightSpaceMatrix);
        skinnedShadowShaderProgram.setUniformInt("boneStartIndex", object.boneBufferStartIndex);
        graphicsDevice.bindVertexArray(object.vaoIds[0]);
        graphicsDevice.drawElementsTriangles(object.geo.getIndices().length);
        graphicsDevice.bindVertexArray(0);
    }

    public SphericalHeightmapShape bakeSphericalHeightmap(GameObject object, int width, int height) {
        Vector3 defaultCenter = new Vector3(
                (object.localMin.x + object.localMax.x) * 0.5f,
                (object.localMin.y + object.localMax.y) * 0.5f,
                (object.localMin.z + object.localMax.z) * 0.5f);
        return bakeSphericalHeightmap(object, width, height, defaultCenter);
    }

    public SphericalHeightmapShape bakeSphericalHeightmap(GameObject object, int width, int height, Vector3 localCenter) {
        if (object == null || width <= 0 || height <= 0) {
            return null;
        }
        return graphicsDevice.bakeSphericalHeightmap(this, object, width, height, localCenter);
    }

    int frame = 0;
    long time = 0;

    float a = 0;

    public void onSurfaceChanged(int w, int h) {
        width = w;
        height = h;
        graphicsDevice.viewport(width, height);
        float ratio = (float) width / height;
        camera.setPerspective(45, ratio, 0.1f, 1000);

    }

    public synchronized void setFps(int i) {
        fps = i;
    }

    public synchronized int getFps() {
        return fps;
    }

    public synchronized ProfilerSnapshot getProfilerSnapshot() {
        return profilerSnapshot;
    }

    public synchronized long getlast() {
        return lasttym;
    }

    public GraphicsDevice getGraphicsDevice() {
        return graphicsDevice;
    }

    private void logException(Exception e) {
        log.logToRootDirectory(e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            log.logToRootDirectory(element.getClassName() + element.getMethodName() + element.getLineNumber());
        }
    }

    private synchronized void updateProfiler(long frameNanos, long updateNanos, long skyboxNanos,
            long renderNanos, int objectCount, int terrainCount) {
        profilerFrameNanos += frameNanos;
        profilerUpdateNanos += updateNanos;
        profilerSkyboxNanos += skyboxNanos;
        profilerRenderNanos += renderNanos;
        profilerObjects += objectCount;
        profilerTerrainObjects += terrainCount;
        profilerFrames++;

        long now = System.currentTimeMillis();
        if ((now - profilerWindowStartMillis) < 1000L || profilerFrames == 0) {
            return;
        }

        float divisor = 1_000_000f * profilerFrames;
        profilerSnapshot = new ProfilerSnapshot(
                profilerFrameNanos / divisor,
                profilerUpdateNanos / divisor,
                profilerSkyboxNanos / divisor,
                profilerRenderNanos / divisor,
                Math.round(profilerObjects / (float) profilerFrames),
                Math.round(profilerTerrainObjects / (float) profilerFrames));

        profilerWindowStartMillis = now;
        profilerFrameNanos = 0L;
        profilerUpdateNanos = 0L;
        profilerSkyboxNanos = 0L;
        profilerRenderNanos = 0L;
        profilerObjects = 0;
        profilerTerrainObjects = 0;
        profilerFrames = 0;
    }
}
