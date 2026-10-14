
package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HerbGardenManager implements Listener {

    private static final long STAGE_TIME_MS = 5 * 60 * 1000L;
    private static final int MAX_HARVEST = 2;

    private final Mazebound plugin;
    private final CustomItemManager items;
    private final File file;

    private final Map<String, GardenPlant> plants = new HashMap<>();

    private enum Stage {
        GROWING,
        MATURING,
        HARVESTABLE
    }

    private static final class GardenPlant {
        private final String world;
        private final int x, y, z;
        private Stage stage;
        private long nextGrowthAt;

        private GardenPlant(Location location, Stage stage, long nextGrowthAt) {
            this.world = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.stage = stage;
            this.nextGrowthAt = nextGrowthAt;
        }

        private Location location() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z);
        }
    }

    public HerbGardenManager(Mazebound plugin, CustomItemManager items) {
        this.plugin = plugin;
        this.items = items;
        this.file = new File(plugin.getDataFolder(), "herb-gardens.yml");

        load();

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public boolean addPlant(Location location) {
        if (location.getWorld() == null) {
            return false;
        }

        String id = key(location);

        if (plants.containsKey(id)) {
            return false;
        }

        GardenPlant plant = new GardenPlant(
                location,
                Stage.GROWING,
                System.currentTimeMillis() + STAGE_TIME_MS
        );

        plants.put(id, plant);
        applyAppearance(plant);
        save();
        return true;
    }

    public boolean removePlant(Location location) {
        GardenPlant plant = plants.remove(key(location));

        if (plant == null) {
            return false;
        }

        // Restore an ordinary block rather than leaving the garden's
        // visual block behind. The configured garden spot becomes dirt.
        Block block = location.getBlock();
        block.setType(Material.DIRT, false);

        save();
        return true;
    }

    public int getPlantCount() {
        return plants.size();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (GardenPlant plant : plants.values()) {
            if (plant.stage == Stage.HARVESTABLE || now < plant.nextGrowthAt) {
                continue;
            }

            plant.stage = plant.stage == Stage.GROWING
                    ? Stage.MATURING
                    : Stage.HARVESTABLE;

            plant.nextGrowthAt = now + STAGE_TIME_MS;
            applyAppearance(plant);
            changed = true;
        }

        if (changed) {
            save();
        }
    }

    private void applyAppearance(GardenPlant plant) {
        Location location = plant.location();

        if (location == null || !location.isWorldLoaded()) {
            return;
        }

        Material appearance = switch (plant.stage) {
            case GROWING -> Material.FERN;
            case MATURING -> Material.SHORT_GRASS;
            case HARVESTABLE -> Material.AZALEA_LEAVES;
        };

        location.getBlock().setType(appearance, false);
    }

    @EventHandler
    public void onPlantInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        GardenPlant plant = plants.get(key(block.getLocation()));

        if (plant == null) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        if (plant.stage != Stage.HARVESTABLE) {
            player.sendMessage(ChatColor.YELLOW + "This herb is still growing.");
            return;
        }

        int amount = 1 + (int) (Math.random() * MAX_HARVEST);
        player.getInventory().addItem(items.createHealingHerb(amount))
                .values()
                .forEach(item -> player.getWorld().dropItemNaturally(
                        player.getLocation(), item
                ));

        plant.stage = Stage.GROWING;
        plant.nextGrowthAt = System.currentTimeMillis() + STAGE_TIME_MS;
        applyAppearance(plant);
        save();

        player.sendMessage(
                ChatColor.GREEN + "You harvested " + amount + " Healing Herb(s)."
        );
    }

    @EventHandler
    public void onPlantBreak(BlockBreakEvent event) {
        if (plants.containsKey(key(event.getBlock().getLocation()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    ChatColor.RED + "Use right-click to interact with this garden."
            );
        }
    }

    private String key(Location location) {
        return location.getWorld().getName() + ":"
                + location.getBlockX() + ":"
                + location.getBlockY() + ":"
                + location.getBlockZ();
    }

    private String key(Block block) {
        return key(block.getLocation());
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("plants");

        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            String path = "plants." + id;
            World world = Bukkit.getWorld(data.getString(path + ".world", ""));

            if (world == null) {
                continue;
            }

            Location location = new Location(
                    world,
                    data.getInt(path + ".x"),
                    data.getInt(path + ".y"),
                    data.getInt(path + ".z")
            );

            Stage stage;
            try {
                stage = Stage.valueOf(
                        data.getString(path + ".stage", "GROWING")
                );
            } catch (IllegalArgumentException exception) {
                stage = Stage.GROWING;
            }

            GardenPlant plant = new GardenPlant(
                    location,
                    stage,
                    data.getLong(path + ".nextGrowthAt",
                            System.currentTimeMillis() + STAGE_TIME_MS)
            );

            plants.put(key(location), plant);
            applyAppearance(plant);
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();

        for (Map.Entry<String, GardenPlant> entry : plants.entrySet()) {
            GardenPlant plant = entry.getValue();
            String path = "plants." + entry.getKey();

            data.set(path + ".world", plant.world);
            data.set(path + ".x", plant.x);
            data.set(path + ".y", plant.y);
            data.set(path + ".z", plant.z);
            data.set(path + ".stage", plant.stage.name());
            data.set(path + ".nextGrowthAt", plant.nextGrowthAt);
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save herb gardens: " + exception.getMessage()
            );
        }
    }
}
