package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Initial title state has no world at all; Enter initializes the normal game exactly once. */
public final class TitleScreenVerification {
    private static boolean enter, enterHeld, otherKeys;
    private static final Method UPDATE = method("update",float.class);
    private static final Method LOAD = method("loadStage",int.class);

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments) -> {
                    int key = arguments == null ? -1 : (Integer)arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.ENTER ? enterHeld : otherKeys;
                    if (method.getName().equals("isKeyJustPressed")) return key == Input.Keys.ENTER ? enter : otherKeys;
                    throw new UnsupportedOperationException(method.getName());
                });
            for (int fps : new int[]{30,60,120}) {
                verifyTitle(fps);
                System.out.printf("%d fps: title passed: no world before Enter, other keys ignored, clean Stage 1 start, "
                    + "held/repeated Enter cannot initialize twice, restart bypasses title.%n",fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyTitle(int fps) throws Exception {
        enter = enterHeld = false;
        otherKeys = true;
        float dt = 1f/fps;
        Main game = new Main();
        check(get(game,"gameState") == GameState.TITLE_SCREEN, "Initial state is TITLE_SCREEN");
        for (int i = 0; i < fps*3; i++) tick(game,dt);
        check(get(game,"gameState") == GameState.TITLE_SCREEN, "Arrows, Space and ESC cannot dismiss title");
        for (String name : new String[]{"stage","player","movingPlatforms","timedTraps","enemies","gradeItems"}) {
            check(get(game,name) == null, "No world construction or updates while waiting: " + name);
        }
        check(number(game,"totalDeathCount") == 0 && number(game,"semesterDeathCount") == 0, "No title deaths");
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field("camera").set(game,camera);
        field("viewport").set(game,new FitViewport(800,600,camera));
        enter = enterHeld = true;
        tick(game,dt);
        assertNewGame(game);
        StageDefinition initial = StageFactory.createStage(1);
        Player player = (Player)get(game,"player");
        check(player.getCenterX()-16 == initial.getStartX() && player.getY() == initial.getStartY(),
            "Simultaneous Enter/arrows/Space/ESC leave player at exact initial spawn");
        otherKeys = false;
        Object items = get(game,"gradeItems"), platforms = get(game,"movingPlatforms");
        for (int i = 0; i < fps*2; i++) {
            tick(game,dt); // Enter remains held, but no fresh key edge.
            check(get(game,"player") == player && get(game,"gradeItems") == items
                && get(game,"movingPlatforms") == platforms, "Held Enter cannot replace runtime");
        }
        // Collect an actual item, then send repeated fresh Enter presses during PLAYING.
        Rectangle item = ((GradeItem)((Array<?>)items).first()).getBounds();
        player.reset(item.x,item.y); tick(game,dt);
        check(number(game,"stageScore") == 5, "Actual item collected before repeated Enter checks");
        for (int i = 0; i < 3; i++) {
            enter = true; tick(game,dt);
            check(get(game,"player") == player && number(game,"stageScore") == 5,
                "Repeated Enter during PLAYING cannot reset progress");
        }
        // Exercise the unchanged GAME_OVER rule in a later semester with five actual death updates.
        LOAD.invoke(game,4);
        for (int i = 0; i < 5; i++) {
            ((Player)get(game,"player")).reset(80,-150); tick(game,dt);
        }
        check(get(game,"gameState") == GameState.GAME_OVER && number(game,"totalDeathCount") == 5, "Fifth failure reaches GAME_OVER");
        enter = true; tick(game,dt);
        assertNewGame(game);
        check(get(game,"gameState") != GameState.TITLE_SCREEN, "GAME_OVER restart goes straight to gameplay");
    }

    private static void assertNewGame(Main game) throws Exception {
        check(get(game,"gameState") == GameState.PLAYING && number(game,"currentStageNumber") == 1
            && number(game,"lives") == 5 && number(game,"semesterDeathCount") == 0
            && number(game,"totalDeathCount") == 0 && number(game,"stageScore") == 0
            && number(game,"collectedItemCount") == 0, "New game starts clean Stage 1 with five lives");
        for (StageResult result : (StageResult[])get(game,"completedResults")) check(result == null, "Results and bonuses empty");
        for (int deaths : (int[])get(game,"semesterDeaths")) check(deaths == 0, "All semester deaths empty");
        for (Object item : (Array<?>)get(game,"gradeItems")) check(!((GradeItem)item).isCollected(), "Items restored");
    }

    private static void tick(Main game, float dt) throws Exception { UPDATE.invoke(game,dt); enter = false; }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static int number(Main game, String name) throws Exception { return (Integer)get(game,name); }
    private static Field field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?> parameter) {
        try { Method m = Main.class.getDeclaredMethod(name,parameter); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
