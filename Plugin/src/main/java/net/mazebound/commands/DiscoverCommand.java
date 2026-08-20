package net.mazebound.commands;

import net.mazebound.Mazebound;
import net.mazebound.exploration.Cell;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscoverCommand implements CommandExecutor {

    private final Mazebound plugin;

    public DiscoverCommand(Mazebound plugin) {
        this.plugin = plugin;
    }

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

        Cell cell = plugin.getExplorationManager()
                .getCellFromLocation(player.getLocation());

        boolean discovered = plugin.getExplorationManager()
        .discoverCell(
                player.getUniqueId(),
                player.getLocation()
        );

        if (discovered) {
            player.sendMessage(
                    "§aDiscovered cell §f" + cell.getId() + "§a!"
            );
        } else {
            player.sendMessage(
                    "§eYou already discovered this cell."
            );
        }

        return true;
    }
}