package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/** UI-coordinate decoration only. All values belong to Main; the item texture is borrowed. */
public final class PixelHudRenderer implements Disposable {
    private static final Color CREAM = new Color(0xF5E8CBFF);
    private static final Color BORDER = new Color(0x594738FF);
    private static final Color INK = new Color(0x493B32FF);
    private static final Color PINK = new Color(0xE991A5FF);
    private static final Color YELLOW = new Color(0xEBCB6EFF);
    private static final Color GREEN = new Color(0xB7CC91FF);
    private static final String[] STAR = {"000010000","000111000","111111111","011111110","001111100","011111110","011000110"};
    private static final String[] HEART = {"0110110","1111111","1111111","0111110","0011100","0001000"};
    private final Texture pixel;
    private final Texture itemTexture;
    private final BitmapFont font;
    public static final float HUD_SCALE = .85f;
    private final GlyphLayout textLayout = new GlyphLayout();
    private final Matrix4 savedTransform = new Matrix4();
    private final Matrix4 hudTransform = new Matrix4();

    public PixelHudRenderer(Texture itemTexture) {
        this.itemTexture = itemTexture;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font = new BitmapFont();
        for (com.badlogic.gdx.graphics.g2d.TextureRegion region : font.getRegions())
            region.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.setUseIntegerPositions(true);
        font.setColor(INK);
    }

    static String semesterText(int year, int semester) {
        return "YEAR " + year + " / SEMESTER " + semester;
    }

    static String scoreText(int score) { return Integer.toString(score); }
    static String itemsText(int collected, int total) { return collected + " / " + total; }

    /** Fit a uniformly scaled HUD group inside the available UI area, centered at the top. */
    static final class Layout {
        static final float YEAR_WIDTH = 300, SCORE_WIDTH = 192, ITEMS_WIDTH = 264;
        static final float GAP = 8, MAIN_HEIGHT = 50, BASE_WIDTH = 772;
        final float left, top, scale, rowWidth, scoreX, itemsX;
        Layout(float width, float height) {
            float margin = Math.max(14f, Math.min(width, height) * .03f);
            scale = Math.min(HUD_SCALE, Math.min((width - 2 * margin) / BASE_WIDTH,
                (height - 2 * margin) / 100f));
            rowWidth = BASE_WIDTH * scale;
            left = (width - rowWidth) / 2;
            top = height - margin;
            scoreX = left + (YEAR_WIDTH + GAP) * scale;
            itemsX = left + (YEAR_WIDTH + SCORE_WIDTH + 2 * GAP) * scale;
        }
        float heartsX(int maxLives) {
            return 80 + Math.max(0, Integer.toString(maxLives).length() - 1) * 14;
        }
        int heartsPerRow(int maxLives) {
            return Math.max(1, (int)((ITEMS_WIDTH - heartsX(maxLives) - 10) / 18));
        }
        float livesHeight(int maxLives) {
            return 8 + 16 * Math.max(1, (int)Math.ceil((float)maxLives / heartsPerRow(maxLives)));
        }
    }
    /** Called with the UI projection and an idle batch. Restore all changed batch state. */
    public void render(SpriteBatch batch, float width, float height, int year, int semester,
                       int score, int collected, int total, int lives, int maxLives) {
        float packed = batch.getPackedColor();
        ShaderProgram shader = batch.getShader();
        boolean blend = batch.isBlendingEnabled();
        int src = batch.getBlendSrcFunc(), dst = batch.getBlendDstFunc();
        int srcAlpha = batch.getBlendSrcFuncAlpha(), dstAlpha = batch.getBlendDstFuncAlpha();
        Layout l = new Layout(width, height);
        try {
            batch.setShader(null);
            batch.enableBlending();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            savedTransform.set(batch.getTransformMatrix());
            hudTransform.set(savedTransform).translate(l.left, l.top, 0).scale(l.scale, l.scale, 1);
            batch.setTransformMatrix(hudTransform);
            batch.begin();
            float scoreX = Layout.YEAR_WIDTH + Layout.GAP;
            float itemsX = scoreX + Layout.SCORE_WIDTH + Layout.GAP;
            panel(batch, 0, -Layout.MAIN_HEIGHT, Layout.YEAR_WIDTH, Layout.MAIN_HEIGHT, GREEN);
            panel(batch, scoreX, -Layout.MAIN_HEIGHT, Layout.SCORE_WIDTH, Layout.MAIN_HEIGHT, YELLOW);
            panel(batch, itemsX, -Layout.MAIN_HEIGHT, Layout.ITEMS_WIDTH, Layout.MAIN_HEIGHT, PINK);
            centeredText(batch, "CAMPUS / ACADEMIC YEAR", 12, -12, .75f);
            centeredText(batch, semesterText(year, semester), 12, -34, 1.15f);
            centeredText(batch, "SCORE", scoreX + 39, -12, .85f);
            centeredText(batch, scoreText(score), scoreX + 39, -34, 1.3f);
            bitmap(batch, STAR, scoreX + 11, -41, 2f, YELLOW);
            centeredText(batch, "ITEMS", itemsX + 46, -12, .85f);
            centeredText(batch, itemsText(collected, total), itemsX + 46, -34, 1.3f);
            batch.setColor(Color.WHITE);
            batch.draw(itemTexture, itemsX + 5, -54.5f, 38, 38);
            float livesTop = -Layout.MAIN_HEIGHT - Layout.GAP;
            panel(batch, itemsX, livesTop - l.livesHeight(maxLives), Layout.ITEMS_WIDTH,
                l.livesHeight(maxLives), PINK);
            centeredText(batch, "LIVES " + lives + "/" + maxLives, itemsX + 10, livesTop - 12, .8f);
            int columns = l.heartsPerRow(maxLives);
            for (int i = 0; i < maxLives; i++) {
                float x = itemsX + l.heartsX(maxLives) + (i % columns) * 18;
                float y = livesTop - 18 - (i / columns) * 16;
                bitmap(batch, HEART, x, y, 2, i < lives ? PINK : BORDER);
                if (i < lives) rect(batch, x + 2, y + 8, 2, 2, CREAM);
            }
        } finally {
            if (batch.isDrawing()) batch.end();
            batch.setTransformMatrix(savedTransform);
            batch.setShader(shader);
            batch.setPackedColor(packed);
            batch.setBlendFunctionSeparate(src, dst, srcAlpha, dstAlpha);
            if (blend) batch.enableBlending(); else batch.disableBlending();
        }
    }

    private void panel(SpriteBatch b, float x, float y, float w, float h, Color accent) {
        rect(b, x + 2, y - 2, w, h, BORDER);
        rect(b, x, y, w, h, BORDER);
        rect(b, x + 2, y + 2, w - 4, h - 4, CREAM);
        rect(b, x + 2, y + h - 4, w - 4, 2, accent);
    }
    private void centeredText(SpriteBatch b, String value, float x, float centerY, float scale) {
        b.setColor(Color.WHITE);
        font.getData().setScale(scale);
        textLayout.setText(font, value);
        font.draw(b, textLayout, Math.round(x), Math.round(centerY + textLayout.height / 2));
    }
    private void bitmap(SpriteBatch b, String[] mask, float x, float y, float scale, Color color) {
        for (int row = 0; row < mask.length; row++)
            for (int col = 0; col < mask[row].length(); col++)
                if (mask[row].charAt(col) == '1')
                    rect(b, x + col * scale, y + (mask.length - 1 - row) * scale, scale, scale, color);
    }
    private void rect(SpriteBatch b, float x, float y, float w, float h, Color color) {
        b.setColor(color);
        b.draw(pixel, x, y, w, h);
    }
    @Override public void dispose() {
        font.dispose();
        pixel.dispose();
        // itemTexture is owned and disposed by Main.
    }
}
