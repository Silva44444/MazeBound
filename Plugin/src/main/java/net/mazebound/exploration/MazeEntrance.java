package net.mazebound.exploration;

public record MazeEntrance(
        int x,
        int z,
        CellDirection direction
) {
}