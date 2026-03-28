package com.njst.gaming;

import com.njst.gaming.android.AndroidActionButton;
import com.njst.gaming.android.AndroidGameConfig;
import com.njst.gaming.android.AndroidPointerBinding;
import com.njst.gaming.ri.battlearena.BattleArenaActions;
import com.njst.gaming.ri.battlearena.BattleArenaDemoLoader;

import java.util.Collections;
import java.util.List;

public class BattleArenaAndroidConfig implements AndroidGameConfig {
    @Override
    public void configureScene(Scene scene) {
        scene.loader = new BattleArenaDemoLoader();
    }

    @Override
    public List<AndroidActionButton> getActionButtons() {
        return Collections.emptyList();
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
}
