package io.jbnu.test;

import java.util.Random;

/** Build connected layouts first, then scramble only rotations. Tile locations never need to move. */
final class CircuitPuzzles {
    private static final int[][][] PATHS = {
        {{0,0},{0,1},{0,2},{1,2},{2,2},{2,3}},
        {{2,0},{2,1},{3,1},{3,2},{3,3},{2,3},{1,3},{1,2},{0,2},{0,3},{0,4}},
        {{4,0},{4,1},{3,1},{2,1},{2,2},{2,3},{3,3},{3,4},{2,4},{1,4},{1,5}}
    };
    static CircuitBoard solved(int index) {
        if (index < 0 || index >= PATHS.length) throw new IllegalArgumentException("Three circuit puzzles only");
        int size = index+4;
        CircuitTile[][] cells = new CircuitTile[size][size];
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) cells[r][c] = new CircuitTile(CircuitTile.Type.EMPTY,0);
        int[][] path = PATHS[index];
        for (int i = 0; i < path.length; i++) {
            int[] position = path[i];
            int ports = 0;
            if (i > 0) ports |= direction(position,path[i-1]);
            if (i+1 < path.length) ports |= direction(position,path[i+1]);
            CircuitTile.Type type = i == 0 ? CircuitTile.Type.START : i == path.length-1 ? CircuitTile.Type.SERVER
                : ports == 5 || ports == 10 ? CircuitTile.Type.STRAIGHT : CircuitTile.Type.ELBOW;
            cells[position[0]][position[1]] = CircuitTile.withPorts(type,ports);
        }
        if (index == 0) cells[3][0] = new CircuitTile(CircuitTile.Type.ELBOW,0);
        if (index == 1) {
            cells[0][0] = new CircuitTile(CircuitTile.Type.STRAIGHT,1);
            cells[4][2] = new CircuitTile(CircuitTile.Type.ELBOW,0);
            cells[4][3] = new CircuitTile(CircuitTile.Type.STRAIGHT,1);
        }
        if (index == 2) {
            cells[5][1] = CircuitTile.withPorts(CircuitTile.Type.ELBOW,3);
            cells[5][2] = CircuitTile.withPorts(CircuitTile.Type.STRAIGHT,10);
            cells[5][3] = CircuitTile.withPorts(CircuitTile.Type.ELBOW,9);
            cells[4][3] = CircuitTile.withPorts(CircuitTile.Type.STRAIGHT,5);
            cells[0][0] = CircuitTile.withPorts(CircuitTile.Type.ELBOW,6);
            cells[0][1] = CircuitTile.withPorts(CircuitTile.Type.ELBOW,12);
            cells[1][0] = CircuitTile.withPorts(CircuitTile.Type.ELBOW,3);
            cells[1][1] = CircuitTile.withPorts(CircuitTile.Type.ELBOW,9);
        }
        return new CircuitBoard(cells);
    }
    static CircuitBoard scrambled(int index, Random random) {
        CircuitBoard board = solved(index);
        for (int r = 0; r < board.getRows(); r++) for (int c = 0; c < board.getColumns(); c++) {
            int rotations = random.nextInt(4);
            for (int i = 0; i < rotations; i++) board.rotate(r,c);
        }
        int[] first = PATHS[index][1];
        while (board.trace().isConnected()) board.rotate(first[0],first[1]);
        return board;
    }
    private static int direction(int[] from, int[] to) {
        for (CircuitTile.Direction direction : CircuitTile.Direction.values())
            if (to[0]-from[0] == direction.rowDelta && to[1]-from[1] == direction.columnDelta) return direction.bit();
        throw new IllegalArgumentException("Path cells must be adjacent");
    }
}
