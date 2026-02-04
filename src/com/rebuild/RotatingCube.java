package com.rebuild;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import com.njst.gaming.SceneLoaders.*;
import com.njst.gaming.*;
import com.njst.gaming.Loaders.ai_training;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.Scanner;

public class RotatingCube {

    // Window dimensions
    private final int width = 800;
    private final int height = 600;
    private long window;
    Renderer renderer;

    GameObject cube;


    public void run() {
        System.out.println("LWJGL Rotating Cube");
        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        System.exit(0);
      //  glfwSetErrorCallback(null).free();
    }
    
    private void init() {
        Scene scene=new Scene();
        // Setup error callback
        //GLFWErrorCallback.createPrint(System.err).set();
	System.out.println("hello ............................................................");
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Window stays hidden
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Rotating Cube", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Center window
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(
            window,
            (vidmode.width() - width) / 2,
            (vidmode.height() - height) / 2
        );

        // Set key callback to close window on ESC
        glfwSetKeyCallback(window, (window, key, na, action, na1) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE ){
                glfwSetWindowShouldClose(window, true);
            }
            switch (key) {
                case GLFW_KEY_X:
                    if(action==GLFW_PRESS)scene.renderer.camera_should_move=true;
                    if(action==GLFW_RELEASE)scene.renderer.camera_should_move=false;
                    break;
                case GLFW_KEY_Z:
                    if(action==GLFW_PRESS)scene.speed*=10;
                    if(action==GLFW_RELEASE)scene.speed/=10;
                    break;
                case GLFW_KEY_Y:
                    scene.renderer.camera.targetPosition=new Vector3();
                case GLFW_KEY_O:
                    if(action==GLFW_PRESS){
                        scene.KEY_ANIMATIONS.forEach((value)->{
                            value.time=0;
                            value.start();
                        });
                    }
                    break;
                case GLFW_KEY_Q:
                    scene.addTetra();
                default:
                    if(scene.actions.containsKey(key))scene.actions.get(key).run();
                    break;
            }
        
            });
        glfwSetWindowSizeCallback(window,(na,h,w)->{
            scene.renderer.onSurfaceChanged(h,w);
        });

        glfwSetCursorPosCallback(window,(na,pos_x,pos_y)->{
            scene.cursorMoved(pos_x,pos_y);
        });
        glfwSetMouseButtonCallback(window, (na,button,action,na1)->{
            if(button==GLFW_MOUSE_BUTTON_LEFT){
                scene.righmouse=action==GLFW_PRESS;
            }
        });
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable vsync
        glfwShowWindow(window);

        // Create OpenGL context
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        renderer=new Renderer();
        renderer.scene=scene;
        scene.renderer=renderer;
        scene.loader=new DefaultLoader();
        renderer.onSurfaceCreated();
        scene.renderer.onSurfaceChanged(width,height);

       
    }
    
    private void loop() {
       
        long last=System.currentTimeMillis();
        int frame=0;
        // Render loop
        Thread na=new Thread(){
            public void run(){
                Scanner scan=new Scanner(System.in);
                while(scan.hasNextLine()){
                    String input=scan.nextLine();
                    if(input.equals("exit")){
                        scan.close();
                        System.exit(0);
                    }
                    System.out.println(input.split(" ")[0]);
                    if (input.split(" ")[0].equals("tp")) {
                       
                        renderer.camera.cameraPosition=new Vector3(
                            Float.parseFloat(input.split(" ")[1]),
                            Float.parseFloat(input.split(" ")[2]),
                            Float.parseFloat(input.split(" ")[3])
                        );
                    }
                }
            }
        };
        // console.start();
        while (!glfwWindowShouldClose(window)) {
            if(frame==30){
                
                long current=System.currentTimeMillis();
                glfwSetWindowTitle(window, "FPS:"+(30000/(current-last)));
                last=current;
                frame=0;
            }
            frame++;
            renderer.onDrawFrame();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    // Simple lookAt matrix (column-major order)
    
    // Create a model matrix with rotation around the Y axis
   
  
    public static void main(String[] args) {
        new RotatingCube().run();
    }
}

