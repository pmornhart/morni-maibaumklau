package de.moinmorni.maibaumklau;

import de.moinmorni.maibaumklau.data.Leaderboard;
import de.moinmorni.maibaumklau.game.Game;
import de.moinmorni.maibaumklau.game.Villager;
import de.moinmorni.maibaumklau.hall.HallMenus;
import de.moinmorni.maibaumklau.logic.Side;
import de.moinmorni.maibaumklau.world.HallPlan;
import de.moinmorni.maibaumklau.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Leiste rechts (Runde und Wirtsgarten) und die schwebenden Tafeln im Wirtsgarten. */
public final class Boards {

    private static final int TOP_ROWS = 8;

    private final MaibaumklauPlugin plugin;
    private final Sidebar sidebar = new Sidebar();
    private final List<Holo> hallHolos = new ArrayList<>();

    Boards(MaibaumklauPlugin plugin) {
        this.plugin = plugin;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    /** Leiste für alle, die eine Runde sehen. */
    public void game(Game g) {
        for (Player p : g.audience()) {
            String lang = plugin.lang(p);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "side.map", Placeholder.unparsed("map", Texts.plain(lang, "map." + g.map().id()))));
            lines.add(g.phase() == Game.Phase.START ? Texts.text(lang, "side.start")
                    : Texts.text(lang, "side.time", Placeholder.unparsed("time", Ui.clock(g.secondsLeft()))));
            lines.add(Component.text(" "));
            for (Side s : Side.values()) {
                lines.add(Texts.text(lang, "side.score", g.village(lang, s), Placeholder.unparsed("n",
                        String.valueOf(g.points(s)))));
            }
            lines.add(Component.text("  "));
            Villager v = g.villager(p.getUniqueId());
            if (v != null) {
                lines.add(Texts.text(lang, "side.own", Placeholder.component("state", g.stateOf(lang, v.side()))));
                lines.add(Texts.text(lang, "side.enemy", Placeholder.component("state", g.stateOf(lang, v.side().other()))));
                lines.add(Component.text("   "));
                lines.add(Texts.text(lang, "side.village", g.village(lang, v.side())));
                lines.add(Texts.text(lang, "side.points", Placeholder.unparsed("n", String.valueOf(v.points()))));
            } else {
                for (Side s : Side.values()) {
                    lines.add(Texts.text(lang, "side.tree", g.village(lang, s), Placeholder.component("state", g.stateOf(lang, s))));
                }
                lines.add(Component.text("   "));
                lines.add(Texts.text(lang, "side.spectating"));
            }
            lines.add(Component.text("    "));
            lines.add(Texts.text(lang, "side.site"));
            sidebar.show(p, Texts.text(lang, "side.title"), lines);
        }
    }

    /** Leiste im Wirtsgarten. */
    public void hall(Player p) {
        String lang = plugin.lang(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "side.hall.players", Placeholder.unparsed("n", String.valueOf(plugin.hall().waiting().size()))));
        int cd = plugin.hall().countdown();
        lines.add(cd >= 0 ? Texts.text(lang, "side.hall.start", Placeholder.unparsed("secs", String.valueOf(cd)))
                : Texts.text(lang, "side.hall.wait"));
        lines.add(Component.text(" "));
        MapType next = plugin.hall().nextMap();
        lines.add(Texts.text(lang, "side.map", Placeholder.unparsed("map",
                next == null ? "–" : Texts.plain(lang, "map." + next.id()))));
        lines.add(Component.text("  "));
        var d = plugin.store().loaded(p.getUniqueId());
        lines.add(Texts.text(lang, "side.hall.wins", Placeholder.unparsed("n", String.valueOf(d.map(x -> x.wins()).orElse(0)))));
        lines.add(Texts.text(lang, "side.hall.captures",
                Placeholder.unparsed("n", String.valueOf(d.map(x -> x.captures()).orElse(0)))));
        lines.add(Component.text("   "));
        lines.add(Texts.text(lang, "side.site"));
        sidebar.show(p, Texts.text(lang, "side.title"), lines);
    }

    public void hallAll() {
        for (UUID u : plugin.hall().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                hall(p);
            }
        }
    }

    // ---------- Tafeln im Wirtsgarten ----------

    public void spawnHall() {
        removeHall();
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < HallPlan.TABLES.length; i++) {
            MapType m = maps[i];
            int[] t = HallPlan.TABLES[i];
            hallHolos.add(Holo.spawn(plugin, at(t[0] + 0.5, 3.7, t[1] + 0.5), lang -> table(lang, m), 1f));
        }
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.BOARD[0] + 0.5, 2.9, HallPlan.BOARD[1] + 0.3), this::top, 0.9f));
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.HOWTO[0] + 0.3, 2.4, HallPlan.HOWTO[1] + 0.5),
                lang -> Texts.text(lang, "board.howto"), 0.8f));
        hallHolos.add(Holo.spawn(plugin, at(0.5, 4.4, 7.5), lang -> Texts.text(lang, "board.welcome"), 1.1f));
    }

    private Location at(double x, double y, double z) {
        return new Location(plugin.world(), x, MapType.FLOOR_Y + y, z);
    }

    private Component table(String lang, MapType m) {
        Game g = plugin.games().on(m);
        String state = g != null ? Texts.raw(lang, "board.table.running")
                : Texts.raw(lang, "board.table.votes").replace("<n>", String.valueOf(plugin.hall().mapVotes().count(m)));
        return Texts.text(lang, "board.table", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id())),
                Placeholder.unparsed("desc", Texts.plain(lang, "map." + m.id() + ".short")),
                Placeholder.parsed("state", state));
    }

    private Component top(String lang) {
        Component c = Texts.text(lang, "board.title");
        List<Leaderboard.Entry> top = plugin.board().top(TOP_ROWS);
        if (top.isEmpty()) {
            c = c.append(Component.newline()).append(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            c = c.append(Component.newline()).append(HallMenus.row(lang, i + 1, top.get(i)));
        }
        return c;
    }

    public void refreshHall() {
        if (hallHolos.isEmpty()) {
            return;
        }
        if (!hallHolos.stream().allMatch(Holo::valid)) {
            spawnHall();
            return;
        }
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length; i++) {
            MapType m = maps[i];
            hallHolos.get(i).update(lang -> table(lang, m));
        }
        hallHolos.get(maps.length).update(this::top);
    }

    public void removeHall() {
        hallHolos.forEach(Holo::remove);
        hallHolos.clear();
    }
}
