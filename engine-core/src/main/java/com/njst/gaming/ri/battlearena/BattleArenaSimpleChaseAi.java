package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;

final class BattleArenaSimpleChaseAi implements BattleArenaCharacterBrain {
    private static final float TURN_DEADZONE_DEGREES = 6f;
    private static final float MELEE_FACING_DEGREES = 18f;
    private static final float TOOL_FACING_DEGREES = 16f;
    private static final float ATTACK_RANGE = 1.35f;
    private static final float FIREBALL_MIN_RANGE = 2.4f;
    private static final float FIREBALL_MAX_RANGE = 7.5f;
    private static final float MUD_WALL_MIN_RANGE = 1.8f;
    private static final float MUD_WALL_MAX_RANGE = 4.2f;
    private static final float RUN_START_RANGE = 3.8f;
    private static final float RUN_STOP_RANGE = 3.0f;
    private static final float STRAFE_DISTANCE = 0.85f;
    private static final float STRAFE_REPOSITION_RANGE = 2.1f;
    private static final float STRAFE_COOLDOWN_SECONDS = 0.6f;
    private static final float ATTACK_COOLDOWN_SECONDS = 1.1f;
    private static final float FIREBALL_COOLDOWN_SECONDS = 2.6f;
    private static final float MUD_WALL_COOLDOWN_SECONDS = 4.0f;

    private float attackCooldownRemaining = 0f;
    private float fireballCooldownRemaining = 0f;
    private float mudWallCooldownRemaining = 0f;
    private float strafeCooldownRemaining = 0f;
    private boolean useKickNext = false;
    private boolean strafeLeftNext = true;
    private boolean incomingFireballThreat = false;
    private boolean chasingAtRunSpeed = false;

    @Override
    public void update(BattleArenaCharacterRuntime self,
                       BattleArenaCharacterRuntime opponent,
                       BattleArenaCharacterControlState controls,
                       float deltaSeconds) {
        controls.clear();
        if (self == null || opponent == null) {
            return;
        }

        attackCooldownRemaining = Math.max(0f, attackCooldownRemaining - Math.max(0f, deltaSeconds));
        fireballCooldownRemaining = Math.max(0f, fireballCooldownRemaining - Math.max(0f, deltaSeconds));
        mudWallCooldownRemaining = Math.max(0f, mudWallCooldownRemaining - Math.max(0f, deltaSeconds));
        strafeCooldownRemaining = Math.max(0f, strafeCooldownRemaining - Math.max(0f, deltaSeconds));

        Vector3 selfPosition = self.getPosition();
        Vector3 opponentPosition = opponent.getPosition();
        float dx = opponentPosition.x - selfPosition.x;
        float dz = opponentPosition.z - selfPosition.z;
        float distance = (float) Math.sqrt((dx * dx) + (dz * dz));
        if (distance <= 0.0001f) {
            return;
        }

        float targetHeading = (float) Math.toDegrees(Math.atan2(dx, dz));
        float headingDelta = normalizeAngle(targetHeading - self.getHeadingDegrees());

        if (headingDelta > TURN_DEADZONE_DEGREES) {
            controls.turnInput = 1f;
        } else if (headingDelta < -TURN_DEADZONE_DEGREES) {
            controls.turnInput = -1f;
        }

        if (shouldCastMudWall(distance, headingDelta)) {
            controls.castMudWallPressed = true;
            mudWallCooldownRemaining = MUD_WALL_COOLDOWN_SECONDS;
            return;
        }

        if (shouldCastFireball(distance, headingDelta)) {
            controls.castFireballPressed = true;
            log("NPC fireball requested distance=" + distance + " headingDelta=" + headingDelta);
            fireballCooldownRemaining = FIREBALL_COOLDOWN_SECONDS;
            return;
        }

        if (distance > ATTACK_RANGE) {
            controls.forwardInput = distance > STRAFE_DISTANCE ? 1f : 0f;
            chasingAtRunSpeed = updateRunChaseState(distance);
            controls.runDown = chasingAtRunSpeed;
            if (distance < STRAFE_REPOSITION_RANGE
                    && Math.abs(headingDelta) <= TOOL_FACING_DEGREES
                    && strafeCooldownRemaining <= 0f) {
                if (strafeLeftNext) {
                    controls.stepLeftPressed = true;
                } else {
                    controls.stepRightPressed = true;
                }
                strafeLeftNext = !strafeLeftNext;
                strafeCooldownRemaining = STRAFE_COOLDOWN_SECONDS;
            }
            return;
        }

        if (Math.abs(headingDelta) > MELEE_FACING_DEGREES) {
            return;
        }
        if (attackCooldownRemaining > 0f) {
            return;
        }

        if (useKickNext) {
            controls.kickPressed = true;
        } else {
            controls.punchPressed = true;
        }
        useKickNext = !useKickNext;
        attackCooldownRemaining = ATTACK_COOLDOWN_SECONDS;
    }

    private boolean shouldCastFireball(float distance, float headingDelta) {
        return fireballCooldownRemaining <= 0f
                && distance >= FIREBALL_MIN_RANGE
                && distance <= FIREBALL_MAX_RANGE
                && Math.abs(headingDelta) <= TOOL_FACING_DEGREES;
    }

    private boolean shouldCastMudWall(float distance, float headingDelta) {
        return incomingFireballThreat
                && mudWallCooldownRemaining <= 0f
                && distance >= MUD_WALL_MIN_RANGE
                && distance <= MUD_WALL_MAX_RANGE
                && Math.abs(headingDelta) <= TOOL_FACING_DEGREES;
    }

    void setIncomingFireballThreat(boolean incomingFireballThreat) {
        this.incomingFireballThreat = incomingFireballThreat;
    }

    private boolean updateRunChaseState(float distance) {
        if (distance >= RUN_START_RANGE) {
            return true;
        }
        if (distance <= RUN_STOP_RANGE) {
            return false;
        }
        return chasingAtRunSpeed;
    }

    private float normalizeAngle(float angleDegrees) {
        float normalized = angleDegrees % 360f;
        if (normalized > 180f) {
            normalized -= 360f;
        } else if (normalized < -180f) {
            normalized += 360f;
        }
        return normalized;
    }

    private void log(String message) {
        System.out.println("[BattleArenaAI] " + message);
    }
}
