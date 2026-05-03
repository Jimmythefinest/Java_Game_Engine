package com.njst.gaming.ri.battlearena;

import java.util.List;

public final class BattleArenaChaseNpcController implements BattleArenaNpcController {
    private static final float ATTACK_RANGE = 1.15f;
    private static final float STOP_RANGE = 0.85f;
    private static final float RUN_RANGE = 3.2f;
    private static final float TURN_DEADZONE_DEGREES = 5f;
    private static final int ATTACK_COOLDOWN_TICKS = 70;

    private final String targetPlayerId;
    private int nextAttackTick;

    public BattleArenaChaseNpcController(String targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    @Override
    public BattleArenaPlayerInput decide(BattleArenaPlayerState self,
                                         List<BattleArenaPlayerState> players,
                                         int tick) {
        BattleArenaPlayerInput input = new BattleArenaPlayerInput();
        BattleArenaPlayerState target = findTarget(self, players);
        if (self == null || target == null) {
            return input;
        }

        float dx = target.x - self.x;
        float dz = target.z - self.z;
        float distanceSquared = dx * dx + dz * dz;
        float distance = (float) Math.sqrt(distanceSquared);
        float desiredHeading = (float) Math.toDegrees(Math.atan2(dx, dz));
        float headingError = wrapDegrees(desiredHeading - self.headingDegrees);
        if (Math.abs(headingError) > TURN_DEADZONE_DEGREES) {
            input.turn = headingError > 0f ? 1f : -1f;
        }
        if (distance > STOP_RANGE) {
            input.moveZ = 1f;
            input.run = distance > RUN_RANGE;
        }
        if (distance <= ATTACK_RANGE && Math.abs(headingError) < 35f && tick >= nextAttackTick) {
            input.punchPressed = true;
            nextAttackTick = tick + ATTACK_COOLDOWN_TICKS;
        }
        return input;
    }

    private BattleArenaPlayerState findTarget(BattleArenaPlayerState self,
                                              List<BattleArenaPlayerState> players) {
        BattleArenaPlayerState preferred = null;
        BattleArenaPlayerState nearest = null;
        float nearestDistanceSquared = Float.MAX_VALUE;
        if (players == null) {
            return null;
        }
        for (BattleArenaPlayerState player : players) {
            if (player == null || self != null && player.playerId.equals(self.playerId)) {
                continue;
            }
            if (targetPlayerId != null && targetPlayerId.equals(player.playerId)) {
                preferred = player;
                break;
            }
            if (self != null) {
                float dx = player.x - self.x;
                float dz = player.z - self.z;
                float distanceSquared = dx * dx + dz * dz;
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = player;
                }
            }
        }
        return preferred != null ? preferred : nearest;
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
}
