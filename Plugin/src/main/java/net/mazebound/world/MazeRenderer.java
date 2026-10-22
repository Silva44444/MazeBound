package net.mazebound.world;

import net.mazebound.Mazebound;
import net.mazebound.exploration.Cell;
import net.mazebound.exploration.CellDirection;
import org.bukkit.Material;

/**
 * Renders the maze into one chunk at a time.
 *
 * For every block column inside a maze cell it overwrites the full vertical
 * slab, so nothing natural (hills, trees, caves, water) survives inside the
 * maze:
 *
 *   maxY - 1            barrier layer (stops elytra / pearls / wind charges)
 *   floorY+1 .. maxY-2  wall column, or air in corridors and rooms
 *   floorY              stone floor
 *   below floorY        filled downward through air/water/lava until solid
 *
 * Hub cells and everything outside the maze bounds are left untouched.
 */
public class MazeRenderer {

    private static final int CELL_SIZE = Mazebound.CELL_SIZE;

    // Doorway: 11 blocks wide, centered in the cell.
    // Keep in sync with MazeCellBuilder until that class is retired.
    private static final int OPENING_CENTER = 27;
    private static final int OPENING_HALF_WIDTH = 5;

    private static final Material WALL = Material.STONE_BRICKS;
    private static final Material FLOOR = Material.STONE;
    private static final Material FILL = Material.STONE;
    private static final Material CEILING = Material.BARRIER;

    private final MazeLayout layout;
    private final int floorY;
    private final int maxFillDepth;

    public MazeRenderer(MazeLayout layout, int floorY, int maxFillDepth) {
        this.layout = layout;
        this.floorY = floorY;
        this.maxFillDepth = maxFillDepth;
    }

    public int floorY() {
        return floorY;
    }

    /** True if the configured floor leaves room for walls in this world. */
    public boolean fitsWorld(int minY, int maxY) {
        return floorY > minY + 1 && floorY + 3 < maxY - 2;
    }

    /**
     * Renders the maze part of one chunk.
     *
     * @return true if any maze column was written
     */
    public boolean renderChunk(
            int chunkX,
            int chunkZ,
            int minY,
            int maxY,
            BlockSink sink
    ) {
        if (!layout.intersectsChunk(chunkX, chunkZ) || !fitsWorld(minY, maxY)) {
            return false;
        }

        int wallTop = maxY - 2;
        int ceilingY = maxY - 1;

        int startX = chunkX << 4;
        int startZ = chunkZ << 4;

        boolean touched = false;

        int cachedCellX = Integer.MIN_VALUE;
        int cachedCellZ = Integer.MIN_VALUE;
        Cell cell = null;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {

                int x = startX + dx;
                int z = startZ + dz;

                int cellX = Math.floorDiv(x, CELL_SIZE);
                int cellZ = Math.floorDiv(z, CELL_SIZE);

                if (cellX != cachedCellX || cellZ != cachedCellZ) {
                    cell = layout.getCell(cellX, cellZ);
                    cachedCellX = cellX;
                    cachedCellZ = cellZ;
                }

                if (cell == null) {
                    continue;
                }

                int localX = Math.floorMod(x, CELL_SIZE);
                int localZ = Math.floorMod(z, CELL_SIZE);

                renderColumn(
                        x, z,
                        isWallColumn(cell, localX, localZ),
                        minY, wallTop, ceilingY,
                        sink
                );

                touched = true;
            }
        }

        return touched;
    }

    private void renderColumn(
            int x,
            int z,
            boolean wall,
            int minY,
            int wallTop,
            int ceilingY,
            BlockSink sink
    ) {
        // 1. Fill downward so there is no hole under the floor
        //    (ocean, ravine, cave, valley).
        int depth = 0;

        for (int y = floorY - 1;
             y >= minY && depth < maxFillDepth;
             y--, depth++) {

            if (sink.getType(x, y, z).isSolid()) {
                break;
            }

            sink.setType(x, y, z, FILL);
        }

        // 2. Floor.
        sink.setType(x, floorY, z, FLOOR);

        // 3. Everything between the floor and the ceiling layer.
        for (int y = floorY + 1; y <= wallTop; y++) {

            if (wall) {
                sink.setType(x, y, z, WALL);

            } else if (!sink.getType(x, y, z).isAir()) {
                sink.setType(x, y, z, Material.AIR);
            }
        }

        // 4. Invisible ceiling.
        sink.setType(x, ceilingY, z, CEILING);
    }

    // ---------------------------------------------------------------
    // Wall shape (same rules as MazeCellBuilder)
    // ---------------------------------------------------------------

    private boolean isWallColumn(Cell cell, int localX, int localZ) {

        int last = CELL_SIZE - 1;

        return (localZ == 0
                && sideIsSolid(cell, CellDirection.NORTH, localX))
                || (localZ == last
                && sideIsSolid(cell, CellDirection.SOUTH, localX))
                || (localX == 0
                && sideIsSolid(cell, CellDirection.WEST, localZ))
                || (localX == last
                && sideIsSolid(cell, CellDirection.EAST, localZ));
    }

    private boolean sideIsSolid(
            Cell cell,
            CellDirection direction,
            int position
    ) {
        // Large room: the whole shared wall is removed.
        if (cell.hasLargeRoomConnection(direction)) {
            return false;
        }

        // Doorway: only the middle of the wall is open.
        return !(cell.hasOpening(direction) && isOpeningPosition(position));
    }

    private boolean isOpeningPosition(int position) {
        return position >= OPENING_CENTER - OPENING_HALF_WIDTH
                && position <= OPENING_CENTER + OPENING_HALF_WIDTH;
    }
}