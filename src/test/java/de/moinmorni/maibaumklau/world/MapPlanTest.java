package de.moinmorni.maibaumklau.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Baupläne ohne Server: Jede Karte ist spielbar (man kommt zu Fuß vom eigenen Wirtshaus zum fremden Maibaum), die
 * Maibäume lassen sich sauber wegnehmen, Startplätze sind frei, Heuwagen stehen, es gibt nur Blöcke von 26.x.
 */
class MapPlanTest {

    /** Blocknamen, die es in 26.x nicht mehr gibt (ein Stück des Bauplans würde still fehlen). */
    private static final Set<String> OLD = Set.of("chain", "grass", "grass_path", "sign", "wall_sign");

    @Test
    void onlyBlocksThatExist() {
        Set<String> unknown = new TreeSet<>();
        for (Plan p : new Plan[] {HallPlan.build(), MapType.BACHDOERFER.plan(), MapType.ALMDOERFER.plan()}) {
            p.forEach((k, s) -> {
                String base = Blocks.base(s);
                if (OLD.contains(base) || Material.matchMaterial(base) == null) {
                    unknown.add(base);
                }
            });
        }
        assertEquals(Set.of(), unknown);
    }

    @Test
    void polesStandOnTheirSquareAndCanBeTakenAwayCleanly() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            for (Side s : Side.values()) {
                Plan pole = m.polePlan(s);
                Plan alone = new Plan();
                Maypole.build(alone, l.poleX(s), l.poleZ(s), s);
                int[] b = Maypole.removable(l.poleX(s), l.poleZ(s));
                Plan expected = alone.crop(b[0], b[1], b[2], b[3], b[4], b[5]);
                assertEquals(expected.size(), pole.size(), m.id() + " " + s + ": im Baum steckt etwas Fremdes");
                expected.forEachAt((x, y, z, spec) -> assertEquals(spec, pole.get(x, y, z), m.id() + " " + x + " " + y + " " + z));
                assertTrue(pole.size() > Maypole.LENGTH, m.id() + ": Baum zu klein");
                assertEquals(s, l.half(l.poleX(s), l.poleZ(s)), "Baum auf der eigenen Hälfte");
                String ground = m.playPlan().get(l.poleX(s) + 4, 0, l.poleZ(s));
                assertNotNull(ground, m.id() + ": kein Dorfplatz");
                assertFalse(ground.startsWith("grass"), m.id() + ": Dorfplatz ist gepflastert");
            }
            assertTrue(Math.abs(l.poleX(Side.BLUE) - l.poleX(Side.RED)) > 60, m.id() + ": Dörfer zu nah");
        }
    }

    @Test
    void startSpotsAreFreeAndAtHome() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            Plan p = m.playPlan();
            for (Side s : Side.values()) {
                int x = (int) Math.floor(l.spawnX(s));
                int z = (int) Math.floor(l.spawnZ(s));
                assertTrue(p.standable(x, 1, z), m.id() + " " + s + ": Startplatz nicht frei");
                assertEquals(s, l.half(x, z));
                assertFalse(l.onSquare(s.other(), x, z));
                double toPole = l.toPole(s, l.spawnX(s), l.spawnZ(s));
                assertTrue(toPole > 6 && toPole < 12, m.id() + ": Startplatz " + toPole + " vom eigenen Baum");
                // Bis zu acht Leute: zwei Reihen zu vier
                int dir = s == Side.BLUE ? 1 : -1;
                for (int i = 0; i < 8; i++) {
                    int sx = (int) Math.floor(l.spawnX(s) + dir * (i / 4) * 1.5);
                    int sz = (int) Math.floor(l.spawnZ(s) + ((i % 4) - 1.5) * 1.5);
                    assertTrue(p.standable(sx, 1, sz), m.id() + " " + s + ": Startplatz " + i + " belegt");
                }
            }
        }
    }

    @Test
    void hayWagonsWork() {
        for (MapType m : MapType.values()) {
            Plan p = m.playPlan();
            assertFalse(m.layout().pads().isEmpty(), m.id());
            for (MapLayout.Pad pad : m.layout().pads()) {
                assertEquals("hay_block[axis=y]", p.get(pad.x(), 0, pad.z()), m.id() + ": Heuballen fehlt");
                assertTrue(p.standable(pad.x(), 1, pad.z()), m.id() + ": über dem Heuwagen ist kein Platz");
                assertTrue(pad.vy() > 0.5, m.id());
            }
        }
    }

    /**
     * Zu Fuß von jedem Wirtshaus zum fremden Dorfplatz: höchstens einen Block hoch (springen), höchstens drei hinunter.
     * Wasser ist begehbar (Waten), Heuwagen braucht es nicht.
     */
    @Test
    void everyVillageCanReachTheOther() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            Plan p = m.playPlan();
            for (Side s : Side.values()) {
                Set<Long> seen = walk(p, l, (int) Math.floor(l.spawnX(s)), 1, (int) Math.floor(l.spawnZ(s)));
                boolean reached = false;
                for (long k : seen) {
                    if (l.onSquare(s.other(), Plan.keyX(k) + 0.5, Plan.keyZ(k) + 0.5)) {
                        reached = true;
                        break;
                    }
                }
                assertTrue(reached, m.id() + ": von " + s + " kommt man nicht zum anderen Dorf (" + seen.size() + " Stellen)");
            }
        }
    }

    /** Auf der Alm geht es nur über die drei Übergänge: dazwischen ist die Schlucht. */
    @Test
    void theGorgeOnlyHasThreeCrossings() {
        MapLayout l = MapType.ALMDOERFER.layout();
        Plan p = MapType.ALMDOERFER.playPlan();
        for (int z = -l.halfZ() + 1; z < l.halfZ(); z++) {
            // Auf dem Geländer der Hängebrücke (z ± 2) könnte man stehen – es gehört zur Brücke.
            boolean crossing = Math.abs(z - AlmPlan.BRIDGE_Z) <= 2 || Math.abs(z - AlmPlan.LOG_Z) <= 0
                    || Math.abs(z - AlmPlan.ARCH_Z) <= 1;
            boolean ground = false;
            for (int y = l.voidY(); y <= 3; y++) {
                ground |= p.standable(0, y, z);
            }
            assertEquals(crossing, ground, "z = " + z);
        }
        // Die Hängebrücke hängt durch, aber nie mehr als einen halben Block je Schritt.
        for (int x = -AlmPlan.BRIDGE_HALF; x < AlmPlan.BRIDGE_HALF; x++) {
            assertTrue(Math.abs(AlmPlan.deck(x) - AlmPlan.deck(x + 1)) <= 0.5, "Stufe bei x = " + x);
        }
        assertTrue(AlmPlan.deck(0) < 0, "hängt in der Mitte durch");
        assertEquals(1.0, AlmPlan.deck(AlmPlan.BRIDGE_HALF), 1e-9);
    }

    /** Am Bach: drei Brücken mit Holz über dem Wasser, dazwischen Wasser. */
    @Test
    void theBrookHasBridges() {
        Plan p = MapType.BACHDOERFER.playPlan();
        for (int z : BachPlan.BRIDGES) {
            assertEquals("spruce_planks", p.get(BachPlan.creek(z), 0, z), "Brücke bei z = " + z);
        }
        assertEquals("water", p.get(BachPlan.creek(9), 0, 9));
        assertEquals("water", p.get(BachPlan.creek(9), -1, 9), "in der Mitte zwei tief");
    }

    @Test
    void theBorderKeepsEveryoneIn() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            Plan p = m.playPlan();
            for (int x = -l.halfX(); x <= l.halfX(); x += 7) {
                for (int y = 2; y <= 6; y++) {
                    assertEquals("barrier", p.get(x, y, l.halfZ()), m.id() + " Rand bei x = " + x);
                    assertEquals("barrier", p.get(x, y, -l.halfZ()), m.id() + " Rand bei x = " + x);
                }
            }
            int[] box = m.playBox();
            assertTrue(box[1] <= l.voidY() - 10 && box[4] >= Maypole.CROWN_TOP, m.id() + ": Wiederherstellung zu klein");
        }
    }

    @Test
    void hallHasRoomToStand() {
        Plan p = HallPlan.build();
        assertTrue(p.standable((int) Math.floor(HallPlan.SPAWN_X), 1, (int) Math.floor(HallPlan.SPAWN_Z)));
        for (int[] t : HallPlan.TABLES) {
            assertEquals("cartography_table", p.get(t[0], 2, t[1]));
        }
        assertNotNull(p.get(0, Maypole.TOP, 0), "Maibaum im Wirtsgarten");
    }

    /** Alle begehbaren Stellen, die man von (x, y, z) aus zu Fuß erreicht. */
    private static Set<Long> walk(Plan p, MapLayout l, int x0, int y0, int z0) {
        Set<Long> seen = new HashSet<>();
        Deque<int[]> todo = new ArrayDeque<>();
        todo.add(new int[] {x0, y0, z0});
        seen.add(Plan.key(x0, y0, z0));
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!todo.isEmpty()) {
            int[] c = todo.poll();
            for (int[] d : dirs) {
                int x = c[0] + d[0];
                int z = c[2] + d[1];
                if (Math.abs(x) >= l.halfX() || Math.abs(z) >= l.halfZ()) {
                    continue;
                }
                for (int dy = 1; dy >= -3; dy--) {
                    int y = c[1] + dy;
                    if (y < l.voidY()) {
                        break;
                    }
                    // Hochsteigen braucht Platz über dem Kopf an der alten Stelle.
                    if (dy == 1 && p.solid(c[0], c[1] + 2, c[2])) {
                        continue;
                    }
                    if (p.standable(x, y, z)) {
                        if (seen.add(Plan.key(x, y, z))) {
                            todo.add(new int[] {x, y, z});
                        }
                        break;
                    }
                    if (p.solid(x, y + 1, z) && dy <= 0) {
                        break;
                    }
                }
            }
        }
        return seen;
    }
}
