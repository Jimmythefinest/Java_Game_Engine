package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Engine;

import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;

public final class BattleArenaGpuExperimentApp extends Engine {
    private final BattleArenaGpuExperimentLoader experimentLoader = new BattleArenaGpuExperimentLoader();
    private final boolean captureEnabled;
    private final int captureFrames;
    private final float captureDurationSeconds;
    private final int captureColumns;
    private final String captureOutputPath;
    private BufferedImage atlas;
    private int capturedFrames;

    public BattleArenaGpuExperimentApp() {
        title = "Battle Arena GPU Experiment";
        captureEnabled = Boolean.getBoolean("battleArena.gpuExperiment.capture");
        captureFrames = Math.max(1, Integer.getInteger("battleArena.gpuExperiment.frames", 16));
        captureDurationSeconds = Math.max(0f,
                readFloatProperty("battleArena.gpuExperiment.durationSeconds", 1f));
        width = readIntProperty(
                "battleArena.gpuExperiment.spriteWidth",
                Integer.getInteger("battleArena.gpuExperiment.width", 960));
        height = readIntProperty(
                "battleArena.gpuExperiment.spriteHeight",
                Integer.getInteger("battleArena.gpuExperiment.height", 540));
        captureColumns = Math.max(1, Integer.getInteger(
                "battleArena.gpuExperiment.columns",
                (int) Math.ceil(Math.sqrt(captureFrames))));
        captureOutputPath = System.getProperty(
                "battleArena.gpuExperiment.output",
                "build/generated/battle-arena-gpu-experiment-atlas.png");
    }

    @Override
    protected void onInit() {
        scene.loader = experimentLoader;
        experimentLoader.setResolution(width, height);
    }

    @Override
    protected void onKey(int key, int action) {
    }

    @Override
    protected void onUpdate() {
        experimentLoader.setResolution(width, height);
        if (!captureEnabled) {
            experimentLoader.setTimeSeconds(System.nanoTime() / 1000000000f);
            return;
        }
        float progress = captureFrames <= 1
                ? 0f
                : capturedFrames / (float) (captureFrames - 1);
        experimentLoader.setTimeSeconds(progress * captureDurationSeconds);
    }

    @Override
    protected void afterDrawFrame() {
        if (!captureEnabled || capturedFrames >= captureFrames) {
            return;
        }
        captureCurrentFrame();
        capturedFrames++;
        if (capturedFrames >= captureFrames) {
            writeAtlas();
            glfwSetWindowShouldClose(window, true);
        }
    }

    public static void main(String[] args) {
        applyArgs(args);
        new BattleArenaGpuExperimentApp().run();
    }

    private void captureCurrentFrame() {
        int rows = (int) Math.ceil(captureFrames / (float) captureColumns);
        if (atlas == null) {
            atlas = new BufferedImage(
                    width * captureColumns,
                    height * rows,
                    BufferedImage.TYPE_INT_ARGB);
        }
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        int column = capturedFrames % captureColumns;
        int row = capturedFrames / captureColumns;
        int baseX = column * width;
        int baseY = row * height;
        for (int y = 0; y < height; y++) {
            int targetY = baseY + (height - 1 - y);
            for (int x = 0; x < width; x++) {
                int offset = ((y * width) + x) * 4;
                int r = pixels.get(offset) & 0xff;
                int g = pixels.get(offset + 1) & 0xff;
                int b = pixels.get(offset + 2) & 0xff;
                int a = pixels.get(offset + 3) & 0xff;
                atlas.setRGB(baseX + x, targetY, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        System.out.println("[GpuExperiment] captured frame " + (capturedFrames + 1)
                + "/" + captureFrames);
    }

    private void writeAtlas() {
        File output = new File(captureOutputPath);
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create atlas output directory: " + parent);
        }
        try {
            ImageIO.write(atlas, "png", output);
            System.out.println("[GpuExperiment] wrote atlas " + output.getAbsolutePath()
                    + " frames=" + captureFrames
                    + " frameSize=" + width + "x" + height
                    + " columns=" + captureColumns);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write atlas: " + output.getAbsolutePath(), e);
        }
    }

    private static void applyArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            if ("--capture".equals(arg)) {
                System.setProperty("battleArena.gpuExperiment.capture", "true");
            } else if (arg.startsWith("--frames=")) {
                System.setProperty("battleArena.gpuExperiment.frames", arg.substring("--frames=".length()));
            } else if (arg.startsWith("--duration=")) {
                System.setProperty("battleArena.gpuExperiment.durationSeconds", arg.substring("--duration=".length()));
            } else if (arg.startsWith("--width=")) {
                System.setProperty("battleArena.gpuExperiment.width", arg.substring("--width=".length()));
            } else if (arg.startsWith("--height=")) {
                System.setProperty("battleArena.gpuExperiment.height", arg.substring("--height=".length()));
            } else if (arg.startsWith("--spriteWidth=")) {
                System.setProperty("battleArena.gpuExperiment.spriteWidth", arg.substring("--spriteWidth=".length()));
            } else if (arg.startsWith("--spriteHeight=")) {
                System.setProperty("battleArena.gpuExperiment.spriteHeight", arg.substring("--spriteHeight=".length()));
            } else if (arg.startsWith("--columns=")) {
                System.setProperty("battleArena.gpuExperiment.columns", arg.substring("--columns=".length()));
            } else if (arg.startsWith("--output=")) {
                System.setProperty("battleArena.gpuExperiment.output", arg.substring("--output=".length()));
            }
        }
    }

    private static float readFloatProperty(String property, float fallback) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int readIntProperty(String property, int fallback) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            return Math.max(1, fallback);
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return Math.max(1, fallback);
        }
    }
}
