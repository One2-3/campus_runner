package io.jbnu.test;

import com.badlogic.gdx.Input;

/** Graphics-free gameplay model. Each transition consumes only the current screen's input. */
public abstract class VacationMinigame {
    public enum Phase { INTRO, PLAYING, RESULT }
    private final VacationEventType type;
    private final double timeLimit;
    private double remaining;
    private Phase phase = Phase.INTRO;
    private boolean success;

    protected VacationMinigame(VacationEventType type, double timeLimit) {
        this.type = type;
        this.timeLimit = timeLimit;
        remaining = timeLimit;
    }

    public static VacationMinigame create(VacationEventType type) {
        switch (type) {
            case CAFE: return new CafeMinigame();
            case CERTIFICATION: return new CertificationMinigame();
            case HACKATHON: return new HackathonMinigame();
            default: throw new IllegalArgumentException("Unknown vacation game");
        }
    }

    public final void update(float delta, MinigameInput input) {
        if (phase == Phase.INTRO) {
            if (input.isPressed(Input.Keys.ENTER)) { phase = Phase.PLAYING; onStart(); }
            return;
        }
        if (phase != Phase.PLAYING || delta <= 0 || !Float.isFinite(delta)) return;
        float step = (float)Math.min(delta,remaining);
        remaining = Math.max(0,remaining-step);
        play(step,input);
        if (phase == Phase.PLAYING && remaining < 0.00001) { remaining = 0; finish(successAtTimeout()); }
    }

    protected void onStart() { }
    protected abstract void play(float delta, MinigameInput input);
    protected boolean successAtTimeout() { return false; }
    protected final void finish(boolean success) {
        if (phase != Phase.PLAYING) return;
        this.success = success;
        phase = Phase.RESULT;
    }

    public VacationEventType getType() { return type; }
    public Phase getPhase() { return phase; }
    public double getTimeRemaining() { return remaining; }
    public double getTimeLimit() { return timeLimit; }
    public boolean isFinished() { return phase == Phase.RESULT; }
    public boolean isSuccess() {
        if (!isFinished()) throw new IllegalStateException("Game has not finished");
        return success;
    }
}
