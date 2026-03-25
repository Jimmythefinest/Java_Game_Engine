package com.njst.gaming.android;

import android.content.Context;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;

import com.njst.gaming.Renderer;
import com.njst.gaming.Scene;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AndroidEngineRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private Renderer renderer;

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
        scene.loader = new AndroidOpenWorldLoader(context);
        renderer.scene = scene;

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
    }
}
