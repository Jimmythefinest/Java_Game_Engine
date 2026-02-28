package com.njst.gaming.objects;

import com.njst.gaming.Geometries.CustomGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Renderer;
import com.njst.gaming.Utils.GameObjectRenderUtil;
import java.awt.image.BufferedImage;

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

    public void render(ShaderProgram shader, int textureHandle, Vector3 cameraPosition, Renderer renderer) {
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
    public void render(ShaderProgram shader, int textureHandle) {
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
        // The quad geometry was authored using localMin/localMax extents directly as vertex positions,
        // so the world-space size is already baked in. No extra scale needed — (1,1,1) preserves it.
        imposter.setScale(1f, 1f, 1f);

        Vector3 toCamera = new Vector3(cameraPosition).sub(center);
        float len = toCamera.length();
        if (len <= 0.0001f) {
            imposter.setRotation(0f, 0f, 0f);
            return;
        }

        float planarLen = (float) Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z);
        float yawDeg = (float) Math.toDegrees(Math.atan2(toCamera.x, toCamera.z));
        float pitchDeg = (float) -Math.toDegrees(Math.atan2(toCamera.y, planarLen));
        imposter.setRotation(pitchDeg, yawDeg, 0f);
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

    private static Geometry createQuadGeometry(GameObject source) {
        float width = 1f;
        float height = 1f;
        if (source != null && source.localMin != null && source.localMax != null) {
            float computedWidth = source.localMax.x - source.localMin.x;
            float computedHeight = source.localMax.y - source.localMin.y;
            if (computedWidth > 0f) {
                width = computedWidth;
            }
            if (computedHeight > 0f) {
                height = computedHeight;
            }
        }

        float halfW = width * 0.5f;
        float halfH = height * 0.5f;

        float[] vertices = new float[] {
                -halfW, -halfH, 0f,
                halfW, -halfH, 0f,
                halfW, halfH, 0f,
                -halfW, halfH, 0f
        };
        int[] indices = new int[] {
                0, 1, 2,
                0, 2, 3
        };
        float[] normals = new float[] {
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f
        };
        float[] uv = new float[] {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
        };

        return new CustomGeometry(vertices, indices, normals, uv);
    }
}
