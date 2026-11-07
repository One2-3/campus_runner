package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;

/** Immutable world geometry shared by spike artwork and damage checks. No graphics dependency. */
public final class SpikeLayout {
    public static final int COUNT = 4;
    public static final float HEIGHT_SCALE = .55f;
    private final float x, cellWidth, inset, bottomY, width, height;

    public SpikeLayout(Rectangle bounds) {
        x = bounds.x;
        cellWidth = bounds.width / COUNT;
        float base = baseHeight(bounds.height);
        inset = cellWidth * base / (2 * bounds.height);
        bottomY = bounds.y + base;
        width = cellWidth - 2 * inset;
        height = (bounds.height - base) * HEIGHT_SCALE;
    }

    public static float baseHeight(float trapHeight) { return Math.min(16f, trapHeight * .5f); }
    public float getCellWidth() { return cellWidth; }
    public float getLeftX(int index) { return x + index * cellWidth + inset; }
    public float getCenterX(int index) { return getLeftX(index) + width / 2; }
    public float getBottomY() { return bottomY; }
    public float getTopY() { return bottomY + height; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    /** Exact positive-area overlap between an axis-aligned body and each upright triangle. */
    public boolean overlaps(Rectangle body) {
        if (body.width <= 0 || body.height <= 0 || body.y >= getTopY()
            || body.y + body.height <= bottomY) return false;
        // A triangle is widest at the lowest overlapping Y. If that cross-section
        // misses the body's X interval, every higher (narrower) section misses too.
        float lowest = Math.max(bottomY, body.y);
        float half = width * .5f * (1 - (lowest - bottomY) / height);
        for (int i = 0; i < COUNT; i++) {
            float center = getCenterX(i);
            if (body.x < center + half && body.x + body.width > center - half) return true;
        }
        return false;
    }
}
