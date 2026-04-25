package com.njst.gaming.android.audio;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.audio.AudioSourceHandle;

public class AndroidAudioSourceHandle implements AudioSourceHandle {
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
        NativeAudio.play(sourceId);
    }

    @Override
    public void pause() {
        NativeAudio.pause(sourceId);
    }

    @Override
    public void stop() {
        NativeAudio.stop(sourceId);
    }

    @Override
    public boolean isPlaying() {
        return NativeAudio.isPlaying(sourceId);
    }

    @Override
    public void setLooping(boolean looping) {
        NativeAudio.setLooping(sourceId, looping);
    }

    @Override
    public void setGain(float gain) {
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
        NativeAudio.setPosition(sourceId, position.x, position.y, position.z);
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        NativeAudio.deleteSource(sourceId);
        cleanedUp = true;
    }
}
