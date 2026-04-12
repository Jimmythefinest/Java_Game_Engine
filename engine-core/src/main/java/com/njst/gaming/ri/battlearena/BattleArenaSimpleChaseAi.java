package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;

final class BattleArenaSimpleChaseAi implements BattleArenaCharacterBrain {
    private static final float TURN_DEADZONE_DEGREES = 6f;
    private static final float ATTACK_RANGE = 1.35f;
    private static final float RUN_RANGE = 3.5f;
    private static final float STRAFE_DISTANCE = 0.85f;
    private static final float ATTACK_COOLDOWN_SECONDS = 1.1f;

    private float attackCooldownRemaining = 0f;
    private boolean useKickNext = false;

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

        if (distance > ATTACK_RANGE) {
            controls.forwardInput = distance > STRAFE_DISTANCE ? 1f : 0f;
            controls.runDown = distance > RUN_RANGE;
            return;
        }

        if (Math.abs(headingDelta) > 18f) {
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

    private float normalizeAngle(float angleDegrees) {
        float normalized = angleDegrees % 360f;
        if (normalized > 180f) {
            normalized -= 360f;
        } else if (normalized < -180f) {
            normalized += 360f;
        }
        return normalized;
    }
}
