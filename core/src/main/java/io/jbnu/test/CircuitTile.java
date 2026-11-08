package io.jbnu.test;

/** Immutable tile; bit positions follow clockwise UP, RIGHT, DOWN, LEFT. */
public final class CircuitTile {
    public enum Direction {
        UP(-1,0), RIGHT(0,1), DOWN(1,0), LEFT(0,-1);
        public final int rowDelta, columnDelta;
        Direction(int rowDelta, int columnDelta) { this.rowDelta = rowDelta; this.columnDelta = columnDelta; }
        public int bit() { return 1 << ordinal(); }
        public Direction opposite() { return values()[(ordinal()+2)%4]; }
    }
    public enum Type { EMPTY, STRAIGHT, ELBOW, START, SERVER }
    private final Type type;
    private final int rotation;

    public CircuitTile(Type type, int rotation) {
        if (type == null) throw new IllegalArgumentException("Circuit tile type required");
        this.type = type;
        this.rotation = Math.floorMod(rotation,4);
    }
    public Type getType() { return type; }
    public int getRotation() { return rotation; }
    public boolean isRotatable() { return type == Type.STRAIGHT || type == Type.ELBOW; }
    public CircuitTile clockwise() { return isRotatable() ? new CircuitTile(type,rotation+1) : this; }
    public int getPorts() {
        int mask;
        switch (type) {
            case STRAIGHT: mask = 5; break;
            case ELBOW: mask = 3; break;
            case START: mask = 2; break;
            case SERVER: mask = 8; break;
            default: return 0;
        }
        return ((mask << rotation) | (mask >> (4-rotation))) & 15;
    }
    static CircuitTile withPorts(Type type, int ports) {
        for (int rotation = 0; rotation < 4; rotation++) {
            CircuitTile tile = new CircuitTile(type,rotation);
            if (tile.getPorts() == ports) return tile;
        }
        throw new IllegalArgumentException("Invalid ports for tile type " + type);
    }
}
