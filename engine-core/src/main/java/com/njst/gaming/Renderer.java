package com.njst.gaming;

import java.util.ArrayList;
import java.util.Comparator;

import com.njst.gaming.Math.*;
import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.NullGraphicsDevice;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.TerrainObject;

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
    public BufferHandle ssbo;

    public long lasttym = 0;
    public int fps;
    public int frame_counter = 0;

    public float[] lightViewMatrix, lightProjectionMatrix;

    private final GraphicsDevice graphicsDevice;
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
            float[] consts = new float[39];
            System.arraycopy(camera.getProjectionMatrix().r, 0, consts, 0, 16);
            System.arraycopy(camera.getViewMatrix().r, 0, consts, 16, 16);
            System.arraycopy(camera.cameraPosition.toArray(), 0, consts, 32, 3);
            System.arraycopy(new float[] { 0, 0, 100, 0 }, 0, consts, 35, 4);
            ssbo.setData(consts, graphicsDevice.dynamicDrawUsage());
            ssbo.bind();
            ssbo.bindToShader(0);

            graphicsDevice.clearColorAndDepth();
            shaderProgram.setUniformVector3("eyepos1", camera.cameraPosition);
            terrainShaderProgram.setUniformVector3("eyepos1", camera.cameraPosition);

            long updateStart = System.nanoTime();
            scene.onDrawFrame();
            long updateEnd = System.nanoTime();

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
            ArrayList<GameObject> renderQueue = new ArrayList<>(scene.objects);
            if (skybox != null) {
                renderQueue.remove(skybox);
            }
            renderQueue.sort(Comparator.comparingDouble(
                    object -> -object.position.distance(camera.cameraPosition)));
            int terrainCount = 0;
            for (GameObject object : renderQueue) {
                object.setGraphicsDevice(graphicsDevice);
                boolean terrainObject = object instanceof TerrainObject;
                if (terrainObject) {
                    terrainCount++;
                }
                ShaderHandle activeShader = terrainObject ? terrainShaderProgram : shaderProgram;
                activeShader.use();
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
        if (hasError || object == null) {
            return;
        }
        float[] consts = new float[39];
        System.arraycopy(camera.getProjectionMatrix().r, 0, consts, 0, 16);
        System.arraycopy(camera.getViewMatrix().r, 0, consts, 16, 16);
        System.arraycopy(camera.cameraPosition.toArray(), 0, consts, 32, 3);
        System.arraycopy(new float[] { 0, 0, 100, 0 }, 0, consts, 35, 4);
        ssbo.setData(consts, graphicsDevice.dynamicDrawUsage());
        ssbo.bind();
        ssbo.bindToShader(0);
        ShaderHandle activeShader = (object instanceof TerrainObject) ? terrainShaderProgram : shaderProgram;
        activeShader.use();
        activeShader.setUniformVector3("eyepos1", camera.cameraPosition);
        object.setGraphicsDevice(graphicsDevice);
        object.updateModelMatrix();
        object.render(activeShader, textureHandle);
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
