package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 플레이어의 입력, 이동, 충돌을 관리합니다. */
public class Player {
    private static final float MOVE_SPEED = 260;
    private static final float JUMP_SPEED = 650;
    private static final float GRAVITY = 1700;

    private final Rectangle bounds = new Rectangle(0, 0, 32, 44);
    private float velocityX, velocityY;
    private boolean grounded;

    public Player(float startX, float startY) {
        reset(startX, startY);
    }

    public void reset(float startX, float startY) {
        bounds.setPosition(startX, startY);
        velocityX = 0;
        velocityY = 0;
        grounded = false;
    }

    public void update(float dt, Array<Rectangle> blocks, float worldWidth) {
        if (dt <= 0) return;

        velocityX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) velocityX -= MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) velocityX += MOVE_SPEED;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && grounded) {
            velocityY = JUMP_SPEED;
            grounded = false;
        }

        velocityY = Math.max(velocityY - GRAVITY * dt, -900);
        moveX(dt, blocks, worldWidth);
        moveY(dt, blocks);
    }

    private void moveX(float dt, Array<Rectangle> blocks, float worldWidth) {
        bounds.x += velocityX * dt;
        for (Rectangle block : blocks) {
            if (!bounds.overlaps(block)) continue;
            if (velocityX > 0) bounds.x = block.x - bounds.width;
            else if (velocityX < 0) bounds.x = block.x + block.width;
        }
        bounds.x = MathUtils.clamp(bounds.x, 0, worldWidth - bounds.width);
    }

    private void moveY(float dt, Array<Rectangle> blocks) {
        float dy = velocityY * dt;
        bounds.y += dy;
        grounded = false;

        for (Rectangle block : blocks) {
            if (!bounds.overlaps(block)) continue;
            if (dy < 0) {
                bounds.y = block.y + block.height;
                velocityY = 0;
                grounded = true;
            } else if (dy > 0) {
                bounds.y = block.y - bounds.height;
                velocityY = 0;
            }
        }
    }

    public void render(SpriteBatch batch, Texture texture) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public float getCenterX() {
        return bounds.x + bounds.width / 2f;
    }

    public float getY() {
        return bounds.y;
    }

    public boolean overlaps(Rectangle rectangle) {
        return bounds.overlaps(rectangle);
    }
}
