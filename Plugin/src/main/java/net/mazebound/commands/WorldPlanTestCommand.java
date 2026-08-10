package net.mazebound.commands;

import net.mazebound.exploration.Cell;
import net.mazebound.exploration.RegionManager;
import net.mazebound.exploration.RegionType;
import net.mazebound.world.WorldPlanner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class WorldPlanTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        RegionManager regionManager =
                new RegionManager();

        WorldPlanner planner =
                new WorldPlanner(regionManager);

        sender.sendMessage("§6§l=== World Plan Test ===");

        for (int z = -2; z <= 2; z++) {

            StringBuilder row = new StringBuilder();

            for (int x = -2; x <= 2; x++) {

                Cell cell = new Cell(x, z);

                RegionType type =
                        planner.getRegionType(cell);

                row.append(symbol(type));
            }

            sender.sendMessage("§f" + row);
        }

        return true;
    }

    private String symbol(RegionType type) {
        return switch (type) {
            case HUB -> "H";
            case MAZE -> "M";
            case SAFE_AREA -> "S";
            case CAVE -> "C";
            case BOSS -> "B";
            case SPECIAL -> "X";
        };
    }
}