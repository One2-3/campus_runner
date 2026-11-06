package io.jbnu.test;

import com.badlogic.gdx.Input;
import java.util.Random;

/** Memorize a visible pattern, then reproduce it using fresh arrow-key presses. */
public final class CertificationMinigame extends VacationMinigame {
    private static final int[] LENGTHS = {2,3,3,4,5};
    private static final int[] DIRECTIONS = {Input.Keys.UP,Input.Keys.DOWN,Input.Keys.LEFT,Input.Keys.RIGHT};
    private final Random random;
    private int round, inputIndex, mistakes;
    private int[] pattern;
    private double showRemaining;

    public CertificationMinigame() { this(new Random()); }
    public CertificationMinigame(Random random) { super(VacationEventType.CERTIFICATION,40); this.random = random; }
    @Override protected void onStart() { newRound(); }

    private void newRound() {
        pattern = new int[LENGTHS[round]];
        for (int i = 0; i < pattern.length; i++) pattern[i] = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        retryRound();
    }
    private void retryRound() { inputIndex = 0; showRemaining = 2.0 + pattern.length*.25; }

    @Override protected void play(float delta, MinigameInput input) {
        if (showRemaining > 0) { showRemaining = Math.max(0,showRemaining-delta); return; }
        int pressed = -1, count = 0;
        for (int direction : DIRECTIONS) if (input.isPressed(direction)) { pressed = direction; count++; }
        if (count != 1) return;
        if (pressed != pattern[inputIndex]) {
            mistakes++;
            if (mistakes == 3) finish(false);
            else retryRound();
            return;
        }
        inputIndex++;
        if (inputIndex == pattern.length) {
            round++;
            if (round == LENGTHS.length) finish(true);
            else newRound();
        }
    }

    public boolean isShowingPattern() { return showRemaining > 0; }
    public int[] getVisiblePattern() { return pattern != null && isShowingPattern() ? pattern.clone() : new int[0]; }
    public int getPatternLength() { return pattern == null ? LENGTHS[0] : pattern.length; }
    public int getRoundNumber() { return Math.min(round+1,5); }
    public int getCompletedRounds() { return round; }
    public int getInputIndex() { return inputIndex; }
    public int getMistakes() { return mistakes; }
    public double getShowTimeRemaining() { return showRemaining; }
}
