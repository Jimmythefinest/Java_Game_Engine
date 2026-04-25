package com.njst.gaming.Natives.audio;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO8;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO8;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_ORIENTATION;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alListener3f;
import static org.lwjgl.openal.AL10.alListenerf;
import static org.lwjgl.openal.AL10.alListenerfv;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.audio.AudioBufferHandle;
import com.njst.gaming.audio.AudioDevice;
import com.njst.gaming.audio.AudioSourceHandle;

public class DesktopAudioDevice implements AudioDevice {
    private final long device;
    private final long context;
    private final Map<String, DesktopAudioBufferHandle> soundCache = new HashMap<>();
    private final List<DesktopAudioSourceHandle> sources = new ArrayList<>();
    private boolean cleanedUp;

    public DesktopAudioDevice() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Unable to open the default OpenAL audio device.");
        }

        context = alcCreateContext(device, (int[]) null);
        if (context == NULL) {
            alcCloseDevice(device);
            throw new IllegalStateException("Unable to create an OpenAL audio context.");
        }

        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
        setListenerPosition(new Vector3(0f, 0f, 0f));
        setListenerOrientation(new Vector3(0f, 0f, -1f), new Vector3(0f, 1f, 0f));
        setMasterGain(1f);
    }

    @Override
    public AudioBufferHandle loadSound(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("Audio resource path must not be empty.");
        }
        DesktopAudioBufferHandle cached = soundCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }

        DesktopAudioBufferHandle buffer = createBuffer(resourcePath);
        soundCache.put(resourcePath, buffer);
        return buffer;
    }

    @Override
    public AudioSourceHandle createSource(AudioBufferHandle buffer) {
        DesktopAudioBufferHandle desktopBuffer = requireDesktopBuffer(buffer);
        DesktopAudioSourceHandle source = new DesktopAudioSourceHandle(desktopBuffer);
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
        alListener3f(AL_POSITION, position.x, position.y, position.z);
    }

    @Override
    public void setListenerOrientation(Vector3 forward, Vector3 up) {
        if (forward == null || up == null) {
            return;
        }
        alListenerfv(AL_ORIENTATION, new float[] {
                forward.x, forward.y, forward.z,
                up.x, up.y, up.z
        });
    }

    @Override
    public void setMasterGain(float gain) {
        alListenerf(AL_GAIN, Math.max(0f, gain));
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        for (DesktopAudioSourceHandle source : new ArrayList<>(sources)) {
            source.cleanup();
        }
        sources.clear();
        for (DesktopAudioBufferHandle buffer : soundCache.values()) {
            buffer.cleanup();
        }
        soundCache.clear();
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
        cleanedUp = true;
    }

    private DesktopAudioBufferHandle createBuffer(String resourcePath) {
        byte[] encodedAudio = ShaderProgram.loadBinaryResource(resourcePath);
        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(encodedAudio))) {
            AudioFormat decodedFormat = toDecodedFormat(sourceStream.getFormat());
            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, sourceStream)) {
                byte[] pcmBytes = readAllBytes(decodedStream);
                ByteBuffer pcmBuffer = BufferUtils.createByteBuffer(pcmBytes.length);
                pcmBuffer.put(pcmBytes);
                pcmBuffer.flip();

                int bufferId = alGenBuffers();
                alBufferData(bufferId, getOpenAlFormat(decodedFormat), pcmBuffer, (int) decodedFormat.getSampleRate());
                return new DesktopAudioBufferHandle(bufferId);
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException("Failed to load audio resource: " + resourcePath, e);
        }
    }

    private AudioFormat toDecodedFormat(AudioFormat sourceFormat) {
        int channels = sourceFormat.getChannels();
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("Only mono and stereo audio are supported. Channels: " + channels);
        }
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                channels,
                channels * 2,
                sourceFormat.getSampleRate(),
                false);
    }

    private int getOpenAlFormat(AudioFormat format) {
        int channels = format.getChannels();
        int bits = format.getSampleSizeInBits();
        if (channels == 1 && bits == 8) {
            return AL_FORMAT_MONO8;
        }
        if (channels == 1 && bits == 16) {
            return AL_FORMAT_MONO16;
        }
        if (channels == 2 && bits == 8) {
            return AL_FORMAT_STEREO8;
        }
        if (channels == 2 && bits == 16) {
            return AL_FORMAT_STEREO16;
        }
        throw new IllegalArgumentException("Unsupported audio format: " + channels + " channels, " + bits + " bits");
    }

    private byte[] readAllBytes(AudioInputStream stream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        while ((bytesRead = stream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    private DesktopAudioBufferHandle requireDesktopBuffer(AudioBufferHandle buffer) {
        if (!(buffer instanceof DesktopAudioBufferHandle)) {
            throw new IllegalArgumentException("DesktopAudioDevice requires buffers created by DesktopAudioDevice.");
        }
        return (DesktopAudioBufferHandle) buffer;
    }
}
