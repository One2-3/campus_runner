package io.jbnu.test;

/** Render-only offsets and a one-shot contact-death clock; never modifies camera or player coordinates. */
public final class HitFeedback {
    public static final double HIT_SECONDS = 1.0;
    public static final double SHAKE_SECONDS = .25;
    public static final float SHAKE_STRENGTH = 5f;
    private boolean active;
    private double elapsed;

    public boolean start() {
        if (active) return false;
        active = true; elapsed = 0; return true;
    }
    public boolean advance(float delta) {
        if (!active || delta <= 0 || !Float.isFinite(delta)) return false;
        elapsed = Math.min(HIT_SECONDS,elapsed+delta);
        if (elapsed >= HIT_SECONDS) { active = false; return true; }
        return false;
    }
    public void reset() { active = false; elapsed = 0; }
    public boolean isActive() { return active; }
    public double getElapsed() { return elapsed; }
    public float getShakeX() { return offset(true); }
    public float getShakeY() { return offset(false); }
    private float offset(boolean x) {
        if (!active || elapsed >= SHAKE_SECONDS) return 0;
        double amplitude = SHAKE_STRENGTH*(1-elapsed/SHAKE_SECONDS);
        double angle = elapsed*Math.PI*40;
        return (float)(amplitude*(x ? Math.cos(angle) : Math.sin(angle)));
    }
}
