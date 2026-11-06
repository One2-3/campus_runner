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

/** Stage 3 integration checks: runtime state is observed, never used to relocate a player. */
public final class SophomoreLevelVerification {
    private static boolean left, right, jump, enter, escape;
    private static float dt;
    private static int frames;
    private static int seenHudCounts;
    private static final Method UPDATE = method("update", float.class);
    private static final Method LOAD = method("loadStage", int.class);

    // Invoked by the real-input Stage 1 -> 2 route, with both recorded results intact.
    public static void verifyConnected(Main game, int fps) throws Exception {
        Input previous = Gdx.input;
        try {
            dt = 1f / fps;
            left = right = jump = enter = escape = false;
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, arguments) -> {
                    int key = arguments == null ? -1 : (Integer) arguments[0];
                    if (method.getName().equals("isKeyPressed")) {
                        return (key == Input.Keys.RIGHT && right) || (key == Input.Keys.LEFT && left);
                    }
                    if (method.getName().equals("isKeyJustPressed")) {
                        return (key == Input.Keys.SPACE && jump) || (key == Input.Keys.ENTER && enter)
                            || (key == Input.Keys.ESCAPE && escape);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyDefinition();
            StageResult first = results(game)[0], second = results(game)[1];
            check(first.getItemScore() == 20 && second.getItemScore() == 25, "Incoming Stage 1/2 results");
            failAndRetry(game, false);
            check(results(game)[0] == first && results(game)[1] == second, "Retry preserves earlier results");
            route(game, false, true);
            check(results(game)[2].getItemScore() == 30 && results(game)[2].getCollectedItemCount() == 6,
                "Stage 3 records all six items / 30 points");
            enter = true;
            tick(game);
            check(number(game, "currentStageNumber") == 4 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game),
                "Enter advances to Stage 4");
            check(results(game)[0] == first && results(game)[1] == second && results(game)[2].getItemScore() == 30,
                "20/25/30 scores preserved after transition");
            check(number(game, "stageScore") == 0 && ((Array<?>) get(game, "gradeItems")).size == 7,
                "Stage 4 loads its own counters and seven items");
            Method total = Main.class.getDeclaredMethod("getTotalScore");
            total.setAccessible(true);
            check((Integer) total.invoke(game) == LifeVerificationSupport.expectedTotal(game,75), "Total is 75 after three semesters");
            SophomoreSemester2Verification.verifyConnected(game, fps);
            route(newGame(), true, false);
            Main retry = newGame();
            failAndRetry(retry, true);
            route(retry, false, false);
            verifyBoardingWindows();
            System.out.printf("%d fps: Stage 3 checks passed; elevator/shuttle, 6/30, partial 5/25, "
                + "real falls/retry, pause and Stage 1-4 progression%n", fps);
        } finally {
            Gdx.input = previous;
        }
    }

    private static void verifyDefinition() {
        StageDefinition stage = StageFactory.createStage(3);
        check(stage.getStageNumber() == 3 && stage.getYear() == 2 && stage.getSemester() == 1,
            "Stage 3 identity");
        check(stage.getWorldWidth() == 2800 && stage.getStartX() == 80 && stage.getStartY() == 60,
            "Stage 3 world/spawn");
        check(stage.getBlocks().size == 5 && stage.getMovingPlatforms().size == 2
            && stage.getTimedTraps().size == 0 && stage.getEnemies().size == 0, "Level contents");
        check(stage.getFlag().equals(new Rectangle(2740, 360, 20, 100)), "Flag on safe final landing");
        int maximum = 0;
        check(stage.getGradeItems().size == 6, "Six items");
        for (GradeItem item : stage.getGradeItems()) {
            check(item.getScore() == 5, "Five points each");
            maximum += item.getScore();
            Rectangle bounds = item.getBounds();
            check(bounds.x >= 0 && bounds.x + bounds.width <= stage.getWorldWidth(), "Item in world");
            for (Rectangle block : stage.getBlocks()) check(!bounds.overlaps(block), "Item outside solids");
        }
        check(maximum == 30, "Maximum 30 points");
        Array<MovingPlatform> platforms = stage.getMovingPlatforms();
        for (int frame = 0; frame < 12 / dt; frame++) {
            for (int i = 0; i < platforms.size; i++) platforms.get(i).update(dt);
            Rectangle v = platforms.get(0).getBounds(), h = platforms.get(1).getBounds();
            check(v.x == 1100 && v.y >= 80 && v.y <= 340 && v.width == 160 && v.height == 20,
                "Vertical travel bounds");
            check(h.y == 340 && h.x >= 1760 && h.x <= 2080 && h.width == 160 && h.height == 20,
                "Horizontal travel bounds");
            check(!v.overlaps(h), "Platforms never intersect");
            for (MovingPlatform platform : platforms) {
                Rectangle p = platform.getBounds();
                Rectangle rider = new Rectangle(p.x, p.y + p.height, p.width, 44);
                for (Rectangle block : stage.getBlocks()) {
                    check(!p.overlaps(block) && !rider.overlaps(block), "No overlap or rider crush");
                }
            }
        }
        check(360 - 60 > 650f * 650 / (2 * 1700), "Upper dock exceeds direct jump height");
        check(2280 - 1720 > 260 * (2 * 650f / 1700) + 32, "Elevated gap requires shuttle");
    }

    private static void approach(Main game, boolean partial, float takeoff) throws Exception {
        tick(game);
        if (partial) { move(game, 100); leap(game, 200, 60); }
        else move(game, 200);
        leap(game, 340, 150);
        land(game, 520, 60);
        move(game, 540); leap(game, 650, 110); land(game, 830, 60);
        move(game, takeoff);
    }

    private static void boardElevator(Main game) throws Exception {
        waitFor(game, () -> platform(game, 0).getBounds().y <= 100, 9, "Lower boarding window");
        jumpToSupport(game, 1160, platform(game, 0));
        waitFor(game, () -> platform(game, 0).getBounds().y >= 240, 9, "Ride through elevator item");
    }

    private static void leaveElevator(Main game) throws Exception {
        move(game, 1220);
        waitFor(game, () -> platform(game, 0).isMovingForward()
            && platform(game, 0).getBounds().y >= 330, 9, "Upper exit window");
        leap(game, 1360, 360);
        check(support(game) == null, "Upper dock dismount");
    }

    private static void route(Main game, boolean partial, boolean exercise) throws Exception {
        frames = 0;
        seenHudCounts = 0;
        hud(game, 0);
        approach(game, partial, 1010);
        boardElevator(game);
        if (exercise) {
            verifyRide(game, platform(game, 0), 9, 65);
            verifyPause(game);
            leaveElevator(game);
            // Reboard from the upper dock, descend and leave at the lower dock.
            move(game, 1330);
            waitFor(game, () -> platform(game, 0).getBounds().y >= 325, 9, "Upper boarding window");
            jumpToSupport(game, 1200, platform(game, 0));
            waitFor(game, () -> !platform(game, 0).isMovingForward()
                && platform(game, 0).getBounds().y <= 100, 9, "Lower dismount window");
            leap(game, 1010, 60);
            check(support(game) == null, "Lower dock dismount");
            boardElevator(game);
        }
        leaveElevator(game);
        move(game, 1670);
        waitFor(game, () -> platform(game, 1).getBounds().x <= 1780, 9, "Elevated shuttle boarding");
        jumpToSupport(game, 1820, platform(game, 1));
        if (exercise) {
            verifyRide(game, platform(game, 1), 9, 90);
            verifyPause(game);
        }
        waitFor(game, () -> platform(game, 1).isMovingForward()
            && platform(game, 1).getBounds().x >= 2060, 9, "Shuttle exit window");
        move(game, platform(game, 1).getBounds().x + 110);
        leap(game, 2310, 360);
        move(game, 2740);
        check(state(game) == GameState.STAGE_CLEAR, "Flag clears Stage 3");
        int count = partial ? 5 : 6;
        hud(game, count);
        check(seenHudCounts == (1 << (count + 1)) - 1, "Every HUD count from zero through " + count);
        check(results(game)[2].getItemScore() == count * 5 && results(game)[2].getCollectedItemCount() == count,
            "Actual collected score recorded for full/partial clear");
        Array<?> items = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < items.size; i++) {
            check(((GradeItem) items.get(i)).isCollected() == (!partial || i != 0), "Intended item " + i);
        }
        System.out.printf("%d fps: Stage 3 %s, %d/6, %d points, %.2fs%n", Math.round(1 / dt),
            partial ? "partial" : exercise ? "all + endpoint/roundtrip checks" : "all", count, count * 5, frames * dt);
    }

    private static void verifyRide(Main game, MovingPlatform platform, int seconds, float speed) throws Exception {
        float offset = x(game) - platform.getBounds().x;
        boolean forward = platform.isMovingForward();
        int reversals = 0;
        float lowest = player(game).getY(), highest = lowest;
        for (int i = 0; i < seconds / dt; i++) {
            tick(game);
            Rectangle p = platform.getBounds();
            check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == platform
                && Math.abs(x(game) - p.x - offset) < .05f
                && Math.abs(player(game).getY() - p.y - p.height) < .01f, "Stable rider on moving top");
            check(Math.abs(platform.getDeltaX()) + Math.abs(platform.getDeltaY()) <= speed * dt + .01f,
                "Continuous direction reversal");
            lowest = Math.min(lowest, player(game).getY());
            highest = Math.max(highest, player(game).getY());
            if (forward != platform.isMovingForward()) { reversals++; forward = platform.isMovingForward(); }
        }
        check(reversals >= 2, "Both endpoint reversals with rider");
        if (platform == platform(game, 0)) check(highest - lowest > 255, "Full ascent and descent");
    }

    private static void verifyPause(Main game) throws Exception {
        float px = x(game), py = player(game).getY();
        Rectangle v = platform(game, 0).getBounds(), h = platform(game, 1).getBounds();
        escape = true; tick(game);
        for (int i = 0; i < 30; i++) tick(game);
        check(state(game) == GameState.PAUSED && x(game) == px && player(game).getY() == py
            && platform(game, 0).getBounds().equals(v) && platform(game, 1).getBounds().equals(h), "Pause freezes all motion");
        escape = true; tick(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Resume");
    }

    private static void failAndRetry(Main game, boolean attemptJump) throws Exception {
        approach(game, false, 1010);
        waitFor(game, () -> platform(game, 0).isMovingForward()
            && platform(game, 0).getBounds().y >= 260, 9, "Elevator unavailable for direct crossing");
        right = true; jump = attemptJump; tick(game);
        waitFor(game, () -> LifeVerificationSupport.died(game), 3, "Actual fall into gap");
        right = false;
        check(LifeVerificationSupport.failedCollected(game) == 3, "Basic items collected before failure");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        hud(game, 0);
        check(number(game, "currentStageNumber") == 3 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)
            && x(game) == 80 && player(game).getY() == 60, "Retry same stage at spawn");
        check(platform(game, 0).getBounds().equals(new Rectangle(1100, 80, 160, 20))
            && platform(game, 1).getBounds().equals(new Rectangle(1760, 340, 160, 20)), "Both platform phases reset");
        for (Object item : (Array<?>) get(game, "gradeItems")) check(!((GradeItem) item).isCollected(), "All six restored");
    }

    private static void verifyBoardingWindows() throws Exception {
        for (float takeoff : new float[] {990, 1010, 1030}) {
            for (boolean ascending : new boolean[] {false, true}) {
                Main game = newGame();
                approach(game, false, takeoff);
                waitFor(game, () -> platform(game, 0).getBounds().y <= 100
                    && platform(game, 0).isMovingForward() == ascending, 9, "Varied lower boarding window");
                jumpToSupport(game, 1160, platform(game, 0));
            }
        }
    }

    private static void jumpToSupport(Main game, float target, MovingPlatform platform) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Moving-platform jump starts grounded");
        jump = true;
        move(game, target);
        waitFor(game, () -> support(game) == platform || state(game) != GameState.PLAYING, 2, "Landing on moving platform");
        check(support(game) == platform && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Actual moving-platform contact");
    }

    private static void leap(Main game, float target, float height) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Jump starts grounded");
        jump = true;
        land(game, target, height);
    }
    private static void land(Main game, float target, float height) throws Exception {
        move(game, target);
        waitFor(game, () -> (Boolean) playerField(game, "grounded") || state(game) != GameState.PLAYING, 2, "Landing completes");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && Math.abs(player(game).getY() - height) < .01f,
            "Landing " + target + "/" + height + " actual " + x(game) + "/" + player(game).getY());
    }
    private static void move(Main game, float target) throws Exception {
        boolean forward = target > x(game);
        right = forward; left = !forward;
        waitFor(game, () -> (forward ? x(game) >= target : x(game) <= target)
            || state(game) != GameState.PLAYING, 12, "Move to " + target);
        left = right = false;
    }
    private interface Condition { boolean ready() throws Exception; }
    private static void waitFor(Main game, Condition condition, int seconds, String message) throws Exception {
        int remaining = (int) (seconds / dt) + 1;
        while (!condition.ready() && !LifeVerificationSupport.died(game) && --remaining > 0) tick(game);
        check(remaining > 0, message + " (x=" + x(game) + ", y=" + player(game).getY() + ")");
    }
    private static Main newGame() throws Exception {
        left = right = jump = enter = escape = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field(Main.class, "camera").set(game, camera);
        field(Main.class, "viewport").set(game, new FitViewport(800, 600, camera));
        LOAD.invoke(game, 3);
        return game;
    }
    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt); jump = enter = escape = false; frames++;
        if (number(game, "currentStageNumber") == 3) {
            int count = number(game, "collectedItemCount");
            check(count >= 0 && count <= 6, "HUD count within current-stage total");
            hud(game, count);
            seenHudCounts |= 1 << count;
        }
    }
    private static void hud(Main game, int count) throws Exception {
        check(((Array<?>) get(game, "gradeItems")).size == 6 && number(game, "collectedItemCount") == count
            && number(game, "stageScore") == count * 5, "HUD data " + count + "/6, SCORE: " + count * 5);
    }
    private static Object get(Main game, String name) throws Exception { return field(Main.class, name).get(game); }
    private static Object playerField(Main game, String name) throws Exception { return field(Player.class, name).get(player(game)); }
    private static int number(Main game, String name) throws Exception { return (Integer) get(game, name); }
    private static Player player(Main game) throws Exception { return (Player) get(game, "player"); }
    private static float x(Main game) throws Exception { return player(game).getCenterX() - 16; }
    private static GameState state(Main game) throws Exception { return (GameState) get(game, "gameState"); }
    private static StageResult[] results(Main game) throws Exception { return (StageResult[]) get(game, "completedResults"); }
    private static MovingPlatform platform(Main game, int index) throws Exception { return (MovingPlatform) ((Array<?>) get(game, "movingPlatforms")).get(index); }
    private static Object support(Main game) throws Exception { return playerField(game, "supportingPlatform"); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Method method(String name, Class<?> parameter) {
        try {
            Method method = Main.class.getDeclaredMethod(name, parameter); method.setAccessible(true); return method;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
