package com.njst.gaming;

import com.njst.gaming.android.AndroidActionButton;
import com.njst.gaming.android.AndroidGameConfig;
import com.njst.gaming.android.AndroidPointerBinding;
import com.njst.gaming.ri.battlearena.*;
import com.njst.gaming.ri.battlearena.controls.*;
import com.njst.gaming.ri.battlearena.networking.BattleArenaTcpControlClient;
import com.njst.gaming.ri.battlearena.networking.BattleArenaTcpSimulationClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BattleArenaAndroidConfig implements AndroidGameConfig {
    private static final String DEFAULT_SIMULATION_HOST = "172.168.64.106";

    @Override
    public void configureScene(Scene scene) {
        configureNetworkSimulationDefaults();
        scene.loader = new BattleArenaGpuSkinningDemoLoader();//BattleArenaDemoLoader(
                // BattleArenaDemoLoader.LOCAL_PLAYER_ANDROID,
                // System.getProperty("battleArena.remoteHost", BattleArenaTcpControlClient.DEFAULT_HOST),
                // BattleArenaDemoLoader.DEFAULT_TCP_CONTROL_PORT);
    }

    private void configureNetworkSimulationDefaults() {
        setDefaultProperty("battleArena.networkSimulation", "true");
        setDefaultProperty("battleArena.simulationHost", DEFAULT_SIMULATION_HOST);
        setDefaultProperty(
                "battleArena.simulationPort",
                String.valueOf(BattleArenaTcpSimulationClient.DEFAULT_PORT));
    }

    private void setDefaultProperty(String key, String value) {
        String existing = System.getProperty(key);
        if (existing == null || existing.trim().isEmpty()) {
            System.setProperty(key, value);
        }
    }

    @Override
    public List<AndroidActionButton> getActionButtons() {
        return Arrays.asList(
                new AndroidActionButton("Punch", BattleArenaActions.PUNCH),
                new AndroidActionButton("Kick", BattleArenaActions.KICK));
    }

    @Override
    public List<AndroidPointerBinding> getPointerBindings() {
        return Collections.singletonList(
                new AndroidPointerBinding(
                        BattleArenaActions.LOOK_POINTER,
                        BattleArenaActions.LOOK,
                        AndroidPointerBinding.Region.RIGHT_HALF));
    }

    @Override
    public String getVirtualJoystickPointerId() {
        return BattleArenaActions.MOVE_POINTER;
    }

    @Override
    public String getExternalAssetRoot() {
        return "/sdcard/Documents/battlearena";
    }

    @Override
    public String getVirtualJoystickSprintActionId() {
        return BattleArenaActions.RUN;
    }

    @Override
    public String getVirtualJoystickStepLeftActionId() {
        return BattleArenaActions.STEP_LEFT;
    }

    @Override
    public String getVirtualJoystickStepRightActionId() {
        return BattleArenaActions.STEP_RIGHT;
    }
}
