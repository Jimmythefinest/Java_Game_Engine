package com.njst.gaming.android;

public final class AndroidPointerBinding {
    public enum Region {
        ANYWHERE,
        LEFT_HALF,
        RIGHT_HALF
    }

    private final String pointerId;
    private final String actionId;
    private final Region region;

    public AndroidPointerBinding(String pointerId, String actionId, Region region) {
        this.pointerId = pointerId;
        this.actionId = actionId;
        this.region = region;
    }

    public String getPointerId() {
        return pointerId;
    }

    public String getActionId() {
        return actionId;
    }

    public boolean matches(float x, float y, int width, int height) {
        switch (region) {
            case LEFT_HALF:
                return x < (width * 0.5f);
            case RIGHT_HALF:
                return x >= (width * 0.5f);
            case ANYWHERE:
            default:
                return true;
        }
    }
}
