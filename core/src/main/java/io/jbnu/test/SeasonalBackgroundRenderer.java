package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

/** One campus gate with optional adjoining scenery, all in a shared parallax world. */
public final class SeasonalBackgroundRenderer implements Disposable {
    public static final float PARALLAX = .3f;
    public static final float HEIGHT = 600f;
    public static final float BACKGROUND_OVERLAY_R = 82f / 255f;
    public static final float BACKGROUND_OVERLAY_G = 99f / 255f;
    public static final float BACKGROUND_OVERLAY_B = 123f / 255f;
    public static final float BACKGROUND_OVERLAY_ALPHA = .18f;
    public static final float BACKGROUND_SATURATION = .80f;
    public static final float BACKGROUND_CONTRAST = .90f;
    private static final String[] FILES = {
        "year1_spring_day.png", "year1_spring_night.png",
        "year2_summer_day.png", "year2_summer_night.png",
        "year3_fall_day.png", "year3_fall_night.png",
        "year4_winter_day.png", "year4_winter_night.png"
    };
    private final Texture[] textures = new Texture[FILES.length];
    private final Texture[] left = new Texture[FILES.length], right = new Texture[FILES.length];
    private final Texture solid;
    private final ShaderProgram gradingShader;
    private final Matrix4 projection = new Matrix4();
    private final Rectangle imageBounds = new Rectangle();
    private final Rectangle viewBounds = new Rectangle();
    private static final Color[] SKY = {
        new Color(.08f,.55f,.97f,1),new Color(.01f,.07f,.23f,1),
        new Color(.05f,.55f,.97f,1),new Color(.01f,.06f,.22f,1),
        new Color(.10f,.55f,.94f,1),new Color(.015f,.08f,.22f,1),
        new Color(.10f,.55f,.93f,1),new Color(.015f,.07f,.24f,1)
    };
    private static final Color DAY_GROUND = new Color(.55f,.32f,.44f,1);
    private static final Color NIGHT_GROUND = new Color(.36f,.20f,.30f,1);

    public SeasonalBackgroundRenderer() {
        this(null);
    }

    // Path override allows a missing-file fallback check without modifying supplied assets.
    SeasonalBackgroundRenderer(String[] paths) {
        if (paths != null && paths.length != FILES.length) throw new IllegalArgumentException("Eight background paths required");
        Pixmap pixel = new Pixmap(1,1,Pixmap.Format.RGBA8888);
        try {
            pixel.setColor(Color.WHITE); pixel.fill();
            solid = new Texture(pixel);
            solid.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        } finally { pixel.dispose(); }
        gradingShader = createGradingShader();
        for (int i = 0; i < textures.length; i++) {
            String path = paths == null ? pathForStage(i + 1) : paths[i];
            textures[i] = load(path, false);
            left[i] = load(extensionPathForStage(i+1,true), true);
            right[i] = load(extensionPathForStage(i+1,false), true);
        }
    }

    private ShaderProgram createGradingShader() {
        ShaderProgram shader = null;
        try {
            shader = new ShaderProgram(Gdx.files.internal("shaders/background.vert"),
                Gdx.files.internal("shaders/background.frag"));
            if (!shader.isCompiled()) throw new IllegalStateException(shader.getLog());
            return shader;
        } catch (RuntimeException error) {
            if (shader != null) shader.dispose();
            Gdx.app.error("SeasonalBackgroundRenderer", "Background shader unavailable; using blue-grey overlay only", error);
            return null;
        }
    }

    private Texture load(String path, boolean optional) {
        if (optional && !Gdx.files.internal(path).exists()) return null;
        Texture texture = null;
        try {
            texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            return texture;
        } catch (RuntimeException error) {
            if (texture != null) texture.dispose();
            Gdx.app.error("SeasonalBackgroundRenderer", "Failed to load " + path + "; using sky/ground colors", error);
            return null;
        }
    }

    public static String pathForStage(int stage) {
        if (stage < 1 || stage > FILES.length) throw new IllegalArgumentException("Background stage must be 1..8");
        return "pixel/backgrounds/" + FILES[stage - 1];
    }

    public static String extensionPathForStage(int stage, boolean isLeft) {
        String path = pathForStage(stage);
        return path.substring(0,path.length()-4)+(isLeft ? "_left.png" : "_right.png");
    }

    static void layout(int imageWidth, int imageHeight, float worldWidth, float cameraX,
                       int screenWidth, int screenHeight, Rectangle gate, Rectangle view) {
        float width = imageWidth * (HEIGHT / imageHeight);
        gate.set((worldWidth-width)/2,0,width,HEIGHT);
        float viewWidth = HEIGHT * screenWidth / screenHeight;
        float center = worldWidth/2 + (cameraX-worldWidth/2)*PARALLAX;
        view.set(center-viewWidth/2,0,viewWidth,HEIGHT);
    }

    public void render(SpriteBatch batch, int stage, float worldWidth, float cameraX, int screenWidth, int screenHeight) {
        if (stage < 1 || stage > textures.length || screenWidth <= 0 || screenHeight <= 0) return;
        Texture texture = textures[stage - 1];
        layout(texture == null ? 2048 : texture.getWidth(),texture == null ? 768 : texture.getHeight(),
            worldWidth,cameraX,screenWidth,screenHeight,imageBounds,viewBounds);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        batch.setProjectionMatrix(projection.setToOrtho2D(viewBounds.x,0,viewBounds.width,HEIGHT));
        float color = batch.getPackedColor();
        boolean blending = batch.isBlendingEnabled();
        int src = batch.getBlendSrcFunc(), dst = batch.getBlendDstFunc();
        int srcAlpha = batch.getBlendSrcFuncAlpha(), dstAlpha = batch.getBlendDstFuncAlpha();
        ShaderProgram previousShader = batch.getShader();
        batch.setShader(gradingShader);
        batch.begin();
        try {
            if (gradingShader != null) {
                gradingShader.setUniformf("u_saturation",BACKGROUND_SATURATION);
                gradingShader.setUniformf("u_contrast",BACKGROUND_CONTRAST);
                gradingShader.setUniformf("u_overlay",BACKGROUND_OVERLAY_R,BACKGROUND_OVERLAY_G,
                    BACKGROUND_OVERLAY_B,BACKGROUND_OVERLAY_ALPHA);
            }
            batch.enableBlending();
            batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_ONE,GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(SKY[stage-1]);
            batch.draw(solid,viewBounds.x,0,viewBounds.width,HEIGHT);
            batch.setColor(stage%2 == 1 ? DAY_GROUND : NIGHT_GROUND);
            batch.draw(solid,viewBounds.x,0,viewBounds.width,120);
            batch.setColor(Color.WHITE);
            Texture l=left[stage-1],r=right[stage-1];
            if (l != null) {
                float width=l.getWidth()*(HEIGHT/l.getHeight());
                batch.draw(l,imageBounds.x-width,0,width,HEIGHT);
            }
            if (r != null) {
                float width=r.getWidth()*(HEIGHT/r.getHeight());
                batch.draw(r,imageBounds.x+imageBounds.width,0,width,HEIGHT);
            }
            // Exactly one draw of the landmark; never tile or stretch its edge pixels.
            if (texture != null) batch.draw(texture,imageBounds.x,imageBounds.y,imageBounds.width,imageBounds.height);
            // The shader grades every background piece uniformly, including color
            // fills and optional extensions. No world or HUD draw uses this shader.
            if (gradingShader == null) {
                batch.setColor(BACKGROUND_OVERLAY_R,BACKGROUND_OVERLAY_G,BACKGROUND_OVERLAY_B,BACKGROUND_OVERLAY_ALPHA);
                batch.draw(solid,viewBounds.x,0,viewBounds.width,HEIGHT);
            }
        } finally {
            batch.end();
            batch.setShader(previousShader);
            batch.setPackedColor(color);
            batch.setBlendFunctionSeparate(src,dst,srcAlpha,dstAlpha);
            if (blending) batch.enableBlending(); else batch.disableBlending();
        }
    }

    /** Called in the existing fixed HUD projection; no full-width white margins. */
    public void renderHudBackdrop(SpriteBatch batch) {
        float color=batch.getPackedColor();
        batch.setColor(.78f,.86f,.94f,.82f);
        batch.draw(solid,10,455,255,135);
        batch.draw(solid,510,520,250,70);
        batch.setPackedColor(color);
    }

    Texture getTexture(int stage) { return textures[stage - 1]; }
    Texture getExtension(int stage, boolean isLeft) { return (isLeft ? left : right)[stage-1]; }
    Texture getSolidTexture() { return solid; }
    ShaderProgram getGradingShader() { return gradingShader; }

    @Override public void dispose() {
        for (Texture[] group : new Texture[][] {textures,left,right}) {
            for (int i = 0; i < group.length; i++) if (group[i] != null) {
                group[i].dispose(); group[i]=null;
            }
        }
        solid.dispose();
        if (gradingShader != null) gradingShader.dispose();
    }
}
