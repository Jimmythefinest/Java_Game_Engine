package com.njst.gaming.android;

import android.content.Context;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.njst.gaming.Renderer;
import com.njst.gaming.Scene;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AndroidEngineRenderer implements GLSurfaceView.Renderer {
    public static String TAG="android_renderer_engine";
    public interface StatsListener {
        void onStatsUpdated(int fps, Renderer.ProfilerSnapshot snapshot);
    }

    private final Context context;
    private final AndroidGameConfig gameConfig;
    private final Map<String, Boolean> pendingActions = new LinkedHashMap<>();
    private Renderer renderer;
    private StatsListener statsListener;
    private long fpsWindowStartMillis;
    private int framesThisWindow;

    public AndroidEngineRenderer(Context context, AndroidGameConfig gameConfig) {
        this.context = context;
        this.gameConfig = gameConfig;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated: initialising GL context");
        try {
            GLES31.glClearColor(0.08f, 0.10f, 0.14f, 1.0f);

            AndroidAssetLoader.setExternalRoot(gameConfig.getExternalAssetRoot());
            AndroidGraphicsDevice graphicsDevice = new AndroidGraphicsDevice(context);
            renderer = new Renderer(graphicsDevice);
            renderer.setShadowMapEnabled(gameConfig.isShadowMapEnabled());

            Scene scene = new Scene();
            scene.renderer = renderer;
            renderer.scene = scene;
            Log.i(TAG, "onSurfaceCreated: calling configureScene");
            gameConfig.configureScene(scene);
            Log.i(TAG, "onSurfaceCreated: configureScene done, applying pending inputs");
            applyPendingInputs(scene);

            fpsWindowStartMillis = System.currentTimeMillis();
            framesThisWindow = 0;
            notifyStats(0);
            renderer.onSurfaceCreated();
            Log.i(TAG, "onSurfaceCreated: complete");
        } catch (Throwable t) {
            Log.e(TAG, "onSurfaceCreated: FATAL error during scene initialisation", t);
            throw t;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (renderer != null) {
            renderer.onSurfaceChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (renderer == null) {
            return;
        }
        renderer.onDrawFrame();
        updateStats();
        if (renderer.scene != null) {
            renderer.scene.inputSystem.beginFrame();
        }
    }

    public void setStatsListener(StatsListener listener) {
        this.statsListener = listener;
        if (listener != null && renderer != null) {
            listener.onStatsUpdated(renderer.getFps(), renderer.getProfilerSnapshot());
        }
    }

    public void pointerMoved(String pointerId, float x, float y) {
        if (renderer == null || renderer.scene == null || pointerId == null || pointerId.isEmpty()) {
            return;
        }
        renderer.scene.handlePointerInput(pointerId, x, y);
    }

    public void setPointerActionState(String pointerId, String actionId, boolean down) {
        if (renderer == null || renderer.scene == null) {
            if (actionId != null && !actionId.isEmpty()) {
                pendingActions.put(actionId, down);
            }
            return;
        }
        if (pointerId != null && !pointerId.isEmpty()) {
            renderer.scene.pointer(pointerId).setActive(down);
        }
        if (actionId != null && !actionId.isEmpty()) {
            pendingActions.put(actionId, down);
            renderer.scene.inputSystem.button(actionId).setDown(down);
        }
    }

    public void resetPointer(String pointerId) {
        if (renderer == null || renderer.scene == null || pointerId == null || pointerId.isEmpty()) {
            return;
        }
        renderer.scene.handlePointerInput(pointerId, 0f, 0f);
        renderer.scene.pointer(pointerId).setActive(false);
    }

    public void setActionState(String actionId, boolean down) {
        if (actionId == null || actionId.isEmpty()) {
            return;
        }
        pendingActions.put(actionId, down);
        if (renderer == null || renderer.scene == null) {
            return;
        }
        renderer.scene.inputSystem.button(actionId).setDown(down);
    }

    public void releaseAllActions() {
        for (String actionId : pendingActions.keySet().toArray(new String[0])) {
            setActionState(actionId, false);
        }
    }

    private void applyPendingInputs(Scene scene) {
        for (Map.Entry<String, Boolean> entry : pendingActions.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                scene.inputSystem.button(entry.getKey()).setDown(true);
            }
        }
    }

    private void updateStats() {
        framesThisWindow++;
        long now = System.currentTimeMillis();
        long elapsed = now - fpsWindowStartMillis;
        if (elapsed < 1000L) {
            return;
        }
        int fps = Math.round((framesThisWindow * 1000f) / elapsed);
        renderer.setFps(fps);
        notifyStats(fps);
        fpsWindowStartMillis = now;
        framesThisWindow = 0;
    }

    private void notifyStats(int fps) {
        if (statsListener != null) {
            statsListener.onStatsUpdated(fps, renderer != null ? renderer.getProfilerSnapshot() : null);
        }
    }
}
