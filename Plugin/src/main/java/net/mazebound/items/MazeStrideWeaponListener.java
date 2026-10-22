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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MazeStrideWeaponListener implements Listener {
    private static final long COOLDOWN_MS = 15_000L;
    private static final long COMBAT_LOCK_MS = 8_000L;
    private static final double DASH_DISTANCE = 8.0;
    private static final int DASH_TICKS = 5;
    private static final double BODY_RADIUS = 0.30;
    private static final double SAMPLE_STEP = 0.2;

    private final Mazebound plugin;
    private final NamespacedKey weaponKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, BukkitTask> activeDashes = new HashMap<>();

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
                ChatColor.GRAY + "Dash up to 8 blocks in the direction you look.",
                ChatColor.GRAY + "Works in mid-air • 15-second cooldown",
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
        UUID uuid = player.getUniqueId();
        if (activeDashes.containsKey(uuid)) return;

        long now = System.currentTimeMillis();
        long combatEnd = combatUntil.getOrDefault(uuid, 0L);
        if (combatEnd > now) {
            player.sendMessage(ChatColor.RED + "Maze Stride is unstable during combat.");
            return;
        }

        long readyAt = cooldowns.getOrDefault(uuid, 0L);
        if (readyAt > now) {
            player.sendMessage(ChatColor.RED + "Phase Dash recharges in "
                    + ((readyAt - now + 999L) / 1000L) + "s.");
            return;
        }

        Location origin = player.getLocation().clone();
        Vector direction = origin.getDirection();
        if (direction.lengthSquared() < 0.001) return;
        direction.normalize();

        player.setVelocity(new Vector(0, 0, 0));
        player.getWorld().spawnParticle(Particle.PORTAL, origin.clone().add(0, 1, 0),
                24, 0.35, 0.5, 0.35, 0.05);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.65f, 1.4f);

        cooldowns.put(uuid, now + COOLDOWN_MS);
        startDash(player, direction);
    }

    private void startDash(Player player, Vector direction) {
        UUID uuid = player.getUniqueId();
        final double stepDistance = DASH_DISTANCE / DASH_TICKS;
        final int[] tick = {0};
        final Location[] lastSafe = {player.getLocation().clone()};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isValid() || player.isDead()) {
                    cancel();
                    activeDashes.remove(uuid);
                    return;
                }

                if (tick[0] >= DASH_TICKS) {
                    finish();
                    return;
                }

                Location current = player.getLocation().clone();
                double travelled = 0;
                while (travelled < stepDistance) {
                    double increment = Math.min(SAMPLE_STEP, stepDistance - travelled);
                    Location candidate = current.clone().add(direction.clone().multiply(increment));
                    candidate.setYaw(current.getYaw());
                    candidate.setPitch(current.getPitch());
                    if (!hasClearBodySpace(candidate)) {
                        finish();
                        return;
                    }
                    current = candidate;
                    travelled += increment;
                }

                player.setVelocity(new Vector(0, 0, 0));
                if (!player.teleport(current)) {
                    finish();
                    return;
                }
                lastSafe[0] = current;
                tick[0]++;

                player.getWorld().spawnParticle(Particle.PORTAL, current.clone().add(0, 1, 0),
                        8, 0.22, 0.35, 0.22, 0.02);

                if (tick[0] >= DASH_TICKS) finish();
            }

            private void finish() {
                player.setVelocity(new Vector(0, 0, 0));
                player.getWorld().spawnParticle(Particle.PORTAL, lastSafe[0].clone().add(0, 1, 0),
                        24, 0.35, 0.5, 0.35, 0.04);
                player.playSound(lastSafe[0], Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Phase Dash!");
                activeDashes.remove(uuid);
                cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);

        activeDashes.put(uuid, task);
    }

    private boolean hasClearBodySpace(Location feetLocation) {
        // Check the player's approximate full body, not just the center block.
        double[] xOffsets = {-BODY_RADIUS, 0, BODY_RADIUS};
        double[] zOffsets = {-BODY_RADIUS, 0, BODY_RADIUS};
        double[] yOffsets = {0.1, 0.9, 1.7};

        for (double x : xOffsets) {
            for (double z : zOffsets) {
                for (double y : yOffsets) {
                    Location sample = feetLocation.clone().add(x, y, z);
                    if (sample.getBlockY() < sample.getWorld().getMinHeight()
                            || sample.getBlockY() >= sample.getWorld().getMaxHeight()) {
                        return false;
                    }
                    Block block = sample.getBlock();
                    if (!block.isPassable() || block.getType() == Material.POWDER_SNOW) {
                        return false;
                    }
                }
            }
        }
        return true;
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
        BukkitTask task = activeDashes.remove(uuid);
        if (task != null) task.cancel();
    }
}
