#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/log.h>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <vector>

#define LOG_TAG "njst_native_audio"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {

struct AudioBuffer {
    std::vector<uint8_t> pcm;
    uint32_t sampleRate = 44100;
    uint16_t channels = 2;
    uint16_t bitsPerSample = 16;
};

struct AudioSource {
    int id = 0;
    int bufferId = 0;
    SLObjectItf playerObject = nullptr;
    SLPlayItf playItf = nullptr;
    SLAndroidSimpleBufferQueueItf queueItf = nullptr;
    SLVolumeItf volumeItf = nullptr;
    bool looping = false;
    bool playing = false;
    float gain = 1.0f;
};

std::mutex gMutex;
SLObjectItf gEngineObject = nullptr;
SLEngineItf gEngine = nullptr;
SLObjectItf gOutputMixObject = nullptr;
std::map<int, std::shared_ptr<AudioBuffer>> gBuffers;
std::map<int, std::shared_ptr<AudioSource>> gSources;
int gNextBufferId = 1;
int gNextSourceId = 1;
float gMasterGain = 1.0f;

uint16_t readU16(const uint8_t *data) {
    return static_cast<uint16_t>(data[0] | (data[1] << 8));
}

uint32_t readU32(const uint8_t *data) {
    return static_cast<uint32_t>(data[0] | (data[1] << 8) | (data[2] << 16) | (data[3] << 24));
}

bool parseWav(const uint8_t *bytes, size_t size, AudioBuffer &out) {
    if (size < 44 || std::memcmp(bytes, "RIFF", 4) != 0 || std::memcmp(bytes + 8, "WAVE", 4) != 0) {
        LOGE("Invalid WAV: missing RIFF/WAVE header");
        return false;
    }

    bool foundFormat = false;
    bool foundData = false;
    uint16_t audioFormat = 0;
    size_t offset = 12;
    while (offset + 8 <= size) {
        const uint8_t *chunk = bytes + offset;
        uint32_t chunkSize = readU32(chunk + 4);
        size_t dataOffset = offset + 8;
        if (dataOffset + chunkSize > size) {
            LOGE("Invalid WAV: chunk exceeds file size");
            return false;
        }

        if (std::memcmp(chunk, "fmt ", 4) == 0) {
            if (chunkSize < 16) {
                LOGE("Invalid WAV: fmt chunk too small");
                return false;
            }
            audioFormat = readU16(bytes + dataOffset);
            out.channels = readU16(bytes + dataOffset + 2);
            out.sampleRate = readU32(bytes + dataOffset + 4);
            out.bitsPerSample = readU16(bytes + dataOffset + 14);
            foundFormat = true;
        } else if (std::memcmp(chunk, "data", 4) == 0) {
            out.pcm.assign(bytes + dataOffset, bytes + dataOffset + chunkSize);
            foundData = true;
        }

        offset = dataOffset + chunkSize + (chunkSize & 1u);
    }

    if (!foundFormat || !foundData) {
        LOGE("Invalid WAV: missing fmt or data chunk");
        return false;
    }
    if (audioFormat != 1) {
        LOGE("Unsupported WAV encoding: %u. Only PCM is supported.", audioFormat);
        return false;
    }
    if ((out.channels != 1 && out.channels != 2) || (out.bitsPerSample != 8 && out.bitsPerSample != 16)) {
        LOGE("Unsupported WAV format: channels=%u bits=%u", out.channels, out.bitsPerSample);
        return false;
    }
    return !out.pcm.empty();
}

SLuint32 sampleFormat(const AudioBuffer &buffer) {
    return buffer.bitsPerSample == 8 ? SL_PCMSAMPLEFORMAT_FIXED_8 : SL_PCMSAMPLEFORMAT_FIXED_16;
}

SLuint32 channelMask(const AudioBuffer &buffer) {
    return buffer.channels == 1 ? SL_SPEAKER_FRONT_CENTER : (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT);
}

SLuint32 milliHertz(uint32_t sampleRate) {
    return sampleRate * 1000u;
}

SLmillibel gainToMillibel(float gain) {
    float clamped = std::max(0.0f, std::min(gain * gMasterGain, 1.0f));
    if (clamped <= 0.00001f) {
        return -9600;
    }
    return static_cast<SLmillibel>(std::max(-9600.0f, 2000.0f * std::log10(clamped)));
}

void applySourceGain(const std::shared_ptr<AudioSource> &source) {
    if (source && source->volumeItf) {
        (*source->volumeItf)->SetVolumeLevel(source->volumeItf, gainToMillibel(source->gain));
    }
}

void destroySourceLocked(const std::shared_ptr<AudioSource> &source) {
    if (!source) {
        return;
    }
    if (source->playerObject) {
        (*source->playerObject)->Destroy(source->playerObject);
        source->playerObject = nullptr;
        source->playItf = nullptr;
        source->queueItf = nullptr;
        source->volumeItf = nullptr;
    }
    source->playing = false;
}

void enqueueSourceLocked(const std::shared_ptr<AudioSource> &source) {
    if (!source || !source->queueItf) {
        return;
    }
    auto bufferIt = gBuffers.find(source->bufferId);
    if (bufferIt == gBuffers.end() || bufferIt->second->pcm.empty()) {
        return;
    }
    const std::vector<uint8_t> &pcm = bufferIt->second->pcm;
    (*source->queueItf)->Enqueue(source->queueItf, pcm.data(), static_cast<SLuint32>(pcm.size()));
}

void bufferQueueCallback(SLAndroidSimpleBufferQueueItf queueItf, void *context) {
    (void) queueItf;
    int sourceId = static_cast<int>(reinterpret_cast<intptr_t>(context));
    std::lock_guard<std::mutex> lock(gMutex);
    auto sourceIt = gSources.find(sourceId);
    if (sourceIt == gSources.end()) {
        return;
    }
    std::shared_ptr<AudioSource> source = sourceIt->second;
    if (source->looping && source->playing) {
        enqueueSourceLocked(source);
        return;
    }
    source->playing = false;
    if (source->playItf) {
        (*source->playItf)->SetPlayState(source->playItf, SL_PLAYSTATE_STOPPED);
    }
}

bool ensureEngineLocked() {
    if (gEngineObject && gEngine && gOutputMixObject) {
        return true;
    }

    SLresult result = slCreateEngine(&gEngineObject, 0, nullptr, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("slCreateEngine failed: %d", result);
        return false;
    }
    result = (*gEngineObject)->Realize(gEngineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Engine Realize failed: %d", result);
        return false;
    }
    result = (*gEngineObject)->GetInterface(gEngineObject, SL_IID_ENGINE, &gEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Get engine interface failed: %d", result);
        return false;
    }
    result = (*gEngine)->CreateOutputMix(gEngine, &gOutputMixObject, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("CreateOutputMix failed: %d", result);
        return false;
    }
    result = (*gOutputMixObject)->Realize(gOutputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("OutputMix Realize failed: %d", result);
        return false;
    }
    LOGI("OpenSL ES audio engine ready");
    return true;
}

std::shared_ptr<AudioSource> findSourceLocked(int sourceId) {
    auto it = gSources.find(sourceId);
    return it != gSources.end() ? it->second : nullptr;
}

} // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_init(JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    ensureEngineLocked();
}

JNIEXPORT jint JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_createBuffer(JNIEnv *env, jclass, jbyteArray wavBytes) {
    if (!wavBytes) {
        return 0;
    }
    jsize length = env->GetArrayLength(wavBytes);
    std::vector<uint8_t> encoded(static_cast<size_t>(length));
    env->GetByteArrayRegion(wavBytes, 0, length, reinterpret_cast<jbyte *>(encoded.data()));

    std::shared_ptr<AudioBuffer> buffer = std::make_shared<AudioBuffer>();
    if (!parseWav(encoded.data(), encoded.size(), *buffer)) {
        return 0;
    }

    std::lock_guard<std::mutex> lock(gMutex);
    int id = gNextBufferId++;
    gBuffers[id] = buffer;
    return id;
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_deleteBuffer(JNIEnv *, jclass, jint bufferId) {
    std::lock_guard<std::mutex> lock(gMutex);
    gBuffers.erase(bufferId);
}

JNIEXPORT jint JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_createSource(JNIEnv *, jclass, jint bufferId) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!ensureEngineLocked() || gBuffers.find(bufferId) == gBuffers.end()) {
        return 0;
    }

    std::shared_ptr<AudioBuffer> buffer = gBuffers[bufferId];
    SLDataLocator_AndroidSimpleBufferQueue locatorQueue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 1};
    SLDataFormat_PCM formatPcm = {
            SL_DATAFORMAT_PCM,
            buffer->channels,
            milliHertz(buffer->sampleRate),
            sampleFormat(*buffer),
            sampleFormat(*buffer),
            channelMask(*buffer),
            SL_BYTEORDER_LITTLEENDIAN
    };
    SLDataSource audioSource = {&locatorQueue, &formatPcm};
    SLDataLocator_OutputMix locatorOutputMix = {SL_DATALOCATOR_OUTPUTMIX, gOutputMixObject};
    SLDataSink audioSink = {&locatorOutputMix, nullptr};
    const SLInterfaceID interfaces[] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    const SLboolean required[] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_FALSE};

    std::shared_ptr<AudioSource> source = std::make_shared<AudioSource>();
    source->id = gNextSourceId++;
    source->bufferId = bufferId;

    SLresult result = (*gEngine)->CreateAudioPlayer(gEngine, &source->playerObject, &audioSource, &audioSink, 2, interfaces, required);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("CreateAudioPlayer failed: %d", result);
        return 0;
    }
    result = (*source->playerObject)->Realize(source->playerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("AudioPlayer Realize failed: %d", result);
        destroySourceLocked(source);
        return 0;
    }
    (*source->playerObject)->GetInterface(source->playerObject, SL_IID_PLAY, &source->playItf);
    (*source->playerObject)->GetInterface(source->playerObject, SL_IID_BUFFERQUEUE, &source->queueItf);
    (*source->playerObject)->GetInterface(source->playerObject, SL_IID_VOLUME, &source->volumeItf);
    if (!source->playItf || !source->queueItf) {
        destroySourceLocked(source);
        return 0;
    }
    (*source->queueItf)->RegisterCallback(source->queueItf, bufferQueueCallback, reinterpret_cast<void *>(static_cast<intptr_t>(source->id)));
    applySourceGain(source);
    gSources[source->id] = source;
    return source->id;
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_deleteSource(JNIEnv *, jclass, jint sourceId) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    destroySourceLocked(source);
    gSources.erase(sourceId);
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_play(JNIEnv *, jclass, jint sourceId) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    if (!source || !source->playItf || !source->queueItf) {
        return;
    }
    (*source->queueItf)->Clear(source->queueItf);
    enqueueSourceLocked(source);
    source->playing = true;
    (*source->playItf)->SetPlayState(source->playItf, SL_PLAYSTATE_PLAYING);
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_pause(JNIEnv *, jclass, jint sourceId) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    if (source && source->playItf) {
        source->playing = false;
        (*source->playItf)->SetPlayState(source->playItf, SL_PLAYSTATE_PAUSED);
    }
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_stop(JNIEnv *, jclass, jint sourceId) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    if (source && source->playItf && source->queueItf) {
        source->playing = false;
        (*source->playItf)->SetPlayState(source->playItf, SL_PLAYSTATE_STOPPED);
        (*source->queueItf)->Clear(source->queueItf);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_isPlaying(JNIEnv *, jclass, jint sourceId) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    return source && source->playing ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setLooping(JNIEnv *, jclass, jint sourceId, jboolean looping) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    if (source) {
        source->looping = looping == JNI_TRUE;
    }
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setGain(JNIEnv *, jclass, jint sourceId, jfloat gain) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::shared_ptr<AudioSource> source = findSourceLocked(sourceId);
    if (source) {
        source->gain = std::max(0.0f, gain);
        applySourceGain(source);
    }
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setPitch(JNIEnv *, jclass, jint, jfloat) {
    // OpenSL ES buffer-queue playback does not support per-source pitch changes.
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setPosition(JNIEnv *, jclass, jint, jfloat, jfloat, jfloat) {
    // Positional audio is reserved for a future 3D mixer; sources currently play as non-positional UI/world sounds.
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setListenerPosition(JNIEnv *, jclass, jfloat, jfloat, jfloat) {
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setListenerOrientation(JNIEnv *, jclass, jfloat, jfloat, jfloat, jfloat, jfloat, jfloat) {
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_setMasterGain(JNIEnv *, jclass, jfloat gain) {
    std::lock_guard<std::mutex> lock(gMutex);
    gMasterGain = std::max(0.0f, gain);
    for (auto &entry : gSources) {
        applySourceGain(entry.second);
    }
}

JNIEXPORT void JNICALL
Java_com_njst_gaming_android_audio_NativeAudio_shutdown(JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    for (auto &entry : gSources) {
        destroySourceLocked(entry.second);
    }
    gSources.clear();
    gBuffers.clear();
    if (gOutputMixObject) {
        (*gOutputMixObject)->Destroy(gOutputMixObject);
        gOutputMixObject = nullptr;
    }
    if (gEngineObject) {
        (*gEngineObject)->Destroy(gEngineObject);
        gEngineObject = nullptr;
        gEngine = nullptr;
    }
}

}
