package de.moinmorni.maibaumklau.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spielerdaten und Bestenliste: zählen, speichern, wieder lesen, Reihenfolge. */
class DataTest {

    private static final UUID ANNA = new UUID(0, 1);
    private static final UUID BERT = new UUID(0, 2);
    private static final UUID CARL = new UUID(0, 3);

    @Test
    void roundsAddUpAndSurviveSaving() {
        PlayerData d = new PlayerData(ANNA, "Anna");
        assertTrue(d.add(new PlayerData.Round(true, false, 2, 1, 1, 1, 37, 41)), "erste Runde ist Bestleistung");
        assertFalse(d.add(new PlayerData.Round(false, true, 1, 0, 0, 0, 0, 2)));
        PlayerData back = PlayerData.fromYaml(ANNA, d.toYaml());
        assertEquals(2, back.games());
        assertEquals(1, back.wins());
        assertEquals(1, back.draws());
        assertEquals(3, back.tags());
        assertEquals(1, back.saves());
        assertEquals(1, back.lifts());
        assertEquals(1, back.captures());
        assertEquals(37, back.carried());
        assertEquals(43, back.points());
        assertEquals(41, back.bestPoints());
        assertEquals("Anna", back.name());
    }

    @Test
    void leaderboardOrdersByWinsThenCapturesThenPoints() {
        Leaderboard b = new Leaderboard();
        b.update(ANNA, "Anna", 2, 1, 50, 4);
        b.update(BERT, "Bert", 2, 2, 10, 5);
        b.update(CARL, "Carl", 3, 0, 5, 9);
        assertEquals("Carl", b.top(3).get(0).name());
        assertEquals("Bert", b.top(3).get(1).name(), "gleiche Siege: mehr heimgebracht vorn");
        assertEquals(3, b.rank(ANNA));
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(b.top(3), back.top(3));
    }

    @Test
    void leaderboardKeepsOnlyTheBest() {
        Leaderboard b = new Leaderboard();
        for (int i = 0; i < Leaderboard.KEEP + 5; i++) {
            b.update(new UUID(1, i), "P" + i, i, 0, 0, 1);
        }
        assertEquals(Leaderboard.KEEP, b.top(1000).size());
        assertEquals(0, b.rank(new UUID(1, 0)), "der Schwächste ist raus");
    }
}
