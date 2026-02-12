package com.njst.gaming;

import com.njst.gaming.Natives.HeadlessContext;
import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.ai.GPUNeuralNetwork;

import java.util.Random;

public class GPUNNTest {
    public static void main(String[] args) {
        System.out.println("Starting GPUNeuralNetwork Inheritance & Verification Test...");

        HeadlessContext context = new HeadlessContext();
        try {
            context.init();

            int numInstances = 100;
            int[] layers = {12, 16, 8};
            Random rnd = new Random(42);

            // 1. Create a single GPUNeuralNetwork (Testing inheritance)
            // It should expose both standard NeuralNetwork methods and GPUNN methods
            GPUNeuralNetwork mainGpuNet = new GPUNeuralNetwork(layers, 0.01f, true);
            
            // Verify polymorphism
            NeuralNetwork polymorphicNet = mainGpuNet;
            float[] sampleInput = new float[layers[0]];
            for(int i=0; i<layers[0]; i++) sampleInput[i] = rnd.nextFloat();
            
            float[] cpuOutput = polymorphicNet.feedForward(sampleInput);
            System.out.println("Polymorphic CPU output size: " + cpuOutput.length);

            // 2. Create batch data for parallel verification
            GPUNeuralNetwork[] batchNets = new GPUNeuralNetwork[numInstances];
            float[] flatWeights = new float[numInstances * mainGpuNet.getWeightBatchSize()];
            float[] flatBiases = new float[numInstances * mainGpuNet.getBiasBatchSize()];
            float[] flatInputs = new float[numInstances * layers[0]];

            for (int i = 0; i < numInstances; i++) {
                batchNets[i] = new GPUNeuralNetwork(layers, 0.01f, true);
                
                // Collect weights for GPU batch
                float[][][] w = batchNets[i].getWeights();
                float[][] b = batchNets[i].getBiases();
                
                int wOffset = i * mainGpuNet.getWeightBatchSize();
                int bOffset = i * mainGpuNet.getBiasBatchSize();
                
                // Helper to flatten local network weights into the batch array
                flattenWeights(w, b, flatWeights, flatBiases, wOffset, bOffset);
                
                for (int j = 0; j < layers[0]; j++) {
                    flatInputs[i * layers[0] + j] = rnd.nextFloat() * 2 - 1;
                }
            }

            // 3. Run GPU Batch
            long startGPU = System.nanoTime();
            mainGpuNet.setWeightsBatch(flatWeights);
            mainGpuNet.setBiasesBatch(flatBiases);
            float[] gpuOutputs = mainGpuNet.feedForwardBatch(flatInputs, numInstances);
            long endGPU = System.nanoTime();

            // 4. Run CPU Reference
            long startCPU = System.nanoTime();
            float[][] cpuReferenceOutputs = new float[numInstances][];
            for (int i = 0; i < numInstances; i++) {
                cpuReferenceOutputs[i] = batchNets[i].feedForward(getInputSlice(flatInputs, i, layers[0]));
            }
            long endCPU = System.nanoTime();

            // 5. Compare
            float maxDiff = 0;
            int outSize = layers[layers.length - 1];
            for (int i = 0; i < numInstances; i++) {
                for (int j = 0; j < outSize; j++) {
                    maxDiff = Math.max(maxDiff, Math.abs(cpuReferenceOutputs[i][j] - gpuOutputs[i * outSize + j]));
                }
            }

            System.out.println("Batch Verification Results:");
            System.out.println("Max Difference: " + maxDiff);
            System.out.println("GPU Time (Batch): " + (endGPU - startGPU) / 1e6 + " ms");
            System.out.println("CPU Time (Iter):  " + (endCPU - startCPU) / 1e6 + " ms");

            if (maxDiff < 0.0001f) {
                System.out.println("SUCCESS: GPUNeuralNetwork (extending NeuralNetwork) verified!");
            } else {
                System.out.println("FAILURE: Discrepancy detected.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.destroy();
        }
    }

    private static void flattenWeights(float[][][] w, float[][] b, float[] fW, float[] fB, int wOff, int bOff) {
        int wPtr = wOff;
        int bPtr = bOff;
        for (int l = 0; l < w.length; l++) {
            for (int r = 0; r < w[l].length; r++) {
                for (int c = 0; c < w[l][r].length; c++) fW[wPtr++] = w[l][r][c];
            }
            for (int bi = 0; bi < b[l].length; bi++) fB[bPtr++] = b[l][bi];
        }
    }

    private static float[] getInputSlice(float[] flat, int index, int size) {
        float[] slice = new float[size];
        System.arraycopy(flat, index * size, slice, 0, size);
        return slice;
    }
}
