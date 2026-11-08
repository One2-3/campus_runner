package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 플레이어의 입력, 이동, 충돌을 관리합니다. */
public class Player {
    private static final float MOVE_SPEED = 260;
    private static final float JUMP_SPEED = 650;
    private static final float GRAVITY = 1700;
    private final PlayerModifier modifier;

    private final Rectangle bounds = new Rectangle(0, 0, 32, 44);
    private float velocityX, velocityY;
    private boolean grounded;
    private boolean facingRight = true;
    private MovingPlatform supportingPlatform;
    private static final float CONTACT_EPSILON = 0.01f;

    public Player(float startX, float startY) {
        this(startX, startY, PlayerModifier.NONE);
    }

    public Player(float startX, float startY, PlayerModifier modifier) {
        if (modifier == null) throw new IllegalArgumentException("Player modifier is required");
        this.modifier = modifier;
        reset(startX, startY);
    }

    public PlayerModifier getModifier() { return modifier; }

    public void reset(float startX, float startY) {
        bounds.setPosition(startX, startY);
        velocityX = 0;
        velocityY = 0;
        grounded = false;
        facingRight = true;
        supportingPlatform = null;
    }

    public void update(float dt, Array<Rectangle> blocks, float worldWidth) {
        update(dt, blocks, new Array<MovingPlatform>(), worldWidth);
    }

    public void update(float dt, Array<Rectangle> blocks,
                       Array<MovingPlatform> platforms, float worldWidth) {
        if (dt <= 0) return;

        velocityX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) velocityX -= MOVE_SPEED * modifier.getMoveSpeedMultiplier();
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) velocityX += MOVE_SPEED * modifier.getMoveSpeedMultiplier();
        if (velocityX > 0) facingRight = true;
        else if (velocityX < 0) facingRight = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && grounded) {
            velocityY = JUMP_SPEED * modifier.getJumpPowerMultiplier();
            grounded = false;
            supportingPlatform = null;
        }

        resolvePlatformMotion(blocks, platforms, worldWidth);
        velocityY = Math.max(velocityY - GRAVITY * dt, -900);
        moveX(dt, blocks, platforms, worldWidth);
        moveY(dt, blocks, platforms);
    }

    private void moveX(float dt, Array<Rectangle> blocks,
                       Array<MovingPlatform> platforms, float worldWidth) {
        bounds.x += velocityX * dt;
        for (Rectangle block : blocks) {
            if (!bounds.overlaps(block)) continue;
            if (velocityX > 0) bounds.x = block.x - bounds.width;
            else if (velocityX < 0) bounds.x = block.x + block.width;
        }
        for (MovingPlatform platform : platforms) {
            Rectangle block = platform.getBounds();
            if (!bounds.overlaps(block)) continue;
            if (velocityX > 0) bounds.x = block.x - bounds.width;
            else if (velocityX < 0) bounds.x = block.x + block.width;
        }
        bounds.x = MathUtils.clamp(bounds.x, 0, worldWidth - bounds.width);
    }

    private void moveY(float dt, Array<Rectangle> blocks, Array<MovingPlatform> platforms) {
        float dy = velocityY * dt;
        bounds.y += dy;
        grounded = false;
        supportingPlatform = null;

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
        for (MovingPlatform platform : platforms) {
            Rectangle block = platform.getBounds();
            if (!bounds.overlaps(block)) continue;
            if (dy < 0) {
                bounds.y = block.y + block.height;
                velocityY = 0;
                grounded = true;
                supportingPlatform = platform;
            } else if (dy > 0) {
                bounds.y = block.y - bounds.height;
                velocityY = 0;
            }
        }
    }

    private boolean overlapsX(Rectangle a, Rectangle b) {
        return a.x < b.x + b.width && a.x + a.width > b.x;
    }

    /** 이동 전 접촉 면으로 탑승/밀림을 결정합니다. 압착 시 발판 이동을 취소합니다. */
    private void resolvePlatformMotion(Array<Rectangle> blocks,
                                       Array<MovingPlatform> platforms, float worldWidth) {
        for (int i = 0; i < platforms.size; i++) {
            MovingPlatform platform = platforms.get(i);
            Rectangle before = platform.getPreviousBounds();
            Rectangle after = platform.getBounds();
            float dx = platform.getDeltaX(), dy = platform.getDeltaY();
            float oldX = bounds.x, oldY = bounds.y;
            boolean riding = supportingPlatform == platform
                && Math.abs(bounds.y - before.y - before.height) <= CONTACT_EPSILON
                && overlapsX(bounds, before);
            if (riding) {
                bounds.x += dx;
                // 덧셈 오차로 윗면 아래에 놓여 X축에서 측면 충돌하는 것을 방지합니다.
                bounds.y = after.y + after.height;
            } else {
                // 발판이 정지한 플레이어를 향해 움직이는 경우도 상대 이동으로 검출합니다.
                if (dx > 0 && oldX >= before.x + before.width - CONTACT_EPSILON
                    && oldX < after.x + after.width
                    && bounds.y < after.y + after.height && bounds.y + bounds.height > after.y) {
                    bounds.x = after.x + after.width;
                } else if (dx < 0 && oldX + bounds.width <= before.x + CONTACT_EPSILON
                    && oldX + bounds.width > after.x
                    && bounds.y < after.y + after.height && bounds.y + bounds.height > after.y) {
                    bounds.x = after.x - bounds.width;
                }
                if (overlapsX(bounds, after)) {
                    if (dy > 0 && oldY >= before.y + before.height - CONTACT_EPSILON
                        && oldY < after.y + after.height) {
                        bounds.y = after.y + after.height;
                    } else if (dy < 0 && oldY + bounds.height <= before.y + CONTACT_EPSILON
                        && oldY + bounds.height > after.y) {
                        bounds.y = after.y - bounds.height;
                        if (velocityY > 0) velocityY = 0;
                    }
                }
            }
            if (!canDisplace(oldX, oldY, blocks, platforms, platform, worldWidth)) {
                bounds.setPosition(oldX, oldY);
                platform.cancelMove();
            }
        }
    }

    private boolean canDisplace(float oldX, float oldY, Array<Rectangle> blocks,
                                Array<MovingPlatform> platforms, MovingPlatform source,
                                float worldWidth) {
        if (bounds.x == oldX && bounds.y == oldY) return true;
        if (bounds.x < 0 || bounds.x + bounds.width > worldWidth) return false;
        // 축 방향 이동 구간 전체를 검사해 얇은 벽/천장도 통과하지 않습니다.
        Rectangle swept = new Rectangle(Math.min(oldX, bounds.x), Math.min(oldY, bounds.y),
            bounds.width + Math.abs(bounds.x - oldX), bounds.height + Math.abs(bounds.y - oldY));
        for (Rectangle block : blocks) if (swept.overlaps(block)) return false;
        for (int i = 0; i < platforms.size; i++) {
            MovingPlatform other = platforms.get(i);
            if (other != source && swept.overlaps(other.getBounds())) return false;
        }
        return true;
    }

    public boolean isGrounded() { return grounded; }
    public boolean isMovingHorizontally() { return velocityX != 0; }
    public boolean isFacingRight() { return facingRight; }

    public float getCenterX() {
        return bounds.x + bounds.width / 2f;
    }

    public float getY() {
        return bounds.y;
    }

    public boolean overlaps(Rectangle rectangle) {
        return bounds.overlaps(rectangle);
    }

    /** Read-only collision geometry for triangular hazards. */
    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }
}
