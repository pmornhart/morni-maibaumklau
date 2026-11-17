package de.moinmorni.maibaumklau.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tempo der Träger und wer wen abfangen darf. */
class RulesTest {

    @Test
    void carriersAreSlowButFasterTogether() {
        Rules r = Rules.defaults();
        assertEquals(0.55, r.carrySpeed(1), 1e-9);
        assertEquals(0.67, r.carrySpeed(2), 1e-9);
        assertEquals(0.79, r.carrySpeed(3), 1e-9);
        assertEquals(0.9, r.carrySpeed(4), 1e-9, "gedeckelt");
        assertEquals(0.9, r.carrySpeed(8), 1e-9);
        assertEquals(1.0, r.carrySpeed(0), 1e-9, "ohne Baum normal");
        assertTrue(r.carrySpeed(2) > r.carrySpeed(1));
        Rules fast = new Rules(8, 20, 0.8, 0.5, 2.0, false, 3);
        assertEquals(1.0, fast.carrySpeed(3), 1e-9, "nie schneller als normal");
    }

    @Test
    void intrudersAreCaughtOnlyOnTheOwnHalf() {
        // Blau fängt Rot auf der blauen Hälfte ab, nicht auf der roten.
        assertTrue(Rules.mayTag(Side.BLUE, Side.RED, Side.BLUE, false, false, false));
        assertFalse(Rules.mayTag(Side.BLUE, Side.RED, Side.RED, false, false, false));
        // Niemand aus dem eigenen Dorf.
        assertFalse(Rules.mayTag(Side.BLUE, Side.BLUE, Side.BLUE, true, false, false));
    }

    @Test
    void carriersAreCaughtEverywhere() {
        assertTrue(Rules.mayTag(Side.BLUE, Side.RED, Side.RED, true, false, false), "auch auf ihrer eigenen Hälfte");
        assertTrue(Rules.mayTag(Side.RED, Side.BLUE, Side.BLUE, true, false, false));
    }

    @Test
    void restingPeopleNeitherCatchNorGetCaught() {
        assertFalse(Rules.mayTag(Side.BLUE, Side.RED, Side.BLUE, false, true, false));
        assertFalse(Rules.mayTag(Side.BLUE, Side.RED, Side.BLUE, true, false, true));
    }

    @Test
    void ratesAreCapped() {
        assertEquals(0, Rules.liftRate(0));
        assertEquals(2, Rules.liftRate(2));
        assertEquals(Rules.MAX_LIFTERS, Rules.liftRate(9));
        assertEquals(1, Rules.returnRate(0));
        assertEquals(2, Rules.returnRate(1));
        assertEquals(3, Rules.returnRate(7));
    }
}
