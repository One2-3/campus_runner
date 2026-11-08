package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact grading boundaries plus screen-input fixtures for all five post-completion results. */
public final class GraduationEndingVerification {
    private static boolean enter, space, escape;
    private static final Method UPDATE = method("update", float.class), LOAD = method("loadStage", int.class);
    private static final Method REPORT = method("getFinalReport");

    public static void main(String[] args) throws Exception {
        verifyBoundaries();
        verifyKoreanFont();
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                (proxy, method, arguments) -> {
                    int key = arguments == null ? -1 : (Integer)arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.RIGHT;
                    if (method.getName().equals("isKeyJustPressed")) return (key == Input.Keys.ENTER && enter)
                        || (key == Input.Keys.SPACE && space) || (key == Input.Keys.ESCAPE && escape);
                    throw new UnsupportedOperationException(method.getName());
                });
            int[] scores = {465,400,345,250,100};
            for (int fps : new int[]{30,60,120}) {
                for (int i = 0; i < scores.length; i++) verifyScreens(scores[i], GraduationEnding.values()[i], 1f/fps);
                System.out.printf("%d fps: five graduation endings passed: exact 0..465 boundaries, Korean glyphs, "
                    + "report -> Space -> ending -> Enter, simultaneous-input guard, frozen worlds, retained scores/deaths and full reset.%n", fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyBoundaries() {
        for (int score = 0; score <= 465; score++) {
            GraduationEnding expected = score < 207 ? GraduationEnding.DEFERRED
                : score < 310 ? GraduationEnding.BARELY_GRADUATED
                : score < 362 ? GraduationEnding.GRADUATED
                : score < 414 ? GraduationEnding.HONORS : GraduationEnding.HIGHEST_HONORS;
            check(GraduationEnding.fromScore(score) == expected, "Exact integer boundary at score " + score);
        }
        check(String.format(Locale.ROOT,"%.2f",413/465.0*4.5).equals("4.00")
            && GraduationEnding.fromScore(413) == GraduationEnding.HONORS, "Rounded 4.00 cannot promote raw GPA below 4.00");
        for (int score : new int[]{0,100,205,210,305,310,345,360,365,400,410,415,465}) {
            FinalReport report = reportForScore(score);
            check(report.getFinalScore() == score && report.getEnding() == GraduationEnding.fromScore(score)
                && Math.abs(report.getGpa()-score/465.0*4.5) < 1e-12, "FinalReport score/GPA/ending remain consistent");
        }
        GraduationEnding deferred = GraduationEnding.DEFERRED;
        check(!deferred.isGraduated() && deferred.getTitle().equals("졸업 유예")
            && deferred.getMessage().equals("아직 졸업할 수 없습니다.")
            && deferred.getDetail().equals("학점이 부족합니다. 다시 도전해 보세요!"), "Deferred copy is failure, without a success message");
        for (GraduationEnding ending : GraduationEnding.values()) {
            check(ending.isGraduated() == (ending != deferred), "Only deferral is unsuccessful graduation");
            for (GraduationEnding other : GraduationEnding.values()) if (ending != other) {
                check(ending.getColorRgb() != other.getColorRgb(), "Every ending has a distinct accent color");
            }
        }
    }

    private static void verifyScreens(int score, GraduationEnding expected, float dt) throws Exception {
        enter = space = escape = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field("camera").set(game,camera);
        field("viewport").set(game,new FitViewport(800,600,camera));
        LOAD.invoke(game,8);
        // This fixture starts after completion. Actual eight-semester flag entry is covered by FinalReportVerification.
        FinalReport fixture = reportForScore(score);
        StageResult[] stored = (StageResult[])get(game,"completedResults");
        int[] history = (int[])get(game,"semesterDeaths");
        for (int i = 0; i < 8; i++) { stored[i] = fixture.getSemester(i); history[i] = stored[i].getDeathCount(); }
        field("totalDeathCount").setInt(game,fixture.getTotalDeathCount());
        field("gameState").set(game,GameState.GAME_COMPLETE);
        Object player = get(game,"player"), platforms = get(game,"movingPlatforms"), enemies = get(game,"enemies"), traps = get(game,"timedTraps");
        enter = true; tick(game,dt);
        check(get(game,"gameState") == GameState.GAME_COMPLETE && stored[7] != null, "Report Enter cannot skip ending or erase records");
        space = enter = escape = true; tick(game,dt);
        check(get(game,"gameState") == GameState.GRADUATION_ENDING, "Space + Enter only opens ending, without immediate restart");
        for (int frame = 0; frame < 180; frame++) { space = true; escape = true; tick(game,dt); }
        FinalReport report = (FinalReport)REPORT.invoke(game);
        check(report.getEnding() == expected && report.getFinalScore() == score
            && report.getTotalDeathCount() == fixture.getTotalDeathCount(), "Ending uses same exact grade, score, and cumulative deaths");
        check(get(game,"gameState") == GameState.GRADUATION_ENDING && get(game,"player") == player
            && get(game,"movingPlatforms") == platforms && get(game,"enemies") == enemies && get(game,"timedTraps") == traps
            && (Integer)get(game,"currentStageNumber") == 8, "Ending cannot retry, enter GAME_OVER, or create Stage 9");
        for (int i = 0; i < 8; i++) check(stored[i] == fixture.getSemester(i), "All semester results retained through ending");
        enter = true; tick(game,dt);
        check(get(game,"gameState") == GameState.PLAYING && (Integer)get(game,"currentStageNumber") == 1
            && (Integer)get(game,"lives") == 5 && (Integer)get(game,"semesterDeathCount") == 0
            && (Integer)get(game,"totalDeathCount") == 0 && (Integer)get(game,"stageScore") == 0
            && (Integer)get(game,"collectedItemCount") == 0 && REPORT.invoke(game) == null, "Ending Enter fully resets Stage 1 and screen state");
        for (StageResult result : stored) check(result == null, "All item, bonus, final-score records removed");
        for (int deaths : history) check(deaths == 0, "All semester death histories removed");
        check(get(game,"player") != player && get(game,"movingPlatforms") != platforms
            && get(game,"enemies") != enemies && get(game,"timedTraps") != traps, "Restart replaces runtime objects");
    }

    private static FinalReport reportForScore(int score) {
        int[] maxima = {20,25,30,35,40,45,50,60};
        StageResult[] results = new StageResult[8];
        int remaining = score, deaths = 0;
        for (int i = 0; i < 8; i++) {
            int semesterScore = Math.min(remaining, maxima[i]+20);
            int items = Math.min(semesterScore,maxima[i]);
            int bonus = semesterScore-items;
            int losses = (20-bonus)/5;
            results[i] = new StageResult(i+1,items,items/5,losses);
            deaths += losses;
            remaining -= semesterScore;
        }
        check(remaining == 0, "Fixture fits within real semester score maxima");
        return new FinalReport(results,deaths);
    }

    private static void verifyKoreanFont() throws Exception {
        Path assets = Paths.get("assets/fonts");
        if (!Files.isDirectory(assets)) assets = Paths.get("../assets/fonts");
        String fnt = new String(Files.readAllBytes(assets.resolve("ending-ko.fnt")),StandardCharsets.UTF_8);
        check(fnt.contains("face=\"Campus Ending Korean\""), "Ending uses renamed OFL derivative");
        String license = new String(Files.readAllBytes(assets.resolve("ending-ko-OFL.txt")),StandardCharsets.UTF_8);
        check(license.contains("SIL OPEN FONT LICENSE Version 1.1") && license.contains("NHN Corporation"),
            "Ending font ships the original copyright and OFL notice");
        Map<Character,Integer> advances = new HashMap<>();
        Matcher matcher = Pattern.compile("char id=(\\d+).*?xadvance=(\\d+)").matcher(fnt);
        while (matcher.find()) advances.put((char)Integer.parseInt(matcher.group(1)),Integer.parseInt(matcher.group(2)));
        check(Files.size(assets.resolve("ending-ko.png")) > 0, "Bundled Korean bitmap atlas exists");
        for (GraduationEnding ending : GraduationEnding.values()) {
            checkText(advances, ending.getTitle(),1.5f);
            checkText(advances, ending.getMessage(),0.85f);
            checkText(advances, ending.getDetail(),0.85f);
        }
        checkText(advances,GraduationEnding.PRECISION_NOTE,0.65f);
    }

    private static void checkText(Map<Character,Integer> advances, String text, float scale) {
        int width = 0;
        for (char c : text.toCharArray()) {
            check(advances.containsKey(c), "Korean bitmap font has glyph for " + c);
            width += advances.get(c);
        }
        check((width+32)*scale <= 700, "Ending copy fits the 700-unit text area: " + text);
    }

    private static void tick(Main game, float dt) throws Exception { UPDATE.invoke(game,dt); enter = space = escape = false; }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static Field field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?>... parameters) {
        try { Method m = Main.class.getDeclaredMethod(name,parameters); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
