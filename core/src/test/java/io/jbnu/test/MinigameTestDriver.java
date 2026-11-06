package io.jbnu.test;

import com.badlogic.gdx.Input;

/** Test player: follows drops, remembers shown arrows, and rotates circuit tiles. No result injection. */
final class MinigameTestDriver {
    private final boolean success;
    private int[] remembered;
    MinigameTestDriver(boolean success) { this.success = success; }

    MinigameInput next(VacationMinigame game) {
        if (game.getPhase() == VacationMinigame.Phase.INTRO) return key(Input.Keys.ENTER);
        if (game.isFinished()) return MinigameInput.NONE;
        if (game instanceof CafeMinigame) {
            CafeMinigame cafe = (CafeMinigame)game;
            CafeMinigame.Drop target = null;
            for (CafeMinigame.Drop drop : cafe.getDrops()) if (drop.isCoffee() == success && drop.getY() > 88
                && (target == null || drop.getY() < target.getY())) target = drop;
            float desired = target == null ? (success ? 345 : 40) : target.getX()+12-CafeMinigame.TRAY_WIDTH/2;
            float difference = desired-cafe.getTrayX();
            return new MinigameInput(difference < -5,difference > 5);
        }
        if (game instanceof CertificationMinigame) {
            CertificationMinigame study = (CertificationMinigame)game;
            if (study.isShowingPattern()) { remembered = study.getVisiblePattern(); return MinigameInput.NONE; }
            int expected = remembered[study.getInputIndex()];
            return key(success ? expected : expected == Input.Keys.UP ? Input.Keys.DOWN : Input.Keys.UP);
        }
        HackathonMinigame hack = (HackathonMinigame)game;
        if (!success) return key(Input.Keys.ENTER);
        CircuitBoard board = hack.getBoard(), solved = CircuitPuzzles.solved(hack.getPuzzleNumber()-1);
        for (int r = 0; r < board.getRows(); r++) for (int c = 0; c < board.getColumns(); c++) {
            CircuitTile tile = board.getTile(r,c);
            if (!tile.isRotatable() || tile.getPorts() == solved.getTile(r,c).getPorts()) continue;
            if (hack.getCursorRow() != r) return key(hack.getCursorRow() < r ? Input.Keys.DOWN : Input.Keys.UP);
            if (hack.getCursorColumn() != c) return key(hack.getCursorColumn() < c ? Input.Keys.RIGHT : Input.Keys.LEFT);
            return key(Input.Keys.SPACE);
        }
        return key(Input.Keys.ENTER);
    }

    static void finish(VacationMinigame game, boolean success, float dt) {
        MinigameTestDriver driver = new MinigameTestDriver(success);
        int remaining = (int)((game.getTimeLimit()+2)/dt)+5;
        while (!game.isFinished() && --remaining > 0) game.update(dt,driver.next(game));
        if (remaining <= 0 || game.isSuccess() != success) throw new AssertionError("Real minigame inputs did not achieve "
            + game.getType()+" success="+success+(game instanceof CafeMinigame ? " score="+((CafeMinigame)game).getScore() : ""));
    }
    static MinigameInput key(int key) { return new MinigameInput(false,false,key); }
}
