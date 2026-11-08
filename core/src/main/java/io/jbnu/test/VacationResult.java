package io.jbnu.test;

/** Confirmed once per vacation; independent of semester scores and GPA. */
public final class VacationResult {
    private final VacationEventType type;
    private final boolean success;
    private final PlayerModifier modifier;

    public VacationResult(VacationEventType type, boolean success) {
        if (type == null) throw new IllegalArgumentException("Vacation type is required");
        this.type = type;
        this.success = success;
        modifier = PlayerModifier.fromVacation(type, success);
    }

    public VacationEventType getType() { return type; }
    public boolean isSuccess() { return success; }
    public PlayerModifier getModifier() { return modifier; }
}
