package com.njst.gaming.ri.battlearena.networking;

import com.google.gson.annotations.SerializedName;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterControlState;

final class BattleArenaTcpControlSnapshot {
    String player;
    float forward;
    float turn;
    boolean run;
    boolean jump;
    boolean punch;
    boolean kick;
    boolean fireball;
    @SerializedName(value = "mudWall", alternate = { "mud_wall" })
    boolean mudWall;
    @SerializedName(value = "createWater", alternate = { "create_water" })
    boolean createWater;
    @SerializedName(value = "createIce", alternate = { "create_ice" })
    boolean createIce;
    @SerializedName(value = "createEarth", alternate = { "create_earth" })
    boolean createEarth;
    @SerializedName(value = "shapeSpear", alternate = { "shape_spear" })
    boolean shapeSpear;
    @SerializedName(value = "shapeWall", alternate = { "shape_wall" })
    boolean shapeWall;
    boolean cool;
    boolean heat;
    @SerializedName(value = "moveForward", alternate = { "move_forward" })
    boolean moveForward;
    @SerializedName(value = "moveForwardDown", alternate = { "move_forward_down" })
    boolean moveForwardDown;
    @SerializedName(value = "stepLeft", alternate = { "step_left" })
    boolean stepLeft;
    @SerializedName(value = "stepRight", alternate = { "step_right" })
    boolean stepRight;
    boolean burst;

    static BattleArenaTcpControlSnapshot fromControls(String player, BattleArenaCharacterControlState controls) {
        BattleArenaTcpControlSnapshot snapshot = new BattleArenaTcpControlSnapshot();
        snapshot.player = player;
        if (controls == null) {
            return snapshot;
        }
        snapshot.forward = clamp(controls.forwardInput);
        snapshot.turn = clamp(controls.turnInput);
        snapshot.run = controls.runDown;
        snapshot.jump = controls.jumpPressed;
        snapshot.punch = controls.punchPressed;
        snapshot.kick = controls.kickPressed;
        snapshot.fireball = controls.castFireballPressed;
        snapshot.mudWall = controls.castMudWallPressed;
        snapshot.createWater = controls.guCreateWaterPressed;
        snapshot.createIce = controls.guCreateIcePressed;
        snapshot.createEarth = controls.guCreateEarthPressed;
        snapshot.shapeSpear = controls.guShapeSpearPressed;
        snapshot.shapeWall = controls.guShapeWallPressed;
        snapshot.cool = controls.guCoolPressed;
        snapshot.heat = controls.guHeatPressed;
        snapshot.moveForward = controls.guMoveForwardPressed;
        snapshot.moveForwardDown = controls.guMoveForwardDown;
        snapshot.stepLeft = controls.stepLeftPressed;
        snapshot.stepRight = controls.stepRightPressed;
        snapshot.burst = controls.burstPressed;
        return snapshot;
    }

    void copyTo(BattleArenaCharacterControlState controls) {
        if (controls == null) {
            return;
        }
        controls.forwardInput = clamp(forward);
        controls.turnInput = clamp(turn);
        controls.runDown = run;
        controls.jumpPressed = jump;
        controls.punchPressed = punch;
        controls.kickPressed = kick;
        controls.castFireballPressed = fireball;
        controls.castMudWallPressed = mudWall;
        controls.guCreateWaterPressed = createWater;
        controls.guCreateIcePressed = createIce;
        controls.guCreateEarthPressed = createEarth;
        controls.guShapeSpearPressed = shapeSpear;
        controls.guShapeWallPressed = shapeWall;
        controls.guCoolPressed = cool;
        controls.guHeatPressed = heat;
        controls.guMoveForwardPressed = moveForward;
        controls.guMoveForwardDown = moveForwardDown;
        controls.stepLeftPressed = stepLeft;
        controls.stepRightPressed = stepRight;
        controls.burstPressed = burst;
    }

    void copyFrom(BattleArenaTcpControlSnapshot other) {
        if (other == null) {
            return;
        }
        player = other.player;
        forward = other.forward;
        turn = other.turn;
        run = other.run;
        jump = other.jump;
        punch = other.punch;
        kick = other.kick;
        fireball = other.fireball;
        mudWall = other.mudWall;
        createWater = other.createWater;
        createIce = other.createIce;
        createEarth = other.createEarth;
        shapeSpear = other.shapeSpear;
        shapeWall = other.shapeWall;
        cool = other.cool;
        heat = other.heat;
        moveForward = other.moveForward;
        moveForwardDown = other.moveForwardDown;
        stepLeft = other.stepLeft;
        stepRight = other.stepRight;
        burst = other.burst;
    }

    void apply(String key, String value) {
        if ("player".equals(key)) {
            player = value;
        } else if ("forward".equals(key)) {
            forward = parseFloat(value);
        } else if ("turn".equals(key)) {
            turn = parseFloat(value);
        } else if ("run".equals(key)) {
            run = parseBoolean(value);
        } else if ("jump".equals(key)) {
            jump = parseBoolean(value);
        } else if ("punch".equals(key)) {
            punch = parseBoolean(value);
        } else if ("kick".equals(key)) {
            kick = parseBoolean(value);
        } else if ("fireball".equals(key)) {
            fireball = parseBoolean(value);
        } else if ("mud_wall".equals(key) || "mudWall".equals(key)) {
            mudWall = parseBoolean(value);
        } else if ("create_water".equals(key) || "createWater".equals(key)) {
            createWater = parseBoolean(value);
        } else if ("create_ice".equals(key) || "createIce".equals(key)) {
            createIce = parseBoolean(value);
        } else if ("create_earth".equals(key) || "createEarth".equals(key)) {
            createEarth = parseBoolean(value);
        } else if ("shape_spear".equals(key) || "shapeSpear".equals(key)) {
            shapeSpear = parseBoolean(value);
        } else if ("shape_wall".equals(key) || "shapeWall".equals(key)) {
            shapeWall = parseBoolean(value);
        } else if ("cool".equals(key)) {
            cool = parseBoolean(value);
        } else if ("heat".equals(key)) {
            heat = parseBoolean(value);
        } else if ("move_forward".equals(key) || "moveForward".equals(key)) {
            moveForward = parseBoolean(value);
        } else if ("move_forward_down".equals(key) || "moveForwardDown".equals(key)) {
            moveForwardDown = parseBoolean(value);
        } else if ("step_left".equals(key) || "stepLeft".equals(key)) {
            stepLeft = parseBoolean(value);
        } else if ("step_right".equals(key) || "stepRight".equals(key)) {
            stepRight = parseBoolean(value);
        } else if ("burst".equals(key)) {
            burst = parseBoolean(value);
        }
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static boolean parseBoolean(String value) {
        return "1".equals(value)
                || "true".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "down".equalsIgnoreCase(value);
    }

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
