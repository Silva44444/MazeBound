package net.mazebound.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class PlayerDataManager {

    private final HashMap<UUID, PlayerData> players;
    private final File dataFolder;

    public PlayerDataManager(JavaPlugin plugin) {
        players = new HashMap<>();

        dataFolder = new File(
                plugin.getDataFolder(),
                "players"
        );

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(UUID uuid) {

        if (!players.containsKey(uuid)) {
            PlayerData data = loadPlayerData(uuid);
            players.put(uuid, data);
        }

        return players.get(uuid);
    }

    public void removePlayerData(UUID uuid) {

        PlayerData data = players.remove(uuid);

        if (data != null) {
            savePlayerData(data);
        }
    }

    public void saveAll() {

        for (PlayerData data : players.values()) {
            savePlayerData(data);
        }
    }

    private PlayerData loadPlayerData(UUID uuid) {

        File file = getFile(uuid);

        PlayerData data = new PlayerData(uuid);

        if (!file.exists()) {
            return data;
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        data.setXp(config.getInt("xp", 0));

        for (String cellId :
                config.getStringList("discovered-cells")) {

            data.discoverCell(cellId);
        }

        return data;
    }

    private void savePlayerData(PlayerData data) {

        File file = getFile(data.getUuid());

        YamlConfiguration config =
                new YamlConfiguration();

        config.set(
                "uuid",
                data.getUuid().toString()
        );

        config.set(
                "xp",
                data.getXp()
        );

        config.set(
        "discovered-cells",
        new java.util.ArrayList<>(data.getDiscoveredCells())
);

        try {
            config.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private File getFile(UUID uuid) {

        return new File(
                dataFolder,
                uuid + ".yml"
        );
    }
}