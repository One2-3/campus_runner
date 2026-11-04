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

/** Stage 2 checks: only real keys and normal stage loading; never relocates a player. */
public final class Semester2LevelVerification {
    private static boolean right, jump, enter, escape;
    private static float dt;
    private static int frames;
    private static final Method UPDATE = method("update", float.class);
    private static final Method LOAD = method("loadStage", int.class);

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, arguments) -> {
                    int key = arguments == null ? -1 : (Integer) arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.RIGHT && right;
                    if (method.getName().equals("isKeyJustPressed")) {
                        return (key == Input.Keys.SPACE && jump) || (key == Input.Keys.ENTER && enter)
                            || (key == Input.Keys.ESCAPE && escape);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyDefinition();
            for (int fps : new int[] {30, 60, 120}) {
                dt = 1f / fps;
                Main game = newGame(1);
                freshmanRoute(game);
                check(state(game) == GameState.STAGE_CLEAR && number(game, "stageScore") == 20,
                    "Stage 1 real-input clear with 20 points");
                enter = true;
                tick(game);
                check(number(game, "currentStageNumber") == 2, "Enter loads Stage 2");
                check(results(game)[0].getScore() == 20, "Stage 1 score retained");
                fallAndRetry(game, false);
                check(results(game)[0].getScore() == 20, "Stage 2 retry preserves Stage 1 result");
                route(game, false, true);
                check(results(game)[0].getScore() == 20 && results(game)[1].getScore() == 25,
                    "Different semester maxima retained");
                enter = true;
                tick(game);
                check(number(game, "currentStageNumber") == 3 && number(game, "stageScore") == 0,
                    "Stage 3 follows Stage 2");
                SophomoreLevelVerification.verifyConnected(game, fps);
                route(newGame(2), true, false);
                verifyFailureAndRetry(fps);
                verifyBoardingWindows(fps);
            }
            System.out.println("Semester 2 verification passed: real-input routes, 5/25 and 4/20, "
                + "shuttle boarding/carry/reversal/jump, pause, falls/retry and score retention.");
        } finally {
            Gdx.input = previous;
        }
    }

    private static void verifyDefinition() {
        StageDefinition stage = StageFactory.createStage(2);
        check(stage.getStageNumber() == 2 && stage.getYear() == 1 && stage.getSemester() == 2,
            "Semester identity");
        check(stage.getWorldWidth() == 2600 && stage.getStartX() == 80 && stage.getStartY() == 60,
            "World and safe spawn");
        check(stage.getGradeItems().size == 5 && stage.getMovingPlatforms().size == 1
            && stage.getTimedTraps().size == 0 && stage.getEnemies().size == 0, "Level contents");
        int maximum = 0;
        for (GradeItem item : stage.getGradeItems()) {
            check(item.getScore() == 5, "Five points per item");
            maximum += item.getScore();
            for (Rectangle block : stage.getBlocks()) {
                check(!item.getBounds().overlaps(block), "Item outside solids");
            }
        }
        check(maximum == 25, "Maximum 25 points");
        // Sample a complete period including both endpoints and all nearby solids.
        MovingPlatform platform = stage.getMovingPlatforms().first();
        for (int i = 0; i < 1440; i++) {
            platform.update(1f / 70);
            Rectangle p = platform.getBounds();
            check(p.x >= 940 && p.x <= 1300 && p.y == 100 && p.width == 140 && p.height == 20,
                "Horizontal shuttle bounds");
            Rectangle rider = new Rectangle(p.x, p.y + p.height, p.width, 44);
            for (Rectangle block : stage.getBlocks()) {
                check(!p.overlaps(block) && !rider.overlaps(block), "No platform overlap or rider crush");
            }
        }
        check(1480 - 900 > 260 * (2 * 650f / 1700) + 32,
            "Gap exceeds even continuous-physics jump plus body width");
    }

    private static void approach(Main game, boolean partial) throws Exception {
        tick(game);
        if (partial) {
            walk(game, 100);
            leap(game, 240, 60); // Pass over the first item without touching it.
        } else walk(game, 240);
        leap(game, 340, 110);
        land(game, 520, 60);
        walk(game, 560);
        leap(game, 680, 130);
        land(game, 830, 60);
        walk(game, 870);
    }

    private static void route(Main game, boolean partial, boolean exerciseRide) throws Exception {
        frames = 0;
        hud(game, 0, 0);
        approach(game, partial);
        MovingPlatform platform = platform(game);
        waitFor(game, () -> platform.getBounds().x <= 960, 12, "Shuttle approaches boarding shore");
        leap(game, 1000, 120);
        check(support(game) == platform, "Actual landing on moving platform");
        if (exerciseRide) {
            // No directional input: stay on board through both turnarounds.
            float offset = x(game) - platform.getBounds().x;
            boolean startDirection = platform.isMovingForward();
            int reversals = 0;
            for (int i = 0; i < 12 / dt; i++) {
                tick(game);
                check(support(game) == platform && Math.abs(x(game) - platform.getBounds().x - offset) < .05f,
                    "Stable carry including endpoints");
                check(Math.abs(platform.getDeltaX()) <= 70 * dt + .01f, "Continuous endpoint motion");
                if (startDirection != platform.isMovingForward()) {
                    reversals++;
                    startDirection = platform.isMovingForward();
                }
            }
            check(reversals >= 2, "Both shuttle reversals exercised with rider");
            float beforePause = platform.getBounds().x;
            escape = true;
            tick(game);
            for (int i = 0; i < 20; i++) tick(game);
            check(state(game) == GameState.PAUSED && platform.getBounds().x == beforePause, "Pause freezes shuttle");
            escape = true;
            tick(game);
            // Jump while it travels left, so it catches the same stationary-X player again.
            waitFor(game, () -> !platform.isMovingForward() && platform.getBounds().x < 1200
                && platform.getBounds().x > 1080, 12, "Leftward moving-jump window");
            jump = true;
            tick(game);
            check(support(game) == null && player(game).getY() > 120, "Jump detaches from moving platform");
            waitFor(game, () -> support(game) == platform, 2, "Jump lands back on moving shuttle");
        }
        waitFor(game, () -> platform.isMovingForward() && platform.getBounds().x >= 1280,
            12, "Ride to exit shore");
        // Walk toward the front of the broad platform before the exit jump.
        walk(game, platform.getBounds().x + 100);
        leap(game, 1510, 60);
        walk(game, 1780);
        leap(game, 1890, 140);
        walk(game, 1970);
        leap(game, 2090, 160);
        walk(game, 2170);
        leap(game, 2290, 240);
        walk(game, 2370);
        leap(game, 2490, 320);
        walk(game, 2540);
        check(state(game) == GameState.STAGE_CLEAR, "Stage 2 flag clear");
        int count = partial ? 4 : 5;
        hud(game, count, count * 5);
        Array<?> items = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < items.size; i++) {
            check(((GradeItem) items.get(i)).isCollected() == (!partial || i != 0), "Intended collection " + i);
        }
        System.out.printf("%d fps: semester 2 %s, %d/5 items, %d points, %.2fs%n",
            Math.round(1 / dt), partial ? "partial" : exerciseRide ? "all-items + ride checks" : "all-items",
            count, count * 5, frames * dt);
    }

    private static void verifyFailureAndRetry(int fps) throws Exception {
        for (boolean attemptJump : new boolean[] {false, true}) {
            Main game = newGame(2);
            fallAndRetry(game, attemptJump);
            if (attemptJump) route(game, false, false); // The retried level is playable, not just reset.
        }
        System.out.printf("%d fps: walk-off and unsupported jump fail; retry restores items/shuttle and clears%n", fps);
    }

    private static void fallAndRetry(Main game, boolean attemptJump) throws Exception {
        approach(game, false);
        waitFor(game, () -> platform(game).getBounds().x >= 1250
            && platform(game).isMovingForward(), 12, "Shuttle away from shore");
        right = true;
        jump = attemptJump;
        tick(game);
        waitFor(game, () -> state(game) == GameState.GAME_OVER, 3, "Real gap fall");
        right = false;
        check(number(game, "collectedItemCount") == 2, "Collected items before real fall");
        enter = true;
        tick(game);
        hud(game, 0, 0);
        check(x(game) == 80 && player(game).getY() == 60 && number(game, "currentStageNumber") == 2,
            "Retry respawns in same semester");
        check(platform(game).getBounds().equals(new Rectangle(940, 100, 140, 20)), "Retry resets shuttle");
        for (Object item : (Array<?>) get(game, "gradeItems")) {
            check(!((GradeItem) item).isCollected(), "Retry restores all five items");
        }
    }

    private static void verifyBoardingWindows(int fps) throws Exception {
        for (float takeoff : new float[] {850, 870, 890}) {
            for (boolean forward : new boolean[] {false, true}) {
                Main game = newGame(2);
                tick(game);
                walk(game, 240); leap(game, 340, 110); land(game, 520, 60);
                walk(game, 560); leap(game, 680, 130); land(game, 830, 60);
                walk(game, takeoff);
                waitFor(game, () -> platform(game).getBounds().x <= 960
                    && platform(game).isMovingForward() == forward, 12, "Boarding window");
                leap(game, 1000, 120);
                check(support(game) == platform(game), "Board from " + takeoff + " direction " + forward);
            }
        }
        System.out.printf("%d fps: real-input boarding succeeds from x850/870/890 in both directions%n", fps);
    }

    private static void freshmanRoute(Main game) throws Exception {
        tick(game);
        walk(game, 240); leap(game, 340, 110); land(game, 520, 60);
        walk(game, 720); leap(game, 890, 60);
        walk(game, 1240); leap(game, 1350, 140);
        walk(game, 1410); leap(game, 1540, 220); land(game, 1710, 60);
        walk(game, 1740); leap(game, 1850, 140);
        walk(game, 1900); leap(game, 2030, 220);
        walk(game, 2070); leap(game, 2170, 300); leap(game, 2290, 360); walk(game, 2360);
    }

    private static void leap(Main game, float target, float height) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Jump starts grounded at " + x(game));
        OrthographicCamera camera = (OrthographicCamera) get(game, "camera");
        check(target >= camera.position.x - 400 && target + 32 <= camera.position.x + 400
            && height + 44 < 600, "Next landing visible before jump");
        jump = true;
        land(game, target, height);
    }

    private static void land(Main game, float target, float height) throws Exception {
        walk(game, target);
        waitFor(game, () -> (Boolean) playerField(game, "grounded") || state(game) != GameState.PLAYING,
            2, "Landing completes");
        check(state(game) == GameState.PLAYING && Math.abs(player(game).getY() - height) < .01f,
            "Landing " + target + "/" + height + " actual " + x(game) + "/" + player(game).getY());
    }

    private static void walk(Main game, float target) throws Exception {
        right = true;
        waitFor(game, () -> x(game) >= target || state(game) != GameState.PLAYING, 12, "Move to " + target);
        right = false;
    }

    private interface Condition { boolean ready() throws Exception; }
    private static void waitFor(Main game, Condition condition, int seconds, String message) throws Exception {
        int limit = (int) (seconds / dt) + 1;
        while (!condition.ready() && --limit > 0) tick(game);
        check(limit > 0, message + " (x=" + x(game) + ", y=" + player(game).getY() + ")");
    }

    private static Main newGame(int stageNumber) throws Exception {
        right = jump = enter = escape = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field(Main.class, "camera").set(game, camera);
        field(Main.class, "viewport").set(game, new FitViewport(800, 600, camera));
        LOAD.invoke(game, stageNumber);
        return game;
    }

    private static void tick(Main game) throws Exception {
        UPDATE.invoke(game, dt);
        jump = enter = escape = false;
        frames++;
    }
    private static void hud(Main game, int count, int score) throws Exception {
        check(((Array<?>) get(game, "gradeItems")).size == 5
            && number(game, "collectedItemCount") == count && number(game, "stageScore") == score,
            "HUD data " + count + "/5, SCORE: " + score);
    }
    private static Object get(Main game, String name) throws Exception { return field(Main.class, name).get(game); }
    private static Object playerField(Main game, String name) throws Exception { return field(Player.class, name).get(player(game)); }
    private static int number(Main game, String name) throws Exception { return (Integer) get(game, name); }
    private static Player player(Main game) throws Exception { return (Player) get(game, "player"); }
    private static float x(Main game) throws Exception { return player(game).getCenterX() - 16; }
    private static GameState state(Main game) throws Exception { return (GameState) get(game, "gameState"); }
    private static StageResult[] results(Main game) throws Exception { return (StageResult[]) get(game, "completedResults"); }
    private static MovingPlatform platform(Main game) throws Exception { return (MovingPlatform) ((Array<?>) get(game, "movingPlatforms")).first(); }
    private static Object support(Main game) throws Exception { return playerField(game, "supportingPlatform"); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static Method method(String name, Class<?> parameter) {
        try {
            Method method = Main.class.getDeclaredMethod(name, parameter);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
