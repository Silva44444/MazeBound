package net.mazebound.world;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

/**
 * Wall block palettes per biome. Each palette is a weighted mix, so walls
 * look weathered instead of uniform. Weights are out of 10,000 and every
 * palette must add up to exactly that.
 *
 * "Rare" entries (suspicious sand) are brushable blocks that carry a vanilla
 * archaeology loot table. They are kept very rare and only placed within a
 * height band, because each one is a block entity.
 */
public final class BiomePalettes {

    public static final int SCALE = 10_000;

    public record Entry(Material material, int weight, String lootTable) {

        public boolean isRare() {
            return lootTable != null;
        }
    }

    public static final class Palette {

        private final String name;
        private final Entry[] entries;
        private final int[] cumulative;

        private Palette(String name, Entry... entries) {
            this.name = name;
            this.entries = entries;
            this.cumulative = new int[entries.length];

            int sum = 0;

            for (int i = 0; i < entries.length; i++) {
                sum += entries[i].weight();
                cumulative[i] = sum;
            }

            if (sum != SCALE) {
                throw new IllegalStateException(
                        "Palette '" + name + "' weights add up to " + sum
                                + ", expected " + SCALE
                );
            }
        }

        public String name() {
            return name;
        }

        public List<Entry> entries() {
            return List.of(entries);
        }

        /**
         * @param roll        number in [0, SCALE)
         * @param rareAllowed false = a rare result becomes the main block
         */
        public Entry pick(int roll, boolean rareAllowed) {

            for (int i = 0; i < entries.length; i++) {
                if (roll < cumulative[i]) {
                    Entry entry = entries[i];
                    return entry.isRare() && !rareAllowed ? entries[0] : entry;
                }
            }

            return entries[0];
        }
    }

    private static Entry e(Material material, int weight) {
        return new Entry(material, weight, null);
    }

    private static Entry rare(Material material, int weight, String lootTable) {
        return new Entry(material, weight, lootTable);
    }

    public static final Palette DEFAULT = new Palette("stone",
            e(Material.STONE_BRICKS, 5500),
            e(Material.CRACKED_STONE_BRICKS, 2000),
            e(Material.MOSSY_STONE_BRICKS, 1500),
            e(Material.ANDESITE, 700),
            e(Material.CHISELED_STONE_BRICKS, 300));

    public static final Palette DESERT = new Palette("desert",
            e(Material.SANDSTONE, 4400),
            e(Material.CUT_SANDSTONE, 2000),
            e(Material.SMOOTH_SANDSTONE, 1000),
            e(Material.SAND, 2000),
            e(Material.CHISELED_SANDSTONE, 595),
            rare(Material.SUSPICIOUS_SAND, 5, "archaeology/desert_pyramid"));

    public static final Palette BADLANDS = new Palette("badlands",
            e(Material.TERRACOTTA, 3000),
            e(Material.RED_SANDSTONE, 2500),
            e(Material.ORANGE_TERRACOTTA, 1500),
            e(Material.YELLOW_TERRACOTTA, 1000),
            e(Material.BROWN_TERRACOTTA, 1000),
            e(Material.CUT_RED_SANDSTONE, 600),
            e(Material.RED_TERRACOTTA, 400));

    public static final Palette JUNGLE = new Palette("jungle",
            e(Material.MOSSY_STONE_BRICKS, 4000),
            e(Material.STONE_BRICKS, 1500),
            e(Material.MOSSY_COBBLESTONE, 2000),
            e(Material.CRACKED_STONE_BRICKS, 1000),
            e(Material.MOSS_BLOCK, 1000),
            e(Material.CHISELED_STONE_BRICKS, 500));

    public static final Palette SWAMP = new Palette("swamp",
            e(Material.MUD_BRICKS, 3500),
            e(Material.MOSSY_STONE_BRICKS, 2000),
            e(Material.STONE_BRICKS, 1500),
            e(Material.MUD, 1000),
            e(Material.PACKED_MUD, 1500),
            e(Material.MOSS_BLOCK, 500));

    public static final Palette SNOWY = new Palette("snowy",
            e(Material.SNOW_BLOCK, 2500),
            e(Material.PACKED_ICE, 1500),
            e(Material.STONE_BRICKS, 2500),
            e(Material.CRACKED_STONE_BRICKS, 1000),
            e(Material.BLUE_ICE, 500),
            e(Material.CALCITE, 2000));

    public static final Palette TAIGA = new Palette("taiga",
            e(Material.STONE_BRICKS, 3000),
            e(Material.MOSSY_STONE_BRICKS, 2000),
            e(Material.COBBLESTONE, 2000),
            e(Material.MOSSY_COBBLESTONE, 1500),
            e(Material.SPRUCE_PLANKS, 500),
            e(Material.CRACKED_STONE_BRICKS, 1000));

    public static final Palette SAVANNA = new Palette("savanna",
            e(Material.TERRACOTTA, 2500),
            e(Material.ORANGE_TERRACOTTA, 1500),
            e(Material.ACACIA_PLANKS, 1000),
            e(Material.STONE_BRICKS, 2500),
            e(Material.COARSE_DIRT, 1000),
            e(Material.CRACKED_STONE_BRICKS, 1500));

    public static final Palette CHERRY = new Palette("cherry",
            e(Material.STONE_BRICKS, 3000),
            e(Material.CHERRY_PLANKS, 1500),
            e(Material.PINK_TERRACOTTA, 1500),
            e(Material.STRIPPED_CHERRY_LOG, 1000),
            e(Material.CALCITE, 1000),
            e(Material.MOSSY_STONE_BRICKS, 1000),
            e(Material.CRACKED_STONE_BRICKS, 1000));

    public static final Palette DARK_FOREST = new Palette("dark_forest",
            e(Material.DEEPSLATE_BRICKS, 3000),
            e(Material.CRACKED_DEEPSLATE_BRICKS, 1500),
            e(Material.MOSSY_COBBLESTONE, 2000),
            e(Material.STONE_BRICKS, 1500),
            e(Material.DARK_OAK_PLANKS, 1000),
            e(Material.COBBLED_DEEPSLATE, 1000));

    public static final Palette MUSHROOM = new Palette("mushroom",
            e(Material.MUSHROOM_STEM, 2500),
            e(Material.BROWN_MUSHROOM_BLOCK, 2500),
            e(Material.RED_MUSHROOM_BLOCK, 1000),
            e(Material.STONE_BRICKS, 2500),
            e(Material.MOSSY_STONE_BRICKS, 1500));

    public static final Palette BEACH = new Palette("beach",
            e(Material.SANDSTONE, 4000),
            e(Material.CUT_SANDSTONE, 2000),
            e(Material.SAND, 2500),
            e(Material.STONE_BRICKS, 1000),
            e(Material.SMOOTH_SANDSTONE, 495),
            rare(Material.SUSPICIOUS_SAND, 5, "archaeology/ocean_ruin_warm"));

    public static final Palette OCEAN = new Palette("ocean",
            e(Material.PRISMARINE_BRICKS, 3000),
            e(Material.PRISMARINE, 2000),
            e(Material.DARK_PRISMARINE, 500),
            e(Material.STONE_BRICKS, 2500),
            e(Material.MOSSY_STONE_BRICKS, 1500),
            e(Material.CRACKED_STONE_BRICKS, 500));

    public static final Palette STONY = new Palette("stony",
            e(Material.COBBLESTONE, 2000),
            e(Material.STONE_BRICKS, 2500),
            e(Material.ANDESITE, 2000),
            e(Material.CRACKED_STONE_BRICKS, 1500),
            e(Material.MOSSY_COBBLESTONE, 1000),
            e(Material.DRIPSTONE_BLOCK, 1000));

    public static List<Palette> all() {
        return List.of(DEFAULT, DESERT, BADLANDS, JUNGLE, SWAMP, SNOWY, TAIGA,
                SAVANNA, CHERRY, DARK_FOREST, MUSHROOM, BEACH, OCEAN, STONY);
    }

    /**
     * Picks a palette from a biome key such as "minecraft:desert". Matching
     * is by name so datapack biomes (Terralith and similar) work too, e.g.
     * "terralith:desert_canyon" counts as desert. Unknown biomes get the
     * default stone palette.
     */
    public static Palette forBiome(String biomeKey) {

        if (biomeKey == null || biomeKey.isBlank()) {
            return DEFAULT;
        }

        String path = biomeKey.toLowerCase(Locale.ROOT);
        int colon = path.indexOf(':');

        if (colon >= 0) {
            path = path.substring(colon + 1);
        }

        // Order matters: more specific names must come before general ones.
        if (path.contains("desert")) return DESERT;
        if (path.contains("badlands") || path.contains("mesa")) return BADLANDS;
        if (path.contains("jungle") || path.contains("bamboo")) return JUNGLE;
        if (path.contains("swamp") || path.contains("mangrove")) return SWAMP;
        if (path.contains("cherry")) return CHERRY;
        if (path.contains("stony")) return STONY;
        if (path.contains("frozen") || path.contains("snow")
                || path.contains("ice") || path.contains("jagged")
                || path.contains("grove")) return SNOWY;
        if (path.contains("taiga")) return TAIGA;
        if (path.contains("savanna")) return SAVANNA;
        if (path.contains("dark_forest")) return DARK_FOREST;
        if (path.contains("mushroom")) return MUSHROOM;
        if (path.contains("beach")) return BEACH;
        if (path.contains("ocean")) return OCEAN;
        if (path.contains("windswept") || path.contains("peaks")) return STONY;

        return DEFAULT;
    }

    private BiomePalettes() {
    }
}