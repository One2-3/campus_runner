package io.jbnu.test;

/** 클리어 시 확정되는 학년별 결과입니다. 저장 후에는 변경하지 않습니다. */
public final class StageResult {
    private final int stageNumber;
    private final int score;
    private final int collectedItemCount;

    public StageResult(int stageNumber, int score, int collectedItemCount) {
        this.stageNumber = stageNumber;
        this.score = score;
        this.collectedItemCount = collectedItemCount;
    }

    public int getStageNumber() { return stageNumber; }
    public int getScore() { return score; }
    public int getCollectedItemCount() { return collectedItemCount; }
}
