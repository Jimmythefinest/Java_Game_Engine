package com.njst.gaming.ri.battlearena;

public final class BattleArenaPlayerInput {
    public float moveX;
    public float moveZ;
    public float turn;
    public boolean run;
    public boolean jumpPressed;
    public boolean punchPressed;
    public boolean kickPressed;
    public boolean castPressed;
    public boolean stepLeftPressed;
    public boolean stepRightPressed;
    public boolean takeHitPressed;
    public float knockbackX;
    public float knockbackZ;
    public String animationOverride;
    public String guLoadoutKey;

    public BattleArenaPlayerInput copy() {
        BattleArenaPlayerInput copy = new BattleArenaPlayerInput();
        copy.moveX = moveX;
        copy.moveZ = moveZ;
        copy.turn = turn;
        copy.run = run;
        copy.jumpPressed = jumpPressed;
        copy.punchPressed = punchPressed;
        copy.kickPressed = kickPressed;
        copy.castPressed = castPressed;
        copy.stepLeftPressed = stepLeftPressed;
        copy.stepRightPressed = stepRightPressed;
        copy.takeHitPressed = takeHitPressed;
        copy.knockbackX = knockbackX;
        copy.knockbackZ = knockbackZ;
        copy.animationOverride = animationOverride;
        copy.guLoadoutKey = guLoadoutKey;
        return copy;
    }

    public BattleArenaPlayerInput copyContinuous() {
        BattleArenaPlayerInput copy = new BattleArenaPlayerInput();
        copy.moveX = moveX;
        copy.moveZ = moveZ;
        copy.turn = turn;
        copy.run = run;
        return copy;
    }
}
