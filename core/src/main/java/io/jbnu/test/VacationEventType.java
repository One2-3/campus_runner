package io.jbnu.test;

/** The winter vacation after each of the first three school years. */
public enum VacationEventType {
    CAFE(2, "CAFE PART-TIME JOB", "STARTING LIVES +1 (6)", "MOVE SPEED -5%"),
    CERTIFICATION(4, "CERTIFICATION STUDY", "JUMP POWER +10%", "JUMP POWER -5%"),
    HACKATHON(6, "HACKATHON", "MOVE SPEED +10%", "MOVE SPEED -5%");

    private final int completedStage;
    private final String title, successReward, failurePenalty;

    VacationEventType(int completedStage, String title, String successReward, String failurePenalty) {
        this.completedStage = completedStage;
        this.title = title;
        this.successReward = successReward;
        this.failurePenalty = failurePenalty;
    }

    public int getCompletedStage() { return completedStage; }
    public int getTargetYear() { return completedStage / 2 + 1; }
    public String getTitle() { return title; }
    public String getSuccessReward() { return successReward; }
    public String getFailurePenalty() { return failurePenalty; }
    public static VacationEventType afterStage(int stage) {
        for (VacationEventType type : values()) if (type.completedStage == stage) return type;
        return null;
    }
}
