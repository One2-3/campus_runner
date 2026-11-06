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
import java.util.Locale;

/** Accounting fixtures use real Main updates; map navigation remains covered by the eight route suites. */
public final class FinalReportVerification {
    private static final int[] MAXIMA = {20,25,30,35,40,45,50,60};
    private static final int[] LOSSES = {1,2,3,4,0,1,2,3};
    private static final int[] BONUSES = {20,15,10,5,0};
    private static boolean enter, escape, space;
    private static float dt;
    private static final Method UPDATE = method("update", float.class);
    private static final Method LOAD = method("loadStage", int.class);
    private static final Method REPORT = method("getFinalReport");

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                (proxy, method, arguments) -> {
                    int key = arguments == null ? -1 : (Integer) arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.RIGHT;
                    if (method.getName().equals("isKeyJustPressed")) return (key == Input.Keys.ENTER && enter)
                        || (key == Input.Keys.ESCAPE && escape) || (key == Input.Keys.SPACE && space);
                    throw new UnsupportedOperationException(method.getName());
                });
            verifySummarySnapshot();
            for (int fps : new int[]{30,60,120}) {
                dt = 1f/fps;
                verifyCompletion(false);
                verifyCompletion(true);
                System.out.printf("%d fps: final report passed: eight saved rows, 305/160/465, GPA 4.50, "
                    + "partial items/death bonuses, actual TOTAL DEATHS, frozen completion, Enter clears all records.%n", fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifySummarySnapshot() {
        StageResult[] results = new StageResult[8];
        for (int i = 0; i < 8; i++) results[i] = new StageResult(i+1, MAXIMA[i], MAXIMA[i]/5, 0);
        FinalReport summary = new FinalReport(results, 0);
        results[7] = null;
        check(summary.getSemester(7).getItemScore() == 60 && summary.getFinalScore() == 465,
            "Report owns a snapshot of immutable saved results");
        try {
            new FinalReport(results, 0);
            throw new AssertionError("Missing final result must not silently produce a report");
        } catch (IllegalArgumentException expected) { /* incomplete eight-semester report rejected */ }
        StageResult[] zeroItems = new StageResult[8];
        for (int i = 0; i < 8; i++) zeroItems[i] = new StageResult(i+1, 0, 0, 4);
        FinalReport zero = new FinalReport(zeroItems, 32);
        check(zero.getFinalScore() == 0 && zero.getGpa() == 0 && zero.getScoreRate() == 0
            && zero.getTotalDeathCount() == 32, "Zero-score lower boundary preserves actual deaths");
    }

    private static void verifyCompletion(boolean partial) throws Exception {
        enter = escape = space = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field("camera").set(game, camera);
        field("viewport").set(game, new FitViewport(800,600,camera));
        LOAD.invoke(game, 1);
        int expectedDeaths = 0, expectedItems = 0, expectedBonus = 0;
        for (int stage = 1; stage <= 8; stage++) {
            check(REPORT.invoke(game) == null, "Report hidden during play");
            int deaths = partial ? LOSSES[stage-1] : 0;
            for (int i = 0; i < deaths; i++) { player(game).reset(80,-150); tick(game); }
            expectedDeaths += deaths;
            check(number(game,"totalDeathCount") == expectedDeaths, "Deaths persist through actual transitions");
            Array<?> items = (Array<?>)get(game,"gradeItems");
            int count = items.size - (partial ? 1 : 0);
            for (int i = 0; i < count; i++) {
                Rectangle bounds = ((GradeItem)items.get(i)).getBounds();
                player(game).reset(bounds.x,bounds.y); tick(game);
            }
            expectedItems += MAXIMA[stage-1] - (partial ? 5 : 0);
            expectedBonus += BONUSES[deaths];
            Rectangle flag = (Rectangle)get(game,"flag");
            player(game).reset(flag.x,flag.y); tick(game);
            check(get(game,"gameState") == VacationVerificationSupport.clearState(stage),
                "Normal flag completion, with Stage 8 saved before report entry");
            StageResult saved = ((StageResult[])get(game,"completedResults"))[stage-1];
            check(saved != null && saved.getItemScore() == MAXIMA[stage-1] - (partial ? 5 : 0)
                && saved.getDeathCount() == deaths && saved.getSurvivalBonus() == BONUSES[deaths], "Actual saved row");
            if (stage < 8) {
                check(REPORT.invoke(game) == null, "Report hidden between semesters");
                enter = true; tick(game);
                VacationVerificationSupport.finishIfPending(game, false);
                check(number(game,"semesterDeathCount") == 0 && number(game,"lives") == 5,
                    "Normal next-semester life rules preserved");
            }
        }
        FinalReport report = (FinalReport)REPORT.invoke(game);
        check(report != null && report.getSemesterCount() == 8, "GAME_COMPLETE exposes eight-row final report");
        check(report.getTotalItemScore() == expectedItems && report.getTotalSurvivalBonus() == expectedBonus
            && report.getFinalScore() == expectedItems+expectedBonus && report.getTotalDeathCount() == expectedDeaths,
            "Items and semester bonuses counted once, actual cumulative deaths disclosed");
        check(report.getTotalItemScore() == (partial ? 265 : 305) && report.getTotalSurvivalBonus() == (partial ? 80 : 160)
            && report.getFinalScore() == (partial ? 345 : 465) && report.getTotalDeathCount() == (partial ? 16 : 0),
            "Explicit full and partial reference totals");
        check(Math.abs(report.getGpa() - report.getFinalScore()/465.0*4.5) < 1e-12
            && Math.abs(report.getScoreRate() - report.getFinalScore()/465.0*100.0) < 1e-12, "Exact unrounded conversion");
        check(String.format(Locale.ROOT,"%.2f",report.getGpa()).equals(partial ? "3.34" : "4.50"), "Two-decimal GPA");
        StageResult[] saved = ((StageResult[])get(game,"completedResults")).clone();
        String frozen = runtimeSnapshot(game);
        for (int i = 0; i < 240; i++) tick(game);
        escape = true; tick(game);
        check(frozen.equals(runtimeSnapshot(game)) && number(game,"currentStageNumber") == 8
            && get(game,"gameState") == GameState.GAME_COMPLETE, "Movement input and ESC cannot advance completion runtime");
        FinalReport again = (FinalReport)REPORT.invoke(game);
        for (int i = 0; i < 8; i++) check(again.getSemester(i) == saved[i], "Repeated report reads never overwrite or duplicate results");
        check(again.getFinalScore() == report.getFinalScore() && again.getTotalDeathCount() == expectedDeaths,
            "Stable summary during completion");
        enter = true; tick(game);
        check(get(game,"gameState") == GameState.GAME_COMPLETE && frozen.equals(runtimeSnapshot(game)),
            "Report Enter cannot skip ending or erase results");
        space = enter = true; tick(game);
        check(get(game,"gameState") == GameState.GRADUATION_ENDING && frozen.equals(runtimeSnapshot(game)),
            "Simultaneous Space and Enter only opens ending");
        for (int i = 0; i < 240; i++) tick(game);
        escape = true; tick(game);
        check(get(game,"gameState") == GameState.GRADUATION_ENDING && frozen.equals(runtimeSnapshot(game)),
            "Ending freezes all runtime state too");
        check(((FinalReport)REPORT.invoke(game)).getTotalDeathCount() == expectedDeaths, "Ending discloses same actual deaths");
        enter = true; tick(game);
        check(REPORT.invoke(game) == null && get(game,"gameState") == GameState.PLAYING
            && number(game,"currentStageNumber") == 1 && number(game,"lives") == 5
            && number(game,"semesterDeathCount") == 0 && number(game,"totalDeathCount") == 0
            && number(game,"stageScore") == 0 && number(game,"collectedItemCount") == 0, "Enter starts a clean Stage 1");
        for (StageResult result : (StageResult[])get(game,"completedResults")) check(result == null, "All report records/bonuses removed");
        for (int deaths : (int[])get(game,"semesterDeaths")) check(deaths == 0, "All death histories removed");
        StageDefinition initial = StageFactory.createStage(1);
        check(player(game).getCenterX()-16 == initial.getStartX() && player(game).getY() == initial.getStartY(), "Fresh player spawn");
        for (Object item : (Array<?>)get(game,"gradeItems")) check(!((GradeItem)item).isCollected(), "Fresh items after report restart");
    }

    private static String runtimeSnapshot(Main game) throws Exception {
        StringBuilder s = new StringBuilder().append(player(game).getCenterX()).append('/').append(player(game).getY());
        for (Object object : (Array<?>)get(game,"movingPlatforms")) s.append(((MovingPlatform)object).getBounds());
        for (Object object : (Array<?>)get(game,"enemies")) {
            PatrollingEnemy enemy = (PatrollingEnemy)object;
            s.append(enemy.getBounds()).append(enemy.isMovingRight());
        }
        for (Object object : (Array<?>)get(game,"timedTraps")) {
            TimedTrap trap = (TimedTrap)object;
            s.append(trap.getState()).append('/').append(trap.getStateElapsedTime());
        }
        s.append('/').append(number(game,"totalDeathCount")).append('/').append(number(game,"stageScore"));
        return s.toString();
    }

    private static void tick(Main game) throws Exception { UPDATE.invoke(game,dt); enter = escape = space = false; }
    private static Player player(Main game) throws Exception { return (Player)get(game,"player"); }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static int number(Main game, String name) throws Exception { return (Integer)get(game,name); }
    private static Field field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?>... parameters) {
        try { Method m = Main.class.getDeclaredMethod(name,parameters); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
