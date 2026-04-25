package com.njst.gaming.android.audio;

import android.util.Log;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.audio.AudioSourceHandle;

public class AndroidAudioSourceHandle implements AudioSourceHandle {
    private static final String TAG = "NJST_AUDIO";

    private final int sourceId;
    private boolean cleanedUp;

    AndroidAudioSourceHandle(int sourceId) {
        this.sourceId = sourceId;
    }

    int getSourceId() {
        return sourceId;
    }

    @Override
    public void play() {
        Log.i(TAG, "Playing sourceId=" + sourceId);
        NativeAudio.play(sourceId);
    }

    @Override
    public void pause() {
        Log.i(TAG, "Pausing sourceId=" + sourceId);
        NativeAudio.pause(sourceId);
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping sourceId=" + sourceId);
        NativeAudio.stop(sourceId);
    }

    @Override
    public boolean isPlaying() {
        return NativeAudio.isPlaying(sourceId);
    }

    @Override
    public void setLooping(boolean looping) {
        Log.i(TAG, "Setting looping sourceId=" + sourceId + " looping=" + looping);
        NativeAudio.setLooping(sourceId, looping);
    }

    @Override
    public void setGain(float gain) {
        Log.i(TAG, "Setting gain sourceId=" + sourceId + " gain=" + gain);
        NativeAudio.setGain(sourceId, gain);
    }

    @Override
    public void setPitch(float pitch) {
        NativeAudio.setPitch(sourceId, pitch);
    }

    @Override
    public void setPosition(Vector3 position) {
        if (position == null) {
            return;
        }
        Log.i(TAG, "Setting source position sourceId=" + sourceId
                + " x=" + position.x + " y=" + position.y + " z=" + position.z);
        NativeAudio.setPosition(sourceId, position.x, position.y, position.z);
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        Log.i(TAG, "Deleting sourceId=" + sourceId);
        NativeAudio.deleteSource(sourceId);
        cleanedUp = true;
    }
}
