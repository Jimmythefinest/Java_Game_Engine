# Build and Run Instructions

The engine uses a shell script based build process for Linux systems.

## Prerequisites

- **Java Development Kit (JDK)**: Ensure `javac` and `java` are in your PATH.
- **LWJGL Libraries**: The engine expects LWJGL native and JAR files to be located in `../../Java_libs/` relative to the project root.

## Compiling and Executing

Use the [run.sh](../run.sh) script located in the project root:

```bash
# From the project root:
bash run.sh
```

### Script Breakdown

1. **Compiling**:
   `javac -d bin -cp "../../Java_libs/*" $(find src -name "*.java")`
   - Compiles all `.java` files in the `src` directory.
   - Outputs compiled classes to the `bin` folder.
   - Includes the class path to external libraries.

2. **Running**:
   `java -cp "../../Java_libs/*:bin" com.rebuild.RotatingCube`
   - Executes the `com.rebuild.RotatingCube` class.
   - Includes both original source bin and libraries in the classpath.

## Manual Troubleshooting

If the application fails to start:
- Verify that the native libraries for your platform (e.g., `.so` files for Linux) are present in the library path.
- Ensure the `bin` directory exists before running the script.
