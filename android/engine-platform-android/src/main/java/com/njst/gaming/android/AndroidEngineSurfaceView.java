package com.njst.gaming.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.njst.gaming.input.InputCodes;

public class AndroidEngineSurfaceView extends GLSurfaceView {
    private final AndroidEngineRenderer renderer;
    private int activeLookPointerId = MotionEvent.INVALID_POINTER_ID;

    public AndroidEngineSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new AndroidEngineRenderer(context.getApplicationContext());
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handlePointerDown(event, actionIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                handleMove(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handlePointerUp(event, actionIndex);
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelLook();
                break;
            default:
                break;
        }
        return true;
    }

    private void handlePointerDown(MotionEvent event, int pointerIndex) {
        float x = event.getX(pointerIndex);
        if (activeLookPointerId != MotionEvent.INVALID_POINTER_ID || x < (getWidth() * 0.5f)) {
            return;
        }
        activeLookPointerId = event.getPointerId(pointerIndex);
        float y = event.getY(pointerIndex);
        renderer.cursorMoved(x, y);
        renderer.setLooking(true);
    }

    private void handleMove(MotionEvent event) {
        if (activeLookPointerId == MotionEvent.INVALID_POINTER_ID) {
            return;
        }
        int pointerIndex = event.findPointerIndex(activeLookPointerId);
        if (pointerIndex < 0) {
            cancelLook();
            return;
        }
        renderer.cursorMoved(event.getX(pointerIndex), event.getY(pointerIndex));
    }

    private void handlePointerUp(MotionEvent event, int pointerIndex) {
        if (event.getPointerId(pointerIndex) != activeLookPointerId) {
            return;
        }
        cancelLook();
    }

    private void cancelLook() {
        activeLookPointerId = MotionEvent.INVALID_POINTER_ID;
        renderer.setLooking(false);
    }

    public void setButtonState(int buttonCode, boolean down) {
        renderer.setButtonState(buttonCode, down);
    }

    public void releaseAllInputs() {
        cancelLook();
        setButtonState(InputCodes.BUTTON_MOVE_FORWARD, false);
        setButtonState(InputCodes.BUTTON_MOVE_BACKWARD, false);
        setButtonState(InputCodes.BUTTON_MOVE_LEFT, false);
        setButtonState(InputCodes.BUTTON_MOVE_RIGHT, false);
    }
}
