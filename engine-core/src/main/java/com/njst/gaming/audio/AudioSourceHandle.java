package com.njst.gaming.audio;

import com.njst.gaming.Math.Vector3;

public interface AudioSourceHandle {
    void play();

    void pause();

    void stop();

    boolean isPlaying();

    void setLooping(boolean looping);

    void setGain(float gain);

    void setPitch(float pitch);

    void setPosition(Vector3 position);

    void cleanup();
}
