package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Shared, render-only homework monster; patrol and contact remain in PatrollingEnemy. */
public final class EnemySprites implements Disposable {
    public static final String PATH = "pixel/enemies/homework_enemy_128.png";
    public static final int SOURCE_X = 12, SOURCE_Y = 13, SOURCE_WIDTH = 110, SOURCE_HEIGHT = 106;
    public static final float HEIGHT_RATIO = 1.125f;
    private Texture texture;
    private final TextureRegion right, left;

    public EnemySprites() {
        try {
            texture = new Texture(Gdx.files.internal(PATH));
            if (texture.getWidth() != 128 || texture.getHeight() != 128)
                throw new GdxRuntimeException("Expected 128x128 canvas, got "
                    + texture.getWidth() + "x" + texture.getHeight());
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            right = new TextureRegion(texture, SOURCE_X, SOURCE_Y, SOURCE_WIDTH, SOURCE_HEIGHT);
            left = new TextureRegion(right);
            left.flip(true, false);
        } catch (RuntimeException error) {
            dispose();
            throw new GdxRuntimeException("Failed to load enemy sprite: " + PATH, error);
        }
    }

    public void render(SpriteBatch batch, PatrollingEnemy enemy) {
        Rectangle bounds = enemy.getBounds();
        float height = bounds.height * HEIGHT_RATIO;
        float width = height * SOURCE_WIDTH / SOURCE_HEIGHT;
        // Both cached directions share one crop, center and foot baseline.
        batch.draw(getFrame(enemy.isMovingRight()), bounds.x + bounds.width / 2f - width / 2f,
            bounds.y, width, height);
    }

    TextureRegion getFrame(boolean movingRight) { return movingRight ? right : left; }

    @Override public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}
