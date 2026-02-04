package com.njst.ai;

import java.util.*;

public class NeatXOR {
    static class Node {
        int id;
        double value;
        Node(int id) { this.id = id; }
    }

    static class Connection {
        int from, to;
        double weight;
        boolean enabled = true;
        Connection(int f, int t, double w) {
            from = f; to = t; weight = w;
        }
    }

    static class Genome {
        List<Node> nodes = new ArrayList<>();
        List<Connection> conns = new ArrayList<>();
        Random rand = new Random();

        Genome(int inputs, int outputs) {
            for (int i = 0; i < inputs + outputs; i++)
                nodes.add(new Node(i));
            // full connect input to output
            for (int i = 0; i < inputs; i++)
                for (int j = inputs; j < inputs + outputs; j++)
                    conns.add(new Connection(i, j, rand.nextDouble() * 2 - 1));
        }

        double[] feed(double[] input) {
            for (int i = 0; i < input.length; i++)
                nodes.get(i).value = input[i];
            for (Node n : nodes)
                if (n.id >= input.length) n.value = 0;
            for (Connection c : conns)
                if (c.enabled)
                    nodes.get(c.to).value += nodes.get(c.from).value * c.weight;
            for (Node n : nodes)
                if (n.id >= input.length)
                    n.value = sigmoid(n.value);
            double[] out = new double[1];
            out[0] = nodes.get(nodes.size() - 1).value;
            return out;
        }

        Genome copy() {
            Genome g = new Genome(0, 0);
            for (Node n : nodes)
                g.nodes.add(new Node(n.id));
            for (Connection c : conns)
                g.conns.add(new Connection(c.from, c.to, c.weight));
            return g;
        }

        void mutate() {
            for (Connection c : conns)
                c.weight += rand.nextGaussian() * 0.1;
        }

        static double sigmoid(double x) {
            return 1.0 / (1 + Math.exp(-x));
        }
    }

    public static void main(String[] args) {
        int population = 50;
        Genome[] gen = new Genome[population];
        double[][] data = {
            {0, 0, 0},
            {0, 1, 1},
            {1, 0, 1},
            {1, 1, 0}
        };

        for (int i = 0; i < population; i++)
            gen[i] = new Genome(2, 1);

        for (int genCount = 0; genCount < 1000; genCount++) {
            double[] fitness = new double[population];
            for (int i = 0; i < population; i++) {
                double error = 0;
                for (double[] d : data) {
                    double[] out = gen[i].feed(new double[]{d[0], d[1]});
                    error += Math.abs(out[0] - d[2]);
                }
                fitness[i] = 4 - error;
            }

            // sort and reproduce best
            int best = 0;
            for (int i = 1; i < population; i++)
                if (fitness[i] > fitness[best]) best = i;

            System.out.println("Gen " + genCount + " Best fitness: " + fitness[best]);

            Genome top = gen[best].copy();
            for (int i = 0; i < population; i++) {
                gen[i] = top.copy();
                if (i != best) gen[i].mutate();
            }
        }

        // final test
        System.out.println("\nFinal XOR Results:");
        for (double[] d : new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) {
            double[] out = gen[0].feed(d);
            System.out.printf("Input: %s, %s => %.4f\n", (int)d[0], (int)d[1], out[0]);
        }
    }
}
