package com.njst.gaming.android.audio;

import android.util.Log;

import com.njst.gaming.audio.AudioBufferHandle;

public class AndroidAudioBufferHandle implements AudioBufferHandle {
    private static final String TAG = "NJST_AUDIO";

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
        Log.i(TAG, "Deleting bufferId=" + bufferId);
        NativeAudio.deleteBuffer(bufferId);
        cleanedUp = true;
    }
}
