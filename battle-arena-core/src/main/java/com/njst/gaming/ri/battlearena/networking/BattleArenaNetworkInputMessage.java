package com.njst.gaming.ri.battlearena.networking;

import com.njst.gaming.ri.battlearena.BattleArenaPlayerInput;

public final class BattleArenaNetworkInputMessage {
    public String player;
    public int tick;
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
    public String animationOverride;

    public static BattleArenaNetworkInputMessage fromInput(String player,
                                                           int tick,
                                                           BattleArenaPlayerInput input) {
        BattleArenaNetworkInputMessage message = new BattleArenaNetworkInputMessage();
        message.player = player;
        message.tick = tick;
        if (input == null) {
            return message;
        }
        message.moveX = input.moveX;
        message.moveZ = input.moveZ;
        message.turn = input.turn;
        message.run = input.run;
        message.jumpPressed = input.jumpPressed;
        message.punchPressed = input.punchPressed;
        message.kickPressed = input.kickPressed;
        message.castPressed = input.castPressed;
        message.stepLeftPressed = input.stepLeftPressed;
        message.stepRightPressed = input.stepRightPressed;
        message.takeHitPressed = input.takeHitPressed;
        message.animationOverride = input.animationOverride;
        return message;
    }

    public BattleArenaPlayerInput toInput() {
        BattleArenaPlayerInput input = new BattleArenaPlayerInput();
        input.moveX = moveX;
        input.moveZ = moveZ;
        input.turn = turn;
        input.run = run;
        input.jumpPressed = jumpPressed;
        input.punchPressed = punchPressed;
        input.kickPressed = kickPressed;
        input.castPressed = castPressed;
        input.stepLeftPressed = stepLeftPressed;
        input.stepRightPressed = stepRightPressed;
        input.takeHitPressed = takeHitPressed;
        input.animationOverride = animationOverride;
        return input;
    }
}
