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

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks) {
        this(stageNumber, stageName, worldWidth, startX, startY, flag, blocks,
            new Array<GradeItem>());
    }

    public StageDefinition(int stageNumber, String stageName, float worldWidth,
                           float startX, float startY, Rectangle flag,
                           Array<Rectangle> blocks, Array<GradeItem> gradeItems) {
        this.stageNumber = stageNumber;
        this.stageName = stageName;
        this.worldWidth = worldWidth;
        this.startX = startX;
        this.startY = startY;
        this.flag = new Rectangle(flag);
        this.blocks = copyBlocks(blocks);
        this.gradeItems = copyGradeItems(gradeItems);
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
