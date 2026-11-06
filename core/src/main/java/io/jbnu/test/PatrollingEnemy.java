package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;

/** minX/maxX는 몸체 왼쪽 좌표의 경계입니다. 몸 전체가 지지면 위에 있도록 배치합니다. */
public final class PatrollingEnemy {
    private final Rectangle bounds;
    private final float initialX, initialY, minX, maxX, speed;
    private final boolean initialMovingRight;
    private final double length;
    private double phase;
    private boolean movingRight;

    public PatrollingEnemy(float startX, float y, float width, float height,
                           float minX, float maxX, float speed, boolean movingRight) {
        if (!Float.isFinite(startX) || !Float.isFinite(y)
            || !Float.isFinite(width) || !Float.isFinite(height)
            || !Float.isFinite(minX) || !Float.isFinite(maxX) || !Float.isFinite(speed)
            || width <= 0 || height <= 0 || speed <= 0 || minX >= maxX
            || startX < minX || startX > maxX) {
            throw new IllegalArgumentException("Invalid enemy bounds, patrol range or speed");
        }
        bounds = new Rectangle(startX, y, width, height);
        initialX = startX;
        initialY = y;
        this.minX = minX;
        this.maxX = maxX;
        this.speed = speed;
        initialMovingRight = movingRight;
        length = (double) maxX - minX;
        reset();
    }

    public void update(double dt) {
        if (dt <= 0 || !Double.isFinite(dt)) return;
        // 편도 거리의 두 배를 주기로 삼아 여러 번 왕복해도 남은 거리를 보존합니다.
        double period = 2 * length;
        double cycleSeconds = period / speed;
        phase = (phase + (dt % cycleSeconds) * speed) % period;
        double epsilon = length * 1e-10;
        if (phase < epsilon || period - phase < epsilon) phase = 0;
        else if (Math.abs(phase - length) < epsilon) phase = length;
        double distance = phase <= length ? phase : period - phase;
        bounds.x = Math.max(minX, Math.min(maxX, (float) (minX + distance)));
        movingRight = phase < length;
    }

    public void reset() {
        bounds.setPosition(initialX, initialY);
        movingRight = initialMovingRight;
        double distance = (double) initialX - minX;
        phase = initialMovingRight ? distance : 2 * length - distance;
    }

    public PatrollingEnemy copyInitial() {
        return new PatrollingEnemy(initialX, initialY, bounds.width, bounds.height,
            minX, maxX, speed, initialMovingRight);
    }

    public Rectangle getBounds() { return new Rectangle(bounds); }
    public boolean isMovingRight() { return movingRight; }

    public boolean touches(Player player) {
        return player.overlaps(bounds);
    }

}
