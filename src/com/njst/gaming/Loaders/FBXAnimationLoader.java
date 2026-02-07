package com.njst.gaming.Loaders;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Animations.KeyframeAnimation.Keyframe;
import com.njst.gaming.Math.Vector3;

import java.util.*;

/**
 * Utility class for loading and extracting animations from FBX files using the
 * Assimp library.
 * This loader processes FBX animation data and converts it into
 * KeyframeAnimation objects
 * that can be used by the game engine's animation system.
 * 
 * @author NJST Gaming
 * @version 1.0
 */
public class FBXAnimationLoader {

    /**
     * Extracts animation data from an FBX file at the specified path.
     * This method loads a specific animation by ID and converts position and
     * rotation keyframes
     * into a format usable by the game engine.
     * 
     * @param path  The file path to the FBX file containing animations
     * @param id    The index of the animation to extract from the FBX file
     * @param scale The scale factor to apply to position values (used to normalize
     *              coordinates)
     * @return A map of bone names to their corresponding KeyframeAnimation objects
     * @throws RuntimeException if the FBX file cannot be loaded or contains no
     *                          animations
     */
    public static Map<String, KeyframeAnimation> extractAnimation(String path, int id, float scale) {
        Map<String, KeyframeAnimation> animationData = new HashMap<>();

        AIScene scene = Assimp.aiImportFile(path,
                Assimp.aiProcess_Triangulate | Assimp.aiProcess_LimitBoneWeights);
        if (scene == null || scene.mAnimations() == null) {
            throw new RuntimeException("Failed to load FBX or no animations present: " + path);
        }

        PointerBuffer anims = scene.mAnimations();

        // for (int i = 0; i < numAnimations&&i<1; i++) {
        AIAnimation aiAnim = AIAnimation.create(anims.get(id));
        PointerBuffer channels = aiAnim.mChannels();
        System.out.println(aiAnim.mName().dataString());

        for (int j = 0; j < aiAnim.mNumChannels(); j++) {
            AINodeAnim channel = AINodeAnim.create(channels.get(j));
            String boneName = channel.mNodeName().dataString();
            KeyframeAnimation keyframes = new KeyframeAnimation(null);
            HashMap<Float, Keyframe> frames = new HashMap<>();

            AIVectorKey.Buffer poskeys = channel.mPositionKeys();
            for (int k = 0; k < channel.mNumPositionKeys(); k++) {
                AIVectorKey key = poskeys.get(k);
                Vector3 position = new Vector3(
                        key.mValue().x() / scale,
                        key.mValue().y() / scale,
                        key.mValue().z() / scale);
                if (frames.containsKey((float) key.mTime() * 10.0f)) {
                    frames.get((float) key.mTime() * 10).position = position;
                } else {
                    frames.put((float) key.mTime() * 10,
                            new Keyframe((float) key.mTime() * 5, position, new Vector3()));
                }
            }

            AIQuatKey.Buffer rotkeys = channel.mRotationKeys();
            for (int k = 0; k < channel.mNumPositionKeys(); k++) {
                AIQuatKey key = rotkeys.get(k);
                Vector3 rotation = new Vector3(quaternionToEulerXYZDegrees(
                        key.mValue().x(),
                        key.mValue().y(),
                        key.mValue().z(),
                        key.mValue().w()));
                if (frames.containsKey((float) key.mTime() * 10)) {
                    frames.get((float) key.mTime() * 10).rotation = rotation;
                } else {
                    frames.put((float) key.mTime() * 10, new Keyframe((float) key.mTime(), new Vector3(), rotation));
                }
            }

            frames.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(frame -> {
                keyframes.keyframes.add(frame.getValue());
            });

            animationData.put(boneName.replace(":", "").replace("_", ""), keyframes);

        }

        Assimp.aiReleaseImport(scene);
        return animationData;
    }

    /**
     * Converts a quaternion rotation to Euler angles in XYZ order (degrees).
     * This is used to convert rotation data from FBX files (which use quaternions)
     * into Euler angles that are easier to work with in the animation system.
     * 
     * @param x The X component of the quaternion
     * @param y The Y component of the quaternion
     * @param z The Z component of the quaternion
     * @param w The W (scalar) component of the quaternion
     * @return A float array containing [roll, pitch, yaw] in degrees
     */
    public static float[] quaternionToEulerXYZDegrees(float x, float y, float z, float w) {
        float[] euler = new float[3];

        // Roll (X-axis rotation)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        euler[0] = (float) Math.toDegrees(Math.atan2(sinr_cosp, cosr_cosp)); // X (roll)

        // Pitch (Y-axis rotation)
        double sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1)
            euler[1] = (float) Math.toDegrees(Math.copySign(Math.PI / 2, sinp)); // Y (pitch)
        else
            euler[1] = (float) Math.toDegrees(Math.asin(sinp));

        // Yaw (Z-axis rotation)
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        euler[2] = (float) Math.toDegrees(Math.atan2(siny_cosp, cosy_cosp)); // Z (yaw)
        // float[] t={euler[0],euler[2],euler[1]};
        return euler;
    }

}