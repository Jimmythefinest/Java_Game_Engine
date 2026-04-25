package com.njst.gaming.audio;

import com.njst.gaming.Math.Vector3;

public class NullAudioSourceHandle implements AudioSourceHandle {
    @Override
    public void play() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void setLooping(boolean looping) {
    }

    @Override
    public void setGain(float gain) {
    }

    @Override
    public void setPitch(float pitch) {
    }

    @Override
    public void setPosition(Vector3 position) {
    }

    @Override
    public void cleanup() {
    }
}
