package net.mazebound.exploration;

public enum CellDirection {

    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int xOffset;
    private final int zOffset;

    CellDirection(int xOffset, int zOffset) {
        this.xOffset = xOffset;
        this.zOffset = zOffset;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getZOffset() {
        return zOffset;
    }

    public CellDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }
}