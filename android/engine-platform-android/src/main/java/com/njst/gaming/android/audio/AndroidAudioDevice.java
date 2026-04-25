package com.njst.gaming.android.audio;

import android.content.Context;
import android.content.res.AssetManager;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.android.AndroidAssetLoader;
import com.njst.gaming.audio.AudioBufferHandle;
import com.njst.gaming.audio.AudioDevice;
import com.njst.gaming.audio.AudioSourceHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidAudioDevice implements AudioDevice {
    private final AssetManager assetManager;
    private final Map<String, AndroidAudioBufferHandle> soundCache = new HashMap<>();
    private final List<AndroidAudioSourceHandle> sources = new ArrayList<>();
    private boolean cleanedUp;

    public AndroidAudioDevice(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null.");
        }
        assetManager = context.getAssets();
        NativeAudio.init();
    }

    @Override
    public AudioBufferHandle loadSound(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("Audio resource path must not be empty.");
        }
        AndroidAudioBufferHandle cached = soundCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        byte[] wavBytes = AndroidAssetLoader.readBytes(assetManager, resourcePath);
        int bufferId = NativeAudio.createBuffer(wavBytes);
        if (bufferId == 0) {
            throw new IllegalStateException("Unable to create Android audio buffer for: " + resourcePath);
        }
        AndroidAudioBufferHandle buffer = new AndroidAudioBufferHandle(bufferId);
        soundCache.put(resourcePath, buffer);
        return buffer;
    }

    @Override
    public AudioSourceHandle createSource(AudioBufferHandle buffer) {
        AndroidAudioBufferHandle androidBuffer = requireAndroidBuffer(buffer);
        int sourceId = NativeAudio.createSource(androidBuffer.getBufferId());
        if (sourceId == 0) {
            throw new IllegalStateException("Unable to create Android audio source.");
        }
        AndroidAudioSourceHandle source = new AndroidAudioSourceHandle(sourceId);
        sources.add(source);
        return source;
    }

    @Override
    public AudioSourceHandle play(AudioBufferHandle buffer) {
        AudioSourceHandle source = createSource(buffer);
        source.play();
        return source;
    }

    @Override
    public AudioSourceHandle play(String resourcePath) {
        return play(loadSound(resourcePath));
    }

    @Override
    public void setListenerPosition(Vector3 position) {
        if (position == null) {
            return;
        }
        NativeAudio.setListenerPosition(position.x, position.y, position.z);
    }

    @Override
    public void setListenerOrientation(Vector3 forward, Vector3 up) {
        if (forward == null || up == null) {
            return;
        }
        NativeAudio.setListenerOrientation(forward.x, forward.y, forward.z, up.x, up.y, up.z);
    }

    @Override
    public void setMasterGain(float gain) {
        NativeAudio.setMasterGain(gain);
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        for (AndroidAudioSourceHandle source : new ArrayList<>(sources)) {
            source.cleanup();
        }
        sources.clear();
        for (AndroidAudioBufferHandle buffer : soundCache.values()) {
            buffer.cleanup();
        }
        soundCache.clear();
        NativeAudio.shutdown();
        cleanedUp = true;
    }

    private AndroidAudioBufferHandle requireAndroidBuffer(AudioBufferHandle buffer) {
        if (!(buffer instanceof AndroidAudioBufferHandle)) {
            throw new IllegalArgumentException("AndroidAudioDevice requires buffers created by AndroidAudioDevice.");
        }
        return (AndroidAudioBufferHandle) buffer;
    }
}
