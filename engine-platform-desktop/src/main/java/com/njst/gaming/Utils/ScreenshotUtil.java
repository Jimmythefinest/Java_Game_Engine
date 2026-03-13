package com.njst.gaming.Utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.njst.gaming.data;

/**
 * Utility to capture the current OpenGL framebuffer and save it as an image.
 */
public class ScreenshotUtil {

    /**
     * Captures the screen and saves it to a file with a timestamp in the root directory.
     * 
     * @param width The width of the viewport.
     * @param height The height of the viewport.
     */
    public static void takeScreenshot(int width, int height) {
        // 1. Read pixels from OpenGL
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // 2. Convert to BufferedImage and flip vertically
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (x + (width * y)) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                
                // OpenGL is bottom-left (0,0), BufferedImage is top-left (0,0)
                // So we write into the image at (height - y - 1)
                image.setRGB(x, height - y - 1, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // 3. Save to file
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String fileName = data.rootDirectory + "/screenshot_" + timestamp + ".png";
        File file = new File(fileName);

        try {
            ImageIO.write(image, "png", file);
            System.out.println("[ScreenshotUtil] Saved screenshot to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[ScreenshotUtil] Failed to save screenshot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
