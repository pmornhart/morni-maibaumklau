package de.moinmorni.maibaumklau.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Wer kommt in welches Dorf? Beide Dörfer sollen gleich groß sein (höchstens einer Unterschied). Partys bleiben
 * zusammen, solange das geht: die größten Gruppen zuerst, jede ins kleinere Dorf. Passt eine Party nicht mehr ganz
 * hinein (sie wäre größer als die Hälfte), wird sie geteilt – lieber getrennt als ungerecht.
 */
public final class Teams {

    private Teams() {
    }

    /**
     * Gruppen (Partys oder Einzelne, in der Reihenfolge, in der sie da waren) auf zwei Dörfer verteilen.
     *
     * @return genau zwei Listen: Dorf 0 (Blau) und Dorf 1 (Rot)
     */
    public static List<List<UUID>> split(List<List<UUID>> groups) {
        int total = groups.stream().mapToInt(List::size).sum();
        int cap = (total + 1) / 2;
        List<List<UUID>> sorted = new ArrayList<>(groups.stream().filter(g -> !g.isEmpty()).toList());
        // Stabil: bei gleicher Größe bleibt, wer zuerst da war, vorn.
        sorted.sort(Comparator.comparingInt((List<UUID> g) -> g.size()).reversed());
        List<List<UUID>> teams = List.of(new ArrayList<>(), new ArrayList<>());
        for (List<UUID> g : sorted) {
            List<UUID> small = teams.get(0).size() <= teams.get(1).size() ? teams.get(0) : teams.get(1);
            List<UUID> big = small == teams.get(0) ? teams.get(1) : teams.get(0);
            if (small.size() + g.size() <= cap) {
                small.addAll(g);
            } else if (big.size() + g.size() <= cap) {
                big.addAll(g);
            } else {
                // Zu groß für beide: das kleinere Dorf auffüllen, der Rest kommt ins andere.
                for (UUID u : g) {
                    List<UUID> into = teams.get(0).size() <= teams.get(1).size() ? teams.get(0) : teams.get(1);
                    into.add(u);
                }
            }
        }
        // Die letzten Einzelnen gleichen aus, falls der Unterschied doch größer als eins wurde.
        while (Math.abs(teams.get(0).size() - teams.get(1).size()) > 1) {
            List<UUID> from = teams.get(0).size() > teams.get(1).size() ? teams.get(0) : teams.get(1);
            List<UUID> to = from == teams.get(0) ? teams.get(1) : teams.get(0);
            to.add(from.remove(from.size() - 1));
        }
        return List.of(List.copyOf(teams.get(0)), List.copyOf(teams.get(1)));
    }
}
