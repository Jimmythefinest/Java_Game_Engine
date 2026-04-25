package com.njst.gaming.Natives.audio;

import static org.lwjgl.openal.AL10.alDeleteBuffers;

import com.njst.gaming.audio.AudioBufferHandle;

public class DesktopAudioBufferHandle implements AudioBufferHandle {
    private final int bufferId;
    private boolean cleanedUp;

    DesktopAudioBufferHandle(int bufferId) {
        this.bufferId = bufferId;
    }

    int getBufferId() {
        return bufferId;
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        alDeleteBuffers(bufferId);
        cleanedUp = true;
    }
}
