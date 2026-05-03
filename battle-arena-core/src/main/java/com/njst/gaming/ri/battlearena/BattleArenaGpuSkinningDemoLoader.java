package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Camera;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.ri.battlearena.controls.BattleArenaActions;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterControlState;
import com.njst.gaming.objects.Weighted_GameObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGpuSkinningDemoLoader implements Scene.SceneLoader {
    private static final String CHARACTER_DEFINITION_FILE = "battle_arena/defeated.character.json";
    private static final float PLAYER_SCALE = 1f;
    private static final boolean RENDER_CHARACTERS = true;
    private static final boolean ANIMATE_CHARACTERS = true;
    private static final String LOCAL_PLAYER_ID = "player_0";
    private static final String NPC_PLAYER_ID = "player_1";
    private static final float CAMERA_DISTANCE = 7.5f;
    private static final float CAMERA_HEIGHT = 2.4f;
    private static final float CAMERA_FOCUS_HEIGHT = 1.1f;
    private static final float LOOK_SENSITIVITY = 0.0125f;
    private static final float MIN_CAMERA_PITCH = -0.8f;
    private static final float MAX_CAMERA_PITCH = 0.45f;
    private static final float MAX_ACCUMULATED_SECONDS = 0.1f;
    private static final float[][] CHARACTER_POSITIONS = new float[][] {
            {-1.6f, 0f, 0f},
            {0f, 0f, 0f},
            {1.6f, 0f, 0f},
            {1.6f*2, 0f, 0f}
    };
    private static final float[][] SINGLE_CHARACTER_POSITION = new float[][] {
            {0f, 0f, 0f}
    };

    private final BattleArenaEnvironmentLoader environmentLoader = new BattleArenaEnvironmentLoader();
    private final BattleArenaCharacterAssembler characterAssembler = new BattleArenaCharacterAssembler();
    private final BattleArenaCharacterDefinitionLoader definitionLoader = new BattleArenaCharacterDefinitionLoader();
    private float cameraYaw;
    private float cameraPitch = -0.18f;

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        if (!BattleArenaGpuBoneSsboManager.isSupported(graphicsDevice)) {
            throw new IllegalStateException("Battle Arena GPU bone compute is not supported by this graphics device");
        }

        scene.setExternalSkeletonBufferActive(true);
        scene.renderer.setShadowMapEnabled(false);
        environmentLoader.load(scene, graphicsDevice);
        ActionInput actions = scene.actionInput;
        PointerState movementPointer = scene.pointer(BattleArenaActions.MOVE_POINTER);
        cameraYaw = 0f;
        cameraPitch = -0.18f;
        scene.registerPointerInput(BattleArenaActions.LOOK_POINTER,
                (activeScene, pointer) -> handlePointerLook(actions, pointer));

        WeightedGeometry geometry = null;
        int texture = 0;
        BattleArenaCharacterDefinition definition = null;
        if (RENDER_CHARACTERS) {
            definition = definitionLoader.load(graphicsDevice, CHARACTER_DEFINITION_FILE);
            geometry = characterAssembler.loadWeightedGeometry(
                    graphicsDevice,
                    definition.model.mesh);
            texture = graphicsDevice.loadTexture(
                    BattleArenaEnvironmentLoader.resolveResourcePath(definition.model.texture));
        }

        BattleArenaGpuBoneSsboManager gpuBoneSsboManager =
                new BattleArenaGpuBoneSsboManager(graphicsDevice);
        BattleArenaSimulationServer simulationServer =
                createSimulationServer(resolveCharacterPositions(), gpuBoneSsboManager);
        BattleArenaGpuDemoCombatController combatController =
                new BattleArenaGpuDemoCombatController(simulationServer, graphicsDevice, definition);
        ArrayList<DemoPoseSource> poseSources = new ArrayList<DemoPoseSource>();
        Map<String, DemoPoseSource> poseSourceByPlayer =
                new HashMap<String, DemoPoseSource>();
        List<BattleArenaPlayerState> initialStates = simulationServer.initialStates();
        for (int i = 0; i < initialStates.size(); i++) {
            BattleArenaPlayerState playerState = initialStates.get(i);
            DemoPoseSource poseSource = createCharacter(
                    scene,
                    geometry,
                    texture,
                    gpuBoneSsboManager.boneCount(),
                    i,
                    playerState);
            gpuBoneSsboManager.registerSkeleton(poseSource);
            poseSources.add(poseSource);
            poseSourceByPlayer.put(playerState.playerId, poseSource);
        }
        combatController.register(scene, definition, initialStates);
        combatController.updateStates(initialStates);

        scene.animations.add(new Animation() {
            private boolean staticPoseUploaded;
            private final BattleArenaCharacterControlState controls =
                    new BattleArenaCharacterControlState();
            private float tickAccumulator;

            @Override
            public void animate(float deltaSeconds) {
                submitLocalInput(simulationServer, actions, movementPointer);
                if (!ANIMATE_CHARACTERS && staticPoseUploaded) {
                    scene.renderer.recordBoneCalculationNanos(0L);
                    updateCamera(scene.renderer.camera, simulationServer.stateForPlayer(LOCAL_PLAYER_ID));
                    return;
                }
                if (ANIMATE_CHARACTERS) {
                    tickAccumulator = Math.min(
                            tickAccumulator + Math.max(0f, deltaSeconds),
                            MAX_ACCUMULATED_SECONDS);
                    while (tickAccumulator >= simulationServer.tickSeconds()) {
                        simulationServer.tick();
                        tickAccumulator -= simulationServer.tickSeconds();
                    }
                } else if (!staticPoseUploaded) {
                    simulationServer.tick();
                }
                BattleArenaSimulationSnapshot snapshot = simulationServer.snapshot();
                combatController.updateStates(snapshot.players);
                for (BattleArenaPlayerState playerState : snapshot.players) {
                    DemoPoseSource poseSource = poseSourceByPlayer.get(playerState.playerId);
                    if (poseSource != null) {
                        poseSource.apply(playerState);
                        gpuBoneSsboManager.syncPose(poseSource);
                    }
                }
                long boneStartNanos = System.nanoTime();
                gpuBoneSsboManager.dispatchAll(graphicsDevice);
                scene.renderer.recordBoneCalculationNanos(System.nanoTime() - boneStartNanos);
                staticPoseUploaded = true;
                updateCamera(scene.renderer.camera, snapshot.stateForPlayer(LOCAL_PLAYER_ID));
            }

            private void submitLocalInput(BattleArenaPlayerStateProvider provider,
                                          ActionInput actions,
                                          PointerState movementPointer) {
                controls.capturePlayerInput(actions, movementPointer);
                BattleArenaPlayerInput input = new BattleArenaPlayerInput();
                input.moveZ = controls.forwardInput;
                input.turn = controls.turnInput;
                input.run = controls.runDown;
                input.jumpPressed = controls.jumpPressed;
                input.punchPressed = controls.punchPressed;
                input.kickPressed = controls.kickPressed;
                input.castPressed = controls.castFireballPressed || controls.castMudWallPressed;
                input.stepLeftPressed = controls.stepLeftPressed;
                input.stepRightPressed = controls.stepRightPressed;
                provider.submitInput(LOCAL_PLAYER_ID, provider.currentTick() + 1, input);
            }

        });

        updateCamera(scene.renderer.camera, simulationServer.stateForPlayer(LOCAL_PLAYER_ID));
        BattleArenaDemoLoader.log("GPU skinning demo loaded characters=" + poseSources.size()
                + " bonesPerCharacter=" + gpuBoneSsboManager.boneCount()
                + " gpuInstances=" + gpuBoneSsboManager.instanceCount());
    }

    private BattleArenaSimulationServer createSimulationServer(float[][] spawnPositions,
                                                              BattleArenaGpuBoneSsboManager gpuBoneSsboManager) {
        BattleArenaSimulationServer simulationServer = new BattleArenaSimulationServer(
                spawnPositions,
                gpuBoneSsboManager.createAnimationTimings(BattleArenaLocalPlayerStateServer.TICK_SECONDS));
        simulationServer.setNpcController(NPC_PLAYER_ID, new BattleArenaChaseNpcController(LOCAL_PLAYER_ID));
        return simulationServer;
    }

    private DemoPoseSource createCharacter(Scene scene,
                                           WeightedGeometry geometry,
                                           int texture,
                                           int boneCount,
                                           int characterIndex,
                                           BattleArenaPlayerState playerState) {
        int boneBufferStartIndex = scene.reserveSkeleton(boneCount);
        Weighted_GameObject meshObject = null;
        if (RENDER_CHARACTERS) {
            meshObject = new Weighted_GameObject(geometry, texture);
            meshObject.name = "BattleArena_GpuSkinning_Probe_" + characterIndex;
            meshObject.shininess = 18f;
            meshObject.ambientlight_multiplier = 1.2f;
            meshObject.setScale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
            meshObject.boneBufferStartIndex = boneBufferStartIndex;
            scene.addGameObject(meshObject);
        }
        DemoPoseSource poseSource = new DemoPoseSource(
                playerState.playerId,
                boneBufferStartIndex,
                boneCount,
                meshObject);
        poseSource.apply(playerState);
        return poseSource;
    }

    private float[][] resolveCharacterPositions() {
        int requestedCount = Integer.getInteger("battleArena.gpuSkinningDemo.characters", -1);
        if (requestedCount == 1) {
            return SINGLE_CHARACTER_POSITION;
        }
        return CHARACTER_POSITIONS;
    }

    private void updateCamera(Camera camera, BattleArenaPlayerState followedPlayer) {
        float focusX = followedPlayer != null ? followedPlayer.x : 0f;
        float focusY = followedPlayer != null ? followedPlayer.y : 0f;
        float focusZ = followedPlayer != null ? followedPlayer.z : 0f;
        Vector3 focus = new Vector3(focusX, focusY + CAMERA_FOCUS_HEIGHT, focusZ);
        float horizontalDistance = (float) Math.cos(cameraPitch) * CAMERA_DISTANCE;
        float verticalOffset = (float) Math.sin(cameraPitch) * CAMERA_DISTANCE;
        camera.lookAt(
                new Vector3(
                        focus.x - ((float) Math.sin(cameraYaw) * horizontalDistance),
                        focus.y + CAMERA_HEIGHT + verticalOffset,
                        focus.z - ((float) Math.cos(cameraYaw) * horizontalDistance)),
                focus,
                new Vector3(0f, 1f, 0f));
    }

    private void handlePointerLook(ActionInput actions, PointerState pointer) {
        if (!actions.button(BattleArenaActions.LOOK).isDown()) {
            return;
        }
        if (pointer == null || (pointer.getDeltaX() == 0f && pointer.getDeltaY() == 0f)) {
            return;
        }
        cameraYaw += pointer.getDeltaX() * LOOK_SENSITIVITY;
        cameraPitch = clamp(
                cameraPitch + pointer.getDeltaY() * LOOK_SENSITIVITY,
                MIN_CAMERA_PITCH,
                MAX_CAMERA_PITCH);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class DemoPoseSource implements BattleArenaGpuSkeletonPoseSource {
        private final String playerId;
        private final int boneBufferStartIndex;
        private final int boneCount;
        private final Weighted_GameObject meshObject;
        private String animationKey = BattleArenaCharacterController.ANIM_IDLE;
        private float animationFrame;

        private DemoPoseSource(String playerId,
                               int boneBufferStartIndex,
                               int boneCount,
                               Weighted_GameObject meshObject) {
            this.playerId = playerId;
            this.boneBufferStartIndex = boneBufferStartIndex;
            this.boneCount = boneCount;
            this.meshObject = meshObject;
        }

        void apply(BattleArenaPlayerState state) {
            if (state == null) {
                return;
            }
            animationKey = state.animationKey != null
                    ? state.animationKey
                    : BattleArenaCharacterController.ANIM_IDLE;
            animationFrame = state.animationFrame;
            if (meshObject != null) {
                meshObject.setPosition(state.x, state.y, state.z);
                meshObject.setRotation(0f, state.headingDegrees, 0f);
            }
        }

        @Override
        public String currentGpuAnimationKey() {
            return animationKey;
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
