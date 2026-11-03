package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 학년 번호에 해당하는 스테이지 정의를 생성합니다. */
public final class StageFactory {
    private StageFactory() {
    }

    public static StageDefinition createStage(int stageNumber) {
        switch (stageNumber) {
            case 1:
                return createFirstYear();
            default:
                throw new IllegalArgumentException("Unsupported stage: " + stageNumber);
        }
    }

    private static StageDefinition createFirstYear() {
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
        return new StageDefinition(1, "1학년", 2600, 80, 60,
            new Rectangle(2420, 344, 20, 100), blocks);
    }
}
