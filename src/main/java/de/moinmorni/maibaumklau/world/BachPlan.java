package de.moinmorni.maibaumklau.world;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.List;
import java.util.Random;

/**
 * Karte „Zwei Dörfer am Bach“ (eigener Entwurf): eine Wiese mitten in den Bergen, in der Mitte schlängelt sich ein Bach
 * von Nord nach Süd. Im Westen liegt Oberbach (Blau), im Osten Unterbach (Rot). Über den Bach führen drei Holzbrücken
 * (die mittlere mit Dach), an zwei Stellen werfen Heuwagen einen über das Wasser – oder man watet hindurch (langsam).
 * Zwischen Dorf und Bach: Felder, Heuschober, Obstbäume, eine Mühle und ein Wegkreuz.
 */
final class BachPlan {

    static final int POLE_X = 44;
    static final int HALF_X = 63;
    static final int HALF_Z = 33;
    /** Brücken über den Bach (z). */
    static final int[] BRIDGES = {-18, 0, 18};
    /** Heuwagen an beiden Ufern (z). Dort fließt der Bach genau in der Mitte. */
    static final int[] PAD_Z = {-24, 24};
    static final int PAD_X = 6;
    static final double PAD_SPEED = 1.35;
    static final double PAD_UP = 0.65;
    static final int VOID_Y = -8;

    private BachPlan() {
    }

    /** Mitte des Bachs bei z (schlängelt sich leicht). */
    static int creek(int z) {
        return (int) Math.round(2.5 * Math.sin(z * Math.PI / 24));
    }

    static MapLayout layout() {
        List<MapLayout.Pad> pads = new java.util.ArrayList<>();
        for (int z : PAD_Z) {
            pads.add(new MapLayout.Pad(-PAD_X, z, PAD_SPEED, PAD_UP, 0));
            pads.add(new MapLayout.Pad(PAD_X, z, -PAD_SPEED, PAD_UP, 0));
        }
        List<int[]> crossings = new java.util.ArrayList<>();
        for (int z : BRIDGES) {
            crossings.add(new int[] {creek(z), z});
        }
        return new MapLayout(new int[][] {{-POLE_X, 0}, {POLE_X, 0}},
                new double[][] {Village.spawn(-POLE_X, 0, 1), Village.spawn(POLE_X, 0, -1)}, Village.SQUARE, VOID_Y,
                HALF_X, HALF_Z, List.copyOf(pads), List.copyOf(crossings));
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(1505);
        Decor.island(p, 70, 40, 10, r);
        creekBed(p, r);
        for (int z : BRIDGES) {
            bridge(p, z, z == 0);
        }
        for (MapLayout.Pad pad : layout().pads()) {
            hayWagon(p, pad);
        }
        Village.build(p, -POLE_X, 0, 1, Side.BLUE, Village.TAL, r);
        Village.build(p, POLE_X, 0, -1, Side.RED, Village.TAL, r);
        for (int s : new int[] {-1, 1}) {
            meadow(p, s, r);
        }
        mill(p, r);
        wayCross(p, 0, -9);
        Decor.border(p, HALF_X, HALF_Z, 8, "spruce_fence");
        for (int i = 0; i < 16; i++) {
            Decor.cloud(p, -90 + r.nextInt(180), -14 - r.nextInt(6), -60 + r.nextInt(120), 3 + r.nextInt(4), r);
        }
        Decor.mountainRing(p, 125, 100, -12, r);
        p.connect();
        return p;
    }

    /** Der Bach: in der Mitte zwei Blöcke tief, am Rand einer, an den Ufern Schilf und Steine. */
    private static void creekBed(Plan p, Random r) {
        for (int z = -42; z <= 42; z++) {
            int c = creek(z);
            for (int dx = -3; dx <= 3; dx++) {
                int x = c + dx;
                if (p.get(x, 0, z) == null) {
                    continue;
                }
                int a = Math.abs(dx);
                if (a <= 1) {
                    p.set(x, 0, z, "water");
                    p.set(x, -1, z, "water");
                    p.set(x, -2, z, Decor.pick(r, "gravel", "sand", "clay", "gravel"));
                } else if (a == 2) {
                    p.set(x, 0, z, "water");
                    p.set(x, -1, z, Decor.pick(r, "sand", "gravel", "mud"));
                    if (r.nextInt(14) == 0) {
                        p.set(x, 1, z, "lily_pad");
                    }
                } else {
                    p.set(x, 0, z, Decor.pick(r, "grass_block[snowy=false]", "grass_block[snowy=false]", "sand", "gravel"));
                    int roll = r.nextInt(10);
                    if (roll < 3) {
                        p.set(x, 1, z, "sugar_cane");
                        if (roll == 0) {
                            p.set(x, 2, z, "sugar_cane");
                        }
                    } else if (roll == 3) {
                        p.set(x, 1, z, "mossy_cobblestone_slab[type=bottom]");
                    }
                }
            }
        }
    }

    /** Holzbrücke über den Bach (drei breit), mit Geländer; die mittlere hat ein Dach. */
    private static void bridge(Plan p, int z, boolean roofed) {
        int c = creek(z);
        for (int x = c - 4; x <= c + 4; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(x, 0, z + dz, "spruce_planks");
                p.clear(x, 1, z + dz);
                p.clear(x, 2, z + dz);
            }
            if (Math.abs(x - c) <= 3) {
                p.set(x, 1, z - 2, "spruce_fence");
                p.set(x, 1, z + 2, "spruce_fence");
                p.set(x, 0, z - 2, "spruce_planks");
                p.set(x, 0, z + 2, "spruce_planks");
            }
        }
        for (int[] e : new int[][] {{c - 4, z - 2}, {c - 4, z + 2}, {c + 4, z - 2}, {c + 4, z + 2}}) {
            Decor.lanternPost(p, e[0], 1, e[1], "spruce_fence");
        }
        if (roofed) {
            for (int[] e : new int[][] {{c - 3, z - 2}, {c - 3, z + 2}, {c + 3, z - 2}, {c + 3, z + 2}}) {
                for (int y = 1; y <= 3; y++) {
                    p.set(e[0], y, e[1], "stripped_spruce_log[axis=y]");
                }
            }
            Kit.roof(p, c - 3, z - 2, c + 3, z + 2, 4, "spruce", false);
            p.set(c, 3, z, "lantern[hanging=true,waterlogged=false]");
        }
    }

    /** Heuwagen am Ufer: Heuballen im Boden, Räder (Zaunpfosten) und Deichsel daneben. */
    private static void hayWagon(Plan p, MapLayout.Pad pad) {
        p.set(pad.x(), 0, pad.z(), "hay_block[axis=y]");
        p.clear(pad.x(), 1, pad.z());
        p.clear(pad.x(), 2, pad.z());
        int back = pad.vx() > 0 ? -1 : 1;
        for (int dz : new int[] {-1, 1}) {
            p.set(pad.x(), 0, pad.z() + dz, "hay_block[axis=x]");
            p.set(pad.x() + back, 1, pad.z() + dz * 2, "spruce_trapdoor[facing=" + (dz < 0 ? "north" : "south")
                    + ",half=bottom,open=true]");
        }
        p.set(pad.x() + back, 1, pad.z(), "hay_block[axis=x]");
        p.set(pad.x() + back * 2, 1, pad.z(), "spruce_fence");
    }

    /** Zwischen Dorf und Bach: Wege zu den Brücken, Felder, Heuschober, Bäume, Blumen. */
    private static void meadow(Plan p, int s, Random r) {
        int dorf = s * (POLE_X - 8);
        for (int z : BRIDGES) {
            line(p, dorf, z == 0 ? 0 : z / 3, creek(z) + s * 5, z, r);
        }
        line(p, dorf, 0, s * 16, 0, r);
        // Felder (Weizen) als niedrige Deckung, ein Feld je Seite oben und unten
        for (int[] f : new int[][] {{14, 22, -30, -24}, {14, 22, 24, 30}}) {
            for (int a = f[0]; a <= f[1]; a++) {
                for (int z = f[2]; z <= f[3]; z++) {
                    int x = s * a;
                    if (Math.floorMod(z, 4) == 0) {
                        p.set(x, 0, z, "water");
                        continue;
                    }
                    p.set(x, 0, z, "farmland[moisture=7]");
                    p.set(x, 1, z, "wheat[age=7]");
                }
            }
        }
        // Heuschober (Deckung)
        for (int[] h : new int[][] {{26, -9}, {12, 11}, {24, 14}, {30, -16}}) {
            int x = s * h[0];
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    p.set(x + dx, 1, h[1] + dz, "hay_block[axis=y]");
                }
            }
            p.set(x, 2, h[1], "hay_block[axis=x]");
        }
        // Bäume und Büsche abseits der Wege
        Decor.greenery(p, s > 0 ? 8 : -30, -31, s > 0 ? 30 : -8, 31, 9, r,
                (x, z) -> Math.abs(z) <= 3 || Math.abs(Math.abs(z) - 18) <= 4 || Math.abs(Math.abs(z) - 24) <= 3);
        for (int i = 0; i < 6; i++) {
            int x = s * (9 + r.nextInt(20));
            int z = -30 + r.nextInt(61);
            if (Math.abs(z) > 5 && Math.abs(Math.abs(z) - 18) > 4 && Math.abs(Math.abs(z) - 24) > 4
                    && "grass_block[snowy=false]".equals(p.get(x, 0, z)) && p.get(x, 1, z) == null) {
                Decor.broadTree(p, x, 1, z, i % 2 == 0 ? "birch_log" : "oak_log",
                        i % 2 == 0 ? "birch_leaves" : "oak_leaves", 4, 2, r);
            }
        }
        Decor.flowers(p, s > 0 ? 6 : -32, -31, s > 0 ? 32 : -6, 31, 7, r, (x, z) -> false);
    }

    /** Pfad von (x1|z1) nach (x2|z2), drei breit. */
    private static void line(Plan p, int x1, int z1, int x2, int z2, Random r) {
        int steps = (int) Math.ceil(Math.hypot(x2 - x1, z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int dz = -1; dz <= 1; dz++) {
                String g = p.get(x, 0, z + dz);
                if (g != null && g.startsWith("grass_block")) {
                    p.set(x, 0, z + dz, dz == 0 || r.nextInt(3) > 0 ? "dirt_path" : "coarse_dirt");
                    String above = p.get(x, 1, z + dz);
                    if (above != null && Blocks.passable(above)) {
                        p.clear(x, 1, z + dz);
                    }
                }
            }
        }
    }

    /** Alte Mühle am oberen Bach (Ostufer) mit Mühlrad aus Holz. */
    private static void mill(Plan p, Random r) {
        int c = creek(-29);
        Kit.house(p, new Kit.House(c + 5, -32, c + 11, -27, 'w', -29, "cobblestone", "spruce", false, false,
                "spruce_planks"), r);
        int wx = c + 3;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -2; dz <= 2; dz++) {
                double d = Math.hypot(dy, dz);
                if (d > 1.6 && d <= 2.6) {
                    p.set(wx, 1 + dy, -29 + dz, "stripped_spruce_wood[axis=x]");
                }
            }
        }
        p.set(wx, 1, -29, "spruce_log[axis=x]");
    }

    /** Wegkreuz (Marterl) an der Mittelbrücke. */
    private static void wayCross(Plan p, int x, int z) {
        int c = creek(z) + 6 + x;
        p.set(c, 1, z, "stone_bricks");
        for (int y = 2; y <= 4; y++) {
            p.set(c, y, z, "dark_oak_fence");
        }
        p.set(c, 4, z - 1, "dark_oak_fence");
        p.set(c, 4, z + 1, "dark_oak_fence");
        p.set(c, 5, z, "dark_oak_slab[type=bottom]");
        p.set(c - 1, 1, z, "potted_red_tulip");
        p.set(c + 1, 1, z, "potted_cornflower");
    }
}
