package de.moinmorni.maibaumklau.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Wartezeiten der Hilfen je Person, in Spielticks (hängt der Server, läuft auch die Wartezeit nicht weiter). */
public final class Cooldowns {

    private final Map<UUID, Map<Help, Long>> readyAt = new HashMap<>();

    /** Ist die Hilfe wieder bereit? */
    public boolean ready(UUID player, Help help, long now) {
        return left(player, help, now) == 0;
    }

    /** Benutzt: erst nach {@code ticks} wieder bereit. */
    public void use(UUID player, Help help, long now, long ticks) {
        readyAt.computeIfAbsent(player, u -> new HashMap<>()).put(help, now + Math.max(0, ticks));
    }

    /** Restliche Ticks (0 = bereit). */
    public long left(UUID player, Help help, long now) {
        Long at = readyAt.getOrDefault(player, Map.of()).get(help);
        return at == null ? 0 : Math.max(0, at - now);
    }

    /** Restliche Sekunden, aufgerundet. */
    public int secondsLeft(UUID player, Help help, long now) {
        return (int) ((left(player, help, now) + Comeback.TICKS_PER_SECOND - 1) / Comeback.TICKS_PER_SECOND);
    }

    public void clear() {
        readyAt.clear();
    }
}
