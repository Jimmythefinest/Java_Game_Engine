package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleArenaCharacterAssembler {
    private static final String LOG_PREFIX = "[BattleArena] ";
    private static final String[] REQUIRED_ANIMATION_KEYS = {
            BattleArenaCharacterController.ANIM_WALK,
            BattleArenaCharacterController.ANIM_WALK_BACKWARD,
            BattleArenaCharacterController.ANIM_RUN,
            BattleArenaCharacterController.ANIM_IDLE
    };
    private static final String[] OPTIONAL_ANIMATION_KEYS = {
            BattleArenaCharacterController.ANIM_JUMP,
            BattleArenaCharacterController.ANIM_PUNCH,
            BattleArenaCharacterController.ANIM_KICK,
            BattleArenaCharacterController.ANIM_LEFTSIDE_STEP,
            BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP,
            BattleArenaCharacterController.ANIM_TAKE_HIT
    };

    WeightedGeometry loadWeightedGeometry(GraphicsDevice graphicsDevice, String modelFile) {
        log("loading model asset=" + modelFile);
        byte[] modelBytes = graphicsDevice.loadBinaryResource(modelFile);
        if (modelBytes == null || modelBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + modelFile);
        }
        log("model bytes=" + modelBytes.length);
        return deserializeWeightedGeometry(modelBytes, modelFile);
    }

    List<String> loadBoneNames(GraphicsDevice graphicsDevice, String boneNamesFile) {
        log("loading bone names asset=" + boneNamesFile);
        String boneNamesJson = graphicsDevice.loadTextResource(boneNamesFile);
        if (boneNamesJson == null || boneNamesJson.isEmpty()) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + boneNamesFile);
        }
        List<String> boneNames = parseJsonArray(boneNamesJson);
        log("parsed bone names count=" + boneNames.size());
        return boneNames;
    }

    BattleArenaCharacterAssembly assembleCharacter(Scene scene,
                                                   GraphicsDevice graphicsDevice,
                                                   WeightedGeometry weightedGeometry,
                                                   BattleArenaCharacterDefinition definition,
                                                   String meshName,
                                                   int texture,
                                                   float playerScale,
                                                   ArrayList<KeyframeAnimation> activeAnimations) {
        return assembleCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition.model.bones,
                loadBoneNames(graphicsDevice, definition.model.boneNames),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_IDLE),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_IDLE),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_WALK),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_WALK),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_WALK_BACKWARD),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_WALK_BACKWARD),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_RUN),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_RUN),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_JUMP),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_JUMP),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_PUNCH),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_PUNCH),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_KICK),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_KICK),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_LEFTSIDE_STEP),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_LEFTSIDE_STEP),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP),
                definitionAnimationAsset(definition, BattleArenaCharacterController.ANIM_TAKE_HIT),
                definitionAnimationFramesPerSecond(definition, BattleArenaCharacterController.ANIM_TAKE_HIT),
                texture,
                meshName,
                playerScale,
                activeAnimations);
    }

    BattleArenaCharacterAssembly assembleCharacter(Scene scene,
                                                   GraphicsDevice graphicsDevice,
                                                   WeightedGeometry weightedGeometry,
                                                   String boneFile,
                                                   List<String> boneNames,
                                                   String idleAnimationFile,
                                                   float idleFramesPerSecond,
                                                   String walkAnimationFile,
                                                   float walkFramesPerSecond,
                                                   String walkBackwardAnimationFile,
                                                   float walkBackwardFramesPerSecond,
                                                   String runAnimationFile,
                                                   float runFramesPerSecond,
                                                   String jumpAnimationFile,
                                                   float jumpFramesPerSecond,
                                                   String punchAnimationFile,
                                                   float punchFramesPerSecond,
                                                   String kickAnimationFile,
                                                   float kickFramesPerSecond,
                                                   String leftsideStepAnimationFile,
                                                   float leftsideStepFramesPerSecond,
                                                   String rightsideStepAnimationFile,
                                                   float rightsideStepFramesPerSecond,
                                                   String takeHitAnimationFile,
                                                   float takeHitFramesPerSecond,
                                                   int texture,
                                                   String meshName,
                                                   float playerScale,
                                                   ArrayList<KeyframeAnimation> activeAnimations) {
        BattleArenaCharacterAssembly assembly = new BattleArenaCharacterAssembly();
        assembly.bones = loadBones(graphicsDevice, boneFile, boneNames);
        assembly.rootBone = findRootBone(assembly.bones);
        if (assembly.rootBone == null) {
            throw new IllegalStateException(LOG_PREFIX + "No root bone found in " + boneFile);
        }
        assembly.hipBone = findBone(assembly.bones, "hips");
        if (assembly.hipBone == null) {
            throw new IllegalStateException(LOG_PREFIX + "No hip bone found in " + boneFile);
        }
        log("root bone=" + assembly.rootBone.name);
        log("hip bone=" + assembly.hipBone.name);
        assembly.rootBasePosition = assembly.rootBone.position_to_parent.clone();
        assembly.rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        assembly.rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        assembly.rootBone.update();
        for (Bone bone : assembly.bones) {
            bone.calculate_bind_matrix();
        }

        assembly.skeleton = new Skeleton(assembly.rootBone);
        Map<String, AnimationAssetSpec> animationSpecs = new LinkedHashMap<>();
        animationSpecs.put(BattleArenaCharacterController.ANIM_WALK, new AnimationAssetSpec(walkAnimationFile, walkFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_WALK_BACKWARD, new AnimationAssetSpec(walkBackwardAnimationFile, walkBackwardFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_RUN, new AnimationAssetSpec(runAnimationFile, runFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_IDLE, new AnimationAssetSpec(idleAnimationFile, idleFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_JUMP, new AnimationAssetSpec(jumpAnimationFile, jumpFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_PUNCH, new AnimationAssetSpec(punchAnimationFile, punchFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_KICK, new AnimationAssetSpec(kickAnimationFile, kickFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_LEFTSIDE_STEP, new AnimationAssetSpec(leftsideStepAnimationFile, leftsideStepFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP, new AnimationAssetSpec(rightsideStepAnimationFile, rightsideStepFramesPerSecond));
        animationSpecs.put(BattleArenaCharacterController.ANIM_TAKE_HIT, new AnimationAssetSpec(takeHitAnimationFile, takeHitFramesPerSecond));
        loadAnimationSets(graphicsDevice, scene, assembly, animationSpecs, activeAnimations);

        assembly.rootBone.update();
        for (Bone bone : assembly.bones) {
            bone.calculate_bind_matrix();
        }

        assembly.meshObject = new Weighted_GameObject(weightedGeometry, texture);
        assembly.meshObject.name = meshName;
        assembly.meshObject.shininess = 18f;
        assembly.meshObject.ambientlight_multiplier = 1.2f;
        assembly.meshObject.setScale(playerScale, playerScale, playerScale);
        assembly.meshObject.boneBufferStartIndex = scene.registerSkeleton(assembly.bones);
        scene.addGameObject(assembly.meshObject);
        return assembly;
    }

    private void loadAnimationSets(GraphicsDevice graphicsDevice,
                                   Scene scene,
                                   BattleArenaCharacterAssembly assembly,
                                   Map<String, AnimationAssetSpec> animationSpecs,
                                   ArrayList<KeyframeAnimation> activeAnimations) {
        for (String animationKey : REQUIRED_ANIMATION_KEYS) {
            AnimationAssetSpec spec = animationSpecs.get(animationKey);
            if (spec == null) {
                throw new IllegalStateException("Missing required animation spec for key=" + animationKey);
            }
            loadAnimationSet(
                    graphicsDevice,
                    scene,
                    assembly.skeleton,
                    spec.path,
                    spec.framesPerSecond,
                    assembly.animationSet(animationKey),
                    activeAnimations);
        }
        for (String animationKey : OPTIONAL_ANIMATION_KEYS) {
            AnimationAssetSpec spec = animationSpecs.get(animationKey);
            if (spec == null) {
                continue;
            }
            loadOptionalAnimationSet(
                    graphicsDevice,
                    scene,
                    assembly.skeleton,
                    spec.path,
                    spec.framesPerSecond,
                    assembly.animationSet(animationKey),
                    activeAnimations);
        }
    }

    private ArrayList<Bone> loadBones(GraphicsDevice graphicsDevice, String boneFile, List<String> boneNames) {
        log("loading bones asset=" + boneFile);
        byte[] boneBytes = graphicsDevice.loadBinaryResource(boneFile);
        if (boneBytes == null || boneBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + boneFile);
        }
        ArrayList<Bone> bones = deserializeBoneList(boneBytes, boneFile);
        log("deserialized bones count=" + bones.size());
        applyBoneNames(bones, boneNames);
        return bones;
    }

    private WeightedGeometry deserializeWeightedGeometry(byte[] modelBytes, String modelFile) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(modelBytes))) {
            Object value = inputStream.readObject();
            if (!(value instanceof WeightedGeometry)) {
                throw new IllegalStateException("Expected WeightedGeometry in " + modelFile + " but found "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            return (WeightedGeometry) value;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize weighted geometry: " + modelFile, e);
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
                                  Skeleton skeleton,
                                  String animationFile,
                                  float framesPerSecond,
                                  ArrayList<KeyframeAnimation> targetList,
                                  ArrayList<KeyframeAnimation> activeAnimations) {
        log("loading animations asset=" + animationFile);
        byte[] animBytes = graphicsDevice.loadBinaryResource(animationFile);
        if (animBytes == null || animBytes.length == 0) {
            throw new IllegalStateException(LOG_PREFIX + "Asset not found or empty: " + animationFile);
        }
        Map<String, KeyframeAnimation> animMap = deserializeAnimations(animBytes);
        log("loaded animation entries file=" + animationFile + " count=" + animMap.size());

        Skeletal_Animation skeletalAnimation = new Skeletal_Animation();
        skeletalAnimation.set_Animation_map(animMap);
        skeleton.map(skeletalAnimation);

        for (Map.Entry<String, KeyframeAnimation> entry : animMap.entrySet()) {
            KeyframeAnimation animation = entry.getValue();
            if (animation.bone == null) {
                continue;
            }
            normalizeAnimationTiming(animation);
            animation.framesPerSecond = framesPerSecond;
            animation.onfinish = () -> animation.time = 0f;
            animation.stop();
            animation.time = 0f;
            animation.speed = 1f;
            targetList.add(animation);
            activeAnimations.add(animation);
            scene.KEY_ANIMATIONS.add(animation);
        }
    }

    private void loadOptionalAnimationSet(GraphicsDevice graphicsDevice,
                                          Scene scene,
                                          Skeleton skeleton,
                                          String animationFile,
                                          float framesPerSecond,
                                          ArrayList<KeyframeAnimation> targetList,
                                          ArrayList<KeyframeAnimation> activeAnimations) {
        if (animationFile == null || animationFile.trim().isEmpty()) {
            log("optional animation asset missing path");
            return;
        }
        byte[] animBytes = graphicsDevice.loadBinaryResource(animationFile);
        if (animBytes == null || animBytes.length == 0) {
            log("optional animation asset missing=" + animationFile);
            return;
        }
        loadAnimationSet(graphicsDevice, scene, skeleton, animationFile, framesPerSecond, targetList, activeAnimations);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Bone> deserializeBoneList(byte[] boneBytes, String boneFile) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(boneBytes))) {
            Object value = inputStream.readObject();
            if (!(value instanceof ArrayList<?>)) {
                throw new IllegalStateException("Expected ArrayList in " + boneFile + " but found "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            ArrayList<?> list = (ArrayList<?>) value;
            ArrayList<Bone> bones = new ArrayList<>(list.size());
            for (Object entry : list) {
                if (!(entry instanceof Bone)) {
                    throw new IllegalStateException("Expected Bone entries in " + boneFile + " but found "
                            + (entry == null ? "null" : entry.getClass().getName()));
                }
                bones.add((Bone) entry);
            }
            return bones;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize bones: " + boneFile, e);
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

    private void normalizeAnimationTiming(KeyframeAnimation animation) {
        if (animation.keyframes == null || animation.keyframes.isEmpty()) {
            animation.duration = 0f;
            return;
        }
        float maxTime = 0f;
        for (KeyframeAnimation.Keyframe keyframe : animation.keyframes) {
            if (keyframe != null) {
                maxTime = Math.max(maxTime, keyframe.time);
            }
        }
        animation.duration = maxTime;
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
        return bones.get(0);
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

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }

    private String definitionAnimationAsset(BattleArenaCharacterDefinition definition, String key) {
        BattleArenaCharacterDefinition.AnimationDefinition animation = definitionAnimation(definition, key);
        return animation != null ? animation.assetPath() : null;
    }

    private float definitionAnimationFramesPerSecond(BattleArenaCharacterDefinition definition, String key) {
        BattleArenaCharacterDefinition.AnimationDefinition animation = definitionAnimation(definition, key);
        return animation != null ? animation.resolvedFramesPerSecond() : BattleArenaCharacterDefinition.DEFAULT_ANIMATION_FPS;
    }

    private BattleArenaCharacterDefinition.AnimationDefinition definitionAnimation(BattleArenaCharacterDefinition definition, String key) {
        if (definition == null || definition.animations == null) {
            return null;
        }
        return definition.animations.get(key);
    }
    private static final class AnimationAssetSpec {
        final String path;
        final float framesPerSecond;

        private AnimationAssetSpec(String path, float framesPerSecond) {
            this.path = path;
            this.framesPerSecond = framesPerSecond;
        }
    }
}
