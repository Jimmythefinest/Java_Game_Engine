package com.njst.gaming.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.njst.gaming.Renderer;

import java.util.List;
import java.util.Locale;

public class AndroidEngineView extends FrameLayout {
    private final AndroidEngineSurfaceView surfaceView;
    private final TextView statsOverlay;

    public AndroidEngineView(Context context, AndroidGameConfig gameConfig) {
        super(context);

        surfaceView = new AndroidEngineSurfaceView(context, gameConfig);
        addView(surfaceView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        statsOverlay = createStatsOverlay(context);
        addView(statsOverlay, createStatsLayoutParams(context));
        surfaceView.setStatsListener((fps, snapshot) -> statsOverlay.post(() -> statsOverlay.setText(formatStats(fps, snapshot))));

        addView(createControlOverlay(context, gameConfig));
    }

    public void onResume() {
        surfaceView.onResume();
    }

    public void onPause() {
        surfaceView.onPause();
        surfaceView.releaseAllInputs();
        surfaceView.cleanupAudio();
    }

    private View createControlOverlay(Context context, AndroidGameConfig gameConfig) {
        FrameLayout overlay = new FrameLayout(context);
        overlay.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        int buttonSize = dp(context, 72);
        int gap = dp(context, 12);
        int outerMargin = dp(context, 24);

        String virtualJoystickPointerId = gameConfig.getVirtualJoystickPointerId();
        if (virtualJoystickPointerId != null && !virtualJoystickPointerId.isEmpty()) {
            String sprintActionId = gameConfig.getVirtualJoystickSprintActionId();
            String stepLeftActionId = gameConfig.getVirtualJoystickStepLeftActionId();
            String stepRightActionId = gameConfig.getVirtualJoystickStepRightActionId();
            float sprintThreshold = gameConfig.getVirtualJoystickSprintThreshold();
            float sideStepThreshold = gameConfig.getVirtualJoystickSideStepThreshold();
            float sideStepCenterBand = gameConfig.getVirtualJoystickSideStepCenterBand();
            AndroidVirtualJoystickView joystickView = new AndroidVirtualJoystickView(context, new AndroidVirtualJoystickView.Listener() {
                @Override
                public void onJoystickStart() {
                    surfaceView.setVirtualPointerActive(virtualJoystickPointerId, true);
                }

                @Override
                public void onJoystickMove(float normalizedX, float normalizedY) {
                    surfaceView.moveVirtualPointer(virtualJoystickPointerId, normalizedX, normalizedY);
                    updateJoystickDerivedActions(
                            sprintActionId,
                            stepLeftActionId,
                            stepRightActionId,
                            normalizedX,
                            normalizedY,
                            sprintThreshold,
                            sideStepThreshold,
                            sideStepCenterBand);
                }

                @Override
                public void onJoystickEnd() {
                    surfaceView.moveVirtualPointer(virtualJoystickPointerId, 0f, 0f);
                    surfaceView.setVirtualPointerActive(virtualJoystickPointerId, false);
                    clearJoystickDerivedActions(sprintActionId, stepLeftActionId, stepRightActionId);
                }
            });
            int joystickSize = dp(context, 164);
            LayoutParams joystickParams = new LayoutParams(joystickSize, joystickSize);
            joystickParams.gravity = Gravity.START | Gravity.BOTTOM;
            joystickParams.leftMargin = outerMargin;
            joystickParams.bottomMargin = outerMargin;
            overlay.addView(joystickView, joystickParams);
        }

        List<AndroidActionButton> actionButtons = gameConfig.getActionButtons();
        for (int i = 0; i < actionButtons.size(); i++) {
            AndroidActionButton actionButton = actionButtons.get(i);
            Button button = createActionButton(context, actionButton.getLabel(), down -> surfaceView.setActionState(actionButton.getActionId(), down));
            LayoutParams params = new LayoutParams(buttonSize, buttonSize);
            params.gravity = Gravity.END | Gravity.BOTTOM;
            params.rightMargin = outerMargin + (i * (buttonSize + gap));
            params.bottomMargin = outerMargin;
            overlay.addView(button, params);
        }

        TextView lookHint = new TextView(context);
        lookHint.setText(gameConfig.getLookHintLabel());
        lookHint.setTextColor(Color.argb(180, 255, 255, 255));
        lookHint.setTextSize(12f);
        lookHint.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        lookHint.setPadding(gap, gap / 2, gap, gap / 2);
        lookHint.setBackgroundColor(Color.argb(80, 10, 14, 20));

        LayoutParams hintParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        hintParams.gravity = Gravity.END | Gravity.BOTTOM;
        hintParams.rightMargin = outerMargin;
        hintParams.bottomMargin = outerMargin + buttonSize + gap;
        overlay.addView(lookHint, hintParams);

        return overlay;
    }

    private TextView createStatsOverlay(Context context) {
        TextView label = new TextView(context);
        int padX = dp(context, 12);
        int padY = dp(context, 8);
        label.setText(formatStats(0, null));
        label.setTextColor(Color.WHITE);
        label.setTextSize(12f);
        label.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        label.setPadding(padX, padY, padX, padY);
        label.setBackgroundColor(Color.argb(140, 8, 12, 18));
        return label;
    }

    private LayoutParams createStatsLayoutParams(Context context) {
        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        int margin = dp(context, 20);
        params.gravity = Gravity.TOP | Gravity.END;
        params.topMargin = margin;
        params.rightMargin = margin;
        return params;
    }

    private String formatStats(int fps, Renderer.ProfilerSnapshot snapshot) {
        if (snapshot == null) {
            return "FPS 0\nframe 0.0ms\nupd 0.0 sky 0.0\nbone 0.0 rnd 0.0\nobj 0 t 0";
        }
        return String.format(Locale.US,
                "FPS %d\nframe %.1fms\nupd %.1f sky %.1f\nbone %.1f rnd %.1f\nobj %d t %d",
                fps,
                snapshot.frameMs,
                snapshot.updateMs,
                snapshot.skyboxMs,
                snapshot.boneMs,
                snapshot.renderMs,
                snapshot.objectCount,
                snapshot.terrainCount);
    }

    private Button createActionButton(Context context, String label, ActionSetter setter) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setAlpha(0.82f);
        button.setOnClickListener(view -> {
            setter.set(true);
            setter.set(false);
        });
        button.setOnTouchListener((view, event) -> handleActionTouch(view, event, setter));
        return button;
    }

    private boolean handleActionTouch(View view, MotionEvent event, ActionSetter setter) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                view.setPressed(true);
                setter.set(true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                view.setPressed(false);
                setter.set(false);
                view.performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                setter.set(false);
                return true;
            default:
                return true;
        }
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void updateJoystickDerivedActions(String sprintActionId,
                                              String stepLeftActionId,
                                              String stepRightActionId,
                                              float normalizedX,
                                              float normalizedY,
                                              float sprintThreshold,
                                              float sideStepThreshold,
                                              float sideStepCenterBand) {
        float radialDistance = (float) Math.sqrt((normalizedX * normalizedX) + (normalizedY * normalizedY));
        boolean sprintDown = radialDistance >= sprintThreshold && normalizedY <= -sideStepCenterBand;
        boolean stepLeftDown = Math.abs(normalizedY) <= sideStepCenterBand && normalizedX <= -sideStepThreshold;
        boolean stepRightDown = Math.abs(normalizedY) <= sideStepCenterBand && normalizedX >= sideStepThreshold;

        setOptionalActionState(sprintActionId, sprintDown);
        setOptionalActionState(stepLeftActionId, stepLeftDown);
        setOptionalActionState(stepRightActionId, stepRightDown);
    }

    private void clearJoystickDerivedActions(String sprintActionId,
                                             String stepLeftActionId,
                                             String stepRightActionId) {
        setOptionalActionState(sprintActionId, false);
        setOptionalActionState(stepLeftActionId, false);
        setOptionalActionState(stepRightActionId, false);
    }

    private void setOptionalActionState(String actionId, boolean down) {
        if (actionId == null || actionId.isEmpty()) {
            return;
        }
        surfaceView.setActionState(actionId, down);
    }

    private interface ActionSetter {
        void set(boolean down);
    }
}
