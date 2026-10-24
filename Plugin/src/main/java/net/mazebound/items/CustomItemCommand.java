
package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CustomItemCommand implements CommandExecutor {

    private final CustomItemManager itemManager;

    public CustomItemCommand(Mazebound plugin, CustomItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be OP to use this command.");
            return true;
        }

        if (args.length != 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.YELLOW
                    + "Usage: /mazebound give <player> herbal_remedy");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "That player is not online.");
            return true;
        }

        if (!args[2].equalsIgnoreCase("herbal_remedy")) {
            sender.sendMessage(ChatColor.RED + "Unknown item. Use herbal_remedy.");
            return true;
        }

        ItemStack item = itemManager.createHerbalRemedy();
        target.getInventory().addItem(item);

        sender.sendMessage(ChatColor.GREEN + "Gave Herbal Remedy to "
                + target.getName() + ".");

        if (target != sender) {
            target.sendMessage(ChatColor.GREEN + "You received an Herbal Remedy.");
        }

        return true;
    }
}
