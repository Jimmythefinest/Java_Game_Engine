package com.njst.gaming.ai;

import com.njst.gaming.RootLogger;
import java.io.*;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;

public class NeuralNetwork {
    // Array of layer sizes. For example: {6, 10, 3} means input=6, one hidden layer
    // with 10 neurons, output=3.
    private int[] layerSizes;
    // weights[l] is the weight matrix connecting layer l to layer l+1.
    // Dimensions: layerSizes[l+1] x layerSizes[l]
    private float[][][] weights;
    // biases[l] is the bias vector for layer l+1.
    // Dimensions: layerSizes[l+1]
    private float[][] biases;

    private float learningRate;
    // If true, the output layer uses a linear (identity) activation.
    private boolean useLinearOutput;
    public RootLogger log;

    // Constructor:
    // layers: an array where the first element is input size and last is output
    // size.
    // learningRate: step size for training.
    // useLinearOutput: if true, the output layer activation is linear.
    public NeuralNetwork(int[] layers, float learningRate, boolean useLinearOutput) {
        this.layerSizes = Arrays.copyOf(layers, layers.length);
        this.learningRate = learningRate;
        this.useLinearOutput = useLinearOutput;

        // Initialize weight and bias arrays for each connection between layers.
        weights = new float[layerSizes.length - 1][][];
        biases = new float[layerSizes.length - 1][];

        for (int l = 0; l < layerSizes.length - 1; l++) {
            int in = layerSizes[l];
            int out = layerSizes[l + 1];
            weights[l] = new float[out][in];
            biases[l] = new float[out];
            // Random initialization between -1 and 1.
            for (int i = 0; i < out; i++) {
                biases[l][i] = (float) Math.random() * 2 - 1;
                for (int j = 0; j < in; j++) {
                    weights[l][i][j] = (float) Math.random() * 2 - 1;
                }
            }
        }
    }

    // Feed-forward: computes and returns the activations for each layer.
    // The returned array has one element per layer (including input and output).
    // activations[0] is the input, and activations[last] is the output.
    private float[][] feedForwardInternal(float[] input) {
        int L = layerSizes.length;
        float[][] activations = new float[L][];
        // We also store the weighted sums (z values) if needed for training.
        float[][] zValues = new float[L - 1][];

        // Set input layer activation.
        activations[0] = Arrays.copyOf(input, input.length);

        // Compute activations for subsequent layers.
        for (int l = 1; l < L; l++) {
            int size = layerSizes[l];
            activations[l] = new float[size];
            zValues[l - 1] = new float[size];
            for (int i = 0; i < size; i++) {
                // Weighted sum: bias + sum(weights * previous activation)
                float sum = biases[l - 1][i];
                for (int j = 0; j < layerSizes[l - 1]; j++) {
                    sum += weights[l - 1][i][j] * activations[l - 1][j];
                }
                zValues[l - 1][i] = sum;
                // For the output layer, optionally use linear activation.
                if (l == L - 1 && useLinearOutput) {
                    activations[l][i] = sum;
                } else {
                    activations[l][i] = sigmoid(sum);
                }
            }
        }
        return activations;
    }

    // Public feedForward: returns the network's output given an input.
    public float[] feedForward(float[] input) {
        float[][] activations = feedForwardInternal(input);
        return activations[activations.length - 1];
    }

    // Sigmoid activation function and its derivative.
    private float sigmoid(float x) {
        return 1.0f / (1.0f + (float) Math.exp(-x));
    }

    private float dsigmoid(float y) {
        // Assumes y is already sigmoid(x).
        return y * (1 - y);
    }

    // Supervised training using backpropagation for one training sample.
    // input: input vector.
    // target: target output vector.
    public void train(float[] input, float[] target) {
        int L = layerSizes.length;
        // Forward pass: store activations and weighted sums (z values) for each layer.
        float[][] activations = new float[L][];
        float[][] zValues = new float[L - 1][];
        activations[0] = Arrays.copyOf(input, input.length);

        for (int l = 1; l < L; l++) {
            int size = layerSizes[l];
            activations[l] = new float[size];
            zValues[l - 1] = new float[size];
            for (int i = 0; i < size; i++) {
                float sum = biases[l - 1][i];
                for (int j = 0; j < layerSizes[l - 1]; j++) {
                    sum += weights[l - 1][i][j] * activations[l - 1][j];
                }
                zValues[l - 1][i] = sum;
                if (l == L - 1 && useLinearOutput) {
                    activations[l][i] = sum;
                } else {
                    activations[l][i] = sigmoid(sum);
                }
            }
        }

        // Backpropagation: compute errors for each layer.
        float[][] errors = new float[L][];
        int outputLayer = L - 1;
        errors[outputLayer] = new float[layerSizes[outputLayer]];
        for (int i = 0; i < layerSizes[outputLayer]; i++) {
            float output = activations[outputLayer][i];
            float error = target[i] - output;
            float derivative = (useLinearOutput ? 1.0f : dsigmoid(output));
            errors[outputLayer][i] = error * derivative;
        }

        // Propagate error backwards.
        for (int l = L - 2; l >= 0; l--) {
            errors[l] = new float[layerSizes[l]];
            for (int i = 0; i < layerSizes[l]; i++) {
                float errorSum = 0;
                for (int k = 0; k < layerSizes[l + 1]; k++) {
                    errorSum += errors[l + 1][k] * weights[l][k][i];
                }
                float activation = activations[l][i];
                float derivative = dsigmoid(activation);
                errors[l][i] = errorSum * derivative;
            }
        }

        // Update weights and biases.
        for (int l = 0; l < weights.length; l++) {
            for (int i = 0; i < weights[l].length; i++) {
                for (int j = 0; j < weights[l][i].length; j++) {
                    weights[l][i][j] += learningRate * errors[l + 1][i] * activations[l][j];
                }
                biases[l][i] += learningRate * errors[l + 1][i];
            }
        }
    }

    // --- Reinforcement training ---
    // Given an input, the network produces an output (action).
    // The computeReward function calculates a reward based on how close the
    // network's output is to the true midpoint.
    // We then treat the reward as the desired target for every output neuron and
    // update the network parameters.
    public void trainReinforcement(float[] input) {
        // Produce an action.
        float[] output = feedForward(input);
        // Compute reward.
        float reward = computeReward(input, output);
        // Create a target vector where each output neuron’s target is the computed
        // reward.
        float[] target = new float[output.length];
        Arrays.fill(target, reward);
        // Train the network using the reward as the target.
        train(input, target);
    }

    // computeReward for the midpoint problem.
    // Input: a 6D vector representing two 3D vectors (first 3 elements are vector
    // A, last 3 are vector B).
    // Output: a 3D predicted midpoint.
    // This function calculates the true midpoint from the input, then computes the
    // average absolute error between
    // the true midpoint and the predicted output, and finally returns a reward
    // value (1.0 is perfect, decreasing as error increases).
    public float computeReward(float[] input, float[] output) {
        float[] expected = new float[3];
        for (int i = 0; i < 3; i++) {
            expected[i] = (input[i] + input[i + 3]) / 2.0f;
        }
        float error = 0;
        for (int i = 0; i < 3; i++) {
            error += Math.abs(expected[i] - output[i]);
        }
        error /= 3.0;
        // Use an exponential decay reward function:
        return (float) Math.exp(-error);
    }

    // --- Normalization helper methods ---
    // Normalize an array of floats to the range [0, 1] given the min and max
    // values.
    public static float[] normalize(float[] data, float dataMin, float dataMax) {
        float[] normalized = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            normalized[i] = (data[i] - dataMin) / (dataMax - dataMin);
        }
        return normalized;
    }

    // De-normalize an array from [0, 1] back to the original range.
    public static float[] denormalize(float[] data, float dataMin, float dataMax) {
        float[] denormalized = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            denormalized[i] = data[i] * (dataMax - dataMin) + dataMin;
        }
        return denormalized;
    }

    // Returns a deep copy of this neural network
    public NeuralNetwork copy() {
        NeuralNetwork clone = new NeuralNetwork(this.layerSizes, this.learningRate, this.useLinearOutput);
        for (int l = 0; l < weights.length; l++) {
            for (int i = 0; i < weights[l].length; i++) {
                for (int j = 0; j < weights[l][i].length; j++) {
                    clone.weights[l][i][j] = this.weights[l][i][j];
                }
                clone.biases[l][i] = this.biases[l][i];
            }
        }
        return clone;
    }

    // Applies mutation to weights and biases
    // mutationRate: probability of mutating each parameter
    // mutationStrength: how much to change the parameter by
    public void mutate(float mutationRate, float mutationStrength) {
        for (int l = 0; l < weights.length; l++) {
            for (int i = 0; i < weights[l].length; i++) {
                for (int j = 0; j < weights[l][i].length; j++) {
                    if (Math.random() < mutationRate) {
                        weights[l][i][j] += ((float) Math.random() * 2 - 1) * mutationStrength;
                    }
                }
                if (Math.random() < mutationRate) {
                    biases[l][i] += ((float) Math.random() * 2 - 1) * mutationStrength;
                }
            }
        }
    }

    public int[] getLayerSizes() {
        return Arrays.copyOf(layerSizes, layerSizes.length);
    }

    // Getter and Setter for learningRate
    public float getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(float learningRate) {
        this.learningRate = learningRate;
    }

    // Getter and Setter for useLinearOutput
    public boolean isUseLinearOutput() {
        return useLinearOutput;
    }

    public void setUseLinearOutput(boolean useLinearOutput) {
        this.useLinearOutput = useLinearOutput;
    }

    // Getter for weights (returns a deep copy)
    public float[][][] getWeights() {
        float[][][] copy = new float[weights.length][][];
        for (int l = 0; l < weights.length; l++) {
            copy[l] = new float[weights[l].length][];
            for (int i = 0; i < weights[l].length; i++) {
                copy[l][i] = Arrays.copyOf(weights[l][i], weights[l][i].length);
            }
        }
        return copy;
    }

    // Setter for weights (deep copy)
    public void setWeights(float[][][] newWeights) {
        for (int l = 0; l < weights.length; l++) {
            for (int i = 0; i < weights[l].length; i++) {
                System.arraycopy(newWeights[l][i], 0, weights[l][i], 0, weights[l][i].length);
            }
        }
    }

    // Getter for biases (returns a deep copy)
    public float[][] getBiases() {
        float[][] copy = new float[biases.length][];
        for (int l = 0; l < biases.length; l++) {
            copy[l] = Arrays.copyOf(biases[l], biases[l].length);
        }
        return copy;
    }

    // Setter for biases (deep copy)
    public void setBiases(float[][] newBiases) {
        for (int l = 0; l < biases.length; l++) {
            System.arraycopy(newBiases[l], 0, biases[l], 0, biases[l].length);
        }
    }

    public byte[] toByteArray() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos)) {

            // Save meta info
            dos.writeInt(layerSizes.length);
            for (int size : layerSizes)
                dos.writeInt(size);
            dos.writeFloat(learningRate);
            dos.writeBoolean(useLinearOutput);

            // Save weights
            for (float[][] matrix : weights) {
                for (float[] row : matrix) {
                    for (float w : row)
                        dos.writeFloat(w);
                }
            }

            // Save biases
            for (float[] layer : biases) {
                for (float b : layer)
                    dos.writeFloat(b);
            }

            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error serializing network", e);
        }
    }

    public void saveToFile(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error saving to file", e);
        }
    }

    public static NeuralNetwork fromByteArray(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {

            int numLayers = dis.readInt();
            int[] sizes = new int[numLayers];
            for (int i = 0; i < numLayers; i++)
                sizes[i] = dis.readInt();

            float lr = dis.readFloat();
            boolean linearOut = dis.readBoolean();

            NeuralNetwork net = new NeuralNetwork(sizes, lr, linearOut);

            // Load weights
            for (int l = 0; l < net.weights.length; l++) {
                for (int i = 0; i < net.weights[l].length; i++) {
                    for (int j = 0; j < net.weights[l][i].length; j++) {
                        net.weights[l][i][j] = dis.readFloat();
                    }
                }
            }

            // Load biases
            for (int l = 0; l < net.biases.length; l++) {
                for (int i = 0; i < net.biases[l].length; i++) {
                    net.biases[l][i] = dis.readFloat();
                }
            }

            return net;

        } catch (IOException e) {
            throw new RuntimeException("Error deserializing network", e);
        }
    }

    public static NeuralNetwork loadFromFile(File file) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            return fromByteArray(data);
        } catch (IOException e) {
            throw new RuntimeException("Error reading network from file", e);
        }
    }

    public BufferedImage renderToBitmap(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set background to white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        int[] sizes = layerSizes;
        int numLayers = sizes.length;

        float hMargin = width / (float) (numLayers + 1);
        // float neuronRadius = Math.min(width, height) / 100f;
        float vSpacing = height / 20f;

        float[][] neuronY = new float[numLayers][];
        float[] layerX = new float[numLayers];

        for (int l = 0; l < numLayers; l++) {
            int neurons = sizes[l];
            neuronY[l] = new float[neurons];
            float top = (height - (neurons - 1) * vSpacing) / 2f;
            layerX[l] = (l + 1) * hMargin;

            for (int n = 0; n < neurons; n++) {
                neuronY[l][n] = top + n * vSpacing;
            }
        }

        // Draw connections
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2f));

        try {
            for (int l = 0; l < weights.length; l++) {
                log.logToRootDirectory("\nlayer:" + l);
                log.logToRootDirectory("\nnum neurons:" + weights[l].length);
                log.logToRootDirectory("\nnum weights:" + weights[l][0].length);

                for (int i = 0; i < weights[l].length; i++) {
                    log.logToRootDirectory("\nneuron:" + i);
                    for (int j = 0; j < weights[l][i].length; j++) {
                        float x1 = layerX[l + 1];
                        float y1 = neuronY[l + 1][i];
                        float x2 = layerX[l];
                        float y2 = neuronY[l][j];

                        float weight = weights[l][i][j];
                        log.logToRootDirectory("\nweight:" + j + "value:" + weight);

                        // Create color based on weight value
                        int red = weight > 0 ? 0 : (int) (-255 * weight);
                        int green = weight > 0 ? (int) (255 * weight) : 0;
                        int blue = 0;
                        int alpha = (int) (Math.abs(weight) * 255);

                        g2d.setColor(new Color(red, green, blue, alpha));
                        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                    }
                }
            }
        } catch (Exception e) {
            logException(e);
        }

        // Draw neurons
        g2d.setColor(Color.BLACK);
        for (int l = 0; l < numLayers; l++) {
            for (int n = 0; n < sizes[l]; n++) {
                g2d.fillOval((int) (layerX[l] - 5), (int) (neuronY[l][n] - 5), 10, 10);
            }
        }

        g2d.dispose();
        return image;
    }

    private void logException(Exception e) {

        log.logToRootDirectory(e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            log.logToRootDirectory(element.getClassName() + element.getMethodName() + element.getLineNumber());
        }
    }

}
