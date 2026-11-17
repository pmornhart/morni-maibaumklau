package de.moinmorni.maibaumklau.world;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.Random;

/**
 * Ein Dorf (eigener Entwurf): Dorfplatz mit dem Maibaum in der Mitte, daneben das Wachhäusl der Maibaumwache, hinter
 * dem Platz das Wirtshaus (davor startet man), ringsum Häuser, Holzstöße, Heuballen und Gärten. Gebaut in eigenen
 * Achsen: {@code u} zeigt vom Maibaum zur Kartenmitte, {@code v} quer dazu (= z der Karte).
 */
final class Village {

    /** Radius des gepflasterten Dorfplatzes. */
    static final double SQUARE = 7.5;
    /** Mitte des Aussichtsfelsens (nur Alm), in eigenen Achsen. */
    static final int LOOKOUT_U = 13;
    static final int LOOKOUT_V = 18;
    /** Startplatz vor dem Wirtshaus: so weit hinter dem Maibaum (in u, Block). */
    static final int SPAWN_U = -8;

    /** Aussehen: Talhäuser (verputzt) oder Almhütten (Holz). */
    record Style(String plaster, String wood, String floor, boolean alm) {
    }

    static final Style TAL = new Style("white_terracotta", "spruce", "spruce_planks", false);
    static final Style ALM = new Style("stripped_spruce_wood[axis=y]", "spruce", "spruce_planks", true);

    private final Plan p;
    private final int cx;
    private final int cz;
    private final int dir;
    private final Side side;
    private final Style style;
    private final Random r;

    private Village(Plan p, int cx, int cz, int dir, Side side, Style style, Random r) {
        this.p = p;
        this.cx = cx;
        this.cz = cz;
        this.dir = dir;
        this.side = side;
        this.style = style;
        this.r = r;
    }

    /**
     * Dorf bauen.
     *
     * @param cx  Fuß des Maibaums (x)
     * @param cz  Fuß des Maibaums (z)
     * @param dir +1, wenn die Kartenmitte bei größerem x liegt (Dorf im Westen), sonst −1
     */
    static void build(Plan p, int cx, int cz, int dir, Side side, Style style, Random r) {
        new Village(p, cx, cz, dir, side, style, r).build();
    }

    private int x(double u) {
        return cx + (int) Math.round(dir * u);
    }

    private int z(double v) {
        return cz + (int) Math.round(v);
    }

    /** Himmelsrichtung für „zur Kartenmitte“ (+u) bzw. „weg davon“ (−u). */
    private String toward(boolean plusU) {
        return (dir > 0) == plusU ? "east" : "west";
    }

    private char doorToward(boolean plusU) {
        return (dir > 0) == plusU ? 'e' : 'w';
    }

    private void build() {
        square();
        Maypole.build(p, cx, cz, side);
        guardHut();
        inn();
        house(-6, 0, -21, -15, 's', true);
        house(-6, 0, 15, 21, 'n', true);
        house(4, 9, -20, -15, 's', false);
        house(4, 9, 15, 20, 'n', false);
        yards();
        if (style.alm()) {
            lookout();
        }
    }

    /** Dorfplatz: Pflaster mit Rauten in der Dorffarbe, Steinrand, vier Laternen, Bänke am Rand. */
    private void square() {
        String colour = side == Side.BLUE ? "light_blue_terracotta" : "red_terracotta";
        for (int u = -8; u <= 8; u++) {
            for (int v = -8; v <= 8; v++) {
                double d = Math.hypot(u, v);
                if (d > SQUARE) {
                    continue;
                }
                String block;
                if (d > 6.6) {
                    block = "stone_bricks";
                } else if (d > 2.2) {
                    block = Decor.lozenge(u, v, 2) ? "polished_andesite" : colour;
                } else {
                    block = "smooth_stone";
                }
                p.set(x(u), 0, z(v), block);
                for (int y = 1; y <= 3; y++) {
                    p.clear(x(u), y, z(v));
                }
            }
        }
        for (int[] c : new int[][] {{5, 5}, {5, -5}, {-5, 5}, {-5, -5}}) {
            Decor.lanternPost(p, x(c[0]), 1, z(c[1]), style.wood() + "_fence");
        }
        // Bänke am Rand, mit Blick zum Baum
        p.set(x(-3), 1, z(-8.5), "spruce_stairs[facing=north,half=bottom,shape=straight]");
        p.set(x(-2), 1, z(-8.5), "spruce_stairs[facing=north,half=bottom,shape=straight]");
        p.set(x(3), 1, z(8.5), "spruce_stairs[facing=south,half=bottom,shape=straight]");
        p.set(x(2), 1, z(8.5), "spruce_stairs[facing=south,half=bottom,shape=straight]");
        // Weg vom Platz zur Kartenmitte
        path(8, 0, 24, 0);
    }

    /** Ein Weg aus Pfad-Erde (in eigenen Achsen, nur auf Wiese). */
    private void path(double u1, double v1, double u2, double v2) {
        int steps = (int) Math.ceil(Math.hypot(u2 - u1, v2 - v1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            double u = u1 + (u2 - u1) * t;
            double v = v1 + (v2 - v1) * t;
            for (int dv = -1; dv <= 1; dv++) {
                int x = x(u);
                int z = z(v + dv);
                String g = p.get(x, 0, z);
                if (g != null && g.startsWith("grass_block")) {
                    p.set(x, 0, z, dv == 0 || r.nextInt(3) > 0 ? "dirt_path" : "coarse_dirt");
                    String above = p.get(x, 1, z);
                    if (above != null && Blocks.passable(above)) {
                        p.clear(x, 1, z);
                    }
                }
            }
        }
    }

    /** Wachhäusl der Maibaumwache: offene Hütte mit Bank, Laterne und Glocke, Blick auf den Baum. */
    private void guardHut() {
        int z1 = z(-12);
        int z2 = z(-10);
        for (int u = -1; u <= 1; u++) {
            for (int v = -12; v <= -10; v++) {
                p.set(x(u), 0, z(v), style.wood() + "_planks");
                for (int y = 1; y <= 3; y++) {
                    p.clear(x(u), y, z(v));
                }
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int u = -1; u <= 1; u++) {
                p.set(x(u), y, z1, style.wood() + "_planks");
            }
            p.set(x(-1), y, z2, "stripped_spruce_log[axis=y]");
            p.set(x(1), y, z2, "stripped_spruce_log[axis=y]");
            p.set(x(-1), y, z(-11), style.wood() + "_planks");
            p.set(x(1), y, z(-11), style.wood() + "_planks");
        }
        for (int u = -2; u <= 2; u++) {
            p.set(x(u), 4, z(-13), "spruce_stairs[facing=south,half=bottom,shape=straight]");
            p.set(x(u), 4, z(-12), "spruce_stairs[facing=south,half=bottom,shape=straight]");
            p.set(x(u), 4, z(-11), "spruce_slab[type=bottom]");
            p.set(x(u), 4, z(-10), "spruce_stairs[facing=north,half=bottom,shape=straight]");
            p.set(x(u), 4, z(-9), "spruce_stairs[facing=north,half=bottom,shape=straight]");
        }
        p.set(x(0), 1, z(-11), "spruce_stairs[facing=north,half=bottom,shape=straight]");
        p.set(x(0), 3, z(-10), "lantern[hanging=true,waterlogged=false]");
        p.set(x(0), 5, z(-11), "bell[attachment=floor,facing=" + toward(true) + "]");
        p.banner(x(0), 2, z(-11), side.dye() + "_wall_banner[facing=south]", Decor.flag(side));
    }

    /** Wirtshaus hinter dem Platz: zwei Geschosse, Balkon über der Tür. */
    private void inn() {
        int xa = x(-18);
        int xb = x(-10);
        Kit.house(p, new Kit.House(Math.min(xa, xb), z(-6), Math.max(xa, xb), z(6), doorToward(true), z(0),
                style.plaster(), style.wood(), true, true, style.floor()), r);
    }

    private void house(int u1, int u2, int v1, int v2, char doorV, boolean upper) {
        int xa = x(u1);
        int xb = x(u2);
        Kit.house(p, new Kit.House(Math.min(xa, xb), Math.min(z(v1), z(v2)), Math.max(xa, xb), Math.max(z(v1), z(v2)),
                doorV, x((u1 + u2) / 2.0), style.plaster(), style.wood(), upper, upper && r.nextBoolean(),
                style.floor()), r);
    }

    /** Holzstöße, Heuballen (Deckung vor dem Dorf), Gärten mit Zaun, Brunnen, Blumen. */
    private void yards() {
        // Heuballen-Stapel vor dem Dorf: Deckung für die Wache
        for (int[] h : new int[][] {{12, -6}, {12, 6}, {15, -11}, {15, 11}}) {
            for (int du = 0; du <= 1; du++) {
                for (int dv = 0; dv <= 1; dv++) {
                    p.set(x(h[0] + du), 1, z(h[1] + dv), "hay_block[axis=y]");
                }
            }
            p.set(x(h[0]), 2, z(h[1]), "hay_block[axis=y]");
        }
        // Holzstöße an den Häusern
        Kit.woodpile(p, x(-5), 1, z(-14), 4, 2, false);
        Kit.woodpile(p, x(-5), 1, z(11), 4, 2, false);
        // Brunnen neben dem Platz
        int bu = -2;
        int bv = 11;
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                p.set(x(bu + du), 1, z(bv + dv), du == 0 && dv == 0 ? "water" : "cobblestone_wall");
            }
        }
        p.set(x(bu), 0, z(bv), "cobblestone");
        // Gärten mit Zaun hinter den Häusern
        for (int v : new int[] {-24, 23}) {
            for (int u = -6; u <= 2; u++) {
                p.set(x(u), 1, z(v), style.wood() + "_fence");
            }
            for (int u = -5; u <= 1; u++) {
                int g = v < 0 ? v + 1 : v - 1;
                p.set(x(u), 1, z(g), Decor.pick(r, "sweet_berry_bush[age=3]", "rose_bush[half=lower]", "peony[half=lower]",
                        "short_grass", "allium"));
            }
        }
        Decor.flowers(p, x(dir > 0 ? -18 : 14), z(-24), x(dir > 0 ? 14 : -18), z(24), 9, r,
                (xx, zz) -> Math.abs(xx - cx) <= 9 && Math.abs(zz - cz) <= 9);
        // doppelte Blumen brauchen ihre obere Hälfte
        for (int v : new int[] {-23, 22}) {
            for (int u = -5; u <= 1; u++) {
                String s = p.get(x(u), 1, z(v));
                if (s != null && s.endsWith("[half=lower]")) {
                    p.set(x(u), 2, z(v), Blocks.base(s) + "[half=upper]");
                }
            }
        }
        if (style.alm()) {
            Decor.spruce(p, x(13), 1, z(-19), 7, r);
        } else {
            Decor.fruitTree(p, x(10), 1, z(-17), r);
            Decor.fruitTree(p, x(10), 1, z(17), r);
        }
    }

    /** Aussichtsfels (nur auf der Alm): vier Blöcke hoch, ein Heuwagen davor wirft einen hinauf. */
    private void lookout() {
        int[] c = {LOOKOUT_U, LOOKOUT_V};
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                for (int y = 1; y <= 4; y++) {
                    p.set(x(c[0] + du), y, z(c[1] + dv), y == 4 ? "moss_block" : Decor.pick(r, "stone", "andesite",
                            "cobblestone", "mossy_cobblestone"));
                }
            }
        }
        p.set(x(c[0]), 5, z(c[1]), side.dye() + "_banner[rotation=0]");
        p.set(x(c[0] + 1), 5, z(c[1] - 1), "lantern[hanging=false,waterlogged=false]");
    }

    /** Heuwagen vor dem Aussichtsfels: Stelle {x, z} (Heuballen im Boden) – nur auf der Alm. */
    static int[] lookoutPad(int cx, int cz, int dir) {
        return new int[] {cx + dir * LOOKOUT_U, cz + LOOKOUT_V - 3};
    }

    /** Startplatz {x, z, Blickrichtung} vor dem Wirtshaus, mit Blick zum Baum. */
    static double[] spawn(int cx, int cz, int dir) {
        return new double[] {cx + dir * SPAWN_U + 0.5, cz + 0.5, dir > 0 ? -90 : 90};
    }
}
