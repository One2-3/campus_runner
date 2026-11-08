package io.jbnu.test;

/** Immutable absolute multipliers; replacing a modifier never compounds the previous one. */
public final class PlayerModifier {
    public static final PlayerModifier NONE = new PlayerModifier(null, false, 1f, 1f, 0);
    private final VacationEventType source;
    private final boolean success;
    private final float moveSpeedMultiplier, jumpPowerMultiplier;
    private final int extraStartingLives;

    private PlayerModifier(VacationEventType source, boolean success, float move, float jump, int extraLives) {
        this.source = source;
        this.success = success;
        moveSpeedMultiplier = move;
        jumpPowerMultiplier = jump;
        extraStartingLives = extraLives;
    }

    static PlayerModifier fromVacation(VacationEventType type, boolean success) {
        switch (type) {
            case CAFE: return new PlayerModifier(type, success, success ? 1f : .95f, 1f, success ? 1 : 0);
            case CERTIFICATION: return new PlayerModifier(type, success, 1f, success ? 1.10f : .95f, 0);
            case HACKATHON: return new PlayerModifier(type, success, success ? 1.10f : .95f, 1f, 0);
            default: throw new IllegalArgumentException("Unknown vacation type");
        }
    }

    public VacationEventType getSource() { return source; }
    public boolean isSuccess() { return success; }
    public int getTargetYear() { return source == null ? 0 : source.getTargetYear(); }
    public float getMoveSpeedMultiplier() { return moveSpeedMultiplier; }
    public float getJumpPowerMultiplier() { return jumpPowerMultiplier; }
    public int getExtraStartingLives() { return extraStartingLives; }
}
