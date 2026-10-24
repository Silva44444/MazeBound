package net.mazebound.world;

import net.mazebound.Mazebound;
import net.mazebound.exploration.Cell;
import net.mazebound.exploration.CellDirection;
import org.bukkit.Material;

/**
 * Renders the maze into one chunk at a time.
 *
 * Terrain modes (maze.terrain-mode in config.yml):
 *
 *   KEEP  natural terrain (hills, trees, water) stays as it is. Only the
 *         walls and the barrier ceiling are added. Wall columns are solid
 *         from the bottom of the world, so caves cannot be used to get
 *         under a wall. NOTE: terrain can block doorways.
 *   FLAT  every column inside a maze cell is overwritten, so nothing
 *         natural survives and the maze is fully walkable:
 *
 *           maxY - 1            barrier layer (stops elytra / pearls)
 *           floorY+1 .. maxY-2  wall column, or air in corridors and rooms
 *           floorY              stone floor
 *           below floorY        filled downward until solid ground
 *
 * Walls: every cell contributes wallThickness blocks on each of its four
 * sides, so the wall between two neighboring cells is twice that thick.
 * Doorways are 11 blocks wide and run straight through the full thickness.
 *
 * Wall blocks: with biome themes on, each wall block is chosen from the
 * palette of the biome at that spot (see BiomePalettes). The choice is a
 * pure function of seed and position, so it is identical on every run and
 * never depends on chunk generation order.
 *
 * Hub cells and everything outside the maze are left untouched.
 */
public class MazeRenderer {

    private static final int CELL_SIZE = Mazebound.CELL_SIZE;

    // Doorway: 11 blocks wide, centered in the cell.
    // Keep in sync with MazeCellBuilder until that class is retired.
    private static final int OPENING_CENTER = 27;
    private static final int OPENING_HALF_WIDTH = 5;

    public static final int MIN_WALL_THICKNESS = 1;
    public static final int MAX_WALL_THICKNESS = 8;

    // Rare brushable blocks only appear in this height band (they are
    // block entities, so they must not be scattered through the whole
    // underground part of every wall).
    private static final int RARE_MIN_Y = 40;
    private static final int RARE_MAX_Y = 160;

    // Biome is read this far below the top of the world, i.e. in the sky
    // above the terrain, so cave biomes never leak into the wall palette.
    private static final int BIOME_SAMPLE_DEPTH = 48;

    private static final Material WALL = Material.STONE_BRICKS;
    private static final Material FLOOR = Material.STONE;
    private static final Material FILL = Material.STONE;
    private static final Material CEILING = Material.BARRIER;

    public enum TerrainMode {

        /** Flat stone floor; everything natural inside the maze is replaced. */
        FLAT,

        /** Natural terrain stays; only walls and the barrier ceiling are added. */
        KEEP;

        public static TerrainMode parse(String value) {

            if (value != null) {
                for (TerrainMode mode : values()) {
                    if (mode.name().equalsIgnoreCase(value.trim())) {
                        return mode;
                    }
                }
            }

            throw new IllegalArgumentException(
                    "maze.terrain-mode must be 'flat' or 'keep', but was '"
                            + value + "'"
            );
        }
    }

    private final MazeLayout layout;
    private final int floorY;
    private final int maxFillDepth;
    private final TerrainMode mode;
    private final int wallThickness;
    private final boolean biomeThemes;
    private final long seed;

    /** FLAT mode, 1-block walls, plain stone bricks. */
    public MazeRenderer(MazeLayout layout, int floorY, int maxFillDepth) {
        this(layout, floorY, maxFillDepth, TerrainMode.FLAT);
    }

    /** 1-block walls, plain stone bricks. */
    public MazeRenderer(
            MazeLayout layout,
            int floorY,
            int maxFillDepth,
            TerrainMode mode
    ) {
        this(layout, floorY, maxFillDepth, mode, 1, false);
    }

    public MazeRenderer(
            MazeLayout layout,
            int floorY,
            int maxFillDepth,
            TerrainMode mode,
            int wallThickness,
            boolean biomeThemes
    ) {
        if (wallThickness < MIN_WALL_THICKNESS
                || wallThickness > MAX_WALL_THICKNESS) {
            throw new IllegalArgumentException(
                    "wall thickness must be between " + MIN_WALL_THICKNESS
                            + " and " + MAX_WALL_THICKNESS
                            + ", but was " + wallThickness
            );
        }

        this.layout = layout;
        this.floorY = floorY;
        this.maxFillDepth = maxFillDepth;
        this.mode = mode;
        this.wallThickness = wallThickness;
        this.biomeThemes = biomeThemes;
        this.seed = layout.seed();
    }

    public int floorY() {
        return floorY;
    }

    public TerrainMode mode() {
        return mode;
    }

    public int wallThickness() {
        return wallThickness;
    }

    /** True if the configured floor leaves room for walls in this world. */
    public boolean fitsWorld(int minY, int maxY) {

        if (mode == TerrainMode.KEEP) {
            return maxY - minY >= 16;
        }

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
        int sampleY = Math.max(minY, maxY - BIOME_SAMPLE_DEPTH);

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

                boolean wall = isWallColumn(cell, localX, localZ);

                if (mode == TerrainMode.KEEP) {
                    renderKeptColumn(
                            x, z, wall, minY, wallTop, ceilingY, sampleY, sink
                    );
                } else {
                    renderColumn(
                            x, z, wall, minY, wallTop, ceilingY, sampleY, sink
                    );
                }

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
            int sampleY,
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
        if (wall) {
            placeWall(sink, x, z, floorY + 1, wallTop, sampleY);

        } else {
            for (int y = floorY + 1; y <= wallTop; y++) {
                if (!sink.getType(x, y, z).isAir()) {
                    sink.setType(x, y, z, Material.AIR);
                }
            }
        }

        // 4. Invisible ceiling.
        sink.setType(x, ceilingY, z, CEILING);
    }

    /**
     * KEEP mode: natural terrain stays untouched. Wall columns are solid
     * from just above the bottom bedrock layer to the top, so a cave can
     * never be used to walk underneath a wall.
     */
    private void renderKeptColumn(
            int x,
            int z,
            boolean wall,
            int minY,
            int wallTop,
            int ceilingY,
            int sampleY,
            BlockSink sink
    ) {
        if (wall) {
            placeWall(sink, x, z, minY + 1, wallTop, sampleY);
        }

        sink.setType(x, ceilingY, z, CEILING);
    }

    // ---------------------------------------------------------------
    // Wall blocks
    // ---------------------------------------------------------------

    private void placeWall(
            BlockSink sink,
            int x,
            int z,
            int fromY,
            int toY,
            int sampleY
    ) {
        if (!biomeThemes) {
            for (int y = fromY; y <= toY; y++) {
                sink.setType(x, y, z, WALL);
            }
            return;
        }

        BiomePalettes.Palette palette =
                BiomePalettes.forBiome(sink.getBiomeKey(x, sampleY, z));

        for (int y = fromY; y <= toY; y++) {

            boolean rareAllowed = y >= RARE_MIN_Y && y <= RARE_MAX_Y;

            BiomePalettes.Entry entry =
                    palette.pick(roll(x, y, z), rareAllowed);

            if (entry.isRare()) {
                sink.setBrushable(
                        x, y, z, entry.material(), entry.lootTable()
                );
            } else {
                sink.setType(x, y, z, entry.material());
            }
        }
    }

    /** Deterministic number in [0, SCALE) from seed and position. */
    private int roll(int x, int y, int z) {

        long h = seed
                ^ (x * 0x9E3779B97F4A7C15L)
                ^ (y * 0xC2B2AE3D27D4EB4FL)
                ^ (z * 0x165667B19E3779F9L);

        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;

        return (int) Math.floorMod(h, (long) BiomePalettes.SCALE);
    }

    // ---------------------------------------------------------------
    // Wall shape (same rules as MazeCellBuilder, but wallThickness deep)
    // ---------------------------------------------------------------

    private boolean isWallColumn(Cell cell, int localX, int localZ) {

        int last = CELL_SIZE - 1;
        int t = wallThickness;

        return (localZ < t
                && sideIsSolid(cell, CellDirection.NORTH, localX))
                || (localZ > last - t
                && sideIsSolid(cell, CellDirection.SOUTH, localX))
                || (localX < t
                && sideIsSolid(cell, CellDirection.WEST, localZ))
                || (localX > last - t
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