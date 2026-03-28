package com.njst.gaming.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class AndroidVirtualJoystickView extends View {
    public interface Listener {
        void onJoystickStart();
        void onJoystickMove(float normalizedX, float normalizedY);
        void onJoystickEnd();
    }

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;
    private float centerX;
    private float centerY;
    private float knobX;
    private float knobY;
    private float radius;
    private boolean active;

    public AndroidVirtualJoystickView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        basePaint.setColor(Color.argb(110, 255, 255, 255));
        knobPaint.setColor(Color.argb(180, 255, 255, 255));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w * 0.5f;
        centerY = h * 0.5f;
        radius = Math.min(w, h) * 0.38f;
        resetKnob();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, radius, basePaint);
        canvas.drawCircle(knobX, knobY, radius * 0.42f, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                active = true;
                if (listener != null) {
                    listener.onJoystickStart();
                }
                updateKnob(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateKnob(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                active = false;
                resetKnob();
                if (listener != null) {
                    listener.onJoystickEnd();
                }
                invalidate();
                return true;
            default:
                return false;
        }
    }

    private void updateKnob(float touchX, float touchY) {
        float dx = touchX - centerX;
        float dy = touchY - centerY;
        float distance = (float) Math.sqrt((dx * dx) + (dy * dy));
        if (distance > radius && distance > 0f) {
            float scale = radius / distance;
            dx *= scale;
            dy *= scale;
        }
        knobX = centerX + dx;
        knobY = centerY + dy;
        if (listener != null) {
            listener.onJoystickMove(dx / radius, dy / radius);
        }
        invalidate();
    }

    private void resetKnob() {
        knobX = centerX;
        knobY = centerY;
        if (active && listener != null) {
            listener.onJoystickMove(0f, 0f);
        }
    }
}
