# Matrix4

This document describes `com.njst.gaming.Math.Matrix4`, the engine's mutable 4x4 matrix helper used for transforms, camera views, projections, and vector conversion.

Source:

- `engine-core/src/main/java/com/njst/gaming/Math/Matrix4.java`

## Purpose

`Matrix4` is a thin wrapper around a `float[16]` named `r`.

It is used for:

- model transforms on `GameObject`
- camera view matrices
- projection matrices
- matrix inversion and composition
- transforming `Vector3` values

Most mutating operations delegate to JOML internally.

## Storage Model

- Matrix data is stored in `public float[] r`
- The implementation follows column-major OpenGL-style layout
- Methods usually mutate the current instance and return `this`

Example:

```java
Matrix4 model = new Matrix4()
        .identity()
        .translate(new Vector3(0f, 1f, 0f))
        .rotate(45f, new Vector3(0f, 1f, 0f))
        .scale(new Vector3(2f, 2f, 2f));
```

## Core Methods

### `identity()`

Resets the matrix to the identity matrix.

Use this before building a fresh transform chain.

### `set(float[] r)`

Replaces the internal matrix array reference.

This does not copy the array.

Use carefully when you do not want aliasing surprises.

### `translate(float x, float y, float z)`
### `translate(Vector3 pos)`

Applies translation to the current matrix.

### `rotate(float radians, Vector3 axis)`

Applies axis-angle rotation using JOML.

Important:

- the parameter name is `radians`
- but the implementation converts it with `Math.toRadians(...)`
- so current engine usage treats this value as degrees

In practice, call it with degrees, not radians.

### `rotate(Quaternion quaternion)`

Applies quaternion rotation.

Useful when orientation is already represented as a quaternion instead of Euler angles.

### `scale(Vector3 scale)`

Applies non-uniform scale.

## Camera and Projection Methods

### `lookAt(Vector3 eye, Vector3 target, Vector3 up)`
### `lookAt(float eyeX, ... float upZ)`

Builds a view matrix from camera position, target, and up direction.

This is used by `Camera.getViewMatrix()`.

### `perspective(float fov, float aspect, float near, float far)`

Builds a perspective projection matrix.

Important:

- the formula uses `tan(fov / 2)`
- there is no internal degree-to-radian conversion here

So mathematically this method expects `fov` in radians.

If you are touching camera/projection code, verify call-site units carefully.

### `ortho(float left, float right, float bottom, float top, float near, float far)`

Builds an orthographic projection matrix through JOML.

## Data Extraction

### `get(float[] out)`

Returns the internal `r` array reference.

The provided parameter is not filled element-by-element; it is simply replaced locally and returned.

Typical use:

```java
float[] raw = matrix.get(new float[16]);
```

### `getMatrix4f()`

Returns the internal `r` array directly.

### `get(FloatBuffer buffer)`

Converts the matrix to a `FloatBuffer` through `Utils.Array_to_Buffer(...)`.

Note:

- this method does not write into the passed buffer
- it replaces the local parameter only

If you need the actual `FloatBuffer`, prefer `getAsBuffer()`.

### `getAsBuffer()`

Returns a newly created `FloatBuffer`.

## Matrix Operations

### `invert()`

Inverts the current matrix in place.

### `multiply(Matrix4 other)`

Returns a new `Matrix4` representing `this * other`.

Important:

- this method uses JOML multiplication order
- it returns a new `Matrix4`
- it also reuses the current internal array while constructing the result, so treat the result as the authoritative value and avoid assumptions about intermediate aliasing

## Vector Transformation

### `multiply(Vector3 vector)`

Transforms a `Vector3` by the matrix using `transformProject(...)`.

That means:

- the vector is treated as `(x, y, z, 1)`
- perspective divide is applied

This is useful for:

- transforming local points into world space
- projecting points through combined matrices

Be aware that this is a point transform, not a direction-only transform.

## Common Usage in the Engine

### Model Matrix Build

`GameObject.updateModelMatrix()` uses `Matrix4` like this:

1. `identity()`
2. `translate(position)`
3. `rotate(x, X-axis)`
4. `rotate(y, Y-axis)`
5. `rotate(z, Z-axis)`
6. `scale(scale)`

### View Matrix

`Camera.getViewMatrix()` returns:

```java
new Matrix4().lookAt(cameraPosition, targetPosition, upDirection)
```

### Projection Matrix

`Camera.getProjectionMatrix()` returns:

```java
new Matrix4().perspective(FOV, aspect, near, far)
```

## Current Gotchas

These are the main things to keep in mind when modifying or relying on `Matrix4`:

1. `rotate(float, axis)` behaves like it expects degrees, not radians.
2. `perspective(float, ...)` uses the input as radians mathematically.
3. `set(float[])` stores the reference rather than copying.
4. `get(FloatBuffer)` does not populate the provided buffer as its signature might suggest.
5. `multiply(Vector3)` performs a projected point transform, not a pure affine direction transform.

## Recommended Safe Usage

- Call `identity()` before building transforms from scratch
- Treat `rotate(...)` inputs as degrees in current engine code
- Treat `perspective(...)` units carefully and verify call sites before changing behavior
- Prefer `getAsBuffer()` over `get(FloatBuffer)` when you need a buffer result
- Avoid mutating arrays returned by `getMatrix4f()` unless you intentionally want to change the matrix
