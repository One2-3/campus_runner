package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 변경되지 않는 스테이지 원본 데이터. 가변 객체는 입력과 반환 시 깊은 복사합니다. */
public final class StageDefinition {
    private final int stageNumber;
    private final String stageName;
    private final float worldWidth;
    private final float startX, startY;
    private final Rectangle flag;
    private final Array<Rectangle> blocks;
    private final Array<GradeItem> gradeItems;
    private final Array<MovingPlatform> movingPlatforms;
    private final Array<TimedTrap> timedTraps;
    private final Array<PatrollingEnemy> enemies;

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks) {
        this(stageNumber, stageName, worldWidth, startX, startY, flag, blocks,
            new Array<GradeItem>());
    }

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks, Array<GradeItem> gradeItems) {
        this(stageNumber, stageName, worldWidth, startX, startY, flag, blocks, gradeItems,
            new Array<MovingPlatform>());
    }

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks, Array<GradeItem> gradeItems,
                           Array<MovingPlatform> movingPlatforms) {
        this(stageNumber, stageName, worldWidth, startX, startY, flag, blocks, gradeItems,
            movingPlatforms, new Array<TimedTrap>());
    }

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks, Array<GradeItem> gradeItems,
                           Array<MovingPlatform> movingPlatforms, Array<TimedTrap> timedTraps) {
        this(stageNumber, stageName, worldWidth, startX, startY, flag, blocks, gradeItems,
            movingPlatforms, timedTraps, new Array<PatrollingEnemy>());
    }

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks, Array<GradeItem> gradeItems,
                           Array<MovingPlatform> movingPlatforms, Array<TimedTrap> timedTraps,
                           Array<PatrollingEnemy> enemies) {
        this.stageNumber = stageNumber;
        this.stageName = stageName;
        this.worldWidth = worldWidth;
        this.startX = startX;
        this.startY = startY;
        this.flag = new Rectangle(flag);
        this.blocks = copyBlocks(blocks);
        this.gradeItems = copyGradeItems(gradeItems);
        this.movingPlatforms = copyMovingPlatforms(movingPlatforms);
        this.timedTraps = copyTimedTraps(timedTraps);
        this.enemies = copyEnemies(enemies);
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public String getStageName() {
        return stageName;
    }

    public float getWorldWidth() {
        return worldWidth;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public Rectangle getFlag() {
        return new Rectangle(flag);
    }

    public Array<Rectangle> getBlocks() {
        return copyBlocks(blocks);
    }

    public Array<GradeItem> getGradeItems() {
        return copyGradeItems(gradeItems);
    }

    public Array<MovingPlatform> getMovingPlatforms() {
        return copyMovingPlatforms(movingPlatforms);
    }

    public Array<TimedTrap> getTimedTraps() {
        return copyTimedTraps(timedTraps);
    }

    public Array<PatrollingEnemy> getEnemies() {
        return copyEnemies(enemies);
    }

    private static Array<PatrollingEnemy> copyEnemies(Array<PatrollingEnemy> source) {
        Array<PatrollingEnemy> copy = new Array<>(source.size);
        for (PatrollingEnemy enemy : source) copy.add(enemy.copyInitial());
        return copy;
    }

    private static Array<TimedTrap> copyTimedTraps(Array<TimedTrap> source) {
        Array<TimedTrap> copy = new Array<>(source.size);
        for (TimedTrap trap : source) copy.add(trap.copyInitial());
        return copy;
    }

    private static Array<MovingPlatform> copyMovingPlatforms(Array<MovingPlatform> source) {
        Array<MovingPlatform> copy = new Array<>(source.size);
        for (MovingPlatform platform : source) copy.add(platform.copyInitial());
        return copy;
    }

    private static Array<GradeItem> copyGradeItems(Array<GradeItem> source) {
        Array<GradeItem> copy = new Array<>(source.size);
        for (GradeItem item : source) {
            copy.add(item.copyUncollected());
        }
        return copy;
    }

    private static Array<Rectangle> copyBlocks(Array<Rectangle> source) {
        Array<Rectangle> copy = new Array<>(source.size);
        for (Rectangle block : source) {
            copy.add(new Rectangle(block));
        }
        return copy;
    }
}
