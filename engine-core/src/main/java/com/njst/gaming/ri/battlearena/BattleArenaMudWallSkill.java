package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.Bounds3;
import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.collision.CollisionEventType;
import com.njst.gaming.objects.GameObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class BattleArenaMudWallSkill implements BattleArenaSkill {
    static final String ID = "mud_wall";
    private static final float MUD_WALL_WIDTH = 1.8f;
    private static final float MUD_WALL_HEIGHT = 1.35f;
    private static final float MUD_WALL_DEPTH = 0.6f;
    private static final float MUD_WALL_RISE_SECONDS = 0.45f;
    private static final float MUD_WALL_LIFETIME_SECONDS = 3.5f;
    private static final float MUD_WALL_FORWARD_OFFSET = 1.8f;
    private static final float MUD_WALL_PUSH_PADDING = 0.02f;
    private static final float FIREBALL_RADIUS = 0.16f;
    private static final String LOG_PREFIX = "[BattleArena] ";

    private final List<ActiveMudWall> activeMudWalls = new ArrayList<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean run(BattleArenaControlledCharacter user,
                       BattleArenaSkillContext context,
                       BattleArenaCharacterRuntime target) {
        if (context == null || context.scene == null || user == null) {
            return false;
        }
        BattleArenaCharacterRuntime casterRuntime = user.runtime;
        Vector3 facing = facingDirection(casterRuntime.getHeadingDegrees());
        Vector3 position = casterRuntime.getPosition().clone().add(facing.clone().mul(MUD_WALL_FORWARD_OFFSET));
        float groundY = context.sampleTerrainHeight(position.x, position.z);

        GameObject mudWall = new GameObject(new CubeGeometry(), 0);
        mudWall.name = casterRuntime.meshObject.name + "_MudWall";
        mudWall.castsShadows = false;
        mudWall.ambientlight_multiplier = 1.4f;
        mudWall.shininess = 1f;
        mudWall.setPosition(position.x, groundY, position.z);
        mudWall.setScale(MUD_WALL_WIDTH, 0.01f, MUD_WALL_DEPTH);
        mudWall.setRotation(0f, casterRuntime.getHeadingDegrees(), 0f);
        context.scene.addGameObject(mudWall);

        BattleArenaMudWallCollider mudWallCollider = new BattleArenaMudWallCollider(mudWall);
        context.scene.getCollisionWorld().addCollider(mudWallCollider);
        BattleArenaMudWallDebugGameObject debugObject = new BattleArenaMudWallDebugGameObject(mudWallCollider);
        debugObject.setEnabled(context.debugEnabled);
        context.scene.addGameObject(debugObject);
        activeMudWalls.add(new ActiveMudWall(mudWall, mudWallCollider, debugObject, casterRuntime, groundY));
        log("spawned mud wall owner=" + casterRuntime.meshObject.name
                + " at=" + position.x + "," + groundY + "," + position.z);
        return true;
    }

    @Override
    public void onCollision(BattleArenaSkillContext context, CollisionEvent event) {
        if (event == null) {
            return;
        }
        if (event.getType() != CollisionEventType.ENTER && event.getType() != CollisionEventType.STAY) {
            return;
        }
        if (event.getFirst() instanceof BattleArenaMudWallCollider
                && event.getSecond() instanceof BattleArenaHitboxCollider) {
            resolveMudWallCollision(
                    (BattleArenaMudWallCollider) event.getFirst(),
                    (BattleArenaHitboxCollider) event.getSecond(),
                    event.getManifold().getPenetrationDepth());
            return;
        }
        if (event.getSecond() instanceof BattleArenaMudWallCollider
                && event.getFirst() instanceof BattleArenaHitboxCollider) {
            resolveMudWallCollision(
                    (BattleArenaMudWallCollider) event.getSecond(),
                    (BattleArenaHitboxCollider) event.getFirst(),
                    event.getManifold().getPenetrationDepth());
        }
    }

    @Override
    public void update(BattleArenaSkillContext context, float deltaSeconds) {
        if (context == null || context.scene == null) {
            return;
        }
        Iterator<ActiveMudWall> iterator = activeMudWalls.iterator();
        while (iterator.hasNext()) {
            ActiveMudWall mudWall = iterator.next();
            mudWall.elapsedSeconds += deltaSeconds;

            if (mudWall.elapsedSeconds <= MUD_WALL_RISE_SECONDS) {
                float riseProgress = clamp(mudWall.elapsedSeconds / MUD_WALL_RISE_SECONDS, 0f, 1f);
                float currentHeight = lerp(0.01f, MUD_WALL_HEIGHT, riseProgress);
                mudWall.obj.setScale(MUD_WALL_WIDTH, currentHeight, MUD_WALL_DEPTH);
                mudWall.obj.setPosition(
                        mudWall.obj.position.x,
                        mudWall.groundY + (currentHeight * 0.5f),
                        mudWall.obj.position.z);
                mudWall.obj.updateModelMatrix();
                continue;
            }

            if (mudWall.elapsedSeconds >= MUD_WALL_LIFETIME_SECONDS) {
                context.scene.getCollisionWorld().removeCollider(mudWall.collider);
                context.scene.removeGameObject(mudWall.obj);
                context.scene.removeGameObject(mudWall.debugObject);
                mudWall.debugObject.cleanup();
                mudWall.obj.cleanup();
                iterator.remove();
            }
        }
    }

    boolean blocksProjectile(Vector3 projectilePosition, BattleArenaCharacterRuntime projectileOwner) {
        if (projectilePosition == null) {
            return false;
        }
        for (ActiveMudWall mudWall : activeMudWalls) {
            if (mudWall == null || mudWall.collider == null || mudWall.owner == projectileOwner) {
                continue;
            }
            Bounds3 bounds = mudWall.collider.getWorldBounds();
            Vector3 min = bounds.getMin();
            Vector3 max = bounds.getMax();
            if (projectilePosition.x < (min.x - FIREBALL_RADIUS) || projectilePosition.x > (max.x + FIREBALL_RADIUS)) {
                continue;
            }
            if (projectilePosition.y < (min.y - FIREBALL_RADIUS) || projectilePosition.y > (max.y + FIREBALL_RADIUS)) {
                continue;
            }
            if (projectilePosition.z < (min.z - FIREBALL_RADIUS) || projectilePosition.z > (max.z + FIREBALL_RADIUS)) {
                continue;
            }
            return true;
        }
        return false;
    }

    boolean hasBlockingWallBetween(Vector3 start, Vector3 target, BattleArenaCharacterRuntime wallOwner) {
        if (start == null || target == null) {
            return false;
        }
        float minX = Math.min(start.x, target.x);
        float maxX = Math.max(start.x, target.x);
        float minZ = Math.min(start.z, target.z);
        float maxZ = Math.max(start.z, target.z);

        for (ActiveMudWall mudWall : activeMudWalls) {
            if (mudWall == null || mudWall.collider == null || mudWall.owner != wallOwner) {
                continue;
            }
            Bounds3 bounds = mudWall.collider.getWorldBounds();
            Vector3 wallMin = bounds.getMin();
            Vector3 wallMax = bounds.getMax();
            boolean overlapsPathX = wallMax.x >= minX && wallMin.x <= maxX;
            boolean overlapsPathZ = wallMax.z >= minZ && wallMin.z <= maxZ;
            if (overlapsPathX && overlapsPathZ) {
                return true;
            }
        }
        return false;
    }

    void setDebugVisible(boolean debugVisible) {
        for (ActiveMudWall mudWall : activeMudWalls) {
            if (mudWall != null && mudWall.debugObject != null) {
                mudWall.debugObject.setEnabled(debugVisible);
            }
        }
    }

    private void resolveMudWallCollision(BattleArenaMudWallCollider mudWall,
                                         BattleArenaHitboxCollider hitbox,
                                         float penetrationDepth) {
        if (mudWall == null || hitbox == null || hitbox.getType() != BattleArenaHitboxCollider.Type.HURTBOX) {
            return;
        }
        if (!"torso".equals(hitbox.getName()) || penetrationDepth <= 0f) {
            return;
        }

        BattleArenaCharacterRuntime character = hitbox.getCharacter();
        if (character == null) {
            return;
        }

        Bounds3 wallBounds = mudWall.getWorldBounds();
        Vector3 wallMin = wallBounds.getMin();
        Vector3 wallMax = wallBounds.getMax();
        Vector3 position = character.getPosition();

        float pushLeft = position.x - wallMin.x;
        float pushRight = wallMax.x - position.x;
        float pushBack = position.z - wallMin.z;
        float pushForward = wallMax.z - position.z;

        float smallestPush = pushLeft;
        int axis = 0;
        float direction = -1f;

        if (pushRight < smallestPush) {
            smallestPush = pushRight;
            axis = 0;
            direction = 1f;
        }
        if (pushBack < smallestPush) {
            smallestPush = pushBack;
            axis = 1;
            direction = -1f;
        }
        if (pushForward < smallestPush) {
            smallestPush = pushForward;
            axis = 1;
            direction = 1f;
        }

        float correction = Math.max(smallestPush, penetrationDepth) + MUD_WALL_PUSH_PADDING;
        if (axis == 0) {
            position.x += direction * correction;
            logMudWallCollision(mudWall, character, hitbox, penetrationDepth, correction, "x");
            return;
        }
        position.z += direction * correction;
        logMudWallCollision(mudWall, character, hitbox, penetrationDepth, correction, "z");
    }

    private void logMudWallCollision(BattleArenaMudWallCollider mudWall,
                                     BattleArenaCharacterRuntime character,
                                     BattleArenaHitboxCollider hitbox,
                                     float penetrationDepth,
                                     float correction,
                                     String axis) {
        Object owner = mudWall.getOwner();
        String wallName = owner instanceof GameObject ? ((GameObject) owner).name : "MudWall";
        Vector3 position = character.getPosition();
        log("MUD WALL COLLISION wall=" + wallName
                + " character=" + character.meshObject.name
                + " hitbox=" + hitbox.getName()
                + " axis=" + axis
                + " penetration=" + penetrationDepth
                + " correction=" + correction
                + " position=" + position.x + "," + position.y + "," + position.z);
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

    static final class ActiveMudWall {
        final GameObject obj;
        final BattleArenaMudWallCollider collider;
        final BattleArenaMudWallDebugGameObject debugObject;
        final BattleArenaCharacterRuntime owner;
        final float groundY;
        float elapsedSeconds;

        ActiveMudWall(GameObject obj,
                      BattleArenaMudWallCollider collider,
                      BattleArenaMudWallDebugGameObject debugObject,
                      BattleArenaCharacterRuntime owner,
                      float groundY) {
            this.obj = obj;
            this.collider = collider;
            this.debugObject = debugObject;
            this.owner = owner;
            this.groundY = groundY;
            this.elapsedSeconds = 0f;
        }
    }
}
