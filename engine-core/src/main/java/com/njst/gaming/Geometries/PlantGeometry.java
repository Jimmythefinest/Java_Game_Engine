package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Procedural plant geometry generated from an L-System string.
 *
 * <p>Extends {@link Geometry} in exactly the same pattern as
 * {@link TerrainGeometry} — the constructor builds all vertex/normal/UV/index
 * arrays on the CPU and the engine's normal {@link com.njst.gaming.objects.GameObject}
 * pipeline uploads them to the GPU.</p>
 *
 * <h2>Quick start</h2>
 * <pre>
 *   // Deterministic from seed
 *   PlantConfig config = new PlantSeed(42).generateConfig();
 *   PlantGeometry geo  = new PlantGeometry(config);
 *   GameObject plant   = new GameObject(geo, textureId);
 *   plant.setPosition(0, 0, 0);
 *   scene.addGameObject(plant);
 * </pre>
 *
 * <h2>Mesh layout</h2>
 * <ul>
 *   <li>Each {@code F} segment → 6-sided open cylinder (12 triangles)</li>
 *   <li>Each {@code L} symbol → 2-triangle billboard quad</li>
 * </ul>
 */
public class PlantGeometry extends Geometry {

    /** Number of sides on each cylinder cross-section. */
    private static final int CYLINDER_SIDES = 6;

    // Raw mesh lists — converted to arrays once after turtle walk
    private final List<Float>   verts  = new ArrayList<>();
    private final List<Float>   norms  = new ArrayList<>();
    private final List<Float>   uvs    = new ArrayList<>();
    private final List<Integer> idxs   = new ArrayList<>();

    // Cached flat arrays
    private float[] vertexArray;
    private float[] normalArray;
    private float[] uvArray;
    private int[]   indexArray;

    // -------------------------------------------------------------------------
    // Turtle state
    // -------------------------------------------------------------------------

    /** Full turtle state pushed/popped on [ / ] */
    private static class TurtleState {
        Vector3 pos;
        Vector3 forward;   // normalised direction of travel
        Vector3 up;        // current "up" axis (for rolling)
        float   radius;

        TurtleState(Vector3 pos, Vector3 forward, Vector3 up, float radius) {
            this.pos     = pos.clone();
            this.forward = forward.clone();
            this.up      = up.clone();
            this.radius  = radius;
        }
    }

    // -------------------------------------------------------------------------

    public PlantGeometry(PlantConfig config) {
        // 1. Expand L-System
        LSystem lsys  = new LSystem(config.rules, config.iterations);
        String  str   = lsys.expand(config.axiom);

        // 2. Turtle walk → mesh
        turtleWalk(str, config);

        // 3. Flatten lists to arrays
        vertexArray = toFloatArray(verts);
        normalArray = toFloatArray(norms);
        uvArray     = toFloatArray(uvs);
        indexArray  = toIntArray(idxs);

        // 4. Compute geometric bounds for collision
        computeBounds();
    }

    // =========================================================================
    // Turtle walker
    // =========================================================================

    private void turtleWalk(String str, PlantConfig cfg) {
        Deque<TurtleState> stack = new ArrayDeque<>();

        // Initial turtle state — starts at origin, pointing up
        Vector3 pos     = new Vector3(0, 0, 0);
        Vector3 forward = new Vector3(0, 1, 0);   // growing upward
        Vector3 up      = new Vector3(0, 0, 1);   // "dorsal" axis for rolling
        float   radius  = cfg.trunkRadius;

        for (char c : str.toCharArray()) {
            switch (c) {

                case 'F': {
                    // Draw a cylinder segment from pos to pos+forward*step
                    Vector3 end = new Vector3(
                        pos.x + forward.x * cfg.stepLength,
                        pos.y + forward.y * cfg.stepLength,
                        pos.z + forward.z * cfg.stepLength);
                    float endRadius = radius * cfg.branchTaper;
                    addCylinder(pos, end, forward, up, radius, endRadius);
                    pos    = end;
                    radius = endRadius;
                    break;
                }

                case '+':
                    forward = rotateAround(forward, up,  cfg.angle);
                    forward.normalize();
                    reorthogonalize(forward, up);
                    break;
                case '-':
                    forward = rotateAround(forward, up, -cfg.angle);
                    forward.normalize();
                    reorthogonalize(forward, up);
                    break;
                case '^': {
                    // Pitch up: rotate BOTH forward AND up around the right axis
                    // so the whole frame tilts together and stays orthogonal.
                    Vector3 right = forward.cross(up).normalize();
                    forward = rotateAround(forward, right,  cfg.angle);
                    up      = rotateAround(up,      right,  cfg.angle);
                    forward.normalize();
                    up.normalize();
                    break;
                }
                case '&': {
                    // Pitch down — same trick in the opposite direction
                    Vector3 right = forward.cross(up).normalize();
                    forward = rotateAround(forward, right, -cfg.angle);
                    up      = rotateAround(up,      right, -cfg.angle);
                    forward.normalize();
                    up.normalize();
                    break;
                }
                case '\\': up = rotateAround(up, forward,  cfg.angle); up.normalize(); break;
                case '/':  up = rotateAround(up, forward, -cfg.angle); up.normalize(); break;

                case '[': {
                    stack.push(new TurtleState(pos, forward, up, radius));
                    // Taper radius at each branch level
                    radius *= cfg.branchTaper;
                    break;
                }
                case ']': {
                    if (!stack.isEmpty()) {
                        TurtleState st = stack.pop();
                        pos     = st.pos;
                        forward = st.forward;
                        up      = st.up;
                        radius  = st.radius;
                    }
                    break;
                }

                case 'L': {
                    addLeaf(pos, forward, up, cfg.leafSize);
                    break;
                }

                default:
                    break; // ignore unknown symbols (X, Y, etc.)
            }
        }
    }

    // =========================================================================
    // Cylinder mesh builder
    // =========================================================================

    /**
     * Adds an open tapered cylinder between {@code start} and {@code end}.
     * {@code forward} is the axis direction; {@code up} defines the radial
     * orientation for consistent UV mapping.
     */
    private void addCylinder(Vector3 start, Vector3 end,
                              Vector3 forward, Vector3 up,
                              float r0, float r1) {

        // Build two orthogonal radial axes (right and up-in-cross-section)
        Vector3 radialX = forward.cross(up).normalize();
        Vector3 radialY = forward.cross(radialX).normalize();

        int baseIndex = verts.size() / 3;

        // Generate ring vertices for base (r0) and cap (r1)
        for (int ring = 0; ring < 2; ring++) {
            Vector3 centre = (ring == 0) ? start : end;
            float   r      = (ring == 0) ? r0    : r1;
            float   v      = ring;                      // UV v = 0 or 1

            for (int s = 0; s < CYLINDER_SIDES; s++) {
                float angle = (float) (2.0 * Math.PI * s / CYLINDER_SIDES);
                float cosA  = (float) Math.cos(angle);
                float sinA  = (float) Math.sin(angle);

                // Vertex
                float vx = centre.x + (radialX.x * cosA + radialY.x * sinA) * r;
                float vy = centre.y + (radialX.y * cosA + radialY.y * sinA) * r;
                float vz = centre.z + (radialX.z * cosA + radialY.z * sinA) * r;
                verts.add(vx);
                verts.add(vy);
                verts.add(vz);

                // Normal (outward radial)
                float nx = radialX.x * cosA + radialY.x * sinA;
                float ny = radialX.y * cosA + radialY.y * sinA;
                float nz = radialX.z * cosA + radialY.z * sinA;
                norms.add(nx);
                norms.add(ny);
                norms.add(nz);

                // UV
                uvs.add((float) s / CYLINDER_SIDES);   // u
                uvs.add(v);                             // v
            }
        }

        // Stitch the two rings with quad pairs → 2 triangles each
        for (int s = 0; s < CYLINDER_SIDES; s++) {
            int next = (s + 1) % CYLINDER_SIDES;

            int b0 = baseIndex + s;
            int b1 = baseIndex + next;
            int t0 = baseIndex + CYLINDER_SIDES + s;
            int t1 = baseIndex + CYLINDER_SIDES + next;

            // Triangle 1
            idxs.add(b0);
            idxs.add(b1);
            idxs.add(t0);
            // Triangle 2
            idxs.add(b1);
            idxs.add(t1);
            idxs.add(t0);
        }
    }

    // =========================================================================
    // Leaf quad builder
    // =========================================================================

    /**
     * Adds a small billboard quad at {@code pos} oriented so it faces sideways
     * relative to the branch direction.
     */
    private void addLeaf(Vector3 pos, Vector3 forward, Vector3 up, float size) {
        // right and up in leaf plane
        Vector3 right = forward.cross(up).normalize();
        Vector3 leafUp = right.cross(forward).normalize();

        int base = verts.size() / 3;

        // 4 corners of the quad
        float[][] corners = {
            { pos.x - right.x * size,                 pos.y - right.y * size,                 pos.z - right.z * size },
            { pos.x + right.x * size,                 pos.y + right.y * size,                 pos.z + right.z * size },
            { pos.x + right.x * size + leafUp.x * size * 1.5f,
              pos.y + right.y * size + leafUp.y * size * 1.5f,
              pos.z + right.z * size + leafUp.z * size * 1.5f },
            { pos.x - right.x * size + leafUp.x * size * 1.5f,
              pos.y - right.y * size + leafUp.y * size * 1.5f,
              pos.z - right.z * size + leafUp.z * size * 1.5f }
        };

        float[][] uvCoords = { {0,0}, {1,0}, {1,1}, {0,1} };

        // Normal = leaf plane normal (forward cross right = leafUp-ish)
        Vector3 normal = right.cross(leafUp).normalize();

        for (int i = 0; i < 4; i++) {
            verts.add(corners[i][0]);
            verts.add(corners[i][1]);
            verts.add(corners[i][2]);
            norms.add(normal.x);
            norms.add(normal.y);
            norms.add(normal.z);
            uvs.add(uvCoords[i][0]);
            uvs.add(uvCoords[i][1]);
        }

        // Two triangles (front face)
        idxs.add(base);     idxs.add(base + 1); idxs.add(base + 2);
        idxs.add(base);     idxs.add(base + 2); idxs.add(base + 3);
        // Back face (so leaf is visible from both sides)
        idxs.add(base + 2); idxs.add(base + 1); idxs.add(base);
        idxs.add(base + 3); idxs.add(base + 2); idxs.add(base);
    }

    // =========================================================================
    // Utility: rotation around an arbitrary axis (Rodrigues formula)
    // =========================================================================

    /**
     * Rotates {@code v} around {@code axis} by {@code angle} radians.
     * Returns a new Vector3 (does not modify inputs).
     */
    private static Vector3 rotateAround(Vector3 v, Vector3 axis, float angle) {
        // Rodrigues: v' = v*cos + (axis×v)*sin + axis*(axis·v)*(1-cos)
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float dot = axis.dot(v);

        Vector3 cross = axis.cross(v);

        return new Vector3(
            v.x * cos + cross.x * sin + axis.x * dot * (1 - cos),
            v.y * cos + cross.y * sin + axis.y * dot * (1 - cos),
            v.z * cos + cross.z * sin + axis.z * dot * (1 - cos)
        );
    }

    /**
     * Gram-Schmidt: re-orthogonalises {@code up} so it stays perpendicular to
     * {@code forward} after floating-point drift. Modifies {@code up} in place.
     */
    private static void reorthogonalize(Vector3 forward, Vector3 up) {
        // up' = up - (up · forward) * forward
        float d = up.dot(forward);
        up.x -= d * forward.x;
        up.y -= d * forward.y;
        up.z -= d * forward.z;
        up.normalize();
    }

    // =========================================================================
    // Bounds
    // =========================================================================

    private void computeBounds() {
        if (vertexArray.length == 0) return;
        float minX = vertexArray[0], maxX = vertexArray[0];
        float minY = vertexArray[1], maxY = vertexArray[1];
        float minZ = vertexArray[2], maxZ = vertexArray[2];
        for (int i = 0; i < vertexArray.length; i += 3) {
            if (vertexArray[i    ] < minX) minX = vertexArray[i    ];
            if (vertexArray[i    ] > maxX) maxX = vertexArray[i    ];
            if (vertexArray[i + 1] < minY) minY = vertexArray[i + 1];
            if (vertexArray[i + 1] > maxY) maxY = vertexArray[i + 1];
            if (vertexArray[i + 2] < minZ) minZ = vertexArray[i + 2];
            if (vertexArray[i + 2] > maxZ) maxZ = vertexArray[i + 2];
        }
        this.min = new Vector3(minX, minY, minZ);
        this.max = new Vector3(maxX, maxY, maxZ);
    }

    // =========================================================================
    // Geometry interface
    // =========================================================================

    @Override
    public float[] getVertices()           { return vertexArray; }

    @Override
    public float[] getNormals()            { return normalArray; }

    @Override
    public float[] getTextureCoordinates() { return uvArray; }

    @Override
    public int[]   getIndices()            { return indexArray; }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
