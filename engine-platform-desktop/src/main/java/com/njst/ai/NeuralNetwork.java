package com.njst.ai;

import java.util.Arrays;

public class NeuralNetwork implements java.io.Serializable {
    // Array of layer sizes. For example: {6, 10, 3} means input=6, one hidden layer with 10 neurons, output=3.
    private int[] layerSizes;
    // weights[l] is the weight matrix connecting layer l to layer l+1.
    // Dimensions: layerSizes[l+1] x layerSizes[l]
    public double[][][] weights;
    // biases[l] is the bias vector for layer l+1.
   // Dimensions: layerSizes[l+1]
    public double[][] biases;
    
    private double learningRate;
    // If true, the output layer uses a linear (identity) activation.
    private boolean useLinearOutput;

    // Constructor: 
// layers: an array where the first element is input size and last is output size.
    // learningRate: step size for training.
    // useLinearOutput: if true, the output layer activation is linear.
    public NeuralNetwork(int[] layers, double learningRate, boolean useLinearOutput) {
        this.layerSizes = Arrays.copyOf(layers, layers.length);
        this.learningRate = learningRate;
        this.useLinearOutput = useLinearOutput;
        
        // Initialize weight and bias arrays for each connection between layers.
        weights = new double[layerSizes.length - 1][][];
        biases = new double[layerSizes.length - 1][];
        
        for (int l = 0; l < layerSizes.length - 1; l++) {
            int in = layerSizes[l];
            int out = layerSizes[l + 1];
            weights[l] = new double[out][in];
            biases[l] = new double[out];
            // Random initialization between -1 and 1.
            for (int i = 0; i < out; i++) {
                biases[l][i] = Math.random() * 2 - 1;
                for (int j = 0; j < in; j++) {
                    weights[l][i][j] =0;// Math.random() * 2 - 1;
                }
            }
        }
    }
    public void setLeanRate(double rate){
        learningRate=rate;
    }
    
    // Feed-forward: computes and returns the activations for each layer.
    // The returned array has one element per layer (including input and output).
    // activations[0] is the input, and activations[last] is the output.
    private double[][] feedForwardInternal(double[] input) {
        int L = layerSizes.length;
        double[][] activations = new double[L][];
        // We also store the weighted sums (z values) if needed for training.
        double[][] zValues = new double[L - 1][];
        
        // Set input layer activation.
        activations[0] = Arrays.copyOf(input, input.length);
        
        // Compute activations for subsequent layers.
        for (int l = 1; l < L; l++) {
            int size = layerSizes[l];
            activations[l] = new double[size];
            zValues[l - 1] = new double[size];
            for (int i = 0; i < size; i++) {
                // Weighted sum: bias + sum(weights * previous activation)
                double sum = biases[l - 1][i];
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
    public double[] feedForward(double[] input) {
        double[][] activations = feedForwardInternal(input);
        return activations[activations.length - 1];
    }
    
    // Sigmoid activation function and its derivative.
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    private double dsigmoid(double y) {
        // Assumes y is already sigmoid(x).
        return y * (1 - y);
    }
    
    // Supervised training using backpropagation for one training sample.
    // input: input vector.
    // target: target output vector.
    public void train(double[] input, double[] target) {
        int L = layerSizes.length;
        // Forward pass: store activations and weighted sums (z values) for each layer.
        double[][] activations = new double[L][];
        double[][] zValues = new double[L - 1][];
        activations[0] = Arrays.copyOf(input, input.length);
        
        for (int l = 1; l < L; l++) {
            int size = layerSizes[l];
            activations[l] = new double[size];
            zValues[l - 1] = new double[size];
            for (int i = 0; i < size; i++) {
                double sum = biases[l - 1][i];
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
        double[][] errors = new double[L][];
        int outputLayer = L - 1;
        errors[outputLayer] = new double[layerSizes[outputLayer]];
        for (int i = 0; i < layerSizes[outputLayer]; i++) {
            double output = activations[outputLayer][i];
            double error = target[i] - output;
            double derivative = (useLinearOutput ? 1.0 : dsigmoid(output));
            errors[outputLayer][i] = error * derivative;
        }
        
        // Propagate error backwards.
        for (int l = L - 2; l >= 0; l--) {
            errors[l] = new double[layerSizes[l]];
            for (int i = 0; i < layerSizes[l]; i++) {
                double errorSum = 0;
                for (int k = 0; k < layerSizes[l + 1]; k++) {
                    errorSum += errors[l + 1][k] * weights[l][k][i];
                }
                double activation = activations[l][i];
                double derivative = dsigmoid(activation);
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
    // The computeReward function calculates a reward based on how close the network's output is to the true midpoint.
    // We then treat the reward as the desired target for every output neuron and update the network parameters.
    public void trainReinforcement(double[] input) {
        // Produce an action.
        double[] output = feedForward(input);
        // Compute reward.
        double reward = computeReward(input, output);
        // Create a target vector where each output neuron’s target is the computed reward.
        double[] target = new double[output.length];
        Arrays.fill(target, reward);
        // Train the network using the reward as the target.
        train(input, target);
    }
    
    // computeReward for the midpoint problem.
    // Input: a 6D vector representing two 3D vectors (first 3 elements are vector A, last 3 are vector B).
    // Output: a 3D predicted midpoint.
    // This function calculates the true midpoint from the input, then computes the average absolute error between
    // the true midpoint and the predicted output, and finally returns a reward value (1.0 is perfect, decreasing as error increases).
    public double computeReward(double[] input, double[] output) {
    double[] expected = new double[3];
    for (int i = 0; i < 3; i++) {
        expected[i] = (input[i] + input[i + 3]) / 2.0;
    }
    double error = 0;
    for (int i = 0; i < 3; i++) {
        error += Math.abs(expected[i] - output[i]);
    }
    error /= 3.0;
    // Use an exponential decay reward function:
    return Math.exp(-error);
}

    
    // --- Normalization helper methods ---
    // Normalize an array of doubles to the range [0, 1] given the min and max values.
    public static double[] normalize(double[] data, double dataMin, double dataMax) {
        double[] normalized = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            normalized[i] = (data[i] - dataMin) / (dataMax - dataMin);
        }
        return normalized;
    }
    
    // De-normalize an array from [0, 1] back to the original range.
    public static double[] denormalize(double[] data, double dataMin, double dataMax) {
        double[] denormalized = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            denormalized[i] = data[i] * (dataMax - dataMin) + dataMin;
        }
        return denormalized;
    }
    
    public static void main(String[] args) {
        // In this test the network is given two 3D vectors (concatenated into a 6D vector)
        // and is trained to output their midpoint (a 3D vector) using reinforcement training.
        int trainingSamples = 1000;
        double[][] inputs = new double[trainingSamples][6];  // Each input is two 3D vectors.
        double[][] targets = new double[trainingSamples][3]; // For reference: the true midpoint.
        
        // Generate random training samples.
        for (int i = 0; i < trainingSamples; i++) {
            double[] vectorA = new double[3];
            double[] vectorB = new double[3];
            double[] input = new double[6];
            double[] midpoint = new double[3];
            for (int j = 0; j < 3; j++) {
                vectorA[j] = Math.random() ;  // Random values in [-1, 1]
                vectorB[j] = Math.random() ;
                midpoint[j] = (vectorA[j] );
            }
            System.arraycopy(vectorA, 0, input, 0, 3);
            System.arraycopy(vectorB, 0, input, 3, 3);
            inputs[i] = input;
            targets[i] = vectorA; // For later reference.
        }
        
        // Create the network.
        // Input size = 6, one hidden layer with 10 neurons, output size = 3.
        int[] layers = {1, 10,10, 3};
        NeuralNetwork nn = new NeuralNetwork(layers, 0.01, true);
        
        // Train the network using reinforcement training.
        boolean b=false;
        int runs=0;
        while(b){
        int reinforcementEpochs = 10000;
        for (int i = 0; i < reinforcementEpochs;i++) {
            nn.train(inputs[i],targets[i]);
        }
        runs++;
        b=runs>100000;
        }
        // Test the network on a few random examples.
        System.out.println("Testing midpoint prediction using reinforcement training for two 3D vectors:");
        for (int i = 0; i < 10; i++) {
            int sample = (int) (Math.random() * trainingSamples);
            double[] output = nn.feedForward(inputs[sample]);
            System.out.printf("Input Vector A: (%.3f, %.3f, %.3f)\n", inputs[sample][0], inputs[sample][1], inputs[sample][2]);
            System.out.printf("Input Vector B: (%.3f, %.3f, %.3f)\n", inputs[sample][3], inputs[sample][4], inputs[sample][5]);
            System.out.printf("Expected Midpoint: (%.3f, %.3f, %.3f)\n", 
                              targets[sample][0], targets[sample][1], targets[sample][2]);
            System.out.printf("NN Predicted   : (%.3f, %.3f, %.3f)\n\n", 
                              output[0], output[1], output[2]);
      }
    }
    // Save the network to a file
public void saveToFile(String filename) throws java.io.IOException {
    try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.FileOutputStream(filename))) {
        out.writeObject(this);
    }
}

// Load a network from a file
public static NeuralNetwork loadFromFile(String filename) throws java.io.IOException, ClassNotFoundException {
    try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(filename))) {
        return (NeuralNetwork) in.readObject();
    }
}

}
