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
}
