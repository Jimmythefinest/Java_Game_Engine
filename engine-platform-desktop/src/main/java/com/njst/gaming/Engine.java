package com.njst.gaming;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.DesktopGraphicsDevice;
import com.njst.gaming.input.InputBindings;
import com.njst.gaming.input.InputSystem;
import com.njst.gaming.input.MouseButtons;
import java.util.Scanner;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public abstract class Engine {
    protected int width = 800;
    protected int height = 600;
    protected String title = "Game Engine";
    protected long window;

    protected Renderer renderer;
    protected Scene scene;
    protected InputSystem input;

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

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        scene = new Scene();
        renderer = new Renderer(new DesktopGraphicsDevice());
        input = scene.inputSystem;
        renderer.scene = scene;
        scene.renderer = renderer;
        configureInputBindings(scene.inputBindings);
        setupCallbacks();

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

    protected void configureInputBindings(InputBindings bindings) {
    }

    protected void setupCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
            applyBoundAction(scene.inputBindings.resolveKeyAction(key), action != GLFW_RELEASE);
            onKey(key, action);
        });

        glfwSetWindowSizeCallback(window, (win, w, h) -> {
            this.width = w;
            this.height = h;
            renderer.onSurfaceChanged(w, h);
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            String pointerId = scene.inputBindings.resolveMousePointer();
            if (pointerId != null && !pointerId.isEmpty()) {
                scene.handlePointerInput(pointerId, (float) x, (float) y);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            int mouseButton = mapGlfwMouseButton(button);
            if (mouseButton >= 0) {
                applyBoundAction(scene.inputBindings.resolveMouseButtonAction(mouseButton), action != GLFW_RELEASE);
            }
        });

        glfwSetScrollCallback(window, (win, xOffset, yOffset) -> {
        });
    }

    private void applyBoundAction(String actionId, boolean down) {
        if (actionId == null || actionId.isEmpty()) {
            return;
        }
        input.button(actionId).setDown(down);
    }

    private int mapGlfwMouseButton(int glfwMouseButton) {
        switch (glfwMouseButton) {
            case GLFW_MOUSE_BUTTON_LEFT:
                return MouseButtons.LEFT;
            case GLFW_MOUSE_BUTTON_RIGHT:
                return MouseButtons.RIGHT;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                return MouseButtons.MIDDLE;
            default:
                return -1;
        }
    }

    protected abstract void onKey(int key, int action);

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

            if (!renderer.hasError) {
                onUpdate();
                renderer.onDrawFrame();
            }

            glfwSwapBuffers(window);
            input.beginFrame();
            glfwPollEvents();
        }
    }

    protected void onUpdate() {
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
