package com.njst.gaming.Natives;

import java.awt.image.BufferedImage;

import com.njst.gaming.Renderer;
import com.njst.gaming.Utils.GameObjectRenderUtil;
import com.njst.gaming.graphics.ImposterBakeResult;
import com.njst.gaming.graphics.ImposterBaker;
import com.njst.gaming.objects.GameObject;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class DesktopImposterBaker implements ImposterBaker {
    @Override
    public ImposterBakeResult bake(Renderer renderer, GameObject object, int width, int height) {
        BufferedImage image = GameObjectRenderUtil.renderToBitmap(renderer, object, width, height);
        if (image == null) {
            return null;
        }
        int textureId = GameObjectRenderUtil.uploadImageAsTexture(image);
        if (textureId == 0) {
            return null;
        }
        return new ImposterBakeResult(textureId, image.getWidth(), image.getHeight());
    }

    @Override
    public void releaseTexture(int textureId) {
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }
}
