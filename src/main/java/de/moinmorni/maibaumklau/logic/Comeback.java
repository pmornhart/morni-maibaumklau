package de.moinmorni.maibaumklau.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * „Zurück ins Spiel“: Wer mitten in einer Runde die Verbindung verliert (zum Beispiel, weil das Spiel abstürzt), behält
 * seinen Platz eine Weile – {@code rueckkehr-sekunden} in der config.yml. Hier stehen nur die Regeln und die Fristen,
 * ohne Server: Die Zeit kommt in Spielticks von außen (hängt der Server, läuft auch die Frist nicht weiter).
 */
public final class Comeback {

    /** Spielticks pro Sekunde. */
    public static final int TICKS_PER_SECOND = 20;

    private final Map<UUID, Long> deadlines = new LinkedHashMap<>();

    /**
     * Platz freihalten? Nur in einer laufenden Runde (nicht im Nachspiel) und nur, wenn es eingeschaltet ist. Wer den
     * Baum trug, legt ihn dabei ab – ein Verbindungsabbruch rettet also nichts.
     *
     * @param seconds {@code rueckkehr-sekunden} (0 = aus)
     * @param running läuft die Runde noch (nicht schon vorbei)?
     */
    public static boolean holds(int seconds, boolean running) {
        return seconds > 0 && running;
    }

    /** Verbindung weg: Platz bis {@code now + seconds} freihalten. */
    public void away(UUID player, long now, int seconds) {
        deadlines.put(player, now + (long) seconds * TICKS_PER_SECOND);
    }

    /** Wartet die Runde noch auf diese Person? */
    public boolean waitingFor(UUID player) {
        return deadlines.containsKey(player);
    }

    /** Wieder da: true, wenn der Platz noch frei war. Die Frist ist danach vergessen. */
    public boolean back(UUID player, long now) {
        Long until = deadlines.remove(player);
        return until != null && now <= until;
    }

    /** Abgelaufene Fristen – in der Reihenfolge, in der die Leute gegangen sind. Sie sind danach vergessen. */
    public List<UUID> expired(long now) {
        List<UUID> out = new ArrayList<>();
        deadlines.entrySet().removeIf(e -> {
            if (now > e.getValue()) {
                out.add(e.getKey());
                return true;
            }
            return false;
        });
        return out;
    }

    /** Restliche Sekunden (aufgerundet); 0, wenn auf niemanden gewartet wird oder die Frist vorbei ist. */
    public int secondsLeft(UUID player, long now) {
        Long until = deadlines.get(player);
        if (until == null || now >= until) {
            return 0;
        }
        return (int) ((until - now + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    /** Wie viele gerade weg sind und zurückkommen dürfen. */
    public int count() {
        return deadlines.size();
    }

    /** Runde vorbei: alle Fristen vergessen. Liefert, auf wen noch gewartet wurde (Angebot zurücknehmen). */
    public List<UUID> clear() {
        List<UUID> out = new ArrayList<>(deadlines.keySet());
        deadlines.clear();
        return out;
    }
}
