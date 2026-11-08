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

/** Dependency-free integration checks using real Player/Main updates and synthetic keys. */
public final class FreshmanLevelVerification {
    private static boolean left, right, jump, enter, escape;
    private static final Method UPDATE = method("update", float.class);
    private static final Method LOAD = method("loadStage", int.class);
    private static int frames;
    private static float dt;

    public static void main(String[] args) throws Exception {
        Input previousInput = Gdx.input;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, arguments) -> {
                    int key = arguments == null ? -1 : (Integer) arguments[0];
                    if (method.getName().equals("isKeyPressed")) {
                        return (key == Input.Keys.RIGHT && right) || (key == Input.Keys.LEFT && left);
                    }
                    if (method.getName().equals("isKeyJustPressed")) {
                        return (key == Input.Keys.SPACE && jump)
                            || (key == Input.Keys.ENTER && enter)
                            || (key == Input.Keys.ESCAPE && escape);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyDefinitions();
            SpikeCollisionVerification.main(args);
            SeasonalBackgroundVerification.main(args);
            PlatformOutlineVerification.main(args);
            PixelHudVerification.main(args);
            PauseMenuVerification.main(args);
            GameOverVerification.main(args);
            StageClearVerification.main(args);
            FinalPixelUiVerification.main(args);
            VacationThemeVerification.main(args);
            VacationKoreanVerification.main(args);
            VacationPauseVerification.main(args);
            for (int fps : new int[] {30, 60, 120}) {
                verifyPhysics(fps);
                verifyRoute(fps, false);
                verifyRoute(fps, true);
                verifyGaps(fps);
            }
            verifyItemRetry();
            verifyProgression();
            Semester2LevelVerification.main(args);
            LifeSystemVerification.main(args);
            FinalReportVerification.main(args);
            GraduationEndingVerification.main(args);
            TitleScreenVerification.main(args);
            VacationSystemVerification.main(args);
            VacationMinigamesVerification.main(args);
            ServerConnectionVerification.main(args);
            PlayerPixelArtVerification.main(args);
            HitFeedbackVerification.main(args);
            System.out.println("Freshman verification passed: layout, 4 items / 20 points, "
                + "30/60/120 fps route, pause, retry, eight semesters and score retention.");
        } finally {
            Gdx.input = previousInput;
        }
    }

    private static void verifyPhysics(int fps) {
        dt = 1f / fps;
        left = right = jump = enter = escape = false;
        Array<Rectangle> floor = new Array<>();
        floor.add(new Rectangle(0, 0, 1000, 60));
        Player probe = new Player(80, 60);
        probe.update(dt, floor, 1000);
        right = jump = true;
        float peak = 60;
        int count = 0;
        do {
            probe.update(dt, floor, 1000);
            jump = false;
            peak = Math.max(peak, probe.getY());
            check(++count < fps * 2, "Jump returns to floor");
        } while (probe.getY() > 60);
        float distance = probe.getCenterX() - 16 - 80;
        check(peak - 60 > 110 && peak - 60 < 124.3f, "Actual jump height");
        check(distance > 180 && distance < 200, "Actual same-height jump range");
        System.out.printf("%d fps: jump rise %.2f, same-height travel %.2f%n", fps, peak - 60, distance);
        left = right = jump = false;
    }

    private static void verifyDefinitions() {
        StageDefinition freshman = StageFactory.createStage(1);
        check(freshman.getYear() == 1 && freshman.getSemester() == 1, "First semester identity");
        check(freshman.getWorldWidth() == 2400, "Compact world");
        check(freshman.getStartX() == 80 && freshman.getStartY() == 60, "Original freshman spawn");
        check(freshman.getFlag().equals(new Rectangle(2360, 360, 20, 100)), "Original freshman flag");
        check(freshman.getGradeItems().size == 4, "Exactly four items / maximum 20 points");
        check(freshman.getMovingPlatforms().size == 0 && freshman.getTimedTraps().size == 0
            && freshman.getEnemies().size == 0, "No freshman hazards");
        Array<Rectangle> blocks = freshman.getBlocks();
        float[][] freshmanBlocks = {
            {0, 0, 780, 60}, {300, 60, 140, 50}, {860, 0, 1040, 60},
            {1000, 130, 160, 20}, {1300, 120, 160, 20}, {1490, 200, 160, 20},
            {1800, 120, 150, 20}, {1980, 200, 140, 20}, {2140, 280, 100, 20},
            {2240, 0, 160, 360}
        };
        check(blocks.size == freshmanBlocks.length, "Freshman block count unchanged");
        for (int i = 0; i < blocks.size; i++) {
            float[] b = freshmanBlocks[i];
            check(blocks.get(i).equals(new Rectangle(b[0], b[1], b[2], b[3])),
                "Freshman block unchanged " + i);
        }
        for (GradeItem item : freshman.getGradeItems()) {
            check(item.getScore() == 5, "Five points per item");
            Rectangle bounds = item.getBounds();
            check(bounds.x >= 0 && bounds.x + bounds.width <= freshman.getWorldWidth(), "Item in world");
            for (Rectangle block : blocks) check(!bounds.overlaps(block), "Item outside solid blocks");
        }
        blocks.first().width = 1;
        check(freshman.getBlocks().first().width == 780, "Definition defensive copy");
        Array<GradeItem> items = freshman.getGradeItems();
        Player collector = new Player(130, 84);
        check(items.first().tryCollect(collector), "Runtime item collectible");
        check(!freshman.getGradeItems().first().isCollected()
            && !StageFactory.createStage(1).getGradeItems().first().isCollected(), "Independent items");

    }

    private static void verifyRoute(int fps, boolean fastRoute) throws Exception {
        dt = 1f / fps;
        frames = 0;
        left = right = jump = enter = escape = false;
        Main game = newGame();
        verifyHudData(game, 0, 4, 0);
        tick(game);

        player(game).reset(80, -150);
        tick(game);
        check(LifeVerificationSupport.died(game), "Freshman uses existing fall failure");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && player(game).getY() == 60
            && (Integer) get(game, "collectedItemCount") == 0, "Freshman retry resets runtime state");
        tick(game);

        escape = true;
        tick(game);
        float pausedX = player(game).getCenterX();
        right = true;
        tick(game);
        check(state(game) == GameState.PAUSED && player(game).getCenterX() == pausedX, "Pause freezes player");
        escape = true;
        tick(game);
        right = false;

        int routeStart = frames;
        walkTo(game, 240);
        leapTo(game, 340, 110);
        dropTo(game, 520, 60);
        walkTo(game, 720);
        leapTo(game, 890, 60);
        if (fastRoute) {
            walkTo(game, 1710); // Walk beneath all three optional item ledges.
        } else {
            walkTo(game, 940);
            leapTo(game, 1050, 150);
            walkTo(game, 1080);
            dropTo(game, 1200, 60);
            walkTo(game, 1240);
            leapTo(game, 1350, 140);
            walkTo(game, 1410);
            leapTo(game, 1540, 220);
            walkTo(game, 1570);
            dropTo(game, 1710, 60);
        }
        walkTo(game, 1740);
        leapTo(game, 1850, 140);
        walkTo(game, 1900);
        leapTo(game, 2030, 220);
        walkTo(game, 2070);
        leapTo(game, 2170, 300);
        leapTo(game, 2290, 360);
        walkTo(game, 2360);
        System.out.printf("%d fps: %s route = %.2f seconds%n", fps,
            fastRoute ? "fast" : "all-items", (frames - routeStart) * dt);
        check(state(game) == GameState.STAGE_CLEAR, "Flag clears freshman");
        int expectedItems = fastRoute ? 3 : 4;
        check((Integer) get(game, "collectedItemCount") == expectedItems
            && (Integer) get(game, "stageScore") == expectedItems * 5,
            "Route collection: expected " + expectedItems + ", actual " + get(game, "collectedItemCount"));
        Array<?> collected = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < collected.size; i++) {
            boolean optional = i == 2;
            check(((GradeItem) collected.get(i)).isCollected() == (!fastRoute || !optional),
                "Route collects intended item " + (i + 1));
        }
        verifyHudData(game, expectedItems, 4, expectedItems * 5);
        System.out.printf("%d fps: route collected %d/4 items, %d points%n",
            fps, expectedItems, expectedItems * 5);
        enter = true;
        tick(game);
        check((Integer) get(game, "currentStageNumber") == 2 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game),
            "Enter starts semester 2");
        check(((Array<?>) get(game, "movingPlatforms")).size == 1
            && ((Array<?>) get(game, "timedTraps")).size == 0
            && ((Array<?>) get(game, "enemies")).size == 0, "Semester 2 shuttle loaded without hazards");
        StageResult[] results = (StageResult[]) get(game, "completedResults");
        check(results[0].getItemScore() == expectedItems * 5 && results[0].getCollectedItemCount() == expectedItems, "Freshman result retained");
        check((Integer) get(game, "stageScore") == 0, "Next semester starts at zero");
        verifyHudData(game, 0, 5, 0);
        player(game).reset(80, -150);
        tick(game);
        check(LifeVerificationSupport.died(game), "Falling still causes game over");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && player(game).getY() == 60, "Enter retries current semester");
        check(results[0] == ((StageResult[]) get(game, "completedResults"))[0], "Retry preserves past result");
    }

    // renderHud reads these runtime values directly; no graphics context is needed here.
    private static void verifyHudData(Main game, int count, int total, int score) throws Exception {
        check((Integer) get(game, "collectedItemCount") == count
            && ((Array<?>) get(game, "gradeItems")).size == total
            && (Integer) get(game, "stageScore") == score,
            "HUD data: ITEMS: " + count + " / " + total + ", SCORE: " + score);
    }

    private static void verifyItemRetry() throws Exception {
        dt = 1f / 60;
        Main game = newGame();
        Array<?> previousItems = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < previousItems.size; i++) {
            Rectangle item = ((GradeItem) previousItems.get(i)).getBounds();
            player(game).reset(item.x, item.y);
            tick(game);
        }
        verifyHudData(game, 4, 4, 20);
        player(game).reset(80, -150);
        tick(game);
        check(LifeVerificationSupport.died(game), "Fail after collecting all four items");
        LifeVerificationSupport.verifyAutomaticRetry(game);
        verifyHudData(game, 0, 4, 0);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && (Integer) get(game, "currentStageNumber") == 1,
            "Retry stays in freshman semester");
        Array<?> restored = (Array<?>) get(game, "gradeItems");
        for (int i = 0; i < restored.size; i++) {
            GradeItem item = (GradeItem) restored.get(i);
            check(item != previousItems.get(i) && !item.isCollected()
                && item.getBounds().equals(((GradeItem) previousItems.get(i)).getBounds()),
                "Retry restores item " + i + " at its original position");
        }
    }

    private static void leapTo(Main game, float target, float height) throws Exception {
        check(grounded(game), "Jump starts from a landing");
        OrthographicCamera camera = (OrthographicCamera) get(game, "camera");
        check(target >= camera.position.x - 400 && target + 32 <= camera.position.x + 400
            && height >= 0 && height + 44 < 600, "Next landing visible before jumping");
        jump = true;
        dropTo(game, target, height);
    }

    private static void dropTo(Main game, float target, float height) throws Exception {
        moveTo(game, target);
        for (int f = 0; f < 2 / dt && !grounded(game); f++) tick(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && Math.abs(player(game).getY() - height) < 0.01f,
            "Landing target " + target + " at height " + height + ", actual "
                + (player(game).getCenterX() - 16) + ", " + player(game).getY());
    }

    private static void moveTo(Main game, float target) throws Exception {
        boolean goingRight = target > player(game).getCenterX() - 16;
        check(goingRight, "Route requires no backtracking at " + target);
        right = goingRight;
        left = !goingRight;
        int remaining = 3000;
        while (state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && --remaining > 0
            && (goingRight ? player(game).getCenterX() - 16 < target : player(game).getCenterX() - 16 > target)) {
            tick(game);
        }
        left = right = false;
        check(remaining > 0, "Movement reaches " + target);
    }

    private static boolean grounded(Main game) throws Exception {
        Field field = Player.class.getDeclaredField("grounded");
        field.setAccessible(true);
        return field.getBoolean(player(game));
    }

    private static void verifyGaps(int fps) throws Exception {
        dt = 1f / fps;
        // Isolated fixtures test broad takeoff windows; full routes above never teleport.
        for (float takeoff : new float[] {700, 720, 740}) {
            Main game = newGame();
            player(game).reset(takeoff, 60);
            tick(game);
            leapTo(game, 870, 60);
        }
        for (float takeoff : new float[] {1870, 1890, 1910}) {
            Main game = newGame();
            player(game).reset(takeoff, 140);
            tick(game);
            leapTo(game, 2000, 220);
        }
        for (float edge : new float[] {740, 1870}) {
            Main game = newGame();
            player(game).reset(edge, 60);
            tick(game);
            float middle = edge < 1000 ? 800 : 1920;
            moveTo(game, middle);
            for (int i = 0; i < fps * 2 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game); i++) tick(game);
            check(LifeVerificationSupport.died(game), "Unjumped floor gap causes GAME_OVER");
        }
        System.out.printf("%d fps: both gaps, forgiving takeoff windows and real falls passed%n", fps);
    }

    private static Main newGame() throws Exception {
        left = right = jump = enter = escape = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        set(game, "camera", camera);
        set(game, "viewport", new FitViewport(800, 600, camera));
        LOAD.invoke(game, 1);
        return game;
    }

    private static void verifyProgression() throws Exception {
        dt = 1f / 60;
        Main game = newGame();
        right = true;
        for (int i = 0; i < 600; i++) tick(game);
        check(state(game) != GameState.STAGE_CLEAR && player(game).getCenterX() < 600,
            "Right alone cannot clear the first obstacle");
        right = false;
        // Walk off a real open edge without jumping, rather than teleporting below the world.
        enter = true;
        tick(game);
        player(game).reset(740, 60);
        right = true;
        for (int i = 0; i < 120 && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game); i++) tick(game);
        check(LifeVerificationSupport.died(game), "Real gap causes game over");
        right = false;
        LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && (Integer) get(game, "currentStageNumber") == 1,
            "Retry stays in same semester");

        int total = 0;
        for (int number = 1; number <= 8; number++) {
            tick(game);
            // Exercise actual collection on each fresh runtime, then force flag contact only
            // for lifecycle coverage (later maps are intentionally not redesigned here).
            Array<?> items = (Array<?>) get(game, "gradeItems");
            int count = number % 3;
            for (int i = 0; i < count; i++) {
                Rectangle item = ((GradeItem) items.get(i)).getBounds();
                player(game).reset(item.x, item.y);
                tick(game);
            }
            check((Integer) get(game, "collectedItemCount") == count, "Independent semester collection");
            Rectangle flag = (Rectangle) get(game, "flag");
            player(game).reset(flag.x, flag.y);
            tick(game);
            total += count * 5;
            check(state(game) == VacationVerificationSupport.clearState(number),
                "Correct clear state for semester " + number);
            StageResult[] results = (StageResult[]) get(game, "completedResults");
            check(results.length == 8, "Eight result slots");
            for (int i = 0; i < number; i++) {
                check(results[i].getStageNumber() == i + 1 && results[i].getItemScore() == ((i + 1) % 3) * 5
                    && results[i].getCollectedItemCount() == (i + 1) % 3, "Retained semester " + (i + 1));
            }
            Method totalMethod = Main.class.getDeclaredMethod("getTotalScore");
            totalMethod.setAccessible(true);
            check((Integer) totalMethod.invoke(game) == LifeVerificationSupport.expectedTotal(game,total), "Total includes all completed semesters");
            if (number == 8) {
                VacationVerificationSupport.continueClear(game);
                jump = true; tick(game);
                check(state(game) == GameState.GRADUATION_ENDING, "Space opens graduation ending after report");
            }
            enter = true;
            tick(game);
            VacationVerificationSupport.finishIfPending(game, false);
            check((Integer) get(game, "currentStageNumber") == (number == 8 ? 1 : number + 1),
                "Enter advances or restarts");
            check((Integer) get(game, "stageScore") == 0 && (Integer) get(game, "collectedItemCount") == 0,
                "Fresh runtime counters");
            if (number < 8) {
                player(game).reset(80, -150);
                tick(game);
                LifeVerificationSupport.verifyAutomaticRetry(game);
                check((Integer) get(game, "currentStageNumber") == number + 1
                    && results[number - 1] != null, "Retry retains prior semester");
            }
        }
        for (StageResult result : (StageResult[]) get(game, "completedResults")) {
            check(result == null, "New game clears every result");
        }
        System.out.println("Eight-semester transitions, partial collection clears, totals and new-game reset passed.");
    }

    private static void walkTo(Main game, float x) throws Exception {
        right = true;
        int limit = 30000;
        while (player(game).getCenterX() - 16 < x && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && --limit > 0) tick(game);
        right = false;
        check(limit > 0, "Route must progress");
    }

    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt);
        jump = enter = escape = false;
        frames++;
    }

    private static Player player(Main game) throws Exception { return (Player) get(game, "player"); }
    private static GameState state(Main game) throws Exception { return (GameState) get(game, "gameState"); }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static void set(Main game, String name, Object value) throws Exception { field(name).set(game, value); }
    private static Field field(String name) throws Exception {
        Field field = Main.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static Method method(String name, Class<?> parameter) {
        try {
            Method method = Main.class.getDeclaredMethod(name, parameter);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
