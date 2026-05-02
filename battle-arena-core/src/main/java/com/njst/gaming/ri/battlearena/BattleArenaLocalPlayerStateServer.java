package com.njst.gaming.ri.battlearena;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaLocalPlayerStateServer implements BattleArenaPlayerStateProvider {
    private static final int TICK_RATE = 60;
    private static final float TICK_SECONDS = 1f / TICK_RATE;
    private static final float WALK_SPEED = 1.6f;
    private static final float RUN_SPEED = 3.2f;
    private static final float TURN_SPEED_DEGREES_PER_SECOND = 145f;
    private static final float ANIMATION_FRAMES_PER_SECOND = 60f;

    private final LinkedHashMap<String, MutablePlayerState> states =
            new LinkedHashMap<String, MutablePlayerState>();
    private final Map<String, BattleArenaPlayerInput> latestInput =
            new LinkedHashMap<String, BattleArenaPlayerInput>();
    private final Map<Integer, Map<String, BattleArenaPlayerInput>> inputByTick =
            new LinkedHashMap<Integer, Map<String, BattleArenaPlayerInput>>();
    private int currentTick;

    public BattleArenaLocalPlayerStateServer(float[][] spawnPositions) {
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
                latestInput.put(state.playerId, input.copy());
            } else {
                input = latestInput.get(state.playerId);
            }
            applyInput(state, input);
            state.animationTick++;
        }
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
        if (input.animationOverride != null && !input.animationOverride.trim().isEmpty()) {
            existing.animationOverride = input.animationOverride;
        }
    }

    private void applyInput(MutablePlayerState state, BattleArenaPlayerInput input) {
        if (input == null) {
            state.setAnimationKey(BattleArenaCharacterController.ANIM_IDLE);
            return;
        }
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
        if (input.animationOverride != null && !input.animationOverride.trim().isEmpty()) {
            state.setAnimationKey(input.animationOverride);
        } else if (Math.abs(moveX) > 0.001f || Math.abs(moveZ) > 0.001f) {
            state.setAnimationKey(input.run ? BattleArenaCharacterController.ANIM_RUN
                    : BattleArenaCharacterController.ANIM_WALK);
        } else {
            state.setAnimationKey(BattleArenaCharacterController.ANIM_IDLE);
        }
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
        int animationTick;

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
                    animationTick * ANIMATION_FRAMES_PER_SECOND / TICK_RATE);
        }

        void setAnimationKey(String nextAnimationKey) {
            String resolvedAnimationKey = nextAnimationKey != null
                    ? nextAnimationKey
                    : BattleArenaCharacterController.ANIM_IDLE;
            if (!resolvedAnimationKey.equals(animationKey)) {
                animationKey = resolvedAnimationKey;
                animationTick = 0;
            }
        }
    }
}
