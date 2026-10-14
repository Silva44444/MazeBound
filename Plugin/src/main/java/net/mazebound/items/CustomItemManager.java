package net.mazebound.items;

import net.mazebound.Mazebound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class CustomItemManager {

    public static final String HERBAL_REMEDY = "herbal_remedy";
    public static final String HEALING_HERB = "healing_herb";

    private final Mazebound plugin;
    private final NamespacedKey itemIdKey;

    public CustomItemManager(Mazebound plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(
                plugin, "custom_item_id"
        );
    }

    public ItemStack createHerbalRemedy() {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException(
                    "Could not create Herbal Remedy item metadata."
            );
        }

        meta.setDisplayName(
                ChatColor.GREEN.toString()
                        + ChatColor.BOLD
                        + "Herbal Remedy"
        );

        meta.setLore(List.of(
        ChatColor.GRAY + "A remedy brewed from natural ingredients.",
        "",
        ChatColor.RED + "Instantly restores 1 heart",
        "",
        ChatColor.DARK_GREEN + "A little vitality goes a long way."
));

        meta.getPersistentDataContainer().set(
                itemIdKey,
                PersistentDataType.STRING,
                HERBAL_REMEDY
        );

        item.setItemMeta(meta);
        return item;
    }

    public String getCustomItemId(ItemStack item) {
        if (item == null || item.getType().isAir()
                || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(
                itemIdKey,
                PersistentDataType.STRING
        );
    }

   public void registerRecipes() {
    NamespacedKey recipeKey = new NamespacedKey(plugin, HERBAL_REMEDY);

    Bukkit.removeRecipe(recipeKey);

    ShapelessRecipe recipe = new ShapelessRecipe(
            recipeKey,
            createHerbalRemedy()
    );

    recipe.addIngredient(2, Material.SWEET_BERRIES);
    recipe.addIngredient(Material.WHEAT);
    recipe.addIngredient(Material.HONEY_BOTTLE);
    recipe.addIngredient(Material.FERN);

    Bukkit.addRecipe(recipe);

    plugin.getLogger().info("Registered Herbal Remedy recipe.");
}

public ItemStack createHealingHerb(int amount) {
    ItemStack item = new ItemStack(Material.FERN, amount);
    ItemMeta meta = item.getItemMeta();

    if (meta == null) {
        throw new IllegalStateException("Could not create Healing Herb.");
    }

    meta.setDisplayName(ChatColor.GREEN.toString()
            + ChatColor.BOLD + "Healing Herb");

    meta.setLore(List.of(
            ChatColor.GRAY + "A medicinal plant gathered from safe-cell gardens.",
            "",
            ChatColor.GREEN + "Used to craft Herbal Remedies."
    ));

    meta.getPersistentDataContainer().set(
            itemIdKey,
            PersistentDataType.STRING,
            HEALING_HERB
    );

    item.setItemMeta(meta);
    return item;
}
}