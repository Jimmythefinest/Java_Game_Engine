package com.njst.gaming.simulation.entities;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;
import java.util.Random;
import java.util.List;

public class NPC {
    public enum State {
        WANDER, CHASE, RUN, HIDE
    }

    public interface ProjectileSpawner {
        void spawnProjectile(Vector3 pos, Vector3 vel, int team);
    }

    public GameObject obj;
    public Vector3 velocity = new Vector3();
    private Random rnd = new Random();

    // NPC Stats
    public int team;
    public int ammo = 3;
    public float ammoRechargeTimer = 0;
    public State state = State.WANDER;

    private static final float MAX_RADIUS = 25f;
    private static final float WANDER_STRENGTH = 0.05f;
    private static final float CHASE_SPEED = 0.3f;
    private static final float SPEED_LIMIT = 0.2f;
    private static final float RECHARGE_TIME = 10.0f;
    private static final float DETECTION_RANGE = 15.0f;
    private static final float SHOOT_RANGE = 10.0f;
    private static final float SHOOT_COOLDOWN = 1.0f;
    private float shootTimer = 0;

    public NPC(GameObject obj, int team) {
        this.obj = obj;
        this.team = team;
    }

    public void update(float dt, List<NPC> allNPCs, ProjectileSpawner spawner) {
        updateAmmo(dt);
        think(allNPCs);
        move(dt);
        combat(dt, allNPCs, spawner);

        obj.updateModelMatrix();
    }

    private void updateAmmo(float dt) {
        if (ammo < 3) {
            ammoRechargeTimer += dt;
            if (ammoRechargeTimer >= RECHARGE_TIME) {
                ammo++;
                ammoRechargeTimer = 0;
            }
        }
    }

    private void think(List<NPC> allNPCs) {
        NPC nearestEnemy = findNearestEnemy(allNPCs);
        float distToEnemy = nearestEnemy != null ? obj.position.clone().sub(nearestEnemy.obj.position).length()
                : Float.MAX_VALUE;

        if (ammo == 0) {
            state = (distToEnemy < DETECTION_RANGE) ? State.RUN : State.HIDE;
        } else {
            state = (distToEnemy < DETECTION_RANGE) ? State.CHASE : State.WANDER;
        }
    }

    private void move(float dt) {
        switch (state) {
            case WANDER:
                velocity.x += (rnd.nextFloat() - 0.5f) * WANDER_STRENGTH;
                velocity.z += (rnd.nextFloat() - 0.5f) * WANDER_STRENGTH;
                break;
            case CHASE:
                // Move toward logic could be added here
                break;
            case RUN:
                // Move away logic could be added here
                break;
            case HIDE:
                // Stay still logic could be added here
                break;
        }

        velocity.y = 0;
        float speed = velocity.length();
        if (speed > SPEED_LIMIT)
            velocity.normalize().mul(SPEED_LIMIT);

        obj.position.add(velocity.clone().mul(dt));

        // Radius Constraint
        float dist = obj.position.length();
        if (dist > MAX_RADIUS) {
            Vector3 push = obj.position.clone().mul(-1f);
            push.normalize().mul(0.1f);
            velocity.add(push);
        }
    }

    private void combat(float dt, List<NPC> allNPCs, ProjectileSpawner spawner) {
        shootTimer -= dt;
        if (state == State.CHASE && ammo > 0 && shootTimer <= 0) {
            NPC target = findNearestEnemy(allNPCs);
            if (target != null && obj.position.clone().sub(target.obj.position).length() < SHOOT_RANGE) {
                shoot(target, spawner);
                shootTimer = SHOOT_COOLDOWN;
            }
        }
    }

    private NPC findNearestEnemy(List<NPC> allNPCs) {
        NPC best = null;
        float bestDist = Float.MAX_VALUE;
        for (NPC other : allNPCs) {
            if (other.team == this.team)
                continue;
            float d = obj.position.clone().sub(other.obj.position).length();
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }
        return best;
    }

    private void shoot(NPC target, ProjectileSpawner spawner) {
        ammo--;
        Vector3 dir = target.obj.position.clone().sub(obj.position).normalize();
        spawner.spawnProjectile(obj.position.clone().add(dir.clone().mul(1.0f)), dir.mul(2.0f), team);
    }
}
