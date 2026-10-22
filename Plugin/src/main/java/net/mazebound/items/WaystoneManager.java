package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WaystoneManager {
    private final Mazebound plugin;
    private final File file;
    private final Map<String, Waystone> waystones = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> discoveries = new LinkedHashMap<>();

    public WaystoneManager(Mazebound plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "waystones.yml");
        load();
    }

    public void setWaystone(String id, String name, Location blockLocation) {
        String key = normalize(id);
        waystones.put(key, new Waystone(key, name, blockLocation.getBlock().getLocation()));
        save();
    }

    public boolean removeWaystone(String id) {
        Waystone removed = waystones.remove(normalize(id));
        if (removed == null) return false;
        for (Set<String> found : discoveries.values()) found.remove(normalize(id));
        save();
        return true;
    }

    public Waystone getWaystone(String id) {
        return waystones.get(normalize(id));
    }

    public Waystone getWaystoneAt(Location location) {
        if (location == null) return null;
        for (Waystone waystone : waystones.values()) {
            Location stored = waystone.blockLocation();
            if (stored.getWorld() != null && stored.getWorld().equals(location.getWorld())
                    && stored.getBlockX() == location.getBlockX()
                    && stored.getBlockY() == location.getBlockY()
                    && stored.getBlockZ() == location.getBlockZ()) {
                return waystone;
            }
        }
        return null;
    }

    public Collection<Waystone> getWaystones() {
        return Collections.unmodifiableCollection(waystones.values());
    }

    public Collection<Waystone> getDiscoveredWaystones(UUID playerId) {
        Set<String> found = discoveries.getOrDefault(playerId, Collections.emptySet());
        Collection<Waystone> result = new ArrayList<>();
        for (String id : found) {
            Waystone waystone = waystones.get(id);
            if (waystone != null && waystone.blockLocation().getWorld() != null) result.add(waystone);
        }
        return result;
    }

    public boolean discover(UUID playerId, String id) {
        String key = normalize(id);
        if (!waystones.containsKey(key)) return false;
        boolean added = discoveries.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).add(key);
        if (added) save();
        return added;
    }

    public boolean isDiscovered(UUID playerId, String id) {
        return discoveries.getOrDefault(playerId, Collections.emptySet()).contains(normalize(id));
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Waystone stone : waystones.values()) {
            String path = "waystones." + stone.id();
            Location loc = stone.blockLocation();
            config.set(path + ".name", stone.name());
            config.set(path + ".world", loc.getWorld() == null ? "" : loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
        }
        for (Map.Entry<UUID, Set<String>> entry : discoveries.entrySet()) {
            config.set("discoveries." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data directory for waystones.");
                return;
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save waystones.yml: " + exception.getMessage());
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection stones = config.getConfigurationSection("waystones");
        if (stones != null) {
            for (String rawId : stones.getKeys(false)) {
                String path = "waystones." + rawId;
                String worldName = config.getString(path + ".world", "");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Skipping waystone '" + rawId + "' because world '" + worldName + "' is unavailable.");
                    continue;
                }
                Location location = new Location(world,
                        config.getInt(path + ".x"),
                        config.getInt(path + ".y"),
                        config.getInt(path + ".z"));
                waystones.put(normalize(rawId), new Waystone(normalize(rawId),
                        config.getString(path + ".name", rawId), location));
            }
        }
        ConfigurationSection players = config.getConfigurationSection("discoveries");
        if (players != null) {
            for (String uuidText : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    Set<String> found = new LinkedHashSet<>();
                    for (String id : config.getStringList("discoveries." + uuidText)) {
                        String key = normalize(id);
                        if (waystones.containsKey(key)) found.add(key);
                    }
                    discoveries.put(uuid, found);
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Ignoring invalid player UUID in waystones.yml: " + uuidText);
                }
            }
        }
    }

    public static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    public record Waystone(String id, String name, Location blockLocation) {
        public Location arrivalLocation() {
            return blockLocation.clone().add(0.5, 1.0, 0.5);
        }
    }
}