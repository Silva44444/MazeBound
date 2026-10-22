package net.mazebound.world;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

/**
 * Maze settings from config.yml. Defaults are written on first start,
 * including a random seed that is then kept forever so the maze is the
 * same after every restart.
 */
public record MazeSettings(
        String worldName,
        long seed,
        int radiusCells,
        int floorY,
        int maxFillDepth,
        boolean debug
) {

    public static MazeSettings load(JavaPlugin plugin) {

        FileConfiguration config = plugin.getConfig();

        config.addDefault("maze.world", "world");
        config.addDefault("maze.radius-cells", 36);
        config.addDefault("maze.floor-y", 63);
        config.addDefault("maze.max-fill-depth", 64);
        config.addDefault("maze.debug", false);

        if (!config.isSet("maze.seed")) {
            config.set("maze.seed", new Random().nextLong());
        }

        config.options().setHeader(List.of(
                "Mazebound settings.",
                "Do NOT change maze.seed, maze.radius-cells or maze.floor-y",
                "after maze chunks have been generated, or new chunks will",
                "not match the old ones."
        ));
        config.options().copyDefaults(true);
        plugin.saveConfig();

        return new MazeSettings(
                config.getString("maze.world", "world"),
                config.getLong("maze.seed"),
                config.getInt("maze.radius-cells", 36),
                config.getInt("maze.floor-y", 63),
                config.getInt("maze.max-fill-depth", 64),
                config.getBoolean("maze.debug", false)
        );
    }
}