package com.njst.gaming.ri.battlearena;

public final class BattleArenaPlayerInput {
    public float moveX;
    public float moveZ;
    public float turn;
    public boolean run;
    public String animationOverride;

    public BattleArenaPlayerInput copy() {
        BattleArenaPlayerInput copy = new BattleArenaPlayerInput();
        copy.moveX = moveX;
        copy.moveZ = moveZ;
        copy.turn = turn;
        copy.run = run;
        copy.animationOverride = animationOverride;
        return copy;
    }
}
