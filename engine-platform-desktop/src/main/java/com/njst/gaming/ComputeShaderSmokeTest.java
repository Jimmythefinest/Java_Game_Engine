package com.njst.gaming;

import com.njst.gaming.Natives.DesktopComputeBackend;
import com.njst.gaming.Natives.HeadlessContext;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

public final class ComputeShaderSmokeTest {
    private static final String SHADER =
            "#version 430 core\n"
                    + "layout(local_size_x = 1) in;\n"
                    + "layout(std430, binding = 5) buffer DataBuffer {\n"
                    + "    float data[];\n"
                    + "};\n"
                    + "void main() {\n"
                    + "    uint id = gl_GlobalInvocationID.x;\n"
                    + "    data[id] = data[id] * float(id);\n"
                    + "}\n";

    private ComputeShaderSmokeTest() {
    }

    public static void main(String[] args) {
        HeadlessContext context = new HeadlessContext(1, 1, "Compute Shader Smoke Test");
        DesktopComputeBackend compute = null;
        try {
            context.init();
            System.out.println("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
            System.out.println("Renderer:       " + GL11.glGetString(GL11.GL_RENDERER));

            compute = new DesktopComputeBackend(SHADER);
            if (compute.hasError()) {
                throw new IllegalStateException(compute.getError());
            }

            float[] input = new float[] {4f, 3f, 2f, 1f, 9f, 2f};
            float[] expected = new float[] {0f, 3f, 4f, 3f, 36f, 10f};
            compute.bindBuffer(5, input);
            compute.dispatch(input.length, 1, 1);
            float[] actual = compute.readBuffer(5);
            if (!matches(expected, actual, 0.0001f)) {
                throw new IllegalStateException("Compute mismatch expected="
                        + Arrays.toString(expected)
                        + " actual=" + Arrays.toString(actual));
            }
            System.out.println("SUCCESS: compute shader SSBO dispatch/readback works: "
                    + Arrays.toString(actual));
        } finally {
            if (compute != null) {
                compute.release();
            }
            context.destroy();
        }
    }

    private static boolean matches(float[] expected, float[] actual, float epsilon) {
        if (expected == null || actual == null || expected.length != actual.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }
}
