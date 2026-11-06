package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Render-only grass/dirt patches at a uniform scale, cropped to the original solid rectangle. */
public final class FixedPlatformRenderer implements Disposable {
    public static final float SCALE = .25f;
    public static final float GRASS_HEIGHT = 24f;
    private final Texture[] textures = new Texture[4];
    private final Patch soil, grass, longGrass, left, right;

    private static final class Patch {
        final Texture texture;
        final int x, y, width, height;
        Patch(Texture texture, int x, int y, int width, int height) {
            if (x+width > texture.getWidth() || y+height > texture.getHeight())
                throw new GdxRuntimeException("Grass image too small for material patch");
            this.texture = texture; this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public FixedPlatformRenderer() {
        String[] names = {"grass_left.png","grass_center.png","grass_right.png","grass_long_reference.png"};
        String loading = "";
        try {
            for (int i = 0; i < names.length; i++) {
                loading = "pixel/platforms/"+names[i];
                textures[i] = new Texture(Gdx.files.internal(loading));
                textures[i].setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            }
            // Interior samples avoid transparent scalloped bottoms and differently sized outer silhouettes.
            soil = new Patch(textures[1],40,156,224,64);
            grass = new Patch(textures[1],16,20,280,96);
            longGrass = new Patch(textures[3],300,70,400,96);
            left = new Patch(textures[0],160,70,64,96);
            right = new Patch(textures[2],16,60,64,96);
        } catch (RuntimeException error) {
            dispose();
            throw new GdxRuntimeException("Failed to initialize fixed platform art: "+loading,error);
        }
    }

    public void render(SpriteBatch batch, Rectangle bounds) {
        float top = bounds.y+bounds.height;
        float grassHeight = Math.min(GRASS_HEIGHT,bounds.height);
        if (bounds.height > grassHeight) fill(batch,soil,bounds.x,bounds.y,bounds.width,bounds.height-grassHeight);
        // The visual walking surface is exactly y + height. All layers stay within the collision rectangle.
        fill(batch,bounds.width >= 480 ? longGrass : grass,bounds.x,top-grassHeight,bounds.width,grassHeight);
        float edgeWidth = Math.min(16,bounds.width/2);
        fill(batch,left,bounds.x,top-grassHeight,edgeWidth,grassHeight);
        fill(batch,right,bounds.x+bounds.width-edgeWidth,top-grassHeight,edgeWidth,grassHeight);
    }

    private void fill(SpriteBatch batch, Patch patch, float x, float y, float width, float height) {
        float tileWidth = patch.width*SCALE, tileHeight = patch.height*SCALE;
        for (float down = 0; down < height; down += tileHeight) {
            float drawHeight = Math.min(tileHeight,height-down);
            for (float across = 0; across < width; across += tileWidth) {
                float drawWidth = Math.min(tileWidth,width-across);
                // Crop the final piece instead of stretching a whole picture into a differently shaped block.
                batch.draw(patch.texture,x+across,y+height-down-drawHeight,drawWidth,drawHeight,
                    patch.x,patch.y,Math.round(drawWidth/SCALE),Math.round(drawHeight/SCALE),false,false);
            }
        }
    }

    @Override public void dispose() {
        for (int i = 0; i < textures.length; i++) if (textures[i] != null) { textures[i].dispose(); textures[i] = null; }
    }
}
