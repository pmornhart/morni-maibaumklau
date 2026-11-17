package de.moinmorni.maibaumklau.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Zwei gleich große Dörfer, Partys zusammen, wer zuerst da war, kommt zuerst. */
class TeamsTest {

    private static List<UUID> people(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new UUID(0, i));
        }
        return out;
    }

    private static List<List<UUID>> singles(List<UUID> ps) {
        return ps.stream().map(List::of).toList();
    }

    @Test
    void singlesAreSplitEvenly() {
        for (int n = 2; n <= 16; n++) {
            List<List<UUID>> t = Teams.split(singles(people(n)));
            assertEquals(2, t.size());
            assertTrue(Math.abs(t.get(0).size() - t.get(1).size()) <= 1, n + " Leute: " + t);
            assertEquals(n, t.get(0).size() + t.get(1).size());
            Set<UUID> all = new HashSet<>(t.get(0));
            all.addAll(t.get(1));
            assertEquals(n, all.size(), "niemand doppelt");
        }
    }

    @Test
    void partiesStayTogether() {
        List<UUID> p = people(6);
        List<List<UUID>> groups = List.of(List.of(p.get(0)), List.of(p.get(1), p.get(2)), List.of(p.get(3)),
                List.of(p.get(4), p.get(5)));
        List<List<UUID>> t = Teams.split(groups);
        assertEquals(3, t.get(0).size());
        assertEquals(3, t.get(1).size());
        for (List<UUID> side : t) {
            assertEquals(side.contains(p.get(1)), side.contains(p.get(2)), "Party 1 zusammen");
            assertEquals(side.contains(p.get(4)), side.contains(p.get(5)), "Party 2 zusammen");
        }
    }

    @Test
    void aPartyOfTwoInFourIsTogether() {
        List<UUID> p = people(4);
        List<List<UUID>> t = Teams.split(List.of(List.of(p.get(0), p.get(2)), List.of(p.get(1)), List.of(p.get(3))));
        List<UUID> withAnna = t.get(0).contains(p.get(0)) ? t.get(0) : t.get(1);
        assertTrue(withAnna.contains(p.get(2)), "Anna und Carl zusammen: " + t);
        assertEquals(2, t.get(0).size());
    }

    @Test
    void tooBigPartiesAreSplitRatherThanUnfair() {
        List<UUID> p = people(4);
        List<List<UUID>> t = Teams.split(List.of(List.of(p.get(0), p.get(1), p.get(2)), List.of(p.get(3))));
        assertEquals(2, t.get(0).size());
        assertEquals(2, t.get(1).size());
    }

    @Test
    void twoPeopleAreOneAgainstOne() {
        List<List<UUID>> t = Teams.split(singles(people(2)));
        assertEquals(1, t.get(0).size());
        assertEquals(1, t.get(1).size());
    }

    @Test
    void groupsPickKeepsPartiesAndTheLimit() {
        List<UUID> p = people(5);
        List<UUID> picked = Groups.pick(List.of(List.of(p.get(0), p.get(1)), List.of(p.get(2), p.get(3)), List.of(p.get(4))),
                3);
        assertEquals(List.of(p.get(0), p.get(1), p.get(4)), picked, "zweite Party wartet, der Einzelne rückt nach");
    }
}
