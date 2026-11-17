package de.moinmorni.maibaumklau;

import de.moinmorni.maibaumklau.logic.Rules;
import org.bukkit.configuration.ConfigurationSection;

/** Werte aus der config.yml (Erklärungen stehen dort). Alles wird auf sinnvolle Grenzen gestutzt. */
public record Settings(
        String world,
        int hallCountdown,
        int hallMinPlayers,
        int maxPerVillage,
        int startSeconds,
        int roundSeconds,
        int afterSeconds,
        int comebackSeconds,
        Rules rules,
        int snackSeconds,
        int snackPause,
        int hayPause,
        int ladders,
        int ladderSeconds) {

    /** Höchstens so lange wartet eine Runde auf jemanden, der die Verbindung verloren hat. */
    static final int MAX_COMEBACK_SECONDS = 600;

    public static Settings load(ConfigurationSection c) {
        Rules defaults = Rules.defaults();
        double alone = clamp(c.getDouble("maibaum.tempo-allein", defaults.speedAlone()), 0.2, 1.0);
        Rules rules = new Rules(
                clamp(c.getDouble("maibaum.umlegen-sekunden", defaults.liftSeconds()), 2, 60),
                clamp(c.getDouble("maibaum.zurueck-sekunden", defaults.returnSeconds()), 3, 120),
                alone,
                clamp(c.getDouble("maibaum.tempo-je-traeger", defaults.speedPerCarrier()), 0, 0.5),
                clamp(c.getDouble("maibaum.tempo-hoechstens", defaults.speedMax()), alone, 1.0),
                c.getBoolean("maibaum.eigener-baum-muss-stehen", defaults.ownMustStand()),
                clamp(c.getDouble("hilfen.verschnauf-sekunden", defaults.restSeconds()), 0, 15));
        return new Settings(
                c.getString("welt", "maibaumklau"),
                clamp(c.getInt("wirtsgarten.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("wirtsgarten.mindestens-spieler", 4), 2, 16),
                clamp(c.getInt("runde.hoechstens-je-dorf", 8), 1, 8),
                clamp(c.getInt("runde.start-sekunden", 5), 3, 30),
                clamp(c.getInt("runde.minuten", 8), 2, 30) * 60,
                clamp(c.getInt("runde.nachspiel-sekunden", 12), 5, 60),
                clamp(c.getInt("rueckkehr-sekunden", 90), 0, MAX_COMEBACK_SECONDS),
                rules,
                clamp(c.getInt("hilfen.brotzeit-sekunden", 4), 1, 20),
                clamp(c.getInt("hilfen.brotzeit-pause", 20), 3, 120),
                clamp(c.getInt("hilfen.heusprung-pause", 12), 3, 120),
                clamp(c.getInt("hilfen.leitern", 4), 0, 16),
                clamp(c.getInt("hilfen.leiter-sekunden", 30), 5, 300));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
