package com.njst.gaming.Natives;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.Utils;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackMallocInt;

public class ShaderProgram {
    public int programId;
    public String log = "";

    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShaderId = compileShader(GL30.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderId = compileShader(GL30.GL_FRAGMENT_SHADER, fragmentShaderSource);

        programId = GL30.glCreateProgram();
        GL30.glAttachShader(programId, vertexShaderId);
        GL30.glAttachShader(programId, fragmentShaderId);
        GL30.glLinkProgram(programId);
        GL30.glValidateProgram(programId);

        GL30.glDeleteShader(vertexShaderId);
        GL30.glDeleteShader(fragmentShaderId);
    }

    public boolean compiled() {
        int[] status = new int[2];
        GL30.glGetProgramiv(programId, GL30.GL_LINK_STATUS, status);
        return !(status[0] == GL30.GL_FALSE);
    }

    public String getLog() {
        return null;
    }

    public static String loadShader(String filePath) {
        StringBuilder shaderSource = new StringBuilder();

        // Try loading from classpath first (for JAR packaging)
        try (InputStream is = ShaderProgram.class.getResourceAsStream(filePath);
                BufferedReader reader = is != null ? new BufferedReader(new InputStreamReader(is)) : null) {
            if (reader != null) {
                String line;
                while ((line = reader.readLine()) != null) {
                    shaderSource.append(line).append("\n");
                }
                return shaderSource.toString();
            }
        } catch (IOException e) {
            // Fall through to file system loading
        }

        // Fallback to file system loading
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load shader: " + filePath);
        }
        return shaderSource.toString();
    }

    private int compileShader(int type, String source) {
        // Create the shader object
        int shaderId = GL30.glCreateShader(type);

        // Check if shader creation was successful
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader of type " + type);
        }

        // Set the shader source code
        GL30.glShaderSource(shaderId, source);

        // Compile the shader
        GL30.glCompileShader(shaderId);

        // Check for compilation errors
        int[] compileStatus = new int[1];
        GL30.glGetShaderiv(shaderId, GL30.GL_COMPILE_STATUS, compileStatus);

        if (compileStatus[0] == GL30.GL_FALSE) {
            // Get the error log
            String errorLog = GL30.glGetShaderInfoLog(shaderId);
            GL30.glDeleteShader(shaderId); // Delete the shader if compilation failed
            throw new RuntimeException("Error compiling shader: " + errorLog);
        }

        return shaderId; // Return the compiled shader ID
    }

    public void use() {
        GL30.glUseProgram(programId);
    }

    public void cleanup() {
        GL30.glDeleteProgram(programId);
    }

    public int getUniformLocation(String name) {
        return GL30.glGetUniformLocation(programId, name);
    }

    public int getTextureLocation(String name) {
        return GL30.glGetUniformLocation(programId, name);
    }

    public void setUniformMatrix4fv(int location, float[] matrix) {
        GL30.glUniformMatrix4fv(location, false, matrix);
        // try (MemoryStack stack = stackPush()) {
        // FloatBuffer buffer = stack.mallocFloat(16); // 4x4 matrix
        // matrix.get(buffer); // Fill the buffer with matrix data
        // glUniformMatrix4fv(location, false, buffer); // Set the uniform matrix
        // }
    }

    public void setUniformVector3(int location, float[] vector3f) {
        GL30.glUniform3fv(location, vector3f);

    }

    public void setUniformVector3(String name, float[] vector3f) {
        GL30.glUniform3fv(getUniformLocation(name), vector3f);

    }

    public void setUniformMatrix4fv(String name, float[] matrix) {
        GL30.glUniformMatrix4fv(getUniformLocation(name), false, matrix);

    }

    public void ActivateTexture(int location, int textureID) {
        glActiveTexture(GL_TEXTURE0);
        GL30.glBindTexture(GL_TEXTURE_2D, textureID);
        GL30.glUniform1i(location, 0);
    }

    public void setUniformMatrix4fv(int location, Matrix4 matrix) {
        GL30.glUniformMatrix4fv(location, false, matrix.getAsBuffer()); // Set the uniform matrix

    }

    public void setUniformMatrix4fv(String name, Matrix4 vector3f) {
        setUniformMatrix4fv(getUniformLocation(name), vector3f);

    }

    public void setUniformVector3(int cameraPositionLocation, Vector3 vector3f) {
        FloatBuffer buffer = Utils.mallocFloat(3); // 3 floats
        buffer.put(vector3f.x).put(vector3f.y).put(vector3f.z);
        buffer.flip();
        GL30.glUniform3fv(cameraPositionLocation, buffer); // Set the uniform vector

    }

    public void setUniformVector3(String name, Vector3 vector3f) {
        setUniformVector3(getUniformLocation(name), vector3f);

    }

    public int getAttributeLocation(String string) {
        return GL30.glGetAttribLocation(programId, string);
    }

    public static int loadTexture(String path) {
        try {
            IntBuffer w = stackMallocInt(1), h = stackMallocInt(1), comp = stackMallocInt(1);
            ByteBuffer image = STBImage.stbi_load(path, w, h, comp, 4);
            if (image == null) {
                System.err.println("Failed to load texture: " + path);
                return 0;
            }
            int textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(), h.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            STBImage.stbi_image_free(image);
            return textureID;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}