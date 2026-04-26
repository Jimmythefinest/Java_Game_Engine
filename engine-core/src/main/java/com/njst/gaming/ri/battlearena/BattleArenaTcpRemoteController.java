package com.njst.gaming.ri.battlearena;

final class BattleArenaTcpRemoteController implements BattleArenaCharacterBrain {
    private final BattleArenaTcpControlClient controlClient;
    private final String remotePlayer;
    private boolean previousJump;
    private boolean previousPunch;
    private boolean previousKick;
    private boolean previousFireball;
    private boolean previousMudWall;
    private boolean previousStepLeft;
    private boolean previousStepRight;
    private boolean previousBurst;

    BattleArenaTcpRemoteController(BattleArenaTcpControlClient controlClient, String remotePlayer) {
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
        boolean currentStepLeft = controls.stepLeftPressed;
        boolean currentStepRight = controls.stepRightPressed;
        boolean currentBurst = controls.burstPressed;

        controls.jumpPressed = currentJump && !previousJump;
        controls.punchPressed = currentPunch && !previousPunch;
        controls.kickPressed = currentKick && !previousKick;
        controls.castFireballPressed = currentFireball && !previousFireball;
        controls.castMudWallPressed = currentMudWall && !previousMudWall;
        controls.stepLeftPressed = currentStepLeft && !previousStepLeft;
        controls.stepRightPressed = currentStepRight && !previousStepRight;
        controls.burstPressed = currentBurst && !previousBurst;

        previousJump = currentJump;
        previousPunch = currentPunch;
        previousKick = currentKick;
        previousFireball = currentFireball;
        previousMudWall = currentMudWall;
        previousStepLeft = currentStepLeft;
        previousStepRight = currentStepRight;
        previousBurst = currentBurst;
    }
}
