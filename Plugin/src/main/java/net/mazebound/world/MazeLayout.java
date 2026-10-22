package net.mazebound.world;

import net.mazebound.Mazebound;
import net.mazebound.exploration.Cell;
import net.mazebound.exploration.MazeGenerator;
import net.mazebound.exploration.RegionManager;
import net.mazebound.exploration.RegionType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * The one source of truth for "what does the maze look like".
 *
 * Generated once at startup from a saved seed, so the maze is identical
 * after every restart. Everything else (renderer, exploration, region
 * checks, boss/loot placement) asks this class instead of guessing.
 *
 * The maze covers cells -radius..+radius on both axes, centered on the
 * world origin. The hub cells in the middle are NOT part of the layout.
 */
public class MazeLayout {

    private static final int CELL_SIZE = Mazebound.CELL_SIZE;

    private final long seed;
    private final int radiusCells;
    private final Map<String, Cell> cells;
    private final RegionManager regionManager;

    private MazeLayout(
            long seed,
            int radiusCells,
            Map<String, Cell> cells,
            RegionManager regionManager
    ) {
        this.seed = seed;
        this.radiusCells = radiusCells;
        this.cells = cells;
        this.regionManager = regionManager;
    }

    public static MazeLayout generate(long seed, int radiusCells) {

        if (radiusCells <= RegionManager.HUB_RADIUS) {
            throw new IllegalArgumentException(
                    "radius-cells must be larger than the hub radius ("
                            + RegionManager.HUB_RADIUS + ")"
            );
        }

        RegionManager regionManager = new RegionManager();
        WorldPlanner planner = new WorldPlanner(regionManager);

        Map<String, Cell> cells = new MazeGenerator(seed).generate(
                -radiusCells,
                radiusCells,
                -radiusCells,
                radiusCells,
                planner
        );

        return new MazeLayout(
                seed,
                radiusCells,
                Collections.unmodifiableMap(cells),
                regionManager
        );
    }

    public long seed() {
        return seed;
    }

    public int radiusCells() {
        return radiusCells;
    }

    public Collection<Cell> cells() {
        return cells.values();
    }

    /** The maze cell at cell coordinates, or null (hub / outside the maze). */
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

    public RegionType getRegionType(int cellX, int cellZ) {
        return regionManager.getRegionType(new Cell(cellX, cellZ));
    }

    /** Lowest block coordinate covered by the maze bounds (both axes). */
    public int minBlock() {
        return -radiusCells * CELL_SIZE;
    }

    /** Highest block coordinate covered by the maze bounds (both axes). */
    public int maxBlock() {
        return (radiusCells + 1) * CELL_SIZE - 1;
    }

    /** Quick reject test: does this chunk touch the maze bounds at all? */
    public boolean intersectsChunk(int chunkX, int chunkZ) {

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        return minX + 15 >= minBlock() && minX <= maxBlock()
                && minZ + 15 >= minBlock() && minZ <= maxBlock();
    }
}