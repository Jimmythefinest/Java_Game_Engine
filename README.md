# NJST Game Engine

A high-performance, modular 3D game engine built in Java and powered by **LWJGL** (LightWeight Java Game Library).

## 🚀 Overview

NJST Game Engine is designed for real-time 3D rendering and integrated artificial intelligence simulations. It provides a robust framework for building interactive environments with specialized support for neural networks and evolutionary algorithms.

## ✨ Key Features

- **Standard Rendering Pipeline**: Efficient OpenGL-based renderer supporting custom GLSL shaders and SSBO (Shader Storage Buffer Objects).
- **Core Engine Architecture**: Clean separation between Scene Management, Rendering logic, and Game Objects.
- **Integrated AI Systems**:
  - Modular **Neural Networks** (Feedforward & Training).
  - **Reinforcement Learning** (Q-Learning) for intelligent agents.
  - **Evolutionary Simulations** for genetic-algorithm-based scenarios.
- **Animation System**: Keyframe-based animation support for objects and models.
- **Physics Engine**: Basic collision detection and physics simulation hooks.

## 🛠 Project Structure

- `src/`: Core source code for the engine and AI components.
- `docs/`: Technical documentation and deep dives into engine internals.
- `run.sh`: Linux shell script for compilation and execution.

## 🏁 Quick Start

### Prerequisites
- JDK 8 or higher.
- LWJGL libraries (expected in `../../Java_libs/`).

### How to Run
To compile and launch the sample Rotating Cube demo:
```bash
./run.sh
```

## 📚 Documentation

For detailed technical references, please see the `docs` folder:
- [General Overview](docs/OVERVIEW.md)
- [Core Engine Components](docs/CORE_ENGINE.md)
- [AI & Simulations](docs/AI_AND_SIMULATION.md)
- [Build & Run Guide](docs/BUILD_AND_RUN.md)

## 🤝 Contributing
Feel free to fork the repository and submit pull requests for features or bug fixes. For large changes, please open an issue first to discuss what you would like to change.
