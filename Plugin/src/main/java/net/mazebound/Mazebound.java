package net.mazebound;

import net.mazebound.commands.BuildCellCommand;
import net.mazebound.commands.DiscoverCommand;
import net.mazebound.commands.MazeTestCommand;
import net.mazebound.commands.ProfileCommand;
import net.mazebound.commands.WorldPlanTestCommand;
import net.mazebound.data.PlayerDataManager;
import net.mazebound.exploration.ExplorationListener;
import net.mazebound.exploration.ExplorationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Mazebound extends JavaPlugin {

    public static final int CELL_SIZE = 55;

    public static final int MAZE_ORIGIN_X = 0;
    public static final int MAZE_ORIGIN_Z = 0;

    private PlayerDataManager playerDataManager;
    private ExplorationManager explorationManager;

    @Override
    public void onEnable() {

        playerDataManager = new PlayerDataManager();
        explorationManager =
                new ExplorationManager(playerDataManager);

        getCommand("profile").setExecutor(
                new ProfileCommand(this)
        );

        getCommand("discover").setExecutor(
                new DiscoverCommand(this)
        );

        getCommand("mazetest").setExecutor(
                new MazeTestCommand()
        );

        getCommand("worldplantest").setExecutor(
                new WorldPlanTestCommand()
        );

        getCommand("buildcell").setExecutor(
                new BuildCellCommand()
        );

        getServer().getPluginManager().registerEvents(
                new ExplorationListener(this),
                this
        );

        getLogger().info("Mazebound v1.0 enabled!");
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ExplorationManager getExplorationManager() {
        return explorationManager;
    }

    @Override
    public void onDisable() {
        getLogger().info("Mazebound has stopped!");
    }
}