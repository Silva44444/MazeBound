package net.mazebound;

import org.bukkit.plugin.java.JavaPlugin;

public class Mazebound extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Mazebound v1.0 enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Mazebound v1.0 disabled!");
    }
}