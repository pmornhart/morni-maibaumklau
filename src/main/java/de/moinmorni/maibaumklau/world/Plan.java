package de.moinmorni.maibaumklau.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Bauplan: welche Blöcke wohin (relativ zum Mittelpunkt, y = 0 ist die oberste Bodenschicht, man steht auf y = 1).
 * Blöcke stehen als Text (z. B. {@code spruce_stairs[facing=north]}) – so lässt sich jeder Plan ohne Server bauen und
 * prüfen. Feste Zufallszahlen: jeder Neubau sieht genau gleich aus.
 */
public final class Plan {

    /** Banner mit Muster ({@code "lime:rhombus"} = Farbe:Muster), Muster brauchen den fertigen Block. */
    public record Banner(int x, int y, int z, List<String> patterns) {
    }

    private static final int OFFSET = 2048;
    private static final int BITS = 16;
    private static final int MASK = 0xFFFF;
    private static final String[][] SIDES = {{"north", "0", "-1"}, {"east", "1", "0"}, {"south", "0", "1"},
        {"west", "-1", "0"}};

    private final Map<Long, String> blocks = new LinkedHashMap<>();
    private final List<Banner> banners = new ArrayList<>();
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    public static long key(int x, int y, int z) {
        return ((long) (x + OFFSET) << (2 * BITS)) | ((long) (y + OFFSET) << BITS) | (z + OFFSET);
    }

    public static int keyX(long k) {
        return (int) ((k >>> (2 * BITS)) & MASK) - OFFSET;
    }

    public static int keyY(long k) {
        return (int) ((k >>> BITS) & MASK) - OFFSET;
    }

    public static int keyZ(long k) {
        return (int) (k & MASK) - OFFSET;
    }

    public void set(int x, int y, int z, String spec) {
        if (spec == null || spec.equals("air") || spec.equals("cave_air")) {
            blocks.remove(key(x, y, z));
            return;
        }
        blocks.put(key(x, y, z), spec);
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }

    /** Nur setzen, wenn dort noch nichts ist. */
    public void setIfEmpty(int x, int y, int z, String spec) {
        if (!blocks.containsKey(key(x, y, z))) {
            set(x, y, z, spec);
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String spec) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, spec);
                }
            }
        }
    }

    /** Hohler Kasten: nur die Wände (ohne Boden und Decke). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, String spec) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                set(x, y, z1, spec);
                set(x, y, z2, spec);
            }
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(x1, y, z, spec);
                set(x2, y, z, spec);
            }
        }
    }

    public void clear(int x, int y, int z) {
        blocks.remove(key(x, y, z));
    }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    blocks.remove(key(x, y, z));
                }
            }
        }
    }

    public String get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    /** Steht dort etwas, durch das man nicht hindurchlaufen kann? */
    public boolean solid(int x, int y, int z) {
        String s = blocks.get(key(x, y, z));
        return s != null && !Blocks.passable(s);
    }

    /** Kann ein Spieler hier stehen (Boden darunter, zwei Blöcke frei)? */
    public boolean standable(int x, int y, int z) {
        return solid(x, y - 1, z) && !solid(x, y, z) && !solid(x, y + 1, z);
    }

    public void banner(int x, int y, int z, String blockSpec, List<String> patterns) {
        set(x, y, z, blockSpec);
        banners.add(new Banner(x, y, z, List.copyOf(patterns)));
    }

    public List<Banner> banners() {
        return banners;
    }

    public void forEach(BiConsumer<Long, String> action) {
        blocks.forEach(action);
    }

    public int size() {
        return blocks.size();
    }

    /** Wie viele Blöcke passen? (für Tests) */
    public int count(Predicate<String> which) {
        return (int) blocks.values().stream().filter(which).count();
    }

    public int[] bounds() {
        return new int[] {minX, minY, minZ, maxX, maxY, maxZ};
    }

    /** Einen anderen Plan verschoben hineinlegen (überschreibt). */
    public void paste(Plan other, int dx, int dy, int dz) {
        other.blocks.forEach((k, s) -> set(keyX(k) + dx, keyY(k) + dy, keyZ(k) + dz, s));
        for (Banner b : other.banners) {
            banners.add(new Banner(b.x() + dx, b.y() + dy, b.z() + dz, b.patterns()));
        }
    }

    /** Ein Block mit seiner Stelle. */
    public interface At {
        void accept(int x, int y, int z, String spec);
    }

    public void forEachAt(At action) {
        blocks.forEach((k, s) -> action.accept(keyX(k), keyY(k), keyZ(k), s));
    }

    /** Nur der Teil innerhalb der Box (Spielfläche für die Wiederherstellung, ein Maibaum zum Wegnehmen …). */
    public Plan crop(int x1, int y1, int z1, int x2, int y2, int z2) {
        Plan out = new Plan();
        forEachAt((x, y, z, s) -> {
            if (x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2) {
                out.set(x, y, z, s);
            }
        });
        for (Banner b : banners) {
            if (b.x() >= x1 && b.x() <= x2 && b.y() >= y1 && b.y() <= y2 && b.z() >= z1 && b.z() <= z2) {
                out.banners.add(b);
            }
        }
        return out;
    }

    /** Ist an dieser Stelle ein Banner mit Muster? */
    public Banner bannerAt(int x, int y, int z) {
        for (Banner b : banners) {
            if (b.x() == x && b.y() == y && b.z() == z) {
                return b;
            }
        }
        return null;
    }

    /** Erste Höhe ab {@code from} aufwärts, auf der man stehen kann (Boden darunter, zwei Blöcke frei). */
    public int standY(int x, int z, int from) {
        int y = from;
        while (y < from + 40 && !standable(x, y, z)) {
            y++;
        }
        return y;
    }

    /**
     * Zäune, Mauern, Scheiben und Gitter mit ihren Nachbarn verbinden – gebaut wird ohne Physik, sonst blieben sie
     * einzelne Pfosten.
     */
    public void connect() {
        Map<Long, String> changed = new HashMap<>();
        blocks.forEach((k, s) -> {
            int x = keyX(k);
            int y = keyY(k);
            int z = keyZ(k);
            if (Blocks.wall(s)) {
                Map<String, String> p = new LinkedHashMap<>();
                int sides = 0;
                boolean ns = false;
                boolean ew = false;
                for (String[] side : SIDES) {
                    String n = get(x + Integer.parseInt(side[1]), y, z + Integer.parseInt(side[2]));
                    boolean join = n != null && (Blocks.wall(n) || Blocks.pane(n) || Blocks.fenceGate(n)
                            || (Blocks.full(n) && !"barrier".equals(Blocks.base(n))));
                    p.put(side[0], join ? "low" : "none");
                    if (join) {
                        sides++;
                        if (side[0].equals("north") || side[0].equals("south")) {
                            ns = true;
                        } else {
                            ew = true;
                        }
                    }
                }
                boolean straight = sides == 2 && (ns != ew);
                p.put("up", String.valueOf(get(x, y + 1, z) != null || !straight));
                changed.put(k, Blocks.with(s, p));
            } else if (Blocks.fence(s) || Blocks.pane(s)) {
                Map<String, String> p = new LinkedHashMap<>();
                for (String[] side : SIDES) {
                    String n = get(x + Integer.parseInt(side[1]), y, z + Integer.parseInt(side[2]));
                    p.put(side[0], String.valueOf(n != null && joins(s, n)));
                }
                changed.put(k, Blocks.with(s, p));
            }
        });
        blocks.putAll(changed);
    }

    private static boolean joins(String self, String other) {
        String o = Blocks.base(other);
        if (o.equals("barrier") || o.contains("leaves") || o.contains("pumpkin") || o.equals("melon")) {
            return false;
        }
        if (Blocks.fence(self)) {
            if (Blocks.fence(other)) {
                return Blocks.woodFence(self) == Blocks.woodFence(other);
            }
            return Blocks.fenceGate(other) || Blocks.full(other);
        }
        return Blocks.pane(other) || Blocks.wall(other) || Blocks.full(other) || o.contains("glass");
    }
}
