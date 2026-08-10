package net.mazebound.commands;

import net.mazebound.exploration.Cell;
import net.mazebound.exploration.MazeGenerator;
import net.mazebound.exploration.RegionManager;
import net.mazebound.world.WorldPlanner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class MazeTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        long seed = System.currentTimeMillis();

        if (args.length > 0) {
            try {
                seed = Long.parseLong(args[0]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§cInvalid seed.");
                return true;
            }
        }

        RegionManager regionManager =
                new RegionManager();

        WorldPlanner planner =
                new WorldPlanner(regionManager);

        MazeGenerator generator =
                new MazeGenerator(seed);

        Map<String, Cell> cells = generator.generate(
                -10,
                10,
                -10,
                10,
                planner
        );

        sender.sendMessage(
                "§aGenerated §f" + cells.size() + " §acells."
        );

        sender.sendMessage("§7Seed: §f" + seed);

        return true;
    }
}