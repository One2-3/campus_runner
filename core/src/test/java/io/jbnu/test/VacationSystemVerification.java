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

/** Real Main lifecycle/accounting fixtures and real Player input under all six modifiers. */
public final class VacationSystemVerification {
    private static boolean right, jump, enter, escape, successKey, failureKey;
    private static float dt;
    private static final Method UPDATE = method("update",float.class), LOAD = method("loadStage",int.class);
    private static final Method REPORT = method("getFinalReport");
    private static final int[] MAXIMA = {20,25,30,35,40,45,50,60};

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments) -> {
                    int key = arguments == null ? -1 : (Integer)arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.RIGHT && right;
                    if (method.getName().equals("isKeyJustPressed")) return (key == Input.Keys.SPACE && jump)
                        || (key == Input.Keys.ENTER && enter) || (key == Input.Keys.ESCAPE && escape)
                        || (key == Input.Keys.S && successKey) || (key == Input.Keys.F && failureKey);
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyManager();
            for (int fps : new int[]{30,60,120}) {
                dt = 1f/fps;
                verifyPlayerPhysics(PlayerModifier.NONE);
                for (VacationEventType type : VacationEventType.values()) {
                    verifyEvent(type,true,fps);
                    verifyEvent(type,false,fps);
                }
                verifyFullRun(true,false);
                verifyFullRun(false,false);
                verifyFullRun(true,true);
                System.out.printf("%d fps: vacation passed: three events/six outcomes, frozen world, no S/F result selection, "
                    + "both semesters, exact speed/jump, six-life exhaustion, no stacking, expiry, all-history reset, max 465/GPA 4.50.%n",fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyManager() {
        VacationManager manager = new VacationManager();
        check(!manager.complete(VacationEventType.CAFE,true), "No result without pending event");
        manager.beginAfterStage(2);
        check(!manager.complete(VacationEventType.CERTIFICATION,true), "Stale/wrong activity callback rejected");
        check(manager.complete(VacationEventType.CAFE,true), "One confirmed result");
        VacationResult result = manager.getResult(VacationEventType.CAFE);
        check(!manager.complete(VacationEventType.CAFE,false) && manager.getResult(VacationEventType.CAFE) == result,
            "Result cannot be overwritten");
        manager.enterStage(3); check(manager.modifierForStage(3).getExtraStartingLives() == 1, "Cafe effect enabled");
        manager.enterStage(4); check(manager.modifierForStage(4) == result.getModifier(), "Same immutable modifier in second semester");
        manager.enterStage(5); check(manager.modifierForStage(5) == PlayerModifier.NONE, "Expired even without a later event result");
        manager.enterStage(3); check(manager.modifierForStage(3) == PlayerModifier.NONE, "Expired modifier cannot reactivate");
        manager.reset();
        check(manager.getPending() == null && manager.getResult(VacationEventType.CAFE) == null, "History reset");
    }

    private static void verifyEvent(VacationEventType type, boolean success, int fps) throws Exception {
        Main game = newGame(type.getCompletedStage());
        Rectangle first = items(game).first().getBounds();
        player(game).reset(first.x,first.y); tick(game);
        // S/F pressed on the flag frame must not also finish the new event in that same update.
        successKey = failureKey = true;
        clear(game);
        int completed = type.getCompletedStage();
        check(state(game) == GameState.VACATION_EVENT && number(game,"currentStageNumber") == completed
            && manager(game).getPending() == type, "Correct vacation entered immediately after flag");
        StageResult saved = results(game)[completed-1];
        check(saved != null && saved.getItemScore() == 5 && saved.getSurvivalBonus() == 20
            && manager(game).getResult(type) == null, "Semester saved before event, result not yet chosen");
        String frozen = snapshot(game);
        for (int i = 0; i < fps; i++) { right = jump = escape = true; tick(game); }
        check(frozen.equals(snapshot(game)) && results(game)[completed-1] == saved && manager(game).getResult(type) == null,
            "Event freezes player, all obstacles, collection, counters and result recording");
        check(!game.completeVacationEvent(other(type),true), "Wrong callback cannot advance semester");
        successKey = failureKey = true; tick(game);
        check(frozen.equals(snapshot(game)) && state(game) == GameState.VACATION_EVENT
            && ((VacationMinigame)get(game,"vacationGame")).getPhase() == VacationMinigame.Phase.INTRO,
            "S/F cannot choose a result or start a minigame");
        right = false;
        VacationMinigame minigame = (VacationMinigame)get(game,"vacationGame");
        check(!game.completeVacationEvent(type,success), "Cannot bypass actual gameplay with a completion callback");
        MinigameTestDriver.finish(minigame,success,dt);
        check(!game.completeVacationEvent(type,!success), "Cannot substitute the opposite result");
        enter = true; tick(game);
        int firstStage = completed+1, initialLives = type == VacationEventType.CAFE && success ? 6 : 5;
        PlayerModifier modifier = player(game).getModifier();
        assertModifier(modifier,type,success);
        check(state(game) == GameState.PLAYING && number(game,"currentStageNumber") == firstStage
            && number(game,"lives") == initialLives && number(game,"semesterMaxLives") == initialLives
            && number(game,"stageScore") == 0 && number(game,"semesterDeathCount") == 0,
            "Choice starts next year exactly once with correct life cap");
        StageDefinition initial = StageFactory.createStage(firstStage);
        check(player(game).getCenterX()-16 == initial.getStartX() && player(game).getY() == initial.getStartY(),
            "Choice frame cannot also advance world physics");
        VacationResult chosen = manager(game).getResult(type);
        check(chosen.isSuccess() == success && chosen.getModifier() == modifier && results(game)[completed-1] == saved,
            "Immutable result applied without altering prior semester");
        check(!game.completeVacationEvent(type,!success), "Duplicate completion cannot change the result");
        for (int i = 0; i < 3; i++) { successKey = failureKey = true; tick(game); }
        check(player(game).getModifier() == modifier && manager(game).getResult(type) == chosen
            && number(game,"lives") == initialLives, "S/F during gameplay does not change effects or refill lives");
        verifyPlayerPhysics(modifier);
        // Verify the loaded Main player actually uses the modifier, not just a detached test player.
        float beforeX = player(game).getCenterX();
        right = true; tick(game); right = false;
        near(player(game).getCenterX()-beforeX,260*modifier.getMoveSpeedMultiplier()*dt,.001f,"Main effective speed");
        jump = true; tick(game);
        near(playerNumber(player(game),"velocityY"),650*modifier.getJumpPowerMultiplier()-1700*dt,.001f,"Main effective jump");
        int firstSemesterDeaths = type == VacationEventType.CAFE && success ? 5 : 1;
        for (int i = 0; i < firstSemesterDeaths; i++) die(game);
        check(number(game,"lives") == initialLives-firstSemesterDeaths && number(game,"semesterDeathCount") == firstSemesterDeaths
            && player(game).getModifier() == modifier && number(game,"stageScore") == 0,
            "Death replaces runtime but keeps modifier and remaining lives");
        clear(game);
        check(state(game) == GameState.STAGE_CLEAR && results(game)[firstStage-1].getSurvivalBonus() == (firstSemesterDeaths == 5 ? 0 : 15),
            "Odd semester has no vacation and bonus uses only semester deaths");
        enter = true; tick(game);
        check(number(game,"currentStageNumber") == firstStage+1 && number(game,"lives") == initialLives
            && number(game,"semesterDeathCount") == 0 && number(game,"totalDeathCount") == firstSemesterDeaths
            && player(game).getModifier() == modifier, "Same effect and fresh life cap in next semester");
        if (type == VacationEventType.CAFE && success) {
            for (int death = 1; death <= 5; death++) {
                die(game);
                check(state(game) == GameState.PLAYING && number(game,"lives") == 6-death
                    && number(game,"semesterDeathCount") == death && player(game).getModifier() == modifier,
                    "Six-life semester survives five deaths without refilling");
            }
            die(game);
            check(state(game) == GameState.GAME_OVER && number(game,"lives") == 0
                && number(game,"semesterDeathCount") == 6 && manager(game).getResult(type) == chosen,
                "Cafe bonus reaches GAME_OVER only on sixth death and retains history until Enter");
            enter = true; tick(game); assertReset(game);
        } else {
            clear(game);
            if (type != VacationEventType.HACKATHON) {
                check(state(game) == GameState.VACATION_EVENT
                    && manager(game).modifierForStage(firstStage+1) == PlayerModifier.NONE,
                    "Previous school-year effect ends when the year finishes");
                VacationEventType next = manager(game).getPending();
                VacationVerificationSupport.finishIfPending(game,!success);
                assertModifier(player(game).getModifier(),next,!success);
                check(number(game,"lives") == 5 && player(game).getModifier().getSource() != type,
                    "Stage 5 and later never inherit cafe extra lives or stack old multipliers");
            } else {
                check(state(game) == GameState.GAME_COMPLETE && results(game)[7] != null, "Stage 8 saves result and opens report, no vacation");
                jump = true; tick(game);
                check(state(game) == GameState.GRADUATION_ENDING, "Report still opens ending");
                enter = true; tick(game); assertReset(game);
            }
        }
    }

    private static void verifyPlayerPhysics(PlayerModifier modifier) throws Exception {
        right = jump = enter = escape = successKey = failureKey = false;
        Array<Rectangle> floor = new Array<>(); floor.add(new Rectangle(0,0,3000,60));
        Player probe = new Player(100,60,modifier);
        probe.update(dt,floor,3000);
        right = true;
        int frames = Math.round(1f/dt);
        for (int i = 0; i < frames; i++) probe.update(dt,floor,3000);
        right = false;
        near(probe.getCenterX()-16-100,260*modifier.getMoveSpeedMultiplier(),.02f,"One second of real movement input");
        jump = true; probe.update(dt,floor,3000); jump = false;
        near(playerNumber(probe,"velocityY"),650*modifier.getJumpPowerMultiplier()-1700*dt,.001f,"Jump impulse uses base power once");
        int remaining = frames*2;
        while (!(Boolean)playerField("grounded").get(probe) && --remaining > 0) probe.update(dt,floor,3000);
        check(remaining > 0 && probe.getY() == 60 && probe.getModifier() == modifier,"Modifier preserves normal landing and identity");
        check(playerField("MOVE_SPEED").getFloat(null) == 260 && playerField("JUMP_SPEED").getFloat(null) == 650
            && playerField("GRAVITY").getFloat(null) == 1700, "Base movement, jump and gravity constants unchanged");
    }

    private static void verifyFullRun(boolean success, boolean exhaustFinal) throws Exception {
        Main game = newGame(0); // Test the real TITLE_SCREEN -> new game initialization path.
        enter = true; tick(game); assertReset(game);
        for (int stage = 1; stage <= 8; stage++) {
            if (stage == 8 && exhaustFinal) {
                for (int i = 0; i < 5; i++) die(game);
                check(state(game) == GameState.GAME_OVER, "Final-year life exhaustion");
                for (VacationEventType type : VacationEventType.values()) check(manager(game).getResult(type) != null,
                    "All vacation history retained during GAME_OVER");
                enter = true; tick(game); assertReset(game); return;
            }
            Array<GradeItem> stageItems = items(game);
            for (int i = 0; i < stageItems.size; i++) {
                Rectangle bounds = stageItems.get(i).getBounds(); player(game).reset(bounds.x,bounds.y); tick(game);
            }
            clear(game);
            check(state(game) == VacationVerificationSupport.clearState(stage)
                && results(game)[stage-1].getItemScore() == MAXIMA[stage-1]
                && results(game)[stage-1].getSurvivalBonus() == 20, "Full-score confirmed semester with unchanged scoring");
            if (stage < 8) {
                if (stage % 2 == 0) VacationVerificationSupport.finishIfPending(game,success);
                else { enter = true; tick(game); }
                check(number(game,"currentStageNumber") == stage+1 && state(game) == GameState.PLAYING,
                    "Normal transition passes through required vacation only");
            }
        }
        FinalReport report = (FinalReport)REPORT.invoke(game);
        check(report.getTotalItemScore() == 305 && report.getTotalSurvivalBonus() == 160 && report.getFinalScore() == 465
            && report.getGpa() == 4.50 && report.getTotalDeathCount() == 0, "All success or all failure still permits maximum 465/GPA 4.50");
        for (VacationEventType type : VacationEventType.values()) check(manager(game).getResult(type).isSuccess() == success,
            "All three vacation results retained until restart");
        jump = true; tick(game);
        check(state(game) == GameState.GRADUATION_ENDING, "Report -> ending remains unchanged");
        enter = true; tick(game); assertReset(game);
    }

    private static void assertModifier(PlayerModifier modifier, VacationEventType type, boolean success) {
        float move = type == VacationEventType.CAFE ? (success ? 1 : .95f)
            : type == VacationEventType.HACKATHON ? (success ? 1.10f : .95f) : 1;
        float jumpPower = type == VacationEventType.CERTIFICATION ? (success ? 1.10f : .95f) : 1;
        check(modifier.getSource() == type && modifier.isSuccess() == success && modifier.getTargetYear() == type.getTargetYear()
            && modifier.getMoveSpeedMultiplier() == move && modifier.getJumpPowerMultiplier() == jumpPower
            && modifier.getExtraStartingLives() == (type == VacationEventType.CAFE && success ? 1 : 0), "Exact activity effect data");
    }

    private static void assertReset(Main game) throws Exception {
        check(state(game) == GameState.PLAYING && number(game,"currentStageNumber") == 1 && number(game,"lives") == 5
            && number(game,"semesterMaxLives") == 5 && number(game,"semesterDeathCount") == 0 && number(game,"totalDeathCount") == 0
            && number(game,"stageScore") == 0 && number(game,"collectedItemCount") == 0
            && player(game).getModifier() == PlayerModifier.NONE && manager(game).getPending() == null,
            "Full restart clears effects, counters and life cap at Stage 1");
        check(get(game,"vacationGame") == null,"Minigame state removed on full restart");
        for (VacationEventType type : VacationEventType.values()) check(manager(game).getResult(type) == null,"All activity results removed");
        for (StageResult result : results(game)) check(result == null,"All semester scores/bonuses removed");
        for (int deaths : (int[])get(game,"semesterDeaths")) check(deaths == 0,"All semester deaths removed");
    }

    private static String snapshot(Main game) throws Exception {
        StringBuilder s = new StringBuilder().append(player(game).getCenterX()).append('/').append(player(game).getY());
        for (Object p : (Array<?>)get(game,"movingPlatforms")) s.append(((MovingPlatform)p).getBounds());
        for (Object e : (Array<?>)get(game,"enemies")) s.append(((PatrollingEnemy)e).getBounds()).append(((PatrollingEnemy)e).isMovingRight());
        for (Object t : (Array<?>)get(game,"timedTraps")) s.append(((TimedTrap)t).getState()).append(((TimedTrap)t).getStateElapsedTime());
        for (GradeItem item : items(game)) s.append(item.isCollected());
        s.append(number(game,"stageScore")).append('/').append(number(game,"lives")).append('/').append(number(game,"totalDeathCount"));
        return s.toString();
    }
    private static void clear(Main game) throws Exception { Rectangle flag = (Rectangle)get(game,"flag"); player(game).reset(flag.x,flag.y); tick(game); }
    private static void die(Main game) throws Exception { player(game).reset(80,-150); tick(game); }
    private static Main newGame(int stage) throws Exception {
        right = jump = enter = escape = successKey = failureKey = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field("camera").set(game,camera); field("viewport").set(game,new FitViewport(800,600,camera));
        if (stage != 0) LOAD.invoke(game,stage);
        return game;
    }
    private static VacationEventType other(VacationEventType type) { return type == VacationEventType.CAFE ? VacationEventType.HACKATHON : VacationEventType.CAFE; }
    private static void tick(Main game) throws Exception { UPDATE.invoke(game,dt); jump = enter = escape = successKey = failureKey = false; }
    private static VacationManager manager(Main game) throws Exception { return (VacationManager)get(game,"vacationManager"); }
    private static Player player(Main game) throws Exception { return (Player)get(game,"player"); }
    private static StageResult[] results(Main game) throws Exception { return (StageResult[])get(game,"completedResults"); }
    private static GameState state(Main game) throws Exception { return (GameState)get(game,"gameState"); }
    @SuppressWarnings("unchecked") private static Array<GradeItem> items(Main game) throws Exception { return (Array<GradeItem>)get(game,"gradeItems"); }
    private static int number(Main game, String name) throws Exception { return (Integer)get(game,name); }
    private static float playerNumber(Player player, String name) throws Exception { return playerField(name).getFloat(player); }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static Field field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Field playerField(String name) throws Exception { Field f = Player.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?>... parameters) {
        try { Method m = Main.class.getDeclaredMethod(name,parameters); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void near(float actual, float expected, float tolerance, String message) {
        check(Math.abs(actual-expected) < tolerance,message+": "+actual+" / "+expected);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
