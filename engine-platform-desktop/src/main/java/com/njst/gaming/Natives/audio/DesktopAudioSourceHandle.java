package com.njst.gaming.Natives.audio;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_FALSE;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_LOOPING;
import static org.lwjgl.openal.AL10.AL_PITCH;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.AL_TRUE;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alSource3f;
import static org.lwjgl.openal.AL10.alSourcePause;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.audio.AudioSourceHandle;

public class DesktopAudioSourceHandle implements AudioSourceHandle {
    private final int sourceId;
    private boolean cleanedUp;

    DesktopAudioSourceHandle(DesktopAudioBufferHandle buffer) {
        sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, buffer.getBufferId());
        alSourcef(sourceId, AL_GAIN, 1f);
        alSourcef(sourceId, AL_PITCH, 1f);
        setPosition(new Vector3(0f, 0f, 0f));
    }

    @Override
    public void play() {
        alSourcePlay(sourceId);
    }

    @Override
    public void pause() {
        alSourcePause(sourceId);
    }

    @Override
    public void stop() {
        alSourceStop(sourceId);
    }

    @Override
    public boolean isPlaying() {
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    @Override
    public void setLooping(boolean looping) {
        alSourcei(sourceId, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
    }

    @Override
    public void setGain(float gain) {
        alSourcef(sourceId, AL_GAIN, Math.max(0f, gain));
    }

    @Override
    public void setPitch(float pitch) {
        alSourcef(sourceId, AL_PITCH, Math.max(0.01f, pitch));
    }

    @Override
    public void setPosition(Vector3 position) {
        if (position == null) {
            return;
        }
        alSource3f(sourceId, AL_POSITION, position.x, position.y, position.z);
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        stop();
        alDeleteSources(sourceId);
        cleanedUp = true;
    }
}
