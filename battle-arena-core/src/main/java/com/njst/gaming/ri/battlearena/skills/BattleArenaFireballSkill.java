package com.njst.gaming.ri.battlearena.skills;

import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.ri.battlearena.BattleArenaCharacterController;
import com.njst.gaming.ri.battlearena.BattleArenaCharacterRuntime;
import com.njst.gaming.ri.battlearena.BattleArenaControlledCharacter;
import com.njst.gaming.simulation.entities.Projectile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BattleArenaFireballSkill implements BattleArenaSkill {
    public static final String ID = "fireball";
    private static final float FIREBALL_RADIUS = 0.16f;
    private static final float FIREBALL_CHARGE_SECONDS = 0.4f;
    private static final float FIREBALL_MIN_SCALE = 0.2f;
    private static final float FIREBALL_MAX_SCALE = 1.1f;
    private static final float FIREBALL_SPEED = 10f;
    private static final float FIREBALL_SPAWN_FORWARD_OFFSET = 0.7f;
    private static final float FIREBALL_SPAWN_HEIGHT_OFFSET = 1.25f;
    private static final float FIREBALL_HIT_RADIUS = 0.7f;
    private static final float FIREBALL_THREAT_RANGE = 4.5f;
    private static final String LOG_PREFIX = "[BattleArena] ";

    private final List<ActiveProjectile> activeProjectiles = new ArrayList<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean run(BattleArenaControlledCharacter user,
                       BattleArenaSkillContext context,
                       BattleArenaCharacterRuntime target) {
        if (user == null || context == null || context.scene == null || target == null) {
            return false;
        }

        BattleArenaCharacterRuntime casterRuntime = user.runtime;
        Vector3 facing = facingDirection(casterRuntime.getHeadingDegrees());
        Vector3 start = fireballAnchorPosition(casterRuntime, facing);

        GameObject fireball = new GameObject(new SphereGeometry(FIREBALL_RADIUS, 12, 12), 0);
        fireball.name = casterRuntime.meshObject.name + "_Fireball";
        fireball.castsShadows = false;
        fireball.ambientlight_multiplier = 10f;
        fireball.shininess = 2f;
        fireball.setPosition(start.x, start.y, start.z);
        fireball.setScale(FIREBALL_MIN_SCALE, FIREBALL_MIN_SCALE, FIREBALL_MIN_SCALE);
        context.scene.addGameObject(fireball);

        Projectile projectile = new Projectile(fireball, new Vector3(0f, 0f, 0f), 0);
        projectile.lifetime = 1.8f;
        activeProjectiles.add(new ActiveProjectile(
                projectile,
                casterRuntime,
                target,
                FIREBALL_CHARGE_SECONDS,
                facing.clone()));
        log("spawned fireball owner=" + casterRuntime.meshObject.name
                + " start=" + start.x + "," + start.y + "," + start.z);
        return true;
    }

    void update(BattleArenaSkillContext context,
                float deltaSeconds,
                BattleArenaMudWallSkill mudWallSkill) {
        if (context == null || context.scene == null) {
            return;
        }
        Iterator<ActiveProjectile> iterator = activeProjectiles.iterator();
        while (iterator.hasNext()) {
            ActiveProjectile activeProjectile = iterator.next();
            updateProjectileState(activeProjectile, deltaSeconds);

            if (activeProjectile.launched && mudWallSkill != null && mudWallSkill.blocksProjectile(activeProjectile.projectile.obj.position, activeProjectile.owner)) {
                log("FIREBALL BLOCKED owner=" + activeProjectile.owner.meshObject.name
                        + " position=" + activeProjectile.projectile.obj.position);
                removeProjectile(context, activeProjectile.projectile);
                iterator.remove();
                continue;
            }

            if (activeProjectile.launched && hasProjectileHitTarget(activeProjectile)) {
                BattleArenaCharacterRuntime target = activeProjectile.target;
                target.onHitTaken(activeProjectile.owner, "torso", BattleArenaCharacterController.ANIM_TAKE_HIT);
                log("FIREBALL HIT "
                        + activeProjectile.owner.meshObject.name
                        + " -> "
                        + target.meshObject.name
                        + " position=" + activeProjectile.projectile.obj.position);
                removeProjectile(context, activeProjectile.projectile);
                iterator.remove();
                continue;
            }

            if (activeProjectile.projectile.isExpired()) {
                removeProjectile(context, activeProjectile.projectile);
                iterator.remove();
            }
        }
    }

    boolean hasIncomingThreat(BattleArenaCharacterRuntime self,
                              BattleArenaCharacterRuntime opponent,
                              BattleArenaMudWallSkill mudWallSkill) {
        if (self == null || opponent == null) {
            return false;
        }
        for (ActiveProjectile activeProjectile : activeProjectiles) {
            if (activeProjectile == null || !activeProjectile.launched) {
                continue;
            }
            if (activeProjectile.owner != opponent || activeProjectile.target != self) {
                continue;
            }
            if (mudWallSkill != null && mudWallSkill.hasBlockingWallBetween(activeProjectile.projectile.obj.position, self.getPosition(), self)) {
                continue;
            }
            Vector3 projectilePosition = activeProjectile.projectile.obj.position;
            Vector3 selfPosition = self.getPosition();
            float dx = selfPosition.x - projectilePosition.x;
            float dz = selfPosition.z - projectilePosition.z;
            float horizontalDistance = (float) Math.sqrt((dx * dx) + (dz * dz));
            if (horizontalDistance <= FIREBALL_THREAT_RANGE) {
                return true;
            }
        }
        return false;
    }

    private void updateProjectileState(ActiveProjectile activeProjectile, float deltaSeconds) {
        if (activeProjectile == null || activeProjectile.projectile == null || activeProjectile.projectile.obj == null) {
            return;
        }
        if (!activeProjectile.launched) {
            activeProjectile.chargeRemainingSeconds = Math.max(0f, activeProjectile.chargeRemainingSeconds - deltaSeconds);
            Vector3 facing = facingDirection(activeProjectile.owner.getHeadingDegrees());
            activeProjectile.launchDirection.set(facing.x, facing.y, facing.z);

            Vector3 anchorPosition = fireballAnchorPosition(activeProjectile.owner, facing);
            activeProjectile.projectile.obj.setPosition(anchorPosition.x, anchorPosition.y, anchorPosition.z);

            float progress = 1f - (activeProjectile.chargeRemainingSeconds / FIREBALL_CHARGE_SECONDS);
            float scale = lerp(FIREBALL_MIN_SCALE, FIREBALL_MAX_SCALE, clamp(progress, 0f, 1f));
            activeProjectile.projectile.obj.setScale(scale, scale, scale);
            activeProjectile.projectile.obj.updateModelMatrix();

            if (activeProjectile.chargeRemainingSeconds <= 0f) {
                activeProjectile.launched = true;
                activeProjectile.projectile.velocity = activeProjectile.launchDirection.clone().mul(FIREBALL_SPEED);
                log("launched fireball owner=" + activeProjectile.owner.meshObject.name
                        + " velocity=" + activeProjectile.projectile.velocity.x + ","
                        + activeProjectile.projectile.velocity.y + ","
                        + activeProjectile.projectile.velocity.z);
            }
            return;
        }

        activeProjectile.projectile.update(deltaSeconds);
    }

    private boolean hasProjectileHitTarget(ActiveProjectile activeProjectile) {
        if (activeProjectile == null || activeProjectile.target == null) {
            return false;
        }
        Vector3 projectilePosition = activeProjectile.projectile.obj.position;
        Vector3 targetPosition = activeProjectile.target.getPosition();
        float targetY = targetPosition.y + 1.0f;
        float dx = projectilePosition.x - targetPosition.x;
        float dy = projectilePosition.y - targetY;
        float dz = projectilePosition.z - targetPosition.z;
        return (dx * dx) + (dy * dy) + (dz * dz) <= FIREBALL_HIT_RADIUS * FIREBALL_HIT_RADIUS;
    }

    private void removeProjectile(BattleArenaSkillContext context, Projectile projectile) {
        if (context == null || context.scene == null || projectile == null || projectile.obj == null) {
            return;
        }
        context.scene.removeGameObject(projectile.obj);
        projectile.obj.cleanup();
    }

    private Vector3 fireballAnchorPosition(BattleArenaCharacterRuntime caster, Vector3 facing) {
        Vector3 start = caster.getPosition().clone().add(facing.clone().mul(FIREBALL_SPAWN_FORWARD_OFFSET));
        start.y += FIREBALL_SPAWN_HEIGHT_OFFSET;
        return start;
    }

    private Vector3 facingDirection(float headingDegrees) {
        float headingRadians = (float) Math.toRadians(headingDegrees);
        return new Vector3((float) Math.sin(headingRadians), 0f, (float) Math.cos(headingRadians));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float lerp(float a, float b, float t) {
        return a + ((b - a) * t);
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }

    static final class ActiveProjectile {
        final Projectile projectile;
        final BattleArenaCharacterRuntime owner;
        final BattleArenaCharacterRuntime target;
        final Vector3 launchDirection;
        float chargeRemainingSeconds;
        boolean launched;

        ActiveProjectile(Projectile projectile,
                         BattleArenaCharacterRuntime owner,
                         BattleArenaCharacterRuntime target,
                         float chargeRemainingSeconds,
                         Vector3 launchDirection) {
            this.projectile = projectile;
            this.owner = owner;
            this.target = target;
            this.chargeRemainingSeconds = chargeRemainingSeconds;
            this.launchDirection = launchDirection != null ? launchDirection : new Vector3(0f, 0f, 1f);
            this.launched = false;
        }
    }
}
