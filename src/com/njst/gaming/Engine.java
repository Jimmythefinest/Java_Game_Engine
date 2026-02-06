package com.njst.gaming;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import com.njst.gaming.Math.Vector3;
import java.util.Scanner;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Base class for desktop-based engine applications.
 * Handles GLFW window lifecycle, input routing, and the main loop.
 */
public abstract class Engine {
    protected int width = 800;
    protected int height = 600;
    protected String title = "Game Engine";
    protected long window;
    
    protected Renderer renderer;
    protected Scene scene;
    protected Input input;

    public void run() {
        init();
        loop();
        cleanup();
    }

    protected void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        centerWindow();
        setupCallbacks();
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        scene = new Scene();
        renderer = new Renderer();
        input = new Input();
        renderer.scene = scene;
        scene.renderer = renderer;

        onInit();
        
        renderer.onSurfaceCreated();
        renderer.onSurfaceChanged(width, height);
    }

    private void centerWindow() {
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(
            window,
            (vidmode.width() - width) / 2,
            (vidmode.height() - height) / 2
        );
    }

    protected void setupCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
            input.setKey(key, action);
            onKey(key, action);
        });

        glfwSetWindowSizeCallback(window, (win, w, h) -> {
            this.width = w;
            this.height = h;
            renderer.onSurfaceChanged(w, h);
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            input.setMousePosition(x, y);
            scene.cursorMoved(x, y);
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            input.setMouseButton(button, action);
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                scene.righmouse = action == GLFW_PRESS;
            }
        });

        glfwSetScrollCallback(window, (win, xOffset, yOffset) -> {
            input.setScroll(xOffset, yOffset);
        });
    }

    /**
     * Hook for subclasses to handle key events.
     */
    protected abstract void onKey(int key, int action);

    /**
     * Hook for subclasses to perform initialization (e.g. scene loading).
     */
    protected abstract void onInit();

    private void loop() {
        long lastTime = System.currentTimeMillis();
        int frameCount = 0;

        startConsoleThread();

        while (!glfwWindowShouldClose(window)) {
            if (frameCount == 30) {
                long currentTime = System.currentTimeMillis();
                glfwSetWindowTitle(window, title + " - FPS: " + (30000 / (currentTime - lastTime)));
                lastTime = currentTime;
                frameCount = 0;
            }
            frameCount++;

            input.update();
            onUpdate();
            renderer.onDrawFrame();
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    /**
     * Hook for custom update logic per frame.
     */
    protected void onUpdate() {
        // Default implementation
    }

    private void startConsoleThread() {
        Thread consoleThread = new Thread(() -> {
            Scanner scan = new Scanner(System.in);
            while (scan.hasNextLine()) {
                String input = scan.nextLine();
                if (input.equals("exit")) {
                    scan.close();
                    System.exit(0);
                }
                onConsoleInput(input);
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    /**
     * Hook for console-based commands.
     */
    protected void onConsoleInput(String input) {
        String[] args = input.split(" ");
        if (args[0].equals("tp") && args.length >= 4) {
            try {
                renderer.camera.cameraPosition = new Vector3(
                    Float.parseFloat(args[1]),
                    Float.parseFloat(args[2]),
                    Float.parseFloat(args[3])
                );
            } catch (NumberFormatException e) {
                System.out.println("Invalid coordinates for tp");
            }
        }
    }

    protected void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
