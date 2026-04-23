package com.njst.gaming.android;

import com.njst.gaming.Scene;

import java.util.Collections;
import java.util.List;

public interface AndroidGameConfig {
    void configureScene(Scene scene);

    List<AndroidActionButton> getActionButtons();

    default List<AndroidPointerBinding> getPointerBindings() {
        return Collections.emptyList();
    }

    default String getVirtualJoystickPointerId() {
        return null;
    }

    default String getLookHintLabel() {
        return "LOOK";
    }

    default String getExternalAssetRoot() {
        return null;
    }

    default boolean isShadowMapEnabled() {
        return false;
    }

    default String getVirtualJoystickSprintActionId() {
        return null;
    }

    default String getVirtualJoystickStepLeftActionId() {
        return null;
    }

    default String getVirtualJoystickStepRightActionId() {
        return null;
    }

    default float getVirtualJoystickSprintThreshold() {
        return 0.85f;
    }

    default float getVirtualJoystickSideStepThreshold() {
        return getVirtualJoystickSprintThreshold();
    }

    default float getVirtualJoystickSideStepCenterBand() {
        return 0.35f;
    }
}
