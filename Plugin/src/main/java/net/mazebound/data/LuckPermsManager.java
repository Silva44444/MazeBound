package net.mazebound.data;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public class LuckPermsManager {

    private final LuckPerms luckPerms;
    private final PlayerDataManager playerDataManager;

    public LuckPermsManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        this.luckPerms = LuckPermsProvider.get();
    }

   public void updateRank(Player player) {

    PlayerData data =
            playerDataManager.getPlayerData(
                    player.getUniqueId()
            );

    MazeboundRank rank = data.getRank();

    String groupName = getGroupName(rank);
    String prefix = rank.getPrefix();

    User user =
            luckPerms.getUserManager()
                    .getUser(player.getUniqueId());

    if (user == null) {
        return;
    }

    // Remove old Mazebound rank groups
    for (MazeboundRank oldRank : MazeboundRank.values()) {

        String oldGroup = getGroupName(oldRank);

        user.data().remove(
                net.luckperms.api.node.types.InheritanceNode
                        .builder(oldGroup)
                        .build()
        );
    }

    // Add current Mazebound rank
    user.data().add(
            net.luckperms.api.node.types.InheritanceNode
                    .builder(groupName)
                    .build()
    );

    // Remove old Mazebound prefixes
    for (MazeboundRank oldRank : MazeboundRank.values()) {

        user.data().remove(
                net.luckperms.api.node.types.PrefixNode
                        .builder(oldRank.getPrefix(), 100)
                        .build()
        );
    }

    // Add current prefix
    user.data().add(
            net.luckperms.api.node.types.PrefixNode
                    .builder(prefix, 100)
                    .build()
    );

    // Make current rank the primary group
    user.setPrimaryGroup(groupName);

    luckPerms.getUserManager()
            .saveUser(user);
}

    private String getGroupName(MazeboundRank rank) {

        return switch (rank) {

            case WANDERER -> "wanderer";
            case PATHFINDER -> "pathfinder";
            case EXPLORER -> "explorer";
            case TRAILBLAZER -> "trailblazer";
            case CARTOGRAPHER -> "cartographer";
            case WAYFINDER -> "wayfinder";
            case MAZE_MASTER -> "maze-master";
            case LEGEND -> "legend";
        };
    }
}