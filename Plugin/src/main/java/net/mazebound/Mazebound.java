package net.mazebound;

import net.mazebound.commands.BuildCellCommand;
import net.mazebound.commands.DiscoverCommand;
import net.mazebound.commands.MazeTestCommand;
import net.mazebound.commands.ProfileCommand;
import net.mazebound.commands.SpawnMazeCommand;
import net.mazebound.commands.WorldPlanTestCommand;
import net.mazebound.data.LuckPermsManager;
import net.mazebound.data.PlayerDataManager;
import net.mazebound.data.PlayerRankListener;
import net.mazebound.exploration.ExplorationListener;
import net.mazebound.exploration.ExplorationManager;
import net.mazebound.items.CustomItemListener;
import net.mazebound.items.CustomItemManager;
import net.mazebound.items.HerbGardenCommand;
import net.mazebound.items.HerbGardenManager;
import net.mazebound.world.MazeLayout;
import net.mazebound.world.MazePopulator;
import net.mazebound.world.MazeRenderer;
import net.mazebound.world.MazeSettings;
import net.mazebound.world.MazeWorldListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Mazebound extends JavaPlugin {

    public static final int CELL_SIZE = 55;

    public static final int MAZE_ORIGIN_X = 0;
    public static final int MAZE_ORIGIN_Z = 0;

    private PlayerDataManager playerDataManager;
    private ExplorationManager explorationManager;
    private LuckPermsManager luckPermsManager;
    private CustomItemManager customItemManager;
    private HerbGardenManager herbGardenManager;
    private MazeLayout mazeLayout;

    @Override
    public void onEnable() {

        playerDataManager = new PlayerDataManager(this);

        luckPermsManager = new LuckPermsManager(playerDataManager);

        explorationManager = new ExplorationManager(playerDataManager);

        // ---------------------------------------------------------
        // Maze: layout, terrain overlay, world hook
        // ---------------------------------------------------------
        MazeSettings mazeSettings = MazeSettings.load(this);

        mazeLayout = MazeLayout.generate(
                mazeSettings.seed(),
                mazeSettings.innerRadiusCells(),
                mazeSettings.outerRadiusCells(),
                mazeSettings.entrancesPerSide()
        );

        getLogger().info(
                "Maze layout ready: " + mazeLayout.cells().size()
                        + " cells, ring from " + mazeLayout.innerRadiusCells() * Mazebound.CELL_SIZE
                        + " to " + (mazeLayout.outerRadiusCells() + 1) * Mazebound.CELL_SIZE
                        + " blocks out, seed " + mazeSettings.seed()
                        + ", terrain mode " + mazeSettings.terrainMode()
                        + ", wall thickness " + mazeSettings.wallThickness()
                        + ", biome themes " + mazeSettings.biomeThemes()
                        + ", floor Y " + mazeSettings.floorY()
                        + ", world '" + mazeSettings.worldName() + "'"
        );

        explorationManager.setLayout(mazeLayout);

        MazePopulator mazePopulator = new MazePopulator(
                new MazeRenderer(
                        mazeLayout,
                        mazeSettings.floorY(),
                        mazeSettings.maxFillDepth(),
                        mazeSettings.terrainMode(),
                        mazeSettings.wallThickness(),
                        mazeSettings.biomeThemes()
                ),
                getLogger(),
                mazeSettings.debug()
        );

        MazeWorldListener mazeWorldListener = new MazeWorldListener(
                mazeSettings.worldName(),
                mazePopulator
        );

        getServer().getPluginManager().registerEvents(
                mazeWorldListener,
                this
        );

        mazeWorldListener.attachIfLoaded();

        // ---------------------------------------------------------
        // Custom items and herb garden
        // ---------------------------------------------------------
        customItemManager = new CustomItemManager(this);
        customItemManager.registerRecipes();

        getServer().getPluginManager().registerEvents(
                new CustomItemListener(this, customItemManager),
                this
        );

        herbGardenManager = new HerbGardenManager(this, customItemManager);

        getServer().getPluginManager().registerEvents(
                herbGardenManager,
                this
        );

        getCommand("herbgarden").setExecutor(
                new HerbGardenCommand(this, herbGardenManager)
        );

        // ---------------------------------------------------------
        // Commands
        // ---------------------------------------------------------
        getCommand("profile").setExecutor(new ProfileCommand(this));
        getCommand("spawnmaze").setExecutor(new SpawnMazeCommand());
        getCommand("discover").setExecutor(new DiscoverCommand(this));
        getCommand("mazetest").setExecutor(new MazeTestCommand());
        getCommand("worldplantest").setExecutor(new WorldPlanTestCommand());
        getCommand("buildcell").setExecutor(new BuildCellCommand());

        // ---------------------------------------------------------
        // Listeners
        // ---------------------------------------------------------
        getServer().getPluginManager().registerEvents(
                new ExplorationListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new PlayerRankListener(this),
                this
        );

        getLogger().info("Mazebound v1.0 enabled!");
    }

    @Override
    public void onDisable() {

        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        if (herbGardenManager != null) {
            herbGardenManager.save();
        }

        getLogger().info("Mazebound has stopped!");
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public LuckPermsManager getLuckPermsManager() {
        return luckPermsManager;
    }

    public ExplorationManager getExplorationManager() {
        return explorationManager;
    }

    public MazeLayout getMazeLayout() {
        return mazeLayout;
    }
}