package com.njst.gaming.graphics;

import com.njst.gaming.Renderer;
import com.njst.gaming.objects.GameObject;

public interface ImposterBaker {
    ImposterBakeResult bake(Renderer renderer, GameObject object, int width, int height);

    void releaseTexture(int textureId);
}
