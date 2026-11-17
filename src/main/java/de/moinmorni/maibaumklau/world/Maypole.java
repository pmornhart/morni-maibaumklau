package de.moinmorni.maibaumklau.world;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.List;

/**
 * Ein geschmückter Maibaum (eigener Entwurf): Sockel aus Steinstufen, unten ein Stück blanker Stamm, darüber gedrehte
 * Streifen in der Farbe des Dorfs (Weiß-Blau bzw. Rot-Weiß), Zunftzeichen als kleine Tafeln, ein Kranz aus blühendem
 * Laub und oben die grüne Krone. Alles relativ zum Fuß (y = 0 ist der Boden). Was beim Umlegen verschwindet, liegt in
 * der Box {@link #removable(int, int)} – der Sockel bleibt stehen.
 */
public final class Maypole {

    /** Erste Höhe, die zum Baum gehört (darunter: Sockel). */
    public static final int FIRST = 2;
    /** Oberstes Stück des Stamms. */
    public static final int TOP = 17;
    /** Höhe des Kranzes. */
    public static final int WREATH = 14;
    /** Höchster Block (Spitze der Krone). */
    public static final int CROWN_TOP = 21;
    /** Länge des Stamms (für das Modell beim Tragen). */
    public static final int LENGTH = TOP - FIRST + 1;

    private Maypole() {
    }

    /** Baum mit Sockel an (bx, bz) in den Plan setzen. */
    public static void build(Plan p, int bx, int bz, Side side) {
        // Sockel: Mitte gemeißelt, ringsum Stufen nach außen
        p.set(bx, 1, bz, "chiseled_stone_bricks");
        p.set(bx + 1, 1, bz, "stone_brick_stairs[facing=west,half=bottom,shape=straight]");
        p.set(bx - 1, 1, bz, "stone_brick_stairs[facing=east,half=bottom,shape=straight]");
        p.set(bx, 1, bz + 1, "stone_brick_stairs[facing=north,half=bottom,shape=straight]");
        p.set(bx, 1, bz - 1, "stone_brick_stairs[facing=south,half=bottom,shape=straight]");
        for (int[] c : new int[][] {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            p.set(bx + c[0], 1, bz + c[1], "stone_brick_slab[type=bottom]");
        }
        // Stamm: unten blank, darüber Streifen (je zwei Blöcke)
        for (int y = FIRST; y <= TOP; y++) {
            p.set(bx, y, bz, stem(side, y));
        }
        // Zunftzeichen: kleine Tafeln (Banner) an allen vier Seiten, versetzt
        List<String> flag = Decor.flag(side);
        p.banner(bx + 1, 8, bz, side.dye() + "_wall_banner[facing=east]", flag);
        p.banner(bx - 1, 8, bz, "white_wall_banner[facing=west]", Decor.gold());
        p.banner(bx, 11, bz + 1, "white_wall_banner[facing=south]", Decor.gold());
        p.banner(bx, 11, bz - 1, side.dye() + "_wall_banner[facing=north]", flag);
        // Kranz aus blühendem Laub mit Bändern in der Dorffarbe
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    p.set(bx + dx, WREATH, bz + dz, (dx + dz) % 2 == 0 ? "flowering_azalea_leaves[persistent=true]"
                            : "azalea_leaves[persistent=true]");
                }
            }
        }
        for (int[] c : new int[][] {{1, 1}, {-1, -1}}) {
            p.set(bx + c[0], WREATH - 1, bz + c[1], side.stripe() + "_wool");
            p.set(bx - c[0], WREATH - 1, bz + c[1], "white_wool");
        }
        // Krone: kleine Fichte
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(bx + dx, TOP + 1, bz + dz, "spruce_leaves[persistent=true]");
                if (Math.abs(dx) + Math.abs(dz) <= 1) {
                    p.set(bx + dx, TOP + 2, bz + dz, "spruce_leaves[persistent=true]");
                }
            }
        }
        p.set(bx, TOP + 1, bz, "spruce_log[axis=y]");
        p.set(bx, TOP + 3, bz, "spruce_leaves[persistent=true]");
        p.set(bx, CROWN_TOP, bz, side.stripe() + "_wool");
    }

    /** Block des Stamms auf Höhe {@code y}: unten blankes Holz, darüber Streifen. */
    static String stem(Side side, int y) {
        if (y < FIRST + 2) {
            return "stripped_spruce_log[axis=y]";
        }
        return ((y - FIRST) / 2) % 2 == 0 ? side.stripe() + "_concrete" : "white_concrete";
    }

    /** Box (minX, minY, minZ, maxX, maxY, maxZ) mit allem, was beim Umlegen verschwindet. */
    public static int[] removable(int bx, int bz) {
        return new int[] {bx - 1, FIRST, bz - 1, bx + 1, CROWN_TOP, bz + 1};
    }
}
