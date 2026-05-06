package com.njst.gaming.ri.battlearena.networking;

import com.njst.gaming.ri.battlearena.BattleArenaCharacterRuntime;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterBrain;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterControlState;

public final class BattleArenaTcpRemoteController implements BattleArenaCharacterBrain {
    private final BattleArenaTcpControlClient controlClient;
    private final String remotePlayer;
    private boolean previousJump;
    private boolean previousPunch;
    private boolean previousKick;
    private boolean previousFireball;
    private boolean previousMudWall;
    private boolean previousCreateWater;
    private boolean previousCreateIce;
    private boolean previousCreateEarth;
    private boolean previousShapeSpear;
    private boolean previousShapeWall;
    private boolean previousCool;
    private boolean previousHeat;
    private boolean previousMoveForward;
    private boolean previousStepLeft;
    private boolean previousStepRight;
    private boolean previousBurst;

    public BattleArenaTcpRemoteController(BattleArenaTcpControlClient controlClient, String remotePlayer) {
        this.controlClient = controlClient;
        this.remotePlayer = remotePlayer;
    }

    @Override
    public void update(BattleArenaCharacterRuntime self,
                       BattleArenaCharacterRuntime opponent,
                       BattleArenaCharacterControlState controls,
                       float deltaSeconds) {
        controls.clear();
        if (controlClient != null) {
            controlClient.copyControls(remotePlayer, controls);
            applyButtonEdges(controls);
        }
    }

    private void applyButtonEdges(BattleArenaCharacterControlState controls) {
        boolean currentJump = controls.jumpPressed;
        boolean currentPunch = controls.punchPressed;
        boolean currentKick = controls.kickPressed;
        boolean currentFireball = controls.castFireballPressed;
        boolean currentMudWall = controls.castMudWallPressed;
        boolean currentCreateWater = controls.guCreateWaterPressed;
        boolean currentCreateIce = controls.guCreateIcePressed;
        boolean currentCreateEarth = controls.guCreateEarthPressed;
        boolean currentShapeSpear = controls.guShapeSpearPressed;
        boolean currentShapeWall = controls.guShapeWallPressed;
        boolean currentCool = controls.guCoolPressed;
        boolean currentHeat = controls.guHeatPressed;
        boolean currentMoveForward = controls.guMoveForwardPressed;
        boolean currentMoveForwardDown = controls.guMoveForwardDown;
        boolean currentStepLeft = controls.stepLeftPressed;
        boolean currentStepRight = controls.stepRightPressed;
        boolean currentBurst = controls.burstPressed;

        controls.jumpPressed = currentJump && !previousJump;
        controls.punchPressed = currentPunch && !previousPunch;
        controls.kickPressed = currentKick && !previousKick;
        controls.castFireballPressed = currentFireball && !previousFireball;
        controls.castMudWallPressed = currentMudWall && !previousMudWall;
        controls.guCreateWaterPressed = currentCreateWater && !previousCreateWater;
        controls.guCreateIcePressed = currentCreateIce && !previousCreateIce;
        controls.guCreateEarthPressed = currentCreateEarth && !previousCreateEarth;
        controls.guShapeSpearPressed = currentShapeSpear && !previousShapeSpear;
        controls.guShapeWallPressed = currentShapeWall && !previousShapeWall;
        controls.guCoolPressed = currentCool && !previousCool;
        controls.guHeatPressed = currentHeat && !previousHeat;
        controls.guMoveForwardPressed = currentMoveForward && !previousMoveForward;
        controls.guMoveForwardDown = currentMoveForwardDown;
        controls.stepLeftPressed = currentStepLeft && !previousStepLeft;
        controls.stepRightPressed = currentStepRight && !previousStepRight;
        controls.burstPressed = currentBurst && !previousBurst;

        previousJump = currentJump;
        previousPunch = currentPunch;
        previousKick = currentKick;
        previousFireball = currentFireball;
        previousMudWall = currentMudWall;
        previousCreateWater = currentCreateWater;
        previousCreateIce = currentCreateIce;
        previousCreateEarth = currentCreateEarth;
        previousShapeSpear = currentShapeSpear;
        previousShapeWall = currentShapeWall;
        previousCool = currentCool;
        previousHeat = currentHeat;
        previousMoveForward = currentMoveForward;
        previousStepLeft = currentStepLeft;
        previousStepRight = currentStepRight;
        previousBurst = currentBurst;
    }
}
