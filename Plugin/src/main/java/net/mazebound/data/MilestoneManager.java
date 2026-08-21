package net.mazebound.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MilestoneManager {

    private static final int[] MILESTONES = {
            10,
            25,
            50,
            100,
            200,
            400,
            800
    };

    private final PlayerDataManager playerDataManager;

    public MilestoneManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    public void checkMilestones(UUID playerId) {

        PlayerData data =
                playerDataManager.getPlayerData(playerId);

        int cells = data.getCellsDiscovered();

        for (int milestone : MILESTONES) {

            if (cells < milestone) {
                continue;
            }

            if (data.hasMilestone(milestone)) {
                continue;
            }

            unlockMilestone(playerId, data, milestone);
        }
    }

    private void unlockMilestone(
            UUID playerId,
            PlayerData data,
            int milestone
    ) {
        data.unlockMilestone(milestone);

        int bonusXp = milestone * 5;
        data.addXp(bonusXp);

        Player player = Bukkit.getPlayer(playerId);

        if (player == null) {
            return;
        }

        player.sendMessage("");
        player.sendMessage(
                "§6§l✦ MILESTONE UNLOCKED ✦"
        );
        player.sendMessage(
                "§eExplorer §f" + milestone
                        + " §ecells discovered!"
        );
        player.sendMessage(
                "§a+" + bonusXp + " XP"
        );
        player.sendMessage("");
    }
}