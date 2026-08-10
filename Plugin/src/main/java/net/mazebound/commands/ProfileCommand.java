package net.mazebound.commands;

import net.mazebound.Mazebound;
import net.mazebound.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfileCommand implements CommandExecutor {

    private final Mazebound plugin;

    public ProfileCommand(Mazebound plugin) {
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

        PlayerData data = plugin.getPlayerDataManager()
                .getPlayerData(player.getUniqueId());

        player.sendMessage("§6§l=== Mazebound Profile ===");
        player.sendMessage("§eXP: §f" + data.getXp());
        player.sendMessage(
                "§eCells discovered: §f"
                        + data.getCellsDiscovered()
        );

        return true;
    }
}