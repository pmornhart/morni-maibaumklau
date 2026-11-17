package de.moinmorni.maibaumklau.data;

import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Werte einer Person über alle Runden: gespielt, gewonnen, unentschieden, abgefangen, Bäume gerettet, umgelegt,
 * heimgebracht, Strecke mit dem Baum, Punkte und die beste Runde.
 */
public final class PlayerData {

    /** Was eine Person in einer Runde geschafft hat. */
    public record Round(boolean won, boolean draw, int tags, int saves, int lifts, int captures, int carried, int points) {
    }

    private final UUID uuid;
    private String name;
    private int games;
    private int wins;
    private int draws;
    private int tags;
    private int saves;
    private int lifts;
    private int captures;
    private int carried;
    private int points;
    private int bestPoints;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int games() {
        return games;
    }

    public int wins() {
        return wins;
    }

    public int draws() {
        return draws;
    }

    /** Wie oft diese Person jemanden abgefangen hat. */
    public int tags() {
        return tags;
    }

    /** Wie oft sie einen Träger abgefangen (den eigenen Baum gerettet) hat. */
    public int saves() {
        return saves;
    }

    public int lifts() {
        return lifts;
    }

    public int captures() {
        return captures;
    }

    /** Blöcke, die sie einen Baum getragen hat. */
    public int carried() {
        return carried;
    }

    public int points() {
        return points;
    }

    public int bestPoints() {
        return bestPoints;
    }

    /** Eine Runde zu Ende: Werte hochzählen; liefert {@code true} bei neuer Bestleistung (meiste Punkte in einer Runde). */
    public boolean add(Round r) {
        games++;
        if (r.won()) {
            wins++;
        }
        if (r.draw()) {
            draws++;
        }
        tags += Math.max(0, r.tags());
        saves += Math.max(0, r.saves());
        lifts += Math.max(0, r.lifts());
        captures += Math.max(0, r.captures());
        carried += Math.max(0, r.carried());
        points += Math.max(0, r.points());
        if (r.points() > bestPoints) {
            bestPoints = r.points();
            return true;
        }
        return false;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("runden", games);
        y.set("siege", wins);
        y.set("unentschieden", draws);
        y.set("abgefangen", tags);
        y.set("gerettet", saves);
        y.set("umgelegt", lifts);
        y.set("heimgebracht", captures);
        y.set("getragen", carried);
        y.set("punkte", points);
        y.set("beste-runde", bestPoints);
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.games = y.getInt("runden");
        d.wins = y.getInt("siege");
        d.draws = y.getInt("unentschieden");
        d.tags = y.getInt("abgefangen");
        d.saves = y.getInt("gerettet");
        d.lifts = y.getInt("umgelegt");
        d.captures = y.getInt("heimgebracht");
        d.carried = y.getInt("getragen");
        d.points = y.getInt("punkte");
        d.bestPoints = y.getInt("beste-runde");
        return d;
    }
}
