package net.mazebound.exploration;

import net.mazebound.world.WorldPlanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Generates the maze layout (which cells exist, which walls have doorways,
 * which neighboring cells are merged into large rooms).
 *
 * Fully deterministic: the same seed and bounds always give the same maze,
 * no matter how many times generate() is called on the same instance.
 *
 * Connectivity note: every maze cell is reachable through normal openings
 * (the DFS creates a spanning tree). Large-room connections add extra loops
 * on top of that. For path-distance calculations (boss/loot placement),
 * treat both hasOpening() and hasLargeRoomConnection() as passable.
 */
public class MazeGenerator {

    private static final long LARGE_ROOM_SALT = 12345L;
    private static final long ENTRANCE_SALT = 987654321L;

    /** Fraction of cells that get a large-room connection (0.03 = 3%). */
    private static final double DEFAULT_LARGE_ROOM_DENSITY = 0.03;

    private static final CellDirection[] LARGE_ROOM_DIRECTIONS = {
            CellDirection.EAST,
            CellDirection.SOUTH
    };

    private final long seed;
    private double largeRoomDensity = DEFAULT_LARGE_ROOM_DENSITY;

    public MazeGenerator(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    /** Sets how many large-room connections are created (fraction of cells). */
    public MazeGenerator withLargeRoomDensity(double density) {
        this.largeRoomDensity = Math.max(0.0, Math.min(1.0, density));
        return this;
    }

    /**
     * Generates the real maze: hub cells are left out and the four spawn
     * entrances are opened toward the hub.
     */
    public Map<String, Cell> generate(
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            WorldPlanner worldPlanner
    ) {
        Map<String, Cell> cells = createCells(
                minX, maxX, minZ, maxZ,
                cell -> worldPlanner.getRegionType(cell) != RegionType.HUB
        );

        carvePassages(cells, new Random(seed));
        addSpawnEntrances(cells);
        addLargeRoomConnections(cells, new Random(seed + LARGE_ROOM_SALT));

        return cells;
    }

    /**
     * Generates a standalone patch (used by /spawnmaze): no hub handling and
     * no spawn entrances, every cell in the bounds is included.
     */
    public Map<String, Cell> generatePatch(
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        Map<String, Cell> cells = createCells(
                minX, maxX, minZ, maxZ,
                cell -> true
        );

        carvePassages(cells, new Random(seed));
        addLargeRoomConnections(cells, new Random(seed + LARGE_ROOM_SALT));

        return cells;
    }

    /**
     * Generates the real maze as a square ring around the world origin.
     *
     * A cell belongs to the maze when max(|x|, |z|) is between innerRadius
     * and outerRadius (in cells, inclusive). Everything closer to the origin
     * is left alone (normal terrain). Doorways are cut through the inner
     * wall so players can walk in from the wilderness; the outer edge stays
     * solid.
     *
     * @param entrancesPerSide how many gateways each of the four inner
     *                         sides gets (placed with seeded jitter)
     */
    public Map<String, Cell> generateRing(
            int innerRadius,
            int outerRadius,
            int entrancesPerSide
    ) {
        if (innerRadius < 1 || outerRadius <= innerRadius) {
            throw new IllegalArgumentException(
                    "Need 1 <= innerRadius < outerRadius, got "
                            + innerRadius + " and " + outerRadius
            );
        }

        Map<String, Cell> cells = createCells(
                -outerRadius, outerRadius, -outerRadius, outerRadius,
                cell -> Math.max(Math.abs(cell.x()), Math.abs(cell.z()))
                        >= innerRadius
        );

        carvePassages(cells, new Random(seed));
        addRingEntrances(
                cells,
                innerRadius,
                entrancesPerSide,
                new Random(seed + ENTRANCE_SALT)
        );
        addLargeRoomConnections(cells, new Random(seed + LARGE_ROOM_SALT));

        return cells;
    }

    private Map<String, Cell> createCells(
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            Predicate<Cell> include
    ) {
        // LinkedHashMap keeps a stable iteration order (x, then z),
        // so generation never depends on hash ordering.
        Map<String, Cell> cells = new LinkedHashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                Cell cell = new Cell(x, z);

                if (include.test(cell)) {
                    cells.put(cell.getId(), cell);
                }
            }
        }

        return cells;
    }

    // ---------------------------------------------------------------
    // Passages (iterative randomized depth-first search)
    // ---------------------------------------------------------------

    private void carvePassages(Map<String, Cell> cells, Random random) {
        Set<String> visited = new HashSet<>();

        // One DFS per connected region (the hub can split the grid).
        for (Cell start : cells.values()) {

            if (visited.contains(start.getId())) {
                continue;
            }

            carveFrom(start, cells, visited, random);
        }
    }

    private void carveFrom(
            Cell start,
            Map<String, Cell> cells,
            Set<String> visited,
            Random random
    ) {
        Deque<Cell> stack = new ArrayDeque<>();

        visited.add(start.getId());
        stack.push(start);

        while (!stack.isEmpty()) {

            Cell current = stack.peek();

            List<CellDirection> options =
                    unvisitedDirections(current, cells, visited);

            if (options.isEmpty()) {
                stack.pop();
                continue;
            }

            CellDirection direction =
                    options.get(random.nextInt(options.size()));

            Cell next = getNeighbor(current, direction, cells);

            current.addOpening(direction);
            next.addOpening(direction.opposite());

            visited.add(next.getId());
            stack.push(next);
        }
    }

    private List<CellDirection> unvisitedDirections(
            Cell cell,
            Map<String, Cell> cells,
            Set<String> visited
    ) {
        List<CellDirection> options = new ArrayList<>(4);

        for (CellDirection direction : CellDirection.values()) {

            Cell neighbor = getNeighbor(cell, direction, cells);

            if (neighbor != null && !visited.contains(neighbor.getId())) {
                options.add(direction);
            }
        }

        return options;
    }

    private Cell getNeighbor(
            Cell cell,
            CellDirection direction,
            Map<String, Cell> cells
    ) {
        return cells.get(
                (cell.x() + direction.getXOffset())
                        + ","
                        + (cell.z() + direction.getZOffset())
        );
    }

    // ---------------------------------------------------------------
    // Spawn entrances
    // ---------------------------------------------------------------

    private void addSpawnEntrances(Map<String, Cell> cells) {

        // First maze ring outside the hub. Derived from the hub size so
        // changing HUB_SIZE can never wall the hub off.
        int edge = RegionManager.HUB_RADIUS + 1;

        openEntrance(cells, 0, -edge, CellDirection.SOUTH);
        openEntrance(cells, edge, 0, CellDirection.WEST);
        openEntrance(cells, 0, edge, CellDirection.NORTH);
        openEntrance(cells, -edge, 0, CellDirection.EAST);
    }

    private void openEntrance(
            Map<String, Cell> cells,
            int x,
            int z,
            CellDirection direction
    ) {
        Cell cell = cells.get(x + "," + z);

        if (cell != null) {
            cell.addOpening(direction);
        }
    }

    /**
     * Cuts gateways through the inner wall of the ring. Each side gets
     * entrancesPerSide gateways, one per equal-sized segment of that side
     * (corners excluded), at a seeded random position inside the segment.
     */
    private void addRingEntrances(
            Map<String, Cell> cells,
            int inner,
            int entrancesPerSide,
            Random random
    ) {
        int span = 2 * inner - 1;
        int count = Math.min(entrancesPerSide, span);

        if (count <= 0) {
            return;
        }

        for (int side = 0; side < 4; side++) {
            for (int i = 0; i < count; i++) {

                int segmentStart = i * span / count;
                int segmentEnd = (i + 1) * span / count;

                int pick = segmentStart + random.nextInt(
                        Math.max(1, segmentEnd - segmentStart)
                );

                int offset = -(inner - 1) + pick;

                switch (side) {
                    case 0 -> openEntrance(cells, offset, -inner, CellDirection.SOUTH);
                    case 1 -> openEntrance(cells, offset, inner, CellDirection.NORTH);
                    case 2 -> openEntrance(cells, -inner, offset, CellDirection.EAST);
                    default -> openEntrance(cells, inner, offset, CellDirection.WEST);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Large rooms (two neighboring cells merged, shared wall removed)
    // ---------------------------------------------------------------

    private void addLargeRoomConnections(
            Map<String, Cell> cells,
            Random random
    ) {
        int target = (int) Math.round(cells.size() * largeRoomDensity);

        if (target <= 0) {
            return;
        }

        List<Cell> candidates = new ArrayList<>(cells.values());
        Collections.shuffle(candidates, random);

        int created = 0;

        for (Cell cell : candidates) {

            for (CellDirection direction : LARGE_ROOM_DIRECTIONS) {

                Cell neighbor = getNeighbor(cell, direction, cells);

                if (neighbor == null
                        || cell.hasLargeRoomConnection(direction)) {
                    continue;
                }

                cell.addLargeRoomConnection(direction);
                neighbor.addLargeRoomConnection(direction.opposite());

                created++;

                if (created >= target) {
                    return;
                }
            }
        }
    }
}