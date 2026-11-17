package de.moinmorni.maibaumklau.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wer kommt in die nächste Runde? Höchstens {@code max} Leute, Partys bleiben zusammen (eine Party, die nicht mehr
 * passt, wartet auf die nächste Runde – kleinere Gruppen dahinter dürfen nachrücken). Reihenfolge = wer zuerst da war.
 */
public final class Groups {

    private Groups() {
    }

    public static List<UUID> pick(List<List<UUID>> groups, int max) {
        List<UUID> out = new ArrayList<>();
        for (List<UUID> g : groups) {
            if (g.isEmpty()) {
                continue;
            }
            if (g.size() > max && out.isEmpty()) {
                // Riesen-Party: die ersten max Leute
                out.addAll(g.subList(0, max));
                return out;
            }
            if (out.size() + g.size() <= max) {
                out.addAll(g);
            }
        }
        return out;
    }
}
