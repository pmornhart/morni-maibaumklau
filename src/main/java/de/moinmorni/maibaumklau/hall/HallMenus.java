package de.moinmorni.maibaumklau.hall;

import de.moinmorni.maibaumklau.Items;
import de.moinmorni.maibaumklau.MaibaumklauPlugin;
import de.moinmorni.maibaumklau.Texts;
import de.moinmorni.maibaumklau.data.Leaderboard;
import de.moinmorni.maibaumklau.data.PlayerData;
import de.moinmorni.maibaumklau.game.Game;
import de.moinmorni.maibaumklau.menu.Menu;
import de.moinmorni.maibaumklau.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Truhen-Menüs des Wirtsgartens: Abstimmung (Karte, zuschauen), So geht's, Statistik. Alles mit einfachem Klick. */
public final class HallMenus {

    public static final int[] MAP_SLOTS = {11, 15};
    public static final int CLOSE = 22;
    /** So geht's: sechs Kapitel in einer Reihe. */
    static final String[] HOWTO = {"goal", "lift", "carry", "tag", "helps", "ransom"};
    static final String[] HOWTO_ICONS = {"white_banner", "iron_axe", "stripped_spruce_log", "stick", "bread", "honey_bottle"};
    public static final int FIRST_HOWTO = 10;

    private HallMenus() {
    }

    private static Material material(String name) {
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m == null ? Material.PAPER : m;
    }

    public static void vote(MaibaumklauPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.vote.title"));
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < MAP_SLOTS.length; i++) {
            MapType m = maps[i];
            Game running = plugin.games().on(m);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "map." + m.id() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.vote.villages", Placeholder.unparsed("na", Texts.plain(lang, "village." + m.id() + ".blau")),
                    Placeholder.unparsed("nb", Texts.plain(lang, "village." + m.id() + ".rot"))));
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n", String.valueOf(plugin.hall().mapVotes().count(m)))));
            boolean mine = plugin.hall().mapVotes().voteOf(p.getUniqueId()) == m;
            if (running != null) {
                lore.add(Texts.text(lang, "menu.vote.busy", Placeholder.unparsed("n", String.valueOf(running.activeCount()))));
            } else if (plugin.construction().restoring(m)) {
                lore.add(Texts.text(lang, "menu.vote.cleaning"));
            } else {
                lore.add(Texts.text(lang, mine ? "menu.vote.mine" : "menu.vote.click"));
            }
            menu.set(MAP_SLOTS[i], Items.glow(Items.named(material(m.icon()), 1, Texts.text(lang, "map." + m.id())
                    .color(TextColor.color(Texts.SKY)), lore), mine), q -> {
                        q.closeInventory();
                        Game g = plugin.games().on(m);
                        if (g != null) {
                            plugin.spectate(q, g);
                        } else {
                            plugin.hall().vote(q, m);
                        }
                    });
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    /** So geht's: ein Kapitel je Gegenstand (Maus drüber zeigt den Text – auf Bedrock antippen). */
    public static void howto(MaibaumklauPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.howto.title"));
        for (int i = 0; i < HOWTO.length; i++) {
            String k = HOWTO[i];
            menu.set(FIRST_HOWTO + i + (i >= 3 ? 1 : 0), Items.named(material(HOWTO_ICONS[i]), 1,
                    Texts.text(lang, "howto." + k).color(TextColor.color(Texts.GOLD)), Texts.lines(lang, "howto." + k + ".text")),
                    q -> q.sendMessage(Texts.msg(lang, "howto." + k + ".chat")));
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static void stats(MaibaumklauPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.stats.title"));
        PlayerData d = plugin.data(p);
        menu.set(11, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "menu.stats.me"), statLines(lang, d)), null);
        List<Component> lore = new ArrayList<>();
        List<Leaderboard.Entry> top = plugin.board().top(10);
        if (top.isEmpty()) {
            lore.add(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            lore.add(row(lang, i + 1, top.get(i)));
        }
        menu.set(15, Items.named(Material.GOLDEN_HELMET, 1, Texts.text(lang, "menu.stats.top"), lore), null);
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static Component row(String lang, int pos, Leaderboard.Entry e) {
        return Texts.text(lang, "top.row", Placeholder.unparsed("pos", String.valueOf(pos)),
                Placeholder.unparsed("name", e.name()), Placeholder.unparsed("wins", String.valueOf(e.wins())),
                Placeholder.unparsed("captures", String.valueOf(e.captures())),
                Placeholder.unparsed("points", String.valueOf(e.points())));
    }

    public static List<Component> statLines(String lang, PlayerData d) {
        return Texts.lines(lang, "stats.lines", Placeholder.unparsed("games", String.valueOf(d.games())),
                Placeholder.unparsed("wins", String.valueOf(d.wins())), Placeholder.unparsed("draws", String.valueOf(d.draws())),
                Placeholder.unparsed("tags", String.valueOf(d.tags())), Placeholder.unparsed("saves", String.valueOf(d.saves())),
                Placeholder.unparsed("lifts", String.valueOf(d.lifts())),
                Placeholder.unparsed("captures", String.valueOf(d.captures())),
                Placeholder.unparsed("carried", String.valueOf(d.carried())),
                Placeholder.unparsed("points", String.valueOf(d.points())),
                Placeholder.unparsed("best", String.valueOf(d.bestPoints())));
    }
}
