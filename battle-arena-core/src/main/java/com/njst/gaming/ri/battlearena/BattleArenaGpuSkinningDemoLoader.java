package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Camera;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.ri.battlearena.controls.BattleArenaActions;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterControlState;
import com.njst.gaming.ri.battlearena.networking.BattleArenaTcpSimulationClient;
import com.njst.gaming.ri.battlearena.gameobjects.BattleArenaAnimatedAtlasGameObject;
import com.njst.gaming.ri.battlearena.gameobjects.BattleArenaPlayerHealthBarGameObject;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BattleArenaGpuSkinningDemoLoader implements Scene.SceneLoader {
    private static final String CHARACTER_DEFINITION_FILE = "battle_arena/defeated.character.json";
    private static final String FIRE_ATLAS_TEXTURE_FILE = "fire-atlas.png";
    private static final int FIRE_ATLAS_COLUMNS = 4;
    private static final int FIRE_ATLAS_ROWS = 8;
    private static final String FIRE_ATLAS_LAYERS_PROPERTY = "battleArena.fireAtlasLayers";
    private static final float PLAYER_SCALE = 1f;
    private static final boolean RENDER_CHARACTERS = true;
    private static final boolean ANIMATE_CHARACTERS = true;
    private static final String LOCAL_PLAYER_ID = "player_0";
    private static final String NPC_PLAYER_ID = "player_1";
    private static final String NETWORK_SIMULATION_PROPERTY = "battleArena.networkSimulation";
    private static final String SIMULATION_HOST_PROPERTY = "battleArena.simulationHost";
    private static final String SIMULATION_PORT_PROPERTY = "battleArena.simulationPort";
    private static final float CAMERA_DISTANCE = 7.5f;
    private static final float CAMERA_HEIGHT = 2.4f;
    private static final float CAMERA_FOCUS_HEIGHT = 1.1f;
    private static final float HEALTH_BAR_WIDTH = 0.9f;
    private static final float HEALTH_BAR_HEIGHT = 0.12f;
    private static final float HEALTH_BAR_VERTICAL_OFFSET = 2.15f;
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
        int fireAtlasTexture = graphicsDevice.loadTexture(
                BattleArenaEnvironmentLoader.resolveResourcePath(FIRE_ATLAS_TEXTURE_FILE));

        BattleArenaGpuBoneSsboManager gpuBoneSsboManager =
                new BattleArenaGpuBoneSsboManager(graphicsDevice);
        boolean networkSimulation = Boolean.getBoolean(NETWORK_SIMULATION_PROPERTY);
        BattleArenaSimulationServer simulationServer = networkSimulation
                ? null
                : createSimulationServer(resolveCharacterPositions(), gpuBoneSsboManager);
        BattleArenaTcpSimulationClient simulationClient = networkSimulation
                ? createSimulationClient()
                : null;
        BattleArenaSnapshotPlayerStatusSource statusSource = new BattleArenaSnapshotPlayerStatusSource();
        ArrayList<DemoPoseSource> poseSources = new ArrayList<DemoPoseSource>();
        Map<String, DemoPoseSource> poseSourceByPlayer =
                new HashMap<String, DemoPoseSource>();
        List<BattleArenaPlayerState> initialStates = simulationServer != null
                ? simulationServer.initialStates()
                : createInitialStates(resolveCharacterPositions());
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
            scene.addGameObject(new BattleArenaPlayerHealthBarGameObject(
                    statusSource,
                    scene.renderer.camera,
                    playerState.playerId,
                    "BattleArena_" + playerState.playerId + "_HealthBar",
                    HEALTH_BAR_WIDTH,
                    HEALTH_BAR_HEIGHT,
                    HEALTH_BAR_VERTICAL_OFFSET));
        }
        statusSource.update(new BattleArenaSimulationSnapshot(0,
                BattleArenaLocalPlayerStateServer.TICK_SECONDS,
                initialStates));

        scene.animations.add(new Animation() {
            private boolean staticPoseUploaded;
            private final BattleArenaCharacterControlState controls =
                    new BattleArenaCharacterControlState();
            private final Map<Integer, GameObject> guRenderObjects = new HashMap<Integer, GameObject>();
            private float tickAccumulator;

            @Override
            public void animate(float deltaSeconds) {
                if (simulationClient != null) {
                    animateNetworkSimulation(deltaSeconds);
                    return;
                }
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
                statusSource.update(snapshot);
                renderSnapshot(snapshot, deltaSeconds);
            }

            private void animateNetworkSimulation(float deltaSeconds) {
                simulationClient.update(deltaSeconds);
                submitNetworkInput(simulationClient, actions, movementPointer);
                BattleArenaSimulationSnapshot snapshot = simulationClient.latestSnapshot();
                if (snapshot == null) {
                    scene.renderer.recordBoneCalculationNanos(0L);
                    updateCamera(scene.renderer.camera, findPlayer(initialStates, LOCAL_PLAYER_ID));
                    return;
                }
                renderSnapshot(snapshot, deltaSeconds);
            }

            private void renderSnapshot(BattleArenaSimulationSnapshot snapshot, float deltaSeconds) {
                statusSource.update(snapshot);
                syncGuObjects(snapshot, deltaSeconds);
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
                updateCamera(scene.renderer.camera, snapshot.stateForPlayer(cameraPlayerId()));
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
                input.guLoadoutKey = resolveGuLoadoutKey(controls);
                input.stepLeftPressed = controls.stepLeftPressed;
                input.stepRightPressed = controls.stepRightPressed;
                provider.submitInput(LOCAL_PLAYER_ID, provider.currentTick() + 1, input);
            }

            private void submitNetworkInput(BattleArenaTcpSimulationClient client,
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
                input.guLoadoutKey = resolveGuLoadoutKey(controls);
                input.stepLeftPressed = controls.stepLeftPressed;
                input.stepRightPressed = controls.stepRightPressed;
                BattleArenaSimulationSnapshot snapshot = client.latestSnapshot();
                int inputTick = snapshot != null ? snapshot.tick + 1 : 0;
                client.sendInput(inputTick, input);
            }

            private String cameraPlayerId() {
                if (simulationClient == null || simulationClient.assignedPlayer() == null) {
                    return LOCAL_PLAYER_ID;
                }
                return simulationClient.assignedPlayer();
            }

            private void syncGuObjects(BattleArenaSimulationSnapshot snapshot, float deltaSeconds) {
                Set<Integer> liveIds = new HashSet<Integer>();
                for (BattleArenaGuObjectState guObject : snapshot.guObjects) {
                    if (guObject == null) {
                        continue;
                    }
                    liveIds.add(guObject.id);
                    GameObject renderObject = guRenderObjects.get(guObject.id);
                    if (renderObject == null) {
                        renderObject = createGuRenderObject(scene, guObject, fireAtlasTexture);
                        guRenderObjects.put(guObject.id, renderObject);
                    }
                    renderObject.setPosition(guObject.x, guObject.y, guObject.z);
                    renderObject.setScale(
                            Math.max(0.02f, guObject.halfX * 2f),
                            Math.max(0.02f, guObject.halfY * 2f),
                            Math.max(0.02f, guObject.halfZ * 2f));
                    renderObject.ambientlight_multiplier = materialBrightness(guObject);
                    renderObject.shininess = materialShininess(guObject);
                    if (renderObject instanceof BattleArenaAnimatedAtlasGameObject) {
                        ((BattleArenaAnimatedAtlasGameObject) renderObject).updateVisual(
                                deltaSeconds,
                                scene.renderer.camera);
                    } else {
                        renderObject.setRotation(0f, guObject.headingDegrees, 0f);
                    }
                }
                ArrayList<Integer> removedIds = new ArrayList<Integer>();
                for (Integer objectId : guRenderObjects.keySet()) {
                    if (!liveIds.contains(objectId)) {
                        removedIds.add(objectId);
                    }
                }
                for (Integer removedId : removedIds) {
                    GameObject object = guRenderObjects.remove(removedId);
                    if (object != null) {
                        scene.removeGameObject(object);
                        object.cleanup();
                    }
                }
            }
        });

        updateCamera(scene.renderer.camera, findPlayer(initialStates, LOCAL_PLAYER_ID));
        BattleArenaDemoLoader.log("GPU skinning demo loaded characters=" + poseSources.size()
                + " bonesPerCharacter=" + gpuBoneSsboManager.boneCount()
                + " gpuInstances=" + gpuBoneSsboManager.instanceCount()
                + " networkSimulation=" + networkSimulation);
    }

    private BattleArenaSimulationServer createSimulationServer(float[][] spawnPositions,
                                                              BattleArenaGpuBoneSsboManager gpuBoneSsboManager) {
        BattleArenaSimulationServer simulationServer = new BattleArenaSimulationServer(
                spawnPositions,
                gpuBoneSsboManager.createAnimationTimings(BattleArenaLocalPlayerStateServer.TICK_SECONDS));
        simulationServer.setNpcController(NPC_PLAYER_ID, new BattleArenaChaseNpcController(LOCAL_PLAYER_ID));
        return simulationServer;
    }

    private BattleArenaTcpSimulationClient createSimulationClient() {
        return new BattleArenaTcpSimulationClient(
                System.getProperty(SIMULATION_HOST_PROPERTY, BattleArenaTcpSimulationClient.DEFAULT_HOST),
                readIntProperty(SIMULATION_PORT_PROPERTY, BattleArenaTcpSimulationClient.DEFAULT_PORT));
    }

    private String resolveGuLoadoutKey(BattleArenaCharacterControlState controls) {
        if (controls.castMudWallPressed) {
            return BattleArenaGuObjectSystem.LOADOUT_EARTH_WALL;
        }
        if (controls.castFireballPressed) {
            return BattleArenaGuObjectSystem.LOADOUT_FIRE_WIND;
        }
        return null;
    }

    private GameObject createGuRenderObject(Scene scene, BattleArenaGuObjectState guObject, int fireAtlasTexture) {
        GameObject object;
        if (isFireAtlasMaterial(guObject)) {
            object = new BattleArenaAnimatedAtlasGameObject(
                    fireAtlasTexture,
                    FIRE_ATLAS_COLUMNS,
                    FIRE_ATLAS_ROWS,
                    18f,
                    fireAtlasLayerCount());
        } else {
            object = new GameObject(new CubeGeometry(), 0);
        }
        object.name = "BattleArena_GuObject_" + guObject.id + "_" + guObject.material;
        object.castsShadows = false;
        object.ambientlight_multiplier = materialBrightness(guObject);
        object.shininess = materialShininess(guObject);
        object.setPosition(guObject.x, guObject.y, guObject.z);
        object.setRotation(0f, guObject.headingDegrees, 0f);
        object.setScale(
                Math.max(0.02f, guObject.halfX * 2f),
                Math.max(0.02f, guObject.halfY * 2f),
                Math.max(0.02f, guObject.halfZ * 2f));
        scene.addGameObject(object);
        return object;
    }

    private boolean isFireAtlasMaterial(BattleArenaGuObjectState guObject) {
        return guObject != null
                && (BattleArenaGuMaterial.HOT_GAS.key.equals(guObject.material)
                || BattleArenaGuMaterial.MOLTEN_EARTH.key.equals(guObject.material));
    }

    private int fireAtlasLayerCount() {
        return Math.max(1, Integer.getInteger(FIRE_ATLAS_LAYERS_PROPERTY, 1));
    }

    private float materialBrightness(BattleArenaGuObjectState guObject) {
        if (guObject == null) {
            return 1f;
        }
        if (BattleArenaGuMaterial.HOT_GAS.key.equals(guObject.material)
                || BattleArenaGuMaterial.MOLTEN_EARTH.key.equals(guObject.material)) {
            return 8f + Math.max(0f, guObject.temperature - 300f) / 240f;
        }
        if (BattleArenaGuMaterial.ICE.key.equals(guObject.material)) {
            return 1.7f;
        }
        if (BattleArenaGuMaterial.WATER.key.equals(guObject.material)) {
            return 1.35f;
        }
        return 1.05f;
    }

    private float materialShininess(BattleArenaGuObjectState guObject) {
        if (guObject == null) {
            return 8f;
        }
        if (BattleArenaGuMaterial.WATER.key.equals(guObject.material)
                || BattleArenaGuMaterial.ICE.key.equals(guObject.material)) {
            return 42f;
        }
        if (BattleArenaGuMaterial.MOLTEN_EARTH.key.equals(guObject.material)) {
            return 4f;
        }
        return 10f;
    }

    private List<BattleArenaPlayerState> createInitialStates(float[][] spawnPositions) {
        return new BattleArenaLocalPlayerStateServer(spawnPositions).initialStates();
    }

    private BattleArenaPlayerState findPlayer(List<BattleArenaPlayerState> states, String playerId) {
        if (states == null || playerId == null) {
            return null;
        }
        for (BattleArenaPlayerState state : states) {
            if (state != null && playerId.equals(state.playerId)) {
                return state;
            }
        }
        return null;
    }

    private static int readIntProperty(String property, int fallback) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
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
