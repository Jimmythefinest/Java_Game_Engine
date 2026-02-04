package com.njst.ai;

public class MathUtils {
    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public static double sigmoidDerivative(double x) {
        return x * (1 - x);
    }

    public static double[] softmax(double[] input) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : input) if (v > max) max = v;

        double sum = 0.0;
        double[] result = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = Math.exp(input[i] - max);
            sum += result[i];
        }
        for (int i = 0; i < input.length; i++) {
            result[i] /= sum;
        }
        return result;
    }
}
