package com.njst.gaming.ai;

import com.njst.gaming.graphics.ComputeBackend;

import java.util.Arrays;

/**
 * A GPU-accelerated Neural Network implementation.
 * Extends standard NeuralNetwork to allow for single evaluation (CPU/GPU) 
 * and provides static batch processing for multiple instances.
 */
public class GPUNeuralNetwork extends NeuralNetwork {
    private final int[] layerSizes;
    private final int numLayers;
    private final int totalWeights;
    private final int totalBiases;
    private final int inputSize;
    private final int outputSize;

    private final ComputeBackend computeBackend;
    
    // Binding configurations
    public int bindingInput = 0;
    public int bindingWeights = 1;
    public int bindingBiases = 2;
    public int bindingOutput = 3;
    public int bindingMeta = 4;

    private final int[] weightLayerOffsets;
    private final int[] biasLayerOffsets;

    public GPUNeuralNetwork(int[] layerSizes, float learningRate, boolean useLinearOutput, ComputeBackend backend) {
        super(layerSizes, learningRate, useLinearOutput);
        if (backend == null) {
            throw new IllegalArgumentException("ComputeBackend is required for GPUNeuralNetwork.");
        }
        this.layerSizes = Arrays.copyOf(layerSizes, layerSizes.length);
        this.numLayers = layerSizes.length;
        this.inputSize = layerSizes[0];
        this.outputSize = layerSizes[numLayers - 1];

        this.weightLayerOffsets = new int[numLayers];
        this.biasLayerOffsets = new int[numLayers];

        int currentWOffset = 0;
        int currentBOffset = 0;

        for (int l = 0; l < numLayers - 1; l++) {
            weightLayerOffsets[l] = currentWOffset;
            biasLayerOffsets[l] = currentBOffset;
            currentWOffset += layerSizes[l] * layerSizes[l + 1];
            currentBOffset += layerSizes[l + 1];
        }
        
        this.totalWeights = currentWOffset;
        this.totalBiases = currentBOffset;
        weightLayerOffsets[numLayers - 1] = totalWeights;
        biasLayerOffsets[numLayers - 1] = totalBiases;

        this.computeBackend = backend;
        if (this.computeBackend.hasError()) {
            throw new RuntimeException("GPU Neural Network Backend Error: " + this.computeBackend.getError());
        }
        syncWithCPU();
    }

    /**
     * Uploads weights and biases from the base NeuralNetwork state to the GPU.
     */
    public void syncWithCPU() {
        float[][][] w = getWeights();
        float[][] b = getBiases();
        
        float[] flatW = new float[totalWeights];
        float[] flatB = new float[totalBiases];
        
        int wPtr = 0;
        int bPtr = 0;
        
        for (int l = 0; l < w.length; l++) {
            for (int r = 0; r < w[l].length; r++) {
                for (int c = 0; c < w[l][r].length; c++) {
                    flatW[wPtr++] = w[l][r][c];
                }
            }
            for (int bi = 0; bi < b[l].length; bi++) {
                flatB[bPtr++] = b[l][bi];
            }
        }
        
        setWeightsBatch(flatW);
        setBiasesBatch(flatB);
    }

    /**
     * Overrides single feedforward to use CPU by default for low latency,
     * but ensures GPU could be used if sync'd.
     */
    @Override
    public float[] feedForward(float[] input) {
        // Use base CPU implementation for single feed-forward to avoid GPU overhead
        return super.feedForward(input);
    }

    /**
     * Sets weights for a batch of instances.
     * Use this for parallel processing of many DIFFERENT networks.
     */
    public void setWeightsBatch(float[] batchWeights) {
        ensureBuffer(bindingWeights, batchWeights);
    }

    /**
     * Sets biases for a batch of instances.
     */
    public void setBiasesBatch(float[] batchBiases) {
        ensureBuffer(bindingBiases, batchBiases);
    }

    /**
     * Performs parallel feed-forward for multiple instances.
     */
    public float[] feedForwardBatch(float[] batchInputs, int numInstances) {
        int[] metaData = new int[26];
        metaData[0] = numInstances;
        metaData[1] = numLayers;
        for (int i = 0; i < numLayers; i++) {
            metaData[2 + i] = layerSizes[i];
            metaData[10 + i] = weightLayerOffsets[i];
            metaData[18 + i] = biasLayerOffsets[i];
        }

        ensureBuffer(bindingInput, batchInputs);
        ensureBuffer(bindingMeta, metaData);
        
        float[] dummyOutput = new float[numInstances * outputSize];
        ensureBuffer(bindingOutput, dummyOutput);

        int groupsX = (numInstances + 63) / 64;
        computeBackend.dispatch(groupsX, 1, 1);

        return computeBackend.readBuffer(bindingOutput);
    }

    private void ensureBuffer(int binding, float[] data) {
        if (!computeBackend.hasBuffer(binding) || 
            computeBackend.getBufferSize(binding) != data.length) {
            computeBackend.bindBuffer(binding, data);
        } else {
            computeBackend.updateBuffer(binding, data);
        }
    }

    private void ensureBuffer(int binding, int[] data) {
        computeBackend.bindBuffer(binding, data);
    }

    public void release() {
        computeBackend.release();
    }

    public int getWeightBatchSize() {
        return totalWeights;
    }

    public int getBiasBatchSize() {
        return totalBiases;
    }
}
