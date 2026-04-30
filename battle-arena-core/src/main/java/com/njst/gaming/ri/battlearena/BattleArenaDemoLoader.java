package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.ParallelKeyframeAnimator;
import com.njst.gaming.Camera;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.audio.AudioBufferHandle;
import com.njst.gaming.audio.AudioSourceHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.ri.battlearena.controls.BattleArenaActions;
import com.njst.gaming.ri.battlearena.networking.BattleArenaTcpControlClient;

public class BattleArenaDemoLoader implements Scene.SceneLoader {
    public static final String LOCAL_PLAYER_ANDROID = "android";
    public static final String LOCAL_PLAYER_DESKTOP = "desktop";
    public static final int DEFAULT_TCP_CONTROL_PORT = 7777;

    private static final String AUDIO_TEST_FILE = "audio/battle_arena_test.wav";
    private static final float CAMERA_DISTANCE = 6.5f;
    private static final float CAMERA_HEIGHT = 1.2f;
    private static final float LOOK_SENSITIVITY = 0.0125f;
    private static final float MIN_PITCH = -0.8f;
    private static final float MAX_PITCH = 0.45f;
    private static final float PLAYER_FOCUS_HEIGHT = 1.6f;
    private static final boolean DISABLE_ACTIVE_ANIMATIONS_FOR_PROFILING = false;
    private static final String LOG_PREFIX = "[BattleArena] ";

    private final BattleArenaEnvironmentLoader environmentLoader = new BattleArenaEnvironmentLoader();
    private final BattleArenaCharacterManager characterManager = new BattleArenaCharacterManager();
    private BattleArenaTcpControlClient tcpControlClient;
    private Scene loadedScene;
    private float cameraYaw = 0f;
    private float cameraPitch = -0.18f;
    private AudioBufferHandle audioTestBuffer;
    private AudioSourceHandle audioTestSource;
    private boolean audioAvailable;
    private final String localPlayer;
    private final String tcpControlHost;
    private final int tcpControlPort;

    public BattleArenaDemoLoader() {
        this(
                System.getProperty("battleArena.localPlayer", LOCAL_PLAYER_DESKTOP),
                System.getProperty("battleArena.remoteHost", BattleArenaTcpControlClient.DEFAULT_HOST),
                readPortProperty("battleArena.remotePort", DEFAULT_TCP_CONTROL_PORT));
    }

    public BattleArenaDemoLoader(String localPlayer) {
        this(
                localPlayer,
                System.getProperty("battleArena.remoteHost", BattleArenaTcpControlClient.DEFAULT_HOST),
                readPortProperty("battleArena.remotePort", DEFAULT_TCP_CONTROL_PORT));
    }

    public BattleArenaDemoLoader(String localPlayer, String tcpControlHost, int tcpControlPort) {
        this.localPlayer = LOCAL_PLAYER_ANDROID.equals(localPlayer)
                ? LOCAL_PLAYER_ANDROID
                : LOCAL_PLAYER_DESKTOP;
        this.tcpControlHost = tcpControlHost == null || tcpControlHost.trim().isEmpty()
                ? BattleArenaTcpControlClient.DEFAULT_HOST
                : tcpControlHost.trim();
        this.tcpControlPort = tcpControlPort > 0 ? tcpControlPort : DEFAULT_TCP_CONTROL_PORT;
    }

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        loadedScene = scene;
        log("load start root=" + com.njst.gaming.data.rootDirectory
                + " graphicsDevice=" + graphicsDevice.getClass().getName());

        characterManager.reset();
        characterManager.setTerrainSampler(environmentLoader::sampleTerrainHeight);
        tcpControlClient = new BattleArenaTcpControlClient(tcpControlHost, tcpControlPort);
        cameraYaw = 0f;
        cameraPitch = -0.18f;
        audioTestBuffer = null;
        audioTestSource = null;
        audioAvailable = false;

        environmentLoader.load(scene, graphicsDevice);
        initAudioSmokeTest(scene);
        try {
            characterManager.loadCharacterAssets(scene, graphicsDevice);
        } catch (Exception e) {
            log("ERROR in loadCharacterAssets: " + e.getMessage());
            throw e;
        }
        log("character assets ready; waiting for tcp player assignment from "
                + tcpControlHost + ":" + tcpControlPort);
        scene.getCollisionWorld().addListener(event ->
                characterManager.handleHitboxCollision(loadedScene, environmentLoader::sampleTerrainHeight, event));

        ActionInput actions = scene.actionInput;
        PointerState movementPointer = scene.pointer(BattleArenaActions.MOVE_POINTER);
        scene.registerPointerInput(BattleArenaActions.LOOK_POINTER,
                (activeScene, pointer) -> handlePointerLook(actions, pointer));

        scene.animations.add(new Animation() {
            @Override
            public void animate(float deltaSeconds) {
                if (tcpControlClient != null) {
                    tcpControlClient.update(deltaSeconds);
                    characterManager.syncTcpCharacters(scene, graphicsDevice, tcpControlClient);
                }
                if (actions.button(BattleArenaActions.SNAP).pressed()) {
                    characterManager.toggleActiveCharacter();
                }
                if (actions.button(BattleArenaActions.TOGGLE_HITBOXES).pressed()) {
                    characterManager.toggleHitboxDebug();
                }
                if (actions.button(BattleArenaActions.PUNCH).pressed()
                        || actions.button(BattleArenaActions.KICK).pressed()
                        || actions.button(BattleArenaActions.FIREBALL).pressed()
                        || actions.button(BattleArenaActions.BURST).pressed()) {
                    playAudioSmokeTest(scene, 0.55f);
                }

                characterManager.updateCharacters(
                        scene,
                        tcpControlClient,
                        actions,
                        movementPointer,
                        environmentLoader::sampleTerrainHeight,
                        deltaSeconds);
                logFrameSnapshot(scene);
                if (!DISABLE_ACTIVE_ANIMATIONS_FOR_PROFILING) {
                    ParallelKeyframeAnimator.animateSkeletons(
                            characterManager.collectActiveSkeletonAnimations(),
                            deltaSeconds);
                }
                characterManager.syncRigs();
                updateCamera(scene.renderer.camera);
            }
        });

        updateCamera(scene.renderer.camera);
        playAudioSmokeTest(scene, 0.35f);
        log("load complete; first tcp connection assignment will spawn the controlled character");
    }

    private void initAudioSmokeTest(Scene scene) {
        try {
            audioTestBuffer = scene.audioDevice.loadSound(AUDIO_TEST_FILE);
            audioTestSource = scene.audioDevice.createSource(audioTestBuffer);
            audioAvailable = true;
            log("audio smoke test loaded=" + AUDIO_TEST_FILE);
        } catch (RuntimeException e) {
            audioAvailable = false;
            log("audio smoke test unavailable: " + e.getMessage());
        }
    }

    private void playAudioSmokeTest(Scene scene, float gain) {
        if (!audioAvailable || audioTestSource == null) {
            return;
        }
        try {
            audioTestSource.stop();
            audioTestSource.setGain(gain);
            audioTestSource.play();
        } catch (RuntimeException e) {
            audioAvailable = false;
            log("audio smoke test disabled after playback error: " + e.getMessage());
        }
    }

    private void handlePointerLook(ActionInput actions, PointerState pointer) {
        if (!actions.button(BattleArenaActions.LOOK).isDown()) {
            return;
        }
        if (pointer.getDeltaX() == 0f && pointer.getDeltaY() == 0f) {
            return;
        }
        cameraYaw += pointer.getDeltaX() * LOOK_SENSITIVITY;
        cameraPitch = clamp(cameraPitch + (pointer.getDeltaY() * LOOK_SENSITIVITY), MIN_PITCH, MAX_PITCH);
    }

    private void updateCamera(Camera camera) {
        BattleArenaControlledCharacter activeCharacter = characterManager.getActiveCharacter();
        if (activeCharacter == null) {
            return;
        }
        Vector3 playerPosition = activeCharacter.runtime.getPosition();
        Vector3 focus = new Vector3(playerPosition.x, playerPosition.y + PLAYER_FOCUS_HEIGHT, playerPosition.z);
        float horizontalDistance = (float) Math.cos(cameraPitch) * CAMERA_DISTANCE;
        float verticalOffset = (float) Math.sin(cameraPitch) * CAMERA_DISTANCE;
        Vector3 cameraPosition = new Vector3(
                focus.x - ((float) Math.sin(cameraYaw) * horizontalDistance),
                focus.y + CAMERA_HEIGHT + verticalOffset,
                focus.z - ((float) Math.cos(cameraYaw) * horizontalDistance));
        camera.lookAt(cameraPosition, focus, new Vector3(0f, 1f, 0f));
    }

    private boolean frameSnapshotLogged = false;

    private void logFrameSnapshot(Scene scene) {
        if (frameSnapshotLogged || scene == null) {
            return;
        }
        frameSnapshotLogged = true;
        log("first frame sceneObjects=" + scene.objects.size()
                + " animations=" + scene.animations.size()
                + " activeCharacter=" + (characterManager.getActiveCharacter() != null
                ? characterManager.getActiveCharacter().runtime.meshObject.name
                : "none"));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }

    private static int readPortProperty(String name, int fallback) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
