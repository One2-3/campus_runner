package io.jbnu.test;

import java.lang.reflect.Field;

/** Explicit future-minigame callback used by existing route/accounting tests after observing an event. */
final class VacationVerificationSupport {
    static GameState clearState(int stage) {
        if (stage == 8) return GameState.GAME_COMPLETE;
        return stage == 2 || stage == 4 || stage == 6 ? GameState.VACATION_EVENT : GameState.STAGE_CLEAR;
    }

    static void finishIfPending(Main game, boolean success) throws Exception {
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
