# Build and Run Instructions

The engine supports a Gradle-based build process.

## Prerequisites

- **Java Development Kit (JDK)**: Ensure `javac` and `java` are in your PATH.
- **LWJGL Libraries**: The engine expects LWJGL native and JAR files to be located in `../../Java_libs/` relative to the project root.

## Building and Executing With Gradle

From project root:

```bash
gradle build
```

Run the demo:

```bash
gradle runDemo
```

### Gradle Configuration Notes

1. Sources are compiled from `src/` (current non-standard project layout).
2. Resources are loaded from `src/resources/`.
3. External dependencies are read from `../../Java_libs/*.jar`.
4. Demo entrypoint is `com.rebuild.RotatingCube`.

## Legacy Script-Based Build (Fallback)

Use the [run.sh](../run.sh) script:

```bash
bash run.sh
```

## Manual Troubleshooting

If the application fails to start:
- Verify that the native libraries for your platform (e.g., `.so` files for Linux) are present in the library path.
- Ensure the `bin` directory exists before running the script.
