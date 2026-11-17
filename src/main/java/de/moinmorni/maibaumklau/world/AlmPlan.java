package de.moinmorni.maibaumklau.world;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.List;
import java.util.Random;

/**
 * Karte „Almdörfer mit Hängebrücke“ (eigener Entwurf): zwei Almen auf hohen Felsen, dazwischen eine tiefe Schlucht mit
 * Wildbach und Wasserfall. Im Westen liegt Hinteralm (Blau), im Osten Vorderalm (Rot). Hinüber geht es nur über die
 * schwankende Hängebrücke in der Mitte, einen schmalen Baumstamm im Norden und einen Felsbogen im Süden. Wer in die
 * Schlucht fällt, landet wieder im eigenen Dorf – und ein getragener Baum fällt zurück. In jedem Dorf wirft ein
 * Heuwagen einen auf den Aussichtsfels.
 */
final class AlmPlan {

    static final int POLE_X = 44;
    static final int HALF_X = 63;
    static final int HALF_Z = 33;
    /** Schlucht: bis hierher (|x|) reicht die Leere, dahinter beginnt der Fels. */
    static final int GORGE = 9;
    static final int FLOOR = -26;
    static final int VOID_Y = -10;
    /** Übergänge (z): Baumstamm, Hängebrücke, Felsbogen. */
    static final int LOG_Z = -26;
    static final int BRIDGE_Z = 0;
    static final int ARCH_Z = 26;
    /** Halbe Länge der Hängebrücke (sie hängt in der Mitte zwei Blöcke durch). */
    static final int BRIDGE_HALF = 10;
    static final double SAG = 2.0;

    private AlmPlan() {
    }

    static MapLayout layout() {
        int[] blue = Village.lookoutPad(-POLE_X, 0, 1);
        int[] red = Village.lookoutPad(POLE_X, 0, -1);
        List<MapLayout.Pad> pads = List.of(new MapLayout.Pad(blue[0], blue[1], 0, 1.0, 0.3),
                new MapLayout.Pad(red[0], red[1], 0, 1.0, 0.3));
        return new MapLayout(new int[][] {{-POLE_X, 0}, {POLE_X, 0}},
                new double[][] {Village.spawn(-POLE_X, 0, 1), Village.spawn(POLE_X, 0, -1)}, Village.SQUARE, VOID_Y,
                HALF_X, HALF_Z, pads, List.of(new int[] {0, LOG_Z}, new int[] {0, BRIDGE_Z}, new int[] {0, ARCH_Z}));
    }

    /** Oberkante der Hängebrücke bei x (in halben Blöcken genau): 1 = Wiese, in der Mitte 1 − SAG. */
    static double deck(int x) {
        double f = Math.min(1, Math.abs(x + 0.5) / (BRIDGE_HALF + 0.5));
        double h = 1 - SAG * (1 - f * f);
        return Math.round(h * 2) / 2.0;
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(2604);
        plateaus(p, r);
        gorge(p, r);
        hangingBridge(p);
        logBridge(p);
        arch(p, r);
        waterfall(p);
        Village.build(p, -POLE_X, 0, 1, Side.BLUE, Village.ALM, r);
        Village.build(p, POLE_X, 0, -1, Side.RED, Village.ALM, r);
        for (MapLayout.Pad pad : layout().pads()) {
            p.set(pad.x(), 0, pad.z(), "hay_block[axis=y]");
            p.clear(pad.x(), 1, pad.z());
            p.set(pad.x() + 1, 0, pad.z(), "hay_block[axis=z]");
            p.set(pad.x() - 1, 0, pad.z(), "hay_block[axis=z]");
        }
        for (int s : new int[] {-1, 1}) {
            paths(p, s, r);
            forest(p, s, r);
        }
        Decor.border(p, HALF_X, HALF_Z, 8, "spruce_fence");
        for (int i = 0; i < 14; i++) {
            Decor.cloud(p, -90 + r.nextInt(180), -38 - r.nextInt(8), -70 + r.nextInt(140), 3 + r.nextInt(4), r);
        }
        Decor.mountainRing(p, 115, 95, -20, r);
        p.connect();
        return p;
    }

    /** Schluchtrand bei z: leicht unregelmäßig, nie näher als {@link #GORGE}. */
    static int rim(int z, int side) {
        return GORGE + (int) Math.floor(Math.abs(Math.sin(z * 0.7 + side * 1.3)) * 1.6);
    }

    /** Zwei Almen: oben Wiese, darunter Erde und Fels, zur Schlucht hin senkrecht bis zum Grund. */
    private static void plateaus(Plan p, Random r) {
        int rx = 70;
        int rz = 40;
        for (int x = -rx - 2; x <= rx + 2; x++) {
            for (int z = -rz - 2; z <= rz + 2; z++) {
                int side = x < 0 ? -1 : 1;
                if (Math.abs(x) < rim(z, side)) {
                    continue;
                }
                double nx = Math.abs(x) / (double) (rx + 2);
                double nz = Math.abs(z) / (double) (rz + 2);
                double rr = Math.pow(Math.pow(nx, 4) + Math.pow(nz, 4), 0.25);
                if (rr > 1) {
                    continue;
                }
                int deep = Math.abs(x) <= 22 ? -FLOOR + 4 : 6 + (int) Math.round(22 * Math.sqrt(Math.max(0, 1 - rr * rr)));
                p.set(x, 0, z, "grass_block[snowy=false]");
                for (int d = 1; d <= deep; d++) {
                    String block;
                    if (d <= 2) {
                        block = "dirt";
                    } else if (Math.abs(x) == rim(z, side) && d < 8 && r.nextInt(3) == 0) {
                        block = Decor.pick(r, "mossy_cobblestone", "moss_block");
                    } else {
                        block = Decor.rock(r, d > 10 ? 10 : 0);
                    }
                    p.set(x, -d, z, block);
                }
                // Ranken und Moos an der Schluchtkante
                if (Math.abs(x) == rim(z, side) && r.nextInt(4) == 0) {
                    p.set(x - side, -1 - r.nextInt(3), z, "vine[" + (side < 0 ? "west" : "east") + "=true]");
                }
            }
        }
    }

    /** Grund der Schlucht mit Wildbach. */
    private static void gorge(Plan p, Random r) {
        for (int x = -GORGE - 1; x <= GORGE + 1; x++) {
            for (int z = -HALF_Z - 6; z <= HALF_Z + 6; z++) {
                for (int y = FLOOR - 4; y < FLOOR; y++) {
                    p.set(x, y, z, Decor.rock(r, 10));
                }
                if (Math.abs(x) <= 2) {
                    p.set(x, FLOOR, z, "water");
                    p.set(x, FLOOR - 1, z, Decor.pick(r, "gravel", "sand"));
                } else if (Math.abs(x) < GORGE) {
                    p.set(x, FLOOR, z, Decor.pick(r, "gravel", "stone", "cobblestone", "mossy_cobblestone"));
                }
            }
        }
    }

    /** Hängebrücke: Bretter (drei breit), hängt in der Mitte durch, Geländer, Pfosten mit Laternen. */
    private static void hangingBridge(Plan p) {
        int z = BRIDGE_Z;
        for (int x = -BRIDGE_HALF; x <= BRIDGE_HALF; x++) {
            double top = deck(x);
            int y = (int) Math.floor(top - 0.01);
            boolean half = top - Math.floor(top) > 0.01;
            for (int dz = -1; dz <= 1; dz++) {
                for (int yy = y; yy <= y + 3; yy++) {
                    p.clear(x, yy, z + dz);
                }
                if (half) {
                    p.set(x, (int) Math.floor(top), z + dz, "spruce_slab[type=bottom]");
                } else {
                    p.set(x, y, z + dz, Math.floorMod(x, 3) == 0 ? "stripped_spruce_wood[axis=x]" : "spruce_planks");
                }
            }
            int rail = (int) Math.floor(top);
            p.set(x, rail, z - 2, "spruce_fence");
            p.set(x, rail, z + 2, "spruce_fence");
            p.set(x, rail - 1, z - 2, "iron_chain[axis=x]");
            p.set(x, rail - 1, z + 2, "iron_chain[axis=x]");
        }
        for (int sx : new int[] {-BRIDGE_HALF - 1, BRIDGE_HALF + 1}) {
            for (int dz : new int[] {-2, 2}) {
                for (int y = 1; y <= 4; y++) {
                    p.set(sx, y, z + dz, "spruce_log[axis=y]");
                }
                p.set(sx, 5, z + dz, "lantern[hanging=false,waterlogged=false]");
            }
            p.set(sx, 0, z, "spruce_planks");
            p.set(sx, 0, z - 1, "spruce_planks");
            p.set(sx, 0, z + 1, "spruce_planks");
        }
    }

    /** Schmaler Baumstamm über die Schlucht (nur einer breit). */
    private static void logBridge(Plan p) {
        for (int x = -GORGE - 2; x <= GORGE + 2; x++) {
            p.set(x, 0, LOG_Z, "spruce_log[axis=x]");
            p.clear(x, 1, LOG_Z);
        }
        p.set(-3, 1, LOG_Z, "spruce_button[face=floor,facing=north]");
        p.set(4, 0, LOG_Z + 1, "spruce_leaves[persistent=true]");
        p.set(-5, 0, LOG_Z - 1, "spruce_leaves[persistent=true]");
    }

    /** Natürlicher Felsbogen: oben drei breit und eben, darunter wird er zur Mitte hin dünner. */
    private static void arch(Plan p, Random r) {
        for (int x = -GORGE - 2; x <= GORGE + 2; x++) {
            double f = Math.abs(x) / (double) GORGE;
            int bottom = -1 - (int) Math.round(12 * f * f);
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = bottom; y <= 0; y++) {
                    p.set(x, y, ARCH_Z + dz, y == 0 ? Decor.pick(r, "mossy_stone_bricks", "stone", "andesite", "moss_block")
                            : Decor.rock(r, 0));
                }
                p.clear(x, 1, ARCH_Z + dz);
            }
            if (r.nextInt(3) == 0) {
                p.set(x, -1, ARCH_Z + 2, "vine[north=true]");
            }
        }
    }

    /** Wasserfall von der Westalm in die Schlucht, mit kleinem Teich oben. */
    private static void waterfall(Plan p) {
        int z = -14;
        for (int x = -GORGE - 4; x <= -GORGE; x++) {
            for (int dz = 0; dz <= 1; dz++) {
                p.set(x, 0, z + dz, "water");
                p.set(x, -1, z + dz, "stone");
            }
        }
        for (int dz = 0; dz <= 1; dz++) {
            for (int y = FLOOR + 1; y <= 0; y++) {
                p.set(-GORGE + 1, y, z + dz, "water");
            }
        }
    }

    /** Wege von jedem Dorf zu den drei Übergängen. */
    private static void paths(Plan p, int s, Random r) {
        int dorf = s * (POLE_X - 8);
        for (int z : new int[] {LOG_Z, BRIDGE_Z, ARCH_Z}) {
            int x1 = dorf;
            int z1 = z / 3;
            int x2 = s * (GORGE + 3);
            int steps = (int) Math.ceil(Math.hypot(x2 - x1, z - z1));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                int x = (int) Math.round(x1 + (x2 - x1) * t);
                int zz = (int) Math.round(z1 + (z - z1) * t);
                for (int dz = -1; dz <= 1; dz++) {
                    String g = p.get(x, 0, zz + dz);
                    if (g != null && g.startsWith("grass_block")) {
                        p.set(x, 0, zz + dz, dz == 0 || r.nextInt(3) > 0 ? "dirt_path" : "coarse_dirt");
                    }
                }
            }
        }
    }

    /** Fichtenwald zwischen Dorf und Schlucht (Wege, Übergänge und Dorf bleiben frei), Felsbrocken. */
    private static void forest(Plan p, int s, Random r) {
        for (int i = 0; i < 70; i++) {
            int x = s * (GORGE + 4 + r.nextInt(46));
            int z = -HALF_Z + 2 + r.nextInt(2 * HALF_Z - 3);
            boolean nearPath = Math.abs(z) <= 4 || Math.abs(z - LOG_Z) <= 4 || Math.abs(z - ARCH_Z) <= 4;
            boolean inVillage = Math.abs(x) >= POLE_X - 16 && Math.abs(z) <= 27;
            if (nearPath || inVillage || !"grass_block[snowy=false]".equals(p.get(x, 0, z)) || p.get(x, 1, z) != null) {
                continue;
            }
            if (r.nextInt(5) == 0) {
                for (int dy = 1; dy <= 1 + r.nextInt(2); dy++) {
                    p.setIfEmpty(x, dy, z, Decor.pick(r, "mossy_cobblestone", "stone", "andesite"));
                }
            } else {
                Decor.spruce(p, x, 1, z, 6 + r.nextInt(4), r);
            }
        }
        Decor.flowers(p, s > 0 ? GORGE + 2 : -60, -31, s > 0 ? 60 : -GORGE - 2, 31, 8, r, (x, z) -> false);
    }
}
