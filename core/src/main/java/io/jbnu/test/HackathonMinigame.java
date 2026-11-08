package io.jbnu.test;

import com.badlogic.gdx.Input;
import java.util.Random;

/** SERVER CONNECTION: rotate actual ports and explicitly check PC-to-server reachability. */
public final class HackathonMinigame extends VacationMinigame {
    private final Random random;
    private CircuitBoard board;
    private CircuitBoard.Trace lastCheck;
    private int puzzle, cursorRow, cursorColumn, mistakes;

    public HackathonMinigame() { this(new Random()); }
    public HackathonMinigame(Random random) { super(VacationEventType.HACKATHON,45); this.random = random; }
    @Override protected void onStart() { nextBoard(); }
    private void nextBoard() {
        board = CircuitPuzzles.scrambled(puzzle,random);
        cursorRow = board.getStartRow(); cursorColumn = board.getStartColumn(); lastCheck = null;
    }
    @Override protected void play(float delta, MinigameInput input) {
        int count = 0, dr = 0, dc = 0;
        for (CircuitTile.Direction direction : CircuitTile.Direction.values()) {
            int key = direction == CircuitTile.Direction.UP ? Input.Keys.UP : direction == CircuitTile.Direction.DOWN
                ? Input.Keys.DOWN : direction == CircuitTile.Direction.LEFT ? Input.Keys.LEFT : Input.Keys.RIGHT;
            if (input.isPressed(key)) { count++; dr = direction.rowDelta; dc = direction.columnDelta; }
        }
        if (count == 1) {
            cursorRow = Math.max(0,Math.min(board.getRows()-1,cursorRow+dr));
            cursorColumn = Math.max(0,Math.min(board.getColumns()-1,cursorColumn+dc));
        }
        if (input.isPressed(Input.Keys.SPACE)) {
            if (board.rotate(cursorRow,cursorColumn)) lastCheck = null;
            return;
        }
        if (input.isPressed(Input.Keys.ENTER)) {
            lastCheck = board.trace();
            if (lastCheck.isConnected()) {
                puzzle++;
                if (puzzle == 3) finish(true);
                else nextBoard();
            } else { mistakes++; if (mistakes == 3) finish(false); }
        }
    }
    public CircuitBoard getBoard() { return board; }
    public CircuitBoard.Trace getLastCheck() { return lastCheck; }
    public int getPuzzleNumber() { return Math.min(puzzle+1,3); }
    public int getCompletedPuzzles() { return puzzle; }
    public int getCursorRow() { return cursorRow; }
    public int getCursorColumn() { return cursorColumn; }
    public int getMistakes() { return mistakes; }
}
