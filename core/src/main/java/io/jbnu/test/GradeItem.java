package io.jbnu.test;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** 점수와 수집 상태를 가진 학점 아이템입니다. */
public final class GradeItem {
    public static final int DEFAULT_SCORE = 5;
    public static final float DRAW_SIZE = 48f;
    public static final float DRAW_OFFSET_Y = -10f;

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

    /** Shared texture, centered horizontally and shifted down visually; collection bounds stay unchanged. */
    public void render(SpriteBatch batch, Texture texture) {
        if (collected) return;
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;
        batch.draw(texture,centerX-DRAW_SIZE/2f,centerY-DRAW_SIZE/2f+DRAW_OFFSET_Y,DRAW_SIZE,DRAW_SIZE);
    }
}
