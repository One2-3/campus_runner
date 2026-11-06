package io.jbnu.test;

import com.badlogic.gdx.utils.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/** Keeps collision evidence after Main has automatically replaced a failed runtime. */
final class LifeVerificationSupport {
    private static final Map<Main, Failure> failures = new IdentityHashMap<>();

    private static final class Failure {
        Player player;
        Array<TimedTrap> traps;
        Array<PatrollingEnemy> enemies;
        int stage, deaths, totalDeaths, lives, collected;
        float delta;
    }

    @SuppressWarnings("unchecked")
    static void update(Main game, Method update, float dt) throws Exception {
        Failure candidate = new Failure();
        candidate.player = (Player) get(game, "player");
        candidate.traps = (Array<TimedTrap>) get(game, "timedTraps");
        candidate.enemies = (Array<PatrollingEnemy>) get(game, "enemies");
        candidate.stage = number(game, "currentStageNumber");
        int beforeDeaths = number(game, "semesterDeathCount"), beforeLives = number(game, "lives");
        int beforeTotalDeaths = number(game, "totalDeathCount");
        Array<GradeItem> items = (Array<GradeItem>) get(game, "gradeItems");
        GameState beforeState = (GameState)get(game,"gameState");
        update.invoke(game, dt);
        if (beforeState == GameState.PLAYING && get(game,"gameState") == GameState.DYING) {
            candidate.deaths = beforeDeaths+1; candidate.totalDeaths = beforeTotalDeaths+1; candidate.lives = beforeLives-1;
            candidate.delta = dt;
            for (GradeItem item : items) if (item.isCollected()) candidate.collected++;
            check(number(game,"lives") == beforeLives && number(game,"semesterDeathCount") == beforeDeaths
                && number(game,"totalDeathCount") == beforeTotalDeaths,"Contact enters DYING without an early life/death charge");
            failures.put(game,candidate);
            return;
        }
        if (number(game, "currentStageNumber") == candidate.stage
            && number(game, "semesterDeathCount") == beforeDeaths + 1) {
            candidate.deaths = beforeDeaths + 1;
            candidate.totalDeaths = beforeTotalDeaths + 1;
            candidate.lives = beforeLives - 1;
            for (GradeItem item : items) if (item.isCollected()) candidate.collected++;
            check(number(game, "lives") == candidate.lives, "Exactly one life lost on collision frame");
            check(number(game, "totalDeathCount") == candidate.totalDeaths, "Exactly one cumulative death on collision frame");
            if (candidate.lives > 0) {
                check(get(game, "gameState") == GameState.PLAYING && get(game, "player") != candidate.player,
                    "Death has already automatically replaced the current runtime");
            } else check(get(game, "gameState") == GameState.GAME_OVER, "Fifth death stops gameplay");
            failures.put(game, candidate);
        }
    }

    static boolean died(Main game) { return failures.containsKey(game); }
    static Player failedPlayer(Main game) { return failure(game).player; }
    static TimedTrap failedTrap(Main game, int index) { return failure(game).traps.get(index); }
    static PatrollingEnemy failedEnemy(Main game, int index) { return failure(game).enemies.get(index); }
    static int failedCollected(Main game) { return failure(game).collected; }

    /** Assert that the runtime has already reset, then acknowledge the observed death event. */
    static void verifyAutomaticRetry(Main game) throws Exception {
        Failure f = failure(game);
        if (get(game,"gameState") == GameState.DYING) {
            Method update = Main.class.getDeclaredMethod("update",float.class); update.setAccessible(true);
            int limit = (int)Math.ceil(1f/f.delta)+2;
            while (get(game,"gameState") == GameState.DYING && --limit > 0) update.invoke(game,f.delta);
            check(limit > 0,"Contact feedback finishes through actual timed updates");
        }
        StageDefinition stage = (StageDefinition) get(game, "stage");
        Player player = (Player) get(game, "player");
        check(f.lives > 0 && get(game, "gameState") == GameState.PLAYING
            && number(game, "currentStageNumber") == f.stage && number(game, "lives") == f.lives
            && number(game, "semesterDeathCount") == f.deaths, "Automatic retry keeps stage/lives/deaths");
        check(number(game, "totalDeathCount") == f.totalDeaths, "Automatic retry keeps cumulative deaths");
        check(player.getCenterX() - 16 == stage.getStartX() && player.getY() == stage.getStartY()
            && number(game, "stageScore") == 0 && number(game, "collectedItemCount") == 0,
            "Runtime and item score reset before any retry key");
        failures.remove(game);
    }

    static int expectedTotal(Main game, int expectedItems) throws Exception {
        int items = 0, expectedFinal = 0;
        StageResult[] results = (StageResult[]) get(game, "completedResults");
        int[] deaths = (int[]) get(game, "semesterDeaths");
        int totalDeaths = 0;
        for (int count : deaths) totalDeaths += count;
        check(number(game, "totalDeathCount") == totalDeaths, "Cumulative deaths match all semester histories");
        for (StageResult result : results) if (result != null) {
            items += result.getItemScore();
            int count = deaths[result.getStageNumber() - 1];
            check(result.getDeathCount() == count && result.getSurvivalBonus() == Math.max(0, 20 - count * 5),
                "Level result retains actual deaths and correct survival bonus");
            check(result.getScore() == result.getFinalScore()
                && result.getFinalScore() == result.getItemScore() + result.getSurvivalBonus(), "Final score accounting");
            expectedFinal += result.getItemScore() + Math.max(0, 20 - count * 5);
        }
        check(items == expectedItems, "Original level item-score total unchanged: " + items + " / " + expectedItems);
        return expectedFinal;
    }

    private static Failure failure(Main game) {
        Failure f = failures.get(game);
        if (f == null) throw new AssertionError("Expected an actual observed death");
        return f;
    }
    private static Object get(Main game, String name) throws Exception {
        Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f.get(game);
    }
    private static int number(Main game, String name) throws Exception { return (Integer) get(game, name); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
