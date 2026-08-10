package net.mazebound.exploration;

import net.mazebound.world.WorldPlanner;
import net.mazebound.exploration.RegionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

public class MazeGenerator {

    private final Random random;

    public MazeGenerator(long seed) {
        random = new Random(seed);
    }

    public Map<String, Cell> generate(
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            WorldPlanner worldPlanner
    ) {
        Map<String, Cell> cells = new HashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                Cell cell = new Cell(x, z);

                if (worldPlanner.getRegionType(cell)
        == RegionType.HUB) {
                    continue;
                }

                cells.put(cell.getId(), cell);
            }
        }

        Set<String> visited = new HashSet<>();

        for (Cell cell : cells.values()) {

            if (visited.contains(cell.getId())) {
                continue;
            }

            generateFromCell(
                    cell,
                    cells,
                    visited,
                    minX,
                    maxX,
                    minZ,
                    maxZ
            );
        }

        addSpawnEntrances(cells);

        return cells;
    }

    private void generateFromCell(
            Cell current,
            Map<String, Cell> cells,
            Set<String> visited,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        visited.add(current.getId());

        List<CellDirection> directions =
                new ArrayList<>(List.of(CellDirection.values()));

        Collections.shuffle(directions, random);

        for (CellDirection direction : directions) {

            int nextX =
                    current.x() + direction.getXOffset();

            int nextZ =
                    current.z() + direction.getZOffset();

            if (nextX < minX || nextX > maxX
                    || nextZ < minZ || nextZ > maxZ) {
                continue;
            }

            String nextId = nextX + "," + nextZ;

            if (visited.contains(nextId)) {
                continue;
            }

            Cell next = cells.get(nextId);

            if (next == null) {
                continue;
            }

            current.addOpening(direction);
            next.addOpening(direction.opposite());

            generateFromCell(
                    next,
                    cells,
                    visited,
                    minX,
                    maxX,
                    minZ,
                    maxZ
            );
        }
    }

    private void addSpawnEntrances(
            Map<String, Cell> cells
    ) {
        Cell north = cells.get("0,-5");
        if (north != null) {
            north.addOpening(CellDirection.SOUTH);
        }

        Cell east = cells.get("5,0");
        if (east != null) {
            east.addOpening(CellDirection.WEST);
        }

        Cell south = cells.get("0,5");
        if (south != null) {
            south.addOpening(CellDirection.NORTH);
        }

        Cell west = cells.get("-5,0");
        if (west != null) {
            west.addOpening(CellDirection.EAST);
        }
    }
}