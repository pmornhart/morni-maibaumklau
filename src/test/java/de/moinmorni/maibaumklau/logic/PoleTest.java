package de.moinmorni.maibaumklau.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Der Zustand eines Maibaums: umlegen, Wache, tragen, ablegen, zurückholen, Fortschritt. */
class PoleTest {

    private static final Rules RULES = new Rules(8, 20, 0.55, 0.12, 0.9, false, 3);

    /** So viele Sekunden (in Viertelsekunden-Schritten), bis der Baum fällt – oder -1. */
    private static double secondsToLift(int lifters, int guards) {
        Pole p = new Pole(Side.RED);
        for (int i = 1; i <= 400; i++) {
            if (p.liftTick(lifters, guards, 0.25, RULES)) {
                return i * 0.25;
            }
        }
        return -1;
    }

    @Test
    void moreThievesFellFaster() {
        assertEquals(8.0, secondsToLift(1, 0), 0.26);
        assertEquals(4.0, secondsToLift(2, 0), 0.26);
        assertEquals(8 / 3.0, secondsToLift(3, 0), 0.26);
        assertEquals(secondsToLift(3, 0), secondsToLift(5, 0), 1e-9, "ab drei nicht schneller");
    }

    @Test
    void everyGuardHoldsOneThiefBack() {
        assertEquals(-1, secondsToLift(1, 1), "gleich viele: nichts geht voran");
        assertEquals(-1, secondsToLift(2, 3));
        assertEquals(secondsToLift(1, 0), secondsToLift(2, 1), 1e-9, "zwei gegen eine Wache: wie einer allein");
        Pole p = new Pole(Side.BLUE);
        p.liftTick(2, 2, 0.25, RULES);
        assertEquals(Pole.Lift.GUARDED, p.liftState());
        assertEquals(0, p.lift());
    }

    @Test
    void progressSlowlyFadesWhenNobodyIsThere() {
        Pole p = new Pole(Side.BLUE);
        for (int i = 0; i < 16; i++) {
            p.liftTick(1, 0, 0.25, RULES);
        }
        assertEquals(0.5, p.lift(), 1e-9);
        assertEquals(Pole.Lift.WORKING, p.liftState());
        p.liftTick(0, 0, 1, RULES);
        assertEquals(Pole.Lift.IDLE, p.liftState());
        assertEquals(0.5 - 0.5 / 8, p.lift(), 1e-9, "halb so schnell zurück, wie es entsteht");
        p.liftTick(0, 0, 100, RULES);
        assertEquals(0, p.lift());
    }

    @Test
    void felledPoleIsCarriedAndDropsWhenCaught() {
        Pole p = new Pole(Side.RED);
        while (!p.liftTick(3, 0, 0.25, RULES)) {
            assertEquals(Pole.State.HOME, p.state());
        }
        assertEquals(Pole.State.CARRIED, p.state());
        assertFalse(p.liftTick(3, 0, 0.25, RULES), "getragen wird nicht noch einmal umgelegt");
        p.drop(RULES);
        assertEquals(Pole.State.DROPPED, p.state());
        assertEquals(20, p.dropLeft(), 1e-9);
        assertFalse(p.canPickUp(), "nicht sofort wieder aufheben");
        assertFalse(p.dropTick(0, 1.0));
        assertFalse(p.canPickUp(), "nach einer Sekunde noch nicht");
        assertFalse(p.dropTick(0, 1.0));
        assertTrue(p.canPickUp(), "nach anderthalb Sekunden wieder");
        p.pickUp();
        assertEquals(Pole.State.CARRIED, p.state());
    }

    @Test
    void droppedPoleReturnsFasterWithGuards() {
        assertEquals(20.0, secondsToReturn(0), 0.26);
        assertEquals(10.0, secondsToReturn(1), 0.26);
        assertEquals(20 / 3.0, secondsToReturn(2), 0.26);
        assertEquals(secondsToReturn(2), secondsToReturn(6), 1e-9, "ab zwei Wachen nicht schneller");
    }

    private static double secondsToReturn(int guards) {
        Pole p = new Pole(Side.RED);
        p.drop(RULES);
        for (int i = 1; i <= 400; i++) {
            if (p.dropTick(guards, 0.25)) {
                assertEquals(Pole.State.HOME, p.state());
                return i * 0.25;
            }
        }
        return -1;
    }

    @Test
    void progressHomeGivesPointsForNewTenthsOnly() {
        Pole p = new Pole(Side.RED);
        assertEquals(0, p.advance(0.05));
        assertEquals(2, p.advance(0.27), "0,05 → 0,27: zwei neue Zehntel");
        assertEquals(0, p.advance(0.1), "zurücklaufen bringt nichts");
        assertEquals(0, p.advance(0.29), "wieder vor, aber kein neues Zehntel");
        assertEquals(1, p.advance(0.3));
        assertEquals(7, p.advance(1.2), "höchstens bis ganz heim");
        assertEquals(1.0, p.best(), 1e-9);
    }

    @Test
    void progressIsMeasuredByDistanceToTheTarget() {
        assertEquals(0, Pole.progress(44, 0, -44, 0, 44, 0), 1e-9);
        assertEquals(0.5, Pole.progress(44, 0, -44, 0, 0, 0), 1e-9);
        assertEquals(1, Pole.progress(44, 0, -44, 0, -44, 0), 1e-9);
        assertEquals(0, Pole.progress(44, 0, -44, 0, 90, 0), 1e-9, "Umwege nach hinten zählen nicht negativ");
        assertTrue(Pole.progress(44, 0, -44, 0, 0, 30) < 0.5, "seitlich ausweichen ist weniger weit");
    }
}
