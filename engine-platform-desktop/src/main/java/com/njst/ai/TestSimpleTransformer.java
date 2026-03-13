package com.njst.ai;

import java.util.Random;

public class TestSimpleTransformer {
    public static void main(String[] args) {
        int vocabSize = 10;
        int modelDim = 8;
        SimpleTransformer transformer = new SimpleTransformer(vocabSize, modelDim);
        Random rand = new Random();

        int epochs = 500;
        float lr = 0.01f;

        for (int e = 0; e < epochs; e++) {
            int[] tokens = new int[5];
            for (int i = 0; i < tokens.length; i++) tokens[i] = rand.nextInt(vocabSize);

            float[] target = new float[modelDim];
            int targetIdx = rand.nextInt(modelDim);
            target[targetIdx] = 1f;

            float loss = transformer.trainStep(tokens, target, lr);
            if (e % 50 == 0) System.out.printf("Epoch %d loss: %.6f\n", e, loss);
        }
    }
}
