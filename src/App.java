import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import com.njst.gaming.DefaultLoader;
import com.njst.gaming.Scene;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;

import java.nio.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class App {

    // The window handle
    private long window;

    public void run() {
        init();
        loop();

        // Free the window callbacks and destroy the window
        //glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    Scene scene;
    private void init() {
        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();
        scene=new Scene();
         scene.loader=new DefaultLoader();
         
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // The window will be hidden initially
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // The window will be resizable

        // Create the window
        window = glfwCreateWindow(800, 600, "Simple LWJGL App", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                window,
                (vidMode.width() - pWidth.get(0)) / 2,
                (vidMode.height() - pHeight.get(0)) / 2
            );
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v sync
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(window); // Show the window
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        // Initialize OpenGL
        GL.createCapabilities();
        shaderprogram = new ShaderProgram(
                    ShaderProgram.loadShader("/resources/shaders/vert1.glsl"),
                    ShaderProgram.loadShader("/resources/shaders/frag1.glsl")
            );
            shaderprogram.use();
            System.out.println(shaderprogram.log);
            obj=new GameObject(new CubeGeometry(), ShaderProgram.loadTexture("/jimmy/desertstorm.jpg"));
            obj.generateBuffers();
       // scene.getRenderer().onSurfaceCreated();
      //  scene.getRenderer().onSurfaceChanged(800,600);
    }
    GameObject obj;
    ShaderProgram shaderprogram;
    private void loop() {
        // Set the clear color
        System.out.println("looping");
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close the window
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer
            //scene.getRenderer().onDrawFrame();
            // Render a triangle
            obj.render(shaderprogram, shaderprogram.getTextureLocation("uTexture"));
            glBegin(GL_TRIANGLES);
            glColor3f(1.0f, 0.0f, 0.0f); // Red
            glVertex3f(0.0f, 0.5f,0); // Top vertex
            glColor3f(0.0f, 1.0f, 0.0f); // Green
            glVertex2f(-0.5f, -0.5f); // Bottom left vertex
            glColor3f(0.0f, 0.0f, 1.0f); // Blue
            glVertex2f(0.5f, -0.5f); // Bottom right vertex
            glEnd();
            

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new App().run();
    }
}