package com.njst.gaming.Utils;

import java.util.Random;

public class PerlinNoise {
    private int[] permutation;
    private int[] p; // Permutation table
    private static final int PERMUTATION_SIZE = 256;

    public PerlinNoise() {
        permutation = new int[PERMUTATION_SIZE];
        p = new int[PERMUTATION_SIZE * 2];

        // Initialize the permutation array
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            permutation[i] = i;
        }

        // Shuffle the permutation array
        Random random = new Random();
        for (int i = PERMUTATION_SIZE - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        // Duplicate the permutation array
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            p[i] = permutation[i];
            p[i + PERMUTATION_SIZE] = permutation[i]; // Duplicate to avoid overflow
        }
    }

    // Fade function as defined by Ken Perlin. This eases coordinate values
    // so that they will ease towards integral values. This means that
    // the transition will ease towards the next integer.
    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // Linear interpolation
    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // Gradient function
    private static double grad(int hash, double x, double y) {
        int h = hash & 3; // Convert low 2 bits of hash code
        double u = h < 2 ? x : y; // Gradient value 1
        double v = h < 2 ? y : x; // Gradient value 2
        return (h & 1) == 0 ? u : -u + ( v ); // Randomly invert components
    }

    // Perlin noise function
    public double noise(double x, double y) {
        // Determine grid cell coordinates
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        // Relative x and y in the grid cell
        x -= Math.floor(x);
        y -= Math.floor(y);

        // Compute fade curves for x and y
        double u = fade(x);
        double v = fade(y);

        // Hash coordinates of the 2 square corners
        int aa = p[p[X] + Y];
        int ab = p[p[X] + Y + 1];
        int ba = p[p[X + 1] + Y];
        int bb = p[p[X + 1] + Y + 1];

        // And add blended results from the corners of the square
        return lerp(
                lerp(grad(aa, x, y), grad(ba, x - 1, y), u),
                lerp(grad(ab, x, y - 1), grad(bb, x - 1, y - 1), u),
                v
        );
    }
}