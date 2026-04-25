package com.njst.gaming;

import com.njst.gaming.Math.Vector3;

public class Light {
    public final Vector3 position;
    public final Vector3 color;
    public float intensity;
    public float range;

    public Light(Vector3 position, Vector3 color, float intensity, float range) {
        this.position = position != null ? position : new Vector3(0f, 0f, 0f);
        this.color = color != null ? color : new Vector3(1f, 1f, 1f);
        this.intensity = intensity;
        this.range = range;
    }

    public static Light point(float x, float y, float z, float r, float g, float b, float intensity, float range) {
        return new Light(new Vector3(x, y, z), new Vector3(r, g, b), intensity, range);
    }
}
