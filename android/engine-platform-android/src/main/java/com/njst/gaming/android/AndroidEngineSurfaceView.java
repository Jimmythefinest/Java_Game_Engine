package com.njst.gaming.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.SparseArray;
import android.view.MotionEvent;

import java.util.List;

public class AndroidEngineSurfaceView extends GLSurfaceView {
    private static final class ActivePointer {
        private final String pointerId;
        private final String actionId;

        private ActivePointer(String pointerId, String actionId) {
            this.pointerId = pointerId;
            this.actionId = actionId;
        }
    }

    private final AndroidEngineRenderer renderer;
    private final List<AndroidPointerBinding> pointerBindings;
    private final SparseArray<ActivePointer> activePointers = new SparseArray<>();

    public AndroidEngineSurfaceView(Context context, AndroidGameConfig gameConfig) {
        super(context);
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new AndroidEngineRenderer(context.getApplicationContext(), gameConfig);
        pointerBindings = gameConfig.getPointerBindings();
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
                cancelPointers();
                break;
            default:
                break;
        }
        return true;
    }

    public void setStatsListener(AndroidEngineRenderer.StatsListener listener) {
        renderer.setStatsListener(listener);
    }

    private void handlePointerDown(MotionEvent event, int pointerIndex) {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        AndroidPointerBinding binding = resolvePointerBinding(x, y);
        if (binding == null) {
            return;
        }
        int pointerId = event.getPointerId(pointerIndex);
        ActivePointer activePointer = new ActivePointer(binding.getPointerId(), binding.getActionId());
        activePointers.put(pointerId, activePointer);
        renderer.pointerMoved(activePointer.pointerId, x, y);
        renderer.setPointerActionState(activePointer.pointerId, activePointer.actionId, true);
    }

    private AndroidPointerBinding resolvePointerBinding(float x, float y) {
        for (AndroidPointerBinding binding : pointerBindings) {
            if (binding.matches(x, y, getWidth(), getHeight())) {
                return binding;
            }
        }
        return null;
    }

    private void handleMove(MotionEvent event) {
        for (int i = 0; i < activePointers.size(); i++) {
            int pointerId = activePointers.keyAt(i);
            int pointerIndex = event.findPointerIndex(pointerId);
            if (pointerIndex < 0) {
                continue;
            }
            ActivePointer activePointer = activePointers.valueAt(i);
            renderer.pointerMoved(activePointer.pointerId, event.getX(pointerIndex), event.getY(pointerIndex));
        }
    }

    private void handlePointerUp(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        ActivePointer activePointer = activePointers.get(pointerId);
        if (activePointer == null) {
            return;
        }
        renderer.setPointerActionState(activePointer.pointerId, activePointer.actionId, false);
        activePointers.remove(pointerId);
    }

    private void cancelPointers() {
        for (int i = 0; i < activePointers.size(); i++) {
            ActivePointer activePointer = activePointers.valueAt(i);
            renderer.setPointerActionState(activePointer.pointerId, activePointer.actionId, false);
        }
        activePointers.clear();
    }

    public void setActionState(String actionId, boolean down) {
        renderer.setActionState(actionId, down);
    }

    public void setVirtualPointerActive(String pointerId, boolean active) {
        renderer.setPointerActionState(pointerId, null, active);
    }

    public void moveVirtualPointer(String pointerId, float normalizedX, float normalizedY) {
        renderer.pointerMoved(pointerId, normalizedX, normalizedY);
    }

    public void releaseAllInputs() {
        cancelPointers();
        renderer.releaseAllActions();
    }
}
