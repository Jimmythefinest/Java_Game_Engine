package com.njst.gaming;

public class SSBOManager {/* 
    private int ssboId;
    // Function to create and send data to SSBO
    public void createAndSendSSBO(float[] data) {
        // Generate and bind the SSBO
        int[] bufferIds = new int[1];
        
        GL30.glGenBuffers( bufferIds);
        ssboId = bufferIds[0];

        GL30.glBindBuffer(GL30.GL_SHADER_STORAGE_BUFFER, ssboId);

        // Allocate and upload data to the SSBO
        // Use Float.BYTES to calculate the size of the data
        GL30.glBufferData(GL30.GL_SHADER_STORAGE_BUFFER, data.length * Float.BYTES, 
                            FloatBuffer.wrap(data), GL30.GL_STATIC_DRAW);

        // Unbind the SSBO
        GL30.glBindBuffer(GL30.GL_SHADER_STORAGE_BUFFER, 0);
    }
    public void createAndSendSSBO(short[] data) {
        // Generate and bind the SSBO
        int[] bufferIds = new int[1];
        GL30.glGenBuffers(1, bufferIds, 0);
        ssboId = bufferIds[0];

        GL30.glBindBuffer(GL30.GL_SHADER_STORAGE_BUFFER, ssboId);

        // Allocate and upload data to the SSBO
        // Use Float.BYTES to calculate the size of the data
        GL30.glBufferData(GL30.GL_SHADER_STORAGE_BUFFER, data.length * Short.BYTES, 
                            ShortBuffer.wrap(data), GL30.GL_STATIC_DRAW);

        // Unbind the SSBO
        GL30.glBindBuffer(GL30.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void bindSSBO(int bindingPoint) {
        // Bind the SSBO to a specific binding point
        GL30.glBindBufferBase(GL30.GL_SHADER_STORAGE_BUFFER, bindingPoint, ssboId);
    }

    public void cleanup() {
        // Cleanup the SSBO
        if (ssboId != 0) {
            int[] bufferIds = { ssboId };
            GL30.glDeleteBuffers(1, bufferIds, 0);
            ssboId = 0;
        }
    }*/
}