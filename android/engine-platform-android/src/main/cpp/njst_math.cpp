#include <jni.h>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "njst_native_math"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jfloatArray JNICALL
Java_com_njst_gaming_android_NativeMath_multiplyMat4(JNIEnv *env, jclass clazz, jfloatArray a, jfloatArray b) {
    jfloat *a_ptr = env->GetFloatArrayElements(a, nullptr);
    jfloat *b_ptr = env->GetFloatArrayElements(b, nullptr);
    
    jfloatArray result = env->NewFloatArray(16);
    jfloat result_ptr[16];

    // Column-major multiplication
    for (int i = 0; i < 4; i++) { // column
        for (int j = 0; j < 4; j++) { // row
            float sum = 0;
            for (int k = 0; k < 4; k++) {
                sum += a_ptr[k * 4 + j] * b_ptr[i * 4 + k];
            }
            result_ptr[i * 4 + j] = sum;
        }
    }

    env->SetFloatArrayRegion(result, 0, 16, result_ptr);
    
    env->ReleaseFloatArrayElements(a, a_ptr, JNI_ABORT);
    env->ReleaseFloatArrayElements(b, b_ptr, JNI_ABORT);
    
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_njst_gaming_android_NativeMath_addVec3(JNIEnv *env, jclass clazz, jfloatArray a, jfloatArray b) {
    jfloat *a_ptr = env->GetFloatArrayElements(a, nullptr);
    jfloat *b_ptr = env->GetFloatArrayElements(b, nullptr);
    
    jfloatArray result = env->NewFloatArray(3);
    jfloat result_ptr[3];

    for (int i = 0; i < 3; i++) {
        result_ptr[i] = a_ptr[i] + b_ptr[i];
    }

    env->SetFloatArrayRegion(result, 0, 3, result_ptr);
    
    env->ReleaseFloatArrayElements(a, a_ptr, JNI_ABORT);
    env->ReleaseFloatArrayElements(b, b_ptr, JNI_ABORT);
    
    return result;
}

JNIEXPORT jfloat JNICALL
Java_com_njst_gaming_android_NativeMath_dotVec3(JNIEnv *env, jclass clazz, jfloatArray a, jfloatArray b) {
    jfloat *a_ptr = env->GetFloatArrayElements(a, nullptr);
    jfloat *b_ptr = env->GetFloatArrayElements(b, nullptr);
    
    float dot = 0;
    for (int i = 0; i < 3; i++) {
        dot += a_ptr[i] * b_ptr[i];
    }

    env->ReleaseFloatArrayElements(a, a_ptr, JNI_ABORT);
    env->ReleaseFloatArrayElements(b, b_ptr, JNI_ABORT);
    
    return dot;
}

JNIEXPORT jfloatArray JNICALL
Java_com_njst_gaming_android_NativeMath_normalizeVec3(JNIEnv *env, jclass clazz, jfloatArray v) {
    jfloat *v_ptr = env->GetFloatArrayElements(v, nullptr);
    
    float lengthSq = 0;
    for (int i = 0; i < 3; i++) {
        lengthSq += v_ptr[i] * v_ptr[i];
    }
    
    float length = std::sqrt(lengthSq);
    
    jfloatArray result = env->NewFloatArray(3);
    jfloat result_ptr[3];

    if (length > 0.000001f) {
        for (int i = 0; i < 3; i++) {
            result_ptr[i] = v_ptr[i] / length;
        }
    } else {
        result_ptr[0] = result_ptr[1] = result_ptr[2] = 0;
    }

    env->SetFloatArrayRegion(result, 0, 3, result_ptr);
    env->ReleaseFloatArrayElements(v, v_ptr, JNI_ABORT);
    
    return result;
}

}
