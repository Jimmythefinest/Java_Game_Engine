package com.njst.gaming.android;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class AndroidEngineSurfaceView extends GLSurfaceView {
    private final AndroidEngineRenderer renderer;

    public AndroidEngineSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new AndroidEngineRenderer(context.getApplicationContext());
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }
}
