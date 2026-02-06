package com.njst.gaming;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import org.lwjgl.opengl.GL30;

import org.lwjgl.opengl.GL15;

import com.njst.gaming.Math.*; 
import com.njst.gaming.Natives.*;
import com.njst.gaming.objects.GameObject;

public class Renderer {

    // Scene objects
    public GameObject screen;
    public GameObject skybox;
    public Scene scene;
    public Camera camera;
    public Camera lightCamera;
    private int textureHandle;
    public boolean hasError = false;


    // Shader programs
    public ShaderProgram shaderProgram;// , shadowShaderProgram, lineProgram;
    ShadowMap shadowMap;
    public float speed=1;
    public GameObject test;

    // Logging
    public RootLogger log;

    // Dimensions
    public int width = 100;
    public int height = 100;
    public float[] clearColor = { 0, 0, 0, 1 };

    int tex1;

    // Miscellaneous
    public float angle;
    public final float[] lightPos = { 0, 50f, 0 };
    public final float[] lightColor = { 1.0f, 1.0f, 1.0f };
    public SSBO ssbo=new SSBO();
    
    // Frame rate measurement
    public long lasttym = 0;
    public int fps;
    public int frame_counter = 0;

    // Shadow matrices
    public float[] lightViewMatrix, lightProjectionMatrix;

    // Bounding box (for drawing wireframes)
    public Renderer() {
        log = new RootLogger("home/nj/render.log");
        camera = new Camera(new Vector3(0f, 0f, -7f), new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f));
        lightCamera = new Camera(new Vector3(0f, 5f, 0f), new Vector3(0f, 0f, 0f), new Vector3(-1f, 10f, 0f));
        log.logToRootDirectory("Renderer initialized");
    }

    public void onSurfaceCreated() {
        try {
            lasttym = System.currentTimeMillis();

            // Initialize main shader program
            // Build and compile shader program
            shaderProgram = new ShaderProgram(
            ShaderProgram.loadShader("/resources/shaders/vert11.glsl"), ShaderProgram.loadShader("/resources/shaders/frag11.glsl"));
            scene.loader.load(scene);
                for (GameObject object : scene.objects) {
                    object.generateBuffers();
                }

        } catch (Exception e) {
            logException(e);
            e.printStackTrace();
        }
    }

    public void onDrawFrame() {
        if (hasError) return;
        try {
            long start=System.nanoTime();
            float[] consts=new float[39];
            System.arraycopy(camera.getProjectionMatrix().r,0,consts,0,16);
            System.arraycopy(camera.getViewMatrix().r,0,consts,16,16);
            System.arraycopy(camera.cameraPosition.toArray(),0,consts,32,3);
            System.arraycopy(new float[]{ 0,0, 100  , 0},0,consts,35,4);
            ssbo.setData(consts,GL15.GL_DYNAMIC_DRAW);
            ssbo.bind();
            ssbo.bindToShader(0);
            
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // shaderProgram.setUniformVector3("lightpos", new float[] { 0, 10, 0 });
        shaderProgram.setUniformVector3("eyepos1", camera.cameraPosition);
        // shaderProgram.setUniformMatrix4fv("uPMatrix", camera.getProjectionMatrix());
        // shaderProgram.setUniformMatrix4fv("uVMatrix", camera.getViewMatrix());
        
        // skybox.position=camera.cameraPosition;
        // skybox.updateModelMatrix();
        scene.onDrawFrame();
        for (GameObject object : scene.objects) {
            shaderProgram.use();
            object.updateModelMatrix();
            object.render(shaderProgram, textureHandle);
        }
        time+=(System.nanoTime()-start);
        if(frame==200){
            frame=0;
            System.out.println(time/200/1000000);
            time=0;
        }
        frame++;
    
        } catch (Exception e) {
            hasError = true;
            logException(e);
            e.printStackTrace();
        }
    }
    int frame=0;
    long time=0;

    float a = 0;
    

    public void onSurfaceChanged(int w, int h) {
        width = w;
        height = h;
        GL30.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        camera.setPerspective(45, ratio, 0.1f, 1000);

    }
    
    public synchronized void setFps(int i) {
        fps = i;
    }

    public synchronized int getFps() {
        return fps;
    }

    public synchronized long getlast() {
        return lasttym;
    }

    private void logException(Exception e) {
        log.logToRootDirectory(e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            log.logToRootDirectory(element.getClassName() + element.getMethodName() + element.getLineNumber());
        }
    }
}
