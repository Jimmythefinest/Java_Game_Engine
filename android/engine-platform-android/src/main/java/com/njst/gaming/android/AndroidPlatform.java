package com.njst.gaming.android;

import android.content.Context;

public final class AndroidPlatform {
    private AndroidPlatform() {
    }

    public static AndroidEngineSurfaceView createSurfaceView(Context context) {
        return new AndroidEngineSurfaceView(context);
    }

    public static AndroidGraphicsDevice createGraphicsDevice(Context context) {
        return new AndroidGraphicsDevice(context);
    }

    public static AndroidComputeBackend createComputeBackend(Context context) {
        return new AndroidComputeBackend(context);
    }
}
