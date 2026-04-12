package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Camera;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.collision.CollisionEventType;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.objects.*;
import com.njst.gaming.objects.Weighted_GameObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BattleArenaDemoLoader implements Scene.SceneLoader {
    private static final String SKYBOX_FILE = "desertstorm.jpg";
    private static final String GROUND_FILE = "stone.jpeg";
    private static final String CHARACTER_DEFINITION_FILE = "battle_arena/defeated.character.json";
    private static final int GROUND_SIZE = 96;
    private static final float CAMERA_DISTANCE = 6.5f;
    private static final float CAMERA_HEIGHT = 1.2f;
    private static final float LOOK_SENSITIVITY = 0.0125f;
    private static final float MIN_PITCH = -0.8f;
    private static final float MAX_PITCH = 0.45f;
    private static final float PLAYER_SCALE = 1f;
    private static final float PLAYER_FOCUS_HEIGHT = 1.6f;
    private static final float SECOND_CHARACTER_START_X = 2.5f;
    private static final String LOG_PREFIX = "[BattleArena] ";

    private TerrainGeometry terrainGeometry;
    private Vector3 terrainOrigin;
    private final List<GameObject> playerMeshes = new ArrayList<>();
    private final List<BattleArenaHitboxDebugGameObject> debugHitboxes = new ArrayList<>();
    private final BattleArenaCharacterController characterController = new BattleArenaCharacterController();
    private final BattleArenaCharacterController secondaryCharacterController = new BattleArenaCharacterController();
    private final BattleArenaCharacterAssembler characterAssembler = new BattleArenaCharacterAssembler();
    private final BattleArenaCharacterDefinitionLoader characterDefinitionLoader = new BattleArenaCharacterDefinitionLoader();
    private final BattleArenaCharacterControlState aiControls = new BattleArenaCharacterControlState();
    private final BattleArenaCharacterBrain secondaryCharacterAi = new BattleArenaSimpleChaseAi();
    private BattleArenaCharacterRuntime primaryCharacter;
    private BattleArenaCharacterRuntime secondaryCharacter;
    private BattleArenaCharacterRuntime activeCharacter;
    private float cameraYaw = 0f;
    private float cameraPitch = -0.18f;
    private boolean debugHitboxesVisible = false;
    private final ArrayList<KeyframeAnimation> activeAnimations = new ArrayList<>();

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        log("load start root=" + com.njst.gaming.data.rootDirectory);
        playerMeshes.clear();
        debugHitboxes.clear();
        activeAnimations.clear();
        primaryCharacter = null;
        secondaryCharacter = null;
        characterController.reset();
        secondaryCharacterController.reset();
        activeCharacter = null;
        aiControls.clear();
        cameraYaw = 0f;
        cameraPitch = -0.18f;
        debugHitboxesVisible = false;

        String skyboxPath = resolveTexturePath(SKYBOX_FILE);
        String groundPath = resolveTexturePath(GROUND_FILE);
        log("loading textures skybox=" + skyboxPath + " ground=" + groundPath);
        int skyboxTexture = graphicsDevice.loadTexture(skyboxPath);
        int groundTexture = graphicsDevice.loadTexture(groundPath);
        log("loaded textures skyboxId=" + skyboxTexture + " groundId=" + groundTexture);

        GameObject skybox = new GameObject(new SphereGeometry(1f, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5f;
        skybox.shininess = 1f;
        skybox.setScale(100f, 100f, 100f);
        skybox.setPosition(0f, 0f, 0f);
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        terrainGeometry = new TerrainGeometry(GROUND_SIZE, GROUND_SIZE, new float[GROUND_SIZE][GROUND_SIZE]);
        terrainOrigin = new Vector3(-GROUND_SIZE * 0.5f, -0.75f, -GROUND_SIZE * 0.5f);
        characterController.setTerrainHeightSampler(this::sampleTerrainHeight);
        secondaryCharacterController.setTerrainHeightSampler(this::sampleTerrainHeight);
        GameObject ground = new GameObject(terrainGeometry, groundTexture);
        ground.ambientlight_multiplier = 1.35f;
        ground.shininess = 3f;
        ground.setPosition(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
       scene.addGameObject(ground);

        try {
            loadPlayer(scene, graphicsDevice);
        } catch (Exception e) {
            log("ERROR in loadPlayer: " + e.getMessage());
            throw e;
        }
        log("player meshes loaded=" + playerMeshes.size() + " bones=" + primaryCharacter.bones.size());
        primaryCharacter.syncRig();
        registerCharacterHitboxes(scene, primaryCharacter);
        registerCharacterHitboxes(scene, secondaryCharacter);
        scene.getCollisionWorld().addListener(this::handleHitboxCollision);

        ActionInput actions = scene.actionInput;
        PointerState movementPointer = scene.pointer(BattleArenaActions.MOVE_POINTER);
        scene.registerPointerInput(BattleArenaActions.LOOK_POINTER,
                (activeScene, pointer) -> handlePointerLook(actions, pointer));

        scene.animations.add(new Animation() {
            @Override
            public void animate(float deltaSeconds) {
                if (actions.button(BattleArenaActions.SNAP).pressed()) {
                    toggleActiveCharacter();
                }
                if (actions.button(BattleArenaActions.TOGGLE_HITBOXES).pressed()) {
                    toggleHitboxDebug();
                }
                primaryCharacter.controller.update(actions, movementPointer, scene.speed);
                secondaryCharacterAi.update(secondaryCharacter, primaryCharacter, aiControls, deltaSeconds);
                secondaryCharacter.controller.update(aiControls, scene.speed);
                updateSideStepFacing(primaryCharacter, secondaryCharacter);
                updateSideStepFacing(secondaryCharacter, primaryCharacter);
                primaryCharacter.applyHeadingToRig();
                secondaryCharacter.applyHeadingToRig();
                // Drive battle arena keyframes with elapsed time instead of frame count.
                for (KeyframeAnimation anim : activeAnimations) {
                    anim.animate(deltaSeconds);
                }
                primaryCharacter.rootBone.update();
                secondaryCharacter.rootBone.update();
                primaryCharacter.syncRig();
                secondaryCharacter.syncRig();
                updateCamera(scene.renderer.camera);
            }
        });

        updateCamera(scene.renderer.camera);
        Vector3 playerPosition = activeCharacter.getPosition();
        log("load complete playerPosition=" + playerPosition.x + "," + playerPosition.y + "," + playerPosition.z);
        // if(rootBone.parent!=null){
        	// log("Root Bone has parent");
        // }
    }

    private void loadPlayer(Scene scene, GraphicsDevice graphicsDevice) {
        BattleArenaCharacterDefinition definition = characterDefinitionLoader.load(graphicsDevice, CHARACTER_DEFINITION_FILE);
        WeightedGeometry weightedGeometry = characterAssembler.loadWeightedGeometry(graphicsDevice, definition.model.mesh);
        int vertexCount = weightedGeometry.getVertices() != null ? weightedGeometry.getVertices().length / 3 : 0;
        int normalCount = weightedGeometry.getNormals() != null ? weightedGeometry.getNormals().length / 3 : 0;
        int uvCount = weightedGeometry.getTextureCoordinates() != null ? weightedGeometry.getTextureCoordinates().length / 2 : 0;
        int indexCount = weightedGeometry.getIndices() != null ? weightedGeometry.getIndices().length : 0;
        int weightCount = weightedGeometry.getWeightss() != null ? weightedGeometry.getWeightss().length / 4 : 0;
        int boneIdCount = weightedGeometry.getBoness() != null ? weightedGeometry.getBoness().length / 4 : 0;
        log("parsed model vertices=" + vertexCount
                + " normals=" + normalCount
                + " uvs=" + uvCount
                + " indices=" + indexCount
                + " weights=" + weightCount
                + " boneIds=" + boneIdCount);

        activeAnimations.clear();
        BattleArenaCharacterAssembly primaryAssembly = characterAssembler.assembleCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition,
                "BattleArenaPlayerMesh",
                PLAYER_SCALE,
                activeAnimations);
        primaryCharacter = new BattleArenaCharacterRuntime(characterController, primaryAssembly, definition);
        activeCharacter = primaryCharacter;
        log("wired animation count total=" + activeAnimations.size()
                + " idle=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_IDLE).size()
                + " walk=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_WALK).size()
                + " walkBackward=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_WALK_BACKWARD).size()
                + " run=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_RUN).size()
                + " jump=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_JUMP).size()
                + " punch=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_PUNCH).size()
                + " hit=" + primaryCharacter.animationSet(BattleArenaCharacterController.ANIM_TAKE_HIT).size());

        log("loaded bones runtimeBones=" + primaryCharacter.bones.size()
                + " root=" + primaryCharacter.rootBone.name);

        Bone_object boneobj=new Bone_object(new CubeGeometry(), loadCharacterTexture(graphicsDevice, definition));
        boneobj.bone=primaryCharacter.rootBone;
        boneobj.scale=new float[]{0.1f,0.1f,0.1f};
        // scene.addGameObject(boneobj);
        playerMeshes.add(primaryCharacter.meshObject);
        log("spawned weighted mesh name=" + primaryCharacter.meshObject.name + " scale=" + PLAYER_SCALE);

        BattleArenaCharacterAssembly secondaryAssembly = characterAssembler.assembleCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition,
                "BattleArenaSecondCharacter",
                PLAYER_SCALE,
                activeAnimations);
        secondaryCharacter = new BattleArenaCharacterRuntime(secondaryCharacterController, secondaryAssembly, definition);
        playerMeshes.add(secondaryCharacter.meshObject);
        log("spawned second character name=" + secondaryCharacter.meshObject.name + " bones=" + secondaryCharacter.bones.size());
        secondaryCharacterController.setPlayerPosition(SECOND_CHARACTER_START_X, 0f, 0f);
        secondaryCharacter.syncRig();
    }

    private void registerCharacterHitboxes(Scene scene, BattleArenaCharacterRuntime character) {
        for (Collider collider : character.getHitboxColliders()) {
            scene.getCollisionWorld().addCollider(collider);
            if (collider instanceof BattleArenaHitboxCollider) {
                BattleArenaHitboxDebugGameObject debugObject =
                        new BattleArenaHitboxDebugGameObject((BattleArenaHitboxCollider) collider, character.meshObject.texture);
                debugObject.setEnabled(debugHitboxesVisible);
                debugHitboxes.add(debugObject);
                scene.addGameObject(debugObject);
            }
        }
        log("registered hitboxes for " + character.meshObject.name + " count=" + character.getHitboxColliders().size());
    }

    private void updateSideStepFacing(BattleArenaCharacterRuntime self, BattleArenaCharacterRuntime opponent) {
        if (self == null || opponent == null || !self.isSideSteppingLeft()) {
            return;
        }
        self.faceTowards(opponent);
    }

    private void handleHitboxCollision(CollisionEvent event) {
        if (event == null || event.getType() != CollisionEventType.ENTER) {
            return;
        }
        if (!(event.getFirst() instanceof BattleArenaHitboxCollider)
                || !(event.getSecond() instanceof BattleArenaHitboxCollider)) {
            return;
        }

        BattleArenaHitboxCollider first = (BattleArenaHitboxCollider) event.getFirst();
        BattleArenaHitboxCollider second = (BattleArenaHitboxCollider) event.getSecond();

        if (first.getType() == BattleArenaHitboxCollider.Type.HITBOX
                && second.getType() == BattleArenaHitboxCollider.Type.HURTBOX) {
            logHit(first, second, event);
            second.getCharacter().onHitTaken(first.getCharacter(), second.getName(), second.getOnHitAnimation());
            return;
        }
        if (second.getType() == BattleArenaHitboxCollider.Type.HITBOX
                && first.getType() == BattleArenaHitboxCollider.Type.HURTBOX) {
            logHit(second, first, event);
            first.getCharacter().onHitTaken(second.getCharacter(), first.getName(), first.getOnHitAnimation());
        }
    }

    private void logHit(BattleArenaHitboxCollider attacker,
                        BattleArenaHitboxCollider defender,
                        CollisionEvent event) {
        log("HIT "
                + attacker.getCharacter().meshObject.name
                + " -> "
                + defender.getCharacter().meshObject.name
                + " hitbox=" + defender.getName()
                + " contact=" + event.getManifold().getContactPoint());
    }

    private void toggleActiveCharacter() {
        activeCharacter = activeCharacter == primaryCharacter ? secondaryCharacter : primaryCharacter;
        Vector3 activePosition = activeCharacter.getPosition();
        log("camera target switched to " + activeCharacter.meshObject.name
                + " x=" + activePosition.x + " z=" + activePosition.z);
    }

    private void toggleHitboxDebug() {
        debugHitboxesVisible = !debugHitboxesVisible;
        for (BattleArenaHitboxDebugGameObject debugHitbox : debugHitboxes) {
            debugHitbox.setEnabled(debugHitboxesVisible);
        }
        log("hitbox debug visible=" + debugHitboxesVisible);
    }

    private int loadCharacterTexture(GraphicsDevice graphicsDevice, BattleArenaCharacterDefinition definition) {
        String texturePath = resolveTexturePath(definition.model.texture);
        int texture = graphicsDevice.loadTexture(texturePath);
        log("model texture path=" + texturePath + " textureId=" + texture);
        return texture;
    }

    private float sampleTerrainHeight(float worldX, float worldZ) {
        if (terrainGeometry == null || terrainGeometry.heightMap == null || terrainGeometry.heightMap.length == 0
                || terrainGeometry.heightMap[0].length == 0) {
            return 0f;
        }

        float localX = worldX - terrainOrigin.x;
        float localZ = worldZ - terrainOrigin.z;
        int x0 = clampIndex((int) Math.floor(localX), terrainGeometry.heightMap.length - 1);
        int z0 = clampIndex((int) Math.floor(localZ), terrainGeometry.heightMap[0].length - 1);
        int x1 = clampIndex(x0 + 1, terrainGeometry.heightMap.length - 1);
        int z1 = clampIndex(z0 + 1, terrainGeometry.heightMap[0].length - 1);

        float tx = clamp(localX - x0, 0f, 1f);
        float tz = clamp(localZ - z0, 0f, 1f);

        float h00 = terrainGeometry.heightMap[x0][z0];
        float h10 = terrainGeometry.heightMap[x1][z0];
        float h01 = terrainGeometry.heightMap[x0][z1];
        float h11 = terrainGeometry.heightMap[x1][z1];
        float hx0 = lerp(h00, h10, tx);
        float hx1 = lerp(h01, h11, tx);
        return terrainOrigin.y + lerp(hx0, hx1, tz);
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
        Vector3 playerPosition = activeCharacter.getPosition();
        Vector3 focus = new Vector3(playerPosition.x, playerPosition.y + PLAYER_FOCUS_HEIGHT, playerPosition.z);
        float horizontalDistance = (float) Math.cos(cameraPitch) * CAMERA_DISTANCE;
        float verticalOffset = (float) Math.sin(cameraPitch) * CAMERA_DISTANCE;
        Vector3 cameraPosition = new Vector3(
                focus.x - ((float) Math.sin(cameraYaw) * horizontalDistance),
                focus.y + CAMERA_HEIGHT + verticalOffset,
                focus.z - ((float) Math.cos(cameraYaw) * horizontalDistance));
        camera.lookAt(cameraPosition, focus, new Vector3(0f, 1f, 0f));
    }

    private int clampIndex(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float lerp(float a, float b, float t) {
        return a + ((b - a) * t);
    }

    private String resolveTexturePath(String fileName) {
        File desktopResource = new File(com.njst.gaming.data.rootDirectory, fileName);
        if (desktopResource.isFile()) {
            return desktopResource.getPath();
        }
        // On Android rootDirectory is not set; fall back to asset name directly
        return fileName;
    }

    /** Logs a message to System.out. On Android the host AndroidEngineRenderer wraps the
     * full load in Log.e so any exception will appear in Logcat automatically.
     */
    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }
}
