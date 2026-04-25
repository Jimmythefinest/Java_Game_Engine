package com.njst.gaming.audio;

import com.njst.gaming.Math.Vector3;

public class NullAudioDevice implements AudioDevice {
    private final AudioBufferHandle nullBuffer = new NullAudioBufferHandle();
    private final AudioSourceHandle nullSource = new NullAudioSourceHandle();

    @Override
    public AudioBufferHandle loadSound(String resourcePath) {
        return nullBuffer;
    }

    @Override
    public AudioSourceHandle createSource(AudioBufferHandle buffer) {
        return nullSource;
    }

    @Override
    public AudioSourceHandle play(AudioBufferHandle buffer) {
        return nullSource;
    }

    @Override
    public AudioSourceHandle play(String resourcePath) {
        return nullSource;
    }

    @Override
    public void setListenerPosition(Vector3 position) {
    }

    @Override
    public void setListenerOrientation(Vector3 forward, Vector3 up) {
    }

    @Override
    public void setMasterGain(float gain) {
    }

    @Override
    public void cleanup() {
    }
}
