package net.mazebound.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;

/**
 * Attaches the maze populator to the configured world, whether that world
 * is already loaded when the plugin enables or only initializes afterwards
 * (which is why plugin.yml needs "load: STARTUP").
 */
public class MazeWorldListener implements Listener {

    private final String worldName;
    private final MazePopulator populator;

    public MazeWorldListener(String worldName, MazePopulator populator) {
        this.worldName = worldName;
        this.populator = populator;
    }

    /** Call once from onEnable, after registering this listener. */
    public void attachIfLoaded() {

        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            attach(world);
        }
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {

        if (event.getWorld().getName().equals(worldName)) {
            attach(event.getWorld());
        }
    }

    private void attach(World world) {

        for (BlockPopulator existing : world.getPopulators()) {
            if (existing == populator) {
                return;
            }
        }

        world.getPopulators().add(populator);
    }
}