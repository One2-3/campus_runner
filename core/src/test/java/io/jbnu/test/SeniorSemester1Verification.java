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

/** Stage 7: only real keys and normal loading; runtime positions and phases are never edited. */
public final class SeniorSemester1Verification {
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
            for (int i = 0; i < 6; i++) check(prior[i].getItemScore() == 20 + i * 5, "Incoming actual result " + i);
            early(game,false);
            waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1000, 6, "Shuttle away");
            move(game,870); waitFor(game, () -> LifeVerificationSupport.died(game), 3, "Actual open gap fall");
            restart(game);
            for (int i = 0; i < 6; i++) check(results(game)[i] == prior[i], "Retry preserves semester " + i);
            route(game,false,true);
            enter = true; tick(game);
            check(number(game,"currentStageNumber") == 8 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)
                && number(game,"stageScore") == 0 && ((Array<?>) get(game,"gradeItems")).size == 12,
                "Stage 8 final map loads twelve items");
            for (int i = 0; i < 6; i++) check(results(game)[i] == prior[i], "Stage 8 preserves result " + i);
            check(results(game)[6].getItemScore() == 50, "Stage 7 result retained");
            Method total = Main.class.getDeclaredMethod("getTotalScore"); total.setAccessible(true);
            check((Integer) total.invoke(game) == LifeVerificationSupport.expectedTotal(game,245), "Seven-semester total 245");
            SeniorSemester2Verification.verifyConnected(game, fps);
            route(newGame(),true,false);
            verifyContacts();
            System.out.printf("%d fps: Stage 7 passed: platform transfer, narrow landings, 4 enemy/5 fixed/3 timed hits, "
                + "10/50 and partial 9/45, restart/pause, camera and Stage 1-8 results%n", fps);
        } finally { Gdx.input = previous; }
    }

    private static void verifyDefinitions() {
        StageDefinition stage = StageFactory.createStage(7);
        check(stage.getStageNumber() == 7 && stage.getYear() == 4 && stage.getSemester() == 1
            && stage.getWorldWidth() == 3800 && stage.getStartX() == 80 && stage.getStartY() == 60, "Stage identity/world/spawn");
        check(stage.getBlocks().size == 7 && stage.getEnemies().size == 4 && stage.getMovingPlatforms().size == 3
            && stage.getTimedTraps().size == 8 && stage.getGradeItems().size == 10, "Level contents");
        check(stage.getFlag().equals(new Rectangle(3760,380,20,100)), "Final safe flag");
        Array<TimedTrap> traps = stage.getTimedTraps();
        for (int i = 0; i < 8; i++) check(traps.get(i).isFixed() == (i < 5), "Hazard kind " + i);
        int maximum = 0;
        for (GradeItem item : stage.getGradeItems()) {
            check(item.getScore() == 5, "Five points per item"); maximum += item.getScore();
            Rectangle b = item.getBounds(); check(b.x >= 0 && b.x + b.width <= 3800, "Item in world");
            for (Rectangle floor : stage.getBlocks()) check(!b.overlaps(floor), "Item outside solids");
            for (TimedTrap t : traps) check(!b.overlaps(t.getBounds()), "Item outside spikes");
        }
        check(maximum == 50, "Maximum 50");
        float[][] patrols = {{420,540,90,60,30,32}, {1540,1660,95,300,30,32},
            {2100,2140,105,320,28,32}, {3410,3530,70,300,28,36}};
        int[] floorIndices = {0,1,3,5}, reversals = new int[4], seen = new int[3], transitions = new int[3];
        Array<PatrollingEnemy> enemies = stage.getEnemies();
        boolean[] directions = new boolean[4]; for (int i = 0; i < 4; i++) directions[i] = enemies.get(i).isMovingRight();
        TrapState[] before = {traps.get(5).getState(),traps.get(6).getState(),traps.get(7).getState()};
        double[][] durations = {{1.15,.65,1.8},{1.05,.75,2.0},{1.0,.7,2.4}};
        Array<MovingPlatform> platforms = stage.getMovingPlatforms();
        boolean distinct = false;
        for (int frame = 0; frame < 18 / dt; frame++) {
            for (int i = 0; i < 4; i++) {
                PatrollingEnemy e = enemies.get(i); float old = e.getBounds().x; e.update(dt);
                Rectangle b = e.getBounds(), floor = stage.getBlocks().get(floorIndices[i]); float[] p = patrols[i];
                check(b.x >= p[0] && b.x <= p[1] && b.y == p[3] && b.width == p[4] && b.height == p[5], "Patrol bounds " + i);
                check(Math.abs(b.x-old) <= p[2]*dt+.01f, "Continuous patrol reversal " + i);
                check(b.x >= floor.x && b.x+b.width <= floor.x+floor.width && b.y == floor.y+floor.height, "Enemy fully supported " + i);
                if (directions[i] != e.isMovingRight()) { reversals[i]++; directions[i] = e.isMovingRight(); }
            }
            for (TimedTrap t : traps) t.update(dt);
            for (int i = 0; i < 5; i++) check(traps.get(i).getState() == TrapState.ACTIVE, "Fixed stays active");
            for (int i = 0; i < 3; i++) {
                TimedTrap t = traps.get(i+5); TrapState current = t.getState(); seen[i] |= 1 << current.ordinal();
                if (before[i] != current) {
                    check((before[i] == TrapState.SAFE && current == TrapState.WARNING)
                        || (before[i] == TrapState.WARNING && current == TrapState.ACTIVE)
                        || (before[i] == TrapState.ACTIVE && current == TrapState.SAFE), "Ordered timed transitions " + i);
                    transitions[i]++; before[i] = current;
                }
                check(t.getStateElapsedTime() >= 0 && t.getStateElapsedTime() < durations[i][current.ordinal()], "Phase duration " + i);
            }
            distinct |= traps.get(5).getState() != traps.get(6).getState() || traps.get(6).getState() != traps.get(7).getState();
            for (MovingPlatform p : platforms) {
                p.update(dt); Rectangle b = p.getBounds(), rider = new Rectangle(b.x,b.y+b.height,b.width,44);
                for (Rectangle floor : stage.getBlocks()) check(!b.overlaps(floor) && !rider.overlaps(floor), "No platform crush");
                for (TimedTrap t : traps) check(!b.overlaps(t.getBounds()) && !rider.overlaps(t.getBounds()), "Platform/spikes separate");
                for (PatrollingEnemy e : enemies) check(!b.overlaps(e.getBounds()) && !rider.overlaps(e.getBounds()), "Platform/enemies separate");
            }
            Rectangle a = platforms.get(0).getBounds(), v = platforms.get(1).getBounds(), b = platforms.get(2).getBounds();
            check(a.x >= 840 && a.x <= 1040 && a.y == 100 && a.width == 140 && a.height == 20, "First shuttle range");
            check(v.x == 1240 && v.y >= 100 && v.y <= 280 && v.width == 140 && v.height == 20, "Elevator range");
            check(b.x >= 2980 && b.x <= 3180 && b.y == 280 && b.width == 140 && b.height == 20, "Final shuttle range");
            check(!a.overlaps(v) && !a.overlaps(b) && !v.overlaps(b), "Platforms never intersect");
        }
        for (int n : reversals) check(n >= 4, "Repeated enemy reversals");
        for (int i = 0; i < 3; i++) check(seen[i] == 7 && transitions[i] >= 6, "Repeated full cycle " + i);
        check(distinct, "Independent timing");
        check(1240-800 > 260*2*650f/1700+32, "First shuttle needed before transfer");
        check(300-120 > 650f*650/(2*1700), "Elevator needed from first shuttle");
        check(3380-2940 > 260*2*650f/1700+32, "Final shuttle required");
        check(2884-2300 > 260*2*650f/1700+32, "Three timed sections cannot be jumped together");
    }

    private static void firstEnemyWait(Main game, boolean partial) throws Exception {
        tick(game); if (partial) leap(game,190,60);
        move(game,200); leap(game,360,60);
        check(x(game)+32 < 420, "Protected observation after first spikes");
    }
    private static void early(Main game, boolean partial) throws Exception {
        firstEnemyWait(game,partial);
        waitFor(game, () -> !enemy(game,0).isMovingRight() && enemy(game,0).getBounds().x <= 460, 4, "First enemy return");
        leap(game,580,60); check(x(game) >= enemy(game,0).getBounds().x+30, "First enemy evasion");
        move(game,590); leap(game,750,60);
    }
    private static void reachUpper(Main game, boolean partial, boolean exercise) throws Exception {
        early(game,partial);
        waitFor(game, () -> platform(game).getBounds().x <= 860, 6, "First shuttle boarding window");
        jumpToSupport(game,900,platform(game));
        if (exercise) { verifyRide(game,platform(game),6,90); verifyPause(game); }
        waitFor(game, () -> platform(game).isMovingForward() && platform(game).getBounds().x >= 1010
            && elevator(game).getBounds().y <= 140, 20, "Observe both moving platforms before transfer");
        move(game,platform(game).getBounds().x+100);
        check(support(game) == platform(game), "Transfer starts on horizontal platform");
        jumpToSupport(game,1280,elevator(game));
        check(support(game) == elevator(game), "Direct horizontal-to-vertical transfer");
        waitFor(game, () -> elevator(game).getBounds().y >= 180, 7, "Ride through elevator item");
        if (exercise) { verifyRide(game,elevator(game),7,65); verifyPause(game); }
        move(game,1340);
        waitFor(game, () -> elevator(game).isMovingForward() && elevator(game).getBounds().y >= 270, 7, "High exit window");
        leap(game,1480,300);
        check(support(game) == null && x(game)+32 < 1540, "Protected upper dock before enemy");
    }
    private static void reachNarrow(Main game, boolean partial, boolean exercise) throws Exception {
        reachUpper(game,partial,exercise);
        waitFor(game, () -> !enemy(game,1).isMovingRight() && enemy(game,1).getBounds().x <= 1580, 4, "Second enemy return");
        leap(game,1680,300); check(x(game) >= enemy(game,1).getBounds().x+30, "Second enemy evasion");
        leap(game,1900,260);
        Rectangle landing = ((StageDefinition) get(game,"stage")).getBlocks().get(2);
        check(player(game).getBounds().overlaps(new Rectangle(landing.x,landing.y+landing.height-1,landing.width,2)), "Standing on 100-unit ledge");
    }
    private static void reachTimed(Main game, boolean partial, boolean exercise) throws Exception {
        reachNarrow(game,partial,exercise);
        waitFor(game, () -> enemy(game,2).isMovingRight() && enemy(game,2).getBounds().x >= 2120, 3, "Narrow enemy landing window");
        leap(game,2050,320);
        check(x(game)+32 < enemy(game,2).getBounds().x, "Narrow landing clear of enemy");
        leap(game,2250,280);
        check(x(game) > enemy(game,2).getBounds().x+28, "Third enemy evasion to lower ledge");
    }
    private static void crossTimed(Main game, int index, float target) throws Exception {
        check(!player(game).overlaps(trap(game,index).getBounds()), "Safe waiting space " + index);
        waitFor(game, () -> trap(game,index).getState() == TrapState.SAFE
            && trap(game,index).getStateElapsedTime() < .08, 5, "Fresh SAFE " + index);
        boolean touched = false; right = true; int remaining = (int) (2/dt);
        while (x(game) < target && --remaining > 0 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)) {
            tick(game);
            if (player(game).overlaps(trap(game,index).getBounds())) {
                touched = true;
                check(trap(game,index).getState() == TrapState.SAFE && !trap(game,index).isDangerousTo(player(game)), "Actual SAFE crossing " + index);
            }
        }
        right = false;
        check(remaining > 0 && touched && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Safe crossing complete " + index);
    }
    private static void reachFinalShuttle(Main game, boolean partial, boolean exercise) throws Exception {
        reachTimed(game,partial,exercise);
        if (exercise) { waitFor(game, () -> trap(game,5).getState() == TrapState.ACTIVE, 5, "Observe first danger window"); verifyPause(game); }
        crossTimed(game,5,2470); crossTimed(game,6,2690); crossTimed(game,7,2900);
        waitFor(game, () -> finalShuttle(game).getBounds().x <= 3000, 5, "Last shuttle boarding window");
        jumpToSupport(game,3040,finalShuttle(game));
        if (exercise) { verifyRide(game,finalShuttle(game),6,100); verifyPause(game); }
        waitFor(game, () -> finalShuttle(game).isMovingForward() && finalShuttle(game).getBounds().x >= 3150
            && enemy(game,3).isMovingRight() && enemy(game,3).getBounds().x >= 3460, 20, "Observe final enemy and shuttle");
        move(game,finalShuttle(game).getBounds().x+100);
        leap(game,3400,300);
        check(support(game) == null && enemy(game,3).getBounds().x > x(game)+32, "Clear final dock landing");
    }
    private static void route(Main game, boolean partial, boolean exercise) throws Exception {
        frames = seenHud = 0;
        reachFinalShuttle(game,partial,exercise);
        leap(game,3550,300); check(x(game) > enemy(game,3).getBounds().x+28, "Fourth enemy evasion");
        move(game,3560); leap(game,3720,380); move(game,3760);
        int count = partial ? 9 : 10;
        check(state(game) == GameState.STAGE_CLEAR && number(game,"collectedItemCount") == count
            && results(game)[6].getItemScore() == count*5 && results(game)[6].getCollectedItemCount() == count,
            "Actual full/partial clear and recorded score; count=" + number(game,"collectedItemCount"));
        check(seenHud == (1 << (count+1))-1, "Every HUD count 0 through " + count);
        Array<?> items = (Array<?>) get(game,"gradeItems");
        for (int i = 0; i < items.size; i++) check(((GradeItem) items.get(i)).isCollected() == (!partial || i != 0), "Intended item " + i);
        System.out.printf("%d fps: Stage 7 %s, %d/10, %d points, %.2fs%n", Math.round(1/dt),
            partial ? "partial" : "all + transfer/endpoints/pause", count, count*5, frames*dt);
    }
    private static void verifyContacts() throws Exception {
        for (int i = 0; i < 4; i++) {
            Main game = newGame();
            if (i == 0) { firstEnemyWait(game,false); move(game,500); }
            if (i == 1) { reachUpper(game,false,false); move(game,1620); }
            if (i == 2) {
                reachNarrow(game,false,false);
                waitFor(game, () -> enemy(game,2).isMovingRight() && enemy(game,2).getBounds().x >= 2120, 3, "Narrow contact setup");
                leap(game,2050,320); move(game,2140);
            }
            if (i == 3) { reachFinalShuttle(game,false,false); move(game,3500); }
            check(LifeVerificationSupport.died(game) && LifeVerificationSupport.failedEnemy(game,i).touches(LifeVerificationSupport.failedPlayer(game)), "Actual enemy hit " + i);
            restart(game);
        }
        for (int i = 0; i < 5; i++) {
            Main game = newGame();
            if (i == 0) tick(game);
            if (i == 1) {
                firstEnemyWait(game,false);
                waitFor(game, () -> !enemy(game,0).isMovingRight() && enemy(game,0).getBounds().x <= 460, 4, "First enemy setup");
                leap(game,580,60);
            }
            if (i == 2) {
                reachUpper(game,false,false);
                waitFor(game, () -> !enemy(game,1).isMovingRight() && enemy(game,1).getBounds().x <= 1580, 4, "Second enemy setup");
                leap(game,1680,300);
            }
            if (i == 3) reachNarrow(game,false,false);
            if (i == 4) { reachFinalShuttle(game,false,false); leap(game,3550,300); }
            move(game,trap(game,i).getBounds().x+30);
            check(LifeVerificationSupport.died(game) && LifeVerificationSupport.failedTrap(game,i).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Actual fixed spike hit " + i);
            restart(game);
        }
        for (int index = 5; index < 8; index++) {
            for (boolean attemptJump : new boolean[] {false,true}) {
                Main game = newGame(); reachTimed(game,false,false);
                if (index >= 6) crossTimed(game,5,2470);
                if (index >= 7) crossTimed(game,6,2690);
                Rectangle b = trap(game,index).getBounds();
                if (attemptJump) move(game,b.x-33); // Deliberately late jump into visible spikes.
                final int selected = index;
                waitFor(game, () -> trap(game,selected).getState() == TrapState.ACTIVE
                    && trap(game,selected).getStateElapsedTime() < .08, 5, "Fresh ACTIVE " + index);
                jump = attemptJump;
                move(game,b.x+(attemptJump ? b.width+10 : 30));
                waitFor(game, () -> LifeVerificationSupport.died(game), 2, "Walk/late jump contacts visible ACTIVE spikes " + index);
                check(LifeVerificationSupport.failedTrap(game,index).isDangerousTo(LifeVerificationSupport.failedPlayer(game)), "Actual timed hit " + index);
                restart(game);
            }
        }
    }
    private static void jumpToSupport(Main game, float target, MovingPlatform p) throws Exception {
        check((Boolean) playerField(game,"grounded"), "Moving jump starts grounded");
        visible(game,target,p.getBounds().y+p.getBounds().height);
        jump = true; move(game,target);
        waitFor(game, () -> support(game) == p || state(game) != GameState.PLAYING, 2, "Moving platform landing");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p, "Actual boarding/transfer");
    }
    private static void visible(Main game, float target, float height) throws Exception {
        OrthographicCamera c = (OrthographicCamera) get(game,"camera");
        check(target >= c.position.x-400 && target+32 <= c.position.x+400 && height >= 0 && height+44 < 600,
            "Landing visible before jump: " + target);
        float low = Math.min(x(game),target), high = Math.max(x(game)+32,target+32);
        for (Object value : (Array<?>) get(game,"enemies")) {
            Rectangle b = ((PatrollingEnemy) value).getBounds();
            if (b.x < high && b.x+b.width > low && b.y < height+150)
                check(b.x >= c.position.x-400 && b.x+b.width <= c.position.x+400, "Path enemy visible before jump");
        }
        for (Object value : (Array<?>) get(game,"timedTraps")) {
            Rectangle b = ((TimedTrap) value).getBounds();
            if (b.x < high && b.x+b.width > low && b.y < height+150)
                check(b.x >= c.position.x-400 && b.x+b.width <= c.position.x+400, "Path spikes visible before jump");
        }
    }
    private static void verifyRide(Main game, MovingPlatform p, int seconds, float speed) throws Exception {
        float offset = x(game)-p.getBounds().x;
        boolean direction = p.isMovingForward(); int reversals = 0;
        float low = player(game).getY(), high = low;
        for (int i = 0; i < seconds/dt; i++) {
            tick(game);
            check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && support(game) == p
                && Math.abs(x(game)-p.getBounds().x-offset) < .05f
                && Math.abs(player(game).getY()-p.getBounds().y-p.getBounds().height) < .01f, "Stable moving rider");
            check(Math.abs(p.getDeltaX())+Math.abs(p.getDeltaY()) <= speed*dt+.01f, "Continuous endpoint reversal");
            if (direction != p.isMovingForward()) { reversals++; direction = p.isMovingForward(); }
            low = Math.min(low,player(game).getY()); high = Math.max(high,player(game).getY());
        }
        check(reversals >= 2, "Both platform endpoints with rider");
        if (p == elevator(game)) check(high-low > 175, "Full elevator ascent/descent");
    }
    private static void restart(Main game) throws Exception {
        check(LifeVerificationSupport.died(game), "Retry follows actual failure");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && number(game,"currentStageNumber") == 7
            && x(game) == 80 && player(game).getY() == 60 && number(game,"collectedItemCount") == 0
            && number(game,"stageScore") == 0, "Retry restores stage/spawn/counters");
        StageDefinition initial = StageFactory.createStage(7);
        for (Object item : (Array<?>) get(game,"gradeItems")) check(!((GradeItem) item).isCollected(), "All ten restored");
        for (int i = 0; i < 3; i++) check(moving(game,i).getBounds().equals(initial.getMovingPlatforms().get(i).getBounds()), "Platform reset " + i);
        for (int i = 0; i < 4; i++) {
            PatrollingEnemy e = initial.getEnemies().get(i);
            check(enemy(game,i).getBounds().equals(e.getBounds()) && enemy(game,i).isMovingRight() == e.isMovingRight(), "Enemy reset " + i);
        }
        for (int i = 0; i < 8; i++) {
            TimedTrap t = initial.getTimedTraps().get(i);
            check(trap(game,i).isFixed() == t.isFixed() && trap(game,i).getBounds().equals(t.getBounds())
                && trap(game,i).getState() == t.getState() && trap(game,i).getStateElapsedTime() == t.getStateElapsedTime(), "Hazard reset " + i);
        }
    }
    private static void verifyPause(Main game) throws Exception {
        Rectangle[] enemies = new Rectangle[4], platforms = new Rectangle[3]; boolean[] directions = new boolean[4];
        TrapState[] states = new TrapState[3]; double[] elapsed = new double[3];
        for (int i = 0; i < 4; i++) { enemies[i] = enemy(game,i).getBounds(); directions[i] = enemy(game,i).isMovingRight(); }
        for (int i = 0; i < 3; i++) { platforms[i] = moving(game,i).getBounds(); states[i] = trap(game,i+5).getState(); elapsed[i] = trap(game,i+5).getStateElapsedTime(); }
        float px = x(game), py = player(game).getY();
        escape = true; tick(game); for (int i = 0; i < 30; i++) tick(game);
        check(state(game) == GameState.PAUSED && x(game) == px && player(game).getY() == py, "Pause freezes player");
        for (int i = 0; i < 4; i++) check(enemy(game,i).getBounds().equals(enemies[i]) && enemy(game,i).isMovingRight() == directions[i], "Pause freezes enemy " + i);
        for (int i = 0; i < 3; i++) check(moving(game,i).getBounds().equals(platforms[i]) && trap(game,i+5).getState() == states[i]
            && trap(game,i+5).getStateElapsedTime() == elapsed[i], "Pause freezes platform and timer " + i);
        escape = true; tick(game); check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game), "Resume");
    }
    private static MovingPlatform moving(Main game, int i) throws Exception { return (MovingPlatform) ((Array<?>) get(game,"movingPlatforms")).get(i); }
    private static MovingPlatform elevator(Main game) throws Exception { return moving(game,1); }
    private static MovingPlatform finalShuttle(Main game) throws Exception { return moving(game,2); }
    private static PatrollingEnemy enemy(Main game, int i) throws Exception { return (PatrollingEnemy) ((Array<?>) get(game,"enemies")).get(i); }
    private static void leap(Main game, float target, float height) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Jump starts grounded");
        visible(game,target,height);
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
        LOAD.invoke(game, 7); return game;
    }
    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt); jump = enter = escape = false; frames++;
        if (number(game, "currentStageNumber") == 7) {
            int count = number(game, "collectedItemCount");
            check(count >= 0 && count <= 10 && ((Array<?>) get(game, "gradeItems")).size == 10
                && number(game, "stageScore") == count * 5, "Dynamic HUD 0-10 / score");
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
