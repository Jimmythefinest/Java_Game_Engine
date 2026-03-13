package com.njst.gaming.objects;

import com.njst.gaming.Geometries.CustomGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Renderer;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ImposterBakeResult;
import com.njst.gaming.graphics.ShaderHandle;

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
        this.imposter.ambientlight_multiplier = 5f;
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
            graphicsDevice.releaseTexture(bakedTextureId);
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
        ImposterBakeResult result = graphicsDevice.bakeImposter(renderer, gameObject, bakeWidth, bakeHeight);
        if (result == null) {
            return;
        }
        int textureId = result.textureId;
        if (textureId == 0) {
            return;
        }
        if (bakedTextureId != 0) {
            graphicsDevice.releaseTexture(bakedTextureId);
        }
        bakedTextureId = textureId;
        bakedTextureWidth = result.width;
        bakedTextureHeight = result.height;
        imposter.texture = textureId;
        hasImposterTexture = true;
        lastBakedViewDir = currentViewDir;
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

        // Derive world-space quad size from a world-space bounding sphere so
        // billboard sizing is orientation-independent.
        float[] ws = computeImposterWorldSize(cameraPosition, center);
        imposter.setScale(ws[0], ws[1], 1f);

        Vector3 toCamera = new Vector3(cameraPosition).sub(center);
        float planarLen = (float) Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z);
        if (planarLen <= 0.0001f) {
            imposter.setRotation(0f, 0f, 0f);
            return;
        }

        // Cylindrical billboard: rotate only around world Y to keep imposters upright.
        float yawDeg = (float) Math.toDegrees(Math.atan2(toCamera.x, toCamera.z));
        imposter.setRotation(0f, yawDeg, 0f);
    }

    private float[] computeImposterWorldSize(Vector3 cameraPosition, Vector3 center) {
        float radius = computeWorldBoundingSphereRadius();
        if (radius <= 0.0001f) {
            return new float[] { 1f, 1f };
        }

        float d = center.distance(cameraPosition);
        if (renderer != null && renderer.camera != null) {
            float centerDepth = -renderer.camera.getViewMatrix().multiply(center).z;
            if (centerDepth > 0.0001f) {
                d = centerDepth;
            }
        }

        float halfSize;
        if (d > radius + 0.0001f) {
            halfSize = (d * radius) / (float) Math.sqrt(d * d - radius * radius);
        } else {
            halfSize = radius;
        }
        halfSize *= 1.5f;

        float size = Math.max(0.001f, halfSize * 2f);
        return new float[] { size, size };
    }

    private float computeWorldBoundingSphereRadius() {
        if (gameObject.localMin != null && gameObject.localMax != null) {
            float ex = Math.abs(gameObject.localMax.x - gameObject.localMin.x) * 0.5f;
            float ey = Math.abs(gameObject.localMax.y - gameObject.localMin.y) * 0.5f;
            float ez = Math.abs(gameObject.localMax.z - gameObject.localMin.z) * 0.5f;
            float localRadius = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
            return Math.max(0.0001f, localRadius * getMaxModelAxisScale());
        }
        if (gameObject.min != null && gameObject.max != null) {
            float dx = gameObject.max.x - gameObject.min.x;
            float dy = gameObject.max.y - gameObject.min.y;
            float dz = gameObject.max.z - gameObject.min.z;
            float worldRadius = 0.5f * (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return Math.max(0.0001f, worldRadius);
        }
        return 1f;
    }

    private float getMaxModelAxisScale() {
        if (gameObject.modelMatrix == null || gameObject.modelMatrix.r == null || gameObject.modelMatrix.r.length < 16) {
            return 1f;
        }
        float[] m = gameObject.modelMatrix.r;
        float sx = (float) Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
        float sy = (float) Math.sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6]);
        float sz = (float) Math.sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10]);
        return Math.max(0.0001f, Math.max(sx, Math.max(sy, sz)));
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
