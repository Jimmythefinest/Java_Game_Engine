package com.njst.gaming.ri.battlearena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGuObjectSystem {
    private static final float EARTH_MELT_TEMPERATURE = 700f;
    private static final float WATER_FREEZE_TEMPERATURE = 0f;
    private static final float HOT_AIR_PRESSURE_TEMPERATURE = 120f;
    private static final float AMBIENT_TEMPERATURE = 22f;
    private static final float COOLING_PER_TICK = 0.018f;
    private static final float ICE_AMBIENT_EXCHANGE_PER_TICK = 0.0006f;
    private static final float GRAVITY_PER_SECOND = 9.4f;
    private static final float GROUND_Y = 0f;
    private static final float SOLIDITY_THRESHOLD = 0.55f;
    private static final float RESISTIVE_SOLIDITY_THRESHOLD = 0.12f;
    private static final float MAX_OBJECT_PASS_THROUGH_DRAG = 0.72f;
    private static final float FLUID_HEAT_GAIN_PER_SECOND = 18f;
    private static final float SEEP_RATE_PER_SECOND = 0.22f;
    private static final float SEEP_SHRINK_PER_SECOND = 0.36f;
    private static final float SETTLE_DRAG_PER_TICK = 0.9f;
    private static final float MIN_PERSISTENT_HALF_EXTENT = 0.03f;
    private static final float PLAYER_BODY_RADIUS = 0.42f;
    private static final float PLAYER_BODY_HEIGHT = 1.9f;
    private static final float EXPLOSION_HALF_EXTENT = 1.15f;
    private static final float EXPLOSION_TEMPERATURE = 820f;
    private static final float EXPLOSION_PRESSURE = 4.2f;
    private static final int EXPLOSION_LIFETIME_TICKS = 18;
    private static final int DEFAULT_LIFETIME_TICKS = BattleArenaLocalPlayerStateServer.TICK_RATE * 4;

    private static final float TARGET_RANGE = 4.8f;
    private static final float TARGET_CONE_DOT = 0.2f;
    private static final float DEFAULT_FORWARD_OFFSET = 1.25f;
    private static final float CREATE_WATER_HALF_X = 0.28f;
    private static final float CREATE_WATER_HALF_Y = 0.28f;
    private static final float CREATE_WATER_HALF_Z = 0.28f;
    private static final float CREATE_EARTH_HALF_X = 0.3f;
    private static final float CREATE_EARTH_HALF_Y = 0.3f;
    private static final float CREATE_EARTH_HALF_Z = 0.3f;
    private static final float CREATE_FLAME_HALF_X = 0.34f;
    private static final float CREATE_FLAME_HALF_Y = 0.34f;
    private static final float CREATE_FLAME_HALF_Z = 0.34f;
    private static final float SPEAR_HALF_X = 0.14f;
    private static final float SPEAR_HALF_Y = 0.14f;
    private static final float SPEAR_HALF_Z = 0.92f;
    private static final float WALL_HALF_X = 0.95f;
    private static final float WALL_HALF_Y = 0.72f;
    private static final float WALL_HALF_Z = 0.28f;
    private static final float CREATE_WATER_COHESION = 0.34f;
    private static final float CREATE_WATER_RIGIDITY = 0.08f;
    private static final float CREATE_WATER_VISCOSITY = 0.52f;
    private static final float CREATE_ICE_COHESION = 0.72f;
    private static final float CREATE_ICE_RIGIDITY = 0.8f;
    private static final float CREATE_ICE_VISCOSITY = 0.88f;
    private static final float CREATE_FLAME_TEMPERATURE = 560f;
    private static final float FLAME_PROXIMITY_RANGE = 0.85f;
    private static final float FLAME_PASSIVE_TEMPERATURE_LOSS_PER_TICK = 1.6f;
    private static final int FLAME_PASSIVE_LIFETIME_LOSS_PER_TICK = 1;
    private static final float FLAME_TRANSFER_COST_SCALE = 0.18f;
    private static final float FLAME_TRANSFER_TEMPERATURE_LOSS_MAX = 6f;

    private static final float CREATE_WATER_DURATION = 0.01f;
    private static final float SHAPE_DURATION = 0.55f;
    private static final float COOL_DURATION = 1.2f;
    private static final float HEAT_DURATION = 1.2f;
    private static final float MOVE_DURATION = 0.35f;
    private static final float COOL_RATE = 145f;
    private static final float HEAT_RATE = 145f;
    private static final float MOVE_RATE = 8.5f;
    private static final float MOVE_CHARGE_RATE = 3.6f;
    private static final float MOVE_CHARGE_MAX = 15f;
    private static final float MOVE_CHARGE_BASE = 3.5f;
    private static final float WATER_SHRINK_START_TEMPERATURE = 70f;
    private static final float WATER_SHRINK_MAX_TEMPERATURE = 260f;
    private static final float WATER_MIN_SIZE_SCALE = 0.48f;

    private final Map<Integer, List<CastRequest>> castsByTick =
            new LinkedHashMap<Integer, List<CastRequest>>();
    private final ArrayList<MutableGuObject> objects = new ArrayList<MutableGuObject>();
    private final ArrayList<ActiveWormJob> activeJobs = new ArrayList<ActiveWormJob>();
    private final Map<String, MoveChargeState> moveChargeByPlayer =
            new LinkedHashMap<String, MoveChargeState>();
    private int nextObjectId = 1;

    public void submitInput(String playerId, int tick, BattleArenaPlayerInput input) {
        if (playerId == null || tick < 0 || input == null) {
            return;
        }
        String wormAction = input.guWormAction;
        boolean moveChargeSignal = input.guMoveForwardHeld
                || BattleArenaGuWormAction.MOVE_FORWARD.equals(wormAction)
                || moveChargeByPlayer.containsKey(playerId);
        if ((!input.castPressed && !moveChargeSignal)
                || ((wormAction == null || wormAction.trim().isEmpty()) && !moveChargeSignal)) {
            return;
        }
        List<CastRequest> requests = castsByTick.get(tick);
        if (requests == null) {
            requests = new ArrayList<CastRequest>();
            castsByTick.put(tick, requests);
        }
        requests.add(new CastRequest(playerId, wormAction, input.guMoveForwardHeld));
    }

    public void update(int tick, float tickSeconds, List<BattleArenaPlayerState> players) {
        applyCasts(tick, players);
        applyActiveJobs(tickSeconds);
        for (MutableGuObject object : objects) {
            boolean controlled = object.lifetimeTicksRemaining > 0;
            if (!controlled) {
                applyPostExpiryPhysics(object, tickSeconds);
            }
            object.x += object.velocityX * tickSeconds;
            object.y += object.velocityY * tickSeconds;
            object.z += object.velocityZ * tickSeconds;
            object.velocityX *= 0.992f;
            object.velocityY *= 0.992f;
            object.velocityZ *= 0.992f;
            object.temperature += (AMBIENT_TEMPERATURE - object.temperature)
                    * ambientExchangeRate(object);
            if (object.material == BattleArenaGuMaterial.HOT_GAS) {
                applyFlameDecay(object);
            }
            clampToGround(object);
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

    private float ambientExchangeRate(MutableGuObject object) {
        if (object != null && object.material == BattleArenaGuMaterial.ICE) {
            return ICE_AMBIENT_EXCHANGE_PER_TICK;
        }
        return COOLING_PER_TICK;
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
            if (BattleArenaGuWormAction.MOVE_FORWARD.equals(request.wormAction)
                    || request.moveForwardHeld
                    || moveChargeByPlayer.containsKey(request.playerId)) {
                handleMoveChargeSignal(caster, request);
                continue;
            }
            applyWormAction(caster, request.wormAction);
        }
    }

    private void handleMoveChargeSignal(BattleArenaPlayerState caster, CastRequest request) {
        if (caster == null || request == null || request.playerId == null) {
            return;
        }
        MoveChargeState charge = moveChargeByPlayer.get(request.playerId);
        if (request.moveForwardHeld) {
            if (charge == null) {
                MutableGuObject target = findTargetObject(caster, BattleArenaGuWormAction.MOVE_FORWARD);
                if (target == null) {
                    return;
                }
                charge = new MoveChargeState(request.playerId, target.id);
                moveChargeByPlayer.put(request.playerId, charge);
            }
            MutableGuObject target = findObjectById(charge.targetObjectId);
            if (target == null || target.lifetimeTicksRemaining <= 0) {
                moveChargeByPlayer.remove(request.playerId);
                return;
            }
            float headingRadians = (float) Math.toRadians(caster.headingDegrees);
            charge.directionX = (float) Math.sin(headingRadians);
            charge.directionY = 0f;
            charge.directionZ = (float) Math.cos(headingRadians);
            charge.accumulatedVelocity = Math.min(
                    MOVE_CHARGE_MAX,
                    charge.accumulatedVelocity + MOVE_CHARGE_RATE);
            return;
        }
        if (charge == null) {
            return;
        }
        MutableGuObject target = findObjectById(charge.targetObjectId);
        moveChargeByPlayer.remove(request.playerId);
        if (target == null || target.lifetimeTicksRemaining <= 0) {
            return;
        }
        float releaseVelocity = MOVE_CHARGE_BASE + charge.accumulatedVelocity;
        target.velocityX += charge.directionX * releaseVelocity;
        target.velocityY += charge.directionY * releaseVelocity;
        target.velocityZ += charge.directionZ * releaseVelocity;
        target.headingDegrees = normalizeDegrees((float) Math.toDegrees(
                Math.atan2(charge.directionX, charge.directionZ)));
    }

    private void applyWormAction(BattleArenaPlayerState caster, String wormAction) {
        if (BattleArenaGuWormAction.CREATE_WATER.equals(wormAction)) {
            MutableGuObject created = createWaterObject(caster);
            if (created != null) {
                objects.add(created);
            }
            return;
        }
        if (BattleArenaGuWormAction.CREATE_ICE.equals(wormAction)) {
            MutableGuObject created = createIceObject(caster);
            if (created != null) {
                objects.add(created);
            }
            return;
        }
        if (BattleArenaGuWormAction.CREATE_EARTH.equals(wormAction)) {
            MutableGuObject created = createEarthObject(caster);
            if (created != null) {
                objects.add(created);
            }
            return;
        }
        if (BattleArenaGuWormAction.CREATE_FLAME.equals(wormAction)) {
            MutableGuObject created = createFlameObject(caster);
            if (created != null) {
                objects.add(created);
            }
            return;
        }
        MutableGuObject target = findTargetObject(caster, wormAction);
        if (target == null) {
            return;
        }
        ActiveWormJob job = createJob(caster, target, wormAction);
        if (job != null) {
            activeJobs.add(job);
        }
    }

    private MutableGuObject createWaterObject(BattleArenaPlayerState caster) {
        MutableGuObject object = createConstructObject(caster,
                BattleArenaGuMaterial.WATER,
                CREATE_WATER_HALF_X,
                CREATE_WATER_HALF_Y,
                CREATE_WATER_HALF_Z);
        object.cohesion = Math.max(object.cohesion, CREATE_WATER_COHESION);
        object.rigidity = Math.max(object.rigidity, CREATE_WATER_RIGIDITY);
        object.viscosity = Math.max(object.viscosity, CREATE_WATER_VISCOSITY);
        object.paths = new BattleArenaGuPathState(0f, 1f, 0.1f, 0f, 0f, 0.1f);
        object.baseHalfX = object.halfX;
        object.baseHalfY = object.halfY;
        object.baseHalfZ = object.halfZ;
        return object;
    }

    private MutableGuObject createIceObject(BattleArenaPlayerState caster) {
        MutableGuObject object = createConstructObject(caster,
                BattleArenaGuMaterial.ICE,
                CREATE_WATER_HALF_X,
                CREATE_WATER_HALF_Y,
                CREATE_WATER_HALF_Z);
        object.cohesion = Math.max(object.cohesion, CREATE_ICE_COHESION);
        object.rigidity = Math.max(object.rigidity, CREATE_ICE_RIGIDITY);
        object.viscosity = Math.max(object.viscosity, CREATE_ICE_VISCOSITY);
        object.paths = new BattleArenaGuPathState(0f, 0.85f, 0.05f, 0f, 1f, 0.18f);
        object.phaseLocked = true;
        return object;
    }

    private MutableGuObject createEarthObject(BattleArenaPlayerState caster) {
        MutableGuObject object = createConstructObject(caster,
                BattleArenaGuMaterial.EARTH,
                CREATE_EARTH_HALF_X,
                CREATE_EARTH_HALF_Y,
                CREATE_EARTH_HALF_Z);
        object.paths = new BattleArenaGuPathState(1f, 0f, 0f, 0f, 0f, 0.14f);
        return object;
    }

    private MutableGuObject createFlameObject(BattleArenaPlayerState caster) {
        MutableGuObject object = createConstructObject(caster,
                BattleArenaGuMaterial.HOT_GAS,
                CREATE_FLAME_HALF_X,
                CREATE_FLAME_HALF_Y,
                CREATE_FLAME_HALF_Z);
        object.temperature = Math.max(object.temperature, CREATE_FLAME_TEMPERATURE);
        object.cohesion = Math.min(object.cohesion, 0.06f);
        object.rigidity = 0f;
        object.viscosity = Math.max(object.viscosity, 0.08f);
        object.pressure = Math.max(object.pressure, 1.9f);
        object.paths = new BattleArenaGuPathState(0f, 0f, 0.55f, 1f, 0f, 0.18f);
        return object;
    }

    private MutableGuObject createConstructObject(BattleArenaPlayerState caster,
                                                  BattleArenaGuMaterial material,
                                                  float halfX,
                                                  float halfY,
                                                  float halfZ) {
        float headingRadians = (float) Math.toRadians(caster.headingDegrees);
        float forwardX = (float) Math.sin(headingRadians);
        float forwardZ = (float) Math.cos(headingRadians);
        float originX = caster.x + forwardX * DEFAULT_FORWARD_OFFSET;
        float originY = caster.y + 0.7f;
        float originZ = caster.z + forwardZ * DEFAULT_FORWARD_OFFSET;
        return createObject(
                caster.playerId,
                material,
                originX,
                originY,
                originZ,
                caster.headingDegrees,
                halfX,
                halfY,
                halfZ);
    }

    private ActiveWormJob createJob(BattleArenaPlayerState caster,
                                    MutableGuObject target,
                                    String wormAction) {
        if (BattleArenaGuWormAction.SHAPE_SPEAR.equals(wormAction)) {
            return ActiveWormJob.shape(
                    caster.playerId,
                    target.id,
                    SHAPE_DURATION,
                    SPEAR_HALF_X,
                    SPEAR_HALF_Y,
                    SPEAR_HALF_Z);
        }
        if (BattleArenaGuWormAction.SHAPE_WALL.equals(wormAction)) {
            return ActiveWormJob.shape(
                    caster.playerId,
                    target.id,
                    SHAPE_DURATION,
                    WALL_HALF_X,
                    WALL_HALF_Y,
                    WALL_HALF_Z);
        }
        if (BattleArenaGuWormAction.COOL.equals(wormAction)) {
            return ActiveWormJob.temperature(caster.playerId, target.id, wormAction, COOL_DURATION, -COOL_RATE);
        }
        return null;
    }

    private void applyActiveJobs(float tickSeconds) {
        Iterator<ActiveWormJob> iterator = activeJobs.iterator();
        while (iterator.hasNext()) {
            ActiveWormJob job = iterator.next();
            MutableGuObject target = findObjectById(job.targetObjectId);
            if (target == null || target.lifetimeTicksRemaining <= 0) {
                iterator.remove();
                continue;
            }
            float step = Math.min(job.remainingSeconds, Math.max(0f, tickSeconds));
            if (step <= 0f) {
                iterator.remove();
                continue;
            }
            if (job.kind == WormKind.SHAPE) {
                applyShapeJob(target, job, step);
            } else if (job.kind == WormKind.TEMPERATURE) {
                applyTemperatureJob(target, job, step);
            } else if (job.kind == WormKind.MOVE) {
                applyMoveJob(target, job, step);
            }
            job.remainingSeconds -= step;
            if (job.remainingSeconds <= 0.0001f) {
                iterator.remove();
            }
        }
    }

    private void applyShapeJob(MutableGuObject target, ActiveWormJob job, float stepSeconds) {
        float t = Math.min(1f, stepSeconds / Math.max(0.0001f, job.durationSeconds));
        target.baseHalfX = approach(target.baseHalfX, job.targetHalfX, t);
        target.baseHalfY = approach(target.baseHalfY, job.targetHalfY, t);
        target.baseHalfZ = approach(target.baseHalfZ, job.targetHalfZ, t);
        applyThermalDimensions(target);
        target.cohesion = Math.max(target.cohesion, 0.7f);
        target.rigidity = Math.max(target.rigidity, 0.65f);
        target.paths = new BattleArenaGuPathState(
                target.paths.earth,
                target.paths.water,
                target.paths.wind,
                target.paths.fire,
                target.paths.cold,
                clamp01(target.paths.rule + (0.45f * t)));
    }

    private void applyTemperatureJob(MutableGuObject target, ActiveWormJob job, float stepSeconds) {
        float delta = job.rate * stepSeconds;
        target.temperature += delta;
        if (delta > 0f) {
            target.paths = new BattleArenaGuPathState(
                    target.paths.earth,
                    target.paths.water,
                    target.paths.wind,
                    clamp01(target.paths.fire + Math.abs(delta) / 500f),
                    target.paths.cold,
                    target.paths.rule);
        } else {
            target.paths = new BattleArenaGuPathState(
                    target.paths.earth,
                    target.paths.water,
                    target.paths.wind,
                    target.paths.fire,
                    clamp01(target.paths.cold + Math.abs(delta) / 160f),
                    target.paths.rule);
        }
    }

    private void applyMoveJob(MutableGuObject target, ActiveWormJob job, float stepSeconds) {
        target.velocityX += job.directionX * job.rate * stepSeconds;
        target.velocityY += job.directionY * job.rate * stepSeconds;
        target.velocityZ += job.directionZ * job.rate * stepSeconds;
        target.headingDegrees = normalizeDegrees((float) Math.toDegrees(Math.atan2(job.directionX, job.directionZ)));
        target.paths = new BattleArenaGuPathState(
                target.paths.earth,
                target.paths.water,
                clamp01(target.paths.wind + (stepSeconds * 1.8f)),
                target.paths.fire,
                target.paths.cold,
                target.paths.rule);
    }

    private MutableGuObject findTargetObject(BattleArenaPlayerState caster, String wormAction) {
        if (caster == null || wormAction == null) {
            return null;
        }
        MutableGuObject owned = findBestTarget(caster, wormAction, true);
        if (owned != null) {
            return owned;
        }
        return findBestTarget(caster, wormAction, false);
    }

    private MutableGuObject findBestTarget(BattleArenaPlayerState caster,
                                           String wormAction,
                                           boolean ownedOnly) {
        float headingRadians = (float) Math.toRadians(caster.headingDegrees);
        float forwardX = (float) Math.sin(headingRadians);
        float forwardZ = (float) Math.cos(headingRadians);
        MutableGuObject best = null;
        float bestScore = Float.MAX_VALUE;
        for (MutableGuObject object : objects) {
            if (object == null || object.removed) {
                continue;
            }
            if (ownedOnly && !caster.playerId.equals(object.ownerPlayerId)) {
                continue;
            }
            if (BattleArenaGuWormAction.COOL.equals(wormAction)
                    && object.material != BattleArenaGuMaterial.WATER
                    && object.material != BattleArenaGuMaterial.ICE
                    && object.material != BattleArenaGuMaterial.MOLTEN_EARTH
                    && object.material != BattleArenaGuMaterial.EARTH) {
                continue;
            }
        if (BattleArenaGuWormAction.HEAT.equals(wormAction)
                    && object.material != BattleArenaGuMaterial.HOT_GAS
                    && object.material != BattleArenaGuMaterial.WATER
                    && object.material != BattleArenaGuMaterial.ICE
                    && object.material != BattleArenaGuMaterial.MOLTEN_EARTH
                    && object.material != BattleArenaGuMaterial.EARTH) {
                continue;
            }
            float dx = object.x - caster.x;
            float dy = (object.y + object.halfY) - (caster.y + 0.7f);
            float dz = object.z - caster.z;
            float distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > TARGET_RANGE * TARGET_RANGE) {
                continue;
            }
            float distance = (float) Math.sqrt(distanceSquared);
            if (distance < 0.0001f) {
                distance = 0.0001f;
            }
            float dot = ((dx / distance) * forwardX) + ((dz / distance) * forwardZ);
            if (dot < TARGET_CONE_DOT) {
                continue;
            }
            float score = distanceSquared - (dot * 0.5f);
            if (score < bestScore) {
                bestScore = score;
                best = object;
            }
        }
        return best;
    }

    private MutableGuObject findObjectById(int id) {
        for (MutableGuObject object : objects) {
            if (object.id == id) {
                return object;
            }
        }
        return null;
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
        object.baseHalfX = halfX;
        object.baseHalfY = halfY;
        object.baseHalfZ = halfZ;
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

    private void applyMaterialLaws(MutableGuObject object) {
        applyThermalDimensions(object);
        if (object.phaseLocked) {
            return;
        }
        if (object.material == BattleArenaGuMaterial.EARTH
                && object.temperature >= EARTH_MELT_TEMPERATURE) {
            changeMaterial(object, BattleArenaGuMaterial.MOLTEN_EARTH);
        } else if (object.material == BattleArenaGuMaterial.MOLTEN_EARTH
                && object.temperature < EARTH_MELT_TEMPERATURE - 80f) {
            changeMaterial(object, BattleArenaGuMaterial.EARTH);
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

    private void applyThermalDimensions(MutableGuObject object) {
        if (object == null) {
            return;
        }
        float scale = 1f;
        if (object.material == BattleArenaGuMaterial.WATER) {
            float heatProgress = normalizedRange(
                    object.temperature,
                    WATER_SHRINK_START_TEMPERATURE,
                    WATER_SHRINK_MAX_TEMPERATURE);
            scale = 1f - ((1f - WATER_MIN_SIZE_SCALE) * heatProgress);
        }
        object.halfX = Math.max(0.02f, object.baseHalfX * scale);
        object.halfY = Math.max(0.02f, object.baseHalfY * scale);
        object.halfZ = Math.max(0.02f, object.baseHalfZ * scale);
    }

    private void applyPostExpiryPhysics(MutableGuObject object, float tickSeconds) {
        if (object == null) {
            return;
        }
        if (object.y - object.halfY > GROUND_Y + 0.001f || object.velocityY > -0.001f) {
            object.velocityY -= GRAVITY_PER_SECOND * tickSeconds;
        }
        if (isLowSolidity(object) && object.y - object.halfY <= GROUND_Y + 0.02f) {
            float seepAmount = SEEP_RATE_PER_SECOND * tickSeconds;
            float shrinkAmount = SEEP_SHRINK_PER_SECOND * tickSeconds;
            object.y -= seepAmount;
            object.baseHalfX = Math.max(0f, object.baseHalfX - shrinkAmount * 0.55f);
            object.baseHalfY = Math.max(0f, object.baseHalfY - shrinkAmount * 0.75f);
            object.baseHalfZ = Math.max(0f, object.baseHalfZ - shrinkAmount * 0.55f);
            object.velocityX *= SETTLE_DRAG_PER_TICK;
            object.velocityY *= 0.65f;
            object.velocityZ *= SETTLE_DRAG_PER_TICK;
            applyThermalDimensions(object);
        } else if (object.y - object.halfY <= GROUND_Y + 0.02f) {
            object.velocityX *= SETTLE_DRAG_PER_TICK;
            object.velocityY = Math.min(0f, object.velocityY * 0.35f);
            object.velocityZ *= SETTLE_DRAG_PER_TICK;
        }
    }

    private void clampToGround(MutableGuObject object) {
        if (object == null) {
            return;
        }
        if (object.lifetimeTicksRemaining <= 0 && isLowSolidity(object)) {
            return;
        }
        float minY = object.y - object.halfY;
        if (minY < GROUND_Y) {
            object.y += (GROUND_Y - minY);
            if (object.velocityY < 0f) {
                object.velocityY = 0f;
            }
        }
    }

    private void applyObjectInteractions() {
        for (int i = 0; i < objects.size(); i++) {
            MutableGuObject first = objects.get(i);
            for (int j = i + 1; j < objects.size(); j++) {
                MutableGuObject second = objects.get(j);
                boolean overlapping = overlaps(first, second);
                if (!overlapping && !isFlameInProximity(first, second)) {
                    continue;
                }
                transferHeat(first, second);
                transferHeat(second, first);
                if (overlapping) {
                    applyCollisionResponse(first, second);
                }
            }
        }
    }

    private void applyCollisionResponse(MutableGuObject first, MutableGuObject second) {
        if (first == null || second == null) {
            return;
        }
        boolean firstBlocking = isBlockingObject(first);
        boolean secondBlocking = isBlockingObject(second);
        if (firstBlocking && secondBlocking) {
            separateObjects(first, second);
            return;
        }
        applyFluidResistance(first, second);
        applyFluidResistance(second, first);
    }

    private void applyFluidResistance(MutableGuObject mover, MutableGuObject medium) {
        if (mover == null || medium == null || mover == medium) {
            return;
        }
        if (isBlockingObject(medium)) {
            return;
        }
        float mediumSolidity = solidityOf(medium);
        if (mediumSolidity < RESISTIVE_SOLIDITY_THRESHOLD) {
            return;
        }
        float drag = normalizedRange(
                mediumSolidity,
                RESISTIVE_SOLIDITY_THRESHOLD,
                SOLIDITY_THRESHOLD) * MAX_OBJECT_PASS_THROUGH_DRAG;
        drag = Math.min(MAX_OBJECT_PASS_THROUGH_DRAG, drag);
        mover.velocityX *= (1f - drag);
        mover.velocityY *= (1f - (drag * 0.4f));
        mover.velocityZ *= (1f - drag);
        if (medium.material == BattleArenaGuMaterial.WATER) {
            mover.temperature += FLUID_HEAT_GAIN_PER_SECOND * drag * BattleArenaLocalPlayerStateServer.TICK_SECONDS;
        }
    }

    private void separateObjects(MutableGuObject first, MutableGuObject second) {
        Bounds firstBounds = boundsFor(first);
        Bounds secondBounds = boundsFor(second);
        float overlapX = Math.min(firstBounds.maxX, secondBounds.maxX) - Math.max(firstBounds.minX, secondBounds.minX);
        float overlapY = Math.min(firstBounds.maxY, secondBounds.maxY) - Math.max(firstBounds.minY, secondBounds.minY);
        float overlapZ = Math.min(firstBounds.maxZ, secondBounds.maxZ) - Math.max(firstBounds.minZ, secondBounds.minZ);
        if (overlapX <= 0f || overlapY <= 0f || overlapZ <= 0f) {
            return;
        }
        float firstWeight = Math.max(0.25f, first.density);
        float secondWeight = Math.max(0.25f, second.density);
        float totalWeight = firstWeight + secondWeight;
        if (overlapX <= overlapZ) {
            float direction = first.x <= second.x ? -1f : 1f;
            float moveFirst = overlapX * (secondWeight / totalWeight);
            float moveSecond = overlapX * (firstWeight / totalWeight);
            first.x += direction * moveFirst;
            second.x -= direction * moveSecond;
            first.velocityX = 0f;
            second.velocityX = 0f;
        } else {
            float direction = first.z <= second.z ? -1f : 1f;
            float moveFirst = overlapZ * (secondWeight / totalWeight);
            float moveSecond = overlapZ * (firstWeight / totalWeight);
            first.z += direction * moveFirst;
            second.z -= direction * moveSecond;
            first.velocityZ = 0f;
            second.velocityZ = 0f;
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
                && object.material != BattleArenaGuMaterial.HOT_GAS
                && object.rigidity >= 0.55f
                && object.cohesion >= 0.45f
                && !object.removed;
    }

    private boolean isFlameInProximity(MutableGuObject first, MutableGuObject second) {
        return isHotGasAuraTouching(first, second) || isHotGasAuraTouching(second, first);
    }

    private boolean isHotGasAuraTouching(MutableGuObject source, MutableGuObject target) {
        if (source == null || target == null || source.material != BattleArenaGuMaterial.HOT_GAS) {
            return false;
        }
        Bounds expanded = boundsFor(source).expand(FLAME_PROXIMITY_RANGE, FLAME_PROXIMITY_RANGE, FLAME_PROXIMITY_RANGE);
        return expanded.overlaps(boundsFor(target));
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
        float heatInfluence = Math.max(source.paths.fire, source.temperature > target.temperature ? 0.15f : 0.0f);
        float coldInfluence = Math.max(source.paths.cold, source.temperature < target.temperature ? 0.12f : 0.0f);
        float influence = Math.max(heatInfluence, coldInfluence);
        if (influence <= 0f) {
            return;
        }
        float transfer = (source.temperature - target.temperature) * Math.min(0.045f, influence * 0.025f);
        target.temperature += transfer;
        applyThermalDimensions(target);
        if (source.material == BattleArenaGuMaterial.HOT_GAS && transfer > 0f) {
            source.temperature = Math.max(AMBIENT_TEMPERATURE,
                    source.temperature - Math.min(FLAME_TRANSFER_TEMPERATURE_LOSS_MAX, transfer * FLAME_TRANSFER_COST_SCALE));
            source.lifetimeTicksRemaining -= Math.max(1, Math.round(transfer * FLAME_TRANSFER_COST_SCALE));
            applyThermalDimensions(source);
        }
    }

    private void applyFlameDecay(MutableGuObject object) {
        if (object == null || object.material != BattleArenaGuMaterial.HOT_GAS) {
            return;
        }
        object.temperature = Math.max(AMBIENT_TEMPERATURE,
                object.temperature - FLAME_PASSIVE_TEMPERATURE_LOSS_PER_TICK);
        object.lifetimeTicksRemaining -= FLAME_PASSIVE_LIFETIME_LOSS_PER_TICK;
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
        BattleArenaGuMaterial previousMaterial = object.material;
        object.material = material;
        object.density = material.defaultDensity;
        object.pressure = Math.max(object.pressure, material.defaultPressure);
        if (material == BattleArenaGuMaterial.WATER) {
            object.cohesion = material.defaultCohesion;
            object.rigidity = material.defaultRigidity;
            object.viscosity = material.defaultViscosity;
        } else {
            object.cohesion = Math.max(object.cohesion, material.defaultCohesion);
            object.rigidity = Math.max(object.rigidity, material.defaultRigidity);
            object.viscosity = Math.max(object.viscosity, material.defaultViscosity);
        }
        BattleArenaGuPathState base = BattleArenaGuPathState.forMaterial(material);
        object.paths = new BattleArenaGuPathState(
                Math.max(base.earth, object.paths.earth),
                Math.max(base.water, object.paths.water),
                Math.max(base.wind, object.paths.wind),
                Math.max(base.fire, object.paths.fire),
                Math.max(base.cold, object.paths.cold),
                Math.max(base.rule, object.paths.rule));
        applyThermalDimensions(object);
        if (previousMaterial != material && material == BattleArenaGuMaterial.ICE) {
            System.out.println("GU object " + object.id + " turned to ice at temperature " + object.temperature);
        }
    }

    private void removeExpiredObjects() {
        Iterator<ActiveWormJob> jobIterator = activeJobs.iterator();
        while (jobIterator.hasNext()) {
            ActiveWormJob job = jobIterator.next();
            if (findObjectById(job.targetObjectId) == null) {
                jobIterator.remove();
            }
        }
        Iterator<MutableGuObject> iterator = objects.iterator();
        while (iterator.hasNext()) {
            MutableGuObject object = iterator.next();
            if (shouldRemove(object)) {
                iterator.remove();
            }
        }
    }

    private boolean shouldRemove(MutableGuObject object) {
        if (object == null) {
            return true;
        }
        if (object.removed) {
            return true;
        }
        if (object.lifetimeTicksRemaining > 0) {
            return false;
        }
        if (isSolidAfterExpiry(object)) {
            return false;
        }
        return object.baseHalfX < MIN_PERSISTENT_HALF_EXTENT
                || object.baseHalfY < MIN_PERSISTENT_HALF_EXTENT
                || object.baseHalfZ < MIN_PERSISTENT_HALF_EXTENT
                || object.y + object.halfY < GROUND_Y - 0.25f;
    }

    private boolean isSolidAfterExpiry(MutableGuObject object) {
        return !isLowSolidity(object);
    }

    private boolean isLowSolidity(MutableGuObject object) {
        if (object == null) {
            return true;
        }
        return solidityOf(object) < SOLIDITY_THRESHOLD;
    }

    private float solidityOf(MutableGuObject object) {
        return ((Math.max(0f, object.rigidity) * 0.65f)
                + (Math.max(0f, object.cohesion) * 0.35f));
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

    private static float approach(float current, float target, float t) {
        return current + ((target - current) * clamp01(t));
    }

    private static float normalizedRange(float value, float start, float end) {
        if (end <= start) {
            return 0f;
        }
        return clamp01((value - start) / (end - start));
    }

    private static final class CastRequest {
        final String playerId;
        final String wormAction;
        final boolean moveForwardHeld;

        CastRequest(String playerId, String wormAction, boolean moveForwardHeld) {
            this.playerId = playerId;
            this.wormAction = wormAction;
            this.moveForwardHeld = moveForwardHeld;
        }
    }

    private static final class MoveChargeState {
        final String playerId;
        final int targetObjectId;
        float accumulatedVelocity;
        float directionX;
        float directionY;
        float directionZ = 1f;

        MoveChargeState(String playerId, int targetObjectId) {
            this.playerId = playerId;
            this.targetObjectId = targetObjectId;
        }
    }

    private enum WormKind {
        SHAPE,
        TEMPERATURE,
        MOVE
    }

    private static final class ActiveWormJob {
        final String ownerPlayerId;
        final int targetObjectId;
        final WormKind kind;
        final String actionKey;
        final float durationSeconds;
        float remainingSeconds;
        final float rate;
        final float directionX;
        final float directionY;
        final float directionZ;
        final float targetHalfX;
        final float targetHalfY;
        final float targetHalfZ;

        private ActiveWormJob(String ownerPlayerId,
                              int targetObjectId,
                              WormKind kind,
                              String actionKey,
                              float durationSeconds,
                              float rate,
                              float directionX,
                              float directionY,
                              float directionZ,
                              float targetHalfX,
                              float targetHalfY,
                              float targetHalfZ) {
            this.ownerPlayerId = ownerPlayerId;
            this.targetObjectId = targetObjectId;
            this.kind = kind;
            this.actionKey = actionKey;
            this.durationSeconds = durationSeconds;
            this.remainingSeconds = durationSeconds;
            this.rate = rate;
            this.directionX = directionX;
            this.directionY = directionY;
            this.directionZ = directionZ;
            this.targetHalfX = targetHalfX;
            this.targetHalfY = targetHalfY;
            this.targetHalfZ = targetHalfZ;
        }

        static ActiveWormJob shape(String ownerPlayerId,
                                   int targetObjectId,
                                   float durationSeconds,
                                   float halfX,
                                   float halfY,
                                   float halfZ) {
            return new ActiveWormJob(
                    ownerPlayerId,
                    targetObjectId,
                    WormKind.SHAPE,
                    null,
                    durationSeconds,
                    0f,
                    0f,
                    0f,
                    0f,
                    halfX,
                    halfY,
                    halfZ);
        }

        static ActiveWormJob temperature(String ownerPlayerId,
                                         int targetObjectId,
                                         String actionKey,
                                         float durationSeconds,
                                         float rate) {
            return new ActiveWormJob(
                    ownerPlayerId,
                    targetObjectId,
                    WormKind.TEMPERATURE,
                    actionKey,
                    durationSeconds,
                    rate,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f);
        }

        static ActiveWormJob move(String ownerPlayerId,
                                  int targetObjectId,
                                  float durationSeconds,
                                  float directionX,
                                  float directionY,
                                  float directionZ,
                                  float rate) {
            return new ActiveWormJob(
                    ownerPlayerId,
                    targetObjectId,
                    WormKind.MOVE,
                    BattleArenaGuWormAction.MOVE_FORWARD,
                    durationSeconds,
                    rate,
                    directionX,
                    directionY,
                    directionZ,
                    0f,
                    0f,
                    0f);
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
        float baseHalfX;
        float baseHalfY;
        float baseHalfZ;
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
        boolean removed;
        boolean phaseLocked;

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

        Bounds expand(float x, float y, float z) {
            return new Bounds(
                    minX - Math.max(0f, x),
                    minY - Math.max(0f, y),
                    minZ - Math.max(0f, z),
                    maxX + Math.max(0f, x),
                    maxY + Math.max(0f, y),
                    maxZ + Math.max(0f, z));
        }

        static Bounds empty() {
            return new Bounds(0f, 0f, 0f, 0f, 0f, 0f);
        }
    }
}
