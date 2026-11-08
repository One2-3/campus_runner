package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/** Contact fixtures execute the real collision -> DYING -> delayed death path; no counter/timer injection. */
public final class HitFeedbackVerification {
    private static boolean allHeld, left, right;
    private static float dt;
    private static final Method UPDATE = method("update",float.class), LOAD = method("loadStage",int.class);
    private static final Method PROJECTION = method("getWorldRenderProjection");

    public static void main(String[] args) throws Exception {
        verifyClock();
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments) -> {
                    int key = arguments == null ? -1 : (Integer)arguments[0];
                    if (method.getName().equals("isKeyPressed")) return allHeld || key == Input.Keys.LEFT && left || key == Input.Keys.RIGHT && right;
                    if (method.getName().equals("isKeyJustPressed")) return allHeld;
                    throw new UnsupportedOperationException(method.getName());
                });
            for (int fps : new int[]{30,60,120}) {
                dt = 1f/fps;
                for (int cause = 0; cause < 4; cause++) verifyContact(cause,fps);
                for (int cause = 0; cause < 3; cause++) verifyTwoAndLastLife(cause,fps);
                verifyBonusLivesAndReset(fps);
                verifyFall();
                System.out.printf("%d fps: hit feedback passed: enemy/fixed/timed/simultaneous contact, 1s frozen hurt, "
                    + ".25s render-only <=5 shake, unchanged camera, exact one charge, input release, "
                    + "two-life delayed retry / last-life immediate GAME_OVER for all hazards, six-life bonus/reset and immediate falls.%n",fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyClock() {
        HitFeedback effect = new HitFeedback();
        check(effect.start() && !effect.start(),"Cannot restart active timer");
        check(effect.getShakeX() == 5 && effect.getShakeY() == 0,"Initial five-unit shake");
        check(!effect.advance(.125f),"Half shake interval is not death");
        near((float)Math.hypot(effect.getShakeX(),effect.getShakeY()),2.5f,.001f,"Linear shake decay");
        check(!effect.advance(.125f) && effect.getShakeX() == 0 && effect.getShakeY() == 0,"Exactly .25 seconds restores render offset");
        check(!effect.advance(.5f) && !effect.advance(0) && effect.isActive(),"Before one second remains active");
        check(effect.advance(.25f) && !effect.advance(1),"Exactly one timer completion");
        effect.reset(); check(!effect.isActive() && effect.getElapsed() == 0,"Reset removes all feedback");
    }

    private static void verifyContact(int cause, int fps) throws Exception {
        Main game = newGame(3);
        collectFirst(game);
        installContact(game,cause);
        OrthographicCamera camera = (OrthographicCamera)get(game,"camera");
        camera.position.set(600,300,0);
        float[] baseMatrix = camera.combined.val.clone();
        left = true;
        player(game).reset(200,60); tick(game); left = false;
        check(state(game) == GameState.DYING && number(game,"lives") == 5 && number(game,"semesterDeathCount") == 0
            && number(game,"totalDeathCount") == 0,"Contact freezes before any life/death charge");
        Player failed = player(game);
        Rectangle bounds = failed.getBounds();
        check(!failed.isFacingRight() && bounds.width == 32 && bounds.height == 44,"Contact preserves facing and collision geometry");
        HitFeedback effect = effect(game);
        check(effect.isActive() && effect.getElapsed() == 0,"Timer starts once at contact, not one delta early");
        // Hazards may overlap an item and flag during the frozen state; neither may be processed.
        GradeItem blockedItem = new GradeItem(bounds.x,bounds.y,24,24);
        items(game).add(blockedItem);
        ((Rectangle)get(game,"flag")).set(bounds);
        String frozen = world(game);
        allHeld = true;
        float previousAmplitude = 5;
        for (int frame = 1; frame < fps; frame++) {
            tick(game);
            check(state(game) == GameState.DYING && player(game) == failed && world(game).equals(frozen)
                && !blockedItem.isCollected(),"All gameplay/ESC/Enter/R input, world updates, pickups and clear ignored before 1s");
            check(PlayerSprites.selectPose(failed,true) == PlayerSprites.Pose.HURT && !failed.isFacingRight(),"Frozen left-facing hurt pose");
            float amplitude = (float)Math.hypot(effect.getShakeX(),effect.getShakeY());
            check(amplitude <= previousAmplitude+.0001f && amplitude <= 5.0001f,"Shake decreases without exceeding five units");
            previousAmplitude = amplitude;
            if (frame*dt >= .25) check(amplitude == 0,"Shake ends at .25 seconds");
            Matrix4 projection = (Matrix4)PROJECTION.invoke(game);
            near(projection.val[Matrix4.M03],-effect.getShakeX(),.0001f,"Render projection X offset only");
            near(projection.val[Matrix4.M13],-effect.getShakeY(),.0001f,"Render projection Y offset only");
            check(camera.position.x == 600 && camera.position.y == 300 && Arrays.equals(camera.combined.val,baseMatrix),
                "Projection shake never changes actual tracking camera or base matrix");
        }
        tick(game);
        check(state(game) == GameState.PLAYING && player(game) != failed && number(game,"currentStageNumber") == 3
            && number(game,"lives") == 4 && number(game,"semesterDeathCount") == 1 && number(game,"totalDeathCount") == 1,
            "One second completes exactly one common death and same-stage retry");
        check(number(game,"stageScore") == 0 && number(game,"collectedItemCount") == 0 && items(game).size == 6,
            "Current semester items and item score restored");
        for (GradeItem item : items(game)) check(!item.isCollected(),"Fresh uncollected items");
        check(effect.getShakeX() == 0 && effect.getShakeY() == 0,"No residual offset after restart");
        Player restarted = player(game);
        Rectangle spawn = restarted.getBounds();
        for (int i = 0; i < 10; i++) tick(game);
        check(player(game) == restarted && restarted.getBounds().equals(spawn) && number(game,"lives") == 4
            && state(game) == GameState.PLAYING,"Keys held through death cannot skip, jump, move, pause, or reset the respawn");
        allHeld = false; tick(game);
        right = true; tick(game); right = false;
        check(restarted.getCenterX() > spawn.x+16,"Fresh input after release resumes normal movement");
    }

    private static void verifyBonusLivesAndReset(int fps) throws Exception {
        Main game = newGame(2);
        collectFirst(game);
        Rectangle flag = (Rectangle)get(game,"flag"); player(game).reset(flag.x,flag.y); tick(game);
        VacationVerificationSupport.finishIfPending(game,true);
        check(number(game,"lives") == 6,"Cafe success starts with six lives");
        StageResult prior = ((StageResult[])get(game,"completedResults"))[1];
        PlayerModifier modifier = player(game).getModifier();
        for (int death = 1; death <= 6; death++) {
            installContact(game,death%3);
            player(game).reset(200,60); tick(game);
            if (death < 6) {
                check(state(game) == GameState.DYING && number(game,"lives") == 7-death,"Nonfinal life keeps full hurt interval");
                for (int i = 0; i < fps; i++) tick(game);
            } else check(state(game) == GameState.GAME_OVER && !effect(game).isActive() && effect(game).getElapsed() == 0,
                "Last bonus life skips DYING and clears feedback immediately");
            check(number(game,"lives") == 6-death && number(game,"semesterDeathCount") == death
                && number(game,"totalDeathCount") == death && ((StageResult[])get(game,"completedResults"))[1] == prior
                && player(game).getModifier() == modifier,"Delayed death retains prior result, vacation effect and exact counters");
            check(state(game) == (death == 6 ? GameState.GAME_OVER : GameState.PLAYING),"Retry or GAME_OVER depends only on remaining lives");
        }
        Player failed = player(game);
        for (int i = 0; i < fps; i++) tick(game);
        check(player(game) == failed && number(game,"lives") == 0 && number(game,"totalDeathCount") == 6
            && PlayerSprites.selectPose(failed,true) == PlayerSprites.Pose.HURT,"GAME_OVER retains hurt without repeated charges");
        allHeld = true; tick(game); allHeld = false;
        check(state(game) == GameState.PLAYING && number(game,"currentStageNumber") == 1 && number(game,"lives") == 5
            && number(game,"totalDeathCount") == 0 && player(game).getModifier() == PlayerModifier.NONE
            && !effect(game).isActive() && effect(game).getShakeX() == 0,"GAME_OVER Enter still performs complete Stage 1 reset");
        for (StageResult result : (StageResult[])get(game,"completedResults")) check(result == null,"Previous records cleared only on new game");
    }

    private static void verifyTwoAndLastLife(int cause, int fps) throws Exception {
        Main game = newGame(3);
        for (int i = 0; i < 3; i++) { player(game).reset(80,-150); tick(game); }
        check(number(game,"lives") == 2,"Two lives reached by actual deaths");
        installContact(game,cause);
        player(game).reset(200,60); tick(game);
        check(state(game) == GameState.DYING && number(game,"lives") == 2 && number(game,"totalDeathCount") == 3,
            "Two-life contact starts hurt without early charge for cause "+cause);
        for (int i = 1; i < fps; i++) {
            tick(game);
            check(state(game) == GameState.DYING && number(game,"lives") == 2,"Two-life contact waits full second");
        }
        tick(game);
        check(state(game) == GameState.PLAYING && number(game,"lives") == 1 && number(game,"totalDeathCount") == 4,
            "Two-life contact retries after exactly one second");
        installContact(game,cause);
        player(game).reset(200,60);
        Player failed = player(game);
        tick(game);
        check(state(game) == GameState.GAME_OVER && player(game) == failed && number(game,"lives") == 0
            && number(game,"semesterDeathCount") == 5 && number(game,"totalDeathCount") == 5,
            "Last-life contact immediately invokes common death exactly once for cause "+cause);
        check(!effect(game).isActive() && effect(game).getElapsed() == 0
            && effect(game).getShakeX() == 0 && effect(game).getShakeY() == 0,
            "Last-life contact never starts DYING timer or shake");
        for (int i = 0; i < fps*2; i++) tick(game);
        check(state(game) == GameState.GAME_OVER && number(game,"totalDeathCount") == 5 && number(game,"lives") == 0,
            "Immediate GAME_OVER cannot charge another life or counter");
        allHeld = true; tick(game); allHeld = false;
        check(state(game) == GameState.PLAYING && number(game,"currentStageNumber") == 1 && number(game,"lives") == 5
            && number(game,"totalDeathCount") == 0,"Immediate GAME_OVER Enter retains full new-game reset");
    }

    private static void verifyFall() throws Exception {
        Main game = newGame(3); Player failed = player(game);
        failed.reset(80,-150); tick(game);
        check(state(game) == GameState.PLAYING && player(game) != failed && number(game,"lives") == 4
            && number(game,"totalDeathCount") == 1 && !effect(game).isActive() && effect(game).getElapsed() == 0,
            "Offscreen fall remains immediate, without hit delay or shake");
    }
    @SuppressWarnings("unchecked") private static void installContact(Main game, int cause) throws Exception {
        Array<TimedTrap> traps = (Array<TimedTrap>)get(game,"timedTraps"); traps.clear();
        Array<PatrollingEnemy> enemies = (Array<PatrollingEnemy>)get(game,"enemies"); enemies.clear();
        if (cause == 0 || cause == 3) enemies.add(new PatrollingEnemy(195,60,40,44,180,260,80,true));
        if (cause == 1 || cause == 3) traps.add(TimedTrap.fixedSpikes(190,60,64,44));
        if (cause == 2) traps.add(TimedTrap.cyclingSpikes(190,60,64,44,.1,.1,2,.3));
    }
    private static void collectFirst(Main game) throws Exception { Rectangle item = items(game).first().getBounds(); player(game).reset(item.x,item.y); tick(game); }
    private static String world(Main game) throws Exception {
        StringBuilder s = new StringBuilder().append(player(game).getBounds()).append(player(game).isFacingRight());
        for (Object p : (Array<?>)get(game,"movingPlatforms")) s.append(((MovingPlatform)p).getBounds());
        for (Object e : (Array<?>)get(game,"enemies")) s.append(((PatrollingEnemy)e).getBounds()).append(((PatrollingEnemy)e).isMovingRight());
        for (Object t : (Array<?>)get(game,"timedTraps")) s.append(((TimedTrap)t).getState()).append(((TimedTrap)t).getStateElapsedTime());
        for (String name : new String[]{"lives","stageScore","collectedItemCount","semesterDeathCount","totalDeathCount"}) s.append('/').append(get(game,name));
        return s.toString();
    }
    private static Main newGame(int stage) throws Exception {
        allHeld = left = right = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() { @Override public void update() { } @Override public void update(boolean f) { } };
        field("camera").set(game,camera); field("viewport").set(game,new FitViewport(800,600,camera));
        LOAD.invoke(game,stage); return game;
    }
    private static void tick(Main game) throws Exception { UPDATE.invoke(game,dt); }
    private static HitFeedback effect(Main game) throws Exception { return (HitFeedback)get(game,"hitFeedback"); }
    private static Player player(Main game) throws Exception { return (Player)get(game,"player"); }
    private static GameState state(Main game) throws Exception { return (GameState)get(game,"gameState"); }
    @SuppressWarnings("unchecked") private static Array<GradeItem> items(Main game) throws Exception { return (Array<GradeItem>)get(game,"gradeItems"); }
    private static int number(Main game, String name) throws Exception { return (Integer)get(game,name); }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static Field field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?>... parameters) { try { Method m = Main.class.getDeclaredMethod(name,parameters); m.setAccessible(true); return m; } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void near(float actual, float expected, float tolerance, String message) { check(Math.abs(actual-expected) < tolerance,message+": "+actual+" / "+expected); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
