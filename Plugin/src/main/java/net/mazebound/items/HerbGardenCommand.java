package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HerbGardenCommand implements CommandExecutor {

    private final Mazebound plugin;
    private final HerbGardenManager gardens;

    public HerbGardenCommand(Mazebound plugin, HerbGardenManager gardens) {
        this.plugin = plugin;
        this.gardens = gardens;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player) || !player.isOp()) {
            sender.sendMessage(ChatColor.RED
                    + "Only an in-game OP can manage herb gardens.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW
                    + "Usage: /herbgarden add | remove | list");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                Block target = player.getTargetBlockExact(6);

                if (target == null) {
                    player.sendMessage(ChatColor.RED
                            + "Look at the block where the plant should grow.");
                    return true;
                }

                if (gardens.addPlant(target.getLocation())) {
                    player.sendMessage(ChatColor.GREEN
                            + "Registered herb garden plant.");
                } else {
                    player.sendMessage(ChatColor.RED
                            + "A garden plant is already registered there.");
                }
            }
            case "remove" -> {
                Block target = player.getTargetBlockExact(6);

                if (target == null
                        || !gardens.removePlant(target.getLocation())) {
                    player.sendMessage(ChatColor.RED
                            + "Look at a registered garden plant.");
                    return true;
                }

                player.sendMessage(ChatColor.GREEN
                        + "Removed herb garden plant.");
            }
            case "list" -> player.sendMessage(ChatColor.GREEN
                    + "Registered garden plants: " + gardens.getPlantCount());
            default -> player.sendMessage(ChatColor.YELLOW
                    + "Usage: /herbgarden add | remove | list");
        }

        return true;
    }
}