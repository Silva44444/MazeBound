package net.mazebound;

import net.mazebound.commands.BuildCellCommand;
import net.mazebound.commands.DiscoverCommand;
import net.mazebound.commands.MazeTestCommand;
import net.mazebound.commands.ProfileCommand;
import net.mazebound.commands.SpawnMazeCommand;
import net.mazebound.commands.WorldPlanTestCommand;
import net.mazebound.data.PlayerDataManager;
import net.mazebound.data.LuckPermsManager;
import net.mazebound.data.PlayerRankListener;
import net.mazebound.exploration.ExplorationListener;
import net.mazebound.exploration.ExplorationManager;
import net.mazebound.items.CustomItemManager;
import net.mazebound.items.CustomItemListener;
import net.mazebound.items.HerbGardenManager;
import net.mazebound.items.HerbGardenCommand;
import net.mazebound.items.WaystoneManager;
import net.mazebound.items.WaystoneRuneListener;
import net.mazebound.items.WaystoneCommand;
import net.mazebound.items.MazeStrideWeaponListener;
import net.mazebound.items.MazeWeaponCommand;
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
    private WaystoneManager waystoneManager;

    @Override
    public void onEnable() {
        playerDataManager = new PlayerDataManager(this);
        luckPermsManager = new LuckPermsManager(playerDataManager);
        explorationManager = new ExplorationManager(playerDataManager);

        customItemManager = new CustomItemManager(this);
        customItemManager.registerRecipes();
        getServer().getPluginManager().registerEvents(
                new CustomItemListener(this, customItemManager), this);

        herbGardenManager = new HerbGardenManager(this, customItemManager);
        getServer().getPluginManager().registerEvents(herbGardenManager, this);
        getCommand("herbgarden").setExecutor(new HerbGardenCommand(this, herbGardenManager));

        waystoneManager = new WaystoneManager(this);
        WaystoneRuneListener waystoneRuneListener = new WaystoneRuneListener(this, waystoneManager);
        getServer().getPluginManager().registerEvents(waystoneRuneListener, this);
        getCommand("waystone").setExecutor(
                new WaystoneCommand(this, waystoneManager, waystoneRuneListener));

        MazeStrideWeaponListener mazeStrideListener = new MazeStrideWeaponListener(this);
        getServer().getPluginManager().registerEvents(mazeStrideListener, this);
        getCommand("mazewpn").setExecutor(new MazeWeaponCommand(this, mazeStrideListener));

        getCommand("profile").setExecutor(new ProfileCommand(this));
        getCommand("spawnmaze").setExecutor(new SpawnMazeCommand());
        getCommand("discover").setExecutor(new DiscoverCommand(this));
        getCommand("mazetest").setExecutor(new MazeTestCommand());
        getCommand("worldplantest").setExecutor(new WorldPlanTestCommand());
        getCommand("buildcell").setExecutor(new BuildCellCommand());

        getServer().getPluginManager().registerEvents(new ExplorationListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRankListener(this), this);

        getLogger().info("Mazebound v1.0 enabled!");
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

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (herbGardenManager != null) herbGardenManager.save();
        if (waystoneManager != null) waystoneManager.save();
        getLogger().info("Mazebound has stopped!");
    }
}