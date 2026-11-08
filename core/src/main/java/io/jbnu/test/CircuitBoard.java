package io.jbnu.test;

import java.util.ArrayDeque;
import java.util.Queue;

/** Connection is graph reachability through reciprocal ports, with bounds and visited checks. */
public final class CircuitBoard {
    public static final class Trace {
        private final boolean[][] reached;
        private final boolean connected;
        private final int count;
        private Trace(boolean[][] reached, boolean connected, int count) {
            this.reached = reached; this.connected = connected; this.count = count;
        }
        public boolean isConnected() { return connected; }
        public boolean isReached(int row, int column) { return reached[row][column]; }
        public int getVisitedCount() { return count; }
    }
    private final CircuitTile[][] tiles;
    private final int startRow, startColumn, serverRow, serverColumn;

    public CircuitBoard(CircuitTile[][] source) {
        if (source == null || source.length == 0 || source[0] == null || source[0].length == 0)
            throw new IllegalArgumentException("Nonempty circuit grid required");
        tiles = new CircuitTile[source.length][source[0].length];
        int starts = 0, servers = 0, sr = -1, sc = -1, er = -1, ec = -1;
        for (int r = 0; r < source.length; r++) {
            if (source[r] == null || source[r].length != tiles[0].length) throw new IllegalArgumentException("Rectangular circuit required");
            for (int c = 0; c < source[r].length; c++) {
                CircuitTile tile = source[r][c];
                if (tile == null) throw new IllegalArgumentException("Use EMPTY instead of null tiles");
                tiles[r][c] = tile;
                if (tile.getType() == CircuitTile.Type.START) { starts++; sr = r; sc = c; }
                if (tile.getType() == CircuitTile.Type.SERVER) { servers++; er = r; ec = c; }
            }
        }
        if (starts != 1 || servers != 1) throw new IllegalArgumentException("Exactly one PC and one server required");
        startRow = sr; startColumn = sc; serverRow = er; serverColumn = ec;
    }
    public int getRows() { return tiles.length; }
    public int getColumns() { return tiles[0].length; }
    public int getStartRow() { return startRow; }
    public int getStartColumn() { return startColumn; }
    public CircuitTile getTile(int row, int column) { return tiles[row][column]; }
    boolean rotate(int row, int column) {
        CircuitTile tile = tiles[row][column];
        if (!tile.isRotatable()) return false;
        tiles[row][column] = tile.clockwise();
        return true;
    }
    public Trace trace() {
        boolean[][] reached = new boolean[getRows()][getColumns()];
        Queue<Integer> queue = new ArrayDeque<>();
        reached[startRow][startColumn] = true;
        queue.add(startRow*getColumns()+startColumn);
        boolean invalidPort = false;
        int count = 0;
        while (!queue.isEmpty()) {
            int cell = queue.remove(), row = cell/getColumns(), column = cell%getColumns();
            count++;
            int ports = getTile(row,column).getPorts();
            for (CircuitTile.Direction direction : CircuitTile.Direction.values()) {
                if ((ports & direction.bit()) == 0) continue;
                int nr = row+direction.rowDelta, nc = column+direction.columnDelta;
                if (nr < 0 || nr >= getRows() || nc < 0 || nc >= getColumns()) { invalidPort = true; continue; }
                if ((getTile(nr,nc).getPorts() & direction.opposite().bit()) == 0) { invalidPort = true; continue; }
                if (!reached[nr][nc]) { reached[nr][nc] = true; queue.add(nr*getColumns()+nc); }
            }
        }
        return new Trace(reached,reached[serverRow][serverColumn] && !invalidPort,count);
    }
}
