package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleArenaHeadlessCombatSystem {
    private static final float MAX_HEALTH = 100f;
    private static final float HIT_DAMAGE = 10f;
    private static final float HOT_GAS_DAMAGE = 8f;
    private static final float MOLTEN_EARTH_DAMAGE = 16f;
    private static final float HIT_KNOCKBACK_DISTANCE = 0.45f;
    private static final int HIT_COOLDOWN_TICKS = 30;
    private static final int GU_HIT_COOLDOWN_TICKS = 18;
    private static final String DEFAULT_HITBOX_TRACKS = "battle_arena/defeated.hitbox_tracks.json";

    private final BattleArenaHitboxTracks hitboxTracks;
    private final Map<String, Float> healthByPlayer = new LinkedHashMap<String, Float>();
    private final Map<String, Integer> nextAllowedHitTickByPair = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> nextAllowedGuHitTickByPair = new LinkedHashMap<String, Integer>();

    BattleArenaHeadlessCombatSystem() {
        this(BattleArenaHitboxTracks.loadFromResource(DEFAULT_HITBOX_TRACKS));
    }

    BattleArenaHeadlessCombatSystem(BattleArenaHitboxTracks hitboxTracks) {
        this.hitboxTracks = hitboxTracks;
    }

    void ensurePlayers(List<BattleArenaPlayerState> players) {
        if (players == null) {
            return;
        }
        for (BattleArenaPlayerState player : players) {
            if (player != null && !healthByPlayer.containsKey(player.playerId)) {
                healthByPlayer.put(player.playerId, MAX_HEALTH);
            }
        }
    }

    void update(int tick,
                List<BattleArenaPlayerState> players,
                BattleArenaPlayerStateProvider inputTarget) {
        ensurePlayers(players);
        if (hitboxTracks == null || hitboxTracks.boxes == null || players == null || inputTarget == null) {
            return;
        }
        for (BattleArenaPlayerState attacker : players) {
            if (attacker == null) {
                continue;
            }
            for (BattleArenaHitboxTracks.BoxDefinitionTrack hitbox : hitboxTracks.boxes) {
                if (!isHitbox(attacker, hitbox)) {
                    continue;
                }
                Aabb hitBounds = resolveBounds(attacker, hitbox);
                if (hitBounds == null) {
                    continue;
                }
                for (BattleArenaPlayerState defender : players) {
                    if (defender == null || attacker.playerId.equals(defender.playerId)) {
                        continue;
                    }
                    for (BattleArenaHitboxTracks.BoxDefinitionTrack hurtbox : hitboxTracks.boxes) {
                        if (!isHurtbox(defender, hurtbox)) {
                            continue;
                        }
                        Aabb hurtBounds = resolveBounds(defender, hurtbox);
                        if (hurtBounds != null && hitBounds.overlaps(hurtBounds)) {
                            applyHit(tick, attacker, defender, hurtbox, inputTarget);
                            break;
                        }
                    }
                }
            }
        }
    }

    void updateGuObjectDamage(int tick,
                              List<BattleArenaPlayerState> players,
                              List<BattleArenaGuObjectState> guObjects,
                              BattleArenaPlayerStateProvider inputTarget) {
        ensurePlayers(players);
        if (players == null || guObjects == null || guObjects.isEmpty() || inputTarget == null) {
            return;
        }
        for (BattleArenaGuObjectState guObject : guObjects) {
            if (!isDamagingGuObject(guObject)) {
                continue;
            }
            Aabb guBounds = resolveGuBounds(guObject);
            for (BattleArenaPlayerState defender : players) {
                if (defender == null || defender.playerId.equals(guObject.ownerPlayerId)) {
                    continue;
                }
                if (guBoundsOverlapsPlayerHurtbox(guBounds, defender)) {
                    applyGuHit(tick, guObject, defender, inputTarget);
                }
            }
        }
    }

    List<BattleArenaPlayerState> attachHealth(List<BattleArenaPlayerState> players) {
        java.util.ArrayList<BattleArenaPlayerState> result =
                new java.util.ArrayList<BattleArenaPlayerState>();
        if (players == null) {
            return result;
        }
        ensurePlayers(players);
        for (BattleArenaPlayerState player : players) {
            if (player == null) {
                continue;
            }
            Float health = healthByPlayer.get(player.playerId);
            result.add(new BattleArenaPlayerState(
                    player.playerId,
                    player.x,
                    player.y,
                    player.z,
                    player.headingDegrees,
                    player.animationKey,
                    player.animationFrame,
                    player.velocityX,
                    player.velocityZ,
                    player.strength,
                    health != null ? health.floatValue() : MAX_HEALTH,
                    MAX_HEALTH));
        }
        return result;
    }

    private boolean isHitbox(BattleArenaPlayerState state, BattleArenaHitboxTracks.BoxDefinitionTrack box) {
        return box != null
                && BattleArenaCharacterDefinition.HITBOX_KIND_HITBOX.equals(box.kind)
                && isBoxActive(state, box, false);
    }

    private boolean isHurtbox(BattleArenaPlayerState state, BattleArenaHitboxTracks.BoxDefinitionTrack box) {
        return box != null
                && !BattleArenaCharacterDefinition.HITBOX_KIND_HITBOX.equals(box.kind)
                && isBoxActive(state, box, true);
    }

    private boolean isBoxActive(BattleArenaPlayerState state,
                                BattleArenaHitboxTracks.BoxDefinitionTrack box,
                                boolean fallbackActive) {
        boolean active = fallbackActive;
        if (box.activeWhen != null && !box.activeWhen.trim().isEmpty()) {
            active = box.activeWhen.equals(state.animationKey);
        }
        return hitboxTracks.isActive(state.animationKey, state.animationFrame, box.name, active);
    }

    private Aabb resolveBounds(BattleArenaPlayerState state, BattleArenaHitboxTracks.BoxDefinitionTrack box) {
        if (state == null || box == null || box.halfExtents == null || box.halfExtents.length < 3) {
            return null;
        }
        Vector3 center = hitboxTracks.sampleCenter(state.animationKey, state.animationFrame, box.name);
        if (center == null) {
            center = new Vector3();
        }
        Vector3 world = toWorld(state, center);
        Vector3 half = new Vector3(box.halfExtents);
        return new Aabb(
                world.x - half.x,
                world.y - half.y,
                world.z - half.z,
                world.x + half.x,
                world.y + half.y,
                world.z + half.z);
    }

    private Vector3 toWorld(BattleArenaPlayerState state, Vector3 rootRelative) {
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

    private void applyHit(int tick,
                          BattleArenaPlayerState attacker,
                          BattleArenaPlayerState defender,
                          BattleArenaHitboxTracks.BoxDefinitionTrack hurtbox,
                          BattleArenaPlayerStateProvider inputTarget) {
        String hitKey = attacker.playerId + "->" + defender.playerId;
        Integer nextAllowedTick = nextAllowedHitTickByPair.get(hitKey);
        if (nextAllowedTick != null && tick < nextAllowedTick.intValue()) {
            return;
        }
        nextAllowedHitTickByPair.put(hitKey, tick + HIT_COOLDOWN_TICKS);
        Float currentHealth = healthByPlayer.get(defender.playerId);
        if (currentHealth != null && currentHealth.floatValue() > 0f) {
            healthByPlayer.put(defender.playerId, Math.max(0f, currentHealth.floatValue() - HIT_DAMAGE));
        }
        BattleArenaPlayerInput input = new BattleArenaPlayerInput();
        input.takeHitPressed = true;
        applyKnockback(attacker, defender, input);
        if (hurtbox.onHitAnimation != null && !hurtbox.onHitAnimation.trim().isEmpty()) {
            input.animationOverride = hurtbox.onHitAnimation;
        }
        inputTarget.submitInput(defender.playerId, tick + 1, input);
    }

    private void applyGuHit(int tick,
                            BattleArenaGuObjectState guObject,
                            BattleArenaPlayerState defender,
                            BattleArenaPlayerStateProvider inputTarget) {
        String hitKey = guObject.id + "->" + defender.playerId;
        Integer nextAllowedTick = nextAllowedGuHitTickByPair.get(hitKey);
        if (nextAllowedTick != null && tick < nextAllowedTick.intValue()) {
            return;
        }
        nextAllowedGuHitTickByPair.put(hitKey, tick + GU_HIT_COOLDOWN_TICKS);
        Float currentHealth = healthByPlayer.get(defender.playerId);
        if (currentHealth != null && currentHealth.floatValue() > 0f) {
            healthByPlayer.put(defender.playerId,
                    Math.max(0f, currentHealth.floatValue() - guDamage(guObject, defender)));
        }
        BattleArenaPlayerInput input = new BattleArenaPlayerInput();
        input.takeHitPressed = true;
        input.animationOverride = BattleArenaCharacterController.ANIM_TAKE_HIT;
        applyGuKnockback(guObject, defender, input);
        inputTarget.submitInput(defender.playerId, tick + 1, input);
    }

    private boolean isDamagingGuObject(BattleArenaGuObjectState guObject) {
        return guObject != null
                && ((BattleArenaGuMaterial.HOT_GAS.key.equals(guObject.material)
                || BattleArenaGuMaterial.MOLTEN_EARTH.key.equals(guObject.material)
                || guObject.temperature >= 220f)
                || guObjectHorizontalSpeed(guObject) >= 0.2f);
    }

    private float guDamage(BattleArenaGuObjectState guObject, BattleArenaPlayerState defender) {
        if (guObject == null || defender == null) {
            return 0f;
        }
        float guSpeed = guObjectHorizontalSpeed(guObject);
        float playerSpeed = playerHorizontalSpeed(defender);
        float relativeImpact = Math.max(0f, guSpeed - playerSpeed);
        return relativeImpact / Math.max(1f, defender.strength + 1f);
    }

    private float guObjectHorizontalSpeed(BattleArenaGuObjectState guObject) {
        if (guObject == null) {
            return 0f;
        }
        return (float) Math.sqrt((guObject.velocityX * guObject.velocityX)
                + (guObject.velocityZ * guObject.velocityZ));
    }

    private float playerHorizontalSpeed(BattleArenaPlayerState player) {
        if (player == null) {
            return 0f;
        }
        return (float) Math.sqrt((player.velocityX * player.velocityX)
                + (player.velocityZ * player.velocityZ));
    }

    private boolean guBoundsOverlapsPlayerHurtbox(Aabb guBounds, BattleArenaPlayerState defender) {
        if (guBounds == null || defender == null || hitboxTracks == null || hitboxTracks.boxes == null) {
            return false;
        }
        for (BattleArenaHitboxTracks.BoxDefinitionTrack hurtbox : hitboxTracks.boxes) {
            if (!isHurtbox(defender, hurtbox)) {
                continue;
            }
            Aabb hurtBounds = resolveBounds(defender, hurtbox);
            if (hurtBounds != null && guBounds.overlaps(hurtBounds)) {
                return true;
            }
        }
        return false;
    }

    private Aabb resolveGuBounds(BattleArenaGuObjectState guObject) {
        if (guObject == null) {
            return null;
        }
        float headingRadians = (float) Math.toRadians(guObject.headingDegrees);
        float cos = Math.abs((float) Math.cos(headingRadians));
        float sin = Math.abs((float) Math.sin(headingRadians));
        float worldHalfX = (guObject.halfX * cos) + (guObject.halfZ * sin);
        float worldHalfZ = (guObject.halfX * sin) + (guObject.halfZ * cos);
        return new Aabb(
                guObject.x - worldHalfX,
                guObject.y - guObject.halfY,
                guObject.z - worldHalfZ,
                guObject.x + worldHalfX,
                guObject.y + guObject.halfY,
                guObject.z + worldHalfZ);
    }

    private void applyGuKnockback(BattleArenaGuObjectState guObject,
                                  BattleArenaPlayerState defender,
                                  BattleArenaPlayerInput input) {
        float dx = defender.x - guObject.x;
        float dz = defender.z - guObject.z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001f) {
            dx = guObject.velocityX;
            dz = guObject.velocityZ;
            length = (float) Math.sqrt(dx * dx + dz * dz);
        }
        if (length < 0.0001f) {
            float headingRadians = (float) Math.toRadians(guObject.headingDegrees);
            dx = (float) Math.sin(headingRadians);
            dz = (float) Math.cos(headingRadians);
            length = 1f;
        }
        input.knockbackX = (dx / length) * (HIT_KNOCKBACK_DISTANCE * 0.72f);
        input.knockbackZ = (dz / length) * (HIT_KNOCKBACK_DISTANCE * 0.72f);
    }

    private void applyKnockback(BattleArenaPlayerState attacker,
                                BattleArenaPlayerState defender,
                                BattleArenaPlayerInput input) {
        float dx = defender.x - attacker.x;
        float dz = defender.z - attacker.z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001f) {
            float headingRadians = (float) Math.toRadians(attacker.headingDegrees);
            dx = (float) Math.sin(headingRadians);
            dz = (float) Math.cos(headingRadians);
            length = 1f;
        }
        input.knockbackX = (dx / length) * HIT_KNOCKBACK_DISTANCE;
        input.knockbackZ = (dz / length) * HIT_KNOCKBACK_DISTANCE;
    }

    private static final class Aabb {
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        Aabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean overlaps(Aabb other) {
            return other != null
                    && minX <= other.maxX && maxX >= other.minX
                    && minY <= other.maxY && maxY >= other.minY
                    && minZ <= other.maxZ && maxZ >= other.minZ;
        }
    }
}
