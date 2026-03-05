package com.njst.gaming.objects;

import com.njst.gaming.Geometries.CustomGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Renderer;
import com.njst.gaming.Utils.GameObjectRenderUtil;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ShaderHandle;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class LODGameObject extends GameObject {
    public final GameObject gameObject;
    public final GameObject imposter;
    public float lodSwitchDistance;
    public float acceptanceConeDegrees = 20f;
    public int bakeWidth = 512;
    public int bakeHeight = 512;
    /** Set this to the active Renderer so the standard render loop triggers LOD automatically. */
    public Renderer renderer = null;

    private int bakedTextureId = 0;
    private int bakedTextureWidth = 0;
    private int bakedTextureHeight = 0;
    private boolean hasImposterTexture = false;
    private boolean wasInsideLodRange = true;
    private Vector3 lastBakedViewDir = null;

    public LODGameObject(GameObject gameObject, int imposterTexture, float lodSwitchDistance) {
        super(createQuadGeometry(gameObject), imposterTexture);
        if (gameObject == null) {
            throw new IllegalArgumentException("gameObject cannot be null");
        }
        this.gameObject = gameObject;
        this.imposter = new GameObject(createQuadGeometry(gameObject), imposterTexture);
        this.lodSwitchDistance = lodSwitchDistance;
        syncProxyBounds();
    }

    public LODGameObject(GameObject gameObject, int imposterTexture) {
        this(gameObject, imposterTexture, 25f);
    }

    public GameObject getActiveObject(Vector3 cameraPosition) {
        if (cameraPosition == null) {
            return gameObject;
        }
        float distance = getCenter().distance(cameraPosition);
        return distance >= lodSwitchDistance ? imposter : gameObject;
    }

    public void render(ShaderHandle shader, int textureHandle, Vector3 cameraPosition, Renderer renderer) {
        gameObject.updateModelMatrix();
        syncProxyBounds();

        if (cameraPosition == null || renderer == null) {
            gameObject.render(shader, textureHandle);
            return;
        }

        Vector3 center = getCenter();
        float distance = center.distance(cameraPosition);
        boolean outsideLodRange = distance >= lodSwitchDistance;
        if (!outsideLodRange) {
            wasInsideLodRange = true;
            gameObject.render(shader, textureHandle);
            return;
        }

        Vector3 currentViewDir = new Vector3(cameraPosition).sub(center);
        currentViewDir.y = 0f;
        float viewLen = currentViewDir.length();
        if (viewLen > 0.0001f) {
            currentViewDir.mul(1f / viewLen);
        } else {
            currentViewDir = new Vector3(0f, 0f, 1f);
        }
        boolean enteredLodRange = wasInsideLodRange;
        boolean needsBake = !hasImposterTexture || enteredLodRange || !isInsideAcceptanceCone(currentViewDir);

        if (needsBake) {
            bakeImposterTexture(renderer, currentViewDir);
        }
        if (!hasImposterTexture) {
            gameObject.render(shader, textureHandle);
            wasInsideLodRange = false;
            return;
        }

        updateImposterTransform(cameraPosition, center);
        imposter.render(shader, textureHandle);
        wasInsideLodRange = false;
    }

    @Override
    public void render(ShaderHandle shader, int textureHandle) {
        gameObject.updateModelMatrix();
        syncProxyBounds();
        if (renderer != null) {
            // Use the LOD-aware path: bake imposter when needed and switch at distance.
            render(shader, textureHandle, renderer.camera.cameraPosition, renderer);
        } else {
            gameObject.render(shader, textureHandle);
        }
    }

    @Override
    public void setGraphicsDevice(GraphicsDevice graphicsDevice) {
        super.setGraphicsDevice(graphicsDevice);
        gameObject.setGraphicsDevice(graphicsDevice);
        imposter.setGraphicsDevice(graphicsDevice);
    }

    @Override
    public void updateModelMatrix() {
        gameObject.updateModelMatrix();
        syncProxyBounds();
    }

    @Override
    public void setPosition(float x, float y, float z) {
        gameObject.setPosition(x, y, z);
        syncProxyBounds();
    }

    @Override
    public void setScale(float sx, float sy, float sz) {
        gameObject.setScale(sx, sy, sz);
        syncProxyBounds();
    }

    @Override
    public void resize(float sx, float sy, float sz) {
        gameObject.resize(sx, sy, sz);
        syncProxyBounds();
    }

    @Override
    public void setRotation(float x, float y, float z) {
        gameObject.setRotation(x, y, z);
        syncProxyBounds();
    }

    @Override
    public void rotate(float x, float y, float z) {
        gameObject.rotate(x, y, z);
        syncProxyBounds();
    }

    @Override
    public void move(float x, float y, float z) {
        gameObject.move(x, y, z);
        syncProxyBounds();
    }

    @Override
    public void translate(Vector3 position) {
        gameObject.translate(position);
        syncProxyBounds();
    }

    public void cleanupImposterTexture() {
        if (bakedTextureId != 0) {
            glDeleteTextures(bakedTextureId);
            if (imposter.texture == bakedTextureId) {
                imposter.texture = 0;
            }
            bakedTextureId = 0;
            bakedTextureWidth = 0;
            bakedTextureHeight = 0;
            hasImposterTexture = false;
        }
    }

    private void syncProxyBounds() {
        modelMatrix = gameObject.modelMatrix;
        position = gameObject.position;
        scale = gameObject.scale;
        min = gameObject.min;
        max = gameObject.max;
        collisionBounds = gameObject.collisionBounds;
    }

    private void bakeImposterTexture(Renderer renderer, Vector3 currentViewDir) {
        BufferedImage image = GameObjectRenderUtil.renderToBitmap(renderer, gameObject, bakeWidth, bakeHeight);
        if (image == null) {
            return;
        }
        int textureId = GameObjectRenderUtil.uploadImageAsTexture(image);
        if (textureId == 0) {
            return;
        }
        if (bakedTextureId != 0) {
            glDeleteTextures(bakedTextureId);
        }
        bakedTextureId = textureId;
        bakedTextureWidth = image.getWidth();
        bakedTextureHeight = image.getHeight();
        imposter.texture = textureId;
        hasImposterTexture = true;
        lastBakedViewDir = currentViewDir;
        saveImposterImage(image);
    }

    /**
     * Saves the baked imposter image to disk as a PNG for inspection/debugging.
     * File is written to the working directory as:
     *   imposter_<name>_<timestamp>.png
     */
    private void saveImposterImage(BufferedImage image) {
        try {
            String name = (gameObject.name != null && !gameObject.name.isEmpty())
                    ? gameObject.name.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                    : "object";
            String filename = "imposter_" + name + "_" + System.currentTimeMillis() + ".png";
            File out = new File(filename);
            ImageIO.write(image, "PNG", out);
            System.out.println("[LODGameObject] Imposter saved: " + out.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[LODGameObject] Failed to save imposter image: " + e.getMessage());
        }
    }

    private boolean isInsideAcceptanceCone(Vector3 currentViewDir) {
        if (lastBakedViewDir == null || currentViewDir == null) {
            return false;
        }
        float dot = lastBakedViewDir.dot(currentViewDir);
        if (dot > 1f) dot = 1f;
        if (dot < -1f) dot = -1f;
        float coneRadians = (float) Math.toRadians(acceptanceConeDegrees);
        float limit = (float) Math.cos(coneRadians);
        return dot >= limit;
    }

    private void updateImposterTransform(Vector3 cameraPosition, Vector3 center) {
        imposter.setPosition(center.x, center.y, center.z);

        // Derive world-space quad size from the 3D AABB's screen projection.
        // This accounts for Z-depth: all 8 corners are projected and the NDC span
        // is back-projected to world units using distance + FOV.
        float[] ws = computeImposterWorldSize(cameraPosition, center);
        imposter.setScale(ws[0], ws[1], 1f);

        Vector3 toCamera = new Vector3(cameraPosition).sub(center);
        float len = toCamera.length();
        if (len <= 0.0001f) {
            imposter.setRotation(0f, 0f, 0f);
            return;
        }

        float planarLen = (float) Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z);
        float yawDeg   = (float)  Math.toDegrees(Math.atan2(toCamera.x, toCamera.z));
        float pitchDeg = (float) -Math.toDegrees(Math.atan2(toCamera.y, planarLen));
        imposter.setRotation(pitchDeg, yawDeg, 0f);
    }

    /**
     * Projects all 8 LOCAL-space AABB corners through the full MVP matrix
     * (proj * view * model), measures the NDC span, then back-projects to
     * world-space size at the imposter's distance from the camera.
     *
     * Using localMin/localMax + modelMatrix instead of the world-space AABB
     * (min/max) avoids the inflated axis-aligned overestimate that occurs when
     * the object is rotated or non-uniformly scaled.
     *
     * Formula:
     *   worldHeight = ndcSpanY * distance * tan(FOV/2)
     *   worldWidth  = ndcSpanX * distance * tan(FOV/2) * aspect
     */
    private float[] computeImposterWorldSize(Vector3 cameraPosition, Vector3 center) {
        if (renderer == null || gameObject.localMin == null || gameObject.localMax == null) {
            // Fallback: flat local extents (no Z-depth correction)
            float w = (gameObject.localMin != null && gameObject.localMax != null)
                    ? Math.abs(gameObject.localMax.x - gameObject.localMin.x) : 1f;
            float h = (gameObject.localMin != null && gameObject.localMax != null)
                    ? Math.abs(gameObject.localMax.y - gameObject.localMin.y) : 1f;
            return new float[]{ Math.max(0.001f, w), Math.max(0.001f, h) };
        }

        // Use LOCAL-space corners so the model matrix is applied precisely,
        // avoiding the inflated AABB that arises from re-boxing after rotation.
        Vector3 mn = gameObject.localMin;
        Vector3 mx = gameObject.localMax;
        Vector3[] corners = {
            new Vector3(mn.x, mn.y, mn.z), new Vector3(mn.x, mn.y, mx.z),
            new Vector3(mn.x, mx.y, mn.z), new Vector3(mn.x, mx.y, mx.z),
            new Vector3(mx.x, mn.y, mn.z), new Vector3(mx.x, mn.y, mx.z),
            new Vector3(mx.x, mx.y, mn.z), new Vector3(mx.x, mx.y, mx.z),
        };

        Matrix4 view  = renderer.camera.getViewMatrix();
        Matrix4 proj  = renderer.camera.getProjectionMatrix();
        Matrix4 viewModel = new Matrix4().set(view.r).multiply(gameObject.modelMatrix);
        // Full MVP: proj * view * model — correctly transforms local corners to NDC
        Matrix4 viewProj = new Matrix4().set(proj.r).multiply(viewModel);

        float minNdcX = Float.POSITIVE_INFINITY,  maxNdcX = Float.NEGATIVE_INFINITY;
        float minNdcY = Float.POSITIVE_INFINITY,  maxNdcY = Float.NEGATIVE_INFINITY;
        float minDepth = Float.POSITIVE_INFINITY;

        for (Vector3 c : corners) {
            Vector3 ndc = viewProj.multiply(c);
            if (ndc.x < minNdcX) minNdcX = ndc.x;
            if (ndc.x > maxNdcX) maxNdcX = ndc.x;
            if (ndc.y < minNdcY) minNdcY = ndc.y;
            if (ndc.y > maxNdcY) maxNdcY = ndc.y;

            Vector3 viewPos = viewModel.multiply(c);
            float depth = -viewPos.z;
            if (depth > 0.0001f && depth < minDepth) {
                minDepth = depth;
            }
        }

        float ndcSpanX = maxNdcX - minNdcX;  // 0..2 (2 = full screen width)
        float ndcSpanY = maxNdcY - minNdcY;  // 0..2 (2 = full screen height)

        float centerDepth = -view.multiply(center).z;
        float d = centerDepth > 0.0001f ? centerDepth : center.distance(cameraPosition);
        // Use nearest visible corner depth to avoid under-sizing thick objects.
        if (minDepth != Float.POSITIVE_INFINITY) {
            d = Math.min(d, minDepth);
        }
        float tanFov   = (float) Math.tan(Math.toRadians(renderer.camera.FOV * 0.5f));
        float aspect   = renderer.camera.aspect;

        float worldH = ndcSpanY * d * tanFov;
        float worldW = ndcSpanX * d * tanFov * aspect;

        return new float[]{ Math.max(0.001f, worldW), Math.max(0.001f, worldH) };
    }

    private Vector3 getCenter() {
        if (gameObject.min == null || gameObject.max == null) {
            return new Vector3(gameObject.position);
        }
        return new Vector3(
                (gameObject.min.x + gameObject.max.x) * 0.5f,
                (gameObject.min.y + gameObject.max.y) * 0.5f,
                (gameObject.min.z + gameObject.max.z) * 0.5f);
    }

    /**
     * Creates a unit quad (-0.5 to +0.5 on X and Y).
     * World-space size is controlled entirely by the imposter's scale, which is
     * set dynamically each frame via computeImposterWorldSize().
     */
    private static Geometry createQuadGeometry(GameObject source) {
        float[] vertices = { -0.5f, -0.5f, 0f,
                              0.5f, -0.5f, 0f,
                              0.5f,  0.5f, 0f,
                             -0.5f,  0.5f, 0f };
        int[]   indices  = { 0, 1, 2,  0, 2, 3 };
        float[] normals  = { 0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f };
        float[] uv       = { 0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f };
        return new CustomGeometry(vertices, indices, normals, uv);
    }
}
