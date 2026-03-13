package com.njst.gaming.ai;
import java.util.Random;

public class RLNeuralNetwork {
    private double[][] weights1;
    private double[][] weights2;
    private int inputSize;
    private int outputSize;
    private int hiddenLayerSize;
    private Random random;

    public RLNeuralNetwork(int inputSize, int outputSize, int hiddenLayerSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.hiddenLayerSize = hiddenLayerSize;
        this.random = new Random();
        
        weights1 = new double[inputSize][hiddenLayerSize];
        weights2 = new double[hiddenLayerSize][outputSize];
        initializeWeights(weights1);
        initializeWeights(weights2);
    }

    private void initializeWeights(double[][] weights) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] = random.nextGaussian() * 0.1; // Small random values
            }
        }
    }

    private double[] relu(double[] input) {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = Math.max(0, input[i]);
        }
        return output;
    }

    static double[] normalize(double[] input, double min, double max) {
        double[] normalized = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            normalized[i] = (input[i] - min) / (max - min);
        }
        return normalized;
    }

    private double[] denormalize(double[] input, double min, double max) {
        double[] denormalized = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            denormalized[i] = input[i] * (max - min) + min;
        }
        return denormalized;
    }

    private double[] matrixVectorMultiply(double[][] matrix, double[] vector) {
        double[] result = new double[matrix[0].length];
        for (int i = 0; i < matrix[0].length; i++) {
            result[i] = 0; // Explicitly initialize
            for (int j = 0; j < matrix.length; j++) {
                result[i] += matrix[j][i] * vector[j];
            }
        }
        return result;
    }

    public double[] predict(double[] state) {
        double[] normState = normalize(state, 0, 10);
        double[] hiddenLayer = relu(matrixVectorMultiply(weights1, normState));
        double[] output = matrixVectorMultiply(weights2, hiddenLayer);
        return denormalize(output, 0, 50);
    }

    public void train(double[][] states, double[][] targets, double learningRate) {
        for (int i = 0; i < states.length; i++) {
            double[] normState = normalize(states[i], 0, 100);
            double[] normTarget = normalize(targets[i], 0, 50);
            
            double[] hiddenLayer = relu(matrixVectorMultiply(weights1, normState));
            double[] outputs = matrixVectorMultiply(weights2, hiddenLayer);
            
            for (int j = 0; j < outputSize; j++) {
                double error = normTarget[j] - outputs[j];
                for (int k = 0; k < hiddenLayerSize; k++) {
                    weights2[k][j] += learningRate * error * hiddenLayer[k];
                }
            }
            
            for (int j = 0; j < hiddenLayerSize; j++) {
                double hiddenGradient = hiddenLayer[j] > 0 ? 1 : 0; // Derivative of ReLU
                for (int k = 0; k < inputSize; k++) {
                    weights1[k][j] += learningRate * hiddenGradient * normState[k];
                }
            }
        }
    }

    public static void main(String[] args) {
        RLNeuralNetwork nn = new RLNeuralNetwork(2, 1, 5);
        
        double[][] states = {{0,0}, {0,1}, {1,0}, {1,1}};
        double[][] targets = {{0}, {1}, {0}, {1}}; // Desired output: input * 5
        
        for (int i = 0; i < 20000; i++) { // Increased training iterations
            nn.train(states, targets, 0.01);
        }
        
        double[] testInput = {0,1};
        double[] prediction = nn.predict(testInput);
        
        System.out.println("Predicted output for 6: " + prediction[0]);
    }
}