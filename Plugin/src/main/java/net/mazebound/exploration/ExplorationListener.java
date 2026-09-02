package net.mazebound.exploration;

import net.mazebound.Mazebound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ExplorationListener implements Listener {

    private final Mazebound plugin;

    public ExplorationListener(Mazebound plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        boolean discovered = plugin.getExplorationManager()
        .discoverCell(
                player.getUniqueId(),
                event.getTo()
        );

Cell cell = plugin.getExplorationManager()
        .getCellFromLocation(event.getTo());

String cellId = cell.getId();

        if (discovered) {

    player.sendMessage(
            "§aDiscovered cell §f" + cellId + "§a!"
    );

    plugin.getLuckPermsManager()
            .updateRank(player);
}
    }
}