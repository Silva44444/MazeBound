package net.mazebound.commands;

import net.mazebound.exploration.Cell;
import net.mazebound.world.MazeCellBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildCellCommand implements CommandExecutor {

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

        int cellX = 0;
        int cellZ = 0;

        if (args.length >= 2) {
            try {
                cellX = Integer.parseInt(args[0]);
                cellZ = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("§cInvalid cell coordinates.");
                return true;
            }
        }

        Cell cell = new Cell(cellX, cellZ);

        MazeCellBuilder builder =
                new MazeCellBuilder(player.getWorld());

        builder.build(cell);

        player.sendMessage(
                "§aBuilt cell §f"
                        + cell.getId()
                        + " §aat Y=50."
        );

        return true;
    }
}