
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GameObjectTest {

    private long window;
    private int width = 800, height = 600;

    public void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        window = glfwCreateWindow(width, height, "GameObject Test - Rotating Cube", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Center the window
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable vsync
        glfwShowWindow(window);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
    }

    private void loop() {
        // Create a basic shader program
        ShaderProgram shader = new ShaderProgram(
                ShaderProgram.loadShader("/resources/shaders/vert.glsl"),
                ShaderProgram.loadShader("/resources/shaders/frag.glsl"));

        // Create a dummy cube geometry and a GameObject from it.
        CubeGeometry cubeGeometry = new CubeGeometry();
        // Assume texture id 0 (no texture)#
        int texture1 = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        System.out.println(texture1);
        System.err.println("Shader log: " + shader.log);
        System.out.println("Shader compile status: " + shader.compiled());
        System.out.println("Matrix Location: " + shader.getUniformLocation("uMMatrix"));

        GameObject cube = new GameObject(cubeGeometry, texture1);
        cube.generateBuffers();

        // Main render loop
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Update the cube: rotate around Y-axis
            cube.rotate(0, 1, 0);

            // Use the shader program
            shader.use();
            // (Optionally, set uniforms like projection/view matrices here)

            // Render the cube GameObject
            cube.render(shader, 0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new GameObjectTest().run();
    }
}
