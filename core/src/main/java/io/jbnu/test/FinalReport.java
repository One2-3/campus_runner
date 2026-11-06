package io.jbnu.test;

/** Read-only summary of the eight confirmed semester results, independent of rendering. */
public final class FinalReport {
    public static final int MAX_ITEM_SCORE = 305;
    public static final int MAX_SURVIVAL_BONUS = 160;
    public static final int MAX_FINAL_SCORE = MAX_ITEM_SCORE + MAX_SURVIVAL_BONUS;
    private static final int[] ITEM_MAXIMA = {20, 25, 30, 35, 40, 45, 50, 60};

    private final StageResult[] semesters;
    private final int totalItemScore;
    private final int totalSurvivalBonus;
    private final int totalDeathCount;

    public FinalReport(StageResult[] results, int totalDeathCount) {
        if (results == null || results.length != ITEM_MAXIMA.length || totalDeathCount < 0) {
            throw new IllegalArgumentException("A final report requires eight results and a nonnegative death count");
        }
        semesters = results.clone();
        int items = 0, bonuses = 0;
        for (int i = 0; i < semesters.length; i++) {
            StageResult result = semesters[i];
            if (result == null || result.getStageNumber() != i + 1) {
                throw new IllegalArgumentException("Missing or unordered semester result: " + (i + 1));
            }
            items += result.getItemScore();
            bonuses += result.getSurvivalBonus();
        }
        this.totalItemScore = items;
        this.totalSurvivalBonus = bonuses;
        this.totalDeathCount = totalDeathCount;
    }

    public StageResult getSemester(int index) { return semesters[index]; }
    public int getItemMaximum(int index) { return ITEM_MAXIMA[index]; }
    public int getSemesterCount() { return semesters.length; }
    public int getTotalItemScore() { return totalItemScore; }
    public int getTotalSurvivalBonus() { return totalSurvivalBonus; }
    public int getFinalScore() { return totalItemScore + totalSurvivalBonus; }
    public int getTotalDeathCount() { return totalDeathCount; }
    public double getScoreRate() { return getFinalScore() / (double) MAX_FINAL_SCORE * 100.0; }
    public double getGpa() { return getFinalScore() / (double) MAX_FINAL_SCORE * 4.5; }
    public GraduationEnding getEnding() { return GraduationEnding.fromScore(getFinalScore()); }
}
