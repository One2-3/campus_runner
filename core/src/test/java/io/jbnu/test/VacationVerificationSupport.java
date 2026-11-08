package io.jbnu.test;

import java.lang.reflect.Field;

/** Explicit future-minigame callback used by existing route/accounting tests after observing an event. */
final class VacationVerificationSupport {
    static GameState clearState(int stage) {
        return GameState.STAGE_CLEAR;
    }

    /** Exercise and assert the new confirmation edge, never bypass it by assigning a state. */
    static void continueClear(Main game) throws Exception {
        if (get(game,"gameState") != GameState.STAGE_CLEAR) throw new AssertionError("Expected semester report before Enter");
        int stage = (Integer)get(game,"currentStageNumber");
        StageResult saved = ((StageResult[])get(game,"completedResults"))[stage-1];
        if (saved == null) throw new AssertionError("Semester report requires confirmed result");
        com.badlogic.gdx.Input previous = com.badlogic.gdx.Gdx.input;
        try {
            com.badlogic.gdx.Gdx.input = (com.badlogic.gdx.Input)java.lang.reflect.Proxy.newProxyInstance(
                com.badlogic.gdx.Input.class.getClassLoader(),new Class<?>[]{com.badlogic.gdx.Input.class},
                (p,m,a)->m.getName().equals("isKeyJustPressed") && (Integer)a[0]==com.badlogic.gdx.Input.Keys.ENTER);
            java.lang.reflect.Method update = Main.class.getDeclaredMethod("update",float.class);
            update.setAccessible(true); update.invoke(game,0f);
        } finally { com.badlogic.gdx.Gdx.input = previous; }
        GameState expected = stage == 8 ? GameState.GAME_COMPLETE
            : VacationEventType.afterStage(stage) != null ? GameState.VACATION_EVENT : GameState.PLAYING;
        if (get(game,"gameState") != expected || ((StageResult[])get(game,"completedResults"))[stage-1] != saved)
            throw new AssertionError("Enter must reach original destination without modifying result");
    }

    static void finishIfPending(Main game, boolean success) throws Exception {
        if (get(game,"gameState") == GameState.STAGE_CLEAR
            && VacationEventType.afterStage((Integer)get(game,"currentStageNumber")) != null) continueClear(game);
        if (get(game,"gameState") != GameState.VACATION_EVENT) return;
        int stage = (Integer)get(game,"currentStageNumber");
        StageResult[] before = ((StageResult[])get(game,"completedResults")).clone();
        if (before[stage-1] == null) throw new AssertionError("Vacation requires saved semester result");
        VacationEventType type = VacationEventType.afterStage(stage);
        VacationMinigame minigame = (VacationMinigame)get(game,"vacationGame");
        MinigameTestDriver.finish(minigame,success,1f/60);
        if (!game.completeVacationEvent(type,success)) throw new AssertionError("Expected vacation completion callback");
        if ((Integer)get(game,"currentStageNumber") != stage+1 || get(game,"gameState") != GameState.PLAYING)
            throw new AssertionError("Vacation advances exactly one semester");
        StageResult[] after = (StageResult[])get(game,"completedResults");
        for (int i = 0; i < before.length; i++) if (before[i] != after[i]) throw new AssertionError("Vacation cannot change any semester score");
    }

    private static Object get(Main game, String name) throws Exception {
        Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f.get(game);
    }
}
