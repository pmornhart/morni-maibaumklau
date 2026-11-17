package de.moinmorni.maibaumklau;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Gegenstände bauen und markieren (Markierung im Gegenstand, nicht am Namen – Namen hängen an der Sprache). */
public final class Items {

    /** Hilfe in der Runde (Stecken, Brotzeit, Heuballen, Leiter, Kompass). */
    private static NamespacedKey helpKey;
    /** Gegenstand des Wirtsgartens: Aktion wie „vote“ oder „lobby“. */
    private static NamespacedKey hallKey;

    private Items() {
    }

    static void init(Plugin plugin) {
        helpKey = new NamespacedKey(plugin, "item");
        hallKey = new NamespacedKey(plugin, "hall");
    }

    public static ItemStack named(Material material, int amount, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        item.editMeta(meta -> {
            if (name != null) {
                meta.displayName(name);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    public static ItemStack glow(ItemStack item, boolean glow) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null));
        return item;
    }

    public static ItemStack helpTag(ItemStack item, String id) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(helpKey, PersistentDataType.STRING, id));
        return item;
    }

    public static String helpTag(ItemStack item) {
        return read(item, helpKey);
    }

    public static ItemStack hallTag(ItemStack item, String action) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(hallKey, PersistentDataType.STRING, action));
        return item;
    }

    public static String hallTag(ItemStack item) {
        return read(item, hallKey);
    }

    private static String read(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /** Lederrüstung in der Farbe des Dorfs (unzerbrechlich). */
    public static ItemStack dyed(Material material, int rgb, Component name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leather) {
                leather.setColor(org.bukkit.Color.fromRGB(rgb));
            }
            meta.displayName(name);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
        });
        return item;
    }

    /** Leere Glasscheibe als Rahmen in Menüs. */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.setHideTooltip(true));
        return item;
    }
}
