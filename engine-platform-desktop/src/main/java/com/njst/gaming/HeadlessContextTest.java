package com.njst.gaming;

import com.njst.gaming.Natives.HeadlessContext;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

public class HeadlessContextTest {
    public static void main(String[] args) {
        System.out.println("Starting HeadlessContext Test...");

        HeadlessContext context = new HeadlessContext(800, 600, "Test Context");
        
        try {
            System.out.println("Initializing context...");
            context.init();
            
            String version = glGetString(GL_VERSION);
            String renderer = glGetString(GL_RENDERER);
            String vendor = glGetString(GL_VENDOR);

            System.out.println("OpenGL Version: " + version);
            System.out.println("Renderer:       " + renderer);
            System.out.println("Vendor:         " + vendor);

            if (version != null && !version.isEmpty()) {
                System.out.println("SUCCESS: OpenGL context created and queried successfully.");
            } else {
                System.out.println("FAILURE: Could not query OpenGL version.");
            }

            // Test simple GL call
            glClearColor(0.1f, 0.2f, 0.3f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            int error = glGetError();
            if (error == GL_NO_ERROR) {
                System.out.println("SUCCESS: Basic OpenGL calls executed without error.");
            } else {
                System.out.println("FAILURE: OpenGL error detected: " + error);
            }

        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE: An exception occurred during context creation:");
            e.printStackTrace();
        } finally {
            System.out.println("Cleaning up...");
            context.destroy();
            System.out.println("HeadlessContext Test Finished.");
        }
    }
}
