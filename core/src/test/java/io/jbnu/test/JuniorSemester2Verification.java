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

/** Stage 6: real input and normal stage loading; runtime positions/phases are read only. */
public final class JuniorSemester2Verification {
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
            verifyDefinitions();
            StageResult[] prior = results(game).clone();
            for (int i = 0; i < 5; i++) check(prior[i].getItemScore() == 20 + i * 5, "Incoming actual semester " + i);
            approach(game, false);
            waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1280,
                7, "Shuttle away before real fall");
            move(game, 1070);
            waitFor(game, () -> LifeVerificationSupport.died(game), 3, "Real gap fall");
            restart(game);
            for (int i = 0; i < 5; i++) check(results(game)[i] == prior[i], "Retry preserves prior result " + i);
            route(game, false, true);
            enter = true; tick(game);
            VacationVerificationSupport.finishIfPending(game, false);
            check(number(game, "currentStageNumber") == 7 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)
                && number(game, "stageScore") == 0 && ((Array<?>) get(game, "gradeItems")).size == 10,
                "Stage 7 loads ten items");
            for (int i = 0; i < 5; i++) check(results(game)[i] == prior[i], "Stage 7 preserves result " + i);
            check(results(game)[5].getItemScore() == 45, "Stage 6 score retained");
            Method total = Main.class.getDeclaredMethod("getTotalScore"); total.setAccessible(true);
            check((Integer) total.invoke(game) == LifeVerificationSupport.expectedTotal(game,195), "Six-semester total 195");
            SeniorSemester1Verification.verifyConnected(game, fps);
            route(newGame(), true, false);
            verifyContacts();
            System.out.printf("%d fps: Stage 6 passed: 3 enemy hits/evasions, 3 fixed hits, 2 timed hits/SAFE crossings, "
                + "both platforms/jump-reland, 9/45 and partial 8/40, fall/retry, pause, Stage 1-7 results%n", fps);
        } finally { Gdx.input = previous; }
    }

    private static void verifyDefinitions() {
        StageDefinition stage = StageFactory.createStage(6);
        check(stage.getStageNumber() == 6 && stage.getYear() == 3 && stage.getSemester() == 2
            && stage.getWorldWidth() == 3400 && stage.getStartX() == 80 && stage.getStartY() == 60, "Identity/world/spawn");
        check(stage.getBlocks().size == 4 && stage.getEnemies().size == 3 && stage.getMovingPlatforms().size == 2
            && stage.getTimedTraps().size == 5 && stage.getGradeItems().size == 9, "Level contents");
        check(stage.getFlag().equals(new Rectangle(3340,400,20,100)), "Safe final flag");
        Array<TimedTrap> traps = stage.getTimedTraps();
        for (int i = 0; i < 5; i++) check(traps.get(i).isFixed() == (i < 3), "Hazard kind " + i);
        int maximum = 0;
        for (GradeItem item : stage.getGradeItems()) {
            check(item.getScore() == 5, "Five points each"); maximum += item.getScore();
            for (Rectangle b : stage.getBlocks()) check(!item.getBounds().overlaps(b), "Item outside solids");
            for (TimedTrap t : traps) check(!item.getBounds().overlaps(t.getBounds()), "Item outside spikes");
        }
        check(maximum == 45, "Maximum 45");
        float[][] patrols = {{500,620,90,60,30,32}, {1540,1720,100,60,32,36}, {2780,2900,110,320,32,32}};
        Array<PatrollingEnemy> enemies = stage.getEnemies();
        boolean[] direction = {enemies.get(0).isMovingRight(), enemies.get(1).isMovingRight(), enemies.get(2).isMovingRight()};
        int[] reversals = new int[3], seen = new int[2], transitions = new int[2];
        TrapState[] before = {traps.get(3).getState(), traps.get(4).getState()};
        double[][] durations = {{1.2,.6,1.8}, {1.05,.75,2.2}};
        boolean distinct = false;
        Array<MovingPlatform> platforms = stage.getMovingPlatforms();
        for (int frame = 0; frame < 16 / dt; frame++) {
            for (int i = 0; i < 3; i++) {
                PatrollingEnemy e = enemies.get(i); float oldX = e.getBounds().x; e.update(dt);
                Rectangle b = e.getBounds(); float[] p = patrols[i]; Rectangle floor = stage.getBlocks().get(i);
                check(b.x >= p[0] && b.x <= p[1] && b.y == p[3] && b.width == p[4] && b.height == p[5], "Patrol range " + i);
                check(Math.abs(b.x - oldX) <= p[2] * dt + .01f, "Continuous enemy reversal " + i);
                check(b.x >= floor.x && b.x + b.width <= floor.x + floor.width && b.y == floor.y + floor.height,
                    "Enemy fully supported " + i);
                if (direction[i] != e.isMovingRight()) { reversals[i]++; direction[i] = e.isMovingRight(); }
            }
            for (TimedTrap t : traps) t.update(dt);
            for (int i = 0; i < 3; i++) check(traps.get(i).getState() == TrapState.ACTIVE, "Fixed stays active");
            for (int i = 0; i < 2; i++) {
                TimedTrap t = traps.get(i + 3); TrapState current = t.getState(); seen[i] |= 1 << current.ordinal();
                if (before[i] != current) {
                    check((before[i] == TrapState.SAFE && current == TrapState.WARNING)
                        || (before[i] == TrapState.WARNING && current == TrapState.ACTIVE)
                        || (before[i] == TrapState.ACTIVE && current == TrapState.SAFE), "Ordered timed cycle " + i);
                    transitions[i]++; before[i] = current;
                }
                check(t.getStateElapsedTime() >= 0 && t.getStateElapsedTime() < durations[i][current.ordinal()], "Phase duration " + i);
            }
            distinct |= traps.get(3).getState() != traps.get(4).getState();
            for (MovingPlatform p : platforms) {
                p.update(dt); Rectangle b = p.getBounds(), rider = new Rectangle(b.x,b.y+b.height,b.width,44);
                for (Rectangle floor : stage.getBlocks()) check(!b.overlaps(floor) && !rider.overlaps(floor), "No platform crush");
                for (TimedTrap t : traps) check(!b.overlaps(t.getBounds()) && !rider.overlaps(t.getBounds()), "Platform/spikes separate");
                for (PatrollingEnemy e : enemies) check(!b.overlaps(e.getBounds()) && !rider.overlaps(e.getBounds()), "Platform/enemy separate");
            }
            Rectangle h = platforms.get(0).getBounds(), v = platforms.get(1).getBounds();
            check(h.x >= 1040 && h.x <= 1320 && h.y == 100 && h.width == 140 && h.height == 20, "Horizontal bounds");
            check(v.x == 2000 && v.y >= 80 && v.y <= 300 && v.width == 140 && v.height == 20, "Vertical bounds");
        }
        for (int n : reversals) check(n >= 4, "Repeated enemy reversals");
        for (int i = 0; i < 2; i++) check(seen[i] == 7 && transitions[i] >= 6, "Repeated timed states " + i);
        check(distinct, "Distinct hazard timing");
        check(1520 - 1000 > 260 * 2 * 650f / 1700 + 32, "Shuttle required for gap");
        check(320 - 60 > 650f * 650 / (2 * 1700), "Elevator required for height");
        check(2684 - 2320 > 260 * 2 * 650f / 1700 + 32, "Both timed sections cannot be jumped at once");
    }

    private static void early(Main game, boolean partial) throws Exception {
        tick(game); if (partial) leap(game, 220, 60);
        move(game, 240); leap(game, 420, 60);
        check(x(game) + 32 < 500, "Safe observation after first spikes");
    }
    private static void approach(Main game, boolean partial) throws Exception {
        early(game, partial);
        waitFor(game, () -> !enemy(game,0).isMovingRight() && enemy(game,0).getBounds().x <= 540, 4, "First enemy return");
        leap(game, 650, 60);
        check(x(game) >= enemy(game,0).getBounds().x + enemy(game,0).getBounds().width, "First enemy jump evasion");
        move(game, 950);
    }
    private static void reachSecond(Main game, boolean partial, boolean exercise) throws Exception {
        approach(game, partial);
        waitFor(game, () -> platform(game).getBounds().x <= 1060, 7, "Shuttle boarding window");
        jumpToSupport(game, 1100, platform(game));
        if (exercise) {
            verifyRide(game, platform(game), 7, 110);
            verifyPause(game);
            waitFor(game, () -> !platform(game).isMovingForward() && platform(game).getBounds().x > 1120
                && platform(game).getBounds().x < 1220, 7, "Moving horizontal jump window");
            jumpAndReland(game, platform(game));
        }
        waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1280
            && enemy(game,1).isMovingRight() && enemy(game,1).getBounds().x >= 1630, 20, "Observe landing enemy and shuttle");
        move(game, platform(game).getBounds().x + 100);
        leap(game, 1540, 60);
        check(support(game) == null && enemy(game,1).getBounds().x > x(game) + 32, "Enemy-free actual landing");
    }
    private static void reachElevator(Main game, boolean partial, boolean exercise) throws Exception {
        reachSecond(game, partial, exercise);
        leap(game, 1740, 60);
        check(x(game) > enemy(game,1).getBounds().x + enemy(game,1).getBounds().width, "Second enemy jump evasion");
        leap(game, 1910, 60);
    }
    private static void reachUpper(Main game, boolean partial, boolean exercise) throws Exception {
        reachElevator(game, partial, exercise);
        waitFor(game, () -> elevator(game).getBounds().y <= 100, 7, "Elevator lower window");
        jumpToSupport(game, 2040, elevator(game));
        waitFor(game, () -> elevator(game).getBounds().y >= 220, 7, "Ride through elevator item");
        if (exercise) {
            verifyRide(game, elevator(game), 7, 80); verifyPause(game);
            waitFor(game, () -> elevator(game).isMovingForward() && elevator(game).getBounds().y >= 140
                && elevator(game).getBounds().y <= 180, 7, "Moving vertical jump window");
            jumpAndReland(game, elevator(game));
        }
        move(game, 2100);
        waitFor(game, () -> elevator(game).isMovingForward() && elevator(game).getBounds().y >= 290, 7, "Upper exit window");
        leap(game, 2240, 320); move(game, 2260);
        check(support(game) == null, "Elevator dismount");
    }
    private static void crossTimed(Main game, int index, float target) throws Exception {
        check(!player(game).overlaps(trap(game,index).getBounds()), "Safe waiting space " + index);
        waitFor(game, () -> trap(game,index).getState() == TrapState.SAFE
            && trap(game,index).getStateElapsedTime() < .08, 5, "Fresh SAFE phase " + index);
        boolean touched = false; right = true; int remaining = (int) (2 / dt);
        while (x(game) < target && --remaining > 0 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)) {
            tick(game);
            if (player(game).overlaps(trap(game,index).getBounds())) {
                touched = true;
                check(trap(game,index).getState() == TrapState.SAFE && !trap(game,index).isDangerousTo(player(game)), "Actual SAFE crossing " + index);
            }
        }
        right = false;
        check(remaining > 0 && touched && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Safe passage complete " + index);
    }
    private static void reachThird(Main game, boolean partial, boolean exercise) throws Exception {
        reachUpper(game,partial,exercise);
        if (exercise) { waitFor(game, () -> trap(game,3).getState() == TrapState.ACTIVE, 5, "Observe active hazard"); verifyPause(game); }
        crossTimed(game,3,2480); crossTimed(game,4,2710);
        check(x(game) + 32 < 2780, "Judgment space between traps and third enemy");
    }
    private static void passThird(Main game) throws Exception {
        waitFor(game, () -> !enemy(game,2).isMovingRight() && enemy(game,2).getBounds().x <= 2820, 4, "Third enemy return");
        leap(game,2930,320);
        check(x(game) >= enemy(game,2).getBounds().x + enemy(game,2).getBounds().width, "Third enemy jump evasion");
    }
    private static void route(Main game, boolean partial, boolean exercise) throws Exception {
        frames = seenHud = 0;
        reachThird(game,partial,exercise); passThird(game);
        move(game,2940); leap(game,3090,320); leap(game,3240,400); move(game,3340);
        int count = partial ? 8 : 9;
        check(state(game) == GameState.VACATION_EVENT && number(game,"collectedItemCount") == count
            && results(game)[5].getItemScore() == count * 5 && results(game)[5].getCollectedItemCount() == count,
            "Full/partial clear and record; actual count=" + number(game,"collectedItemCount"));
        check(seenHud == (1 << (count + 1)) - 1, "Every HUD count 0 through " + count);
        Array<?> items = (Array<?>) get(game,"gradeItems");
        for (int i = 0; i < items.size; i++) check(((GradeItem) items.get(i)).isCollected() == (!partial || i != 0), "Intended item " + i);
        System.out.printf("%d fps: Stage 6 %s, %d/9, %d points, %.2fs%n", Math.round(1/dt),
            partial ? "partial" : "all + endpoints/jump-reland/pause", count, count * 5, frames * dt);
    }
    private static void verifyContacts() throws Exception {
        for (int i = 0; i < 3; i++) {
            Main game = newGame();
            if (i == 0) { early(game,false); move(game,600); }
            if (i == 1) { reachSecond(game,false,false); move(game,1690); }
            if (i == 2) { reachThird(game,false,false); move(game,2870); }
            check(LifeVerificationSupport.died(game) && LifeVerificationSupport.failedEnemy(game,i).touches(LifeVerificationSupport.failedPlayer(game)), "Enemy hit " + i);
            restart(game);
        }
        for (int i = 0; i < 3; i++) {
            Main game = newGame();
            if (i == 0) tick(game);
            if (i == 1) { reachSecond(game,false,false); leap(game,1740,60); }
            if (i == 2) { reachThird(game,false,false); passThird(game); }
            move(game,trap(game,i).getBounds().x + 30);
            check(LifeVerificationSupport.died(game) && LifeVerificationSupport.failedTrap(game,i).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Fixed hit " + i);
            restart(game);
        }
        for (int index = 3; index < 5; index++) {
            for (boolean attemptJump : new boolean[] {false,true}) {
                Main game = newGame(); reachUpper(game,false,false);
                if (index == 4) crossTimed(game,3,2480);
                final int selected = index;
                waitFor(game, () -> trap(game,selected).getState() == TrapState.ACTIVE
                    && trap(game,selected).getStateElapsedTime() < .08, 5, "Fresh ACTIVE " + index);
                Rectangle b = trap(game,index).getBounds(); jump = attemptJump;
                move(game,b.x + (attemptJump ? b.width + 10 : 30));
                waitFor(game, () -> LifeVerificationSupport.died(game), 2, "Active walk/jump cannot bypass " + index);
                check(LifeVerificationSupport.failedTrap(game,index).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Active hit " + index);
                restart(game);
            }
        }
    }
    private static void jumpToSupport(Main game, float target, MovingPlatform p) throws Exception {
        check((Boolean) playerField(game,"grounded"), "Boarding starts grounded");
        jump = true; move(game,target);
        waitFor(game, () -> support(game) == p || state(game) != GameState.PLAYING, 2, "Board moving platform");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p, "Actual platform boarding");
    }
    private static void jumpAndReland(Main game, MovingPlatform p) throws Exception {
        check(support(game) == p, "Moving jump starts supported");
        jump = true; tick(game);
        check(support(game) == null && player(game).getY() > p.getBounds().y + p.getBounds().height, "Jump detaches");
        waitFor(game, () -> support(game) == p || state(game) != GameState.PLAYING, 2, "Reland on moving platform");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p, "Actual reattachment");
    }
    private static void verifyRide(Main game, MovingPlatform p, int seconds, float speed) throws Exception {
        float offset = x(game) - p.getBounds().x;
        boolean direction = p.isMovingForward(); int reversals = 0;
        float low = player(game).getY(), high = low;
        for (int i = 0; i < seconds / dt; i++) {
            tick(game);
            check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p
                && Math.abs(x(game) - p.getBounds().x - offset) < .05f
                && Math.abs(player(game).getY() - p.getBounds().y - p.getBounds().height) < .01f, "Stable moving rider");
            check(Math.abs(p.getDeltaX()) + Math.abs(p.getDeltaY()) <= speed * dt + .01f, "Continuous platform reversal");
            if (direction != p.isMovingForward()) { reversals++; direction = p.isMovingForward(); }
            low = Math.min(low,player(game).getY()); high = Math.max(high,player(game).getY());
        }
        check(reversals >= 2, "Both endpoints with rider");
        if (p == elevator(game)) check(high - low > 215, "Full vertical ascent/descent");
    }
    private static void restart(Main game) throws Exception {
        check(LifeVerificationSupport.died(game), "Retry follows real failure");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && number(game,"currentStageNumber") == 6
            && x(game) == 80 && player(game).getY() == 60 && number(game,"collectedItemCount") == 0
            && number(game,"stageScore") == 0, "Retry stage/spawn/counters");
        StageDefinition initial = StageFactory.createStage(6);
        for (Object item : (Array<?>) get(game,"gradeItems")) check(!((GradeItem) item).isCollected(), "Nine items restored");
        check(platform(game).getBounds().equals(initial.getMovingPlatforms().get(0).getBounds())
            && elevator(game).getBounds().equals(initial.getMovingPlatforms().get(1).getBounds()), "Both platforms restored");
        for (int i = 0; i < 3; i++) {
            PatrollingEnemy e = initial.getEnemies().get(i);
            check(enemy(game,i).getBounds().equals(e.getBounds()) && enemy(game,i).isMovingRight() == e.isMovingRight(), "Enemy reset " + i);
        }
        for (int i = 0; i < 5; i++) {
            TimedTrap t = initial.getTimedTraps().get(i);
            check(trap(game,i).isFixed() == t.isFixed() && trap(game,i).getBounds().equals(t.getBounds())
                && trap(game,i).getState() == t.getState() && trap(game,i).getStateElapsedTime() == t.getStateElapsedTime(), "Trap reset " + i);
        }
    }
    private static void verifyPause(Main game) throws Exception {
        Rectangle[] enemies = {enemy(game,0).getBounds(),enemy(game,1).getBounds(),enemy(game,2).getBounds()};
        boolean[] directions = {enemy(game,0).isMovingRight(),enemy(game,1).isMovingRight(),enemy(game,2).isMovingRight()};
        Rectangle h = platform(game).getBounds(), v = elevator(game).getBounds();
        TrapState[] states = {trap(game,3).getState(),trap(game,4).getState()};
        double[] elapsed = {trap(game,3).getStateElapsedTime(),trap(game,4).getStateElapsedTime()};
        float px = x(game), py = player(game).getY();
        escape = true; tick(game); for (int i = 0; i < 30; i++) tick(game);
        check(state(game) == GameState.PAUSED && x(game) == px && player(game).getY() == py
            && platform(game).getBounds().equals(h) && elevator(game).getBounds().equals(v), "Pause freezes player/platforms");
        for (int i = 0; i < 3; i++) check(enemy(game,i).getBounds().equals(enemies[i])
            && enemy(game,i).isMovingRight() == directions[i], "Pause freezes enemy " + i);
        for (int i = 0; i < 2; i++) check(trap(game,i+3).getState() == states[i]
            && trap(game,i+3).getStateElapsedTime() == elapsed[i], "Pause freezes timed hazard " + i);
        escape = true; tick(game); check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Resume");
    }
    private static MovingPlatform elevator(Main game) throws Exception { return (MovingPlatform) ((Array<?>) get(game,"movingPlatforms")).get(1); }
    private static PatrollingEnemy enemy(Main game, int index) throws Exception { return (PatrollingEnemy) ((Array<?>) get(game,"enemies")).get(index); }
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
        LOAD.invoke(game, 6); return game;
    }
    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt); jump = enter = escape = false; frames++;
        if (number(game, "currentStageNumber") == 6) {
            int count = number(game, "collectedItemCount");
            check(count >= 0 && count <= 9 && ((Array<?>) get(game, "gradeItems")).size == 9
                && number(game, "stageScore") == count * 5, "Dynamic HUD 0-9 / score");
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
