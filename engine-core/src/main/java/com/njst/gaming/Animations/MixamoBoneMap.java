package com.njst.gaming.Animations;

import java.util.ArrayList;
import java.util.Locale;

import com.njst.gaming.Bone;

public class MixamoBoneMap {
    public final Bone root;
    public final Bone hips;
    public final Bone spine;
    public final Bone chest;
    public final Bone neck;
    public final Bone head;
    public final Bone leftUpperArm;
    public final Bone leftLowerArm;
    public final Bone rightUpperArm;
    public final Bone rightLowerArm;
    public final Bone leftUpperLeg;
    public final Bone leftLowerLeg;
    public final Bone leftFoot;
    public final Bone rightUpperLeg;
    public final Bone rightLowerLeg;
    public final Bone rightFoot;

    private MixamoBoneMap(Bone root, Bone hips, Bone spine, Bone chest, Bone neck, Bone head,
            Bone leftUpperArm, Bone leftLowerArm, Bone rightUpperArm, Bone rightLowerArm,
            Bone leftUpperLeg, Bone leftLowerLeg, Bone leftFoot,
            Bone rightUpperLeg, Bone rightLowerLeg, Bone rightFoot) {
        this.root = root;
        this.hips = hips;
        this.spine = spine;
        this.chest = chest;
        this.neck = neck;
        this.head = head;
        this.leftUpperArm = leftUpperArm;
        this.leftLowerArm = leftLowerArm;
        this.rightUpperArm = rightUpperArm;
        this.rightLowerArm = rightLowerArm;
        this.leftUpperLeg = leftUpperLeg;
        this.leftLowerLeg = leftLowerLeg;
        this.leftFoot = leftFoot;
        this.rightUpperLeg = rightUpperLeg;
        this.rightLowerLeg = rightLowerLeg;
        this.rightFoot = rightFoot;
    }

    public static MixamoBoneMap resolve(ArrayList<Bone> bones, Bone rootBone) {
        Bone hips = find(bones, "hips");
        Bone spine = firstNonNull(find(bones, "spine"), hips);
        Bone chest = firstNonNull(find(bones, "spine2"), find(bones, "spine1"), spine);
        Bone neck = firstNonNull(find(bones, "neck"), chest);
        Bone head = firstNonNull(find(bones, "head"), neck);
        Bone leftUpperArm = find(bones, "leftarm");
        Bone leftLowerArm = find(bones, "leftforearm");
        Bone rightUpperArm = find(bones, "rightarm");
        Bone rightLowerArm = find(bones, "rightforearm");
        Bone leftUpperLeg = find(bones, "leftupleg");
        Bone leftLowerLeg = find(bones, "leftleg");
        Bone leftFoot = find(bones, "leftfoot");
        Bone rightUpperLeg = find(bones, "rightupleg");
        Bone rightLowerLeg = find(bones, "rightleg");
        Bone rightFoot = find(bones, "rightfoot");

        if (hips == null || head == null || leftUpperArm == null || rightUpperArm == null
                || leftUpperLeg == null || rightUpperLeg == null || leftLowerLeg == null || rightLowerLeg == null) {
            return null;
        }

        return new MixamoBoneMap(rootBone, hips, spine, chest, neck, head,
                leftUpperArm, leftLowerArm, rightUpperArm, rightLowerArm,
                leftUpperLeg, leftLowerLeg, leftFoot, rightUpperLeg, rightLowerLeg, rightFoot);
    }

    private static Bone find(ArrayList<Bone> bones, String token) {
        for (Bone bone : bones) {
            if (normalize(bone.name).contains(token)) {
                return bone;
            }
        }
        return null;
    }

    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replace("mixamorig:", "").replace("_", "");
    }

    private static Bone firstNonNull(Bone... candidates) {
        for (Bone candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
