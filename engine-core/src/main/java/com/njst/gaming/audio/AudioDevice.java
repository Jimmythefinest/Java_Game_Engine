package com.njst.gaming.audio;

import com.njst.gaming.Math.Vector3;

public interface AudioDevice {
    AudioBufferHandle loadSound(String resourcePath);

    AudioSourceHandle createSource(AudioBufferHandle buffer);

    AudioSourceHandle play(AudioBufferHandle buffer);

    AudioSourceHandle play(String resourcePath);

    void setListenerPosition(Vector3 position);

    void setListenerOrientation(Vector3 forward, Vector3 up);

    void setMasterGain(float gain);

    void cleanup();
}
