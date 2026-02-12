package com.njst.gaming.Natives;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Provides a way to create an OpenGL context without a visible window.
 * Useful for compute shaders, off-screen rendering, or background processing.
 */
public class HeadlessContext {
    private long window = NULL;
    private final int width;
    private final int height;
    private final String title;

    public HeadlessContext() {
        this(1, 1, "Headless Context");
    }

    public HeadlessContext(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    /**
     * Initializes GLFW and creates an invisible window with an OpenGL context.
     * @throws IllegalStateException if GLFW initialization fails or window creation fails.
     */
    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW for headless operation
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Invisible window
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        
        // Request a core profile if possible
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            // Fallback to default if 3.3 core fails
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            window = glfwCreateWindow(width, height, title, NULL, NULL);
            if (window == NULL) {
                throw new RuntimeException("Failed to create the GLFW window");
            }
        }

        makeCurrent();
        GL.createCapabilities();
    }

    /**
     * Makes the OpenGL context current in the calling thread.
     */
    public void makeCurrent() {
        if (window != NULL) {
            glfwMakeContextCurrent(window);
        }
    }

    /**
     * Destroys the window and terminates GLFW.
     */
    public void destroy() {
        if (window != NULL) {
            glfwDestroyWindow(window);
            window = NULL;
        }
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    public long getWindowHandle() {
        return window;
    }
}
