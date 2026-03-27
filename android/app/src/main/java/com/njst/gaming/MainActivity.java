package com.njst.gaming;

import android.app.Activity;
import android.os.Bundle;

import com.njst.gaming.android.AndroidEngineView;
import com.njst.gaming.android.AndroidPlatform;

public class MainActivity extends Activity {
    private AndroidEngineView engineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engineView = AndroidPlatform.createSurfaceView(this);
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
