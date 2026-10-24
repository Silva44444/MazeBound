package net.mazebound.world;

import net.mazebound.Mazebound;
import net.mazebound.exploration.Cell;
import net.mazebound.exploration.MazeGenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * The one source of truth for "what does the maze look like".
 *
 * The maze is a square ring around the world origin: everything within
 * innerRadiusCells of spawn stays normal terrain, and the maze fills the
 * cells from innerRadiusCells out to outerRadiusCells (inclusive), measured
 * as max(|cellX|, |cellZ|).
 *
 * Generated once at startup from a saved seed, so the maze is identical
 * after every restart. Everything else (renderer, exploration, boss/loot
 * placement) asks this class instead of guessing.
 */
public class MazeLayout {

    private static final int CELL_SIZE = Mazebound.CELL_SIZE;

    private final long seed;
    private final int innerRadiusCells;
    private final int outerRadiusCells;
    private final Map<String, Cell> cells;

    private MazeLayout(
            long seed,
            int innerRadiusCells,
            int outerRadiusCells,
            Map<String, Cell> cells
    ) {
        this.seed = seed;
        this.innerRadiusCells = innerRadiusCells;
        this.outerRadiusCells = outerRadiusCells;
        this.cells = cells;
    }

    public static MazeLayout generate(
            long seed,
            int innerRadiusCells,
            int outerRadiusCells,
            int entrancesPerSide
    ) {
        Map<String, Cell> cells = new MazeGenerator(seed).generateRing(
                innerRadiusCells,
                outerRadiusCells,
                entrancesPerSide
        );

        return new MazeLayout(
                seed,
                innerRadiusCells,
                outerRadiusCells,
                Collections.unmodifiableMap(cells)
        );
    }

    public long seed() {
        return seed;
    }

    public int innerRadiusCells() {
        return innerRadiusCells;
    }

    public int outerRadiusCells() {
        return outerRadiusCells;
    }

    public Collection<Cell> cells() {
        return cells.values();
    }

    /** The maze cell at cell coordinates, or null (not part of the maze). */
    public Cell getCell(int cellX, int cellZ) {
        return cells.get(cellX + "," + cellZ);
    }

    /** The maze cell containing this block column, or null. */
    public Cell getCellAtBlock(int blockX, int blockZ) {
        return getCell(
                Math.floorDiv(blockX, CELL_SIZE),
                Math.floorDiv(blockZ, CELL_SIZE)
        );
    }

    /** True if this block column is inside a generated maze cell. */
    public boolean isMazeBlock(int blockX, int blockZ) {
        return getCellAtBlock(blockX, blockZ) != null;
    }

    /** Lowest block coordinate covered by the maze's outer edge. */
    public int minBlock() {
        return -outerRadiusCells * CELL_SIZE;
    }

    /** Highest block coordinate covered by the maze's outer edge. */
    public int maxBlock() {
        return (outerRadiusCells + 1) * CELL_SIZE - 1;
    }

    /** Lowest block coordinate of the untouched area inside the ring. */
    public int holeMinBlock() {
        return -(innerRadiusCells - 1) * CELL_SIZE;
    }

    /** Highest block coordinate of the untouched area inside the ring. */
    public int holeMaxBlock() {
        return innerRadiusCells * CELL_SIZE - 1;
    }

    /**
     * Quick reject test: could this chunk contain maze columns? False for
     * chunks outside the outer edge and chunks entirely inside the hole.
     */
    public boolean intersectsChunk(int chunkX, int chunkZ) {

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        boolean overlapsBounds = maxX >= minBlock() && minX <= maxBlock()
                && maxZ >= minBlock() && minZ <= maxBlock();

        if (!overlapsBounds) {
            return false;
        }

        boolean insideHole = minX >= holeMinBlock() && maxX <= holeMaxBlock()
                && minZ >= holeMinBlock() && maxZ <= holeMaxBlock();

        return !insideHole;
    }
}