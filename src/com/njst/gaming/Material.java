package com.njst.gaming;

import org.lwjgl.opengl.GL30;

public class Material {
    public int program;
    static String vertexShaderCode = 
            "#version 310 es\n" +
            "vec3 a=vec3(0,1,2);"+
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uMMatrix;\n" +
            "uniform mat4 uPMatrix;\n" +
            "uniform mat4 uVMatrix;\n" +
            "layout(location = 0) in vec4 aPosition;\n" +
            "in vec4 aColor;\n" +
            "layout(location = 2)  in vec3 aNormal;\n" +
            "out vec4 vColor;\n" +
            "out vec3 vNormal;\n" + // Pass normal to fragment shader
            "layout(location = 1) in in vec2 aTexCoord;\n"+
            "out vec2 vTexCoord;\n"+// Receive normal from vertex shader
            "uniform vec3 vLightPos;\n" +
            "uniform vec3 CamPos;\n" +
            "out vec3 fragPos;"+
            "out vec3 lightPos;\n" +
            "void main() {\n" +
            "    vColor = aColor;\n" +
            "    vTexCoord =aTexCoord;"+
            "    fragPos = normalize(vec3(uMMatrix * aPosition));"+ // Transform vertex position
            "     lightPos = vec3(3,0,0);"+ // Transform vertex position
            "    vNormal = vec3((mat3(uMMatrix)*aNormal));\n" + // Pass normal
            "    gl_Position = uPMatrix*uVMatrix*uMMatrix * aPosition;\n" +
            "}";

        String fragmentShaderCod1e =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec4 vColor;\n" +
            "uniform sampler2D uTexture;\n"+
            "out vec4 fragColor;\n" +
            "in vec2 vTexCoord;"+
            "uniform vec4 uColor;\n" + 
            "in vec3 vNormal;\n" + // Receive normal from vertex shader
            
            "uniform vec3 vLightPos;\n" +
            "void main() {\n" +
            "    fragColor = texture(uTexture, vTexCoord);\n" +
            "    vec3 norm = normalize(vNormal);\n" + // Normalize the normal
        //   "    vec4 texColor = texture(uTexture, vTe"out vec3 fragPos;"+xCoord);\n"+
            "    vec3 lightDir = normalize(vLightPos - vec3(-1.0, 0.0, 0.0));\n" + // Light direction
            "    float diff = max(dot(norm, vLightPos), 0.0);\n" + // Diffuse lighting
            
            "    fragColor= diff * vec3(textColor);\n" + // Apply diffuse lighting
            "}";
      static  String fragmentShaderCode =
    "#version 300 es\n" +
    "precision highp float;\n" +
    "in vec4 vColor;\n" +
    "uniform sampler2D uTexture;\n" +
    "out vec4 fragColor;\n" +
    "in vec2 vTexCoord;\n" +
    "in vec3 vNormal;\n" + // Receive normal from vertex shader
    "uniform vec3 vLightPos;\n" +
"in vec3 fragPos;"+
"in vec3 lightPos;"+
    "void main() {\n" +
    "    vec4 texColor = texture(uTexture, vTexCoord);\n" + // Sample texture color once
    "    vec3 norm = normalize(vNormal);\n" + // Normalize the normal
    "    vec3 lightDir = normalize(fragPos-lightPos) ;\n" + // Light direction
    "    float diff = max(dot(norm, lightDir), 0.0);\n" + // Correct diffuse lighting calculation
    "    fragColor = vec4( texColor.rgb*diff, texColor.a); // Apply diffuse lighting and preserve alpha\n" +
    "}";
    public Material() {
       
    }

public void onSurfaceCreated(){
        // Set the background color
        org.lwjgl.opengl.GL30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL30.glEnable(GL30.GL_DEPTH_TEST);
        // Initialize shaders and create programN
        
        int vertexShader = loadShader(GL30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GL30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GL30.glCreateProgram();
        GL30.glAttachShader(program, vertexShader);
        GL30.glAttachShader(program, fragmentShader);
        GL30.glLinkProgram(program);
}
private int loadShader(int type, String shaderCode) {
        int shader = GL30.glCreateShader(type);
        GL30.glShaderSource(shader, shaderCode);
        GL30.glCompileShader(shader);
        return shader;
    }
    public void Activate(){
        GL30.glUseProgram(program);
    }
}
