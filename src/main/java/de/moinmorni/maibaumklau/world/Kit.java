package de.moinmorni.maibaumklau.world;

import java.util.Random;

/**
 * Häuser und Möbel der Karten: Alpenhäuser mit Fensterläden und Blumenkästen, Satteldächer, Tische mit Bänken,
 * Kachelofen, Schänke. Eigene Entwürfe, nur Minecraft-Blöcke.
 */
final class Kit {

    /** Erdgeschoss: Boden y = 0, Wände y = 1–4, Decke (Boden oben) y = 5. */
    static final int GROUND_TOP = 4;
    /** Obergeschoss: Wände y = 6–8, Dachboden ab y = 9. */
    static final int UPPER_TOP = 8;

    private Kit() {
    }

    /** Ein Alpenhaus. {@code door}: Seite der Haustür (n, s, e, w); {@code doorAt}: x bzw. z der Tür. */
    record House(int x1, int z1, int x2, int z2, char door, int doorAt, String plaster, String wood, boolean upper,
            boolean balcony, String floor) {
    }

    /**
     * Alpenhaus bauen (innen leer): weiß verputztes Erdgeschoss, Holz-Obergeschoss, Fenster mit Läden und
     * Blumenkästen (Attrappen für Blumentöpfe), Balkon über der Tür, Satteldach mit Überstand, Kamin.
     */
    static void house(Plan p, House h, Random r) {
        int top = h.upper() ? UPPER_TOP : GROUND_TOP;
        String log = h.wood() + "_log";
        for (int x = h.x1(); x <= h.x2(); x++) {
            for (int z = h.z1(); z <= h.z2(); z++) {
                boolean ex = x == h.x1() || x == h.x2();
                boolean ez = z == h.z1() || z == h.z2();
                boolean edge = ex || ez;
                p.set(x, 0, z, edge ? "stone_bricks" : h.floor());
                for (int y = 1; y <= top; y++) {
                    if (!edge) {
                        p.clear(x, y, z);
                        continue;
                    }
                    String block;
                    if (ex && ez) {
                        block = log + "[axis=y]";
                    } else if (y == GROUND_TOP + 1) {
                        block = log + (ex ? "[axis=z]" : "[axis=x]");
                    } else if (y <= GROUND_TOP) {
                        block = h.plaster();
                    } else {
                        block = h.wood() + "_planks";
                    }
                    p.set(x, y, z, block);
                }
                if (!edge) {
                    p.set(x, GROUND_TOP + 1, z, h.wood() + "_planks");
                    if (h.upper()) {
                        p.set(x, UPPER_TOP + 1, z, h.wood() + "_planks");
                    }
                }
            }
        }
        // Fenster
        windows(p, h, 2, r);
        if (h.upper()) {
            windows(p, h, GROUND_TOP + 2, r);
        }
        door(p, h);
        if (h.balcony() && h.upper()) {
            balcony(p, h, r);
        }
        roof(p, h.x1(), h.z1(), h.x2(), h.z2(), top + 1, h.wood(), (h.x2() - h.x1()) <= (h.z2() - h.z1()));
        // Kamin
        int cx = h.x2() - 2;
        int cz = h.z1() + 2;
        for (int y = top + 1; y <= top + 2 + Math.max(h.x2() - h.x1(), h.z2() - h.z1()) / 2; y++) {
            p.set(cx, y, cz, "bricks");
        }
    }

    private static boolean doorSide(House h, char side, int along) {
        return h.door() == side && Math.abs(along - h.doorAt()) <= 1;
    }

    private static void windows(Plan p, House h, int y, Random r) {
        for (int x = h.x1() + 2; x <= h.x2() - 2; x += 3) {
            if (!doorSide(h, 'n', x)) {
                window(p, x, y, h.z1(), 'n', h.wood(), r);
            }
            if (!doorSide(h, 's', x)) {
                window(p, x, y, h.z2(), 's', h.wood(), r);
            }
        }
        for (int z = h.z1() + 2; z <= h.z2() - 2; z += 3) {
            if (!doorSide(h, 'w', z)) {
                window(p, h.x1(), y, z, 'w', h.wood(), r);
            }
            if (!doorSide(h, 'e', z)) {
                window(p, h.x2(), y, z, 'e', h.wood(), r);
            }
        }
    }

    /** Fenster (2 hoch) in der Wand, außen Läden und ein Blumenkasten mit Topf. */
    static void window(Plan p, int x, int y, int z, char side, String wood, Random r) {
        int ox = side == 'e' ? 1 : side == 'w' ? -1 : 0;
        int oz = side == 's' ? 1 : side == 'n' ? -1 : 0;
        String facing = facing(side);
        p.set(x, y, z, "glass_pane");
        p.set(x, y + 1, z, "glass_pane");
        // Läden links und rechts (offene Falltüren an der Wand)
        int sx = oz != 0 ? 1 : 0;
        int sz = ox != 0 ? 1 : 0;
        p.set(x + ox - sx, y, z + oz - sz, wood + "_trapdoor[facing=" + facing + ",half=bottom,open=true]");
        p.set(x + ox + sx, y, z + oz + sz, wood + "_trapdoor[facing=" + facing + ",half=bottom,open=true]");
        p.set(x + ox - sx, y + 1, z + oz - sz, wood + "_trapdoor[facing=" + facing + ",half=bottom,open=true]");
        p.set(x + ox + sx, y + 1, z + oz + sz, wood + "_trapdoor[facing=" + facing + ",half=bottom,open=true]");
        // Blumenkasten: Falltür unten, Topf darauf
        p.set(x + ox, y - 1, z + oz, wood + "_trapdoor[facing=" + facing + ",half=top,open=false]");
        p.set(x + ox, y, z + oz, Decor.POTS[r.nextInt(Decor.POTS.length)]);
    }

    private static void door(Plan p, House h) {
        int x = h.door() == 'e' ? h.x2() : h.door() == 'w' ? h.x1() : h.doorAt();
        int z = h.door() == 's' ? h.z2() : h.door() == 'n' ? h.z1() : h.doorAt();
        String facing = facing(h.door());
        p.set(x, 1, z, h.wood() + "_door[facing=" + facing + ",half=lower,hinge=left,open=true,powered=false]");
        p.set(x, 2, z, h.wood() + "_door[facing=" + facing + ",half=upper,hinge=left,open=true,powered=false]");
        p.set(x, 3, z, h.plaster());
        int ox = h.door() == 'e' ? 1 : h.door() == 'w' ? -1 : 0;
        int oz = h.door() == 's' ? 1 : h.door() == 'n' ? -1 : 0;
        // Vordach mit Laterne
        p.set(x + ox, 4, z + oz, h.wood() + "_slab[type=top]");
        if (h.door() == 'n' || h.door() == 's') {
            p.set(x + ox - 1, 4, z + oz, h.wood() + "_slab[type=top]");
            p.set(x + ox + 1, 4, z + oz, h.wood() + "_slab[type=top]");
        } else {
            p.set(x + ox, 4, z + oz - 1, h.wood() + "_slab[type=top]");
            p.set(x + ox, 4, z + oz + 1, h.wood() + "_slab[type=top]");
        }
        p.set(x + ox, 3, z + oz, "lantern[hanging=true,waterlogged=false]");
    }

    private static void balcony(Plan p, House h, Random r) {
        int y = GROUND_TOP + 1;
        if (h.door() == 'n' || h.door() == 's') {
            int z = h.door() == 's' ? h.z2() + 1 : h.z1() - 1;
            for (int x = h.x1() + 1; x <= h.x2() - 1; x++) {
                p.set(x, y, z, h.wood() + "_slab[type=top]");
                p.set(x, y + 1, z, h.wood() + "_fence");
                if (Math.floorMod(x, 2) == 0) {
                    p.set(x, y + 2, z, Decor.POTS[r.nextInt(Decor.POTS.length)]);
                }
            }
        } else {
            int x = h.door() == 'e' ? h.x2() + 1 : h.x1() - 1;
            for (int z = h.z1() + 1; z <= h.z2() - 1; z++) {
                p.set(x, y, z, h.wood() + "_slab[type=top]");
                p.set(x, y + 1, z, h.wood() + "_fence");
                if (Math.floorMod(z, 2) == 0) {
                    p.set(x, y + 2, z, Decor.POTS[r.nextInt(Decor.POTS.length)]);
                }
            }
        }
    }

    /**
     * Satteldach mit einem Block Überstand ringsum. {@code ridgeAlongZ}: der First läuft in z-Richtung (Dachflächen
     * zeigen nach Ost und West), sonst in x-Richtung. Giebel aus Brettern.
     */
    static void roof(Plan p, int x1, int z1, int x2, int z2, int y0, String wood, boolean ridgeAlongZ) {
        String stairs = wood + "_stairs";
        String planks = wood + "_planks";
        if (ridgeAlongZ) {
            for (int i = 0;; i++) {
                int left = x1 - 1 + i;
                int right = x2 + 1 - i;
                int y = y0 + i;
                if (left > right) {
                    break;
                }
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    if (left == right) {
                        p.set(left, y, z, wood + "_slab[type=bottom]");
                        continue;
                    }
                    p.set(left, y, z, stairs + "[facing=east,half=bottom,shape=straight]");
                    p.set(right, y, z, stairs + "[facing=west,half=bottom,shape=straight]");
                    if (z == z1 || z == z2) {
                        for (int x = left + 1; x < right; x++) {
                            p.set(x, y, z, planks);
                        }
                    }
                }
            }
        } else {
            for (int i = 0;; i++) {
                int front = z1 - 1 + i;
                int back = z2 + 1 - i;
                int y = y0 + i;
                if (front > back) {
                    break;
                }
                for (int x = x1 - 1; x <= x2 + 1; x++) {
                    if (front == back) {
                        p.set(x, y, front, wood + "_slab[type=bottom]");
                        continue;
                    }
                    p.set(x, y, front, stairs + "[facing=south,half=bottom,shape=straight]");
                    p.set(x, y, back, stairs + "[facing=north,half=bottom,shape=straight]");
                    if (x == x1 || x == x2) {
                        for (int z = front + 1; z < back; z++) {
                            p.set(x, y, z, planks);
                        }
                    }
                }
            }
        }
    }

    /**
     * Flaches Satteldach (halbe Neigung, aus Stufen) wie bei großen Alpenhäusern, ein Block Überstand ringsum.
     * Die Giebel werden mit Brettern bis unters Dach gefüllt.
     */
    static void flatRoof(Plan p, int x1, int z1, int x2, int z2, int y0, String wood, boolean ridgeAlongZ) {
        String slab = wood + "_slab";
        String planks = wood + "_planks";
        int lo = ridgeAlongZ ? x1 - 1 : z1 - 1;
        int hi = ridgeAlongZ ? x2 + 1 : z2 + 1;
        int aLo = ridgeAlongZ ? z1 - 1 : x1 - 1;
        int aHi = ridgeAlongZ ? z2 + 1 : x2 + 1;
        int span = hi - lo;
        for (int i = 0; i <= span / 2; i++) {
            int y = y0 + i / 2;
            String type = i % 2 == 0 ? "bottom" : "top";
            for (int a = aLo; a <= aHi; a++) {
                for (int side : new int[] {lo + i, hi - i}) {
                    if (ridgeAlongZ) {
                        p.set(side, y, a, slab + "[type=" + type + "]");
                    } else {
                        p.set(a, y, side, slab + "[type=" + type + "]");
                    }
                }
                // Giebel unter der Dachfläche schließen
                boolean gable = ridgeAlongZ ? (a == z1 || a == z2) : (a == x1 || a == x2);
                if (gable && i > 0) {
                    for (int yy = y0; yy < y + (i % 2 == 0 ? 0 : 1); yy++) {
                        for (int side : new int[] {lo + i, hi - i}) {
                            if (ridgeAlongZ) {
                                p.set(side, yy, a, planks);
                            } else {
                                p.set(a, yy, side, planks);
                            }
                        }
                    }
                }
            }
        }
    }

    static String facing(char side) {
        return switch (side) {
            case 'n' -> "north";
            case 'e' -> "east";
            case 'w' -> "west";
            default -> "south";
        };
    }

    // ---------- Möbel ----------

    /**
     * Langer Tisch (obere Fichtenstufen) mit Bänken (Treppen) auf beiden Seiten. {@code alongX}: Tisch läuft in
     * x-Richtung. Auf dem Tisch stehen Krüge, Laternen und Blumentöpfe (Attrappen).
     */
    static void longTable(Plan p, int x, int y, int z, int length, boolean alongX, String wood, boolean benches,
            Random r, String... onTop) {
        for (int i = 0; i < length; i++) {
            int tx = alongX ? x + i : x;
            int tz = alongX ? z : z + i;
            p.set(tx, y, tz, wood + "_slab[type=top]");
            if (benches) {
                if (alongX) {
                    p.set(tx, y, tz - 1, "spruce_stairs[facing=north,half=bottom,shape=straight]");
                    p.set(tx, y, tz + 1, "spruce_stairs[facing=south,half=bottom,shape=straight]");
                } else {
                    p.set(tx - 1, y, tz, "spruce_stairs[facing=west,half=bottom,shape=straight]");
                    p.set(tx + 1, y, tz, "spruce_stairs[facing=east,half=bottom,shape=straight]");
                }
            }
            if (onTop.length > 0 && (i % 2 == 0 || r.nextInt(3) == 0)) {
                p.set(tx, y + 1, tz, onTop[r.nextInt(onTop.length)]);
            }
        }
    }

    /** Kleiner runder Tisch (Pfosten + Druckplatte) mit vier Stühlen. */
    static void roundTable(Plan p, int x, int y, int z, String wood, String onTop) {
        p.set(x, y, z, wood + "_fence");
        p.set(x, y + 1, z, onTop != null ? onTop : wood + "_pressure_plate");
        p.set(x, y, z - 1, "spruce_stairs[facing=north,half=bottom,shape=straight]");
        p.set(x, y, z + 1, "spruce_stairs[facing=south,half=bottom,shape=straight]");
        p.set(x - 1, y, z, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        p.set(x + 1, y, z, "spruce_stairs[facing=east,half=bottom,shape=straight]");
    }

    /** Kachelofen (grün glasiert) mit Ofenbank, 3 × 2 Grundfläche ab (x, z). */
    static void tiledStove(Plan p, int x, int y, int z, boolean alongX) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                int sx = alongX ? x + i : x + j;
                int sz = alongX ? z + j : z + i;
                p.set(sx, y, sz, "green_terracotta");
                p.set(sx, y + 1, sz, i == 1 && j == 0 ? "iron_bars" : "green_glazed_terracotta");
                p.set(sx, y + 2, sz, "mud_bricks");
            }
        }
    }

    /** Holzstapel (liegende Stämme) als Mauer, Länge in x oder z. */
    static void woodpile(Plan p, int x, int y, int z, int length, int height, boolean alongX) {
        for (int i = 0; i < length; i++) {
            for (int h = 0; h < height; h++) {
                p.set(alongX ? x + i : x, y + h, alongX ? z : z + i, "spruce_log[axis=" + (alongX ? "z" : "x") + "]");
            }
        }
    }
}
