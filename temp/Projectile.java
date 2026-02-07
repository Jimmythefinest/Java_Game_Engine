package com.njst.game1;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

public class Projectile {
    public GameObject obj;
    public Vector3 velocity;
    public int team;
    public float lifetime = 5.0f; // Seconds before it disappears

    public Projectile(GameObject obj, Vector3 velocity, int team) {
        this.obj = obj;
        this.velocity = velocity;
        this.team = team;
    }

    public void update(float dt) {
        obj.position.add(velocity.clone().mul(dt));
        obj.updateModelMatrix();
        lifetime -= dt;
    }

    public boolean isExpired() {
        return lifetime <= 0;
    }
}
