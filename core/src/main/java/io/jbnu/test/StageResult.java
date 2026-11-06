package io.jbnu.test;

/** 클리어 시 확정되는 학기별 결과입니다. 저장 후에는 변경하지 않습니다. */
public final class StageResult {
    private final int stageNumber;
    private final int itemScore;
    private final int collectedItemCount;
    private final int deathCount;
    private final int survivalBonus;

    public StageResult(int stageNumber, int score, int collectedItemCount) {
        this(stageNumber, score, collectedItemCount, 0);
    }

    public StageResult(int stageNumber, int itemScore, int collectedItemCount, int deathCount) {
        if (deathCount < 0) throw new IllegalArgumentException("Negative semester death count");
        this.stageNumber = stageNumber;
        this.itemScore = itemScore;
        this.collectedItemCount = collectedItemCount;
        this.deathCount = deathCount;
        this.survivalBonus = Math.max(0, 20 - deathCount * 5);
    }

    public int getStageNumber() { return stageNumber; }
    public int getItemScore() { return itemScore; }
    public int getDeathCount() { return deathCount; }
    public int getSurvivalBonus() { return survivalBonus; }
    public int getFinalScore() { return itemScore + survivalBonus; }
    /** Confirmed semester score, including the survival bonus. HUD uses getItemScore(). */
    public int getScore() { return getFinalScore(); }
    public int getCollectedItemCount() { return collectedItemCount; }
}
