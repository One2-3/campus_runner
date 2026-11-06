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

/** Stage 5 real-input checks; observes state without relocating players or changing patrol phases. */
public final class JuniorSemester1Verification {
    private static boolean right, jump, enter, escape, spaceHeld;
    private static float dt;
    private static int frames, seenHud;
    private static final Method UPDATE = method("update", float.class);
    private static final Method LOAD = method("loadStage", int.class);

    public static void verifyConnected(Main game, int fps) throws Exception {
        Input previous = Gdx.input;
        try {
            dt = 1f / fps;
            right = jump = enter = escape = spaceHeld = false;
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                (proxy, method, arguments) -> {
                    int key = arguments == null ? -1 : (Integer) arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.RIGHT && right;
                    if (method.getName().equals("isKeyJustPressed")) return (key == Input.Keys.SPACE && jump)
                        || (key == Input.Keys.ENTER && enter) || (key == Input.Keys.ESCAPE && escape);
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyDefinitionsAndPatrols();
            StageResult[] prior = results(game).clone();
            for (int i = 0; i < 4; i++) check(prior[i].getItemScore() == 20 + i * 5, "Incoming actual result " + i);
            approachShuttle(game, false);
            waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1660,
                7, "Shuttle away for actual fall");
            move(game, 1450);
            waitFor(game, () -> LifeVerificationSupport.died(game), 3, "Actual gap fall");
            restart(game);
            for (int i = 0; i < 4; i++) check(results(game)[i] == prior[i], "Retry retains semester " + i);
            route(game, false, true);
            enter = true; tick(game);
            check(number(game, "currentStageNumber") == 6 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)
                && number(game, "stageScore") == 0 && ((Array<?>) get(game, "gradeItems")).size == 9,
                "Stage 6 loads nine items");
            for (int i = 0; i < 4; i++) check(results(game)[i] == prior[i], "Stage 6 retains result " + i);
            check(results(game)[4].getItemScore() == 40, "Stage 5 recorded score retained");
            Method total = Main.class.getDeclaredMethod("getTotalScore"); total.setAccessible(true);
            check((Integer) total.invoke(game) == LifeVerificationSupport.expectedTotal(game,150), "Five-semester total 150");
            JuniorSemester2Verification.verifyConnected(game, fps);
            route(newGame(), true, false);
            verifyContacts();
            System.out.printf("%d fps: Stage 5 passed: two patrols/hits/jump evasion, shuttle landing timing, "
                + "8/40 and partial 7/35, spikes, fall/retry, pause, Stage 1-6 scores%n", fps);
        } finally { Gdx.input = previous; }
    }

    private static void verifyDefinitionsAndPatrols() {
        StageDefinition stage = StageFactory.createStage(5);
        check(stage.getYear() == 3 && stage.getSemester() == 1 && stage.getStageNumber() == 5
            && stage.getWorldWidth() == 3200 && stage.getStartX() == 80 && stage.getStartY() == 60,
            "Stage identity/world/spawn");
        check(stage.getBlocks().size == 4 && stage.getEnemies().size == 2 && stage.getMovingPlatforms().size == 1
            && stage.getTimedTraps().size == 3 && stage.getGradeItems().size == 8, "Level contents");
        check(stage.getFlag().equals(new Rectangle(3140, 180, 20, 100)), "Flag on final platform");
        Array<TimedTrap> traps = stage.getTimedTraps();
        check(traps.get(0).isFixed() && traps.get(1).isFixed() && !traps.get(2).isFixed(), "Two fixed/one timed spikes");
        int maximum = 0;
        for (GradeItem item : stage.getGradeItems()) {
            check(item.getScore() == 5, "Five points per item"); maximum += item.getScore();
            for (Rectangle block : stage.getBlocks()) check(!item.getBounds().overlaps(block), "Item outside solids");
            for (TimedTrap trap : traps) check(!item.getBounds().overlaps(trap.getBounds()), "Item outside spikes");
        }
        check(maximum == 40, "Maximum 40");
        Array<PatrollingEnemy> enemies = stage.getEnemies();
        float[][] patrols = {{850,970,85,30,32}, {1920,2100,90,32,36}};
        boolean[] directions = {enemies.get(0).isMovingRight(), enemies.get(1).isMovingRight()};
        int[] reversals = new int[2];
        int seen = 0, transitions = 0;
        TrapState before = traps.get(2).getState();
        MovingPlatform platform = stage.getMovingPlatforms().first();
        for (int frame = 0; frame < 16 / dt; frame++) {
            for (int i = 0; i < 2; i++) {
                PatrollingEnemy enemy = enemies.get(i);
                float previousX = enemy.getBounds().x; enemy.update(dt);
                Rectangle b = enemy.getBounds(); float[] p = patrols[i];
                check(b.x >= p[0] && b.x <= p[1] && b.y == 100 && b.width == p[3] && b.height == p[4], "Patrol bounds " + i);
                check(Math.abs(b.x - previousX) <= p[2] * dt + .01f, "No teleport at reversal " + i);
                Rectangle floor = stage.getBlocks().get(i + 1);
                check(b.x >= floor.x && b.x + b.width <= floor.x + floor.width && b.y == floor.y + floor.height,
                    "Enemy stays fully supported " + i);
                if (directions[i] != enemy.isMovingRight()) { reversals[i]++; directions[i] = enemy.isMovingRight(); }
            }
            for (TimedTrap t : traps) t.update(dt);
            check(traps.get(0).getState() == TrapState.ACTIVE && traps.get(1).getState() == TrapState.ACTIVE, "Fixed active");
            TimedTrap t = traps.get(2); TrapState current = t.getState(); seen |= 1 << current.ordinal();
            if (before != current) {
                check((before == TrapState.SAFE && current == TrapState.WARNING)
                    || (before == TrapState.WARNING && current == TrapState.ACTIVE)
                    || (before == TrapState.ACTIVE && current == TrapState.SAFE), "Ordered cycle");
                transitions++; before = current;
            }
            double duration = current == TrapState.SAFE ? 1.15 : current == TrapState.WARNING ? .65 : 2;
            check(t.getStateElapsedTime() >= 0 && t.getStateElapsedTime() < duration, "Configured phase duration");
            platform.update(dt); Rectangle b = platform.getBounds();
            check(b.x >= 1420 && b.x <= 1700 && b.y == 140 && b.width == 140 && b.height == 20, "Shuttle range");
            Rectangle rider = new Rectangle(b.x, b.y + b.height, b.width, 44);
            for (Rectangle block : stage.getBlocks()) check(!b.overlaps(block) && !rider.overlaps(block), "No crush/overlap");
            for (PatrollingEnemy enemy : enemies) check(!b.overlaps(enemy.getBounds()) && !rider.overlaps(enemy.getBounds()),
                "Enemy and moving-platform volumes separated");
        }
        check(reversals[0] >= 4 && reversals[1] >= 4 && seen == 7 && transitions >= 6, "Repeated patrol/cycle transitions");
        check(1900 - 1380 > 260 * 2 * 650f / 1700 + 32, "Shuttle gap cannot be bypassed by normal jump");
        check(stage.getEnemies().get(0).getBounds().equals(new Rectangle(900,100,30,32))
            && stage.getEnemies().get(1).getBounds().equals(new Rectangle(2070,100,32,36)), "Independent initial enemies");
    }

    private static void early(Main game, boolean partial) throws Exception {
        tick(game);
        if (partial) leap(game, 220, 60);
        move(game, 240); leap(game, 420, 60); leap(game, 610, 100);
        move(game, 790);
        check(x(game) + 32 < 850, "Safe first-enemy observation position");
    }
    private static void approachShuttle(Main game, boolean partial) throws Exception {
        early(game, partial);
        waitFor(game, () -> !enemy(game, 0).isMovingRight() && enemy(game, 0).getBounds().x <= 890,
            4, "First enemy returning toward takeoff side");
        leap(game, 1000, 100);
        check(x(game) > enemy(game, 0).getBounds().x + enemy(game, 0).getBounds().width, "Jump cleared first enemy");
        move(game, 1330);
    }
    private static void board(Main game, boolean partial, boolean exercise) throws Exception {
        approachShuttle(game, partial);
        waitFor(game, () -> platform(game).getBounds().x <= 1440, 7, "Shuttle approach");
        check((Boolean) playerField(game, "grounded"), "Grounded before boarding");
        jump = true; move(game, 1480);
        waitFor(game, () -> support(game) == platform(game) || state(game) != GameState.PLAYING, 2, "Shuttle landing");
        check(support(game) == platform(game) && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Actual boarding");
        if (exercise) {
            float offset = x(game) - platform(game).getBounds().x;
            boolean direction = platform(game).isMovingForward(); int reversals = 0;
            for (int i = 0; i < 7 / dt; i++) {
                tick(game);
                check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == platform(game)
                    && Math.abs(x(game) - platform(game).getBounds().x - offset) < .05f && player(game).getY() == 160,
                    "Stable rider through both endpoints");
                if (direction != platform(game).isMovingForward()) { reversals++; direction = platform(game).isMovingForward(); }
            }
            check(reversals >= 2, "Both shuttle reversals");
            verifyPause(game);
        }
    }
    private static void reachSecondEnemy(Main game, boolean partial, boolean exercise) throws Exception {
        board(game, partial, exercise);
        waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1660
            && enemy(game, 1).isMovingRight() && enemy(game, 1).getBounds().x >= 2010,
            20, "Observe platform and second-enemy direction together");
        move(game, platform(game).getBounds().x + 100);
        leap(game, 1920, 100);
        check(support(game) == null && enemy(game, 1).getBounds().x > x(game) + 32,
            "Landing is clear of second enemy");
    }
    private static void reachLate(Main game, boolean partial, boolean exercise) throws Exception {
        reachSecondEnemy(game, partial, exercise);
        leap(game, 2120, 100);
        check(x(game) > enemy(game, 1).getBounds().x + enemy(game, 1).getBounds().width, "Jump clears second enemy");
        move(game, 2170); leap(game, 2360, 100); leap(game, 2520, 180);
        move(game, 2600);
    }
    private static void route(Main game, boolean partial, boolean exercise) throws Exception {
        frames = seenHud = 0;
        reachLate(game, partial, exercise);
        if (exercise) {
            waitFor(game, () -> trap(game, 2).getState() == TrapState.ACTIVE, 4, "Observe danger");
            verifyPause(game);
        }
        waitFor(game, () -> trap(game, 2).getState() == TrapState.SAFE
            && trap(game, 2).getStateElapsedTime() < .08, 4, "Fresh safe window");
        boolean touched = false; right = true;
        int remaining = (int) (2 / dt);
        while (x(game) < 2830 && --remaining > 0 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)) {
            tick(game);
            if (player(game).overlaps(trap(game, 2).getBounds())) {
                touched = true;
                check(trap(game, 2).getState() == TrapState.SAFE && !trap(game, 2).isDangerousTo(player(game)), "Inactive crossing");
            }
        }
        right = false;
        check(remaining > 0 && touched && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Actual SAFE crossing");
        move(game, 3140);
        int count = partial ? 7 : 8;
        check(state(game) == GameState.STAGE_CLEAR && number(game, "collectedItemCount") == count
            && results(game)[4].getItemScore() == count * 5 && results(game)[4].getCollectedItemCount() == count,
            "Full/partial clear and stored score; actual count=" + number(game, "collectedItemCount"));
        check(seenHud == (1 << (count + 1)) - 1, "Every HUD count 0 through " + count);
        Array<?> items = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < items.size; i++) check(((GradeItem) items.get(i)).isCollected() == (!partial || i != 0), "Intended item " + i);
        System.out.printf("%d fps: Stage 5 %s, %d/8, %d points, %.2fs%n", Math.round(1 / dt),
            partial ? "partial" : exercise ? "all + ride/pause" : "all", count, count * 5, frames * dt);
    }
    private static void verifyContacts() throws Exception {
        for (int i = 0; i < 2; i++) {
            Main game = newGame();
            if (i == 0) { early(game, false); move(game, 1010); }
            else { reachSecondEnemy(game, false, false); move(game, 2070); }
            check(LifeVerificationSupport.died(game) && LifeVerificationSupport.failedEnemy(game,i).touches(LifeVerificationSupport.failedPlayer(game)), "Enemy contact " + i);
            restart(game);
        }
        Main fixed1 = newGame(); tick(fixed1); move(fixed1, 330);
        check(LifeVerificationSupport.died(fixed1) && LifeVerificationSupport.failedTrap(fixed1,0).isDangerousTo(LifeVerificationSupport.failedPlayer(fixed1)), "First fixed hit");
        Main fixed2 = newGame(); reachSecondEnemy(fixed2, false, false); leap(fixed2, 2120, 100); move(fixed2, 2270);
        check(LifeVerificationSupport.died(fixed2) && LifeVerificationSupport.failedTrap(fixed2,1).isDangerousTo(LifeVerificationSupport.failedPlayer(fixed2)), "Second fixed hit");
        Main active = newGame(); reachLate(active, false, false);
        waitFor(active, () -> trap(active, 2).getState() == TrapState.ACTIVE
            && trap(active, 2).getStateElapsedTime() < .08, 4, "Fresh active phase");
        move(active, 2700);
        check(LifeVerificationSupport.died(active) && LifeVerificationSupport.failedTrap(active,2).isDangerousTo(LifeVerificationSupport.failedPlayer(active)), "Active spike hit");
        restart(active);
    }
    private static void restart(Main game) throws Exception {
        check(LifeVerificationSupport.died(game), "Retry follows actual failure");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(number(game, "currentStageNumber") == 5 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)
            && x(game) == 80 && player(game).getY() == 60 && number(game, "collectedItemCount") == 0
            && number(game, "stageScore") == 0, "Retry spawn/counters");
        StageDefinition initial = StageFactory.createStage(5);
        for (Object item : (Array<?>) get(game, "gradeItems")) check(!((GradeItem) item).isCollected(), "All eight restored");
        check(platform(game).getBounds().equals(initial.getMovingPlatforms().first().getBounds()), "Shuttle reset");
        for (int i = 0; i < 2; i++) {
            PatrollingEnemy expected = initial.getEnemies().get(i);
            check(enemy(game, i).getBounds().equals(expected.getBounds())
                && enemy(game, i).isMovingRight() == expected.isMovingRight(), "Enemy position/direction restored " + i);
        }
        for (int i = 0; i < 3; i++) {
            TimedTrap expected = initial.getTimedTraps().get(i);
            check(trap(game, i).isFixed() == expected.isFixed() && trap(game, i).getBounds().equals(expected.getBounds())
                && trap(game, i).getState() == expected.getState()
                && trap(game, i).getStateElapsedTime() == expected.getStateElapsedTime(), "Trap reset " + i);
        }
    }
    private static void verifyPause(Main game) throws Exception {
        Rectangle[] enemies = {enemy(game, 0).getBounds(), enemy(game, 1).getBounds()};
        boolean[] directions = {enemy(game, 0).isMovingRight(), enemy(game, 1).isMovingRight()};
        float px = x(game), py = player(game).getY(); Rectangle p = platform(game).getBounds();
        TrapState state = trap(game, 2).getState(); double elapsed = trap(game, 2).getStateElapsedTime();
        escape = true; tick(game); for (int i = 0; i < 30; i++) tick(game);
        check(state(game) == GameState.PAUSED && x(game) == px && player(game).getY() == py
            && platform(game).getBounds().equals(p) && trap(game, 2).getState() == state
            && trap(game, 2).getStateElapsedTime() == elapsed, "Pause freezes physics/platform/timer");
        for (int i = 0; i < 2; i++) check(enemy(game, i).getBounds().equals(enemies[i])
            && enemy(game, i).isMovingRight() == directions[i], "Pause freezes enemy " + i);
        escape = true; tick(game); check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Resume");
    }
    private static PatrollingEnemy enemy(Main game, int index) throws Exception {
        return (PatrollingEnemy) ((Array<?>) get(game, "enemies")).get(index);
    }
    private static void leap(Main game, float target, float height) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Jump starts grounded");
        jump = true; move(game, target);
        waitFor(game, () -> (Boolean) playerField(game, "grounded") || state(game) != GameState.PLAYING, 2, "Landing");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && Math.abs(player(game).getY() - height) < .01f,
            "Land " + target + "/" + height + " actual " + x(game) + "/" + player(game).getY());
    }
    private static void move(Main game, float target) throws Exception {
        right = true;
        waitFor(game, () -> x(game) >= target || state(game) != GameState.PLAYING, 12, "Move " + target);
        right = false;
    }
    private interface Condition { boolean ready() throws Exception; }
    private static void waitFor(Main game, Condition condition, int seconds, String message) throws Exception {
        int remaining = (int) (seconds / dt) + 1;
        while (!condition.ready() && !LifeVerificationSupport.died(game) && --remaining > 0) tick(game);
        check(remaining > 0, message + " x=" + x(game) + " y=" + player(game).getY());
    }
    private static Main newGame() throws Exception {
        right = jump = enter = escape = spaceHeld = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field(Main.class, "camera").set(game, camera);
        field(Main.class, "viewport").set(game, new FitViewport(800, 600, camera));
        LOAD.invoke(game, 5); return game;
    }
    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt); jump = enter = escape = false; frames++;
        if (number(game, "currentStageNumber") == 5) {
            int count = number(game, "collectedItemCount");
            check(count >= 0 && count <= 8 && ((Array<?>) get(game, "gradeItems")).size == 8
                && number(game, "stageScore") == count * 5, "Dynamic HUD 0-8 / score");
            seenHud |= 1 << count;
        }
    }
    private static Object get(Main game, String name) throws Exception { return field(Main.class, name).get(game); }
    private static Object playerField(Main game, String name) throws Exception { return field(Player.class, name).get(player(game)); }
    private static int number(Main game, String name) throws Exception { return (Integer) get(game, name); }
    private static Player player(Main game) throws Exception { return (Player) get(game, "player"); }
    private static float x(Main game) throws Exception { return player(game).getCenterX() - 16; }
    private static GameState state(Main game) throws Exception { return (GameState) get(game, "gameState"); }
    private static StageResult[] results(Main game) throws Exception { return (StageResult[]) get(game, "completedResults"); }
    private static MovingPlatform platform(Main game) throws Exception { return (MovingPlatform) ((Array<?>) get(game, "movingPlatforms")).first(); }
    private static TimedTrap trap(Main game, int index) throws Exception { return (TimedTrap) ((Array<?>) get(game, "timedTraps")).get(index); }
    private static Object support(Main game) throws Exception { return playerField(game, "supportingPlatform"); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Method method(String name, Class<?> parameter) {
        try {
            Method method = Main.class.getDeclaredMethod(name, parameter); method.setAccessible(true); return method;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
