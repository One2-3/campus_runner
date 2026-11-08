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

/** Game-over presentation only; all result values come directly from Main. */
public final class GameOverRenderer implements Disposable {
    static final String[] OPTIONS = {"RESTART", "TITLE"};
    private static final Color CREAM = new Color(0xF5E8CBFF);
    private static final Color BORDER = new Color(0x594738FF);
    private static final Color INK = new Color(0x493B32FF);
    private static final Color YELLOW = new Color(0xEBCB6EFF);
    private static final Color PINK = new Color(0xE991A5FF);
    private final Texture pixel;
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout text = new GlyphLayout();
    private final Matrix4 savedProjection = new Matrix4(), savedTransform = new Matrix4();
    private final Matrix4 screen = new Matrix4(), identity = new Matrix4();

    public GameOverRenderer() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try { p.setColor(Color.WHITE); p.fill(); pixel = new Texture(p); }
        finally { p.dispose(); }
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (com.badlogic.gdx.graphics.g2d.TextureRegion region : font.getRegions())
            region.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.setColor(INK);
        font.setUseIntegerPositions(true);
    }

    public void render(SpriteBatch batch, FitViewport viewport, int selected, int year, int semester,
                       int score, int collected, int total, int deaths) {
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
            rect(batch, x + 24, y + h - 50, w - 48, 30, PINK);
            centered(batch, "GAME OVER", x + w / 2, y + h - 35, 2f);
            centered(batch, "ALL LIVES LOST", x + w / 2, y + h - 64, .9f);
            centered(batch, semesterLine(year, semester), x + w / 2, y + h - 96, 1.1f);
            centered(batch, scoreLine(score), x + w / 2, y + h - 120, 1.1f);
            centered(batch, itemsLine(collected, total), x + w / 2, y + h - 144, 1.1f);
            centered(batch, deathsLine(deaths), x + w / 2, y + h - 168, 1.1f);
            for (int i = 0; i < OPTIONS.length; i++) {
                float rowY = y + h - 216 - i * 46;
                if (i == selected) {
                    rect(batch, x + 24, rowY - 18, w - 48, 36, BORDER);
                    rect(batch, x + 26, rowY - 16, w - 52, 32, YELLOW);
                    rect(batch, x + 38, rowY - 4, 4, 8, INK);
                    rect(batch, x + 42, rowY - 2, 4, 4, INK);
                }
                centered(batch, OPTIONS[i], x + w / 2, rowY, 1.3f);
            }
            centered(batch, "RESTART FROM YEAR 1, SEMESTER 1", x + w / 2, y + 66, .8f);
            centered(batch, "UP / DOWN : SELECT", x + w / 2, y + 38, .8f);
            centered(batch, "ENTER : CONFIRM", x + w / 2, y + 20, .8f);
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

    static String semesterLine(int year, int semester) { return "YEAR " + year + " / SEMESTER " + semester; }
    static String scoreLine(int score) { return "SCORE: " + score; }
    static String itemsLine(int collected, int total) { return "ITEMS: " + collected + " / " + total; }
    static String deathsLine(int deaths) { return "SEMESTER DEATHS: " + deaths; }

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
