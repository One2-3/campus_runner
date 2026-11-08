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

/** Real keys and normal loading only; never edits player or hazard positions/phases. */
public final class SophomoreSemester2Verification {
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
                    if (method.getName().equals("isKeyPressed")) return (key == Input.Keys.RIGHT && right)
                        || (key == Input.Keys.SPACE && spaceHeld);
                    if (method.getName().equals("isKeyJustPressed")) return (key == Input.Keys.SPACE && jump)
                        || (key == Input.Keys.ENTER && enter) || (key == Input.Keys.ESCAPE && escape);
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyDefinitionsAndCycle();
            StageResult[] prior = results(game).clone();
            check(prior[0].getItemScore() == 20 && prior[1].getItemScore() == 25 && prior[2].getItemScore() == 30,
                "Incoming scores from actual Stage 1-3 routes");
            early(game, false);
            waitFor(game, () -> platform(game).getBounds().x >= 1300 && platform(game).isMovingForward(),
                8, "Shuttle away from shore");
            move(game, 1100);
            waitFor(game, () -> LifeVerificationSupport.died(game), 3, "Real unbridged gap fall");
            restart(game);
            for (int i = 0; i < 3; i++) check(results(game)[i] == prior[i], "Retry keeps semester " + i);
            route(game, false, true);
            enter = true; tick(game);
            VacationVerificationSupport.finishIfPending(game, false);
            check(number(game, "currentStageNumber") == 5 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)
                && number(game, "stageScore") == 0 && ((Array<?>) get(game, "gradeItems")).size == 8,
                "Stage 5 loads eight items and fresh score");
            for (int i = 0; i < 3; i++) check(results(game)[i] == prior[i], "Stage 5 retains semester " + i);
            check(results(game)[3].getItemScore() == 35, "Stage 4 result retained");
            Method total = Main.class.getDeclaredMethod("getTotalScore"); total.setAccessible(true);
            check((Integer) total.invoke(game) == LifeVerificationSupport.expectedTotal(game,110), "20 + 25 + 30 + 35 total");
            JuniorSemester1Verification.verifyConnected(game, fps);
            route(newGame(), true, false);
            verifyContacts();
            verifyHeldJump();
            System.out.printf("%d fps: harder Stage 4 passed: four fixed hits, two active hits/SAFE crossings, "
                + "both moving platforms, 7/35 and partial 6/30, fall/retry, pause, Stage 1-5 scores%n", fps);
        } finally { Gdx.input = previous; }
    }

    private static void verifyDefinitionsAndCycle() {
        StageDefinition stage = StageFactory.createStage(4);
        check(stage.getStageNumber() == 4 && stage.getYear() == 2 && stage.getSemester() == 2
            && stage.getWorldWidth() == 3000 && stage.getStartX() == 80 && stage.getStartY() == 60,
            "Stage identity/world/spawn");
        check(stage.getBlocks().size == 4 && stage.getMovingPlatforms().size == 2
            && stage.getEnemies().size == 0 && stage.getGradeItems().size == 7, "Level contents");
        check(stage.getFlag().equals(new Rectangle(2940, 360, 20, 100)), "Elevated final flag");
        Array<TimedTrap> traps = stage.getTimedTraps();
        float[][] expected = {{420,60,64,24}, {680,60,64,24}, {880,60,56,24},
            {1690,60,72,28}, {2010,60,144,72}, {2220,60,144,72}};
        check(traps.size == 6, "Four fixed plus two timed sections");
        for (int i = 0; i < traps.size; i++) {
            float[] b = expected[i];
            check(traps.get(i).isFixed() == (i < 4)
                && traps.get(i).getBounds().equals(new Rectangle(b[0], b[1], b[2], b[3])), "Hazard " + i);
        }
        int maximum = 0;
        for (GradeItem item : stage.getGradeItems()) {
            check(item.getScore() == 5, "Five points each"); maximum += item.getScore();
            for (Rectangle block : stage.getBlocks()) check(!item.getBounds().overlaps(block), "Item outside solids");
            for (TimedTrap trap : traps) check(!item.getBounds().overlaps(trap.getBounds()), "Item outside hazards");
        }
        check(maximum == 35, "Maximum 35");
        Array<MovingPlatform> platforms = stage.getMovingPlatforms();
        int[] seen = new int[2], transitions = new int[2];
        TrapState[] before = {traps.get(4).getState(), traps.get(5).getState()};
        boolean different = false;
        double[][] durations = {{1.4,.6,1.6}, {1.1,.7,2.0}};
        for (int frame = 0; frame < 12 / dt; frame++) {
            for (TimedTrap trap : traps) trap.update(dt);
            for (MovingPlatform p : platforms) p.update(dt);
            for (int i = 0; i < 4; i++) check(traps.get(i).getState() == TrapState.ACTIVE, "Fixed stays active");
            for (int i = 0; i < 2; i++) {
                TimedTrap t = traps.get(i + 4); TrapState current = t.getState();
                seen[i] |= 1 << current.ordinal();
                if (current != before[i]) {
                    check((before[i] == TrapState.ACTIVE && current == TrapState.SAFE)
                        || (before[i] == TrapState.SAFE && current == TrapState.WARNING)
                        || (before[i] == TrapState.WARNING && current == TrapState.ACTIVE), "Ordered timed transitions " + i);
                    transitions[i]++; before[i] = current;
                }
                check(t.getStateElapsedTime() >= 0
                    && t.getStateElapsedTime() < durations[i][current.ordinal()], "Phase duration " + i);
            }
            different |= traps.get(4).getState() != traps.get(5).getState();
            Rectangle h = platforms.get(0).getBounds(), v = platforms.get(1).getBounds();
            check(h.x >= 1060 && h.x <= 1340 && h.y == 100 && h.width == 140 && h.height == 20, "Horizontal range");
            check(v.x == 2500 && v.y >= 80 && v.y <= 340 && v.width == 140 && v.height == 20, "Vertical range");
            for (MovingPlatform p : platforms) {
                Rectangle b = p.getBounds(), rider = new Rectangle(b.x, b.y + b.height, b.width, 44);
                for (Rectangle block : stage.getBlocks()) check(!b.overlaps(block) && !rider.overlaps(block), "No overlap/crush");
                for (TimedTrap t : traps) check(!rider.overlaps(t.getBounds()), "Rider clear of spikes");
            }
        }
        for (int i = 0; i < 2; i++) check(seen[i] == 7 && transitions[i] >= 6, "Repeated full cycle " + i);
        check(different, "Timed hazards require distinct timing");
        check(1540 - 1020 > 260 * 2 * 650f / 1700 + 32, "Horizontal gap cannot be jumped directly");
        check(360 - 60 > 650f * 650 / (2 * 1700), "Elevator needed for upper landing");
    }

    private static void firstTwo(Main game, boolean partial) throws Exception {
        tick(game);
        if (partial) { move(game, 100); leap(game, 240, 60); }
        move(game, 360); leap(game, 550, 60);
        move(game, 620); leap(game, 810, 60);
    }
    private static void early(Main game, boolean partial) throws Exception {
        firstTwo(game, partial);
        move(game, 820); leap(game, 970, 60); move(game, 980);
    }
    private static void reachShore(Main game, boolean partial, boolean exercise) throws Exception {
        early(game, partial);
        waitFor(game, () -> platform(game).getBounds().x <= 1080, 8, "Horizontal boarding window");
        jumpToSupport(game, 1120, platform(game));
        if (exercise) { verifyRide(game, platform(game), 8, 85); verifyPause(game); }
        waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1320,
            8, "Horizontal exit window");
        move(game, platform(game).getBounds().x + 100);
        leap(game, 1560, 60);
        check(support(game) == null, "Shuttle dismount on narrow shore");
    }
    private static void reachTimed(Main game, boolean partial, boolean exercise) throws Exception {
        reachShore(game, partial, exercise);
        move(game, 1620); leap(game, 1800, 60); leap(game, 1960, 60);
        check(!player(game).overlaps(trap(game, 4).getBounds()), "Safe first waiting area");
    }
    private static void crossTimed(Main game, int index, float target) throws Exception {
        check(!player(game).overlaps(trap(game, index).getBounds()), "Wait outside timed spikes");
        waitFor(game, () -> trap(game, index).getState() == TrapState.SAFE
            && trap(game, index).getStateElapsedTime() < .08, 5, "Fresh SAFE window " + index);
        boolean touched = false;
        right = true;
        int remaining = (int) (2 / dt);
        while (x(game) < target && --remaining > 0 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)) {
            tick(game);
            if (player(game).overlaps(trap(game, index).getBounds())) {
                touched = true;
                check(trap(game, index).getState() == TrapState.SAFE && !trap(game, index).isDangerousTo(player(game)),
                    "Actual inactive crossing " + index);
            }
        }
        right = false;
        check(remaining > 0 && touched && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Safe crossing complete " + index);
    }
    private static void route(Main game, boolean partial, boolean exercise) throws Exception {
        frames = seenHud = 0;
        reachTimed(game, partial, exercise);
        if (exercise) {
            waitFor(game, () -> trap(game, 4).getState() == TrapState.ACTIVE, 5, "Observe first active window");
            verifyPause(game);
        }
        crossTimed(game, 4, 2170);
        crossTimed(game, 5, 2380);
        move(game, 2400);
        waitFor(game, () -> elevator(game).getBounds().y <= 100, 8, "Lower elevator window after timed crossing");
        jumpToSupport(game, 2540, elevator(game));
        waitFor(game, () -> elevator(game).getBounds().y >= 240, 8, "Ride through elevator item");
        if (exercise) { verifyRide(game, elevator(game), 8, 75); verifyPause(game); }
        move(game, 2600);
        waitFor(game, () -> elevator(game).isMovingForward() && elevator(game).getBounds().y >= 330,
            8, "High exit window");
        leap(game, 2740, 360);
        check(support(game) == null, "Elevator dismount at elevated final platform");
        move(game, 2940);
        int count = partial ? 6 : 7;
        check(state(game) == GameState.STAGE_CLEAR && number(game, "collectedItemCount") == count,
            "Full/partial clear");
        check(seenHud == (1 << (count + 1)) - 1, "Every HUD count 0 through " + count);
        check(results(game)[3].getItemScore() == count * 5 && results(game)[3].getCollectedItemCount() == count,
            "Actual full/partial result");
        Array<?> items = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < items.size; i++) check(((GradeItem) items.get(i)).isCollected() == (!partial || i != 0),
            "Intended item " + i);
        System.out.printf("%d fps: harder Stage 4 %s, %d/7, %d points, %.2fs%n", Math.round(1 / dt),
            partial ? "partial" : "all + endpoint/pause checks", count, count * 5, frames * dt);
    }
    private static void verifyRide(Main game, MovingPlatform p, int seconds, float speed) throws Exception {
        float offset = x(game) - p.getBounds().x;
        boolean forward = p.isMovingForward(); int reversals = 0;
        float low = player(game).getY(), high = low;
        for (int i = 0; i < seconds / dt; i++) {
            tick(game);
            check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p
                && Math.abs(x(game) - p.getBounds().x - offset) < .05f
                && Math.abs(player(game).getY() - p.getBounds().y - p.getBounds().height) < .01f,
                "Stable moving-platform rider");
            check(Math.abs(p.getDeltaX()) + Math.abs(p.getDeltaY()) <= speed * dt + .01f, "Continuous endpoint reversal");
            if (forward != p.isMovingForward()) { reversals++; forward = p.isMovingForward(); }
            low = Math.min(low, player(game).getY()); high = Math.max(high, player(game).getY());
        }
        check(reversals >= 2, "Both endpoints with rider");
        if (p == elevator(game)) check(high - low > 255, "Full elevator ascent/descent");
    }
    private static void verifyContacts() throws Exception {
        for (int i = 0; i < 4; i++) {
            Main game = newGame();
            if (i == 0) tick(game);
            if (i == 1) { tick(game); move(game, 360); leap(game, 550, 60); }
            if (i == 2) firstTwo(game, false);
            if (i == 3) reachShore(game, false, false);
            move(game, trap(game, i).getBounds().x + 30);
            check(LifeVerificationSupport.died(game) && LifeVerificationSupport.failedTrap(game,i).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Fixed spike hit " + i);
            restart(game);
        }
        Main top = newGame(); tick(top); move(top, 360); jump = true; move(top, 440);
        float previousFootY = player(top).getY();
        int landingFrames = (int) (2 / dt) + 1;
        while (!LifeVerificationSupport.died(top) && --landingFrames > 0) {
            previousFootY = player(top).getY(); tick(top);
        }
        check(landingFrames > 0, "Land on fixed spike tips");
        Player landed = LifeVerificationSupport.failedPlayer(top);
        TimedTrap landedTrap = LifeVerificationSupport.failedTrap(top,0);
        float visibleTipY = landedTrap.getSpikeLayout().getTopY();
        check(previousFootY >= visibleTipY && landed.getY() < visibleTipY
            && landed.getY() + landed.getBounds().height > visibleTipY && landedTrap.isDangerousTo(landed),
            "Descending body crosses actual visible tip (30fps may reach the floor in one step)");
        for (int index = 4; index < 6; index++) {
            for (boolean attemptJump : new boolean[] {false, true}) {
                Main game = newGame(); reachTimed(game, false, false);
                if (index == 5) crossTimed(game, 4, 2170);
                final int selected = index;
                Rectangle bounds = trap(game, index).getBounds();
                // A correctly timed jump can now clear the shorter visible spikes.
                // This negative case deliberately starts too late, beside the first
                // actual triangle, and must still produce real contact at every fps.
                if (attemptJump) move(game, bounds.x - 33);
                waitFor(game, () -> trap(game, selected).getState() == TrapState.ACTIVE
                    && trap(game, selected).getStateElapsedTime() < .08, 5, "Fresh ACTIVE " + index);
                jump = attemptJump;
                move(game, bounds.x + (attemptJump ? bounds.width + 10 : 30));
                waitFor(game, () -> LifeVerificationSupport.died(game), 2, "Walk or deliberately late jump hits visible timed spikes " + index);
                check(LifeVerificationSupport.failedTrap(game,index).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Active spike hit " + index);
                restart(game);
            }
        }
    }
    private static void verifyHeldJump() throws Exception {
        Main game = newGame(); tick(game); move(game, 360);
        spaceHeld = jump = true; // One key-down edge, followed by held SPACE (no second edge).
        right = true; tick(game);
        waitFor(game, () -> LifeVerificationSupport.died(game), 4, "Holding jump cannot clear repeated spikes");
        right = false;
        spaceHeld = false;
        check(LifeVerificationSupport.failedTrap(game,1).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Fresh second jump is required");
    }
    private static void jumpToSupport(Main game, float target, MovingPlatform p) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Boarding starts grounded");
        jump = true; move(game, target);
        waitFor(game, () -> support(game) == p || state(game) != GameState.PLAYING, 2, "Actual platform landing");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p, "Boarded intended moving platform");
    }
    private static void restart(Main game) throws Exception {
        check(LifeVerificationSupport.died(game), "Retry follows real failure");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && number(game, "currentStageNumber") == 4 && x(game) == 80
            && player(game).getY() == 60 && number(game, "stageScore") == 0
            && number(game, "collectedItemCount") == 0, "Retry restores stage/spawn/counters");
        for (Object item : (Array<?>) get(game, "gradeItems")) check(!((GradeItem) item).isCollected(), "All seven restored");
        StageDefinition initial = StageFactory.createStage(4);
        Array<MovingPlatform> initialPlatforms = initial.getMovingPlatforms();
        check(platform(game).getBounds().equals(initialPlatforms.get(0).getBounds())
            && elevator(game).getBounds().equals(initialPlatforms.get(1).getBounds()), "Both platform phases reset");
        Array<TimedTrap> initialTraps = initial.getTimedTraps();
        for (int i = 0; i < 6; i++) {
            TimedTrap actual = trap(game, i), expected = initialTraps.get(i);
            check(actual.isFixed() == expected.isFixed() && actual.getBounds().equals(expected.getBounds())
                && actual.getState() == expected.getState() && actual.getStateElapsedTime() == expected.getStateElapsedTime(),
                "Hazard reset " + i);
        }
    }
    private static void verifyPause(Main game) throws Exception {
        float px = x(game), py = player(game).getY();
        Rectangle h = platform(game).getBounds(), v = elevator(game).getBounds();
        TrapState[] states = {trap(game, 4).getState(), trap(game, 5).getState()};
        double[] elapsed = {trap(game, 4).getStateElapsedTime(), trap(game, 5).getStateElapsedTime()};
        escape = true; tick(game); for (int i = 0; i < 30; i++) tick(game);
        check(state(game) == GameState.PAUSED && x(game) == px && player(game).getY() == py
            && platform(game).getBounds().equals(h) && elevator(game).getBounds().equals(v), "Pause freezes player/both platforms");
        for (int i = 0; i < 2; i++) check(trap(game, i + 4).getState() == states[i]
            && trap(game, i + 4).getStateElapsedTime() == elapsed[i], "Pause freezes timer " + i);
        escape = true; tick(game); check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Resume");
    }
    private static MovingPlatform elevator(Main game) throws Exception {
        return (MovingPlatform) ((Array<?>) get(game, "movingPlatforms")).get(1);
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
        LOAD.invoke(game, 4); return game;
    }
    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt); jump = enter = escape = false; frames++;
        if (number(game, "currentStageNumber") == 4) {
            int count = number(game, "collectedItemCount");
            check(count >= 0 && count <= 7 && ((Array<?>) get(game, "gradeItems")).size == 7
                && number(game, "stageScore") == count * 5, "Dynamic HUD 0-7 / score");
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
