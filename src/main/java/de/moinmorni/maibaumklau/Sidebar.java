package de.moinmorni.maibaumklau;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Leiste rechts: eigene Anzeige je Spieler, Zeilen als eigene Namen (ohne Zahlen). */
public final class Sidebar {

    private static final String OBJECTIVE = "mm_maibaumklau";
    private static final String TEAM_PREFIX = "mb";
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public void show(Player p, Component title, List<Component> lines) {
        Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(), u -> Bukkit.getScoreboardManager().getNewScoreboard());
        if (p.getScoreboard() != sb) {
            p.setScoreboard(sb);
        }
        Objective o = sb.getObjective(OBJECTIVE);
        if (o == null) {
            o = sb.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
            o.numberFormat(NumberFormat.blank());
        } else {
            o.displayName(title);
        }
        int n = lines.size();
        for (String entry : sb.getEntries()) {
            int idx = index(entry);
            if (idx < 0 || idx >= n) {
                sb.resetScores(entry);
            }
        }
        for (int i = 0; i < n; i++) {
            Score s = o.getScore(entry(i));
            s.customName(lines.get(i));
            s.setScore(n - i);
        }
    }

    /**
     * Farben der Namen (über dem Kopf, in der Tab-Liste) für diesen Betrachter: je Farbe ein Team mit farbigem Punkt
     * davor. Leere Karte = alle Farben weg.
     */
    public void teams(Player viewer, Map<String, Integer> colours) {
        Scoreboard sb = boards.computeIfAbsent(viewer.getUniqueId(), u -> Bukkit.getScoreboardManager().getNewScoreboard());
        if (viewer.getScoreboard() != sb) {
            viewer.setScoreboard(sb);
        }
        Map<Integer, Team> byColour = new HashMap<>();
        for (Team t : sb.getTeams()) {
            if (t.getName().startsWith(TEAM_PREFIX)) {
                t.unregister();
            }
        }
        colours.forEach((name, rgb) -> {
            Team t = byColour.computeIfAbsent(rgb, c -> {
                Team n = sb.registerNewTeam(TEAM_PREFIX + Integer.toHexString(c));
                TextColor tc = TextColor.color(c);
                n.color(NamedTextColor.nearestTo(tc));
                n.prefix(Component.text("● ", tc));
                return n;
            });
            t.addEntry(name);
        });
    }

    public void hide(Player p) {
        Scoreboard sb = boards.remove(p.getUniqueId());
        if (sb != null && p.getScoreboard() == sb) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void forget(UUID uuid) {
        boards.remove(uuid);
    }

    private static String entry(int i) {
        return "line" + i;
    }

    private static int index(String entry) {
        if (!entry.startsWith("line")) {
            return -1;
        }
        try {
            return Integer.parseInt(entry.substring(4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
