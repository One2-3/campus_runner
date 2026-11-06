package io.jbnu.test;

import com.badlogic.gdx.Input;
import java.util.Random;

/** Tile geometry, reciprocal graph traversal, alternative routes, and real cursor/rotation input. */
public final class ServerConnectionVerification {
    public static void main(String[] args) {
        verifyTiles(); verifyConnections();
        for (int fps : new int[]{30,60,120}) {
            float dt = 1f/fps;
            verifyGameRules(dt);
            for (int seed = 0; seed < 100; seed++) {
                for (int puzzle = 0; puzzle < 3; puzzle++) {
                    CircuitBoard board = CircuitPuzzles.scrambled(puzzle,new Random(seed));
                    check(!board.trace().isConnected(),"Every initial board requires rotation");
                    check(board.getRows() == puzzle+4 && board.getColumns() == puzzle+4,"4x4 / 5x5 / 6x6 dimensions");
                }
                MinigameTestDriver.finish(new HackathonMinigame(new Random(seed)),true,dt);
            }
            HackathonMinigame zeroRandom = new HackathonMinigame(new Random(0) { @Override public int nextInt(int bound) { return 0; } });
            MinigameTestDriver.finish(zeroRandom,true,dt);
            System.out.printf("%d fps: server connection passed: five tile types, 90-degree input, reciprocal BFS, "
                + "broken/one-sided/wrong-server/outside/disconnected-loop rejection, alternate route, "
                + "3 boards/45s/3 mistakes, cumulative timer/mistakes, 100 solvable seeds and degenerate RNG.%n",fps);
        }
    }

    private static void verifyTiles() {
        CircuitTile straight = tile(CircuitTile.Type.STRAIGHT,0);
        check(straight.getPorts() == 5 && straight.clockwise().getPorts() == 10,"Straight vertical/horizontal rotation");
        CircuitTile bend = tile(CircuitTile.Type.ELBOW,0);
        int[] masks = {3,6,12,9};
        for (int i = 0; i < 4; i++) { check(bend.getPorts() == masks[i],"Clockwise elbow ports"); bend = bend.clockwise(); }
        check(bend.getRotation() == 0,"Four turns restore orientation");
        for (CircuitTile.Type type : new CircuitTile.Type[]{CircuitTile.Type.EMPTY,CircuitTile.Type.START,CircuitTile.Type.SERVER}) {
            CircuitTile fixed = tile(type,0);
            check(!fixed.isRotatable() && fixed.clockwise() == fixed,"Fixed/empty tile cannot rotate");
        }
        check(tile(CircuitTile.Type.EMPTY,3).getPorts() == 0,"Empty has no connections");
        check(tile(CircuitTile.Type.START,0).getPorts() == 2 && tile(CircuitTile.Type.SERVER,0).getPorts() == 8,
            "PC and server have explicit fixed endpoint directions");
    }

    private static void verifyConnections() {
        CircuitTile start = tile(CircuitTile.Type.START,0), server = tile(CircuitTile.Type.SERVER,0);
        CircuitTile horizontal = tile(CircuitTile.Type.STRAIGHT,1), empty = tile(CircuitTile.Type.EMPTY,0);
        CircuitTile[][] cells = {{start,horizontal,server}};
        CircuitBoard line = new CircuitBoard(cells);
        cells[0][1] = empty;
        check(line.trace().isConnected() && line.trace().getVisitedCount() == 3,"Reciprocal connected route and copied grid");
        check(!new CircuitBoard(cells).trace().isConnected(),"Broken middle tile fails");
        check(!new CircuitBoard(new CircuitTile[][]{{start,tile(CircuitTile.Type.STRAIGHT,0),server}}).trace().isConnected(),
            "One-sided adjacent ports do not conduct");
        check(!new CircuitBoard(new CircuitTile[][]{{start,horizontal,tile(CircuitTile.Type.SERVER,1)}}).trace().isConnected(),
            "Wrong server approach direction fails");
        check(!new CircuitBoard(new CircuitTile[][]{{tile(CircuitTile.Type.START,3),horizontal,server}}).trace().isConnected(),
            "Signal exiting the grid fails safely");
        check(!new CircuitBoard(new CircuitTile[][]{{start,CircuitTile.withPorts(CircuitTile.Type.ELBOW,9),server}}).trace().isConnected(),
            "Reachable circuit pointing outside the grid fails");
        CircuitBoard equivalent = new CircuitBoard(new CircuitTile[][]{{start,tile(CircuitTile.Type.STRAIGHT,3),server}});
        check(equivalent.trace().isConnected(),"Different 180-degree rotation value with same real ports is accepted");
        for (int i = 0; i < 3; i++) check(CircuitPuzzles.solved(i).trace().isConnected(),"Every designed template is connected");

        CircuitBoard withLoop = CircuitPuzzles.solved(2);
        CircuitBoard.Trace trace = withLoop.trace();
        check(trace.isConnected() && !trace.isReached(0,0) && trace.getVisitedCount() <= 36,
            "Unrelated closed loop neither invalidates source path nor causes infinite traversal");
        rotateTo(withLoop,4,1,3); // Disconnect the PC from either candidate route; preserve the remote circuits.
        trace = withLoop.trace();
        check(!trace.isConnected() && trace.getVisitedCount() == 1,"Remote completed circuits without PC access cannot succeed");

        CircuitBoard alternative = CircuitPuzzles.solved(2);
        rotateTo(alternative,4,1,12);
        rotateTo(alternative,3,3,6);
        check(alternative.trace().isConnected() && alternative.trace().isReached(5,2)
            && !alternative.trace().isReached(3,1),"Alternative bottom route succeeds instead of template route");
        alternative.rotate(0,0);
        check(alternative.trace().isConnected(),"Unrelated tile orientations are not part of correctness");
    }

    private static void verifyGameRules(float dt) {
        HackathonMinigame game = new HackathonMinigame(new Random(11));
        game.update(dt,key(Input.Keys.ENTER));
        check(game.getTimeRemaining() == 45 && game.getMistakes() == 0 && game.getLastCheck() == null,
            "Intro Enter starts only; no premature check or clock advancement");
        go(game,0,0,dt);
        for (int i = 0; i < 10; i++) { game.update(dt,key(Input.Keys.UP)); game.update(dt,key(Input.Keys.LEFT)); }
        check(game.getCursorRow() == 0 && game.getCursorColumn() == 0,"Cursor cannot leave top/left boundaries");
        CircuitTile fixed = game.getBoard().getTile(0,0);
        game.update(dt,key(Input.Keys.SPACE)); check(game.getBoard().getTile(0,0) == fixed,"PC cannot rotate through input");
        go(game,2,3,dt); fixed = game.getBoard().getTile(2,3);
        game.update(dt,key(Input.Keys.SPACE)); check(game.getBoard().getTile(2,3) == fixed,"Server cannot rotate through input");
        go(game,3,3,dt); fixed = game.getBoard().getTile(3,3);
        for (int i = 0; i < 10; i++) { game.update(dt,key(Input.Keys.DOWN)); game.update(dt,key(Input.Keys.RIGHT)); }
        check(game.getCursorRow() == 3 && game.getCursorColumn() == 3,"Cursor cannot leave bottom/right boundaries");
        game.update(dt,key(Input.Keys.SPACE)); check(game.getBoard().getTile(3,3) == fixed,"Empty cannot rotate through input");
        go(game,0,1,dt);
        int rotation = game.getBoard().getTile(0,1).getRotation();
        game.update(dt,new MinigameInput(false,false,Input.Keys.SPACE,Input.Keys.ENTER));
        check(game.getBoard().getTile(0,1).getRotation() == (rotation+1)%4 && game.getMistakes() == 0 && game.getLastCheck() == null,
            "One Space means exactly 90 degrees; simultaneous Enter does not also check");
        for (int i = 0; i < 20; i++) game.update(dt,new MinigameInput(true,true));
        check(game.getBoard().getTile(0,1).getRotation() == (rotation+1)%4,"Held keys without fresh edges do not rotate repeatedly");

        HackathonMinigame progress = new HackathonMinigame(new Random(4)); progress.update(dt,key(Input.Keys.ENTER));
        progress.update(dt,key(Input.Keys.ENTER));
        check(progress.getMistakes() == 1 && progress.getLastCheck() != null,"Wrong Enter checks actual connectivity once");
        for (int i = 0; i < 20; i++) progress.update(dt,MinigameInput.NONE);
        check(progress.getMistakes() == 1,"Held Enter without new edge cannot add mistakes");
        for (int puzzle = 0; puzzle < 3; puzzle++) {
            solveCurrent(progress,dt);
            if (puzzle == 2) {
                inputRotateTo(progress,4,1,12,dt);
                inputRotateTo(progress,3,3,6,dt);
                check(progress.getBoard().trace().isReached(5,2),"Real input solved alternative route");
            }
            check(progress.getCompletedPuzzles() == puzzle && progress.getBoard().trace().isConnected(),
                "Solved ports do not auto-advance without Enter");
            double before = progress.getTimeRemaining();
            progress.update(dt,key(Input.Keys.ENTER));
            check(progress.getCompletedPuzzles() == puzzle+1 && progress.getMistakes() == 1
                && progress.getTimeRemaining() < before,"Puzzle transitions preserve remaining time and accumulated mistakes");
            if (puzzle < 2) check(!progress.getBoard().trace().isConnected(),"Next puzzle starts unsolved");
        }
        check(progress.isFinished() && progress.isSuccess() && progress.getTimeRemaining() > 0,"All three boards within shared timer succeed");
        progress.update(50,key(Input.Keys.ENTER)); check(progress.isSuccess() && progress.getMistakes() == 1,"Final result is immutable");

        HackathonMinigame wrong = new HackathonMinigame(new Random(4)); wrong.update(dt,key(Input.Keys.ENTER));
        for (int i = 1; i <= 3; i++) { wrong.update(dt,key(Input.Keys.ENTER)); check(wrong.getMistakes() == i,"One mistake per explicit check"); }
        check(wrong.isFinished() && !wrong.isSuccess(),"Three incorrect connection checks fail immediately");
        HackathonMinigame timeout = new HackathonMinigame(new Random(4)); timeout.update(dt,key(Input.Keys.ENTER));
        int frames = (int)(46/dt);
        while (!timeout.isFinished() && --frames > 0) timeout.update(dt,MinigameInput.NONE);
        check(frames > 0 && !timeout.isSuccess() && timeout.getTimeRemaining() == 0,"45-second shared deadline fails without completion");
    }

    private static void solveCurrent(HackathonMinigame game, float dt) {
        MinigameTestDriver driver = new MinigameTestDriver(true);
        int limit = 1000;
        while (--limit > 0) {
            MinigameInput input = driver.next(game);
            if (input.isPressed(Input.Keys.ENTER)) break;
            game.update(dt,input);
        }
        check(limit > 0 && !game.isFinished(),"Current board solved with cursor and Space only");
    }
    private static void inputRotateTo(HackathonMinigame game, int row, int column, int ports, float dt) {
        go(game,row,column,dt);
        int count = 0;
        while (game.getBoard().getTile(row,column).getPorts() != ports && count++ < 4) game.update(dt,key(Input.Keys.SPACE));
        check(game.getBoard().getTile(row,column).getPorts() == ports,"Target ports attainable through real rotations");
    }
    private static void go(HackathonMinigame game, int row, int column, float dt) {
        while (game.getCursorRow() != row) game.update(dt,key(game.getCursorRow() < row ? Input.Keys.DOWN : Input.Keys.UP));
        while (game.getCursorColumn() != column) game.update(dt,key(game.getCursorColumn() < column ? Input.Keys.RIGHT : Input.Keys.LEFT));
    }
    private static void rotateTo(CircuitBoard board, int row, int column, int ports) {
        int count = 0;
        while (board.getTile(row,column).getPorts() != ports && count++ < 4) board.rotate(row,column);
        check(board.getTile(row,column).getPorts() == ports,"Test board orientation valid");
    }
    private static CircuitTile tile(CircuitTile.Type type, int rotation) { return new CircuitTile(type,rotation); }
    private static MinigameInput key(int key) { return MinigameTestDriver.key(key); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
