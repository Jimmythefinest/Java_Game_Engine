package com.njst.gaming;

import android.app.Activity;
import android.os.Bundle;

import com.njst.gaming.android.AndroidEngineSurfaceView;

public class MainActivity extends Activity {
    private AndroidEngineSurfaceView engineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engineView = new AndroidEngineSurfaceView(this);
        setContentView(engineView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        engineView.onResume();
    }

    @Override
    protected void onPause() {
        engineView.onPause();
        super.onPause();
    }
}
