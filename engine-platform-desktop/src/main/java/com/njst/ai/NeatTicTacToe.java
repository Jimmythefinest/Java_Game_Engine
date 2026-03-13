package com.njst.ai;

import java.util.*;

public class NeatTicTacToe {
    static int nodeIdCounter = 0;
    static int innovationCounter = 0;

    static class Node {
        int id;
        enum Type { INPUT, HIDDEN, OUTPUT }
        Type type;
        double value;

        Node(Type type) {
            this.id = nodeIdCounter++;
            this.type = type;
        }
    }

    static class Connection {
        int from, to;
        double weight;
        boolean enabled;
        int innovation;

        Connection(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.enabled = true;
            this.innovation = innovationCounter++;
        }
    }

    static class Genome {
        List<Node> nodes = new ArrayList<>();
        List<Connection> conns = new ArrayList<>();
        Random rand = new Random();

        Genome(int inputCount, int outputCount) {
            for (int i = 0; i < inputCount; i++) nodes.add(new Node(Node.Type.INPUT));
            for (int i = 0; i < outputCount; i++) nodes.add(new Node(Node.Type.OUTPUT));
            for (Node i : getNodesByType(Node.Type.INPUT)) {
                for (Node o : getNodesByType(Node.Type.OUTPUT)) {
                    conns.add(new Connection(i.id, o.id, rand.nextDouble() * 2 - 1));
                }
            }
        }

        List<Node> getNodesByType(Node.Type type) {
            List<Node> result = new ArrayList<>();
            for (Node n : nodes) if (n.type == type) result.add(n);
            return result;
        }

        double[] feed(double[] input) {
            for (Node n : nodes) n.value = 0;
            for (int i = 0; i < input.length; i++) nodes.get(i).value = input[i];

            for (Connection c : conns)
                if (c.enabled)
                    getNode(c.to).value += getNode(c.from).value * c.weight;

            for (Node n : nodes)
                if (n.type != Node.Type.INPUT)
                    n.value = sigmoid(n.value);

            return getOutputValues();
        }

        Node getNode(int id) {
            for (Node n : nodes) if (n.id == id) return n;
            throw new RuntimeException("Node not found: " + id);
        }

        void mutateAddConnection() {
            List<Node> fromNodes = new ArrayList<>(nodes);
            List<Node> toNodes = new ArrayList<>(nodes);
            Collections.shuffle(fromNodes);
            Collections.shuffle(toNodes);

            for (Node from : fromNodes) {
                for (Node to : toNodes) {
                    if (from.id == to.id || hasConnection(from.id, to.id)) continue;
                    if (from.type == Node.Type.OUTPUT && to.type != Node.Type.OUTPUT) continue;
                    conns.add(new Connection(from.id, to.id, rand.nextDouble() * 2 - 1));
                    return;
                }
            }
        }

        void mutateAddNode() {
            if (conns.isEmpty()) return;
            Connection c = conns.get(rand.nextInt(conns.size()));
            if (!c.enabled) return;

            c.enabled = false;
            Node newNode = new Node(Node.Type.HIDDEN);
            nodes.add(newNode);
            conns.add(new Connection(c.from, newNode.id, 1.0));
            conns.add(new Connection(newNode.id, c.to, c.weight));
        }

        void mutateWeights() {
            for (Connection c : conns)
                c.weight += rand.nextGaussian() * 0.1;
        }

        Genome copy() {
            Genome g = new Genome(0, 0);
            for (final Node n : nodes) g.nodes.add(new Node(n.type) {{ id = n.id; }});
            for (final Connection c : conns) g.conns.add(new Connection(c.from, c.to, c.weight) {{
                enabled = c.enabled;
                innovation = c.innovation;
            }});
            return g;
        }

        boolean hasConnection(int from, int to) {
            for (Connection c : conns)
                if (c.from == from && c.to == to)
                    return true;
            return false;
        }

        static double sigmoid(double x) {
            return 1.0 / (1.0 + Math.exp(-x));
        }

        double[] getOutputValues() {
            List<Node> outputNodes = getNodesByType(Node.Type.OUTPUT);
            double[] result = new double[outputNodes.size()];
            for (int i = 0; i < outputNodes.size(); i++) {
                result[i] = outputNodes.get(i).value;
            }
            return result;
        }
    }

    static class TicTacToe {
        int[] board = new int[9];  // 0: empty, 1: X, -1: O

        boolean isGameOver() {
            return getWinner() != 0 || isBoardFull();
        }

        int getWinner() {
            for (int i = 0; i < 3; i++) {
                if (board[i * 3] == board[i * 3 + 1] && board[i * 3 + 1] == board[i * 3 + 2] && board[i * 3] != 0)
                    return board[i * 3];
                if (board[i] == board[i + 3] && board[i + 3] == board[i + 6] && board[i] != 0)
                    return board[i];
            }
            if (board[0] == board[4] && board[4] == board[8] && board[0] != 0) return board[0];
            if (board[2] == board[4] && board[4] == board[6] && board[2] != 0) return board[2];
            return 0;
        }

        boolean isBoardFull() {
            for (int i : board) if (i == 0) return false;
            return true;
        }

        void playMove(int index, int player) {
            if (board[index] == 0) board[index] = player;
        }

        List<Integer> getAvailableMoves() {
            List<Integer> moves = new ArrayList<>();
            for (int i = 0; i < 9; i++) if (board[i] == 0) moves.add(i);
            return moves;
        }

        void reset() {
            Arrays.fill(board, 0);
        }
    }

    public static void main(String[] args) {
        int population = 100;
        Genome[] gen = new Genome[population];
        for (int i = 0; i < population; i++)
            gen[i] = new Genome(9, 9);  // 9 inputs for board state, 9 outputs for possible moves

        for (int generation = 0; generation < 1000; generation++) {
            double[] fitness = new double[population];
            for (int i = 0; i < population; i++) {
                double score = 0;
                for (int game = 0; game < 10; game++) {
                    score += playGame(gen[i]);
                }
                fitness[i] = score;
            }

            int best = 0;
            for (int i = 1; i < population; i++)
                if (fitness[i] > fitness[best]) best = i;

            System.out.printf("Generation %d - Best fitness: %.4f\n", generation, fitness[best]);

            Genome top = gen[best].copy();
            for (int i = 0; i < population; i++) {
                gen[i] = top.copy();
                if (i != best) {
                    if (Math.random() < 0.3) gen[i].mutateAddConnection();
                    if (Math.random() < 0.2) gen[i].mutateAddNode();
                    gen[i].mutateWeights();
                }
            }
        }
    }

    public static double playGame(Genome genome) {
        TicTacToe game = new TicTacToe();
        int player = 1;

        while (!game.isGameOver()) {
            double[] input = new double[9];
            for (int i = 0; i < 9; i++) input[i] = game.board[i] == 0 ? 0 : (game.board[i] == 1 ? 1 : -1);
            double[] output = genome.feed(input);

            List<Integer> availableMoves = game.getAvailableMoves();
            int move = getBestMove(output, availableMoves);
            game.playMove(move, player);

            if (game.getWinner() != 0) {
                return (game.getWinner() == player) ? 1 : -1;
            }

            player = -player;
        }
        return 0;  // Draw
    }

    public static int getBestMove(double[] output, List<Integer> availableMoves) {
        int bestMove = availableMoves.get(0);
        double bestScore = output[bestMove];
        for (int move : availableMoves) {
            if (output[move] > bestScore) {
                bestScore = output[move];
                bestMove = move;
            }
        }
        return bestMove;
    }
}
