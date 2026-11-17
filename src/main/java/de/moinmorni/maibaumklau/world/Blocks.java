package de.moinmorni.maibaumklau.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Blockzustände als Text ({@code spruce_stairs[facing=north,half=bottom]}): zerlegen, Eigenschaften setzen und grob
 * einordnen (voller Block? kann man hindurchlaufen?). Ohne Bukkit – die Baupläne lassen sich so ohne Server prüfen.
 */
public final class Blocks {

    /** Blöcke ohne vollen Würfel (Zäune verbinden sich nicht damit, Licht scheint durch). */
    private static final Set<String> PARTIAL = Set.of("air", "cave_air", "light", "barrier", "water", "lava", "snow",
            "cobweb", "scaffolding", "ladder", "lever", "bell", "anvil", "chipped_anvil", "damaged_anvil", "cauldron",
            "water_cauldron", "lantern", "soul_lantern", "torch", "wall_torch", "campfire", "soul_campfire",
            "iron_chain", "iron_bars", "flower_pot", "composter", "hopper", "brewing_stand", "grindstone", "lectern",
            "stonecutter", "enchanting_table", "end_rod", "lightning_rod", "decorated_pot", "dirt_path", "farmland",
            "cake", "chest", "trapped_chest", "ender_chest", "bamboo", "sugar_cane", "cactus", "candle", "rail",
            "tripwire_hook", "daylight_detector", "conduit", "heavy_core", "pointed_dripstone", "azalea",
            "flowering_azalea", "candle_cake");
    private static final List<String> PARTIAL_SUFFIX = List.of("_slab", "_stairs", "_fence", "_fence_gate", "_wall",
            "_pane", "_door", "_trapdoor", "_carpet", "_button", "_pressure_plate", "_sign", "_banner", "_candle",
            "_bed", "_head", "_skull", "_leaves", "_sapling", "_rail", "_tulip", "_bush", "_petals", "_coral",
            "_coral_fan", "_shulker_box");
    /** Pflanzen und Dinge ohne Kollision: hier kann man stehen und hindurchlaufen. */
    private static final Set<String> PASSABLE = Set.of("air", "cave_air", "light", "short_grass", "tall_grass", "fern",
            "large_fern", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
            "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "sunflower", "lilac",
            "rose_bush", "peony", "pink_petals", "wildflowers", "torch", "wall_torch", "tripwire", "vine", "wheat",
            "carrots", "potatoes", "beetroots", "sweet_berry_bush", "cobweb", "redstone_wire", "rail", "water",
            "sugar_cane", "bush", "firefly_bush", "leaf_litter", "short_dry_grass", "tall_dry_grass", "seagrass");
    private static final List<String> PASSABLE_SUFFIX = List.of("_sign", "_banner", "_button", "_pressure_plate",
            "_sapling", "_carpet");

    private Blocks() {
    }

    /** Name ohne Zustand und ohne {@code minecraft:} (Baupläne schreiben alles klein). */
    public static String base(String spec) {
        int bracket = spec.indexOf('[');
        int colon = spec.indexOf(':');
        int from = colon >= 0 && (bracket < 0 || colon < bracket) ? colon + 1 : 0;
        return bracket >= 0 ? spec.substring(from, bracket) : from == 0 ? spec : spec.substring(from);
    }

    /** Eigenschaften in Reihenfolge, z. B. {facing=north, half=bottom}. */
    public static Map<String, String> props(String spec) {
        Map<String, String> out = new LinkedHashMap<>();
        int open = spec.indexOf('[');
        int close = spec.lastIndexOf(']');
        if (open < 0 || close < open) {
            return out;
        }
        for (String part : spec.substring(open + 1, close).split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                out.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return out;
    }

    /** Zustand mit geänderten Eigenschaften. */
    public static String with(String spec, Map<String, String> changes) {
        Map<String, String> p = props(spec);
        p.putAll(changes);
        StringBuilder sb = new StringBuilder(base(spec));
        if (!p.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (var e : p.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
            sb.append(']');
        }
        return sb.toString();
    }

    /** Voller, undurchsichtiger Würfel (grob)? */
    public static boolean full(String spec) {
        String b = base(spec);
        if (PARTIAL.contains(b) || PASSABLE.contains(b) || b.startsWith("potted_")) {
            return false;
        }
        for (String p : PARTIAL_SUFFIX) {
            if (b.endsWith(p)) {
                return false;
            }
        }
        return true;
    }

    /** Kann man hindurchlaufen (Luft, Gras, Blumen, Schilder …)? */
    public static boolean passable(String spec) {
        if (spec == null) {
            return true;
        }
        String b = base(spec);
        if (PASSABLE.contains(b)) {
            return true;
        }
        for (String s : PASSABLE_SUFFIX) {
            if (b.endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean fence(String spec) {
        String b = base(spec);
        return b.endsWith("_fence");
    }

    public static boolean fenceGate(String spec) {
        return base(spec).endsWith("_fence_gate");
    }

    public static boolean wall(String spec) {
        String b = base(spec);
        return b.endsWith("_wall") && !b.endsWith("_sign") && !b.contains("_wall_");
    }

    public static boolean pane(String spec) {
        String b = base(spec);
        return b.endsWith("_pane") || b.equals("iron_bars");
    }

    /** Holzzaun (verbindet sich nur mit Holzzäunen) oder Netherziegelzaun. */
    static boolean woodFence(String spec) {
        return fence(spec) && !base(spec).equals("nether_brick_fence");
    }

    /** Für Tests: lesbarer Text in Kleinbuchstaben. */
    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
