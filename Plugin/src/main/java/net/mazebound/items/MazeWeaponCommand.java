package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MazeWeaponCommand implements CommandExecutor {
    private final Mazebound plugin;
    private final MazeStrideWeaponListener weaponListener;

    public MazeWeaponCommand(Mazebound plugin, MazeStrideWeaponListener weaponListener) {
        this.plugin = plugin;
        this.weaponListener = weaponListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mazebound.weapon.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage Mazebound weapons.");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mazewpn give <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "That player isn't online.");
            return true;
        }

        ItemStack weapon = weaponListener.createWeapon();
        target.getInventory().addItem(weapon).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        sender.sendMessage(ChatColor.GREEN + "Gave Maze Stride to " + target.getName() + ".");
        target.sendMessage(ChatColor.LIGHT_PURPLE + "You received Maze Stride, an epic exploration weapon.");
        return true;
    }
}
