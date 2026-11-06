package io.jbnu.test;

import java.util.Arrays;

/** Owns pending activity, history and one active next-year modifier. No score changes. */
public final class VacationManager {
    private final VacationResult[] results = new VacationResult[VacationEventType.values().length];
    private VacationEventType pending;
    private PlayerModifier active = PlayerModifier.NONE;

    public void beginAfterStage(int completedStage) {
        VacationEventType type = VacationEventType.afterStage(completedStage);
        if (type == null || pending != null || results[type.ordinal()] != null) {
            throw new IllegalStateException("Vacation cannot begin after stage " + completedStage);
        }
        pending = type;
        active = PlayerModifier.NONE; // The preceding school year is finished.
    }

    public boolean complete(VacationEventType type, boolean success) {
        if (type == null || type != pending || results[type.ordinal()] != null) return false;
        VacationResult result = new VacationResult(type, success);
        results[type.ordinal()] = result;
        active = result.getModifier();
        pending = null;
        return true;
    }

    public void enterStage(int stage) {
        if ((stage + 1) / 2 != active.getTargetYear()) active = PlayerModifier.NONE;
    }

    public PlayerModifier modifierForStage(int stage) {
        return (stage + 1) / 2 == active.getTargetYear() ? active : PlayerModifier.NONE;
    }

    public VacationEventType getPending() { return pending; }
    public VacationResult getResult(VacationEventType type) { return results[type.ordinal()]; }
    public void reset() { Arrays.fill(results, null); pending = null; active = PlayerModifier.NONE; }
}
