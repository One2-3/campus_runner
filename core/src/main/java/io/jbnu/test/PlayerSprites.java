package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.util.EnumMap;

/** Shared, render-only player art. Crop transparent margins without changing any collision geometry. */
public final class PlayerSprites implements Disposable {
    // One scale preserves head/body proportions across poses. Idle's visible 394px height becomes 44 world units.
    public static final float SCALE = 44f / 394f;
    public enum Pose {
        IDLE("idle_01.png",86,56,276,394),
        RUN("run_02.png",87,56,274,390),
        JUMP("jump_01.png",72,56,304,360),
        HURT("hurt_01.png",46,48,356,422);

        private final String fileName;
        private final int x, y, width, height;
        Pose(String fileName, int x, int y, int width, int height) {
            this.fileName = fileName; this.x = x; this.y = y; this.width = width; this.height = height;
        }
        public String getPath() { return "pixel/characters/player/" + fileName; }
        public int getSourceX() { return x; }
        public int getSourceY() { return y; }
        public int getSourceWidth() { return width; }
        public int getSourceHeight() { return height; }
        public float getDrawWidth() { return width*SCALE; }
        public float getDrawHeight() { return height*SCALE; }
    }

    private final EnumMap<Pose, Texture> textures = new EnumMap<>(Pose.class);
    private final EnumMap<Pose, TextureRegion> right = new EnumMap<>(Pose.class);
    private final EnumMap<Pose, TextureRegion> left = new EnumMap<>(Pose.class);

    public PlayerSprites() {
        for (Pose pose : Pose.values()) {
            try {
                Texture texture = new Texture(Gdx.files.internal(pose.getPath()));
                textures.put(pose,texture);
                if (texture.getWidth() != 448 || texture.getHeight() != 512)
                    throw new GdxRuntimeException("Expected 448x512 canvas, got " + texture.getWidth() + "x" + texture.getHeight());
                texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
                TextureRegion region = new TextureRegion(texture,pose.x,pose.y,pose.width,pose.height);
                right.put(pose,region);
                TextureRegion flipped = new TextureRegion(region);
                flipped.flip(true,false);
                left.put(pose,flipped);
            } catch (RuntimeException error) {
                dispose();
                throw new GdxRuntimeException("Failed to load player sprite: " + pose.getPath(),error);
            }
        }
    }

    public static Pose selectPose(Player player, boolean hurt) {
        if (hurt) return Pose.HURT;
        if (!player.isGrounded()) return Pose.JUMP;
        return player.isMovingHorizontally() ? Pose.RUN : Pose.IDLE;
    }

    public void render(SpriteBatch batch, Player player, boolean hurt) {
        Pose pose = selectPose(player,hurt);
        // Every source crop is centered at canvas x=224 and ends exactly at its pose's feet.
        batch.draw(getFrame(pose,player.isFacingRight()),player.getCenterX()-pose.getDrawWidth()/2f,
            player.getY(),pose.getDrawWidth(),pose.getDrawHeight());
    }

    TextureRegion getFrame(Pose pose, boolean facingRight) { return (facingRight ? right : left).get(pose); }

    @Override public void dispose() {
        for (Texture texture : textures.values()) texture.dispose();
        textures.clear(); right.clear(); left.clear();
    }
}
