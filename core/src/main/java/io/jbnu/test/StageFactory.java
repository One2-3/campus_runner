package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 1~8학기에 해당하는 독립적인 스테이지 정의를 생성합니다. */
public final class StageFactory {
    public static final int STAGE_COUNT = 8;

    private StageFactory() {
    }

    public static StageDefinition createStage(int stageNumber) {
        if (stageNumber < 1 || stageNumber > STAGE_COUNT) {
            throw new IllegalArgumentException("Unsupported stage: " + stageNumber);
        }
        return stageNumber == 1 ? createFreshman()
            : createTestStage(stageNumber, semesterName(stageNumber));
    }

    private static String semesterName(int number) {
        return "Year " + ((number + 1) / 2) + " - Semester " + ((number - 1) % 2 + 1);
    }

    private static StageDefinition createFreshman() {
        Array<Rectangle> blocks = new Array<>();
        // A/B: safe walking, one low obstacle, then an 80-unit floor gap.
        blocks.add(new Rectangle(0, 0, 780, 60));
        blocks.add(new Rectangle(300, 60, 140, 50));
        blocks.add(new Rectangle(860, 0, 1040, 60));
        // B/C: optional item ledges; the clear route stays on the floor.
        blocks.add(new Rectangle(1000, 130, 160, 20));
        blocks.add(new Rectangle(1300, 120, 160, 20));
        blocks.add(new Rectangle(1490, 200, 160, 20));
        // D: broad steps cross the second 80-unit gap and reach the fixed flag.
        blocks.add(new Rectangle(1800, 120, 150, 20));
        blocks.add(new Rectangle(1980, 200, 140, 20));
        blocks.add(new Rectangle(2140, 280, 100, 20));
        blocks.add(new Rectangle(2240, 0, 160, 360));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(130, 84, 24, 24));
        items.add(new GradeItem(520, 84, 24, 24));
        items.add(new GradeItem(1360, 164, 24, 24));
        items.add(new GradeItem(2310, 384, 24, 24));
        return new StageDefinition(1, semesterName(1), 2400, 80, 60,
            new Rectangle(2360, 360, 20, 100), blocks, items);
    }

    // Preserve the original test layout; every call builds independent objects.
    private static StageDefinition createTestStage(int stageNumber, String stageName) {
        Array<Rectangle> blocks = new Array<>();
        // 기존 실습 맵의 좌표와 크기를 그대로 사용합니다.
        blocks.add(new Rectangle(0, 0, 620, 60));
        blocks.add(new Rectangle(760, 0, 540, 60));
        blocks.add(new Rectangle(1440, 0, 1160, 60));
        blocks.add(new Rectangle(300, 60, 80, 64));
        blocks.add(new Rectangle(900, 60, 96, 80));
        blocks.add(new Rectangle(1080, 180, 160, 24));
        blocks.add(new Rectangle(1650, 60, 120, 70));
        blocks.add(new Rectangle(1790, 60, 120, 140));
        blocks.add(new Rectangle(1930, 60, 120, 210));
        blocks.add(new Rectangle(2100, 270, 110, 24));
        blocks.add(new Rectangle(2280, 320, 210, 24));
        Array<GradeItem> items = new Array<>();
        // 바닥 경로, 장애물 위, 선택 점프 위치에 배치합니다. 크기는 24 x 24입니다.
        items.add(new GradeItem(180, 84, 24, 24));
        items.add(new GradeItem(330, 148, 24, 24));
        items.add(new GradeItem(500, 180, 24, 24));
        items.add(new GradeItem(820, 84, 24, 24));
        items.add(new GradeItem(940, 164, 24, 24));
        items.add(new GradeItem(1140, 228, 24, 24));
        items.add(new GradeItem(1520, 84, 24, 24));
        items.add(new GradeItem(1838, 224, 24, 24));
        items.add(new GradeItem(2140, 318, 24, 24));
        items.add(new GradeItem(2340, 368, 24, 24));
        Array<MovingPlatform> movingPlatforms = new Array<>();
        // 바닥 위 통행 높이(44)를 확보하고 기존 장애물과 겹치지 않는 경로입니다.
        movingPlatforms.add(new MovingPlatform(500, 110, 780, 110, 96, 18, 70));
        // 두 번째 낭떠러지 안의 선택형 승강 발판입니다.
        movingPlatforms.add(new MovingPlatform(1360, 70, 1360, 230, 64, 18, 55));
        Array<TimedTrap> timedTraps = new Array<>();
        // 평평한 바닥 위의 짧은 구간으로, 대기하거나 점프로 회피할 수 있습니다.
        timedTraps.add(new TimedTrap(420, 60, 48, 24, 0.0));
        timedTraps.add(new TimedTrap(1550, 60, 48, 24, 1.2));
        Array<PatrollingEnemy> enemies = new Array<>();
        // 몸 전체가 바닥/고정 발판 안에 머물며 함정 구간과 분리됩니다.
        enemies.add(new PatrollingEnemy(220, 60, 28, 28, 190, 248, 45, true));
        enemies.add(new PatrollingEnemy(1180, 204, 28, 28, 1100, 1190, 38, false));
        return new StageDefinition(stageNumber, stageName, 2600, 80, 60,
            new Rectangle(2420, 344, 20, 100), blocks, items, movingPlatforms, timedTraps, enemies);
    }
}
