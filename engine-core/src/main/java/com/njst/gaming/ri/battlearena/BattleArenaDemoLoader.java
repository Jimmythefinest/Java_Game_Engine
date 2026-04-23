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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BattleArenaDemoLoader implements Scene.SceneLoader {
    private static final String SKYBOX_FILE = "desertstorm.jpg";
    private static final String GROUND_FILE = "j.jpg";
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
    private static final float HEALTH_BAR_WIDTH = 1.35f;
    private static final float HEALTH_BAR_HEIGHT = 0.18f;
    private static final float HEALTH_BAR_VERTICAL_OFFSET = 2.35f;
    private static final boolean DISABLE_ACTIVE_ANIMATIONS_FOR_PROFILING = false;
    private static final String LOG_PREFIX = "[BattleArena] ";

    private TerrainGeometry terrainGeometry;
    private Vector3 terrainOrigin;
    private Scene loadedScene;
    private final List<GameObject> playerMeshes = new ArrayList<>();
    private final List<BattleArenaHitboxDebugGameObject> debugHitboxes = new ArrayList<>();
    private final BattleArenaCharacterAssembler characterAssembler = new BattleArenaCharacterAssembler();
    private final BattleArenaCharacterDefinitionLoader characterDefinitionLoader = new BattleArenaCharacterDefinitionLoader();
    private final BattleArenaSkillSystem skillSystem = new BattleArenaSkillSystem();
    private final List<BattleArenaControlledCharacter> arenaCharacters = new ArrayList<>();
    private final List<BattleArenaControlledCharacter> npcCharacters = new ArrayList<>();
    private BattleArenaControlledCharacter playerCharacter;
    private BattleArenaControlledCharacter activeCharacter;
    private float cameraYaw = 0f;
    private float cameraPitch = -0.18f;
    private boolean debugHitboxesVisible = false;
    private final ArrayList<KeyframeAnimation> activeAnimations = new ArrayList<>();

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        loadedScene = scene;
        log("load start root=" + com.njst.gaming.data.rootDirectory);
        playerMeshes.clear();
        debugHitboxes.clear();
        arenaCharacters.clear();
        npcCharacters.clear();
        activeAnimations.clear();
        playerCharacter = null;
        activeCharacter = null;
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
        GameObject ground = new GameObject(terrainGeometry, groundTexture);
        ground.ambientlight_multiplier = 3f;
        ground.shininess = 3f;
        ground.setPosition(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
       scene.addGameObject(ground);

        try {
            loadPlayer(scene, graphicsDevice);
        } catch (Exception e) {
            log("ERROR in loadPlayer: " + e.getMessage());
            throw e;
        }
        log("player meshes loaded=" + playerMeshes.size() + " bones=" + playerCharacter.runtime.bones.size());
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            character.runtime.syncRig();
            registerCharacterHitboxes(scene, character.runtime);
        }
        scene.getCollisionWorld().addListener(this::handleHitboxCollision);

        ActionInput actions = scene.actionInput;
        PointerState movementPointer = scene.pointer(BattleArenaActions.MOVE_POINTER);
        scene.registerPointerInput(BattleArenaActions.LOOK_POINTER,
                (activeScene, pointer) -> handlePointerLook(actions, pointer));

        scene.animations.add(new Animation() {
            @Override
            public void animate(float deltaSeconds) {
                BattleArenaSkillContext skillContext = createSkillContext();
                if (actions.button(BattleArenaActions.SNAP).pressed()) {
                    toggleActiveCharacter();
                }
                if (actions.button(BattleArenaActions.TOGGLE_HITBOXES).pressed()) {
                    toggleHitboxDebug();
                }
                if (actions.button(BattleArenaActions.MUD_WALL).pressed()) {
                    if (activeCharacter != null) {
                        activeCharacter.runSkill(BattleArenaMudWallSkill.ID, skillContext, resolveOpponent(activeCharacter));
                    }
                }
                if (playerCharacter != null) {
                    playerCharacter.captureControls(actions, movementPointer, primaryOpponentRuntime(), deltaSeconds);
                    playerCharacter.updateController(scene.speed);
                }
                for (BattleArenaControlledCharacter npc : npcCharacters) {
                    if (npc.brain instanceof BattleArenaSimpleChaseAi) {
                        ((BattleArenaSimpleChaseAi) npc.brain)
                                .setIncomingFireballThreat(skillSystem.hasIncomingFireballThreat(
                                        npc.runtime,
                                        playerCharacter != null ? playerCharacter.runtime : null));
                    }
                    npc.captureControls(actions, movementPointer, playerCharacter != null ? playerCharacter.runtime : null, deltaSeconds);
                    npc.updateController(scene.speed);
                    if (npc.controls.castMudWallPressed) {
                        npc.runSkill(BattleArenaMudWallSkill.ID, skillContext, resolveOpponent(npc));
                    }
                }
                updateFireballCasting(skillContext);
                skillSystem.update(skillContext, deltaSeconds);
                if (playerCharacter != null) {
                    for (BattleArenaControlledCharacter npc : npcCharacters) {
                        updateSideStepFacing(playerCharacter.runtime, npc.runtime);
                        updateSideStepFacing(npc.runtime, playerCharacter.runtime);
                    }
                }
                // Temporary profiling switch so we can isolate animation cost.
                if (!DISABLE_ACTIVE_ANIMATIONS_FOR_PROFILING) {
                    ArrayList<KeyframeAnimation> runningAnimations = new ArrayList<>(activeAnimations);
                    for (KeyframeAnimation anim : runningAnimations) {
                        anim.animate(deltaSeconds);
                    }
                }
                for (BattleArenaControlledCharacter character : arenaCharacters) {
                    character.runtime.syncRig();
                }
                updateCamera(scene.renderer.camera);
            }
        });

        updateCamera(scene.renderer.camera);
        Vector3 playerPosition = activeCharacter.runtime.getPosition();
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
        int characterTexture = loadCharacterTexture(graphicsDevice, definition);
        playerCharacter = spawnCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition,
                "BattleArenaPlayerMesh",
                "BattleArenaPlayerHealthBar",
                characterTexture,
                0f,
                null,
                true);
        activeCharacter = playerCharacter;

        log("wired animation count total=" + activeAnimations.size()
                + " idle=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_IDLE).size()
                + " walk=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_WALK).size()
                + " walkBackward=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_WALK_BACKWARD).size()
                + " run=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_RUN).size()
                + " jump=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_JUMP).size()
                + " punch=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_PUNCH).size()
                + " hit=" + playerCharacter.runtime.animationSet(BattleArenaCharacterController.ANIM_TAKE_HIT).size());

        log("loaded bones runtimeBones=" + playerCharacter.runtime.bones.size()
                + " root=" + playerCharacter.runtime.rootBone.name);

        Bone_object boneobj=new Bone_object(new CubeGeometry(), characterTexture);
        boneobj.bone=playerCharacter.runtime.rootBone;
        boneobj.scale=new float[]{0.1f,0.1f,0.1f};
        // scene.addGameObject(boneobj);
        log("spawned weighted mesh name=" + playerCharacter.runtime.meshObject.name + " scale=" + PLAYER_SCALE);

        BattleArenaControlledCharacter secondCharacter = spawnCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition,
                "BattleArenaSecondCharacter",
                "BattleArenaSecondCharacterHealthBar",
                characterTexture,
                SECOND_CHARACTER_START_X,
                new BattleArenaSimpleChaseAi(),
                false);
        npcCharacters.add(secondCharacter);
        log("spawned second character name=" + secondCharacter.runtime.meshObject.name + " bones=" + secondCharacter.runtime.bones.size());
    }

    private BattleArenaControlledCharacter spawnCharacter(Scene scene,
                                                          GraphicsDevice graphicsDevice,
                                                          WeightedGeometry weightedGeometry,
                                                          BattleArenaCharacterDefinition definition,
                                                          String meshName,
                                                          String healthBarName,
                                                          int texture,
                                                          float startX,
                                                          BattleArenaCharacterBrain brain,
                                                          boolean playerControlled) {
        BattleArenaCharacterController controller = new BattleArenaCharacterController();
        controller.setTerrainHeightSampler(this::sampleTerrainHeight);
        BattleArenaCharacterAssembly assembly = characterAssembler.assembleCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition,
                meshName,
                texture,
                PLAYER_SCALE,
                activeAnimations);
        BattleArenaCharacterRuntime runtime = new BattleArenaCharacterRuntime(controller, assembly, definition, activeAnimations);
        BattleArenaControlledCharacter character = new BattleArenaControlledCharacter(
                runtime,
                controller,
                skillSystem.skills(),
                brain,
                playerControlled);
        if (startX != 0f) {
            controller.setPlayerPosition(startX, 0f, 0f);
        }
        runtime.syncRig();
        playerMeshes.add(runtime.meshObject);
        scene.addGameObject(new BattleArenaHealthBarGameObject(
                character,
                scene.renderer.camera,
                healthBarName,
                HEALTH_BAR_WIDTH,
                HEALTH_BAR_HEIGHT,
                HEALTH_BAR_VERTICAL_OFFSET));
        arenaCharacters.add(character);
        return character;
    }

    private void updateFireballCasting(BattleArenaSkillContext skillContext) {
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            character.castLatched = updateFireballCasting(
                    skillContext,
                    character,
                    resolveOpponent(character),
                    character.castLatched);
        }
    }

    private boolean updateFireballCasting(BattleArenaSkillContext skillContext,
                                          BattleArenaControlledCharacter caster,
                                          BattleArenaCharacterRuntime target,
                                          boolean castLatched) {
        if (caster == null) {
            return false;
        }
        boolean casting = caster.runtime.isCasting();
        if (casting && !castLatched) {
            caster.runSkill(BattleArenaFireballSkill.ID, skillContext, target);
        }
        return casting;
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
        if (self == null || opponent == null || (!self.isSideSteppingLeft() && !self.isSideSteppingRight())) {
            return;
        }
        self.faceTowards(opponent);
    }

    private void handleHitboxCollision(CollisionEvent event) {
        if (event == null) {
            return;
        }
        if (event.getFirst() instanceof BattleArenaMudWallCollider
                || event.getSecond() instanceof BattleArenaMudWallCollider) {
            skillSystem.onCollision(createSkillContext(), event);
            return;
        }
        if (event.getType() != CollisionEventType.ENTER) {
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
        if (arenaCharacters.isEmpty()) {
            return;
        }
        int currentIndex = arenaCharacters.indexOf(activeCharacter);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % arenaCharacters.size();
        activeCharacter = arenaCharacters.get(nextIndex);
        Vector3 activePosition = activeCharacter.runtime.getPosition();
        log("camera target switched to " + activeCharacter.runtime.meshObject.name
                + " x=" + activePosition.x + " z=" + activePosition.z);
    }

    private void toggleHitboxDebug() {
        debugHitboxesVisible = !debugHitboxesVisible;
        for (BattleArenaHitboxDebugGameObject debugHitbox : debugHitboxes) {
            debugHitbox.setEnabled(debugHitboxesVisible);
        }
        skillSystem.setDebugVisible(debugHitboxesVisible);
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

    private BattleArenaCharacterRuntime resolveOpponent(BattleArenaControlledCharacter self) {
        if (self == null) {
            return null;
        }
        if (self != playerCharacter && playerCharacter != null) {
            return playerCharacter.runtime;
        }
        if (!npcCharacters.isEmpty()) {
            return npcCharacters.get(0).runtime;
        }
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            if (character != self) {
                return character.runtime;
            }
        }
        return null;
    }

    private BattleArenaCharacterRuntime primaryOpponentRuntime() {
        return resolveOpponent(playerCharacter);
    }

    private BattleArenaSkillContext createSkillContext() {
        return new BattleArenaSkillContext(
                loadedScene,
                this::sampleTerrainHeight,
                debugHitboxesVisible);
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
