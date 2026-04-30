package com.njst.gaming.android;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class BoneCpuBenchmarkInstrumentedTest {
    private static final String TAG = "BoneCpuBenchmark";
    private static final int BONE_COUNT = 96;
    private static final int UPDATE_ITERATIONS = 20_000;
    private static final int MATRIX_ITERATIONS = 5_000;

    @Test
    public void benchmarkBoneUpdateAndMatrixPacking() {
        ArrayList<Bone> bones = createSkeleton(BONE_COUNT);
        Bone root = bones.get(0);
        for (Bone bone : bones) {
            bone.calculate_bind_matrix();
        }

        for (int i = 0; i < 2_000; i++) {
            animateRoot(root, i);
            root.update();
        }

        long updateStart = System.nanoTime();
        for (int i = 0; i < UPDATE_ITERATIONS; i++) {
            animateRoot(root, i);
            root.update();
        }
        long updateNanos = System.nanoTime() - updateStart;

        float[] packed = new float[bones.size() * 16];
        long matrixStart = System.nanoTime();
        for (int i = 0; i < MATRIX_ITERATIONS; i++) {
            animateRoot(root, i);
            root.update();
            int offset = 0;
            for (Bone bone : bones) {
                System.arraycopy(bone.getAnimationMatrix().r, 0, packed, offset, 16);
                offset += 16;
            }
        }
        long matrixNanos = System.nanoTime() - matrixStart;

        String result = "Bone CPU benchmark bones=" + BONE_COUNT
                + " updateOnly iterations=" + UPDATE_ITERATIONS
                + " totalMs=" + nanosToMillis(updateNanos)
                + " perUpdateUs=" + nanosToMicros(updateNanos / (double) UPDATE_ITERATIONS)
                + " updateAndPack iterations=" + MATRIX_ITERATIONS
                + " totalMs=" + nanosToMillis(matrixNanos)
                + " perIterationUs=" + nanosToMicros(matrixNanos / (double) MATRIX_ITERATIONS)
                + " checksum=" + checksum(packed);
        Log.i(TAG, result);
        System.out.println(result);
    }

    private static ArrayList<Bone> createSkeleton(int boneCount) {
        ArrayList<Bone> bones = new ArrayList<Bone>();
        Bone root = createBone(0);
        bones.add(root);
        for (int i = 1; i < boneCount; i++) {
            Bone bone = createBone(i);
            Bone parent = bones.get((i - 1) / 2);
            parent.Children.add(bone);
            bones.add(bone);
        }
        root.set_Parent_position(new Vector3(0f, 0f, 0f));
        root.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        root.update();
        return bones;
    }

    private static Bone createBone(int index) {
        Bone bone = new Bone();
        bone.name = "benchmark_" + index;
        bone.position_to_parent.set(
                (index % 3) * 0.03f,
                0.08f + ((index % 5) * 0.01f),
                (index % 7) * 0.015f);
        bone.rotation.set(index % 11, index % 13, index % 17);
        return bone;
    }

    private static void animateRoot(Bone root, int iteration) {
        root.rotation.set(
                iteration % 360,
                (iteration * 2) % 360,
                (iteration * 3) % 360);
    }

    private static String nanosToMillis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }

    private static String nanosToMicros(double nanos) {
        return String.format("%.3f", nanos / 1_000.0);
    }

    private static float checksum(float[] values) {
        float sum = 0f;
        for (float value : values) {
            sum += value;
        }
        return sum;
    }
}
