package io.jbnu.test;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** 고체 충돌에는 참여하지 않으며 ACTIVE일 때 위험합니다. 고정 가시는 항상 ACTIVE입니다. */
public final class TimedTrap {
    private final Rectangle bounds;
    private final double safeDuration, warningDuration, activeDuration, cycleDuration;
    private final double initialOffset;
    private final boolean fixed, spikeDesign;
    private double phase;
    private TrapState state;
    private double stateElapsedTime;

    public TimedTrap(float x, float y, float width, float height, double initialOffset) {
        this(x, y, width, height, 2.0, 0.8, 1.5, initialOffset);
    }

    public TimedTrap(float x, float y, float width, float height,
                     double safeDuration, double warningDuration, double activeDuration,
                     double initialOffset) {
        this(x, y, width, height, safeDuration, warningDuration, activeDuration, initialOffset, false, false);
    }

    public static TimedTrap fixedSpikes(float x, float y, float width, float height) {
        return new TimedTrap(x, y, width, height, 2.0, 0.8, 1.5, 0, true, true);
    }

    public static TimedTrap cyclingSpikes(float x, float y, float width, float height,
                                         double safe, double warning, double active, double offset) {
        return new TimedTrap(x, y, width, height, safe, warning, active, offset, false, true);
    }

    private TimedTrap(float x, float y, float width, float height,
                      double safeDuration, double warningDuration, double activeDuration,
                      double initialOffset, boolean fixed, boolean spikeDesign) {
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
        this.fixed = fixed;
        this.spikeDesign = spikeDesign;
        reset();
    }

    public void update(double dt) {
        if (fixed || dt <= 0 || !Double.isFinite(dt)) return;
        // 먼저 dt를 줄여 큰 시간에도 덧셈 오버플로와 반복 루프를 피합니다.
        phase = (phase + dt % cycleDuration) % cycleDuration;
        refreshState();
    }

    public void reset() {
        phase = initialOffset;
        if (fixed) {
            state = TrapState.ACTIVE;
            stateElapsedTime = 0;
            return;
        }
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
            safeDuration, warningDuration, activeDuration, initialOffset, fixed, spikeDesign);
    }

    public TrapState getState() { return state; }
    public double getStateElapsedTime() { return stateElapsedTime; }
    public Rectangle getBounds() { return new Rectangle(bounds); }
    public boolean isFixed() { return fixed; }

    public boolean isDangerousTo(Player player) {
        if (state != TrapState.ACTIVE || !player.overlaps(bounds)) return false;
        if (!spikeDesign) return true; // Preserve legacy test-map collision behavior.
        Rectangle body = player.getBounds();
        // Each upright triangle narrows monotonically above its base. At the body's
        // lowest overlapping Y, its widest possible intersection decides contact.
        float bottom = Math.max(bounds.y, body.y);
        float width = bounds.width / 4f;
        float half = width * 0.5f * (1 - (bottom - bounds.y) / bounds.height);
        for (int i = 0; i < 4; i++) {
            float center = bounds.x + (i + 0.5f) * width;
            if (body.x < center + half && body.x + body.width > center - half) return true;
        }
        return false;
    }

    public void render(ShapeRenderer shapes) {
        if (spikeDesign) {
            if (state == TrapState.ACTIVE) shapes.setColor(0.9f, 0.12f, 0.12f, 1f);
            else if (state == TrapState.WARNING) shapes.setColor(0.95f, 0.65f, 0.08f, 1f);
            else shapes.setColor(0.28f, 0.30f, 0.32f, 1f);
            float width = bounds.width / 4f;
            for (int i = 0; i < 4; i++) {
                float left = bounds.x + i * width;
                shapes.triangle(left, bounds.y, left + width, bounds.y,
                    left + width / 2f, bounds.y + bounds.height);
            }
            return;
        }
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
