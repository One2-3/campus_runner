package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Shared wood artwork only; the platform owns all movement and collision data. */
public final class MovingPlatformRenderer implements Disposable {
    private final Texture[] textures = new Texture[3];

    public MovingPlatformRenderer() {
        String[] names = {"moving_wood_short.png", "moving_wood_long.png", "moving_wood_hanging.png"};
        String loading = "";
        try {
            for (int i = 0; i < names.length; i++) {
                loading = "pixel/platforms/" + names[i];
                textures[i] = new Texture(Gdx.files.internal(loading));
                textures[i].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        } catch (RuntimeException error) {
            dispose();
            throw new GdxRuntimeException("Failed to initialize moving platform art: " + loading, error);
        }
    }

    public void render(SpriteBatch batch, MovingPlatform platform) {
        Rectangle bounds = platform.getBounds();
        int index = !platform.isHorizontal() ? 2 : bounds.width >= 150 ? 1 : 0;
        Texture texture = textures[index];
        // Source rows measured from the PNG top: the broad wooden walking surface,
        // not the top of the decorative chains or the rounded corner silhouette.
        float surfaceRow = index == 2 ? 160 : 8;
        float heightFactor = index == 0 ? .75f : index == 2 ? .70f : 1f;
        float scale = bounds.width / texture.getWidth() * heightFactor;
        float height = texture.getHeight() * scale;
        float y = bounds.y + bounds.height - (texture.getHeight() - surfaceRow) * scale;
        if (index == 1) {
            batch.draw(texture, bounds.x, y, bounds.width, height);
            return;
        }
        // Uniformly shrink the end ornaments/chains and repeat only the bare middle
        // plank. This reduces height without narrowing the landing surface or
        // vertically squashing the PNG. No collision data is changed.
        int capPixels = index == 0 ? 160 : 150;
        int middlePixels = texture.getWidth() - 2 * capPixels;
        float capWidth = capPixels * scale;
        batch.draw(texture, bounds.x, y, capWidth, height,
            0, 0, capPixels, texture.getHeight(), false, false);
        float middleWidth = bounds.width - 2 * capWidth;
        float tileWidth = middlePixels * scale;
        for (float across = 0; across < middleWidth; across += tileWidth) {
            float width = Math.min(tileWidth, middleWidth - across);
            // Fractional UV cropping keeps the final tile at the same pixel scale.
            float u = (float) capPixels / texture.getWidth();
            batch.draw(texture, bounds.x + capWidth + across, y, width, height,
                u, 1f, u + width / scale / texture.getWidth(), 0f);
        }
        batch.draw(texture, bounds.x + bounds.width - capWidth, y, capWidth, height,
            texture.getWidth() - capPixels, 0, capPixels, texture.getHeight(), false, false);
    }

    @Override public void dispose() {
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] != null) {
                textures[i].dispose();
                textures[i] = null;
            }
        }
    }
}
