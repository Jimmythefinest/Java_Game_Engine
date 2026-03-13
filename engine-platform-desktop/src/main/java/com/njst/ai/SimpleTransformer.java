package com.njst.ai;

import java.util.*;

/**
 * A simple, fully self-contained Transformer implementation in Java.
 * This version includes only a single attention head and feedforward layer for
 * clarity.
 */
public class SimpleTransformer {

    private int modelDim;
    private float[][] tokenEmbedding;
    private float[][] Wq, Wk, Wv, Wo;
    private float[][] W1, W2; // Feedforward layer

    private Random rand = new Random();
    public SimpleTransformer(int vocabSize, int modelDim) {
        this.modelDim = modelDim;

        // Initialize embedding and weight matrices
        tokenEmbedding = initWeight(vocabSize, modelDim);
        Wq = initWeight(modelDim, modelDim);
        Wk = initWeight(modelDim, modelDim);
        Wv = initWeight(modelDim, modelDim);
        Wo = initWeight(modelDim, modelDim);

        W1 = initWeight(modelDim, modelDim * 2); // FFN expand
        W2 = initWeight(modelDim * 2, modelDim); // FFN reduce
    }

    // Converts token indices into their embedding vectors
    public float[][] embed(int[] tokens) {
        float[][] x = new float[tokens.length][modelDim];
        for (int i = 0; i < tokens.length; i++) {
            x[i] = tokenEmbedding[tokens[i]].clone();
        }
        return x;
    }

    // Forward pass through Transformer
    public float[][] forward(int[] tokens) {
        float[][] x = embed(tokens);
        float[][] attended = selfAttention(x);
        float[][] ffnOut = feedForward(attended);
        return ffnOut;
    }

    private float[][] selfAttention(float[][] x) {
        int T = x.length;
        float[][] q = matMul(x, Wq);
        float[][] k = matMul(x, Wk);
        float[][] v = matMul(x, Wv);

        float[][] scores = new float[T][T];
        for (int i = 0; i < T; i++) {
            for (int j = 0; j < T; j++) {
                scores[i][j] = dot(q[i], k[j]) / (float) Math.sqrt(modelDim);
            }
        }
        softmax2D(scores);

        float[][] context = new float[T][modelDim];
        for (int i = 0; i < T; i++) {
            for (int j = 0; j < T; j++) {
                for (int d = 0; d < modelDim; d++) {
                    context[i][d] += scores[i][j] * v[j][d];
                }
            }
        }

        return matMul(context, Wo);
    }

    private float[][] feedForward(float[][] x) {
        float[][] out1 = matMul(x, W1);
        relu(out1);
        return matMul(out1, W2);
    }

    // Utility methods below

    private float[][] initWeight(int in, int out) {
        float[][] w = new float[in][out];
        for (int i = 0; i < in; i++)
            for (int j = 0; j < out; j++)
                w[i][j] = (float) rand.nextGaussian() * 0.01f;
        return w;
    }

    private float[][] matMul(float[][] a, float[][] b) {
        int rows = a.length, cols = b[0].length, shared = b.length;
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                for (int k = 0; k < shared; k++)
                    result[i][j] += a[i][k] * b[k][j];
        return result;
    }

    private float dot(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++)
            sum += a[i] * b[i];
        return sum;
    }

    private void softmax2D(float[][] x) {
        for (int i = 0; i < x.length; i++) {
            float max = Float.NEGATIVE_INFINITY;
            for (float v : x[i])
                if (v > max)
                    max = v;
            float sum = 0;
            for (int j = 0; j < x[i].length; j++) {
                x[i][j] = (float) Math.exp(x[i][j] - max);
                sum += x[i][j];
            }
            for (int j = 0; j < x[i].length; j++)
                x[i][j] /= sum;
        }
    }

    private void relu(float[][] x) {
        for (int i = 0; i < x.length; i++)
            for (int j = 0; j < x[i].length; j++)
                x[i][j] = Math.max(0, x[i][j]);
    }

    // Use this method to get predictions
    public int predictNextToken(int[] context) {
        float[][] out = forward(context);
        float[] last = out[out.length - 1];
        int best = 0;
        for (int i = 1; i < last.length; i++) {
            if (last[i] > last[best])
                best = i;
        }
        return best;
    }

    /**
     * A simple training step with Mean Squared Error (MSE) loss on the last token
     * output.
     * Only updates the last feedforward layer weights (W2) for demonstration.
     * 
     * @param tokens       input token sequence
     * @param target       one-hot target vector of length modelDim (next token
     *                     embedding)
     * @param learningRate gradient descent learning rate
     * @return loss value
     */
   public float trainStep(int[] tokens, float[] target, float learningRate) {
    // Forward pass
    float[][] x = embed(tokens);                  // [seqLen][modelDim]
    float[][] attended = selfAttention(x);       // [seqLen][modelDim]
    float[][] ffnInput = attended;                // input to FFN

    // Feedforward layer 1 (with ReLU) and cache mask for backprop
    float[][] out1 = matMul(ffnInput, W1);       // [seqLen][hiddenDim]
    boolean[][] reluMask = new boolean[out1.length][out1[0].length];
    for (int i = 0; i < out1.length; i++) {
        for (int j = 0; j < out1[0].length; j++) {
            if (out1[i][j] > 0) {
                reluMask[i][j] = true;
            } else {
                out1[i][j] = 0;
                reluMask[i][j] = false;
            }
        }
    }

    // Feedforward layer 2 (output logits)
    float[][] prediction = matMul(out1, W2);     // [seqLen][modelDim]

    int lastIdx = prediction.length - 1;         // Index of last token in sequence

    // Compute Mean Squared Error loss on last token output only
    float loss = 0f;
    float[] gradPred = new float[prediction[0].length]; // gradient of loss wrt prediction last token
    for (int i = 0; i < prediction[0].length; i++) {
        float diff = prediction[lastIdx][i] - target[i];
        loss += diff * diff;
        gradPred[i] = 2 * diff;
    }
    loss /= prediction[0].length;

    // Backpropagation to W2 weights and output of first FFN layer (out1)
    float[][] gradW2 = new float[W2.length][W2[0].length];
    float[][] gradOut1 = new float[out1.length][out1[0].length];

    // Compute gradients of W2 using last token only
    for (int i = 0; i < W2.length; i++) {           // hiddenDim
        for (int j = 0; j < W2[0].length; j++) {    // modelDim
            gradW2[i][j] = out1[lastIdx][i] * gradPred[j];
        }
    }

    // Compute gradients for out1 (input to W2)
    for (int j = 0; j < W2[0].length; j++) {
        for (int i = 0; i < W2.length; i++) {
            gradOut1[lastIdx][i] += W2[i][j] * gradPred[j];
        }
    }

    // Backpropagate through ReLU mask on last token only
    for (int j = 0; j < gradOut1[0].length; j++) {
        if (!reluMask[lastIdx][j]) {
            gradOut1[lastIdx][j] = 0;
        }
    }

    // Backpropagation to W1 weights (only last token)
    float[][] gradW1 = new float[W1.length][W1[0].length];
    for (int i = 0; i < W1.length; i++) {          // modelDim
        for (int j = 0; j < W1[0].length; j++) {   // hiddenDim
            gradW1[i][j] = ffnInput[lastIdx][i] * gradOut1[lastIdx][j];
        }
    }

    // Gradient descent update on W2 and W1 weights
    for (int i = 0; i < W2.length; i++) {
        for (int j = 0; j < W2[0].length; j++) {
            W2[i][j] -= learningRate * gradW2[i][j];
        }
    }
    for (int i = 0; i < W1.length; i++) {
        for (int j = 0; j < W1[0].length; j++) {
            W1[i][j] -= learningRate * gradW1[i][j];
        }
    }

    // Return loss for monitoring
    return loss;
}

}
