package net.mazebound.data;

import java.util.HashMap;
import java.util.UUID;

public class PlayerDataManager {

    private final HashMap<UUID, PlayerData> players;

    public PlayerDataManager() {
        players = new HashMap<>();
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (!players.containsKey(uuid)) {
            players.put(uuid, new PlayerData(uuid));
        }

        return players.get(uuid);
    }

    public void removePlayerData(UUID uuid) {
        players.remove(uuid);
    }
}