package com.njst.gaming.ri.battlearena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGuObjectSystem {
    public static final String LOADOUT_FIRE_WIND = "fire_wind";
    public static final String LOADOUT_EARTH_WALL = "earth_wall";
    public static final String LOADOUT_WATER_COLD = "water_cold";

    private static final float EARTH_MELT_TEMPERATURE = 700f;
    private static final float WATER_FREEZE_TEMPERATURE = 0f;
    private static final float HOT_AIR_PRESSURE_TEMPERATURE = 120f;
    private static final float AMBIENT_TEMPERATURE = 22f;
    private static final float COOLING_PER_TICK = 0.018f;
    private static final float PROJECTILE_HALF_EXTENT = 0.18f;
    private static final float PLAYER_BODY_RADIUS = 0.42f;
    private static final float PLAYER_BODY_HEIGHT = 1.9f;
    private static final float EXPLOSION_HALF_EXTENT = 1.15f;
    private static final float EXPLOSION_TEMPERATURE = 820f;
    private static final float EXPLOSION_PRESSURE = 4.2f;
    private static final int EXPLOSION_LIFETIME_TICKS = 18;
    private static final int DEFAULT_LIFETIME_TICKS = BattleArenaLocalPlayerStateServer.TICK_RATE * 4;

    private final Map<Integer, List<CastRequest>> castsByTick =
            new LinkedHashMap<Integer, List<CastRequest>>();
    private final ArrayList<MutableGuObject> objects = new ArrayList<MutableGuObject>();
    private int nextObjectId = 1;

    public void submitInput(String playerId, int tick, BattleArenaPlayerInput input) {
        if (playerId == null || tick < 0 || input == null || !input.castPressed) {
            return;
        }
        String loadoutKey = input.guLoadoutKey != null && !input.guLoadoutKey.trim().isEmpty()
                ? input.guLoadoutKey
                : LOADOUT_FIRE_WIND;
        List<CastRequest> requests = castsByTick.get(tick);
        if (requests == null) {
            requests = new ArrayList<CastRequest>();
            castsByTick.put(tick, requests);
        }
        requests.add(new CastRequest(playerId, loadoutKey));
    }

    public void update(int tick, float tickSeconds, List<BattleArenaPlayerState> players) {
        applyCasts(tick, players);
        for (MutableGuObject object : objects) {
            object.x += object.velocityX * tickSeconds;
            object.y += object.velocityY * tickSeconds;
            object.z += object.velocityZ * tickSeconds;
            object.velocityX *= 0.992f;
            object.velocityY *= 0.992f;
            object.velocityZ *= 0.992f;
            object.temperature += (AMBIENT_TEMPERATURE - object.temperature) * COOLING_PER_TICK;
            object.lifetimeTicksRemaining--;
        }
        applyObjectInteractions();
        explodeCollidingProjectiles(players);
        for (MutableGuObject object : objects) {
            applyMaterialLaws(object);
        }
        removeExpiredObjects();
    }

    public List<BattleArenaGuObjectState> snapshot() {
        ArrayList<BattleArenaGuObjectState> snapshot =
                new ArrayList<BattleArenaGuObjectState>(objects.size());
        for (MutableGuObject object : objects) {
            snapshot.add(object.snapshot());
        }
        return snapshot;
    }

    private void applyCasts(int tick, List<BattleArenaPlayerState> players) {
        List<CastRequest> requests = castsByTick.remove(tick);
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (CastRequest request : requests) {
            BattleArenaPlayerState caster = findPlayer(players, request.playerId);
            if (caster == null) {
                continue;
            }
            applyLoadout(caster, request.loadoutKey);
        }
    }

    private void applyLoadout(BattleArenaPlayerState caster, String loadoutKey) {
        if (LOADOUT_EARTH_WALL.equals(loadoutKey)) {
            applyEffects(caster, 1.45f, effects(
                    BattleArenaGuEffect.releaseMatter(BattleArenaGuMaterial.EARTH, 0.95f, 0.72f, 0.28f),
                    BattleArenaGuEffect.shape(0.95f, 0.72f, 0.28f)));
            return;
        }
        if (LOADOUT_WATER_COLD.equals(loadoutKey)) {
            applyEffects(caster, 1.25f, effects(
                    BattleArenaGuEffect.releaseMatter(BattleArenaGuMaterial.WATER, 0.42f, 0.28f, 0.42f),
                    BattleArenaGuEffect.removeHeat(42f),
                    BattleArenaGuEffect.shape(0.48f, 0.34f, 0.48f)));
            return;
        }
        applyEffects(caster, 1.2f, effects(
                BattleArenaGuEffect.releaseMatter(
                        BattleArenaGuMaterial.HOT_GAS,
                        PROJECTILE_HALF_EXTENT,
                        PROJECTILE_HALF_EXTENT,
                        PROJECTILE_HALF_EXTENT),
                BattleArenaGuEffect.addHeat(180f),
                BattleArenaGuEffect.push(0f, 0f, 1f, 4.8f)));
    }

    private void applyEffects(BattleArenaPlayerState caster,
                              float forwardOffset,
                              List<BattleArenaGuEffect> effects) {
        MutableGuObject target = null;
        float headingRadians = (float) Math.toRadians(caster.headingDegrees);
        float forwardX = (float) Math.sin(headingRadians);
        float forwardZ = (float) Math.cos(headingRadians);
        float originX = caster.x + forwardX * forwardOffset;
        float originY = caster.y + 0.7f;
        float originZ = caster.z + forwardZ * forwardOffset;
        for (BattleArenaGuEffect effect : effects) {
            if (effect == null) {
                continue;
            }
            if (effect.type == BattleArenaGuEffect.Type.RELEASE_MATTER) {
                target = createObject(
                        caster.playerId,
                        effect.material,
                        originX,
                        originY,
                        originZ,
                        caster.headingDegrees,
                        effect.halfX,
                        effect.halfY,
                        effect.halfZ);
                objects.add(target);
            } else if (target != null) {
                applyEffectToObject(target, effect, forwardX, forwardZ);
            }
        }
    }

    private MutableGuObject createObject(String ownerPlayerId,
                                         BattleArenaGuMaterial material,
                                         float x,
                                         float y,
                                         float z,
                                         float headingDegrees,
                                         float halfX,
                                         float halfY,
                                         float halfZ) {
        BattleArenaGuMaterial resolvedMaterial = material != null ? material : BattleArenaGuMaterial.AIR;
        MutableGuObject object = new MutableGuObject();
        object.id = nextObjectId++;
        object.ownerPlayerId = ownerPlayerId;
        object.material = resolvedMaterial;
        object.x = x;
        object.y = y;
        object.z = z;
        object.headingDegrees = normalizeDegrees(headingDegrees);
        object.halfX = halfX;
        object.halfY = halfY;
        object.halfZ = halfZ;
        object.temperature = resolvedMaterial.defaultTemperature;
        object.pressure = resolvedMaterial.defaultPressure;
        object.density = resolvedMaterial.defaultDensity;
        object.cohesion = resolvedMaterial.defaultCohesion;
        object.rigidity = resolvedMaterial.defaultRigidity;
        object.viscosity = resolvedMaterial.defaultViscosity;
        object.paths = BattleArenaGuPathState.forMaterial(resolvedMaterial);
        object.lifetimeTicksRemaining = DEFAULT_LIFETIME_TICKS;
        return object;
    }

    private void applyEffectToObject(MutableGuObject object,
                                     BattleArenaGuEffect effect,
                                     float forwardX,
                                     float forwardZ) {
        if (effect.type == BattleArenaGuEffect.Type.ADD_HEAT) {
            object.temperature += effect.amount;
            object.paths = new BattleArenaGuPathState(
                    object.paths.earth,
                    object.paths.water,
                    object.paths.wind,
                    clamp01(object.paths.fire + effect.amount / 500f),
                    object.paths.cold,
                    object.paths.rule);
        } else if (effect.type == BattleArenaGuEffect.Type.REMOVE_HEAT) {
            object.temperature -= effect.amount;
            object.paths = new BattleArenaGuPathState(
                    object.paths.earth,
                    object.paths.water,
                    object.paths.wind,
                    object.paths.fire,
                    clamp01(object.paths.cold + effect.amount / 120f),
                    object.paths.rule);
        } else if (effect.type == BattleArenaGuEffect.Type.PUSH) {
            float pushX = (forwardX * effect.z) + effect.x;
            float pushZ = (forwardZ * effect.z) + effect.z * 0f;
            object.velocityX += pushX * effect.amount;
            object.velocityY += effect.y * effect.amount;
            object.velocityZ += pushZ * effect.amount;
            object.paths = new BattleArenaGuPathState(
                    object.paths.earth,
                    object.paths.water,
                    clamp01(object.paths.wind + Math.abs(effect.amount) / 8f),
                    object.paths.fire,
                    object.paths.cold,
                    object.paths.rule);
        } else if (effect.type == BattleArenaGuEffect.Type.SHAPE) {
            object.halfX = effect.halfX;
            object.halfY = effect.halfY;
            object.halfZ = effect.halfZ;
            object.cohesion = Math.max(object.cohesion, 0.7f);
            object.rigidity = Math.max(object.rigidity, 0.65f);
            object.paths = new BattleArenaGuPathState(
                    object.paths.earth,
                    object.paths.water,
                    object.paths.wind,
                    object.paths.fire,
                    object.paths.cold,
                    clamp01(object.paths.rule + 0.25f));
        }
    }

    private void applyMaterialLaws(MutableGuObject object) {
        if (object.material == BattleArenaGuMaterial.EARTH
                && object.temperature >= EARTH_MELT_TEMPERATURE) {
            changeMaterial(object, BattleArenaGuMaterial.MOLTEN_EARTH);
        } else if (object.material == BattleArenaGuMaterial.WATER
                && object.temperature <= WATER_FREEZE_TEMPERATURE) {
            changeMaterial(object, BattleArenaGuMaterial.ICE);
        } else if (object.material == BattleArenaGuMaterial.ICE
                && object.temperature > WATER_FREEZE_TEMPERATURE + 8f) {
            changeMaterial(object, BattleArenaGuMaterial.WATER);
        }
        if ((object.material == BattleArenaGuMaterial.AIR || object.material == BattleArenaGuMaterial.HOT_GAS)
                && object.temperature > HOT_AIR_PRESSURE_TEMPERATURE) {
            object.pressure = 1f + ((object.temperature - HOT_AIR_PRESSURE_TEMPERATURE) / 200f);
            if (object.pressure > 2.4f) {
                float expansion = Math.min(0.012f, object.pressure * 0.0025f);
                object.halfX += expansion;
                object.halfY += expansion;
                object.halfZ += expansion;
            }
        }
    }

    private void applyObjectInteractions() {
        for (int i = 0; i < objects.size(); i++) {
            MutableGuObject first = objects.get(i);
            for (int j = i + 1; j < objects.size(); j++) {
                MutableGuObject second = objects.get(j);
                if (!overlaps(first, second)) {
                    continue;
                }
                transferHeat(first, second);
                transferHeat(second, first);
            }
        }
    }

    private void explodeCollidingProjectiles(List<BattleArenaPlayerState> players) {
        for (MutableGuObject object : objects) {
            if (!isExplosiveProjectile(object)) {
                continue;
            }
            if (overlapsAnyPlayer(object, players) || overlapsAnyBlockingObject(object)) {
                explode(object);
            }
        }
    }

    private boolean isExplosiveProjectile(MutableGuObject object) {
        return object != null
                && !object.exploded
                && object.material == BattleArenaGuMaterial.HOT_GAS
                && object.temperature >= HOT_AIR_PRESSURE_TEMPERATURE
                && ((object.velocityX * object.velocityX)
                + (object.velocityY * object.velocityY)
                + (object.velocityZ * object.velocityZ)) > 0.04f;
    }

    private boolean overlapsAnyPlayer(MutableGuObject object, List<BattleArenaPlayerState> players) {
        if (players == null || players.isEmpty()) {
            return false;
        }
        Bounds objectBounds = boundsFor(object);
        for (BattleArenaPlayerState player : players) {
            if (player == null || player.playerId.equals(object.ownerPlayerId)) {
                continue;
            }
            if (objectBounds.overlaps(playerBounds(player))) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsAnyBlockingObject(MutableGuObject object) {
        Bounds objectBounds = boundsFor(object);
        for (MutableGuObject other : objects) {
            if (other == null || other == object || !isBlockingObject(other)) {
                continue;
            }
            if (objectBounds.overlaps(boundsFor(other))) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockingObject(MutableGuObject object) {
        return object != null
                && object.rigidity >= 0.55f
                && object.cohesion >= 0.45f
                && object.lifetimeTicksRemaining > 0;
    }

    private void explode(MutableGuObject object) {
        object.exploded = true;
        object.velocityX = 0f;
        object.velocityY = 0f;
        object.velocityZ = 0f;
        object.halfX = Math.max(object.halfX, EXPLOSION_HALF_EXTENT);
        object.halfY = Math.max(object.halfY, EXPLOSION_HALF_EXTENT);
        object.halfZ = Math.max(object.halfZ, EXPLOSION_HALF_EXTENT);
        object.temperature = Math.max(object.temperature, EXPLOSION_TEMPERATURE);
        object.pressure = Math.max(object.pressure, EXPLOSION_PRESSURE);
        object.paths = new BattleArenaGuPathState(
                object.paths.earth,
                object.paths.water,
                Math.max(object.paths.wind, 0.85f),
                Math.max(object.paths.fire, 1f),
                object.paths.cold,
                Math.max(object.paths.rule, 0.2f));
        object.lifetimeTicksRemaining = Math.min(
                object.lifetimeTicksRemaining,
                EXPLOSION_LIFETIME_TICKS);
    }

    private void transferHeat(MutableGuObject source, MutableGuObject target) {
        if (source == null || target == null) {
            return;
        }
        float heatInfluence = Math.max(source.paths.fire, source.temperature > target.temperature ? 0.15f : 0f);
        float coldInfluence = Math.max(source.paths.cold, source.temperature < target.temperature ? 0.12f : 0f);
        float influence = Math.max(heatInfluence, coldInfluence);
        if (influence <= 0f) {
            return;
        }
        target.temperature += (source.temperature - target.temperature) * Math.min(0.045f, influence * 0.025f);
    }

    private boolean overlaps(MutableGuObject first, MutableGuObject second) {
        return first != null && second != null
                && first.x - first.halfX <= second.x + second.halfX
                && first.x + first.halfX >= second.x - second.halfX
                && first.y - first.halfY <= second.y + second.halfY
                && first.y + first.halfY >= second.y - second.halfY
                && first.z - first.halfZ <= second.z + second.halfZ
                && first.z + first.halfZ >= second.z - second.halfZ;
    }

    private Bounds boundsFor(MutableGuObject object) {
        if (object == null) {
            return Bounds.empty();
        }
        float headingRadians = (float) Math.toRadians(object.headingDegrees);
        float cos = Math.abs((float) Math.cos(headingRadians));
        float sin = Math.abs((float) Math.sin(headingRadians));
        float worldHalfX = (object.halfX * cos) + (object.halfZ * sin);
        float worldHalfZ = (object.halfX * sin) + (object.halfZ * cos);
        return new Bounds(
                object.x - worldHalfX,
                object.y - object.halfY,
                object.z - worldHalfZ,
                object.x + worldHalfX,
                object.y + object.halfY,
                object.z + worldHalfZ);
    }

    private Bounds playerBounds(BattleArenaPlayerState player) {
        return new Bounds(
                player.x - PLAYER_BODY_RADIUS,
                player.y,
                player.z - PLAYER_BODY_RADIUS,
                player.x + PLAYER_BODY_RADIUS,
                player.y + PLAYER_BODY_HEIGHT,
                player.z + PLAYER_BODY_RADIUS);
    }

    private void changeMaterial(MutableGuObject object, BattleArenaGuMaterial material) {
        object.material = material;
        object.density = material.defaultDensity;
        object.pressure = Math.max(object.pressure, material.defaultPressure);
        object.cohesion = material.defaultCohesion;
        object.rigidity = material.defaultRigidity;
        object.viscosity = material.defaultViscosity;
        object.paths = BattleArenaGuPathState.forMaterial(material);
    }

    private void removeExpiredObjects() {
        Iterator<MutableGuObject> iterator = objects.iterator();
        while (iterator.hasNext()) {
            MutableGuObject object = iterator.next();
            if (object.lifetimeTicksRemaining <= 0) {
                iterator.remove();
            }
        }
    }

    private BattleArenaPlayerState findPlayer(List<BattleArenaPlayerState> players, String playerId) {
        if (players == null || playerId == null) {
            return null;
        }
        for (BattleArenaPlayerState player : players) {
            if (player != null && playerId.equals(player.playerId)) {
                return player;
            }
        }
        return null;
    }

    private static List<BattleArenaGuEffect> effects(BattleArenaGuEffect first,
                                                     BattleArenaGuEffect second) {
        ArrayList<BattleArenaGuEffect> effects = new ArrayList<BattleArenaGuEffect>();
        effects.add(first);
        effects.add(second);
        return effects;
    }

    private static List<BattleArenaGuEffect> effects(BattleArenaGuEffect first,
                                                     BattleArenaGuEffect second,
                                                     BattleArenaGuEffect third) {
        ArrayList<BattleArenaGuEffect> effects = new ArrayList<BattleArenaGuEffect>();
        effects.add(first);
        effects.add(second);
        effects.add(third);
        return effects;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float normalizeDegrees(float value) {
        float wrapped = value % 360f;
        if (wrapped < -180f) {
            return wrapped + 360f;
        }
        if (wrapped > 180f) {
            return wrapped - 360f;
        }
        return wrapped;
    }

    private static final class CastRequest {
        final String playerId;
        final String loadoutKey;

        CastRequest(String playerId, String loadoutKey) {
            this.playerId = playerId;
            this.loadoutKey = loadoutKey;
        }
    }

    private static final class MutableGuObject {
        int id;
        String ownerPlayerId;
        BattleArenaGuMaterial material;
        float x;
        float y;
        float z;
        float headingDegrees;
        float halfX;
        float halfY;
        float halfZ;
        float velocityX;
        float velocityY;
        float velocityZ;
        float temperature;
        float pressure;
        float density;
        float cohesion;
        float rigidity;
        float viscosity;
        BattleArenaGuPathState paths;
        int lifetimeTicksRemaining;
        boolean exploded;

        BattleArenaGuObjectState snapshot() {
            BattleArenaGuPathState pathState = paths != null
                    ? paths
                    : BattleArenaGuPathState.forMaterial(material);
            return new BattleArenaGuObjectState(
                    id,
                    ownerPlayerId,
                    material.key,
                    x,
                    y,
                    z,
                    headingDegrees,
                    halfX,
                    halfY,
                    halfZ,
                    velocityX,
                    velocityY,
                    velocityZ,
                    temperature,
                    pressure,
                    density,
                    cohesion,
                    rigidity,
                    viscosity,
                    pathState.earth,
                    pathState.water,
                    pathState.wind,
                    pathState.fire,
                    pathState.cold,
                    pathState.rule,
                    lifetimeTicksRemaining);
        }
    }

    private static final class Bounds {
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean overlaps(Bounds other) {
            return other != null
                    && minX <= other.maxX && maxX >= other.minX
                    && minY <= other.maxY && maxY >= other.minY
                    && minZ <= other.maxZ && maxZ >= other.minZ;
        }

        static Bounds empty() {
            return new Bounds(0f, 0f, 0f, 0f, 0f, 0f);
        }
    }
}
