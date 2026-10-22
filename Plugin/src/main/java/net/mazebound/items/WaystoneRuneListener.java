package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WaystoneRuneListener implements Listener {
    private static final long COOLDOWN_MS = 10 * 60 * 1000L;
    private static final long COMBAT_LOCK_MS = 10_000L;
    private static final int CHANNEL_TICKS = 60;
    private static final String MENU_TITLE = ChatColor.DARK_AQUA + "Waystone Network";

    private final Mazebound plugin;
    private final WaystoneManager waystones;
    private final NamespacedKey runeKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, Location> channelOrigins = new HashMap<>();
    private final Map<UUID, String> menuDestinations = new HashMap<>();
    private final Map<UUID, Inventory> openMenus = new HashMap<>();

    public WaystoneRuneListener(Mazebound plugin, WaystoneManager waystones) {
        this.plugin = plugin;
        this.waystones = waystones;
        this.runeKey = new NamespacedKey(plugin, "waystone_rune");
    }

    public ItemStack createRune() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA.toString() + ChatColor.BOLD + "Waystone Rune");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "An ancient artifact attuned to safe cells.");
        lore.add("");
        lore.add(ChatColor.AQUA + "Right-click to open discovered waystones.");
        lore.add(ChatColor.GRAY + "10-minute cooldown");
        lore.add(ChatColor.DARK_GRAY + "The maze forgets its paths. The rune does not.");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(runeKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isRune(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(runeKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        WaystoneManager.Waystone clickedStone = event.getClickedBlock() == null
                ? null : waystones.getWaystoneAt(event.getClickedBlock().getLocation());

        if (clickedStone != null) {
            event.setCancelled(true);
            boolean firstVisit = waystones.discover(player.getUniqueId(), clickedStone.id());
            if (firstVisit) {
                player.sendMessage(ChatColor.AQUA + "Waystone discovered: " + ChatColor.WHITE + clickedStone.name());
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
            } else {
                player.sendMessage(ChatColor.GRAY + "Waystone already discovered: " + clickedStone.name());
            }
            if (isRune(player.getInventory().getItemInMainHand())) openMenu(player);
            return;
        }

        if (!isRune(player.getInventory().getItemInMainHand())) return;
        event.setCancelled(true);
        openMenu(player);
    }

    private void openMenu(Player player) {
        if (isInCombat(player)) {
            player.sendMessage(ChatColor.RED + "The rune is unstable while you are in combat.");
            return;
        }
        List<WaystoneManager.Waystone> destinations = new ArrayList<>(waystones.getDiscoveredWaystones(player.getUniqueId()));
        if (destinations.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You haven't discovered any waystones yet. Find and activate one in a safe cell.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, MENU_TITLE);
        menuDestinations.remove(player.getUniqueId());
        for (int i = 0; i < destinations.size() && i < inventory.getSize(); i++) {
            WaystoneManager.Waystone stone = destinations.get(i);
            ItemStack icon = new ItemStack(Material.LODESTONE);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + stone.name());
            meta.setLore(List.of(ChatColor.GRAY + "Click to teleport", ChatColor.DARK_GRAY + "ID: " + stone.id()));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "waystone_destination"),
                    PersistentDataType.STRING, stone.id());
            icon.setItemMeta(meta);
            inventory.setItem(i, icon);
        }
        openMenus.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory expected = openMenus.get(player.getUniqueId());
        if (expected == null || event.getView().getTopInventory() != expected) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != expected || event.getCurrentItem() == null) return;
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "waystone_destination"),
                PersistentDataType.STRING);
        if (id == null || !waystones.isDiscovered(player.getUniqueId(), id)) return;
        WaystoneManager.Waystone stone = waystones.getWaystone(id);
        if (stone == null || stone.blockLocation().getWorld() == null) {
            player.sendMessage(ChatColor.RED + "That waystone is unavailable.");
            player.closeInventory();
            return;
        }
        if (isInCombat(player)) {
            player.sendMessage(ChatColor.RED + "You cannot use the rune while in combat.");
            player.closeInventory();
            return;
        }
        long availableAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        if (now < availableAt) {
            player.sendMessage(ChatColor.RED + "The rune needs " + ((availableAt - now + 999L) / 1000L) + "s to recharge.");
            player.closeInventory();
            return;
        }
        if (player.getWorld().equals(stone.blockLocation().getWorld())
                && player.getLocation().distanceSquared(stone.arrivalLocation()) < 9.0) {
            player.sendMessage(ChatColor.YELLOW + "You're already near that waystone.");
            player.closeInventory();
            return;
        }
        player.closeInventory();
        beginChannel(player, stone);
    }

    private void beginChannel(Player player, WaystoneManager.Waystone stone) {
        UUID uuid = player.getUniqueId();
        Location origin = player.getLocation().clone();
        channelOrigins.put(uuid, origin);
        player.sendMessage(ChatColor.AQUA + "Attuning to " + stone.name() + "... don't move or take damage.");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.4f);

        new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!player.isOnline() || !channelOrigins.containsKey(uuid)) {
                    cancel();
                    return;
                }
                if (isInCombat(player) || movedFromOrigin(player, origin)) {
                    channelOrigins.remove(uuid);
                    player.sendMessage(ChatColor.RED + "Teleportation interrupted.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
                    cancel();
                    return;
                }
                if (elapsed >= CHANNEL_TICKS) {
                    channelOrigins.remove(uuid);
                    Location arrival = stone.arrivalLocation();
                    if (!isSafe(arrival)) {
                        player.sendMessage(ChatColor.RED + "That waystone's arrival point is blocked. Tell an administrator.");
                        cancel();
                        return;
                    }
                    player.teleport(arrival);
                    cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_MS);
                    player.sendMessage(ChatColor.AQUA + "Teleported to " + stone.name() + ".");
                    player.playSound(arrival, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    cancel();
                    return;
                }
                elapsed += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private boolean movedFromOrigin(Player player, Location origin) {
        Location current = player.getLocation();
        return !current.getWorld().equals(origin.getWorld())
                || current.distanceSquared(origin) > 0.04
                || Math.abs(current.getYaw() - origin.getYaw()) > 12.0f
                || Math.abs(current.getPitch() - origin.getPitch()) > 12.0f;
    }

    private boolean isSafe(Location location) {
        if (location.getWorld() == null) return false;
        return location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable()
                && !location.clone().subtract(0, 1, 0).getBlock().isLiquid();
    }

    private boolean isInCombat(Player player) {
        return combatUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    @EventHandler
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player p ? p : null;
        Player attacker = event.getDamager() instanceof Player p ? p : null;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player p) attacker = p;
        long until = System.currentTimeMillis() + COMBAT_LOCK_MS;
        if (victim != null) combatUntil.put(victim.getUniqueId(), until);
        if (attacker != null) combatUntil.put(attacker.getUniqueId(), until);
        if (victim != null) channelOrigins.remove(victim.getUniqueId());
        if (attacker != null) channelOrigins.remove(attacker.getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!channelOrigins.containsKey(event.getPlayer().getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ())) {
            UUID uuid = event.getPlayer().getUniqueId();
            channelOrigins.remove(uuid);
            event.getPlayer().sendMessage(ChatColor.RED + "Teleportation interrupted because you moved.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        channelOrigins.remove(uuid);
        openMenus.remove(uuid);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inventory = openMenus.get(player.getUniqueId());
        if (inventory != null && event.getInventory() == inventory) openMenus.remove(player.getUniqueId());
    }
}