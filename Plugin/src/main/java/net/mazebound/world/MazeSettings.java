package net.mazebound.world;

import net.mazebound.Mazebound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

/**
 * Maze settings from config.yml. Defaults are written on first start,
 * including a random seed that is then kept forever so the maze is the
 * same after every restart.
 *
 * Radii are configured in blocks (distance from spawn along an axis) and
 * rounded up to whole cells.
 */
public record MazeSettings(
        String worldName,
        long seed,
        int innerRadiusCells,
        int outerRadiusCells,
        int entrancesPerSide,
        MazeRenderer.TerrainMode terrainMode,
        int wallThickness,
        boolean biomeThemes,
        int floorY,
        int maxFillDepth,
        boolean debug
) {

    public static MazeSettings load(JavaPlugin plugin) {

        FileConfiguration config = plugin.getConfig();

        config.addDefault("maze.world", "world");
        config.addDefault("maze.inner-radius-blocks", 2000);
        config.addDefault("maze.outer-radius-blocks", 3000);
        config.addDefault("maze.entrances-per-side", 3);
        config.addDefault("maze.terrain-mode", "keep");
        config.addDefault("maze.wall-thickness", 3);
        config.addDefault("maze.biome-themes", true);
        config.addDefault("maze.floor-y", 63);
        config.addDefault("maze.max-fill-depth", 64);
        config.addDefault("maze.debug", false);

        if (!config.isSet("maze.seed")) {
            config.set("maze.seed", new Random().nextLong());
        }

        config.options().setHeader(List.of(
                "Mazebound settings.",
                "The maze is a square ring around spawn: normal terrain inside",
                "inner-radius-blocks, maze from there out to outer-radius-blocks.",
                "terrain-mode: 'keep' leaves natural terrain in place (hills can",
                "block doorways); 'flat' replaces it with a flat, walkable floor.",
                "wall-thickness: blocks each cell contributes per side (1-8), so a",
                "wall between two cells is twice this thick.",
                "biome-themes: wall blocks follow the biome (sandstone in desert...).",
                "Do NOT change seed, the radii, entrances-per-side, terrain-mode,",
                "wall-thickness, biome-themes or floor-y",
                "after maze chunks have been generated, or new chunks will",
                "not match the old ones."
        ));
        config.options().copyDefaults(true);
        plugin.saveConfig();

        int innerBlocks = config.getInt("maze.inner-radius-blocks", 2000);
        int outerBlocks = config.getInt("maze.outer-radius-blocks", 3000);

        int wallThickness = config.getInt("maze.wall-thickness", 3);

        if (wallThickness < MazeRenderer.MIN_WALL_THICKNESS
                || wallThickness > MazeRenderer.MAX_WALL_THICKNESS) {
            throw new IllegalArgumentException(
                    "maze.wall-thickness must be between "
                            + MazeRenderer.MIN_WALL_THICKNESS + " and "
                            + MazeRenderer.MAX_WALL_THICKNESS
                            + ", but was " + wallThickness
            );
        }

        int innerCells = Math.max(1, toCells(innerBlocks));
        int outerCells = toCells(outerBlocks);

        if (outerCells <= innerCells) {
            throw new IllegalArgumentException(
                    "maze.outer-radius-blocks (" + outerBlocks
                            + ") must be clearly larger than "
                            + "maze.inner-radius-blocks (" + innerBlocks + ")"
            );
        }

        return new MazeSettings(
                config.getString("maze.world", "world"),
                config.getLong("maze.seed"),
                innerCells,
                outerCells,
                config.getInt("maze.entrances-per-side", 3),
                MazeRenderer.TerrainMode.parse(
                        config.getString("maze.terrain-mode", "keep")
                ),
                wallThickness,
                config.getBoolean("maze.biome-themes", true),
                config.getInt("maze.floor-y", 63),
                config.getInt("maze.max-fill-depth", 64),
                config.getBoolean("maze.debug", false)
        );
    }

    private static int toCells(int blocks) {
        return (int) Math.ceil(blocks / (double) Mazebound.CELL_SIZE);
    }
}