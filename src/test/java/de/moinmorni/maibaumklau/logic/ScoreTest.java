package de.moinmorni.maibaumklau.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Punkte, Gewinner nach Zeit, Ausgang der Runde, Wartezeiten der Hilfen. */
class ScoreTest {

    @Test
    void liftPointsAreShared() {
        assertArrayEquals(new int[] {10}, Score.liftShares(1));
        assertArrayEquals(new int[] {5, 5}, Score.liftShares(2));
        assertArrayEquals(new int[] {4, 3, 3}, Score.liftShares(3));
        for (int n = 1; n <= 8; n++) {
            assertEquals(Score.LIFT, Arrays.stream(Score.liftShares(n)).sum(), n + " Leute");
        }
        assertEquals(0, Score.liftShares(0).length);
    }

    @Test
    void stepsCountNewTenths() {
        assertEquals(0, Score.steps(0.3, 0.3));
        assertEquals(0, Score.steps(0.5, 0.4));
        assertEquals(1, Score.steps(0.29, 0.31));
        assertEquals(10, Score.steps(0, 1));
        assertEquals(1, Score.steps(0.0, 0.1), "genau ein Zehntel zählt");
    }

    @Test
    void savingAPoleIsWorthMoreThanATag() {
        assertTrue(Score.SAVE > Score.TAG);
        assertTrue(Score.CAPTURE > Score.LIFT);
    }

    @Test
    void outcomes() {
        Outcome o = Outcome.byPoints(12, 7);
        assertEquals(Side.BLUE, o.winner());
        assertEquals("points", o.key());
        assertFalse(o.ransom());
        Outcome d = Outcome.byPoints(5, 5);
        assertTrue(d.draw());
        assertNull(d.winner());
        assertEquals("draw", d.key());
        Outcome c = Outcome.capture(Side.RED);
        assertTrue(c.ransom(), "geklauter Baum wird ausgelöst");
        assertEquals("capture", c.key());
        assertEquals(Side.BLUE, Outcome.walkover(Side.BLUE).winner());
        assertEquals("walkover", Outcome.walkover(Side.BLUE).key());
        assertEquals(1, Score.byPoints(0, 3));
        assertEquals(-1, Score.byPoints(0, 0));
    }

    @Test
    void sidesKnowEachOther() {
        assertEquals(Side.RED, Side.BLUE.other());
        assertEquals(Side.BLUE, Side.of(0));
        assertEquals(1, Side.RED.index());
    }

    @Test
    void cooldownsCountInTicks() {
        Cooldowns c = new Cooldowns();
        UUID u = new UUID(0, 1);
        assertTrue(c.ready(u, Help.HAY, 100));
        c.use(u, Help.HAY, 100, 240);
        assertFalse(c.ready(u, Help.HAY, 101));
        assertTrue(c.ready(u, Help.SNACK, 101), "jede Hilfe für sich");
        assertEquals(12, c.secondsLeft(u, Help.HAY, 100));
        assertEquals(1, c.secondsLeft(u, Help.HAY, 339));
        assertTrue(c.ready(u, Help.HAY, 340));
    }

    @Test
    void onlyStickAndCompassWorkWhileCarrying() {
        for (Help h : Help.values()) {
            assertEquals(h == Help.STICK || h == Help.COMPASS, h.whileCarrying(), h.id());
            assertEquals(h, Help.byId(h.id()));
        }
    }
}
