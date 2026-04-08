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
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.objects.*;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class BattleArenaDemoLoader implements Scene.SceneLoader {
    private static final String SKYBOX_FILE = "desertstorm.jpg";
    private static final String GROUND_FILE = "stone.jpeg";
    private static final String MODEL_FILE = "weighted_geometry/defeated_mesh.ser";
    private static final String BONE_FILE = "weighted_geometry/defeated_bones.ser";
    private static final String BONE_NAMES_FILE = "weighted_geometry/defeated_bone_names.json";
    private static final String WALK_ANIMATIONS_FILE = "weighted_geometry/walking_animation.ser";
    private static final String WALK_BACKWARD_ANIMATIONS_FILE = "weighted_geometry/walking_backwards_animation.ser";
    private static final String RUN_ANIMATIONS_FILE = "weighted_geometry/run_animation.ser";
    private static final String IDLE_ANIMATIONS_FILE = "weighted_geometry/idle_animation.ser";
    private static final String IDLE_ANIMATIONS_FALLBACK_FILE = "weighted_geometry/indle_animation.ser";
    private static final String JUMP_ANIMATIONS_FILE = "weighted_geometry/jump_animation.ser";
    private static final String MODEL_TEXTURE_FILE = "j.jpg";
    private static final int GROUND_SIZE = 96;
    private static final float MOVE_DEADZONE = 0.12f;
    private static final float WALK_SPEED = 0.02f;
    private static final float RUN_SPEED = 0.035f;
    private static final float TURN_SPEED_DEGREES = 3.2f;
    private static final float CAMERA_DISTANCE = 6.5f;
    private static final float CAMERA_HEIGHT = 1.2f;
    private static final float LOOK_SENSITIVITY = 0.0125f;
    private static final float MIN_PITCH = -0.8f;
    private static final float MAX_PITCH = 0.45f;
    private static final float PLAYER_SCALE = 1f;
    private static final float PLAYER_FOCUS_HEIGHT = 1.6f;
    private static final float JUMP_VELOCITY = 0.22f;
    private static final float JUMP_GRAVITY = 0.012f;
    private static final String LOG_PREFIX = "[BattleArena] ";

    private TerrainGeometry terrainGeometry;
    private Vector3 terrainOrigin;
    private final List<GameObject> playerMeshes = new ArrayList<>();
    private final Vector3 playerPosition = new Vector3(0f, 0f, 0f);
    private float playerHeadingDegrees = 0f;
    private float cameraYaw = 0f;
    private float cameraPitch = -0.18f;
    private ArrayList<Bone> playerBones = new ArrayList<>();
    private Bone rootBone;
    private Bone hipBone;
    private Vector3 rootBasePosition = new Vector3(0f, 0f, 0f);
    private Vector3 rootBaseRotation = new Vector3(0f, 0f, 0f);
    private Skeleton playerSkeleton;
    private final ArrayList<KeyframeAnimation> activeAnimations = new ArrayList<>();
    private final ArrayList<KeyframeAnimation> idleAnimations = new ArrayList<>();
    private final ArrayList<KeyframeAnimation> walkAnimations = new ArrayList<>();
    private final ArrayList<KeyframeAnimation> walkBackwardAnimations = new ArrayList<>();
    private final ArrayList<KeyframeAnimation> runAnimations = new ArrayList<>();
    private final ArrayList<KeyframeAnimation> jumpAnimations = new ArrayList<>();
    private final ArrayList<KeyframeAnimation> spawnedIdleAnimations = new ArrayList<>();
    private final ArrayList<Bone> spawnedRootBones = new ArrayList<>();
    private final ArrayList<Vector3> spawnedPositions = new ArrayList<>();
    private final ArrayList<Vector3> spawnedRootBasePositions = new ArrayList<>();
    private ArrayList<KeyframeAnimation> currentAnimationSet = null;
    private boolean playerMoving = false;
    private boolean playerMovingBackward = false;
    private boolean playerRunning = false;
    private float jumpHeight = 0f;
    private float verticalVelocity = 0f;
    private boolean jumping = false;

    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        log("load start root=" + com.njst.gaming.data.rootDirectory);
        playerMeshes.clear();
        playerBones.clear();
        activeAnimations.clear();
        spawnedIdleAnimations.clear();
        spawnedRootBones.clear();
        spawnedPositions.clear();
        spawnedRootBasePositions.clear();
        rootBone = null;
        hipBone = null;
        playerSkeleton = null;
        currentAnimationSet = null;
        playerMoving = false;
        playerMovingBackward = false;
        playerRunning = false;
        jumpHeight = 0f;
        verticalVelocity = 0f;
        jumping = false;
        playerPosition.set(0f, 0f, 0f);
        playerHeadingDegrees = 0f;
        cameraYaw = 0f;
        cameraPitch = -0.18f;

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
        log("player meshes loaded=" + playerMeshes.size() + " bones=" + playerBones.size());
        snapPlayerToGround();
        syncPlayerRig();

        ActionInput actions = scene.actionInput;
        PointerState movementPointer = scene.pointer(BattleArenaActions.MOVE_POINTER);
        scene.registerPointerInput(BattleArenaActions.LOOK_POINTER,
                (activeScene, pointer) -> handlePointerLook(actions, pointer));

        scene.animations.add(new Animation() {
            @Override
            public void animate() {
                applyPlayerInput(actions, movementPointer, scene.speed);
                // drive keyframe animations each frame
                for (KeyframeAnimation anim : activeAnimations) {
                    anim.animate();
                }
                if (rootBone != null) {
                    rootBone.update();
                }
                for (Bone spawnedRootBone : spawnedRootBones) {
                    spawnedRootBone.update();
                }
                syncPlayerRig();
                syncSpawnedRigs();
                updateCamera(scene.renderer.camera);
            }
        });

        updateCamera(scene.renderer.camera);
        log("load complete playerPosition=" + playerPosition.x + "," + playerPosition.y + "," + playerPosition.z);
        // if(rootBone.parent!=null){
        	// log("Root Bone has parent");
        // }
    }

    private void loadPlayer(Scene scene, GraphicsDevice graphicsDevice) {
        log("loading model asset=" + MODEL_FILE);
        byte[] modelBytes = graphicsDevice.loadBinaryResource(MODEL_FILE);
        if (modelBytes == null || modelBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + MODEL_FILE
                    + " - check that the file is in the Android assets folder under weighted_geometry/");
        }
        log("model bytes=" + modelBytes.length);
        WeightedGeometry weightedGeometry = deserializeWeightedGeometry(modelBytes);
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

        log("loading bone names asset=" + BONE_NAMES_FILE);
        String boneNamesJson = graphicsDevice.loadTextResource(BONE_NAMES_FILE);
        if (boneNamesJson == null || boneNamesJson.isEmpty()) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + BONE_NAMES_FILE);
        }
        List<String> boneNames = parseJsonArray(boneNamesJson);
        log("parsed bone names count=" + boneNames.size());

        log("loading bones asset=" + BONE_FILE);
        byte[] boneBytes = graphicsDevice.loadBinaryResource(BONE_FILE);
        if (boneBytes == null || boneBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + BONE_FILE);
        }
        playerBones = deserializeBoneList(boneBytes);
        log("deserialized bones count=" + playerBones.size());
        applyBoneNames(playerBones, boneNames);
        rootBone = findRootBone(playerBones);
        if (rootBone == null) {
            throw new IllegalStateException(LOG_PREFIX + "No root bone found in " + BONE_FILE);
        }
        hipBone = findBone(playerBones, "hips");
        if (hipBone == null) {
            throw new IllegalStateException(LOG_PREFIX + "No hip bone found in " + BONE_FILE);
        }
        log("root bone=" + rootBone.name);
        log("hip bone=" + hipBone.name);
        rootBasePosition = rootBone.position_to_parent.clone();
        rootBaseRotation = rootBone.rotation.clone();
        rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        rootBone.update();
        for (Bone bone : playerBones) {
            bone.calculate_bind_matrix();
        }

        // ---- Animations ----
        activeAnimations.clear();
        idleAnimations.clear();
        walkAnimations.clear();
        walkBackwardAnimations.clear();
        runAnimations.clear();
        jumpAnimations.clear();

        playerSkeleton = new Skeleton(rootBone);
        loadAnimationSet(graphicsDevice, scene, WALK_ANIMATIONS_FILE, walkAnimations);
        loadAnimationSet(graphicsDevice, scene, WALK_BACKWARD_ANIMATIONS_FILE, walkBackwardAnimations);
        loadAnimationSet(graphicsDevice, scene, RUN_ANIMATIONS_FILE, runAnimations);
        loadAnimationSet(graphicsDevice, scene, resolveIdleAnimationFile(graphicsDevice), idleAnimations);
        loadOptionalAnimationSet(graphicsDevice, scene, JUMP_ANIMATIONS_FILE, jumpAnimations);
        setCurrentAnimationSet(idleAnimations);
        log("wired animation count total=" + activeAnimations.size()
                + " idle=" + idleAnimations.size()
                + " walk=" + walkAnimations.size()
                + " walkBackward=" + walkBackwardAnimations.size()
                + " run=" + runAnimations.size()
                + " jump=" + jumpAnimations.size());

        // Initial bone update so the bind matrices reflect the rest pose
        rootBone.update();
        for (Bone bone : playerBones) {
            bone.calculate_bind_matrix();
        }

        log("loaded bones names=" + boneNames.size() + " runtimeBones=" + playerBones.size()
                + " root=" + rootBone.name);

        String texturePath = resolveTexturePath(MODEL_TEXTURE_FILE);
        int texture = graphicsDevice.loadTexture(texturePath);
        log("model texture path=" + texturePath + " textureId=" + texture);

        Weighted_GameObject meshObject = new Weighted_GameObject(weightedGeometry, texture);
        meshObject.name = "BattleArenaPlayerMesh";
        Bone_object boneobj=new Bone_object(new CubeGeometry(),texture);
        boneobj.bone=rootBone;
        boneobj.scale=new float[]{0.1f,0.1f,0.1f};
        // scene.addGameObject(boneobj);
        meshObject.shininess = 18f;
        meshObject.ambientlight_multiplier = 1.2f;
        meshObject.setScale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        meshObject.boneBufferStartIndex = scene.registerSkeleton(playerBones);
       scene.addGameObject(meshObject);
        playerMeshes.add(meshObject);
        log("spawned weighted mesh name=" + meshObject.name + " scale=" + PLAYER_SCALE);

        spawnIdleModel(scene, graphicsDevice, weightedGeometry, texture, boneNames);
    }

    private void syncPlayerRig() {
        if (rootBone == null) {
            return;
        }
        rootBone.position_to_parent.set(
                rootBasePosition.x + playerPosition.x,
                rootBasePosition.y + playerPosition.y,
                rootBasePosition.z + playerPosition.z);
        rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        rootBone.update();
        rootBone.rotate(new Vector3());
        // for (GameObject mesh : playerMeshes) {
            // mesh.setPosition(0f, 0f, 0f);
            // mesh.setRotation(0f, 0f, 0f);
        // }
    }

    private void syncSpawnedRigs() {
        for (int i = 0; i < spawnedRootBones.size(); i++) {
            Bone spawnedRootBone = spawnedRootBones.get(i);
            Vector3 spawnedPosition = spawnedPositions.get(i);
            Vector3 spawnedRootBasePosition = spawnedRootBasePositions.get(i);
            spawnedPosition.y = sampleTerrainHeight(spawnedPosition.x, spawnedPosition.z);
            spawnedRootBone.position_to_parent.set(
                    spawnedRootBasePosition.x + spawnedPosition.x,
                    spawnedRootBasePosition.y + spawnedPosition.y,
                    spawnedRootBasePosition.z + spawnedPosition.z);
            spawnedRootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
            spawnedRootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
            spawnedRootBone.update();
            spawnedRootBone.rotate(new Vector3());
        }
    }

    private WeightedGeometry deserializeWeightedGeometry(byte[] modelBytes) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(modelBytes))) {
            Object value = inputStream.readObject();
            if (!(value instanceof WeightedGeometry)) {
                throw new IllegalStateException("Expected WeightedGeometry in " + MODEL_FILE + " but found "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            return (WeightedGeometry) value;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize weighted geometry: " + MODEL_FILE, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, KeyframeAnimation> deserializeAnimations(byte[] bytes) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object value = inputStream.readObject();
            if (!(value instanceof Map<?, ?>)) {
                throw new IllegalStateException("Expected Map in animation resource but found "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            Map<?, ?> raw = (Map<?, ?>) value;
            Map<String, KeyframeAnimation> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof KeyframeAnimation)) {
                    throw new IllegalStateException("Unexpected entry type in animation resource");
                }
                result.put((String) entry.getKey(), (KeyframeAnimation) entry.getValue());
            }
            return result;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize animations", e);
        }
    }

    private void loadAnimationSet(GraphicsDevice graphicsDevice,
                                  Scene scene,
                                  String animationFile,
                                  ArrayList<KeyframeAnimation> targetList) {
        log("loading animations asset=" + animationFile);
        byte[] animBytes = graphicsDevice.loadBinaryResource(animationFile);
        if (animBytes == null || animBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + animationFile);
        }
        Map<String, KeyframeAnimation> animMap = deserializeAnimations(animBytes);
        log("loaded animation entries file=" + animationFile + " count=" + animMap.size());

        Skeletal_Animation skeletalAnimation = new Skeletal_Animation();
        skeletalAnimation.set_Animation_map(animMap);
        playerSkeleton.map(skeletalAnimation);

        for (Map.Entry<String, KeyframeAnimation> entry : animMap.entrySet()) {
            KeyframeAnimation kfa = entry.getValue();
            if (kfa.bone == null) {
                continue;
            }
            kfa.onfinish = () -> kfa.time = 0f;
            kfa.stop();
            kfa.time = 0f;
            targetList.add(kfa);
            activeAnimations.add(kfa);
            scene.KEY_ANIMATIONS.add(kfa);
        }
    }

    private void loadOptionalAnimationSet(GraphicsDevice graphicsDevice,
                                          Scene scene,
                                          String animationFile,
                                          ArrayList<KeyframeAnimation> targetList) {
        byte[] animBytes = graphicsDevice.loadBinaryResource(animationFile);
        if (animBytes == null || animBytes.length == 0) {
            log("optional animation asset missing=" + animationFile);
            return;
        }
        loadAnimationSet(graphicsDevice, scene, animationFile, targetList);
    }

    private void spawnIdleModel(Scene scene,
                                GraphicsDevice graphicsDevice,
                                WeightedGeometry weightedGeometry,
                                int texture,
                                List<String> boneNames) {
        byte[] boneBytes = graphicsDevice.loadBinaryResource(BONE_FILE);
        if (boneBytes == null || boneBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + BONE_FILE);
        }
        ArrayList<Bone> spawnedBones = deserializeBoneList(boneBytes);
        applyBoneNames(spawnedBones, boneNames);
        Bone spawnedRootBone = findRootBone(spawnedBones);
        if (spawnedRootBone == null) {
            throw new IllegalStateException(LOG_PREFIX + "No root bone found for spawned model in " + BONE_FILE);
        }
        Vector3 spawnedRootBasePosition = spawnedRootBone.position_to_parent.clone();
        spawnedRootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        spawnedRootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        spawnedRootBone.update();
        for (Bone bone : spawnedBones) {
            bone.calculate_bind_matrix();
        }

        String idleAnimationFile = resolveIdleAnimationFile(graphicsDevice);
        byte[] animBytes = graphicsDevice.loadBinaryResource(idleAnimationFile);
        if (animBytes == null || animBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + idleAnimationFile);
        }
        Map<String, KeyframeAnimation> idleAnimMap = deserializeAnimations(animBytes);
        Skeleton spawnedSkeleton = new Skeleton(spawnedRootBone);
        Skeletal_Animation skeletalAnimation = new Skeletal_Animation();
        skeletalAnimation.set_Animation_map(idleAnimMap);
        spawnedSkeleton.map(skeletalAnimation);

        for (Map.Entry<String, KeyframeAnimation> entry : idleAnimMap.entrySet()) {
            KeyframeAnimation kfa = entry.getValue();
            if (kfa.bone == null) {
                continue;
            }
            kfa.onfinish = () -> kfa.time = 0f;
            kfa.time = 0f;
            kfa.start();
            spawnedIdleAnimations.add(kfa);
            activeAnimations.add(kfa);
            scene.KEY_ANIMATIONS.add(kfa);
        }

        Weighted_GameObject spawnedMeshObject = new Weighted_GameObject(weightedGeometry, texture);
        spawnedMeshObject.name = "BattleArenaIdleNpc";
        spawnedMeshObject.shininess = 18f;
        spawnedMeshObject.ambientlight_multiplier = 1.2f;
        spawnedMeshObject.setScale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        spawnedMeshObject.boneBufferStartIndex = scene.registerSkeleton(spawnedBones);
        scene.addGameObject(spawnedMeshObject);
        playerMeshes.add(spawnedMeshObject);
        spawnedRootBones.add(spawnedRootBone);
        spawnedPositions.add(new Vector3(2.5f, 0f, 0f));
        spawnedRootBasePositions.add(spawnedRootBasePosition);
        syncSpawnedRigs();
        log("spawned idle npc name=" + spawnedMeshObject.name + " bones=" + spawnedBones.size());
    }

    private String resolveIdleAnimationFile(GraphicsDevice graphicsDevice) {
        byte[] idleBytes = graphicsDevice.loadBinaryResource(IDLE_ANIMATIONS_FILE);
        if (idleBytes != null && idleBytes.length > 0) {
            return IDLE_ANIMATIONS_FILE;
        }
        return IDLE_ANIMATIONS_FALLBACK_FILE;
    }

    private void setCurrentAnimationSet(ArrayList<KeyframeAnimation> nextAnimationSet) {
        if (nextAnimationSet == null || nextAnimationSet.isEmpty() || currentAnimationSet == nextAnimationSet) {
            return;
        }
        if (currentAnimationSet != null) {
            for (KeyframeAnimation animation : currentAnimationSet) {
                animation.stop();
                animation.time = 0f;
            }
        }
        for (KeyframeAnimation animation : nextAnimationSet) {
            animation.time = 0f;
            animation.start();
        }
        currentAnimationSet = nextAnimationSet;
    }

    private void updateMovementAnimationState() {
        if (jumping) {
            if (!jumpAnimations.isEmpty()) {
                setCurrentAnimationSet(jumpAnimations);
            }
            return;
        }
        if (!playerMoving) {
            setCurrentAnimationSet(idleAnimations);
            return;
        }
        if (playerMovingBackward) {
            setCurrentAnimationSet(walkBackwardAnimations);
            return;
        }
        setCurrentAnimationSet(playerRunning ? runAnimations : walkAnimations);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Bone> deserializeBoneList(byte[] boneBytes) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(boneBytes))) {
            Object value = inputStream.readObject();
            if (!(value instanceof ArrayList<?>)) {
                throw new IllegalStateException("Expected ArrayList in " + BONE_FILE + " but found "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            ArrayList<?> list = (ArrayList<?>) value;
            ArrayList<Bone> bones = new ArrayList<>(list.size());
            for (Object entry : list) {
                if (!(entry instanceof Bone)) {
                    throw new IllegalStateException("Expected Bone entries in " + BONE_FILE + " but found "
                            + (entry == null ? "null" : entry.getClass().getName()));
                }
                bones.add((Bone) entry);
            }
            return bones;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize bones: " + BONE_FILE, e);
        }
    }

    private void applyBoneNames(ArrayList<Bone> bones, List<String> names) {
        if (bones.size() != names.size()) {
            throw new IllegalStateException("Bone count mismatch names=" + names.size() + " bones=" + bones.size());
        }
        for (int i = 0; i < bones.size(); i++) {
            bones.get(i).name = names.get(i);
        }
    }

    private Bone findRootBone(ArrayList<Bone> bones) {
        Map<Bone, Boolean> children = new IdentityHashMap<>();
        for (Bone bone : bones) {
            for (Bone child : bone.Children) {
                children.put(child, Boolean.TRUE);
            }
        }
        for (Bone bone : bones) {
            if (!children.containsKey(bone)) {
                return bone;
            }
        }
        return  bones.get(0);
    }

    private Bone findBone(ArrayList<Bone> bones, String nameFragment) {
        String needle = nameFragment.toLowerCase();
        for (Bone bone : bones) {
            if (bone.name != null && bone.name.toLowerCase().contains(needle)) {
                return bone;
            }
        }
        return null;
    }

    private List<String> parseJsonArray(String json) {
        ArrayList<String> values = new ArrayList<>();
        int quote = 34;
        int slash = 92;
        int index = 0;
        while (index < json.length()) {
            int startQuote = json.indexOf(quote, index);
            if (startQuote < 0) {
                break;
            }
            StringBuilder value = new StringBuilder();
            boolean escaping = false;
            int cursor = startQuote + 1;
            while (cursor < json.length()) {
                char c = json.charAt(cursor++);
                if (escaping) {
                    if (c == 'n') {
                        value.append((char) 10);
                    } else if (c == 'r') {
                        value.append((char) 13);
                    } else if (c == 't') {
                        value.append((char) 9);
                    } else {
                        value.append(c);
                    }
                    escaping = false;
                    continue;
                }
                if (c == slash) {
                    escaping = true;
                    continue;
                }
                if (c == quote) {
                    values.add(value.toString());
                    index = cursor;
                    break;
                }
                value.append(c);
            }
        }
        return values;
    }

    private void applyPlayerInput(ActionInput actions, PointerState movementPointer, float sceneSpeed) {
        float forwardInput = 0f;
        float turnInput = 0f;

        if (movementPointer.isActive()) {
            forwardInput += -applyDeadzone(movementPointer.getY());
            turnInput += applyDeadzone(movementPointer.getX());
        }
        if (actions.button(BattleArenaActions.FORWARD).isDown()) {
            forwardInput += 1f;
        }
        if (actions.button(BattleArenaActions.BACKWARD).isDown()) {
            forwardInput -= 1f;
        }
        if (actions.button(BattleArenaActions.TURN_LEFT).isDown()) {
            turnInput -= 1f;
        }
        if (actions.button(BattleArenaActions.ROTATE).isDown()) {
            turnInput += 1f;
        }

        if (actions.button(BattleArenaActions.JUMP).pressed() && !jumping) {
            jumping = true;
            verticalVelocity = JUMP_VELOCITY * sceneSpeed;
            updateMovementAnimationState();
        }

        forwardInput = clamp(forwardInput);
        turnInput = clamp(turnInput);
        float turnAmount = turnInput * TURN_SPEED_DEGREES * sceneSpeed;
        playerHeadingDegrees += turnAmount;
        if (hipBone != null && Math.abs(turnAmount) > 0.0001f) {
            rootBone.setRotation(new Vector3(0f, playerHeadingDegrees, 0f));
            hipBone.rotate(new Vector3());
        }

        boolean wantsToRun = actions.button(BattleArenaActions.RUN).isDown();
        float movementSpeed = wantsToRun ? RUN_SPEED : WALK_SPEED;
        float moveAmount = forwardInput * movementSpeed * sceneSpeed;
        boolean isMovingNow = Math.abs(moveAmount) > 0.0001f;
        boolean isMovingBackwardNow = moveAmount < -0.0001f;
        boolean isRunningNow = isMovingNow && wantsToRun;
        if (isMovingNow != playerMoving
                || isMovingBackwardNow != playerMovingBackward
                || isRunningNow != playerRunning) {
            playerMoving = isMovingNow;
            playerMovingBackward = isMovingBackwardNow;
            playerRunning = isRunningNow;
            updateMovementAnimationState();
        }
        if (isMovingNow) {
            float headingRadians = (float) Math.toRadians(playerHeadingDegrees);
            playerPosition.x += (float) Math.sin(headingRadians) * moveAmount;
            playerPosition.z += (float) Math.cos(headingRadians) * moveAmount;
        }
        updateJumpPhysics(sceneSpeed);
        snapPlayerToGround();
    }

    private void updateJumpPhysics(float sceneSpeed) {
        if (!jumping) {
            jumpHeight = 0f;
            verticalVelocity = 0f;
            return;
        }

        jumpHeight += verticalVelocity;
        verticalVelocity -= JUMP_GRAVITY * sceneSpeed;
        if (jumpHeight <= 0f && verticalVelocity <= 0f) {
            jumpHeight = 0f;
            verticalVelocity = 0f;
            jumping = false;
            updateMovementAnimationState();
        }
    }

    private void snapPlayerToGround() {
        playerPosition.y = sampleTerrainHeight(playerPosition.x, playerPosition.z) + jumpHeight;
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
        Vector3 focus = new Vector3(playerPosition.x, playerPosition.y + PLAYER_FOCUS_HEIGHT, playerPosition.z);
        float horizontalDistance = (float) Math.cos(cameraPitch) * CAMERA_DISTANCE;
        float verticalOffset = (float) Math.sin(cameraPitch) * CAMERA_DISTANCE;
        Vector3 cameraPosition = new Vector3(
                focus.x - ((float) Math.sin(cameraYaw) * horizontalDistance),
                focus.y + CAMERA_HEIGHT + verticalOffset,
                focus.z - ((float) Math.cos(cameraYaw) * horizontalDistance));
        camera.lookAt(cameraPosition, focus, new Vector3(0f, 1f, 0f));
    }

    private float applyDeadzone(float value) {
        if (Math.abs(value) < MOVE_DEADZONE) {
            return 0f;
        }
        return clamp(value);
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
