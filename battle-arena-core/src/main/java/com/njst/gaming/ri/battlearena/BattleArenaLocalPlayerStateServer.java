package com.njst.gaming.ri.battlearena;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaLocalPlayerStateServer implements BattleArenaPlayerStateProvider {
    public static final int TICK_RATE = 60;
    public static final float TICK_SECONDS = 1f / TICK_RATE;
    private static final float WALK_SPEED = 1.6f;
    private static final float RUN_SPEED = 3.2f;
    private static final float TURN_SPEED_DEGREES_PER_SECOND = 145f;
    private static final float BODY_RADIUS = 0.42f;
    private static final float BODY_DIAMETER = BODY_RADIUS * 2f;
    private static final float BODY_HALF_HEIGHT = 0.95f;
    private static final float GU_BLOCKING_RIGIDITY = 0.55f;
    private static final float GU_BLOCKING_COHESION = 0.45f;
    private static final float GU_RESISTIVE_SOLIDITY = 0.12f;
    private static final float GU_MAX_SLOWDOWN = 0.72f;
    private static final int JUMP_TICKS = 44;
    private static final int PUNCH_TICKS = 32;
    private static final int KICK_TICKS = 36;
    private static final int CAST_TICKS = 32;
    private static final int SIDE_STEP_TICKS = 26;
    private static final int TAKE_HIT_TICKS = 22;

    private final LinkedHashMap<String, MutablePlayerState> states =
            new LinkedHashMap<String, MutablePlayerState>();
    private final Map<String, BattleArenaPlayerInput> latestInput =
            new LinkedHashMap<String, BattleArenaPlayerInput>();
    private final Map<Integer, Map<String, BattleArenaPlayerInput>> inputByTick =
            new LinkedHashMap<Integer, Map<String, BattleArenaPlayerInput>>();
    private final Map<String, BattleArenaAnimationTiming> animationTimings;
    private int currentTick;

    public BattleArenaLocalPlayerStateServer(float[][] spawnPositions) {
        this(spawnPositions, null);
    }

    public BattleArenaLocalPlayerStateServer(float[][] spawnPositions,
                                             Map<String, BattleArenaAnimationTiming> animationTimings) {
        this.animationTimings = animationTimings != null
                ? new LinkedHashMap<String, BattleArenaAnimationTiming>(animationTimings)
                : new LinkedHashMap<String, BattleArenaAnimationTiming>();
        if (spawnPositions == null || spawnPositions.length == 0) {
            addPlayer("player_0", 0f, 0f, 0f);
            return;
        }
        for (int i = 0; i < spawnPositions.length; i++) {
            float[] position = spawnPositions[i];
            addPlayer(
                    "player_" + i,
                    position != null && position.length > 0 ? position[0] : 0f,
                    position != null && position.length > 1 ? position[1] : 0f,
                    position != null && position.length > 2 ? position[2] : 0f);
        }
    }

    public void addPlayer(String playerId, float x, float y, float z) {
        String resolvedId = playerId != null ? playerId : "player_" + states.size();
        states.put(resolvedId, new MutablePlayerState(resolvedId, x, y, z));
    }

    @Override
    public List<BattleArenaPlayerState> initialStates() {
        return snapshotStates();
    }

    @Override
    public int currentTick() {
        return currentTick;
    }

    @Override
    public float tickSeconds() {
        return TICK_SECONDS;
    }

    @Override
    public void submitInput(String playerId, int tick, BattleArenaPlayerInput input) {
        if (playerId == null || !states.containsKey(playerId)) {
            return;
        }
        if (tick < currentTick || input == null) {
            return;
        }
        Map<String, BattleArenaPlayerInput> tickInputs = inputByTick.get(tick);
        if (tickInputs == null) {
            tickInputs = new LinkedHashMap<String, BattleArenaPlayerInput>();
            inputByTick.put(tick, tickInputs);
        }
        mergeInput(tickInputs, playerId, input);
    }

    @Override
    public void tick() {
        Map<String, BattleArenaPlayerInput> tickInputs = inputByTick.remove(currentTick);
        for (MutablePlayerState state : states.values()) {
            BattleArenaPlayerInput input = tickInputs != null ? tickInputs.get(state.playerId) : null;
            if (input != null) {
                latestInput.put(state.playerId, input.copyContinuous());
            } else {
                input = latestInput.get(state.playerId);
            }
            applyInput(state, input);
            state.advanceAnimationFrame(framesPerTick(state.animationKey));
        }
        resolveBodyOverlaps();
        currentTick++;
    }

    @Override
    public List<BattleArenaPlayerState> snapshotStates() {
        ArrayList<BattleArenaPlayerState> snapshot =
                new ArrayList<BattleArenaPlayerState>(states.size());
        for (MutablePlayerState state : states.values()) {
            snapshot.add(state.snapshot());
        }
        return snapshot;
    }

    @Override
    public BattleArenaPlayerState stateForPlayer(String playerId) {
        MutablePlayerState state = states.get(playerId);
        return state != null ? state.snapshot() : null;
    }

    public void resolveGuObjectCollisions(List<BattleArenaGuObjectState> guObjects) {
        if (guObjects == null || guObjects.isEmpty()) {
            return;
        }
        for (MutablePlayerState player : states.values()) {
            for (BattleArenaGuObjectState guObject : guObjects) {
                resolveGuObjectCollision(player, guObject);
            }
        }
        resolveBodyOverlaps();
    }

    private void mergeInput(Map<String, BattleArenaPlayerInput> tickInputs,
                            String playerId,
                            BattleArenaPlayerInput input) {
        BattleArenaPlayerInput existing = tickInputs.get(playerId);
        if (existing == null) {
            tickInputs.put(playerId, input.copy());
            return;
        }
        existing.moveX = input.moveX;
        existing.moveZ = input.moveZ;
        existing.turn = input.turn;
        existing.run = input.run;
        existing.jumpPressed |= input.jumpPressed;
        existing.punchPressed |= input.punchPressed;
        existing.kickPressed |= input.kickPressed;
        existing.castPressed |= input.castPressed;
        existing.stepLeftPressed |= input.stepLeftPressed;
        existing.stepRightPressed |= input.stepRightPressed;
        existing.takeHitPressed |= input.takeHitPressed;
        existing.knockbackX += input.knockbackX;
        existing.knockbackZ += input.knockbackZ;
        if (input.animationOverride != null && !input.animationOverride.trim().isEmpty()) {
            existing.animationOverride = input.animationOverride;
        }
        if (input.guWormAction != null && !input.guWormAction.trim().isEmpty()) {
            existing.guWormAction = input.guWormAction;
        }
    }

    private void applyInput(MutablePlayerState state, BattleArenaPlayerInput input) {
        if (input == null) {
            state.velocityX = 0f;
            state.velocityZ = 0f;
            updateActionLock(state);
            if (!state.actionLocked()) {
                state.setAnimationKey(BattleArenaCharacterController.ANIM_IDLE);
            }
            return;
        }
        startActionIfRequested(state, input);
        float moveX = clamp(input.moveX);
        float moveZ = clamp(input.moveZ);
        float turn = clamp(input.turn);
        state.headingDegrees += turn * TURN_SPEED_DEGREES_PER_SECOND * TICK_SECONDS;
        state.headingDegrees = wrapDegrees(state.headingDegrees);
        float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length > 1f) {
            moveX /= length;
            moveZ /= length;
        }
        float speed = input.run ? RUN_SPEED : WALK_SPEED;
        float startX = state.x;
        float startZ = state.z;
        float headingRadians = (float) Math.toRadians(state.headingDegrees);
        float forwardX = (float) Math.sin(headingRadians);
        float forwardZ = (float) Math.cos(headingRadians);
        float rightX = (float) Math.cos(headingRadians);
        float rightZ = -(float) Math.sin(headingRadians);
        state.lastMoveDeltaX = ((forwardX * moveZ) + (rightX * moveX)) * speed * TICK_SECONDS;
        state.lastMoveDeltaZ = ((forwardZ * moveZ) + (rightZ * moveX)) * speed * TICK_SECONDS;
        state.x += state.lastMoveDeltaX;
        state.z += state.lastMoveDeltaZ;
        state.x += input.knockbackX;
        state.z += input.knockbackZ;
        state.velocityX = (state.x - startX) / TICK_SECONDS;
        state.velocityZ = (state.z - startZ) / TICK_SECONDS;
        updateActionLock(state);
        if (state.actionLocked()) {
            return;
        }
        if (input.animationOverride != null && !input.animationOverride.trim().isEmpty()) {
            state.setAnimationKey(input.animationOverride);
        } else if (moveZ < -0.001f) {
            state.setAnimationKey(BattleArenaCharacterController.ANIM_WALK_BACKWARD);
        } else if (Math.abs(moveX) > 0.001f || Math.abs(moveZ) > 0.001f) {
            state.setAnimationKey(input.run ? BattleArenaCharacterController.ANIM_RUN
                    : BattleArenaCharacterController.ANIM_WALK);
        } else {
            state.setAnimationKey(BattleArenaCharacterController.ANIM_IDLE);
        }
    }

    private void startActionIfRequested(MutablePlayerState state, BattleArenaPlayerInput input) {
        if (state.actionLocked()) {
            return;
        }
        if (input.takeHitPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_TAKE_HIT,
                    lockTicks(BattleArenaCharacterController.ANIM_TAKE_HIT));
        } else if (input.punchPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_PUNCH,
                    lockTicks(BattleArenaCharacterController.ANIM_PUNCH));
        } else if (input.kickPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_KICK,
                    lockTicks(BattleArenaCharacterController.ANIM_KICK));
        } else if (input.castPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_CAST,
                    lockTicks(BattleArenaCharacterController.ANIM_CAST));
        } else if (input.jumpPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_JUMP,
                    lockTicks(BattleArenaCharacterController.ANIM_JUMP));
        } else if (input.stepLeftPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_LEFTSIDE_STEP,
                    lockTicks(BattleArenaCharacterController.ANIM_LEFTSIDE_STEP));
        } else if (input.stepRightPressed) {
            state.startLockedAnimation(BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP,
                    lockTicks(BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP));
        }
    }

    private void updateActionLock(MutablePlayerState state) {
        if (state.lockTicksRemaining > 0) {
            state.lockTicksRemaining--;
        }
    }

    private float framesPerTick(String animationKey) {
        BattleArenaAnimationTiming timing = animationTimings.get(animationKey);
        float framesPerSecond = timing != null && timing.framesPerSecond > 0f
                ? timing.framesPerSecond
                : TICK_RATE;
        return framesPerSecond * TICK_SECONDS;
    }

    private int lockTicks(String animationKey) {
        BattleArenaAnimationTiming timing = animationTimings.get(animationKey);
        if (timing != null && timing.lockTicks > 0) {
            return timing.lockTicks;
        }
        return fallbackLockTicks(animationKey);
    }

    private static int fallbackLockTicks(String animationKey) {
        if (BattleArenaCharacterController.ANIM_TAKE_HIT.equals(animationKey)) {
            return TAKE_HIT_TICKS;
        }
        if (BattleArenaCharacterController.ANIM_PUNCH.equals(animationKey)) {
            return PUNCH_TICKS;
        }
        if (BattleArenaCharacterController.ANIM_KICK.equals(animationKey)) {
            return KICK_TICKS;
        }
        if (BattleArenaCharacterController.ANIM_CAST.equals(animationKey)) {
            return CAST_TICKS;
        }
        if (BattleArenaCharacterController.ANIM_JUMP.equals(animationKey)) {
            return JUMP_TICKS;
        }
        if (BattleArenaCharacterController.ANIM_LEFTSIDE_STEP.equals(animationKey)
                || BattleArenaCharacterController.ANIM_RIGHTSIDE_STEP.equals(animationKey)) {
            return SIDE_STEP_TICKS;
        }
        return TICK_RATE;
    }

    private void resolveBodyOverlaps() {
        ArrayList<MutablePlayerState> playerStates =
                new ArrayList<MutablePlayerState>(states.values());
        for (int i = 0; i < playerStates.size(); i++) {
            MutablePlayerState first = playerStates.get(i);
            for (int j = i + 1; j < playerStates.size(); j++) {
                MutablePlayerState second = playerStates.get(j);
                resolveBodyOverlap(first, second);
            }
        }
    }

    private void resolveBodyOverlap(MutablePlayerState first, MutablePlayerState second) {
        float dx = second.x - first.x;
        float dz = second.z - first.z;
        float distanceSquared = dx * dx + dz * dz;
        if (distanceSquared >= BODY_DIAMETER * BODY_DIAMETER) {
            return;
        }
        float distance = (float) Math.sqrt(distanceSquared);
        float normalX;
        float normalZ;
        if (distance < 0.0001f) {
            normalX = 1f;
            normalZ = 0f;
            distance = 0f;
        } else {
            normalX = dx / distance;
            normalZ = dz / distance;
        }
        float push = (BODY_DIAMETER - distance) * 0.5f;
        first.x -= normalX * push;
        first.z -= normalZ * push;
        second.x += normalX * push;
        second.z += normalZ * push;
    }

    private void resolveGuObjectCollision(MutablePlayerState player, BattleArenaGuObjectState guObject) {
        if (player == null || guObject == null) {
            return;
        }
        if (!verticalBodyOverlap(player, guObject)) {
            return;
        }
        if (!bodyTouchesGuObject(player, guObject)) {
            return;
        }
        if (!isBlockingGuObject(guObject)) {
            applyGuSlowdown(player, guObject);
            return;
        }
        resolveBlockingGuObjectCollision(player, guObject);
    }

    private void resolveBlockingGuObjectCollision(MutablePlayerState player, BattleArenaGuObjectState guObject) {
        float headingRadians = (float) Math.toRadians(guObject.headingDegrees);
        float cos = (float) Math.cos(headingRadians);
        float sin = (float) Math.sin(headingRadians);
        float relativeX = player.x - guObject.x;
        float relativeZ = player.z - guObject.z;
        float localX = (relativeX * cos) - (relativeZ * sin);
        float localZ = (relativeX * sin) + (relativeZ * cos);
        float closestX = clamp(localX, -guObject.halfX, guObject.halfX);
        float closestZ = clamp(localZ, -guObject.halfZ, guObject.halfZ);
        float localDx = localX - closestX;
        float localDz = localZ - closestZ;
        float distanceSquared = localDx * localDx + localDz * localDz;
        if (distanceSquared > BODY_RADIUS * BODY_RADIUS) {
            return;
        }
        if (distanceSquared > 0.000001f) {
            float distance = (float) Math.sqrt(distanceSquared);
            float push = BODY_RADIUS - distance;
            float pushLocalX = (localDx / distance) * push;
            float pushLocalZ = (localDz / distance) * push;
            player.x += (pushLocalX * cos) + (pushLocalZ * sin);
            player.z += (-pushLocalX * sin) + (pushLocalZ * cos);
            return;
        }

        float pushLeft = localX + guObject.halfX;
        float pushRight = guObject.halfX - localX;
        float pushBack = localZ + guObject.halfZ;
        float pushForward = guObject.halfZ - localZ;
        float smallest = pushLeft;
        int axis = 0;
        float direction = -1f;
        if (pushRight < smallest) {
            smallest = pushRight;
            axis = 0;
            direction = 1f;
        }
        if (pushBack < smallest) {
            smallest = pushBack;
            axis = 1;
            direction = -1f;
        }
        if (pushForward < smallest) {
            smallest = pushForward;
            axis = 1;
            direction = 1f;
        }
        float correction = smallest + BODY_RADIUS;
        float correctionLocalX = 0f;
        float correctionLocalZ = 0f;
        if (axis == 0) {
            correctionLocalX = direction * correction;
        } else {
            correctionLocalZ = direction * correction;
        }
        player.x += (correctionLocalX * cos) + (correctionLocalZ * sin);
        player.z += (-correctionLocalX * sin) + (correctionLocalZ * cos);
    }

    private boolean bodyTouchesGuObject(MutablePlayerState player, BattleArenaGuObjectState guObject) {
        float headingRadians = (float) Math.toRadians(guObject.headingDegrees);
        float cos = (float) Math.cos(headingRadians);
        float sin = (float) Math.sin(headingRadians);
        float relativeX = player.x - guObject.x;
        float relativeZ = player.z - guObject.z;
        float localX = (relativeX * cos) - (relativeZ * sin);
        float localZ = (relativeX * sin) + (relativeZ * cos);
        float closestX = clamp(localX, -guObject.halfX, guObject.halfX);
        float closestZ = clamp(localZ, -guObject.halfZ, guObject.halfZ);
        float localDx = localX - closestX;
        float localDz = localZ - closestZ;
        return (localDx * localDx) + (localDz * localDz) <= BODY_RADIUS * BODY_RADIUS;
    }

    private void applyGuSlowdown(MutablePlayerState player, BattleArenaGuObjectState guObject) {
        float solidity = guSolidity(guObject);
        if (solidity < GU_RESISTIVE_SOLIDITY) {
            return;
        }
        float slowdown = normalizedRange(solidity, GU_RESISTIVE_SOLIDITY, GU_BLOCKING_RIGIDITY);
        slowdown = Math.min(GU_MAX_SLOWDOWN, slowdown * GU_MAX_SLOWDOWN);
        player.x -= player.lastMoveDeltaX * slowdown;
        player.z -= player.lastMoveDeltaZ * slowdown;
    }

    private boolean isBlockingGuObject(BattleArenaGuObjectState guObject) {
        return guObject != null
                && guObject.halfX > 0f
                && guObject.halfY > 0f
                && guObject.halfZ > 0f
                && !BattleArenaGuMaterial.HOT_GAS.key.equals(guObject.material)
                && guObject.rigidity >= GU_BLOCKING_RIGIDITY
                && guObject.cohesion >= GU_BLOCKING_COHESION;
    }

    private float guSolidity(BattleArenaGuObjectState guObject) {
        if (guObject == null) {
            return 0f;
        }
        return (Math.max(0f, guObject.rigidity) * 0.65f)
                + (Math.max(0f, guObject.cohesion) * 0.35f);
    }

    private boolean verticalBodyOverlap(MutablePlayerState player, BattleArenaGuObjectState guObject) {
        float bodyMinY = player.y;
        float bodyMaxY = player.y + BODY_HALF_HEIGHT * 2f;
        float objectMinY = guObject.y - guObject.halfY;
        float objectMaxY = guObject.y + guObject.halfY;
        return bodyMinY <= objectMaxY && bodyMaxY >= objectMinY;
    }

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float normalizedRange(float value, float start, float end) {
        if (end <= start) {
            return 0f;
        }
        return clamp((value - start) / (end - start), 0f, 1f);
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360f;
        if (wrapped < -180f) {
            return wrapped + 360f;
        }
        if (wrapped > 180f) {
            return wrapped - 360f;
        }
        return wrapped;
    }

    private static final class MutablePlayerState {
        final String playerId;
        float x;
        float y;
        float z;
        float headingDegrees;
        float lastMoveDeltaX;
        float lastMoveDeltaZ;
        float velocityX;
        float velocityZ;
        float strength;
        String animationKey = BattleArenaCharacterController.ANIM_IDLE;
        float animationFrame;
        int lockTicksRemaining;

        MutablePlayerState(String playerId, float x, float y, float z) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        BattleArenaPlayerState snapshot() {
            return new BattleArenaPlayerState(
                    playerId,
                    x,
                    y,
                    z,
                    headingDegrees,
                    animationKey,
                    animationFrame,
                    velocityX,
                    velocityZ,
                    strength,
                    100f,
                    100f);
        }

        void setAnimationKey(String nextAnimationKey) {
            String resolvedAnimationKey = nextAnimationKey != null
                    ? nextAnimationKey
                    : BattleArenaCharacterController.ANIM_IDLE;
            if (!resolvedAnimationKey.equals(animationKey)) {
                animationKey = resolvedAnimationKey;
                animationFrame = 0f;
            }
        }

        void advanceAnimationFrame(float framesPerTick) {
            animationFrame += Math.max(0f, framesPerTick);
        }

        boolean actionLocked() {
            return lockTicksRemaining > 0;
        }

        void startLockedAnimation(String nextAnimationKey, int ticks) {
            setAnimationKey(nextAnimationKey);
            lockTicksRemaining = Math.max(1, ticks);
        }
    }
}
