package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Presentation of an already confirmed semester result; never recalculates scores. */
public final class StageClearRenderer implements Disposable {
    private static final Color CREAM = new Color(0xF5E8CBFF);
    private static final Color BORDER = new Color(0x594738FF);
    private static final Color INK = new Color(0x493B32FF);
    private static final Color YELLOW = new Color(0xEBCB6EFF);
    private static final Color HIGHLIGHT = new Color(0xF7DE8DFF);
    private final Texture pixel;
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout text = new GlyphLayout();
    private final Matrix4 savedProjection = new Matrix4(), savedTransform = new Matrix4();
    private final Matrix4 screen = new Matrix4(), identity = new Matrix4();

    public StageClearRenderer() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try { p.setColor(Color.WHITE); p.fill(); pixel = new Texture(p); }
        finally { p.dispose(); }
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (com.badlogic.gdx.graphics.g2d.TextureRegion region : font.getRegions())
            region.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.setColor(INK);
        font.setUseIntegerPositions(true);
    }

    public void render(SpriteBatch batch, FitViewport viewport, StageResult result, int totalItems) {
        float color = batch.getPackedColor();
        ShaderProgram shader = batch.getShader();
        boolean blending = batch.isBlendingEnabled();
        int src = batch.getBlendSrcFunc(), dst = batch.getBlendDstFunc();
        int srcAlpha = batch.getBlendSrcFuncAlpha(), dstAlpha = batch.getBlendDstFuncAlpha();
        savedProjection.set(batch.getProjectionMatrix());
        savedTransform.set(batch.getTransformMatrix());
        try {
            batch.setShader(null);
            batch.setTransformMatrix(identity);
            batch.enableBlending();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            // Dim the entire window, including the existing FitViewport side margins.
            Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            batch.setProjectionMatrix(screen.setToOrtho2D(0, 0, 1, 1));
            batch.begin();
            batch.setColor(.08f, .07f, .10f, .60f);
            batch.draw(pixel, 0, 0, 1, 1);
            batch.end();
            viewport.apply();
            batch.setProjectionMatrix(viewport.getCamera().combined);
            float w = Math.min(360, viewport.getWorldWidth() - 48);
            float h = Math.min(392, viewport.getWorldHeight() - 120);
            float x = (viewport.getWorldWidth() - w) / 2, y = (viewport.getWorldHeight() - h) / 2;
            batch.begin();
            rect(batch, x + 4, y - 4, w, h, BORDER);
            rect(batch, x, y, w, h, BORDER);
            rect(batch, x + 3, y + 3, w - 6, h - 6, CREAM);
            rect(batch, x + 3, y + h - 7, w - 6, 4, YELLOW);
            centered(batch, "SEMESTER CLEAR!", x + w / 2, y + h - 35, 2f);
            rect(batch, x + 24, y + h - 51, w - 48, 3, YELLOW);
            centered(batch, semesterLine(result), x + w / 2, y + h - 72, 1.1f);
            row(batch, "ITEM SCORE", Integer.toString(result.getItemScore()), x, y + h - 112, w);
            row(batch, "ITEMS", result.getCollectedItemCount() + " / " + totalItems, x, y + h - 138, w);
            row(batch, "DEATHS", Integer.toString(result.getDeathCount()), x, y + h - 164, w);
            row(batch, "SURVIVAL BONUS", "+" + result.getSurvivalBonus(), x, y + h - 190, w);
            rect(batch, x + 24, y + h - 242, w - 48, 32, YELLOW);
            row(batch, "FINAL SCORE", Integer.toString(result.getFinalScore()), x, y + h - 226, w);
            float menuY = y + h - 296;
            rect(batch, x + 24, menuY - 18, w - 48, 36, BORDER);
            rect(batch, x + 26, menuY - 16, w - 52, 32, HIGHLIGHT);
            rect(batch, x + 38, menuY - 4, 4, 8, INK);
            rect(batch, x + 42, menuY - 2, 4, 4, INK);
            centered(batch, "CONTINUE", x + w / 2, menuY, 1.3f);
            centered(batch, "PRESS ENTER TO CONTINUE", x + w / 2, y + 40, .8f);
            centered(batch, nextStepLine(result.getStageNumber()), x + w / 2, y + 20, .7f);
        } finally {
            if (batch.isDrawing()) batch.end();
            viewport.apply();
            batch.setProjectionMatrix(savedProjection);
            batch.setTransformMatrix(savedTransform);
            batch.setShader(shader);
            batch.setPackedColor(color);
            batch.setBlendFunctionSeparate(src, dst, srcAlpha, dstAlpha);
            if (blending) batch.enableBlending(); else batch.disableBlending();
        }
    }

    static String semesterLine(StageResult result) {
        int n = result.getStageNumber();
        return "YEAR " + ((n + 1) / 2) + " / SEMESTER " + ((n - 1) % 2 + 1);
    }
    static String nextStepLine(int stage) {
        VacationEventType vacation = VacationEventType.afterStage(stage);
        if (vacation != null) return "NEXT: " + vacation.getTitle();
        return stage == StageFactory.STAGE_COUNT ? "NEXT: FINAL ACADEMIC REPORT" : "NEXT: SEMESTER " + (stage + 1);
    }
    private void row(SpriteBatch batch, String label, String value, float x, float y, float width) {
        batch.setColor(Color.WHITE);
        font.getData().setScale(1.1f);
        text.setText(font, label);
        font.draw(batch, text, Math.round(x + 36), Math.round(y + text.height / 2));
        text.setText(font, value);
        font.draw(batch, text, Math.round(x + width - 36 - text.width), Math.round(y + text.height / 2));
    }
    private void rect(SpriteBatch b, float x, float y, float w, float h, Color color) {
        b.setColor(color); b.draw(pixel, x, y, w, h);
    }
    private void centered(SpriteBatch b, String value, float x, float y, float scale) {
        b.setColor(Color.WHITE);
        font.getData().setScale(scale);
        text.setText(font, value);
        font.draw(b, text, Math.round(x - text.width / 2), Math.round(y + text.height / 2));
    }
    @Override public void dispose() { font.dispose(); pixel.dispose(); }
}
