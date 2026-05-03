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
    }

    private void applyInput(MutablePlayerState state, BattleArenaPlayerInput input) {
        if (input == null) {
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
        float headingRadians = (float) Math.toRadians(state.headingDegrees);
        float forwardX = (float) Math.sin(headingRadians);
        float forwardZ = (float) Math.cos(headingRadians);
        float rightX = (float) Math.cos(headingRadians);
        float rightZ = -(float) Math.sin(headingRadians);
        state.x += ((forwardX * moveZ) + (rightX * moveX)) * speed * TICK_SECONDS;
        state.z += ((forwardZ * moveZ) + (rightZ * moveX)) * speed * TICK_SECONDS;
        state.x += input.knockbackX;
        state.z += input.knockbackZ;
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

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
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
                    animationFrame);
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
