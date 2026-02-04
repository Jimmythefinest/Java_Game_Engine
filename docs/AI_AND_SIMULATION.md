# AI and Simulation Systems

The engine includes a modular AI package located at `com.njst.gaming.ai`.

## Neural Networks ([NeuralNetwork.java](../src/com/njst/gaming/ai/NeuralNetwork.java))

The base `NeuralNetwork` class supports:
- **Layer-based Architecture**: Configurable number of inputs, hidden layers, and outputs.
- **Training**: Forward and backpropagation logic.
- **Serialization**: Ability to save and load network weights.

## Reinforcement Learning ([RLNeuralNetwork.java](../src/com/njst/gaming/ai/RLNeuralNetwork.java))

Extends the base neural network to support reinforcement learning principles. It is designed for agents that learn from environmental interaction through rewards.

## Q-Learning ([QLearningNeuralNetwork.java](../src/com/njst/gaming/ai/QLearningNeuralNetwork.java))

A specialized implementation for Q-Learning, used in discrete state-action environments. An example application is found in `TicTacToeQLearning.java`.

## Evolutionary Simulations ([EvolutionSimulation.java](../src/com/njst/gaming/EvolutionSimulation.java))

Allows for the creation of environments where multiple agents (entities) can evolve.
- **Fitness Evaluation**: Scoring agents based on performance.
- **Crossover/Mutation**: Generating the next generation of agents based on the highest-scoring individuals.
