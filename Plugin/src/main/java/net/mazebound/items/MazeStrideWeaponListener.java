package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MazeStrideWeaponListener implements Listener {
    private static final String ITEM_ID = "maze_stride";
    private static final long COOLDOWN_MS = 20_000L;
    private static final long COMBAT_LOCK_MS = 8_000L;
    private static final double DASH_DISTANCE = 5.0;
    private static final double SAMPLE_STEP = 0.2;

    private final Mazebound plugin;
    private final NamespacedKey weaponKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();

    public MazeStrideWeaponListener(Mazebound plugin) {
        this.plugin = plugin;
        this.weaponKey = new NamespacedKey(plugin, "maze_stride_weapon");
    }

    public ItemStack createWeapon() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Could not create Maze Stride weapon metadata.");
        }

        meta.setDisplayName(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Maze Stride");
        meta.setLore(List.of(
                ChatColor.DARK_PURPLE + "EPIC • EXPLORATION RELIC",
                "",
                ChatColor.GRAY + "A blade that remembers the maze's hidden paths.",
                ChatColor.LIGHT_PURPLE + "Right-click: Phase Dash",
                ChatColor.GRAY + "Dash up to 5 blocks through open passages.",
                ChatColor.GRAY + "20-second cooldown",
                "",
                ChatColor.DARK_GRAY + "Stone cannot be crossed. Find another path."
        ));
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMazeStride(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(weaponKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!isMazeStride(player.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);
        long now = System.currentTimeMillis();
        long combatEnd = combatUntil.getOrDefault(player.getUniqueId(), 0L);
        if (combatEnd > now) {
            player.sendMessage(ChatColor.RED + "Maze Stride is unstable during combat.");
            return;
        }

        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            player.sendMessage(ChatColor.RED + "Phase Dash recharges in "
                    + ((readyAt - now + 999L) / 1000L) + "s.");
            return;
        }

        Location destination = findDashDestination(player);
        if (destination == null) {
            player.sendMessage(ChatColor.YELLOW + "No clear passage ahead. Find an open route.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            return;
        }

        Location origin = player.getLocation().clone();
        player.getWorld().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0), 24, 0.35, 0.5, 0.35, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.65f, 1.4f);
        boolean moved = player.teleport(destination);
        if (!moved) {
            player.sendMessage(ChatColor.RED + "The phase dash failed. Try again.");
            return;
        }

        cooldowns.put(player.getUniqueId(), now + COOLDOWN_MS);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                32, 0.4, 0.6, 0.4, 0.08);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Phase Dash!");
    }

    private Location findDashDestination(Player player) {
        Location origin = player.getLocation().clone();
        Vector direction = origin.getDirection().setY(0);
        if (direction.lengthSquared() < 0.001) return null;
        direction.normalize();

        Location best = null;
        for (double distance = SAMPLE_STEP; distance <= DASH_DISTANCE; distance += SAMPLE_STEP) {
            Location candidate = origin.clone().add(direction.clone().multiply(distance));
            candidate.setYaw(origin.getYaw());
            candidate.setPitch(origin.getPitch());

            Block feet = candidate.getBlock();
            Block head = candidate.clone().add(0, 1, 0).getBlock();
            if (!feet.isPassable() || !head.isPassable()) break;

            Block floor = candidate.clone().subtract(0, 0.1, 0).getBlock();
            if (floor.isPassable() || floor.isLiquid()) continue;

            best = candidate;
        }

        if (best == null || best.distanceSquared(origin) < 1.0) return null;
        return best;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        Player attacker = asPlayer(event.getDamager());
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        Player victim = event.getEntity() instanceof Player player ? player : null;
        long until = System.currentTimeMillis() + COMBAT_LOCK_MS;

        if (attacker != null) combatUntil.put(attacker.getUniqueId(), until);
        if (victim != null) combatUntil.put(victim.getUniqueId(), until);
    }

    private Player asPlayer(Entity entity) {
        return entity instanceof Player player ? player : null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cooldowns.remove(uuid);
        combatUntil.remove(uuid);
    }
}
