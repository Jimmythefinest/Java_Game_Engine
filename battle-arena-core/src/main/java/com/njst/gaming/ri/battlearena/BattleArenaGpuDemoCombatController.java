package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.collision.CollisionEventType;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.ri.battlearena.gameobjects.BattleArenaPlayerHealthBarGameObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGpuDemoCombatController implements BattleArenaPlayerStatusSource {
    private static final float MAX_HEALTH = 100f;
    private static final float HIT_DAMAGE = 10f;
    private static final float HIT_KNOCKBACK_DISTANCE = 0.45f;
    private static final int HIT_COOLDOWN_TICKS = 30;
    private static final float HEALTH_BAR_WIDTH = 0.9f;
    private static final float HEALTH_BAR_HEIGHT = 0.12f;
    private static final float HEALTH_BAR_VERTICAL_OFFSET = 2.15f;

    private final BattleArenaPlayerStateProvider stateProvider;
    private final BattleArenaHitboxTracks hitboxTracks;
    private final Map<String, BattleArenaPlayerState> statesByPlayer =
            new LinkedHashMap<String, BattleArenaPlayerState>();
    private final Map<String, Float> healthByPlayer = new LinkedHashMap<String, Float>();
    private final Map<String, Integer> nextAllowedHitTickByPair = new LinkedHashMap<String, Integer>();

    BattleArenaGpuDemoCombatController(BattleArenaPlayerStateProvider stateProvider,
                                       GraphicsDevice graphicsDevice,
                                       BattleArenaCharacterDefinition definition) {
        this.stateProvider = stateProvider;
        this.hitboxTracks = BattleArenaHitboxTracks.load(graphicsDevice, resolveHitboxTrackPath(definition));
    }

    void register(Scene scene,
                  BattleArenaCharacterDefinition definition,
                  List<BattleArenaPlayerState> initialStates) {
        if (scene == null || definition == null || initialStates == null) {
            return;
        }
        for (BattleArenaPlayerState playerState : initialStates) {
            if (playerState == null) {
                continue;
            }
            healthByPlayer.put(playerState.playerId, MAX_HEALTH);
            registerHitboxes(scene, definition, playerState.playerId);
            scene.addGameObject(new BattleArenaPlayerHealthBarGameObject(
                    this,
                    scene.renderer.camera,
                    playerState.playerId,
                    "BattleArena_" + playerState.playerId + "_HealthBar",
                    HEALTH_BAR_WIDTH,
                    HEALTH_BAR_HEIGHT,
                    HEALTH_BAR_VERTICAL_OFFSET));
        }
        scene.getCollisionWorld().addListener(this::handleCollision);
    }

    void updateStates(List<BattleArenaPlayerState> playerStates) {
        statesByPlayer.clear();
        if (playerStates == null) {
            return;
        }
        for (BattleArenaPlayerState state : playerStates) {
            if (state != null) {
                statesByPlayer.put(state.playerId, state);
            }
        }
    }

    BattleArenaPlayerState stateForPlayer(String playerId) {
        return statesByPlayer.get(playerId);
    }

    @Override
    public float healthRatio(String playerId) {
        Float health = healthByPlayer.get(playerId);
        if (health == null || MAX_HEALTH <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, health.floatValue() / MAX_HEALTH));
    }

    @Override
    public Vector3 positionForPlayer(String playerId) {
        BattleArenaPlayerState state = statesByPlayer.get(playerId);
        if (state == null) {
            return new Vector3();
        }
        return new Vector3(state.x, state.y, state.z);
    }

    Vector3 resolveWorldCenter(BattleArenaGpuDemoHitboxCollider collider,
                               Vector3 fallbackRootRelativeCenter) {
        BattleArenaPlayerState state = statesByPlayer.get(collider.playerId());
        Vector3 rootRelative = null;
        if (state != null && hitboxTracks != null) {
            rootRelative = hitboxTracks.sampleCenter(state.animationKey, state.animationFrame, collider.name());
        }
        if (rootRelative == null) {
            rootRelative = new Vector3(fallbackRootRelativeCenter);
        }
        if (state == null) {
            return rootRelative;
        }
        float headingRadians = (float) Math.toRadians(state.headingDegrees);
        float forwardX = (float) Math.sin(headingRadians);
        float forwardZ = (float) Math.cos(headingRadians);
        float rightX = (float) Math.cos(headingRadians);
        float rightZ = -(float) Math.sin(headingRadians);
        return new Vector3(
                state.x + (rightX * rootRelative.x) + (forwardX * rootRelative.z),
                state.y + rootRelative.y,
                state.z + (rightZ * rootRelative.x) + (forwardZ * rootRelative.z));
    }

    boolean isBoxActive(BattleArenaGpuDemoHitboxCollider collider, boolean fallbackActive) {
        BattleArenaPlayerState state = statesByPlayer.get(collider.playerId());
        if (state == null || hitboxTracks == null) {
            return fallbackActive;
        }
        return hitboxTracks.isActive(state.animationKey, state.animationFrame, collider.name(), fallbackActive);
    }

    private void registerHitboxes(Scene scene,
                                  BattleArenaCharacterDefinition definition,
                                  String playerId) {
        if (definition.hitboxes == null) {
            return;
        }
        for (Map.Entry<String, BattleArenaCharacterDefinition.HitboxDefinition> entry
                : definition.hitboxes.entrySet()) {
            BattleArenaCharacterDefinition.HitboxDefinition box = entry.getValue();
            if (box == null || box.halfExtents == null || box.halfExtents.length < 3) {
                continue;
            }
            Vector3 center = box.center != null && box.center.length >= 3
                    ? new Vector3(box.center)
                    : new Vector3();
            Vector3 halfExtents = new Vector3(box.halfExtents);
            BattleArenaGpuDemoHitboxCollider.Type type =
                    BattleArenaCharacterDefinition.HITBOX_KIND_HITBOX.equals(box.kind)
                            ? BattleArenaGpuDemoHitboxCollider.Type.HITBOX
                            : BattleArenaGpuDemoHitboxCollider.Type.HURTBOX;
            scene.getCollisionWorld().addCollider(new BattleArenaGpuDemoHitboxCollider(
                    this,
                    playerId,
                    type,
                    entry.getKey(),
                    box.activeWhen,
                    box.onHitAnimation,
                    center,
                    halfExtents));
        }
    }

    private void handleCollision(CollisionEvent event) {
        if (event == null || event.getType() != CollisionEventType.ENTER) {
            return;
        }
        if (!(event.getFirst() instanceof BattleArenaGpuDemoHitboxCollider)
                || !(event.getSecond() instanceof BattleArenaGpuDemoHitboxCollider)) {
            return;
        }
        BattleArenaGpuDemoHitboxCollider first = (BattleArenaGpuDemoHitboxCollider) event.getFirst();
        BattleArenaGpuDemoHitboxCollider second = (BattleArenaGpuDemoHitboxCollider) event.getSecond();
        if (first.type() == BattleArenaGpuDemoHitboxCollider.Type.HITBOX
                && second.type() == BattleArenaGpuDemoHitboxCollider.Type.HURTBOX) {
            applyHit(first, second);
        } else if (second.type() == BattleArenaGpuDemoHitboxCollider.Type.HITBOX
                && first.type() == BattleArenaGpuDemoHitboxCollider.Type.HURTBOX) {
            applyHit(second, first);
        }
    }

    private void applyHit(BattleArenaGpuDemoHitboxCollider attacker,
                          BattleArenaGpuDemoHitboxCollider defender) {
        Float currentHealth = healthByPlayer.get(defender.playerId());
        if (currentHealth == null) {
            return;
        }
        String hitKey = attacker.playerId() + "->" + defender.playerId();
        Integer nextAllowedTick = nextAllowedHitTickByPair.get(hitKey);
        if (nextAllowedTick != null && stateProvider.currentTick() < nextAllowedTick.intValue()) {
            return;
        }
        nextAllowedHitTickByPair.put(hitKey, stateProvider.currentTick() + HIT_COOLDOWN_TICKS);
        if (currentHealth.floatValue() > 0f) {
            healthByPlayer.put(defender.playerId(), Math.max(0f, currentHealth.floatValue() - HIT_DAMAGE));
        }
        BattleArenaPlayerInput input = new BattleArenaPlayerInput();
        input.takeHitPressed = true;
        applyKnockback(attacker, defender, input);
        if (defender.onHitAnimation() != null && !defender.onHitAnimation().trim().isEmpty()) {
            input.animationOverride = defender.onHitAnimation();
        }
        stateProvider.submitInput(defender.playerId(), stateProvider.currentTick() + 1, input);
        BattleArenaDemoLoader.log("GPU demo HIT "
                + attacker.playerId()
                + " -> "
                + defender.playerId()
                + " hurtbox="
                + defender.name()
                + " health="
                + healthByPlayer.get(defender.playerId()));
    }

    private void applyKnockback(BattleArenaGpuDemoHitboxCollider attacker,
                                BattleArenaGpuDemoHitboxCollider defender,
                                BattleArenaPlayerInput input) {
        BattleArenaPlayerState attackerState = statesByPlayer.get(attacker.playerId());
        BattleArenaPlayerState defenderState = statesByPlayer.get(defender.playerId());
        if (attackerState == null || defenderState == null) {
            return;
        }
        float dx = defenderState.x - attackerState.x;
        float dz = defenderState.z - attackerState.z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001f) {
            float headingRadians = (float) Math.toRadians(attackerState.headingDegrees);
            dx = (float) Math.sin(headingRadians);
            dz = (float) Math.cos(headingRadians);
            length = 1f;
        }
        input.knockbackX = (dx / length) * HIT_KNOCKBACK_DISTANCE;
        input.knockbackZ = (dz / length) * HIT_KNOCKBACK_DISTANCE;
    }

    private String resolveHitboxTrackPath(BattleArenaCharacterDefinition definition) {
        if (definition != null && definition.hitboxTracks != null && !definition.hitboxTracks.trim().isEmpty()) {
            return definition.hitboxTracks;
        }
        return "battle_arena/defeated.hitbox_tracks.json";
    }
}
