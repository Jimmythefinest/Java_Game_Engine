package com.njst.gaming.android;

import android.content.Context;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;

import com.njst.gaming.Renderer;
import com.njst.gaming.ri.battlearena.BattleArenaDemoLoader;
import com.njst.gaming.Scene;
import com.njst.gaming.input.InputCodes;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AndroidEngineRenderer implements GLSurfaceView.Renderer {
    public interface StatsListener {
        void onStatsUpdated(int fps, Renderer.ProfilerSnapshot snapshot);
    }

    private final Context context;
    private final boolean[] pendingButtons = new boolean[InputCodes.MAX_BUTTONS];
    private Renderer renderer;
    private boolean pendingLooking;
    private StatsListener statsListener;
    private long fpsWindowStartMillis;
    private int framesThisWindow;

    public AndroidEngineRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES31.glClearColor(0.08f, 0.10f, 0.14f, 1.0f);

        AndroidGraphicsDevice graphicsDevice = new AndroidGraphicsDevice(context);
        renderer = new Renderer(graphicsDevice);

        Scene scene = new Scene();
        scene.renderer = renderer;
        scene.loader = new BattleArenaDemoLoader();
        renderer.scene = scene;
        applyPendingInputs(scene);

        fpsWindowStartMillis = System.currentTimeMillis();
        framesThisWindow = 0;
        notifyStats(0);
        renderer.onSurfaceCreated();
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

    public void cursorMoved(float x, float y) {
        if (renderer == null || renderer.scene == null) {
            return;
        }
        renderer.scene.cursorMoved(x, y);
    }

    public void setLooking(boolean active) {
        pendingLooking = active;
        setButtonState(InputCodes.BUTTON_LOOK, active);
        if (renderer == null || renderer.scene == null) {
            return;
        }
        renderer.scene.righmouse = active;
        renderer.scene.inputSystem.pointer.setActive(active);
    }

    public void setButtonState(int buttonCode, boolean down) {
        if (buttonCode < 0 || buttonCode >= pendingButtons.length) {
            return;
        }
        pendingButtons[buttonCode] = down;
        if (renderer == null || renderer.scene == null) {
            return;
        }
        renderer.scene.inputSystem.button(buttonCode).setDown(down);
        if (buttonCode == InputCodes.BUTTON_LOOK) {
            renderer.scene.inputSystem.pointer.setActive(down);
        }
    }

    private void applyPendingInputs(Scene scene) {
        scene.righmouse = pendingLooking;
        scene.inputSystem.pointer.setActive(pendingLooking);
        for (int i = 0; i < pendingButtons.length; i++) {
            if (pendingButtons[i]) {
                scene.inputSystem.button(i).setDown(true);
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
