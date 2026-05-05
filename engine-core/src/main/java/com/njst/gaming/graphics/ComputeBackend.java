package com.njst.gaming.graphics;

/**
 * An abstraction for a GPU compute shader backend.
 * Enables arbitrary data parallel execution on the GPU, useful for neural networks
 * or highly parallel simulations outside of the standard graphics pipeline.
 */
public interface ComputeBackend {
    /**
     * @return true if the compute backend encountered a compilation or runtime error.
     */
    boolean hasError();

    /**
     * @return the string description of the error, if any.
     */
    String getError();

    /**
     * Checks if a buffer is currently bound at the specified index.
     * @param bindingIndex the binding point to check
     * @return true if a buffer exists at the binding point
     */
    boolean hasBuffer(int bindingIndex);

    /**
     * Retrieves the size of the buffer bound at the specified index.
     * @param bindingIndex the binding point
     * @return the buffer size in elements
     */
    int getBufferSize(int bindingIndex);

    /**
     * Binds a new float array as a buffer at the specified index.
     * @param bindingIndex the shader binding layout index
     * @param data the float data to bind
     */
    void bindBuffer(int bindingIndex, float[] data);

    /**
     * Binds a new integer array as a buffer at the specified index.
     * @param bindingIndex the shader binding layout index
     * @param data the integer data to bind
     */
    void bindBuffer(int bindingIndex, int[] data);

    /**
     * Updates an existing float buffer at the specified index.
     * @param bindingIndex the shader binding layout index
     * @param data the new float data
     */
    void updateBuffer(int bindingIndex, float[] data);

    /**
     * Updates an existing integer buffer at the specified index.
     * @param bindingIndex the shader binding layout index
     * @param data the new integer data
     */
    void updateBuffer(int bindingIndex, int[] data);

    /**
     * Dispatches compute work groups.
     * @param x the number of work groups in the X dimension
     * @param y the number of work groups in the Y dimension
     * @param z the number of work groups in the Z dimension
     */
    void dispatch(int x, int y, int z);

    /**
     * Reads float data back from the compute buffer.
     * @param bindingIndex the shader binding layout index
     * @return the float array mapped from the GPU buffer
     */
    float[] readBuffer(int bindingIndex);

    /**
     * Binds an existing buffer to a secondary binding point, 
     * allowing multiple shader inputs to alias the same memory.
     * @param sourceBindingIndex the original binding point containing the buffer
     * @param targetBindingIndex the new binding point to alias the buffer
     */
    void bindBufferToShaderBinding(int sourceBindingIndex, int targetBindingIndex);

    /**
     * Releases compute backend resources and cleans up the associated compute program.
     */
    void release();
}
