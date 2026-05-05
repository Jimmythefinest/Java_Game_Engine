package com.njst.gaming.graphics;

/**
 * An abstraction for a generic GPU buffer, such as a Shader Storage Buffer Object (SSBO).
 * Used for uploading large contiguous arrays of data (e.g., animation matrices) to the GPU.
 */
public interface BufferHandle {
    /**
     * Binds this buffer to the active context.
     */
    void bind();

    /**
     * Unbinds this buffer from the active context.
     */
    void unbind();

    /**
     * Allocates and sets float data to the buffer.
     * @param data the float array to upload
     * @param usage the platform usage hint (e.g. dynamic draw)
     */
    void setData(float[] data, int usage);

    /**
     * Allocates and sets integer data to the buffer.
     * @param data the integer array to upload
     * @param usage the platform usage hint (e.g. dynamic draw)
     */
    void setData(int[] data, int usage);

    /**
     * Updates an already allocated buffer with new float data.
     * @param data the new float array data
     */
    void updateData(float[] data);

    /**
     * Updates an already allocated buffer with new integer data.
     * @param data the new integer array data
     */
    void updateData(int[] data);

    /**
     * Binds the buffer base to a specific binding point in the shader program.
     * @param bindingPoint the layout binding index
     */
    void bindToShader(int bindingPoint);

    /**
     * Reads a slice of float data back from the GPU.
     * @param numElements the number of floats to read
     * @return the retrieved float array
     */
    float[] getData(int numElements);

    /**
     * Releases the buffer resources on the GPU.
     */
    void delete();

    /**
     * @return the raw platform buffer ID
     */
    int getId();
}
