package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 학년 번호에 해당하는 스테이지 정의를 생성합니다. */
public final class StageFactory {
    public static final int STAGE_COUNT = 4;

    private StageFactory() {
    }

    public static StageDefinition createStage(int stageNumber) {
        switch (stageNumber) {
            case 1:
                return createFreshman();
            case 2:
                return createSophomore();
            case 3:
                return createJunior();
            case 4:
                return createSenior();
            default:
                throw new IllegalArgumentException("Unsupported stage: " + stageNumber);
        }
    }

    private static StageDefinition createFreshman() {
        return createTestStage(1, "Freshman");
    }

    private static StageDefinition createSophomore() {
        return createTestStage(2, "Sophomore");
    }

    private static StageDefinition createJunior() {
        return createTestStage(3, "Junior");
    }

    private static StageDefinition createSenior() {
        return createTestStage(4, "Senior");
    }

    // 기존 완성 맵을 보존합니다. 향후 학년별 생성 함수에서 독자적인 배치로 교체합니다.
    // 호출할 때마다 새 데이터와 객체를 생성하므로 학년 간 상태를 공유하지 않습니다.
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
