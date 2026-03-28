package com.njst.gaming.android;

public final class AndroidActionButton {
    private final String label;
    private final String actionId;

    public AndroidActionButton(String label, String actionId) {
        this.label = label;
        this.actionId = actionId;
    }

    public String getLabel() {
        return label;
    }

    public String getActionId() {
        return actionId;
    }
}
