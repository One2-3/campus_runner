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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Rule tests use normal game updates and key snapshots only; no score/order/timer mutations. */
public final class VacationMinigamesVerification {
    private static float dt;
    private static MinigameInput input = MinigameInput.NONE;
    private static final Method UPDATE = method("update",float.class), LOAD = method("loadStage",int.class);

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments) -> {
                    int key = arguments == null ? -1 : (Integer)arguments[0];
                    if (method.getName().equals("isKeyPressed")) return key == Input.Keys.LEFT ? input.isLeftHeld()
                        : key == Input.Keys.RIGHT && input.isRightHeld();
                    if (method.getName().equals("isKeyJustPressed")) return key == Input.Keys.S || key == Input.Keys.F || input.isPressed(key);
                    throw new UnsupportedOperationException(method.getName());
                });
            for (int fps : new int[]{30,60,120}) {
                dt = 1f/fps;
                verifyCafe(); verifyCertification();
                for (VacationEventType type : VacationEventType.values()) {
                    verifyConnection(type,true); verifyConnection(type,false);
                }
                // Reproducible balance checks: thirty different falling-object layouts, both outcomes.
                for (int seed = 0; seed < 30; seed++) {
                    MinigameTestDriver.finish(new CafeMinigame(new Random(seed)),true,dt);
                    MinigameTestDriver.finish(new CafeMinigame(new Random(seed)),false,dt);
                }
                System.out.printf("%d fps: real minigames passed: coffee/trash/floor/misses/removal/30s/100, "
                    + "memory 5 rounds/3 mistakes/40s, server-circuit integration, fresh-key guards, "
                    + "six Main outcomes, frozen world, result Enter, no S/F bypass, 30 seeded cafe layouts.%n",fps);
            }
        } finally { Gdx.input = previous; }
    }

    private static void verifyCafe() {
        CafeMinigame cafe = centeredCafe();
        start(cafe);
        int coffee = 0, trash = 0, expectedScore = 0;
        boolean reachedEarly = false;
        while (!cafe.isFinished()) {
            cafe.update(dt,MinigameInput.NONE);
            int newCoffee = cafe.getCoffeeCaught()-coffee, newTrash = cafe.getTrashCaught()-trash;
            // Objects are separated in this fixed-column layout, so score events occur individually.
            check(newCoffee+newTrash <= 1,"Individual collision events in controlled layout");
            expectedScore = Math.max(0,expectedScore+newCoffee*10-newTrash*10);
            check(cafe.getScore() == expectedScore,"Coffee +10, trash -10 and zero floor");
            coffee = cafe.getCoffeeCaught(); trash = cafe.getTrashCaught();
            if (cafe.getScore() >= 100 && cafe.getTimeRemaining() > 1) {
                reachedEarly = true;
                check(!cafe.isFinished(),"Reaching target does not end cafe before thirty seconds");
            }
            check(cafe.getSpawnedCount() == cafe.getRemovedCount()+cafe.getDrops().size() && cafe.getDrops().size() < 10,
                "Caught and offscreen objects are removed normally");
        }
        check(reachedEarly && coffee >= 10 && trash > 0 && cafe.isSuccess() && cafe.getTimeRemaining() == 0,
            "Thirty-second successful cafe with sufficient coffee supply");
        int score = cafe.getScore(); cafe.update(60,MinigameTestDriver.key(Input.Keys.ENTER));
        check(cafe.isSuccess() && cafe.getScore() == score,"Cafe result cannot be recalculated or restarted");

        CafeMinigame missed = centeredCafe(); start(missed);
        while (!missed.isFinished()) missed.update(dt,new MinigameInput(true,false));
        check(!missed.isSuccess() && missed.getScore() == 0 && missed.getMissedCoffee() > 20
            && missed.getCoffeeCaught() == 0,"Missed coffee has no penalty or reward; under target fails at 30s");

        CafeMinigame zeroTrash = centeredCafe(); start(zeroTrash);
        for (int i = 0; i < Math.ceil(.2/dt); i++) zeroTrash.update(dt,new MinigameInput(true,false));
        List<CafeMinigame.Drop> firstCoffee = new ArrayList<>();
        while (firstCoffee.size() < 3 || firstCoffee.get(2).getY()+CafeMinigame.DROP_HEIGHT >= CafeMinigame.TRAY_Y) {
            zeroTrash.update(dt,MinigameInput.NONE);
            for (CafeMinigame.Drop drop : zeroTrash.getDrops()) if (drop.isCoffee() && firstCoffee.size() < 3 && !firstCoffee.contains(drop)) firstCoffee.add(drop);
        }
        check(zeroTrash.getScore() == 0,"Avoid first three coffees before floor test");
        int limit = (int)(2/dt);
        while (zeroTrash.getTrashCaught() == 0 && --limit > 0) zeroTrash.update(dt,new MinigameInput(false,zeroTrash.getTrayX() < 345));
        check(limit > 0 && zeroTrash.getTrashCaught() == 1 && zeroTrash.getScore() == 0,"Trash at zero stays zero");

        CafeMinigame side = centeredCafe(); start(side);
        for (int i = 0; i < Math.ceil(.2/dt); i++) side.update(dt,new MinigameInput(true,false));
        while (side.getDrops().isEmpty()) side.update(dt,MinigameInput.NONE);
        CafeMinigame.Drop firstDrop = side.getDrops().get(0);
        while (firstDrop.getY() >= 88) side.update(dt,MinigameInput.NONE);
        limit = (int)(1/dt);
        while (side.getCoffeeCaught() == 0 && --limit > 0) side.update(dt,new MinigameInput(false,side.getTrayX() < 345));
        check(limit > 0 && side.getScore() == 10,"Moving tray into an overlapping cup also counts as a collision");

        CafeMinigame exact = centeredCafe(); start(exact);
        while (!exact.isFinished()) {
            float target = exact.getScore() < 100 ? 345 : 245;
            exact.update(dt,new MinigameInput(exact.getTrayX() > target+5,exact.getTrayX() < target-5));
        }
        check(exact.getScore() == 100 && exact.isSuccess(),"Exactly 100 points at deadline succeeds");
    }

    private static CafeMinigame centeredCafe() {
        return new CafeMinigame(new Random(0) { @Override public float nextFloat() { return .5f; } });
    }

    private static void verifyCertification() {
        CertificationMinigame study = new CertificationMinigame(new Random(7));
        start(study);
        check(study.getPatternLength() == 2 && study.isShowingPattern() && study.getTimeRemaining() == 40,
            "Intro Enter starts only, without spending time or typing a pattern");
        int[] remembered = study.getVisiblePattern();
        int first = remembered[0];
        int[] changed = study.getVisiblePattern(); changed[0] = -1;
        check(study.getVisiblePattern()[0] == first,"Visible pattern is a read-only copy");
        while (study.isShowingPattern()) study.update(dt,MinigameTestDriver.key(Input.Keys.UP));
        check(study.getInputIndex() == 0 && study.getMistakes() == 0 && study.getVisiblePattern().length == 0,
            "Showing stage including its last frame ignores arrows; hidden pattern not exposed");
        study.update(dt,MinigameTestDriver.key(first));
        check(study.getInputIndex() == 1,"One arrow press consumes exactly one element");
        for (int i = 0; i < 20; i++) study.update(dt,new MinigameInput(true,true));
        check(study.getInputIndex() == 1,"Held arrows without fresh edges cannot repeat");
        study.update(dt,MinigameTestDriver.key(remembered[1]));
        check(study.getCompletedRounds() == 1 && study.getRoundNumber() == 2 && study.getPatternLength() == 3,
            "Correct sequence advances round");
        MinigameTestDriver.finish(study,true,dt);
        check(study.getCompletedRounds() == 5 && study.getMistakes() == 0 && study.getTimeRemaining() > 0,"All five rounds succeed before deadline");

        CertificationMinigame wrong = new CertificationMinigame(new Random(7)); start(wrong);
        int[] original = wrong.getVisiblePattern();
        for (int mistakes = 1; mistakes <= 3; mistakes++) {
            while (wrong.isShowingPattern()) wrong.update(dt,MinigameInput.NONE);
            wrong.update(dt,MinigameTestDriver.key(original[0] == Input.Keys.UP ? Input.Keys.DOWN : Input.Keys.UP));
            check(wrong.getMistakes() == mistakes && wrong.getCompletedRounds() == 0,"Wrong arrow increases mistakes once without advancing");
            if (mistakes < 3) check(Arrays.equals(original,wrong.getVisiblePattern()) && wrong.getInputIndex() == 0,"Mistake retries same pattern");
        }
        check(wrong.isFinished() && !wrong.isSuccess(),"Third memory mistake fails");
        wrong.update(dt,MinigameTestDriver.key(Input.Keys.UP)); check(wrong.getMistakes() == 3,"Result freezes memory mistakes");
        CertificationMinigame timeout = new CertificationMinigame(new Random(1)); start(timeout);
        runClock(timeout);
        check(!timeout.isSuccess() && timeout.getTimeRemaining() == 0 && timeout.getMistakes() == 0,"40 seconds with no answers fails");
    }

    private static void verifyConnection(VacationEventType type, boolean success) throws Exception {
        input = MinigameInput.NONE;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field("camera").set(game,camera); field("viewport").set(game,new FitViewport(800,600,camera));
        LOAD.invoke(game,type.getCompletedStage());
        Rectangle first = ((GradeItem)((Array<?>)get(game,"gradeItems")).first()).getBounds();
        player(game).reset(first.x,first.y); tick(game,MinigameInput.NONE);
        Rectangle flag = (Rectangle)get(game,"flag"); player(game).reset(flag.x,flag.y);
        tick(game,MinigameTestDriver.key(Input.Keys.ENTER));
        check(get(game,"gameState") == GameState.STAGE_CLEAR,"Flag-frame Enter cannot skip semester report");
        tick(game,MinigameTestDriver.key(Input.Keys.ENTER));
        VacationMinigame minigame = (VacationMinigame)get(game,"vacationGame");
        check(get(game,"gameState") == GameState.VACATION_EVENT && minigame.getType() == type
            && minigame.getPhase() == VacationMinigame.Phase.INTRO,"Correct game intro; flag-frame Enter cannot skip it");
        StageResult saved = ((StageResult[])get(game,"completedResults"))[type.getCompletedStage()-1];
        check(saved != null && saved.getItemScore() == 5,"Semester stored before minigame");
        String frozen = world(game);
        for (int i = 0; i < 10; i++) tick(game,MinigameInput.NONE); // Proxy continuously reports S/F pressed.
        check(minigame.getPhase() == VacationMinigame.Phase.INTRO && minigame.getTimeRemaining() == minigame.getTimeLimit(),
            "S/F cannot start, finish, or choose an outcome");
        check(!game.completeVacationEvent(type,success),"Public completion cannot bypass gameplay");
        tick(game,MinigameTestDriver.key(Input.Keys.ENTER));
        check(minigame.getPhase() == VacationMinigame.Phase.PLAYING && minigame.getTimeRemaining() == minigame.getTimeLimit(),
            "Intro Enter is consumed before actual gameplay");
        MinigameTestDriver driver = new MinigameTestDriver(success);
        int remaining = (int)((minigame.getTimeLimit()+2)/dt);
        while (!minigame.isFinished() && --remaining > 0) tick(game,driver.next(minigame));
        check(remaining > 0 && minigame.isSuccess() == success,"Real Main input loop determines actual result");
        VacationManager manager = (VacationManager)get(game,"vacationManager");
        check(get(game,"gameState") == GameState.VACATION_EVENT && (Integer)get(game,"currentStageNumber") == type.getCompletedStage()
            && manager.getResult(type) == null && frozen.equals(world(game)),"World/score/lives frozen and reward waits for result confirmation");
        double time = minigame.getTimeRemaining();
        for (int i = 0; i < 20; i++) tick(game,new MinigameInput(true,true,Input.Keys.SPACE));
        check(minigame.getTimeRemaining() == time && manager.getResult(type) == null,"Result screen does not run timer or confirm via Space/S/F");
        check(!game.completeVacationEvent(type,!success),"Caller cannot replace the actual outcome");
        tick(game,new MinigameInput(true,true,Input.Keys.ENTER,Input.Keys.SPACE));
        int next = type.getCompletedStage()+1;
        StageDefinition stage = StageFactory.createStage(next);
        check(get(game,"gameState") == GameState.PLAYING && (Integer)get(game,"currentStageNumber") == next
            && get(game,"vacationGame") == null && manager.getResult(type).isSuccess() == success,
            "Result Enter applies existing reward once and removes game state");
        check(player(game).getCenterX()-16 == stage.getStartX() && player(game).getY() == stage.getStartY(),
            "Result Enter/Space/arrows cannot move or jump in next year on confirmation frame");
        check(((StageResult[])get(game,"completedResults"))[type.getCompletedStage()-1] == saved
            && (Integer)get(game,"totalDeathCount") == 0 && (Integer)get(game,"stageScore") == 0,
            "No item-score, bonus, deaths, or GPA side effect from minigame");
        PlayerModifier modifier = player(game).getModifier();
        check(modifier.getSource() == type && modifier.isSuccess() == success,"Actual game result uses existing modifier model");
        check(!game.completeVacationEvent(type,success),"Duplicate result confirmation cannot reload next stage");
    }

    private static String world(Main game) throws Exception {
        StringBuilder s = new StringBuilder().append(player(game).getCenterX()).append('/').append(player(game).getY());
        for (Object p : (Array<?>)get(game,"movingPlatforms")) s.append(((MovingPlatform)p).getBounds());
        for (Object e : (Array<?>)get(game,"enemies")) s.append(((PatrollingEnemy)e).getBounds()).append(((PatrollingEnemy)e).isMovingRight());
        for (Object t : (Array<?>)get(game,"timedTraps")) s.append(((TimedTrap)t).getState()).append(((TimedTrap)t).getStateElapsedTime());
        for (Object item : (Array<?>)get(game,"gradeItems")) s.append(((GradeItem)item).isCollected());
        for (String name : new String[]{"lives","semesterDeathCount","totalDeathCount","stageScore","collectedItemCount"}) s.append('/').append(get(game,name));
        return s.toString();
    }
    private static void tick(Main game, MinigameInput next) throws Exception { input = next; UPDATE.invoke(game,dt); input = MinigameInput.NONE; }
    private static void start(VacationMinigame game) { game.update(dt,MinigameTestDriver.key(Input.Keys.ENTER)); }
    private static void runClock(VacationMinigame game) {
        int remaining = (int)((game.getTimeLimit()+1)/dt);
        while (!game.isFinished() && --remaining > 0) game.update(dt,MinigameInput.NONE);
        check(remaining > 0,"Timer finishes within limit plus one frame");
    }
    private static Player player(Main game) throws Exception { return (Player)get(game,"player"); }
    private static Object get(Main game, String name) throws Exception { return field(name).get(game); }
    private static Field field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Method method(String name, Class<?>... parameters) {
        try { Method m = Main.class.getDeclaredMethod(name,parameters); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
