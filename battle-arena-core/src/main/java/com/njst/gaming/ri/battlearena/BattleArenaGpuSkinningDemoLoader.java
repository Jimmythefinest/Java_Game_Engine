package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Camera;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.Weighted_GameObject;

import java.util.ArrayList;

public final class BattleArenaGpuSkinningDemoLoader implements Scene.SceneLoader {
    private static final String CHARACTER_DEFINITION_FILE = "battle_arena/defeated.character.json";
    private static final float PLAYER_SCALE = 1f;
    private static final boolean RENDER_CHARACTERS = true;
    private static final boolean ANIMATE_CHARACTERS = true;
    private static final float[][] CHARACTER_POSITIONS = new float[][] {
            {-1.6f, 0f, 0f},
            {0f, 0f, 0f},
            {1.6f, 0f, 0f},
            {1.6f*2, 0f, 0f},
            {1.6f*3, 0f, 0f},
            {1.6f*4, 0f, 0f},
            {1.6f*5, 0f, 0f},
            {1.6f*6, 0f, 0f},
            {1.6f*7, 0f, 0f},
            {1.6f*8, 0f, 0f},
            {1.6f*9, 0f, 0f},
            {-1.6f*2, 0f, 0f},
            {-1.6f*3, 0f, 0f},
            {-1.6f*4, 0f, 0f},
            {-1.6f*5, 0f, 0f},
            {-1.6f*6, 0f, 0f},
            {-1.6f*7, 0f, 0f},
            {-1.6f*8, 0f, 0f},
            {-1.6f*9, 0f, 0f}
    };
    private static final float[][] SINGLE_CHARACTER_POSITION = new float[][] {
            {0f, 0f, 0f}
    };

    private final BattleArenaEnvironmentLoader environmentLoader = new BattleArenaEnvironmentLoader();
    private final BattleArenaCharacterAssembler characterAssembler = new BattleArenaCharacterAssembler();
    private final BattleArenaCharacterDefinitionLoader definitionLoader = new BattleArenaCharacterDefinitionLoader();

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        if (!BattleArenaGpuBoneSsboManager.isSupported(graphicsDevice)) {
            throw new IllegalStateException("Battle Arena GPU bone compute is not supported by this graphics device");
        }

        scene.setExternalSkeletonBufferActive(true);
        scene.renderer.setShadowMapEnabled(false);
        environmentLoader.load(scene, graphicsDevice);

        WeightedGeometry geometry = null;
        int texture = 0;
        if (RENDER_CHARACTERS) {
            BattleArenaCharacterDefinition definition =
                    definitionLoader.load(graphicsDevice, CHARACTER_DEFINITION_FILE);
            geometry = characterAssembler.loadWeightedGeometry(
                    graphicsDevice,
                    definition.model.mesh);
            texture = graphicsDevice.loadTexture(
                    BattleArenaEnvironmentLoader.resolveResourcePath(definition.model.texture));
        }

        BattleArenaGpuBoneSsboManager gpuBoneSsboManager =
                new BattleArenaGpuBoneSsboManager(graphicsDevice);
        ArrayList<DemoPoseSource> poseSources = new ArrayList<DemoPoseSource>();
        float[][] characterPositions = resolveCharacterPositions();
        for (int i = 0; i < characterPositions.length; i++) {
            DemoPoseSource poseSource = createCharacter(
                    scene,
                    geometry,
                    texture,
                    characterPositions,
                    gpuBoneSsboManager.boneCount(),
                    i);
            poseSource.advance(i * 0.35f);
            gpuBoneSsboManager.registerSkeleton(poseSource);
            poseSources.add(poseSource);
        }

        scene.animations.add(new Animation() {
            private boolean staticPoseUploaded;

            @Override
            public void animate(float deltaSeconds) {
                if (!ANIMATE_CHARACTERS && staticPoseUploaded) {
                    scene.renderer.recordBoneCalculationNanos(0L);
                    updateCamera(scene.renderer.camera);
                    return;
                }
                for (DemoPoseSource poseSource : poseSources) {
                    if (ANIMATE_CHARACTERS) {
                        poseSource.advance(deltaSeconds);
                    }
                    gpuBoneSsboManager.syncPose(poseSource);
                }
                long boneStartNanos = System.nanoTime();
                gpuBoneSsboManager.dispatchAll(graphicsDevice);
                scene.renderer.recordBoneCalculationNanos(System.nanoTime() - boneStartNanos);
                staticPoseUploaded = true;
                updateCamera(scene.renderer.camera);
            }
        });

        updateCamera(scene.renderer.camera);
        BattleArenaDemoLoader.log("GPU skinning demo loaded characters=" + poseSources.size()
                + " bonesPerCharacter=" + gpuBoneSsboManager.boneCount()
                + " gpuInstances=" + gpuBoneSsboManager.instanceCount());
    }

    private DemoPoseSource createCharacter(Scene scene,
                                           WeightedGeometry geometry,
                                           int texture,
                                           float[][] characterPositions,
                                           int boneCount,
                                           int characterIndex) {
        int boneBufferStartIndex = scene.reserveSkeleton(boneCount);
        if (RENDER_CHARACTERS) {
            Weighted_GameObject meshObject = new Weighted_GameObject(geometry, texture);
            meshObject.name = "BattleArena_GpuSkinning_Probe_" + characterIndex;
            meshObject.shininess = 18f;
            meshObject.ambientlight_multiplier = 1.2f;
            meshObject.setScale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
            float[] position = characterPositions[characterIndex];
            meshObject.setPosition(position[0], position[1], position[2]);
            meshObject.boneBufferStartIndex = boneBufferStartIndex;
            scene.addGameObject(meshObject);
        }
        return new DemoPoseSource(boneBufferStartIndex, boneCount);
    }

    private float[][] resolveCharacterPositions() {
        int requestedCount = Integer.getInteger("battleArena.gpuSkinningDemo.characters", -1);
        if (requestedCount == 1) {
            return SINGLE_CHARACTER_POSITION;
        }
        return CHARACTER_POSITIONS;
    }

    private void updateCamera(Camera camera) {
        camera.lookAt(
                new Vector3(0f, 1.7f, -15.2f),
                new Vector3(0f, 1.1f, 0f),
                new Vector3(0f, 1f, 0f));
    }

    private static final class DemoPoseSource implements BattleArenaGpuSkeletonPoseSource {
        private final int boneBufferStartIndex;
        private final int boneCount;
        private float animationFrame;

        private DemoPoseSource(int boneBufferStartIndex, int boneCount) {
            this.boneBufferStartIndex = boneBufferStartIndex;
            this.boneCount = boneCount;
        }

        void advance(float deltaSeconds) {
            animationFrame += Math.max(0f, deltaSeconds) * 30f;
        }

        @Override
        public String currentGpuAnimationKey() {
            return BattleArenaCharacterController.ANIM_IDLE;
        }

        @Override
        public float currentGpuAnimationFrame() {
            return animationFrame;
        }

        @Override
        public int gpuBoneBufferStartIndex() {
            return boneBufferStartIndex;
        }

        @Override
        public int gpuBoneCount() {
            return boneCount;
        }
    }
}
