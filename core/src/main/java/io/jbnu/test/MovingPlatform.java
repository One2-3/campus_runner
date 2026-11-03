package io.jbnu.test;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** 두 지점 사이를 일정 속도로 왕복합니다. 위치는 경로상의 거리로 계산합니다. */
public final class MovingPlatform {
    private final float startX, startY, endX, endY, speed;
    private final double length;
    private final Rectangle bounds;
    private final Rectangle previousBounds;
    private double phase, previousPhase;
    private float deltaX, deltaY;

    public MovingPlatform(float startX, float startY, float endX, float endY,
                          float width, float height, float speed) {
        if (!Float.isFinite(startX) || !Float.isFinite(startY)
            || !Float.isFinite(endX) || !Float.isFinite(endY)
            || !Float.isFinite(width) || !Float.isFinite(height) || !Float.isFinite(speed)
            || width <= 0 || height <= 0 || speed <= 0
            || (startX == endX && startY == endY)) {
            throw new IllegalArgumentException("Invalid moving platform path, size or speed");
        }
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.speed = speed;
        length = Math.hypot((double) endX - startX, (double) endY - startY);
        bounds = new Rectangle(startX, startY, width, height);
        previousBounds = new Rectangle(bounds);
    }

    public void update(float dt) {
        previousBounds.set(bounds);
        previousPhase = phase;
        deltaX = deltaY = 0;
        if (dt <= 0 || !Float.isFinite(dt)) return;
        // 큰 dt로 끝점을 여러 번 지나도 남은 거리를 보존합니다.
        phase = (phase + (double) speed * dt) % (2 * length);
        double distance = phase <= length ? phase : 2 * length - phase;
        double ratio = distance / length;
        bounds.setPosition((float) (startX + ((double) endX - startX) * ratio),
            (float) (startY + ((double) endY - startY) * ratio));
        deltaX = bounds.x - previousBounds.x;
        deltaY = bounds.y - previousBounds.y;
    }

    /** 압착할 공간이 없으면 이번 이동을 취소하고 다음 프레임에 다시 시도합니다. */
    public void cancelMove() {
        phase = previousPhase;
        bounds.set(previousBounds);
        deltaX = deltaY = 0;
    }

    public void reset() {
        phase = previousPhase = 0;
        bounds.setPosition(startX, startY);
        previousBounds.set(bounds);
        deltaX = deltaY = 0;
    }

    public MovingPlatform copyInitial() {
        return new MovingPlatform(startX, startY, endX, endY, bounds.width, bounds.height, speed);
    }

    public Rectangle getBounds() { return new Rectangle(bounds); }
    public Rectangle getPreviousBounds() { return new Rectangle(previousBounds); }
    public float getDeltaX() { return deltaX; }
    public float getDeltaY() { return deltaY; }
    public boolean isMovingForward() { return phase < length; }

    public void render(ShapeRenderer shapes) {
        if (startY == endY) shapes.setColor(0.05f, 0.60f, 0.65f, 1f);
        else shapes.setColor(0.55f, 0.25f, 0.80f, 1f);
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
