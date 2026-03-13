package com.njst.gaming.ai;

import java.util.Random;
import java.util.Arrays;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TicTacToeQLearning {
    private int inputSize = 9;
    private int outputSize = 9;
    private int[] hiddenSizes = {10, 10};
    private double[][][] weights;
    private double learningRate = 0.01;
    private double gamma = 0.9; // Discount factor
    private Random rand;
    private JButton[] buttons = new JButton[9];
    private JFrame frame;
    private int[] board = new int[9]; // 0 = empty, 1 = player, -1 = AI

    public TicTacToeQLearning() {
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
        setupUI();
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

    private boolean checkWin(int player) {
        int[][] winPatterns = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
        };
        for (int[] pattern : winPatterns) {
            if (board[pattern[0]] == player && board[pattern[1]] == player && board[pattern[2]] == player) {
                return true;
            }
        }
        return false;
    }

    private void setupUI() {
        frame = new JFrame("Tic-Tac-Toe Q-Learning");
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 3));

        for (int i = 0; i < 9; i++) {
            buttons[i] = new JButton("");
            buttons[i].setFont(new Font("Arial", Font.PLAIN, 40));
            buttons[i].addActionListener(new ButtonClickListener(i));
            frame.add(buttons[i]);
        }
        frame.setVisible(true);
    }

    private class ButtonClickListener implements ActionListener {
        private int index;

        public ButtonClickListener(int index) {
            this.index = index;
        }

        public void actionPerformed(ActionEvent e) {
            if (board[index] == 0) {
                board[index] = 1;
                buttons[index].setText("X");
                if (checkWin(1)) {
                    JOptionPane.showMessageDialog(frame, "Player Wins!");
                    return;
                }
               // int aiMove = chooseAction(Arrays.stream(board).asDoubleStream().toArray());
              //  board[aiMove] = -1;
              //  buttons[aiMove].setText("O");
                if (checkWin(-1)) {
                    JOptionPane.showMessageDialog(frame, "AI Wins!");
                }
            }
        }
    }

    public static void main(String[] args) {
        new TicTacToeQLearning();
    }
}