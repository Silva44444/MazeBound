package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomItemListener implements Listener {

    private static final long COOLDOWN_MS = 10_000L;
    private static final double HEAL_AMOUNT = 4.0;
    private static final int REGENERATION_TICKS = 100;

    private final Mazebound plugin;
    private final CustomItemManager itemManager;

    private final Map<UUID, Map<String, Long>> cooldowns =
            new HashMap<>();

    public CustomItemListener(
            Mazebound plugin,
            CustomItemManager itemManager
    ) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        String itemId = itemManager.getCustomItemId(held);

        if (itemId == null) {
            return;
        }

        if (!CustomItemManager.HERBAL_REMEDY.equals(itemId)) {
            return;
        }

        event.setCancelled(true);

        long now = System.currentTimeMillis();

        Map<String, Long> playerCooldowns =
                cooldowns.computeIfAbsent(
                        player.getUniqueId(),
                        ignored -> new HashMap<>()
                );

        long availableAt = playerCooldowns.getOrDefault(
                itemId, 0L
        );

        if (now < availableAt) {
            long remainingSeconds =
                    (availableAt - now + 999L) / 1000L;

            player.sendMessage(
                    ChatColor.RED
                            + "You must wait "
                            + remainingSeconds
                            + "s before using another Herbal Remedy."
            );
            return;
        }

        consumeMainHand(player);

        double newHealth = Math.min(
                player.getMaxHealth(),
                player.getHealth() + HEAL_AMOUNT
        );

        player.setHealth(newHealth);

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.REGENERATION,
                        REGENERATION_TICKS,
                        0
                )
        );

        playerCooldowns.put(
                itemId, now + COOLDOWN_MS
        );

        player.sendMessage(
                ChatColor.GREEN
                        + "You used an Herbal Remedy."
        );
    }

    private void consumeMainHand(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(
                    new ItemStack(Material.AIR)
            );
        } else {
            held.setAmount(held.getAmount() - 1);
        }
    }
}