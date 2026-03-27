package com.njst.gaming.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.njst.gaming.input.InputCodes;

public class AndroidEngineView extends FrameLayout {
    private final AndroidEngineSurfaceView surfaceView;
    private final TextView fpsCounter;

    public AndroidEngineView(Context context) {
        super(context);

        surfaceView = new AndroidEngineSurfaceView(context);
        addView(surfaceView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        fpsCounter = createFpsCounter(context);
        addView(fpsCounter, createFpsLayoutParams(context));
        surfaceView.setFpsListener(fps -> fpsCounter.post(() -> fpsCounter.setText("FPS " + fps)));

        addView(createMovementOverlay(context));
    }

    public void onResume() {
        surfaceView.onResume();
    }

    public void onPause() {
        surfaceView.onPause();
        surfaceView.releaseAllInputs();
    }

    private View createMovementOverlay(Context context) {
        FrameLayout overlay = new FrameLayout(context);
        overlay.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        int buttonSize = dp(context, 72);
        int gap = dp(context, 12);
        int outerMargin = dp(context, 24);

        Button forward = createMovementButton(context, "W", InputCodes.BUTTON_MOVE_FORWARD);
        Button backward = createMovementButton(context, "S", InputCodes.BUTTON_MOVE_BACKWARD);
        Button left = createMovementButton(context, "A", InputCodes.BUTTON_MOVE_LEFT);
        Button right = createMovementButton(context, "D", InputCodes.BUTTON_MOVE_RIGHT);

        LayoutParams forwardParams = new LayoutParams(buttonSize, buttonSize);
        forwardParams.gravity = Gravity.START | Gravity.BOTTOM;
        forwardParams.leftMargin = outerMargin + buttonSize + gap;
        forwardParams.bottomMargin = outerMargin + buttonSize + gap;
        overlay.addView(forward, forwardParams);

        LayoutParams leftParams = new LayoutParams(buttonSize, buttonSize);
        leftParams.gravity = Gravity.START | Gravity.BOTTOM;
        leftParams.leftMargin = outerMargin;
        leftParams.bottomMargin = outerMargin;
        overlay.addView(left, leftParams);

        LayoutParams backwardParams = new LayoutParams(buttonSize, buttonSize);
        backwardParams.gravity = Gravity.START | Gravity.BOTTOM;
        backwardParams.leftMargin = outerMargin + buttonSize + gap;
        backwardParams.bottomMargin = outerMargin;
        overlay.addView(backward, backwardParams);

        LayoutParams rightParams = new LayoutParams(buttonSize, buttonSize);
        rightParams.gravity = Gravity.START | Gravity.BOTTOM;
        rightParams.leftMargin = outerMargin + ((buttonSize + gap) * 2);
        rightParams.bottomMargin = outerMargin;
        overlay.addView(right, rightParams);

        LinearLayout hintStrip = new LinearLayout(context);
        hintStrip.setOrientation(LinearLayout.VERTICAL);
        hintStrip.setGravity(Gravity.END);
        hintStrip.setPadding(gap, gap / 2, gap, gap / 2);
        hintStrip.setBackgroundColor(Color.argb(80, 10, 14, 20));

        Button lookHint = new Button(context);
        lookHint.setText("LOOK");
        lookHint.setEnabled(false);
        lookHint.setAllCaps(false);
        lookHint.setAlpha(0.65f);
        hintStrip.addView(lookHint, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        LayoutParams hintParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        hintParams.gravity = Gravity.END | Gravity.BOTTOM;
        hintParams.rightMargin = outerMargin;
        hintParams.bottomMargin = outerMargin;
        overlay.addView(hintStrip, hintParams);

        return overlay;
    }

    private TextView createFpsCounter(Context context) {
        TextView label = new TextView(context);
        int padX = dp(context, 12);
        int padY = dp(context, 8);
        label.setText("FPS 0");
        label.setTextColor(Color.WHITE);
        label.setTextSize(14f);
        label.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        label.setPadding(padX, padY, padX, padY);
        label.setBackgroundColor(Color.argb(120, 8, 12, 18));
        return label;
    }

    private LayoutParams createFpsLayoutParams(Context context) {
        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        int margin = dp(context, 20);
        params.gravity = Gravity.TOP | Gravity.END;
        params.topMargin = margin;
        params.rightMargin = margin;
        return params;
    }

    private Button createMovementButton(Context context, String label, int buttonCode) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setAlpha(0.82f);
        button.setOnTouchListener((view, event) -> handleMovementTouch(buttonCode, event));
        return button;
    }

    private boolean handleMovementTouch(int buttonCode, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                surfaceView.setButtonState(buttonCode, true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                surfaceView.setButtonState(buttonCode, false);
                return true;
            default:
                return true;
        }
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
