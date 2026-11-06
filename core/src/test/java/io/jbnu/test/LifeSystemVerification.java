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

/** Controlled lifecycle fixtures plus repeated real-input falls; never edits lives/results/death counters. */
public final class LifeSystemVerification {
    private static boolean right, jump, enter, escape;
    private static float dt;
    private static final int[] ITEM_MAXIMA = {20,25,30,35,40,45,50,60};
    private static final int[] BONUSES = {20,15,10,5,0};
    private static final Method UPDATE = method("update",float.class), LOAD = method("loadStage",int.class);

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments) -> {
                    int key = arguments == null ? -1 : (Integer) arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.RIGHT && right;
                    if (method.getName().equals("isKeyJustPressed")) return (key == Input.Keys.SPACE && jump)
                        || (key == Input.Keys.ENTER && enter) || (key == Input.Keys.ESCAPE && escape);
                    throw new UnsupportedOperationException(method.getName());
                });
            for (int fps : new int[]{30,60,120}) {
                dt = 1f / fps;
                verifyStartsAndBonuses();
                for (int stage = 1; stage <= 8; stage++) verifyExhaustion(stage);
                verifyMaximum();
                verifyCumulativeDeaths();
                verifyPauseAndSimultaneousDamage();
                verifyRealInputExhaustion(fps);
                System.out.printf("%d fps: lives passed: eight starts, 0-4 death bonuses, every stage fifth-death wipe (including 1/4/8), "
                    + "all records/deaths reset, exact single-hit loss, real-input retries, max 465 and GAME_COMPLETE; "
                    + "TOTAL DEATHS accumulates across eight stages, survives GAME_OVER/completion, resets on new game.%n",fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyStartsAndBonuses() throws Exception {
        for (int stage = 1; stage <= 8; stage++) {
            for (int deaths = 0; deaths <= 4; deaths++) {
                Main game = newGame(stage);
                check(number(game,"lives") == 5 && number(game,"semesterDeathCount") == 0, "All stages start at five lives");
                check(number(game,"totalDeathCount") == 0, "Fresh attempt starts with zero total deaths");
                for (int i = 1; i <= deaths; i++) {
                    collectFirst(game);
                    dieByFall(game);
                    check(state(game) == GameState.PLAYING && number(game,"currentStageNumber") == stage
                        && number(game,"lives") == 5-i && number(game,"semesterDeathCount") == i, "Automatic same-stage retry");
                    verifyRuntimeReset(game);
                    check(number(game,"totalDeathCount") == i, "Automatic retry retains cumulative deaths");
                    check(results(game)[stage-1] == null && total(game) == 0, "No bonus recorded on death");
                }
                collectAll(game);
                check(number(game,"stageScore") == ITEM_MAXIMA[stage-1] && total(game) == 0,
                    "HUD item score excludes unconfirmed survival bonus");
                clear(game);
                StageResult result = results(game)[stage-1];
                check(result.getStageNumber() == stage && result.getItemScore() == ITEM_MAXIMA[stage-1]
                    && result.getCollectedItemCount() == ITEM_MAXIMA[stage-1]/5 && result.getDeathCount() == deaths
                    && result.getSurvivalBonus() == BONUSES[deaths] && result.getFinalScore() == ITEM_MAXIMA[stage-1]+BONUSES[deaths]
                    && result.getScore() == result.getFinalScore(), "All five bonus outcomes saved for every stage");
                check(total(game) == result.getFinalScore(), "Total includes confirmed final score");
                check(number(game,"totalDeathCount") == deaths, "Flag completion retains total deaths");
                for (int i = 0; i < 8; i++) tick(game);
                check(results(game)[stage-1] == result && total(game) == result.getFinalScore(), "Bonus cannot be added twice");
                enter = true; tick(game);
                if (stage < 8) {
                    check(number(game,"currentStageNumber") == stage+1 && number(game,"lives") == 5
                        && number(game,"semesterDeathCount") == 0 && results(game)[stage-1] == result,
                        "Normal advancement restores five lives and retains previous result");
                    check(number(game,"totalDeathCount") == deaths, "Advancement only resets semester deaths");
                } else verifyFullNewGame(game);
            }
        }
    }

    private static void verifyExhaustion(int target) throws Exception {
        Main game = newGame(1);
        for (int stage = 1; stage < target; stage++) {
            for (int death = 0; death < stage%5; death++) dieByFall(game);
            collectAll(game); clear(game); enter = true; tick(game);
        }
        check(number(game,"currentStageNumber") == target && number(game,"lives") == 5, "Requested exhaustion stage reached normally");
        StageResult[] prior = results(game).clone();
        int previousTotal = total(game);
        int previousDeaths = number(game,"totalDeathCount");
        for (int death = 1; death <= 5; death++) {
            collectFirst(game);
            Player failed = player(game);
            failed.reset(80,-150); tick(game);
            check(number(game,"lives") == 5-death && number(game,"semesterDeathCount") == death
                && ((int[])get(game,"semesterDeaths"))[target-1] == death, "Exactly one life/death per failure");
            check(number(game,"totalDeathCount") == previousDeaths + death, "Exactly one cumulative death per failure");
            for (int i = 0; i < target-1; i++) check(results(game)[i] == prior[i], "Deaths retain all prior records until new game");
            check(total(game) == previousTotal, "Current failures do not alter prior score or award bonuses");
            if (death < 5) {
                check(state(game) == GameState.PLAYING && player(game) != failed && number(game,"currentStageNumber") == target,
                    "First four failures automatically restart current stage without a key");
                verifyRuntimeReset(game);
                enter = true; tick(game);
                check(number(game,"lives") == 5-death && number(game,"semesterDeathCount") == death
                    && number(game,"currentStageNumber") == target, "Enter during play cannot replenish lives");
            } else {
                check(state(game) == GameState.GAME_OVER && player(game) == failed && number(game,"currentStageNumber") == target,
                    "Fifth failure freezes current runtime instead of restarting it");
                check(number(game,"stageScore") == 5 && number(game,"collectedItemCount") == 1,
                    "Fifth failure waits for restart input before wiping the attempt");
            }
        }
        for (int i = 0; i < 180; i++) tick(game);
        escape = true; tick(game);
        check(state(game) == GameState.GAME_OVER && number(game,"lives") == 0 && number(game,"semesterDeathCount") == 5
            && number(game,"currentStageNumber") == target && total(game) == previousTotal,
            "GAME_OVER cannot auto-advance, auto-retry or charge extra deaths");
        check(number(game,"totalDeathCount") == previousDeaths + 5, "GAME_OVER waiting retains cumulative deaths without duplicate charges");
        enter = true; tick(game);
        verifyFullNewGame(game);
    }

    private static void verifyFullNewGame(Main game) throws Exception {
        check(state(game) == GameState.PLAYING && number(game,"currentStageNumber") == 1
            && number(game,"lives") == 5 && number(game,"semesterDeathCount") == 0
            && number(game,"stageScore") == 0 && number(game,"collectedItemCount") == 0 && total(game) == 0,
            "Every exhausted stage restarts at Year 1 Semester 1 with zero total");
        check(number(game,"totalDeathCount") == 0, "New game clears total deaths");
        for (StageResult result : results(game)) check(result == null, "All item scores/bonuses/final scores removed");
        for (int death : (int[])get(game,"semesterDeaths")) check(death == 0, "All eight death histories removed");
        verifyRuntimeReset(game);
    }

    private static void verifyMaximum() throws Exception {
        Main game = newGame(1);
        int expectedTotal = 0;
        for (int stage = 1; stage <= 8; stage++) {
            check(number(game,"lives") == 5 && number(game,"semesterDeathCount") == 0, "No-death semester starts full");
            collectAll(game); clear(game);
            expectedTotal += ITEM_MAXIMA[stage-1]+20;
            check(total(game) == expectedTotal && results(game)[stage-1].getSurvivalBonus() == 20, "No-death confirmed result");
            if (stage < 8) { enter = true; tick(game); }
        }
        check(expectedTotal == 465 && total(game) == 465 && state(game) == GameState.GAME_COMPLETE
            && number(game,"currentStageNumber") == 8 && results(game).length == 8,
            "305 item points + 160 survival points = no-death maximum 465");
        for (int i = 0; i < 60; i++) tick(game);
        check(total(game) == 465 && state(game) == GameState.GAME_COMPLETE && number(game,"currentStageNumber") == 8,
            "Final completion never advances to Stage 9 or modifies score");
        check(number(game,"totalDeathCount") == 0, "No-death maximum keeps zero total deaths");
    }

    private static void verifyCumulativeDeaths() throws Exception {
        Main game = newGame(1);
        int[] semesterLosses = {1,2,3,4,0,1,2,3};
        int expectedDeaths = 0, expectedScore = 0;
        for (int stage = 1; stage <= 8; stage++) {
            check(number(game,"lives") == 5 && number(game,"semesterDeathCount") == 0
                && number(game,"totalDeathCount") == expectedDeaths, "New semester retains cumulative deaths only");
            for (int death = 0; death < semesterLosses[stage-1]; death++) {
                dieByFall(game);
                expectedDeaths++;
                check(number(game,"totalDeathCount") == expectedDeaths
                    && number(game,"semesterDeathCount") == death+1, "Both counters increment once across all stages");
            }
            collectAll(game); clear(game);
            StageResult result = results(game)[stage-1];
            expectedScore += ITEM_MAXIMA[stage-1] + BONUSES[semesterLosses[stage-1]];
            check(result.getDeathCount() == semesterLosses[stage-1]
                && result.getSurvivalBonus() == BONUSES[semesterLosses[stage-1]] && total(game) == expectedScore,
                "Survival bonus uses semester deaths, never total deaths");
            check(number(game,"totalDeathCount") == expectedDeaths, "Recording a result preserves cumulative deaths");
            if (stage < 8) { enter = true; tick(game); }
        }
        for (int i = 0; i < 180; i++) tick(game);
        escape = true; tick(game);
        check(expectedDeaths == 16 && number(game,"totalDeathCount") == 16 && expectedScore == 385
            && total(game) == 385 && state(game) == GameState.GAME_COMPLETE && number(game,"currentStageNumber") == 8,
            "GAME_COMPLETE retains all sixteen deaths and results without Stage 9");
        enter = true; tick(game); verifyFullNewGame(game);
    }

    @SuppressWarnings("unchecked")
    private static void verifyPauseAndSimultaneousDamage() throws Exception {
        Main game = newGame(4);
        player(game).reset(80,-150);
        escape = true; tick(game);
        for (int i = 0; i < 10; i++) tick(game);
        check(state(game) == GameState.PAUSED && number(game,"lives") == 5, "Pause prevents damage processing");
        check(number(game,"totalDeathCount") == 0, "Paused damage cannot increment total deaths");
        escape = true; tick(game); tick(game);
        check(state(game) == GameState.PLAYING && number(game,"lives") == 4 && number(game,"semesterDeathCount") == 1,
            "Resume charges the pending fall once");
        check(number(game,"totalDeathCount") == 1, "Resumed fall increments cumulative deaths once");
        verifyRuntimeReset(game);
        ((Array<TimedTrap>)get(game,"timedTraps")).add(TimedTrap.fixedSpikes(80,60,32,44));
        ((Array<PatrollingEnemy>)get(game,"enemies")).add(new PatrollingEnemy(80,60,28,28,70,100,10,true));
        tick(game);
        check(number(game,"lives") == 3 && number(game,"semesterDeathCount") == 2, "Simultaneous enemy and spikes cost one life");
        check(number(game,"totalDeathCount") == 2, "Simultaneous hazards increment cumulative deaths only once");
        verifyRuntimeReset(game);
    }

    private static void verifyRealInputExhaustion(int fps) throws Exception {
        Main game = newGame(1);
        for (int death = 1; death <= 5; death++) {
            tick(game); move(game,240);
            jump = true; move(game,340);
            waitFor(game, () -> grounded(game), 2, "Real obstacle landing");
            move(game,520);
            waitFor(game, () -> grounded(game) && player(game).getY() == 60, 2, "Real return to floor");
            move(game,800);
            final int expected = death;
            waitFor(game, () -> number(game,"semesterDeathCount") == expected, 2, "Real input gap fall");
            check(number(game,"lives") == 5-death, "Real-input failure costs one life");
            check(number(game,"totalDeathCount") == death, "Real-input fall increments total deaths once");
            if (death < 5) verifyRuntimeReset(game);
        }
        check(state(game) == GameState.GAME_OVER, "Five real-input falls reach GAME_OVER");
        enter = true; tick(game); verifyFullNewGame(game);
    }

    private static void verifyRuntimeReset(Main game) throws Exception {
        StageDefinition initial = StageFactory.createStage(number(game,"currentStageNumber"));
        Player player = player(game);
        check(player.getCenterX()-16 == initial.getStartX() && player.getY() == initial.getStartY()
            && number(game,"stageScore") == 0 && number(game,"collectedItemCount") == 0, "Runtime spawn/item counters reset");
        Array<?> items = (Array<?>) get(game,"gradeItems");
        check(items.size == initial.getGradeItems().size, "All stage items restored");
        for (int i = 0; i < items.size; i++) {
            GradeItem item = (GradeItem)items.get(i);
            check(!item.isCollected() && item.getBounds().equals(initial.getGradeItems().get(i).getBounds()), "Item reset " + i);
        }
        Array<?> platforms = (Array<?>)get(game,"movingPlatforms"), enemies = (Array<?>)get(game,"enemies"), traps = (Array<?>)get(game,"timedTraps");
        check(platforms.size == initial.getMovingPlatforms().size && enemies.size == initial.getEnemies().size
            && traps.size == initial.getTimedTraps().size, "Runtime collection sizes restored");
        for (int i = 0; i < platforms.size; i++) check(((MovingPlatform)platforms.get(i)).getBounds().equals(initial.getMovingPlatforms().get(i).getBounds()), "Platform reset");
        for (int i = 0; i < enemies.size; i++) {
            PatrollingEnemy actual = (PatrollingEnemy)enemies.get(i), expected = initial.getEnemies().get(i);
            check(actual.getBounds().equals(expected.getBounds()) && actual.isMovingRight() == expected.isMovingRight(), "Enemy position/direction reset");
        }
        for (int i = 0; i < traps.size; i++) {
            TimedTrap actual = (TimedTrap)traps.get(i), expected = initial.getTimedTraps().get(i);
            check(actual.getBounds().equals(expected.getBounds()) && actual.isFixed() == expected.isFixed()
                && actual.getState() == expected.getState() && actual.getStateElapsedTime() == expected.getStateElapsedTime(), "Hazard phase reset");
        }
    }

    // Lifecycle fixtures isolate score recording from navigation, whose real-key paths run separately.
    private static void collectFirst(Main game) throws Exception {
        Rectangle item = ((GradeItem)((Array<?>)get(game,"gradeItems")).first()).getBounds();
        player(game).reset(item.x,item.y); tick(game);
        check(number(game,"stageScore") == 5 && number(game,"collectedItemCount") == 1, "Actual first-item collection");
    }
    private static void collectAll(Main game) throws Exception {
        Array<?> items = (Array<?>)get(game,"gradeItems");
        for (int i = 0; i < items.size; i++) {
            Rectangle item = ((GradeItem)items.get(i)).getBounds(); player(game).reset(item.x,item.y); tick(game);
            check(state(game) == GameState.PLAYING && number(game,"semesterDeathCount") < 5, "Collection fixture remains alive");
        }
        check(number(game,"stageScore") == ITEM_MAXIMA[number(game,"currentStageNumber")-1], "All actual items collected");
    }
    private static void clear(Main game) throws Exception {
        Rectangle flag = (Rectangle)get(game,"flag"); player(game).reset(flag.x,flag.y); tick(game);
        check(state(game) == (number(game,"currentStageNumber") == 8 ? GameState.GAME_COMPLETE : GameState.STAGE_CLEAR), "Normal flag completion");
    }
    private static void dieByFall(Main game) throws Exception { player(game).reset(80,-150); tick(game); }
    private static boolean grounded(Main game) throws Exception { return (Boolean)field(Player.class,"grounded").get(player(game)); }
    private interface Condition { boolean ready() throws Exception; }
    private static void waitFor(Main game, Condition condition, int seconds, String message) throws Exception {
        int remaining = (int)(seconds/dt)+1; while (!condition.ready() && --remaining > 0) tick(game);
        check(remaining > 0,message);
    }
    private static void move(Main game, float target) throws Exception {
        right = true; waitFor(game, () -> player(game).getCenterX()-16 >= target || state(game) != GameState.PLAYING, 3,"Real input move " + target); right = false;
    }
    private static Main newGame(int stage) throws Exception {
        right = jump = enter = escape = false; Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field(Main.class,"camera").set(game,camera); field(Main.class,"viewport").set(game,new FitViewport(800,600,camera));
        LOAD.invoke(game,stage); return game;
    }
    private static void tick(Main game) throws Exception { UPDATE.invoke(game,dt); jump = enter = escape = false; }
    private static Object get(Main game, String name) throws Exception { return field(Main.class,name).get(game); }
    private static int number(Main game, String name) throws Exception { return (Integer)get(game,name); }
    private static Player player(Main game) throws Exception { return (Player)get(game,"player"); }
    private static GameState state(Main game) throws Exception { return (GameState)get(game,"gameState"); }
    private static StageResult[] results(Main game) throws Exception { return (StageResult[])get(game,"completedResults"); }
    private static int total(Main game) throws Exception { Method m = Main.class.getDeclaredMethod("getTotalScore"); m.setAccessible(true); return (Integer)m.invoke(game); }
    private static Field field(Class<?> type, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?> parameter) { try { Method m = Main.class.getDeclaredMethod(name,parameter); m.setAccessible(true); return m; } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); } }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
