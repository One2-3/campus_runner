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
            : stageNumber == 2 ? createFreshmanSemester2()
            : stageNumber == 3 ? createSophomoreSemester1()
            : stageNumber == 4 ? createSophomoreSemester2()
            : stageNumber == 5 ? createJuniorSemester1()
            : stageNumber == 6 ? createJuniorSemester2()
            : stageNumber == 7 ? createSeniorSemester1()
            : stageNumber == 8 ? createSeniorSemester2()
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

    // First exam period: watch, board and ride before the final fixed steps.
    private static StageDefinition createFreshmanSemester2() {
        Array<Rectangle> blocks = new Array<>();
        blocks.add(new Rectangle(0, 0, 900, 60));
        blocks.add(new Rectangle(300, 60, 140, 50));
        blocks.add(new Rectangle(620, 60, 140, 70));
        // The 580-unit gap requires the shuttle; neither shore is a jump away.
        blocks.add(new Rectangle(1480, 0, 1120, 60));
        blocks.add(new Rectangle(1840, 60, 160, 80));
        blocks.add(new Rectangle(2040, 140, 160, 20));
        blocks.add(new Rectangle(2240, 220, 160, 20));
        blocks.add(new Rectangle(2440, 0, 160, 320));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(940, 100, 1300, 100, 140, 20, 70));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(160, 84, 24, 24));
        items.add(new GradeItem(350, 134, 24, 24));
        items.add(new GradeItem(1180, 144, 24, 24));
        items.add(new GradeItem(1710, 84, 24, 24));
        items.add(new GradeItem(2490, 344, 24, 24));
        return new StageDefinition(2, semesterName(2), 2600, 80, 60,
            new Rectangle(2540, 320, 20, 100), blocks, items, platforms);
    }

    // A low campus route rises by elevator to an elevated horizontal crossing.
    private static StageDefinition createSophomoreSemester1() {
        Array<Rectangle> blocks = new Array<>();
        blocks.add(new Rectangle(0, 0, 1040, 60));
        blocks.add(new Rectangle(280, 130, 180, 20));
        blocks.add(new Rectangle(600, 90, 180, 20));
        blocks.add(new Rectangle(1320, 340, 400, 20));
        blocks.add(new Rectangle(2280, 0, 520, 360));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(1100, 80, 1100, 340, 160, 20, 65));
        platforms.add(new MovingPlatform(1760, 340, 2080, 340, 160, 20, 90));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(160, 84, 24, 24));
        items.add(new GradeItem(350, 174, 24, 24));
        items.add(new GradeItem(660, 134, 24, 24));
        items.add(new GradeItem(1160, 264, 24, 24));
        items.add(new GradeItem(1980, 384, 24, 24));
        items.add(new GradeItem(2660, 384, 24, 24));
        return new StageDefinition(3, semesterName(3), 2800, 80, 60,
            new Rectangle(2740, 360, 20, 100), blocks, items, platforms);
    }

    private static StageDefinition createSophomoreSemester2() {
        Array<Rectangle> blocks = new Array<>();
        blocks.add(new Rectangle(0, 0, 1020, 60));
        blocks.add(new Rectangle(1540, 0, 280, 60));
        blocks.add(new Rectangle(1940, 0, 500, 60));
        blocks.add(new Rectangle(2700, 340, 300, 20));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(1060, 100, 1340, 100, 140, 20, 85));
        platforms.add(new MovingPlatform(2500, 80, 2500, 340, 140, 20, 75));
        Array<TimedTrap> traps = new Array<>();
        traps.add(TimedTrap.fixedSpikes(420, 60, 64, 24));
        traps.add(TimedTrap.fixedSpikes(680, 60, 64, 24));
        traps.add(TimedTrap.fixedSpikes(880, 60, 56, 24));
        traps.add(TimedTrap.fixedSpikes(1690, 60, 72, 28));
        traps.add(TimedTrap.cyclingSpikes(2010, 60, 144, 72, 1.4, 0.6, 1.6, 0));
        traps.add(TimedTrap.cyclingSpikes(2220, 60, 144, 72, 1.1, 0.7, 2.0, 2.2));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(160, 84, 24, 24));
        items.add(new GradeItem(550, 84, 24, 24));
        items.add(new GradeItem(810, 84, 24, 24));
        items.add(new GradeItem(1180, 144, 24, 24));
        items.add(new GradeItem(2560, 264, 24, 24));
        items.add(new GradeItem(2170, 84, 24, 24));
        items.add(new GradeItem(2860, 384, 24, 24));
        return new StageDefinition(4, semesterName(4), 3000, 80, 60,
            new Rectangle(2940, 360, 20, 100), blocks, items, platforms, traps);
    }

    private static StageDefinition createJuniorSemester1() {
        Array<Rectangle> blocks = new Array<>();
        blocks.add(new Rectangle(0, 0, 460, 60));
        blocks.add(new Rectangle(580, 0, 800, 100));
        blocks.add(new Rectangle(1900, 0, 480, 100));
        blocks.add(new Rectangle(2500, 0, 700, 180));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(1420, 140, 1700, 140, 140, 20, 110));
        Array<TimedTrap> traps = new Array<>();
        traps.add(TimedTrap.fixedSpikes(300, 60, 56, 24));
        traps.add(TimedTrap.fixedSpikes(2230, 100, 72, 28));
        traps.add(TimedTrap.cyclingSpikes(2660, 180, 144, 72, 1.15, 0.65, 2.0, 0.4));
        Array<PatrollingEnemy> enemies = new Array<>();
        enemies.add(new PatrollingEnemy(900, 100, 30, 32, 850, 970, 85, true));
        enemies.add(new PatrollingEnemy(2070, 100, 32, 36, 1920, 2100, 90, false));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(140, 84, 24, 24));
        items.add(new GradeItem(430, 84, 24, 24));
        items.add(new GradeItem(740, 124, 24, 24));
        items.add(new GradeItem(1040, 124, 24, 24));
        items.add(new GradeItem(1570, 184, 24, 24));
        items.add(new GradeItem(1920, 124, 24, 24));
        items.add(new GradeItem(2860, 204, 24, 24));
        items.add(new GradeItem(3080, 204, 24, 24));
        return new StageDefinition(5, semesterName(5), 3200, 80, 60,
            new Rectangle(3140, 180, 20, 100), blocks, items, platforms, traps, enemies);
    }

    private static StageDefinition createJuniorSemester2() {
        Array<Rectangle> blocks = new Array<>();
        blocks.add(new Rectangle(0, 0, 1000, 60));
        blocks.add(new Rectangle(1520, 0, 420, 60));
        blocks.add(new Rectangle(2200, 300, 920, 20));
        blocks.add(new Rectangle(3160, 380, 240, 20));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(1040, 100, 1320, 100, 140, 20, 110));
        platforms.add(new MovingPlatform(2000, 80, 2000, 300, 140, 20, 80));
        Array<TimedTrap> traps = new Array<>();
        traps.add(TimedTrap.fixedSpikes(300, 60, 56, 24));
        traps.add(TimedTrap.fixedSpikes(1810, 60, 64, 24));
        traps.add(TimedTrap.fixedSpikes(3000, 320, 64, 24));
        traps.add(TimedTrap.cyclingSpikes(2320, 320, 144, 72, 1.2, 0.6, 1.8, 0));
        traps.add(TimedTrap.cyclingSpikes(2540, 320, 144, 72, 1.05, 0.75, 2.2, 2.3));
        Array<PatrollingEnemy> enemies = new Array<>();
        enemies.add(new PatrollingEnemy(560, 60, 30, 32, 500, 620, 90, true));
        enemies.add(new PatrollingEnemy(1670, 60, 32, 36, 1540, 1720, 100, false));
        enemies.add(new PatrollingEnemy(2860, 320, 32, 32, 2780, 2900, 110, false));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(140, 84, 24, 24));
        items.add(new GradeItem(420, 84, 24, 24));
        items.add(new GradeItem(700, 84, 24, 24));
        items.add(new GradeItem(1160, 144, 24, 24));
        items.add(new GradeItem(1540, 84, 24, 24));
        items.add(new GradeItem(2060, 224, 24, 24));
        items.add(new GradeItem(2480, 344, 24, 24));
        items.add(new GradeItem(2710, 344, 24, 24));
        items.add(new GradeItem(3300, 424, 24, 24));
        return new StageDefinition(6, semesterName(6), 3400, 80, 60,
            new Rectangle(3340, 400, 20, 100), blocks, items, platforms, traps, enemies);
    }

    private static StageDefinition createSeniorSemester1() {
        Array<Rectangle> blocks = new Array<>();
        blocks.add(new Rectangle(0, 0, 800, 60));
        blocks.add(new Rectangle(1440, 280, 360, 20));
        blocks.add(new Rectangle(1880, 240, 100, 20));
        blocks.add(new Rectangle(2040, 300, 140, 20));
        blocks.add(new Rectangle(2240, 260, 700, 20));
        blocks.add(new Rectangle(3380, 280, 280, 20));
        blocks.add(new Rectangle(3680, 360, 120, 20));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(840, 100, 1040, 100, 140, 20, 90));
        platforms.add(new MovingPlatform(1240, 100, 1240, 280, 140, 20, 65));
        platforms.add(new MovingPlatform(2980, 280, 3180, 280, 140, 20, 100));
        Array<TimedTrap> traps = new Array<>();
        traps.add(TimedTrap.fixedSpikes(260, 60, 56, 24));
        traps.add(TimedTrap.fixedSpikes(650, 60, 56, 24));
        traps.add(TimedTrap.fixedSpikes(1730, 300, 48, 24));
        traps.add(TimedTrap.fixedSpikes(1950, 260, 24, 24));
        traps.add(TimedTrap.fixedSpikes(3610, 300, 48, 24));
        traps.add(TimedTrap.cyclingSpikes(2300, 280, 144, 72, 1.15, 0.65, 1.8, 0));
        traps.add(TimedTrap.cyclingSpikes(2520, 280, 144, 72, 1.05, 0.75, 2.0, 2.3));
        traps.add(TimedTrap.cyclingSpikes(2740, 280, 144, 72, 1.0, 0.7, 2.4, 1.2));
        Array<PatrollingEnemy> enemies = new Array<>();
        enemies.add(new PatrollingEnemy(480, 60, 30, 32, 420, 540, 90, true));
        enemies.add(new PatrollingEnemy(1600, 300, 30, 32, 1540, 1660, 95, false));
        enemies.add(new PatrollingEnemy(2130, 320, 28, 32, 2100, 2140, 105, true));
        enemies.add(new PatrollingEnemy(3500, 300, 28, 36, 3410, 3530, 70, false));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(140, 84, 24, 24));
        items.add(new GradeItem(750, 84, 24, 24));
        items.add(new GradeItem(580, 84, 24, 24));
        items.add(new GradeItem(980, 144, 24, 24));
        items.add(new GradeItem(1300, 204, 24, 24));
        items.add(new GradeItem(1900, 284, 24, 24));
        items.add(new GradeItem(2050, 344, 24, 24));
        items.add(new GradeItem(2470, 304, 24, 24));
        items.add(new GradeItem(2690, 304, 24, 24));
        items.add(new GradeItem(3710, 404, 24, 24));
        return new StageDefinition(7, semesterName(7), 3800, 80, 60,
            new Rectangle(3760, 380, 20, 100), blocks, items, platforms, traps, enemies);
    }

    private static StageDefinition createSeniorSemester2() {
        Array<Rectangle> blocks = new Array<>();
        // A: descending terraces, with hazards on the far ends rather than blind landings.
        blocks.add(new Rectangle(0, 340, 220, 20));
        blocks.add(new Rectangle(280, 260, 180, 20));
        blocks.add(new Rectangle(540, 180, 150, 20));
        blocks.add(new Rectangle(760, 100, 240, 20));
        // B: a real upper/lower fork, connected at both ends for item recovery.
        blocks.add(new Rectangle(1000, 100, 160, 20));
        blocks.add(new Rectangle(1200, 200, 100, 20));
        blocks.add(new Rectangle(1370, 260, 90, 20));
        blocks.add(new Rectangle(1530, 220, 100, 20));
        blocks.add(new Rectangle(1200, 40, 480, 20));
        blocks.add(new Rectangle(1700, 100, 260, 20));
        // C: rise in a compact tower, ride upward, jump left, then climb right.
        blocks.add(new Rectangle(2000, 180, 100, 20));
        blocks.add(new Rectangle(2010, 340, 100, 20));
        blocks.add(new Rectangle(2240, 420, 100, 20));
        blocks.add(new Rectangle(2370, 380, 140, 20));
        // D: uneven broken bridge islands; no continuous safety floor underneath.
        blocks.add(new Rectangle(2600, 300, 160, 20));
        blocks.add(new Rectangle(2820, 340, 140, 20));
        blocks.add(new Rectangle(3060, 260, 160, 20));
        blocks.add(new Rectangle(3280, 320, 90, 20));
        blocks.add(new Rectangle(3450, 240, 180, 20));
        // E: graduation staircase and a moving final approach.
        blocks.add(new Rectangle(3740, 300, 140, 20));
        blocks.add(new Rectangle(3930, 380, 100, 20));
        blocks.add(new Rectangle(4300, 460, 200, 20));
        Array<MovingPlatform> platforms = new Array<>();
        platforms.add(new MovingPlatform(2190, 120, 2190, 320, 120, 20, 65));
        platforms.add(new MovingPlatform(4060, 400, 4140, 400, 120, 20, 65));
        Array<TimedTrap> traps = new Array<>();
        traps.add(TimedTrap.fixedSpikes(400, 280, 40, 24));
        traps.add(TimedTrap.fixedSpikes(900, 120, 40, 24));
        traps.add(TimedTrap.fixedSpikes(3170, 280, 40, 24));
        traps.add(TimedTrap.cyclingSpikes(1530, 60, 96, 48, 1.3, 0.7, 1.8, 0));
        traps.add(TimedTrap.cyclingSpikes(2890, 360, 56, 48, 1.1, 0.7, 1.6, 1.0));
        Array<PatrollingEnemy> enemies = new Array<>();
        enemies.add(new PatrollingEnemy(1370, 60, 28, 32, 1320, 1420, 85, true));
        enemies.add(new PatrollingEnemy(2690, 320, 28, 32, 2670, 2700, 90, false));
        enemies.add(new PatrollingEnemy(3550, 260, 30, 32, 3520, 3580, 95, false));
        Array<GradeItem> items = new Array<>();
        items.add(new GradeItem(140, 384, 24, 24));
        items.add(new GradeItem(570, 224, 24, 24));
        items.add(new GradeItem(1230, 244, 24, 24));
        items.add(new GradeItem(1390, 304, 24, 24));
        items.add(new GradeItem(1260, 84, 24, 24));
        items.add(new GradeItem(1640, 84, 24, 24));
        items.add(new GradeItem(2080, 224, 24, 24));
        items.add(new GradeItem(2280, 464, 24, 24));
        items.add(new GradeItem(2740, 344, 24, 24));
        items.add(new GradeItem(3290, 364, 24, 24));
        items.add(new GradeItem(3800, 344, 24, 24));
        items.add(new GradeItem(4380, 504, 24, 24));
        return new StageDefinition(8, semesterName(8), 4500, 80, 360,
            new Rectangle(4440, 480, 20, 100), blocks, items, platforms, traps, enemies);
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
