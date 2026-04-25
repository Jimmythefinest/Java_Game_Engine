package com.njst.gaming.android.audio;

final class NativeAudio {
    static {
        System.loadLibrary("njst_audio");
    }

    private NativeAudio() {
    }

    static native void init();

    static native int createBuffer(byte[] wavBytes);

    static native void deleteBuffer(int bufferId);

    static native int createSource(int bufferId);

    static native void deleteSource(int sourceId);

    static native void play(int sourceId);

    static native void pause(int sourceId);

    static native void stop(int sourceId);

    static native boolean isPlaying(int sourceId);

    static native void setLooping(int sourceId, boolean looping);

    static native void setGain(int sourceId, float gain);

    static native void setPitch(int sourceId, float pitch);

    static native void setPosition(int sourceId, float x, float y, float z);

    static native void setListenerPosition(float x, float y, float z);

    static native void setListenerOrientation(float forwardX, float forwardY, float forwardZ, float upX, float upY, float upZ);

    static native void setMasterGain(float gain);

    static native void shutdown();
}
