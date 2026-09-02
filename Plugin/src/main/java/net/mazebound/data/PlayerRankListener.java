package net.mazebound.data;

import net.mazebound.Mazebound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerRankListener implements Listener {

    private final Mazebound plugin;

    public PlayerRankListener(Mazebound plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        plugin.getLuckPermsManager()
                .updateRank(event.getPlayer());
    }
}