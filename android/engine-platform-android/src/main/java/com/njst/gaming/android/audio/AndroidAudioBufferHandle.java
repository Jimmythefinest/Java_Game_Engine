package com.njst.gaming.android.audio;

import com.njst.gaming.audio.AudioBufferHandle;

public class AndroidAudioBufferHandle implements AudioBufferHandle {
    private final int bufferId;
    private boolean cleanedUp;

    AndroidAudioBufferHandle(int bufferId) {
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
        NativeAudio.deleteBuffer(bufferId);
        cleanedUp = true;
    }
}
