package com.njst.gaming.graphics;

import com.njst.gaming.Renderer;
import com.njst.gaming.objects.GameObject;

public class NullImposterBaker implements ImposterBaker {
    @Override
    public ImposterBakeResult bake(Renderer renderer, GameObject object, int width, int height) {
        return null;
    }

    @Override
    public void releaseTexture(int textureId) {
        // no-op
    }
}
