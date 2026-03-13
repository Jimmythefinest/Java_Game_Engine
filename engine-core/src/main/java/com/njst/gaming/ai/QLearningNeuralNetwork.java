package com.njst.gaming.ai;

import java.util.Random;
import java.util.Arrays;

public class QLearningNeuralNetwork {
    private int outputSize;
    private int[] hiddenSizes;
    private double[][][] weights;
    private double learningRate;
    private double gamma; // Discount factor
    private Random rand;

    public QLearningNeuralNetwork(int inputSize, int[] hiddenSizes, int outputSize, double learningRate, double gamma) {
        this.hiddenSizes = hiddenSizes;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.gamma = gamma;
        rand = new Random();

        int layers = hiddenSizes.length + 1;
        weights = new double[layers][][];

        weights[0] = new double[inputSize][hiddenSizes[0]];
        for (int i = 1; i < hiddenSizes.length; i++) {
            weights[i] = new double[hiddenSizes[i - 1]][hiddenSizes[i]];
        }
        weights[layers - 1] = new double[hiddenSizes[hiddenSizes.length - 1]][outputSize];

        for (double[][] layer : weights) {
            initializeWeights(layer);
        }
    }

    private void initializeWeights(double[][] weights) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] = rand.nextGaussian() * 0.01;
            }
        }
    }

    private double[] relu(double[] x) {
        return Arrays.stream(x).map(v -> Math.max(0, v)).toArray();
    }

    private double[] matrixVectorMultiply(double[][] matrix, double[] vector) {
        double[] result = new double[matrix[0].length];
        for (int i = 0; i < matrix[0].length; i++) {
            for (int j = 0; j < vector.length; j++) {
                result[i] += matrix[j][i] * vector[j];
            }
        }
        return result;
    }

    public double[] predict(double[] state) {
        double[] output = state;
        for (int i = 0; i < weights.length - 1; i++) {
            output = relu(matrixVectorMultiply(weights[i], output));
        }
        return matrixVectorMultiply(weights[weights.length - 1], output);
    }

    public void update(double[] state, int action, double reward, double[] nextState) {
        double[] qValues = predict(state);
        double[] nextQValues = predict(nextState);
        double maxNextQ = Arrays.stream(nextQValues).max().orElse(0);
        
        qValues[action] = reward + gamma * maxNextQ;
        
        train(state, qValues);
    }

    private void train(double[] state, double[] targetQValues) {
        double[][] activations = new double[hiddenSizes.length + 2][];
        activations[0] = state;

        for (int i = 0; i < hiddenSizes.length; i++) {
            activations[i + 1] = relu(matrixVectorMultiply(weights[i], activations[i]));
        }
        activations[hiddenSizes.length + 1] = matrixVectorMultiply(weights[hiddenSizes.length], activations[hiddenSizes.length]);
        
        double[] outputError = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            outputError[i] = targetQValues[i] - activations[hiddenSizes.length + 1][i];
        }
        
        double[][] deltas = new double[hiddenSizes.length + 1][];
        deltas[deltas.length - 1] = outputError;

        for (int i = deltas.length - 2; i >= 0; i--) {
            double[] layerError = new double[hiddenSizes[i]];
            for (int j = 0; j < hiddenSizes[i]; j++) {
                for (int k = 0; k < deltas[i + 1].length; k++) {
                    layerError[j] += deltas[i + 1][k] * weights[i + 1][j][k];
                }
            }
            deltas[i] = layerError;
        }

        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                for (int k = 0; k < weights[i][j].length; k++) {
                    weights[i][j][k] += learningRate * deltas[i][k] * activations[i][j];
                }
            }
        }
    }

    public static void main(String[] args) {
        int[] hiddenLayers = {10, 10};
        QLearningNeuralNetwork qnn = new QLearningNeuralNetwork(4, hiddenLayers, 2, 0.01, 0.9);

        double[] state = {1, 0, 0, 1};
        int action = 1;
        double reward = 1.0;
        double[] nextState = {0, 1, 1, 0};

        System.out.println("Before update: " + Arrays.toString(qnn.predict(state)));
        qnn.update(state, action, reward, nextState);
        System.out.println("After update: " + Arrays.toString(qnn.predict(state)));
    }
}
