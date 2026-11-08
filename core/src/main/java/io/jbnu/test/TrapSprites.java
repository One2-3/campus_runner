package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Render-only panels, lights and pink spike material from the nine supplied PNGs. */
public final class TrapSprites implements Disposable {
    public static final float SPIKE_HEIGHT_SCALE = SpikeLayout.HEIGHT_SCALE;
    private final Texture[] textures = new Texture[9];
    private final float[] vertices = new float[20];
    private static final int[] BODY_TOP = {92, 94, 98};
    private static final int[] BODY_INSET = {42, 32, 22};
    private static final int[] SPIKE_CENTER = {192, 194, 286};
    private static final int[] WARNING_BODY_TOP = {62, 65, 68};

    public TrapSprites() {
        String loading = "";
        try {
            String[] states = {"safe", "warning", "active"};
            for (int state = 0; state < 3; state++) {
                for (int tile = 0; tile < 3; tile++) {
                    loading = "pixel/traps/" + states[state] + "_" + (tile + 1) + "tile.png";
                    Texture texture = new Texture(Gdx.files.internal(loading));
                    textures[state * 3 + tile] = texture;
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                }
            }
        } catch (RuntimeException error) {
            dispose();
            throw new GdxRuntimeException("Failed to load trap sprite: " + loading, error);
        }
    }

    static int tileIndex(float width) { return width <= 64 ? 0 : width <= 112 ? 1 : 2; }

    public void render(SpriteBatch batch, TimedTrap trap) {
        Rectangle b = trap.getBounds();
        int tile = tileIndex(b.width);
        TrapState state = trap.getState();
        Texture texture = getFrame(state, tile);
        float baseHeight = SpikeLayout.baseHeight(b.height);
        if (state == TrapState.ACTIVE) {
            if (trap.usesSpikeDesign()) {
                SpikeLayout layout = trap.getSpikeLayout();
                for (int i = 0; i < SpikeLayout.COUNT; i++) {
                    float width = layout.getCellWidth();
                    if (trap.isFixed()) {
                        // Preserve the old material visible through the translucent
                        // fixed base, but clip it so the old tall spike cannot show.
                        spike(batch, texture, tile, b.x + i * width, b.y, width, b.height, baseHeight);
                    }
                    // Rendering and damage read the exact same immutable triangles.
                    spike(batch, texture, tile, layout.getLeftX(i), layout.getBottomY(),
                        layout.getWidth(), layout.getHeight());
                }
            } else {
                // Legacy test traps are dangerous over their entire rectangle.
                batch.draw(texture, b.x, b.y + baseHeight, b.width, b.height - baseHeight,
                    (SPIKE_CENTER[tile] - 4f) / texture.getWidth(), (BODY_TOP[tile] - 4f) / texture.getHeight(),
                    (SPIKE_CENTER[tile] + 4f) / texture.getWidth(), (BODY_TOP[tile] - 12f) / texture.getHeight());
            }
        }
        // One source, crop, scale and cap layout for every timed state. Merely
        // using equal destination rectangles still enlarged ACTIVE's smaller crop.
        TrapState baseState = trap.isFixed() ? TrapState.ACTIVE : TrapState.SAFE;
        base(batch, getFrame(baseState, tile), baseState, tile, b, baseHeight);
        if (state == TrapState.WARNING) {
            warningPanel(batch, texture, tile, b, baseHeight);
            // These source strips contain yellow lamps/glow only, never raised spikes.
            int sourceWidth = 96, sourceHeight = 60;
            float scale = Math.min(.28f, b.width / (2 * sourceWidth));
            float width = sourceWidth * scale, height = sourceHeight * scale;
            batch.draw(texture, b.x, b.y + baseHeight, width, height,
                0, 0, sourceWidth, sourceHeight, false, false);
            batch.draw(texture, b.x + b.width - width, b.y + baseHeight, width, height,
                texture.getWidth() - sourceWidth, 0, sourceWidth, sourceHeight, false, false);
        } else if (state == TrapState.ACTIVE && !trap.isFixed()) {
            // Red warning lamps are decorations above the base, not a larger body.
            float scale = Math.min(.22f, b.width / 144f);
            float width = 72 * scale, height = 60 * scale;
            batch.draw(texture, b.x, b.y + baseHeight, width, height,
                0, 0, 72, 60, false, false);
            batch.draw(texture, b.x + b.width - width, b.y + baseHeight, width, height,
                texture.getWidth() - 72, 0, 72, 60, false, false);
        }
    }

    private void warningPanel(SpriteBatch batch, Texture texture, int tile, Rectangle b, float baseHeight) {
        // Tile a yellow stripe inside the common panel, never replace its silhouette.
        float height = Math.min(3f, baseHeight / 4);
        float scale = height / 12f, tileWidth = 24 * scale;
        float margin = Math.min(baseHeight, b.width / 4);
        float length = b.width - 2 * margin;
        float u = 70f / texture.getWidth();
        float v = (WARNING_BODY_TOP[tile] + 10f) / texture.getHeight();
        for (float across = 0; across < length; across += tileWidth) {
            float width = Math.min(tileWidth, length - across);
            batch.draw(texture, b.x + margin + across, b.y + baseHeight - height - 1, width, height,
                u, v + 12f / texture.getHeight(), u + width / scale / texture.getWidth(), v);
        }
    }

    private void base(SpriteBatch batch, Texture texture, TrapState state, int tile, Rectangle b, float height) {
        int sourceX = state == TrapState.ACTIVE ? BODY_INSET[tile] : 2;
        int sourceY = state == TrapState.ACTIVE ? BODY_TOP[tile]
            : state == TrapState.WARNING ? WARNING_BODY_TOP[tile] : 2;
        int sourceWidth = texture.getWidth() - 2 * sourceX;
        int sourceHeight = texture.getHeight() - 3 - sourceY;
        float scale = height / sourceHeight;
        int cap = Math.min(80, Math.min(sourceWidth / 3, (int) (b.width / (2 * scale))));
        float capWidth = cap * scale;
        batch.draw(texture, b.x, b.y, capWidth, height,
            sourceX, sourceY, cap, sourceHeight, false, false);
        int middle = sourceWidth - 2 * cap;
        float remaining = b.width - 2 * capWidth, tileWidth = middle * scale;
        for (float across = 0; across < remaining; across += tileWidth) {
            float width = Math.min(tileWidth, remaining - across);
            float u = (float) (sourceX + cap) / texture.getWidth();
            batch.draw(texture, b.x + capWidth + across, b.y, width, height,
                u, (float) (sourceY + sourceHeight) / texture.getHeight(),
                u + width / scale / texture.getWidth(), (float) sourceY / texture.getHeight());
        }
        batch.draw(texture, b.x + b.width - capWidth, b.y, capWidth, height,
            sourceX + sourceWidth - cap, sourceY, cap, sourceHeight, false, false);
    }

    private void spike(SpriteBatch batch, Texture texture, int tile, float x, float y, float width, float height) {
        spike(batch, texture, tile, x, y, width, height, height);
    }

    private void spike(SpriteBatch batch, Texture texture, int tile, float x, float y, float width, float height,
                       float visibleHeight) {
        float clipTop = y + visibleHeight;
        // Use an actual dark rim texel from the PNG for a readable silhouette on white.
        float rimU = (SPIKE_CENTER[tile] + .5f) / texture.getWidth();
        float rimV = 20.5f / texture.getHeight();
        float color = batch.getPackedColor();
        triangle(batch, texture, x, y, width, height, clipTop, color, rimU, rimU, rimU, rimV, rimV);
        float inset = Math.min(1.25f, width / 6f);
        x += inset; y += 1; width -= 2 * inset; height -= 3;
        float top = 14, scale = height / (BODY_TOP[tile] - top);
        float u = (float) SPIKE_CENTER[tile] / texture.getWidth();
        float halfU = width / (2 * scale * texture.getWidth());
        float bottomV = (float) BODY_TOP[tile] / texture.getHeight(), topV = top / texture.getHeight();
        triangle(batch, texture, x, y, width, height, clipTop, color, u - halfU, u, u + halfU, bottomV, topV);
    }

    private void triangle(SpriteBatch batch, Texture texture, float x, float y, float width, float height,
                          float clipTop, float color, float leftU, float topU, float rightU, float bottomV, float topV) {
        if (clipTop <= y) return;
        if (clipTop >= y + height) {
            vertex(0, x, y, color, leftU, bottomV);
            vertex(5, x + width / 2, y + height, color, topU, topV);
            vertex(10, x + width, y, color, rightU, bottomV);
        } else {
            // Interpolate the original triangle's UVs at the cut. This retains
            // its below-base appearance without stretching or editing the PNG.
            float fraction = (clipTop - y) / height;
            float inset = width / 2 * fraction;
            float v = bottomV + (topV - bottomV) * fraction;
            vertex(0, x, y, color, leftU, bottomV);
            vertex(5, x + inset, clipTop, color, leftU + (topU - leftU) * fraction, v);
            vertex(10, x + width - inset, clipTop, color, rightU + (topU - rightU) * fraction, v);
        }
        vertex(15, x + width, y, color, rightU, bottomV);
        batch.draw(texture, vertices, 0, vertices.length);
    }

    private void vertex(int offset, float x, float y, float color, float u, float v) {
        vertices[offset] = x; vertices[offset + 1] = y; vertices[offset + 2] = color;
        vertices[offset + 3] = u; vertices[offset + 4] = v;
    }

    Texture getFrame(TrapState state, int tile) { return textures[state.ordinal() * 3 + tile]; }

    @Override public void dispose() {
        for (int i = 0; i < textures.length; i++) if (textures[i] != null) {
            textures[i].dispose(); textures[i] = null;
        }
    }
}
