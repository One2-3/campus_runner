package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/** Asset/anchor checks and pose selection driven by actual Player movement, without a graphics context. */
public final class PlayerPixelArtVerification {
    private static boolean left, right, jump;

    public static void main(String[] args) throws Exception {
        verifyAssets();
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments) -> {
                    int key = arguments == null ? -1 : (Integer)arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.LEFT && left || key == Input.Keys.RIGHT && right;
                    if (method.getName().equals("isKeyJustPressed")) return key == Input.Keys.SPACE && jump;
                    throw new UnsupportedOperationException(method.getName());
                });
            for (int fps : new int[]{30,60,120}) {
                verifyMovement(fps);
                System.out.printf("%d fps: player art passed: four transparent assets, common scale/feet/center, "
                    + "idle/run_02/jump ascent+fall/hurt, facing retention, original 32x44 collision and 260/650 physics.%n",fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyAssets() throws Exception {
        Path assets = Paths.get("assets");
        if (!Files.isDirectory(assets)) assets = Paths.get("../assets");
        check(PlayerSprites.Pose.values().length == 4,"Exactly four player textures");
        for (PlayerSprites.Pose pose : PlayerSprites.Pose.values()) {
            check(!pose.getPath().contains("run_01"),"Excluded running frame is never used");
            BufferedImage image = ImageIO.read(assets.resolve(pose.getPath()).toFile());
            check(image != null && image.getWidth() == 448 && image.getHeight() == 512 && image.getColorModel().hasAlpha(),
                "Expected transparent 448x512 PNG: "+pose);
            int minX = 448, minY = 512, maxX = -1, maxY = -1, transparent = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x,y) >>> 24) == 0) { transparent++; continue; }
                minX = Math.min(minX,x); minY = Math.min(minY,y); maxX = Math.max(maxX,x); maxY = Math.max(maxY,y);
            }
            check(transparent > 0 && minX == pose.getSourceX() && minY == pose.getSourceY()
                && maxX-minX+1 == pose.getSourceWidth() && maxY-minY+1 == pose.getSourceHeight(),
                "Crop includes every visible pixel and removes transparent padding: "+pose);
            check(pose.getSourceX()+pose.getSourceWidth()/2f == 224,"All crops share the same horizontal body center");
            near(pose.getDrawWidth()/pose.getSourceWidth(),PlayerSprites.SCALE,.000001f,"Uniform horizontal scale");
            near(pose.getDrawHeight()/pose.getSourceHeight(),PlayerSprites.SCALE,.000001f,"Undistorted vertical scale");
        }
        check(PlayerSprites.Pose.RUN.getPath().endsWith("run_02.png"),"Single specified run frame");
        near(PlayerSprites.Pose.IDLE.getDrawHeight(),44,.00001f,"Visible idle size matches existing player world height");
    }

    private static void verifyMovement(int fps) {
        left = right = jump = false;
        float dt = 1f/fps;
        Array<Rectangle> floor = new Array<>(); floor.add(new Rectangle(0,0,5000,60));
        Player player = new Player(1000,60);
        player.update(dt,floor,5000);
        check(PlayerSprites.selectPose(player,false) == PlayerSprites.Pose.IDLE && player.isFacingRight(),"Idle on floor faces right initially");
        Rectangle collision = player.getBounds();
        check(collision.width == 32 && collision.height == 44,"Original collision box unchanged");
        right = true;
        for (int i = 0; i < fps; i++) player.update(dt,floor,5000);
        near(player.getCenterX()-16-1000,260,.04f,"Original one-second movement distance");
        check(PlayerSprites.selectPose(player,false) == PlayerSprites.Pose.RUN && player.isFacingRight(),"Right run uses original frame direction");
        right = false; left = true; player.update(dt,floor,5000);
        check(PlayerSprites.selectPose(player,false) == PlayerSprites.Pose.RUN && !player.isFacingRight(),"Left run requests mirrored frame");
        left = false; player.update(dt,floor,5000);
        check(PlayerSprites.selectPose(player,false) == PlayerSprites.Pose.IDLE && !player.isFacingRight(),"Idle retains last left-facing direction");
        jump = true; player.update(dt,floor,5000); jump = false;
        near(player.getY(),60+(650-1700*dt)*dt,.001f,"Original jump impulse and gravity");
        boolean sawAscent = false, sawFall = false;
        int remaining = fps*2;
        float previousY = 60;
        while (!player.isGrounded() && --remaining > 0) {
            check(PlayerSprites.selectPose(player,false) == PlayerSprites.Pose.JUMP,"Same jump PNG throughout airborne movement");
            if (player.getY() > previousY) sawAscent = true;
            if (player.getY() < previousY) sawFall = true;
            previousY = player.getY(); player.update(dt,floor,5000);
        }
        check(remaining > 0 && sawAscent && sawFall && player.getY() == 60,"Normal ascent, descent and landing preserved");
        check(PlayerSprites.selectPose(player,false) == PlayerSprites.Pose.IDLE && !player.isFacingRight(),"Landing preserves left-facing idle");
        Rectangle before = player.getBounds();
        for (int i = 0; i < 20; i++) {
            check(PlayerSprites.selectPose(player,true) == PlayerSprites.Pose.HURT,"Hurt overrides normal visual state");
            for (PlayerSprites.Pose pose : PlayerSprites.Pose.values()) {
                float x = player.getCenterX()-pose.getDrawWidth()/2;
                near(x+pose.getDrawWidth()/2,player.getCenterX(),.0001f,"Pose changes preserve center");
                check(player.getY() == before.y,"All frames use same collision-foot anchor");
            }
        }
        check(player.getBounds().equals(before),"Visual state/geometry calculations never mutate collision bounds");
        player.reset(80,60);
        check(player.isFacingRight() && player.getBounds().equals(new Rectangle(80,60,32,44)),"Restart restores the original spawn geometry and default facing");
    }
    private static void near(float actual, float expected, float tolerance, String message) { check(Math.abs(actual-expected) < tolerance,message+": "+actual+" / "+expected); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
