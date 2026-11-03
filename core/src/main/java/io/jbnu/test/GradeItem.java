package io.jbnu.test;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** 점수와 수집 상태를 가진 학점 아이템입니다. */
public final class GradeItem {
    public static final int DEFAULT_SCORE = 5;

    private final Rectangle bounds;
    private final int score;
    private boolean collected;

    public GradeItem(float x, float y, float width, float height) {
        this(x, y, width, height, DEFAULT_SCORE);
    }

    public GradeItem(float x, float y, float width, float height, int score) {
        this.bounds = new Rectangle(x, y, width, height);
        this.score = score;
    }

    /** 배치 데이터로부터 수집되지 않은 실행용 아이템을 생성합니다. */
    public GradeItem copyUncollected() {
        return new GradeItem(bounds.x, bounds.y, bounds.width, bounds.height, score);
    }

    /** 처음 수집된 순간에만 true를 반환합니다. */
    public boolean tryCollect(Player player) {
        if (collected || !player.overlaps(bounds)) return false;
        collected = true;
        return true;
    }

    public int getScore() {
        return score;
    }

    public boolean isCollected() {
        return collected;
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    /** 향후 이미지로 바꿀 때 이 렌더링 부분을 교체합니다. */
    public void render(ShapeRenderer shapes) {
        if (collected) return;
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;
        shapes.setColor(0.85f, 0.48f, 0.02f, 1f);
        shapes.triangle(centerX, bounds.y + bounds.height,
            bounds.x, centerY, bounds.x + bounds.width, centerY);
        shapes.triangle(centerX, bounds.y,
            bounds.x, centerY, bounds.x + bounds.width, centerY);
    }
}
