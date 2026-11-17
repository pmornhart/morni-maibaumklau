package de.moinmorni.maibaumklau.world;

import java.util.List;
import java.util.Random;

/**
 * Bausteine für Wirtsgarten und Karten: schwebende Inseln, Bäume, Berge, Laternen, Zäune, Blumen, Banner in unseren
 * Farben. Eigene Entwürfe, nur Minecraft-Blöcke.
 */
final class Decor {

    static final String[] FLOWERS = {"short_grass", "short_grass", "short_grass", "short_grass", "dandelion", "poppy",
        "cornflower", "azure_bluet", "oxeye_daisy", "allium", "blue_orchid"};
    static final String[] POTS = {"potted_red_tulip", "potted_poppy", "potted_cornflower", "potted_azure_bluet",
        "potted_dandelion", "potted_orange_tulip", "potted_allium", "potted_white_tulip", "potted_pink_tulip"};

    private Decor() {
    }

    static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    /** Fels mit etwas Abwechslung; tiefer unten mehr Tiefenschiefer. */
    static String rock(Random r, int depth) {
        int roll = r.nextInt(100);
        if (depth > 8) {
            return roll < 45 ? "deepslate" : roll < 70 ? "tuff" : roll < 85 ? "stone" : "cobbled_deepslate";
        }
        return roll < 60 ? "stone" : roll < 75 ? "andesite" : roll < 88 ? "tuff" : roll < 96 ? "cobblestone" : "gravel";
    }

    /**
     * Schwebende Insel (alles schwebt im leeren Himmel): Fläche mit runden Ecken (Halbachsen {@code rx}, {@code rz}),
     * oben {@code soilDepth} Blöcke Erde, darunter Fels, nach unten spitz zulaufend. Oberfläche bei y = 0 (Gras).
     */
    static void island(Plan p, int rx, int rz, int depth, Random r) {
        int ex = rx + 2;
        int ez = rz + 2;
        for (int x = -ex; x <= ex; x++) {
            for (int z = -ez; z <= ez; z++) {
                double nx = Math.abs(x) / (double) ex;
                double nz = Math.abs(z) / (double) ez;
                double rr = Math.pow(Math.pow(nx, 4) + Math.pow(nz, 4), 0.25);
                if (rr > 1 + (r.nextDouble() * 1.5 - 0.5) / Math.max(ex, ez)) {
                    continue;
                }
                double f = Math.max(0, 1 - rr * rr);
                int col = 2 + (int) Math.round(depth * Math.sqrt(f)) + r.nextInt(2);
                p.setIfEmpty(x, 0, z, "grass_block[snowy=false]");
                for (int d = 1; d <= col; d++) {
                    p.setIfEmpty(x, -d, z, d <= 2 ? "dirt" : rock(r, d));
                }
            }
        }
    }

    /**
     * Rand der Spielfläche: Weidezaun (Holz) bei {@code |x| = hx} bzw. {@code |z| = hz} und darüber unsichtbare Wand bis
     * {@code top} – niemand läuft oder springt hinaus. Nur dort, wo Boden ist (über Schluchten bleibt es offen, dort fällt
     * man ohnehin).
     */
    static void border(Plan p, int hx, int hz, int top, String fence) {
        for (int x = -hx; x <= hx; x++) {
            post(p, x, -hz, top, fence);
            post(p, x, hz, top, fence);
        }
        for (int z = -hz; z <= hz; z++) {
            post(p, -hx, z, top, fence);
            post(p, hx, z, top, fence);
        }
    }

    private static void post(Plan p, int x, int z, int top, String fence) {
        String ground = p.get(x, 0, z);
        boolean land = ground != null && !ground.startsWith("water");
        for (int y = 1; y <= top; y++) {
            p.set(x, y, z, y == 1 && land ? fence : "barrier");
        }
    }

    /** Wolke aus weißer Wolle und Schnee (flach, ausgefranst). */
    static void cloud(Plan p, int cx, int y, int cz, int size, Random r) {
        for (int dx = -size * 2; dx <= size * 2; dx++) {
            for (int dz = -size; dz <= size; dz++) {
                double d = Math.hypot(dx / 2.0, dz);
                if (d <= size - r.nextDouble() * 0.8) {
                    p.setIfEmpty(cx + dx, y, cz + dz, r.nextInt(5) == 0 ? "snow_block" : "white_wool");
                    if (d <= size * 0.5 && r.nextBoolean()) {
                        p.setIfEmpty(cx + dx, y + 1, cz + dz, "white_wool");
                    }
                }
            }
        }
    }

    /** Berge rundherum in der Ferne (Kulisse, gehört nicht zur Spielfläche). */
    static void mountainRing(Plan p, int distanceX, int distanceZ, int y0, Random r) {
        int count = 14;
        for (int i = 0; i < count; i++) {
            double a = 2 * Math.PI * i / count + r.nextDouble() * 0.2;
            int x = (int) Math.round(Math.cos(a) * (distanceX + r.nextInt(14)));
            int z = (int) Math.round(Math.sin(a) * (distanceZ + r.nextInt(14)));
            mountain(p, x, z, y0, 18 + r.nextInt(10), 26 + r.nextInt(18), r);
        }
    }

    /** Fichte: Stamm und kegelförmige Krone. */
    static void spruce(Plan p, int x, int y, int z, int height, Random r) {
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, "spruce_log[axis=y]");
        }
        int top = y + height;
        p.set(x, top, z, "spruce_leaves[persistent=true]");
        p.set(x, top + 1, z, "spruce_leaves[persistent=true]");
        for (int layer = 0; layer < height - 1; layer++) {
            int ly = top - layer;
            int rad = Math.max(1, Math.min(3, 1 + layer / 2 - (layer % 2 == 0 ? 0 : 1)));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if ((dx != 0 || dz != 0) && Math.abs(dx) + Math.abs(dz) <= rad + 1 && r.nextInt(10) > 0) {
                        p.setIfEmpty(x + dx, ly, z + dz, "spruce_leaves[persistent=true]");
                    }
                }
            }
        }
    }

    /** Laubbaum mit runder Krone (Kastanie, Linde, Apfelbaum …). */
    static void broadTree(Plan p, int x, int y, int z, String log, String leaves, int trunk, int radius, Random r) {
        for (int i = 0; i < trunk; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
        int cy = y + trunk + radius / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius + 1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
                    if (d <= radius + 0.3 - r.nextDouble() * 0.6) {
                        p.setIfEmpty(x + dx, cy + dy, z + dz, leaves + "[persistent=true]");
                    }
                }
            }
        }
        for (int i = trunk; i < trunk + radius / 2 + 1; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
    }

    /** Obstbaum: kurzer Stamm, Krone aus Eichen- und blühenden Azaleenblättern. */
    static void fruitTree(Plan p, int x, int y, int z, Random r) {
        for (int i = 0; i < 3; i++) {
            p.set(x, y + i, z, "oak_log[axis=y]");
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    double d = Math.sqrt(dx * dx + (dy - 1) * (dy - 1) * 2.0 + dz * dz);
                    if (d <= 2.4 - r.nextDouble() * 0.5) {
                        p.setIfEmpty(x + dx, y + 3 + dy, z + dz, r.nextInt(4) == 0
                                ? "flowering_azalea_leaves[persistent=true]" : "oak_leaves[persistent=true]");
                    }
                }
            }
        }
        p.set(x, y + 3, z, "oak_log[axis=y]");
    }

    /** Busch aus Laub (Azaleen blühen manchmal), etwa 2 × 2 groß, oben gerundet. */
    static void bush(Plan p, int x, int y, int z, Random r) {
        String[] leaves = {"oak_leaves[persistent=true]", "azalea_leaves[persistent=true]",
            "flowering_azalea_leaves[persistent=true]", "birch_leaves[persistent=true]"};
        String main = leaves[r.nextInt(leaves.length)];
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (r.nextInt(5) > 0) {
                    p.setIfEmpty(x + dx, y, z + dz, r.nextInt(4) == 0 ? leaves[r.nextInt(leaves.length)] : main);
                }
            }
        }
        p.setIfEmpty(x + r.nextInt(2), y + 1, z + r.nextInt(2), main);
    }

    /** Ein paar Büsche und Bäumchen auf freier Wiese in einem Rechteck (nicht auf Wegen, nicht in Häusern). */
    static void greenery(Plan p, int x1, int z1, int x2, int z2, int count, Random r,
            java.util.function.BiPredicate<Integer, Integer> keep) {
        for (int i = 0; i < count * 4 && count > 0; i++) {
            int x = x1 + r.nextInt(Math.max(1, x2 - x1 + 1));
            int z = z1 + r.nextInt(Math.max(1, z2 - z1 + 1));
            String g = p.get(x, 0, z);
            if (g == null || !g.startsWith("grass_block") || keep.test(x, z)) {
                continue;
            }
            boolean free = true;
            for (int dx = -1; dx <= 2 && free; dx++) {
                for (int dz = -1; dz <= 2 && free; dz++) {
                    String a = p.get(x + dx, 1, z + dz);
                    free = a == null || Blocks.passable(a);
                }
            }
            if (!free) {
                continue;
            }
            if (r.nextInt(3) == 0) {
                fruitTree(p, x, 1, z, r);
            } else {
                bush(p, x, 1, z, r);
            }
            count--;
        }
    }

    /** Laternenpfahl: drei Zaunpfosten, oben eine Laterne. */
    static void lanternPost(Plan p, int x, int y, int z, String fence) {
        for (int i = 0; i < 3; i++) {
            p.set(x, y + i, z, fence);
        }
        p.set(x, y + 3, z, "lantern[hanging=false,waterlogged=false]");
    }

    /** Hängende Laterne an einer Kette. */
    static void hangingLantern(Plan p, int x, int y, int z, int chain) {
        for (int i = 0; i < chain; i++) {
            p.set(x, y - i, z, "iron_chain[axis=y]");
        }
        p.set(x, y - chain, z, "lantern[hanging=true,waterlogged=false]");
    }

    /** Kulissen-Berg mit Schneekappe in der Ferne (nur Fels, Wiese unten, Schnee oben). */
    static void mountain(Plan p, int cx, int cz, int y0, int radius, int height, Random r) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / radius;
                if (d > 1) {
                    continue;
                }
                double ridge = 0.12 * Math.sin(dx * 0.7) * Math.cos(dz * 0.5);
                int top = (int) Math.round(height * Math.pow(1 - d, 1.25) + height * ridge * (1 - d));
                if (top < 1) {
                    continue;
                }
                int crust = 5 + r.nextInt(3);
                for (int y = Math.max(-6, top - crust); y <= top; y++) {
                    String block;
                    if (y >= top - 1 && top > height * 0.62) {
                        block = "snow_block";
                    } else if (y >= top - 1 && top < height * 0.3) {
                        block = r.nextInt(3) == 0 ? "moss_block" : "grass_block[snowy=false]";
                    } else {
                        block = rock(r, top - y > 3 ? 10 : 0);
                    }
                    p.setIfEmpty(cx + dx, y0 + y, cz + dz, block);
                }
            }
        }
    }

    /** Rautenmuster wie die weiß-blaue Fahne: schräg gestelltes Schachbrett. */
    static boolean lozenge(int x, int z, int size) {
        return Math.floorMod(Math.floorDiv(x + z, size) + Math.floorDiv(x - z, size), 2) == 0;
    }

    /** Zaun von a nach b (gerade Linie in x oder z). */
    static void fence(Plan p, int x1, int z1, int x2, int z2, int y, String fence) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                p.set(x, y, z, fence);
            }
        }
    }

    /** Blumen und Gras auf freien Wiesenblöcken innerhalb des Rechtecks; {@code keep} hält Flächen frei. */
    static void flowers(Plan p, int x1, int z1, int x2, int z2, int every, Random r,
            java.util.function.BiPredicate<Integer, Integer> keep) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                String d = p.get(x, 0, z);
                if (d == null || !d.startsWith("grass_block") || p.get(x, 1, z) != null || keep.test(x, z)
                        || r.nextInt(every) != 0) {
                    continue;
                }
                p.set(x, 1, z, FLOWERS[r.nextInt(FLOWERS.length)]);
            }
        }
    }

    // ---------- Banner in unseren Farben (eigene Muster, als Text „farbe:muster“) ----------

    /** Weiß-blaue Rauten. */
    static List<String> lozenges() {
        return List.of("light_blue:rhombus", "light_blue:triangles_top", "light_blue:triangles_bottom");
    }

    /** Smaragd: grün mit hellgrüner Raute und goldenem Rand. */
    static List<String> emerald() {
        return List.of("lime:rhombus", "green:circle", "yellow:border");
    }

    /** Rot-weiß gestreift (rotes Dorf). */
    static List<String> redWhite() {
        return List.of("white:stripe_center", "red:stripe_top", "red:stripe_bottom", "white:border");
    }

    /** Fahne in der Farbe eines Dorfs: weiß-blaue Rauten bzw. rot-weiß gestreift. */
    static List<String> flag(de.moinmorni.maibaumklau.logic.Side side) {
        return side == de.moinmorni.maibaumklau.logic.Side.BLUE ? lozenges() : redWhite();
    }

    /** Gold: gelb mit orangem Verlauf und Zackenrand. */
    static List<String> gold() {
        return List.of("orange:gradient_up", "yellow:stripe_middle", "orange:curly_border");
    }
}
