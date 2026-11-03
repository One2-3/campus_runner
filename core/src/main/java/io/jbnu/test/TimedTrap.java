package io.jbnu.test;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** 고체 충돌에는 참여하지 않고 ACTIVE일 때만 위험한 주기형 함정입니다. */
public final class TimedTrap {
    private final Rectangle bounds;
    private final double safeDuration, warningDuration, activeDuration, cycleDuration;
    private final double initialOffset;
    private double phase;
    private TrapState state;
    private double stateElapsedTime;

    public TimedTrap(float x, float y, float width, float height, double initialOffset) {
        this(x, y, width, height, 2.0, 0.8, 1.5, initialOffset);
    }

    public TimedTrap(float x, float y, float width, float height,
                     double safeDuration, double warningDuration, double activeDuration,
                     double initialOffset) {
        double cycle = safeDuration + warningDuration + activeDuration;
        if (!Float.isFinite(x) || !Float.isFinite(y)
            || !Float.isFinite(width) || !Float.isFinite(height) || width <= 0 || height <= 0
            || !Double.isFinite(safeDuration) || safeDuration <= 0
            || !Double.isFinite(warningDuration) || warningDuration <= 0
            || !Double.isFinite(activeDuration) || activeDuration <= 0
            || !Double.isFinite(cycle) || !Double.isFinite(initialOffset) || initialOffset < 0) {
            throw new IllegalArgumentException("Invalid trap bounds, durations or offset");
        }
        bounds = new Rectangle(x, y, width, height);
        this.safeDuration = safeDuration;
        this.warningDuration = warningDuration;
        this.activeDuration = activeDuration;
        cycleDuration = cycle;
        this.initialOffset = initialOffset % cycleDuration;
        reset();
    }

    public void update(double dt) {
        if (dt <= 0 || !Double.isFinite(dt)) return;
        // 먼저 dt를 줄여 큰 시간에도 덧셈 오버플로와 반복 루프를 피합니다.
        phase = (phase + dt % cycleDuration) % cycleDuration;
        refreshState();
    }

    public void reset() {
        phase = initialOffset;
        refreshState();
    }

    private void refreshState() {
        // 주기 경계에서 부동소수점 누적으로 이전 상태에 남는 것을 방지합니다.
        double epsilon = Math.min(safeDuration, Math.min(warningDuration, activeDuration)) * 1e-9;
        if (phase < epsilon || cycleDuration - phase < epsilon) phase = 0;
        else if (Math.abs(phase - safeDuration) < epsilon) phase = safeDuration;
        else if (Math.abs(phase - safeDuration - warningDuration) < epsilon)
            phase = safeDuration + warningDuration;

        if (phase < safeDuration) {
            state = TrapState.SAFE;
            stateElapsedTime = phase;
        } else if (phase < safeDuration + warningDuration) {
            state = TrapState.WARNING;
            stateElapsedTime = phase - safeDuration;
        } else {
            state = TrapState.ACTIVE;
            stateElapsedTime = phase - safeDuration - warningDuration;
        }
    }

    public TimedTrap copyInitial() {
        return new TimedTrap(bounds.x, bounds.y, bounds.width, bounds.height,
            safeDuration, warningDuration, activeDuration, initialOffset);
    }

    public TrapState getState() { return state; }
    public double getStateElapsedTime() { return stateElapsedTime; }
    public Rectangle getBounds() { return new Rectangle(bounds); }

    public boolean isDangerousTo(Player player) {
        return state == TrapState.ACTIVE && player.overlaps(bounds);
    }

    public void render(ShapeRenderer shapes) {
        if (state == TrapState.SAFE) {
            shapes.setColor(0.45f, 0.45f, 0.45f, 1f);
            shapes.rect(bounds.x, bounds.y, bounds.width, Math.min(5f, bounds.height));
            return;
        }
        if (state == TrapState.WARNING) {
            shapes.setColor(1f, 0.75f, 0.05f, 1f);
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            // 경고 느낌표. 노란 상태는 아직 안전합니다.
            shapes.setColor(0.35f, 0.20f, 0f, 1f);
            shapes.rect(bounds.x + bounds.width * 0.46f, bounds.y + bounds.height * 0.4f,
                bounds.width * 0.08f, bounds.height * 0.45f);
            shapes.rect(bounds.x + bounds.width * 0.46f, bounds.y + bounds.height * 0.15f,
                bounds.width * 0.08f, bounds.height * 0.12f);
            return;
        }
        // 위험 사각형 전체를 채워 삼각형 사이 빈 공간에서도 판정이 명확합니다.
        shapes.setColor(0.9f, 0.12f, 0.12f, 1f);
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapes.setColor(0.55f, 0.02f, 0.02f, 1f);
        float spikeWidth = bounds.width / 4f;
        for (int i = 0; i < 4; i++) {
            float left = bounds.x + i * spikeWidth;
            shapes.triangle(left, bounds.y, left + spikeWidth, bounds.y,
                left + spikeWidth / 2f, bounds.y + bounds.height);
        }
    }
}
