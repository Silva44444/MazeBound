package net.mazebound.commands;

import net.mazebound.exploration.Cell;
import net.mazebound.world.MazeCellBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SpawnMazeCommand implements CommandExecutor {

    private static final int SIZE = 5;
    private static final int HALF = SIZE / 2;
    private static final int CELL_SIZE = 55;

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        long seed = System.currentTimeMillis();

        if (args.length > 0) {
            try {
                seed = Long.parseLong(args[0]);
            } catch (NumberFormatException exception) {
                player.sendMessage("§cInvalid seed.");
                return true;
            }
        }

        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();

        int originX =
                Math.floorDiv(centerX, CELL_SIZE) - HALF;

        int originZ =
                Math.floorDiv(centerZ, CELL_SIZE) - HALF;

        Map<String, Cell> cells = new HashMap<>();

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {

                int cellX = originX + x;
                int cellZ = originZ + z;

                Cell cell = new Cell(cellX, cellZ);

                cells.put(cell.getId(), cell);
            }
        }

        // Generate a connected maze through the 5x5 cells.
        generateMaze(cells, originX, originX + SIZE - 1,
                originZ, originZ + SIZE - 1, seed);

        MazeCellBuilder builder =
                new MazeCellBuilder(player.getWorld());

        for (Cell cell : cells.values()) {
            builder.build(cell);
        }

        player.sendMessage("§a§l5x5 MAZE SPAWNED!");
        player.sendMessage("§7Seed: §f" + seed);
        player.sendMessage(
                "§7Cells: §f" + cells.size()
        );

        return true;
    }

    private void generateMaze(
            Map<String, Cell> cells,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            long seed
    ) {
        java.util.Random random =
                new java.util.Random(seed);

        java.util.Set<String> visited =
                new java.util.HashSet<>();

        Cell start = cells.get(minX + "," + minZ);

        generateFromCell(
                start,
                cells,
                visited,
                minX,
                maxX,
                minZ,
                maxZ,
                random
        );
    }

    private void generateFromCell(
            Cell current,
            Map<String, Cell> cells,
            java.util.Set<String> visited,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            java.util.Random random
    ) {
        visited.add(current.getId());

        java.util.List<net.mazebound.exploration.CellDirection>
                directions =
                new java.util.ArrayList<>(
                        java.util.List.of(
                                net.mazebound.exploration.CellDirection.values()
                        )
                );

        java.util.Collections.shuffle(
                directions,
                random
        );

        for (net.mazebound.exploration.CellDirection direction
                : directions) {

            int nextX =
                    current.x() + direction.getXOffset();

            int nextZ =
                    current.z() + direction.getZOffset();

            if (nextX < minX || nextX > maxX
                    || nextZ < minZ || nextZ > maxZ) {
                continue;
            }

            Cell next =
                    cells.get(nextX + "," + nextZ);

            if (next == null
                    || visited.contains(next.getId())) {
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
                    maxZ,
                    random
            );
        }
    }
}