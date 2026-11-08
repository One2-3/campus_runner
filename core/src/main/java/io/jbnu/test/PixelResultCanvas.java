package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Shared presentation resources for the two final screens; no game state or score ownership. */
final class PixelResultCanvas implements Disposable {
    static final Color CREAM = new Color(0xF5E8CBFF), BORDER = new Color(0x594738FF);
    static final Color INK = new Color(0x493B32FF), YELLOW = new Color(0xEBCB6EFF);
    static final Color PALE = new Color(0xEFE0BDFF);
    final Texture pixel;
    final BitmapFont english = new BitmapFont();
    final BitmapFont korean = new BitmapFont(Gdx.files.internal("fonts/ending-ko.fnt"));
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 projection = new Matrix4(), transform = new Matrix4(), screen = new Matrix4();
    private ShaderProgram shader;
    private float tint;
    private boolean blending;
    private int src, dst, srcAlpha, dstAlpha;

    PixelResultCanvas() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try { p.setColor(Color.WHITE); p.fill(); pixel = new Texture(p); }
        finally { p.dispose(); }
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (BitmapFont font : new BitmapFont[]{english,korean}) {
            for (TextureRegion region : font.getRegions())
                region.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            font.setUseIntegerPositions(true);
        }
        // The existing Korean atlas declares a zero-advance space. Correct only this UI instance.
        BitmapFont.Glyph space = korean.getData().getGlyph(' ');
        if (space != null && space.xadvance == 0) {
            space.xadvance = 10;
            space.width = 0;
            space.height = 0;
        }
    }

    void begin(SpriteBatch batch, FitViewport viewport) {
        projection.set(batch.getProjectionMatrix()); transform.set(batch.getTransformMatrix());
        shader = batch.getShader(); tint = batch.getPackedColor(); blending = batch.isBlendingEnabled();
        src = batch.getBlendSrcFunc(); dst = batch.getBlendDstFunc();
        srcAlpha = batch.getBlendSrcFuncAlpha(); dstAlpha = batch.getBlendDstFuncAlpha();
        batch.setShader(null); batch.setTransformMatrix(screen.idt());
        batch.enableBlending(); batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glViewport(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        batch.setProjectionMatrix(screen.setToOrtho2D(0,0,1,1));
        batch.begin(); batch.setColor(.08f,.07f,.10f,.60f); batch.draw(pixel,0,0,1,1); batch.end();
        viewport.apply(); batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
    }

    void end(SpriteBatch batch, FitViewport viewport) {
        if (batch.isDrawing()) batch.end();
        viewport.apply(); batch.setProjectionMatrix(projection); batch.setTransformMatrix(transform);
        batch.setShader(shader); batch.setPackedColor(tint);
        batch.setBlendFunctionSeparate(src,dst,srcAlpha,dstAlpha);
        if (blending) batch.enableBlending(); else batch.disableBlending();
    }

    void panel(SpriteBatch b,float x,float y,float w,float h,Color accent) {
        rect(b,x+4,y-4,w,h,BORDER); rect(b,x,y,w,h,BORDER);
        rect(b,x+3,y+3,w-6,h-6,CREAM); rect(b,x+3,y+h-7,w-6,4,accent);
    }
    void rect(SpriteBatch b,float x,float y,float w,float h,Color color) {
        b.setColor(color); b.draw(pixel,x,y,w,h);
    }
    void text(SpriteBatch b,BitmapFont font,String text,float x,float y,float scale,Color color,int alignment) {
        font.setColor(color); font.getData().setScale(scale); layout.setText(font,text);
        float left = alignment == 0 ? x-layout.width/2 : alignment > 0 ? x-layout.width : x;
        b.setColor(Color.WHITE);
        font.draw(b,layout,Math.round(left),Math.round(y+layout.height/2));
    }
    void center(SpriteBatch b,String text,float x,float y,float scale) {
        text(b,english,text,x,y,scale,INK,0);
    }
    @Override public void dispose() { pixel.dispose(); english.dispose(); korean.dispose(); }
}
