package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class WaystoneCommand implements CommandExecutor {
    private final Mazebound plugin;
    private final WaystoneManager manager;
    private final WaystoneRuneListener runeListener;

    public WaystoneCommand(Mazebound plugin, WaystoneManager manager, WaystoneRuneListener runeListener) {
        this.plugin = plugin;
        this.manager = manager;
        this.runeListener = runeListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mazebound.waystone.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage waystones.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /waystone set <id> <name...> | remove <id> | list | give <player>");
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only a player can set a waystone location.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /waystone set <id> <name...> (look at the block to use as the waystone)");
                    return true;
                }
                String id = WaystoneManager.normalize(args[1]);
                if (id.isBlank()) {
                    sender.sendMessage(ChatColor.RED + "Use an ID containing letters, numbers, underscores, or hyphens.");
                    return true;
                }
                org.bukkit.block.Block target = player.getTargetBlockExact(6);
                if (target == null || target.isPassable()) {
                    sender.sendMessage(ChatColor.RED + "Look at a solid block within 6 blocks.");
                    return true;
                }
                String name = java.util.Arrays.stream(args, 2, args.length).collect(Collectors.joining(" "));
                manager.setWaystone(id, name, target.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Registered waystone '" + name + "' (" + id + ") at "
                        + target.getX() + ", " + target.getY() + ", " + target.getZ() + ".");
                sender.sendMessage(ChatColor.GRAY + "Players must right-click this block to discover it.");
                return true;
            }
            case "remove" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /waystone remove <id>");
                    return true;
                }
                if (manager.removeWaystone(args[1])) sender.sendMessage(ChatColor.GREEN + "Waystone removed.");
                else sender.sendMessage(ChatColor.RED + "No waystone exists with that ID.");
                return true;
            }
            case "list" -> {
                if (manager.getWaystones().isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No waystones registered yet.");
                    return true;
                }
                sender.sendMessage(ChatColor.AQUA + "Registered waystones:");
                manager.getWaystones().stream()
                        .sorted(Comparator.comparing(WaystoneManager.Waystone::id))
                        .forEach(stone -> sender.sendMessage(ChatColor.GRAY + "- " + stone.id()
                                + ChatColor.WHITE + " — " + stone.name() + ChatColor.DARK_GRAY + " ("
                                + stone.blockLocation().getWorld().getName() + " "
                                + stone.blockLocation().getBlockX() + ", "
                                + stone.blockLocation().getBlockY() + ", "
                                + stone.blockLocation().getBlockZ() + ")"));
                return true;
            }
            case "give" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /waystone give <player>");
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "That player isn't online.");
                    return true;
                }
                ItemStack rune = runeListener.createRune();
                var leftovers = target.getInventory().addItem(rune);
                leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                sender.sendMessage(ChatColor.GREEN + "Gave a Waystone Rune to " + target.getName() + ".");
                target.sendMessage(ChatColor.AQUA + "You received a Waystone Rune.");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /waystone set <id> <name...> | remove <id> | list | give <player>");
                return true;
            }
        }
    }
}