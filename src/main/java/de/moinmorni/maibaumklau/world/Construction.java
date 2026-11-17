package de.moinmorni.maibaumklau.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Baut Wirtsgarten und Karten aus den Bauplänen – nur wenn sich die Bau-Version geändert hat (gemerkt in
 * {@code bau.yml}) – und stellt nach jeder Runde die Spielfläche einer Karte wieder her: jeder Block, der nicht zum
 * Bauplan passt, wird zurückgesetzt (Leitern weg, Maibäume wieder da), herumliegende Dinge verschwinden. Die Baupläne
 * sind reiner Text und entstehen im Hintergrund; gesetzt wird Stück für Stück (höchstens {@link #BUDGET_NANOS} pro
 * Tick).
 */
public final class Construction {

    /** Bau-Version: erhöhen, wenn sich Wirtsgarten oder Karten ändern – dann wird beim nächsten Start neu gebaut. */
    public static final int VERSION = 1;
    private static final long BUDGET_NANOS = 25_000_000L;
    private static final int CHECK_EVERY = 256;
    /** Mit -Dmaibaumklau.debug=true nennt die Wiederherstellung die ersten zurückgesetzten Blöcke. */
    private static final boolean DEBUG = Boolean.getBoolean("maibaumklau.debug");

    private interface Job {
        boolean step(long deadline);
    }

    private final Plugin plugin;
    private final Logger log;
    private final World world;
    private final Path stateFile;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private final Map<String, BlockData> cache = new HashMap<>();
    private final Set<String> broken = new HashSet<>();
    private final Set<MapType> restoring = EnumSet.noneOf(MapType.class);
    private BukkitTask task;
    private volatile int planning;
    private int placed;
    private boolean building;
    private int lastRestored;

    public Construction(Plugin plugin, World world) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.world = world;
        this.stateFile = plugin.getDataFolder().toPath().resolve("bau.yml");
    }

    /** Wird gerade (neu) gebaut? */
    public boolean building() {
        return building;
    }

    /** Wird diese Karte gerade wiederhergestellt (oder gebaut)? */
    public boolean restoring(MapType m) {
        return restoring.contains(m);
    }

    /** Blöcke, die bei der letzten Wiederherstellung zurückgesetzt wurden (für Tests). */
    public int lastRestored() {
        return lastRestored;
    }

    /**
     * Baut, falls nötig (sonst Wiederherstellung beider Karten); {@code done} läuft auf dem Hauptthread, sobald alles
     * steht. Die Baupläne entstehen im Hintergrund – der Server läuft währenddessen weiter.
     */
    public void ensure(boolean force, Runnable done) {
        YamlConfiguration state = readState();
        restoring.addAll(EnumSet.allOf(MapType.class));
        if (!force && state.getInt("version") == VERSION) {
            // Falls der Server mitten in einer Runde anhielt: Karten trotzdem in Ordnung bringen.
            plans(plans -> {
                for (MapType m : MapType.values()) {
                    jobs.add(restoreJob(m, null));
                }
                jobs.add(deadline -> {
                    done.run();
                    return true;
                });
            });
            return;
        }
        building = true;
        log.info("Wirtsgarten und Karten werden gebaut (Bau-Version " + VERSION + ") …");
        long start = System.currentTimeMillis();
        List<String> boxes = new ArrayList<>();
        List<int[]> later = new ArrayList<>();
        for (String box : state.getStringList("bereiche")) {
            int[] b = parseBox(box);
            if (b == null) {
                continue;
            }
            if (Math.abs(b[0]) < 500 && Math.abs(b[3]) < 500) {
                jobs.add(clearJob(b));
            } else {
                later.add(b);
            }
        }
        Plan hall = HallPlan.build();
        boxes.add(boxOf(hall, 0, MapType.FLOOR_Y, 0));
        jobs.add(placeJob(hall, 0, MapType.FLOOR_Y, 0));
        runNow();
        later.forEach(b -> jobs.add(clearJob(b)));
        plans(plans -> {
            for (var e : plans.entrySet()) {
                MapType m = e.getKey();
                boxes.add(boxOf(e.getValue(), m.originX(), MapType.FLOOR_Y, m.originZ()));
                jobs.add(placeJob(e.getValue(), m.originX(), MapType.FLOOR_Y, m.originZ()));
                jobs.add(deadline -> {
                    m.forgetFullPlan();
                    restoring.remove(m);
                    return true;
                });
            }
            jobs.add(deadline -> {
                YamlConfiguration fresh = new YamlConfiguration();
                fresh.set("version", VERSION);
                fresh.set("bereiche", boxes);
                writeState(fresh);
                building = false;
                log.info("Fertig gebaut: " + placed + " Blöcke in " + (System.currentTimeMillis() - start) / 1000 + " s.");
                done.run();
                return true;
            });
        });
    }

    /** Baupläne aller Karten im Hintergrund erstellen, dann {@code then} auf dem Hauptthread. */
    private void plans(java.util.function.Consumer<Map<MapType, Plan>> then) {
        planning++;
        startTask();
        var scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskAsynchronously(plugin, () -> {
            Map<MapType, Plan> plans = new EnumMap<>(MapType.class);
            RuntimeException failure = null;
            try {
                for (MapType m : MapType.values()) {
                    m.playPlan();
                    plans.put(m, m.plan());
                }
            } catch (RuntimeException e) {
                failure = e;
            }
            RuntimeException err = failure;
            scheduler.runTask(plugin, () -> {
                planning--;
                if (err != null) {
                    log.log(Level.SEVERE, "Baupläne ließen sich nicht erstellen", err);
                }
                then.accept(plans);
                startTask();
            });
        });
    }

    /** Spielfläche einer Karte wiederherstellen; {@code done} (darf null sein) läuft danach. */
    public void restore(MapType m, Runnable done) {
        restoring.add(m);
        jobs.add(restoreJob(m, done));
        startTask();
    }

    /** Teile eines Plans sofort setzen (Maibaum wieder aufstellen) – an der Stelle der Karte. */
    public void placeNow(Plan part, MapType m) {
        Job job = placeJob(part, m.originX(), MapType.FLOOR_Y, m.originZ());
        job.step(Long.MAX_VALUE);
    }

    /** Teile eines Plans sofort wegnehmen (Maibaum umlegen). */
    public void removeNow(Plan part, MapType m) {
        part.forEachAt((x, y, z, s) -> world.getBlockAt(m.originX() + x, MapType.FLOOR_Y + y, m.originZ() + z)
                .setType(Material.AIR, false));
    }

    private void startTask() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    private void runNow() {
        while (!jobs.isEmpty()) {
            Job job = jobs.peekFirst();
            if (job.step(Long.MAX_VALUE)) {
                jobs.pollFirst();
            }
        }
    }

    private void tick() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            Job job = jobs.peekFirst();
            boolean finished;
            try {
                finished = job.step(deadline);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Bauschritt fehlgeschlagen – wird übersprungen", e);
                finished = true;
            }
            if (finished) {
                jobs.pollFirst();
            }
        }
        if (jobs.isEmpty() && task != null && planning == 0) {
            task.cancel();
            task = null;
        }
    }

    /** Blockzustand aus Text; unbekannte Namen werden einmal laut gemeldet und ausgelassen. */
    private BlockData data(String spec) {
        BlockData d = cache.get(spec);
        if (d != null || broken.contains(spec)) {
            return d;
        }
        try {
            d = Bukkit.createBlockData(spec.indexOf(':') >= 0 ? spec : "minecraft:" + spec);
            cache.put(spec, d);
        } catch (IllegalArgumentException e) {
            broken.add(spec);
            log.severe("Unbekannter Block im Bauplan: " + spec + " – wird ausgelassen.");
        }
        return d;
    }

    private Job placeJob(Plan plan, int ox, int oy, int oz) {
        List<Long> keys = new ArrayList<>(plan.size());
        List<String> specs = new ArrayList<>(plan.size());
        plan.forEach((k, s) -> {
            keys.add(k);
            specs.add(s);
        });
        List<Plan.Banner> banners = plan.banners();
        int[] index = {0};
        return deadline -> {
            int i = index[0];
            int n = 0;
            while (i < keys.size()) {
                long k = keys.get(i);
                BlockData d = data(specs.get(i));
                if (d != null) {
                    world.getBlockAt(ox + Plan.keyX(k), oy + Plan.keyY(k), oz + Plan.keyZ(k)).setBlockData(d, false);
                    placed++;
                }
                i++;
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    index[0] = i;
                    return false;
                }
            }
            index[0] = i;
            for (Plan.Banner b : banners) {
                patterns(world.getBlockAt(ox + b.x(), oy + b.y(), oz + b.z()), b);
            }
            return true;
        };
    }

    private void patterns(Block block, Plan.Banner b) {
        BlockState st = block.getState();
        if (st instanceof org.bukkit.block.Banner banner) {
            banner.setPatterns(patterns(b.patterns()));
            banner.update(true, false);
        }
    }

    /** {@code "lime:rhombus"} → Muster. */
    private List<Pattern> patterns(List<String> specs) {
        var registry = io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.BANNER_PATTERN);
        List<Pattern> out = new ArrayList<>();
        for (String s : specs) {
            String[] parts = s.split(":");
            try {
                PatternType type = registry.get(NamespacedKey.minecraft(parts[1]));
                if (type != null) {
                    out.add(new Pattern(DyeColor.valueOf(parts[0].toUpperCase(Locale.ROOT)), type));
                }
            } catch (RuntimeException e) {
                log.warning("Bannermuster unbekannt: " + s);
            }
        }
        return out;
    }

    /**
     * Wiederherstellung: Chunk für Chunk eine Momentaufnahme lesen, mit dem Bauplan vergleichen und nur abweichende
     * Blöcke setzen. Leere Abschnitte (16 Blöcke hoch), in denen auch der Plan nichts hat, werden übersprungen.
     */
    private Job restoreJob(MapType m, Runnable done) {
        Plan plan = m.playPlan();
        int[] b = m.playBox();
        int ox = m.originX();
        int oy = MapType.FLOOR_Y;
        int oz = m.originZ();
        int x1 = ox + b[0];
        int x2 = ox + b[3];
        int y1 = Math.max(world.getMinHeight(), oy + b[1]);
        int y2 = Math.min(world.getMaxHeight() - 1, oy + b[4]);
        int z1 = oz + b[2];
        int z2 = oz + b[5];
        int minH = world.getMinHeight();
        Set<Long> planSections = new HashSet<>();
        plan.forEachAt((x, y, z, s) -> planSections.add(section((ox + x) >> 4, (oy + y - minH) >> 4, (oz + z) >> 4)));
        List<int[]> chunks = new ArrayList<>();
        for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) {
            for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
                chunks.add(new int[] {cx, cz});
            }
        }
        int[] index = {0};
        int[] changed = {0};
        return deadline -> {
            while (index[0] < chunks.size()) {
                int[] c = chunks.get(index[0]++);
                Chunk chunk = world.getChunkAt(c[0], c[1]);
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) {
                        e.remove();
                    }
                }
                ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
                for (int sy = (y1 - minH) >> 4; sy <= (y2 - minH) >> 4; sy++) {
                    if (snap.isSectionEmpty(sy) && !planSections.contains(section(c[0], sy, c[1]))) {
                        continue;
                    }
                    int from = Math.max(y1, minH + sy * 16);
                    int to = Math.min(y2, minH + sy * 16 + 15);
                    for (int lx = 0; lx < 16; lx++) {
                        int x = (c[0] << 4) + lx;
                        if (x < x1 || x > x2) {
                            continue;
                        }
                        for (int lz = 0; lz < 16; lz++) {
                            int z = (c[1] << 4) + lz;
                            if (z < z1 || z > z2) {
                                continue;
                            }
                            for (int y = from; y <= to; y++) {
                                String spec = plan.get(x - ox, y - oy, z - oz);
                                BlockData want = spec == null ? null : data(spec);
                                BlockData cur = snap.getBlockData(lx, y, lz);
                                if (want == null) {
                                    if (!cur.getMaterial().isAir()) {
                                        if (changed[0] < 8 && DEBUG) {
                                            log.info("Weggeräumt bei " + x + " " + y + " " + z + ": " + cur.getAsString());
                                        }
                                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                                        changed[0]++;
                                    }
                                } else if (!cur.equals(want)) {
                                    if (changed[0] < 8 && DEBUG) {
                                        log.info("Zurückgesetzt bei " + x + " " + y + " " + z + ": " + cur.getAsString()
                                                + " → " + want.getAsString());
                                    }
                                    Block block = world.getBlockAt(x, y, z);
                                    block.setBlockData(want, false);
                                    Plan.Banner banner = plan.bannerAt(x - ox, y - oy, z - oz);
                                    if (banner != null) {
                                        patterns(block, banner);
                                    }
                                    changed[0]++;
                                }
                            }
                        }
                    }
                }
                if (System.nanoTime() > deadline) {
                    return index[0] >= chunks.size() && finish(m, changed[0], done);
                }
            }
            return finish(m, changed[0], done);
        };
    }

    private boolean finish(MapType m, int changed, Runnable done) {
        restoring.remove(m);
        lastRestored = changed;
        if (changed > 0) {
            log.info("Karte " + m.id() + " wiederhergestellt: " + changed + " Blöcke zurückgesetzt.");
        }
        if (done != null) {
            done.run();
        }
        return true;
    }

    private static long section(int cx, int sy, int cz) {
        return ((long) (cx & 0xFFFFF) << 40) | ((long) (sy & 0xFFFF) << 20) | (cz & 0xFFFFF);
    }

    /** Alten Bau abräumen: Chunk für Chunk, leere Abschnitte werden übersprungen. */
    private Job clearJob(int[] b) {
        int minH = world.getMinHeight();
        int y1 = Math.max(minH, b[1]);
        int y2 = Math.min(world.getMaxHeight() - 1, b[4]);
        List<int[]> chunks = new ArrayList<>();
        for (int cx = b[0] >> 4; cx <= b[3] >> 4; cx++) {
            for (int cz = b[2] >> 4; cz <= b[5] >> 4; cz++) {
                chunks.add(new int[] {cx, cz});
            }
        }
        int[] index = {0};
        return deadline -> {
            while (index[0] < chunks.size()) {
                int[] c = chunks.get(index[0]++);
                Chunk chunk = world.getChunkAt(c[0], c[1]);
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) {
                        e.remove();
                    }
                }
                ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
                for (int sy = (y1 - minH) >> 4; sy <= (y2 - minH) >> 4; sy++) {
                    if (snap.isSectionEmpty(sy)) {
                        continue;
                    }
                    int from = Math.max(y1, minH + sy * 16);
                    int to = Math.min(y2, minH + sy * 16 + 15);
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int y = from; y <= to; y++) {
                                if (!snap.getBlockType(lx, y, lz).isAir()) {
                                    world.getBlockAt((c[0] << 4) + lx, y, (c[1] << 4) + lz).setType(Material.AIR, false);
                                }
                            }
                        }
                    }
                }
                if (System.nanoTime() > deadline) {
                    return index[0] >= chunks.size();
                }
            }
            return true;
        };
    }

    private static String boxOf(Plan plan, int ox, int oy, int oz) {
        int[] b = plan.bounds();
        return (ox + b[0]) + " " + (oy + b[1]) + " " + (oz + b[2]) + " " + (ox + b[3]) + " " + (oy + b[4]) + " "
                + (oz + b[5]);
    }

    private static int[] parseBox(String s) {
        String[] parts = s.trim().split("\\s+");
        if (parts.length != 6) {
            return null;
        }
        int[] b = new int[6];
        try {
            for (int i = 0; i < 6; i++) {
                b[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return b;
    }

    private YamlConfiguration readState() {
        YamlConfiguration y = new YamlConfiguration();
        if (Files.isRegularFile(stateFile)) {
            try {
                y.loadFromString(Files.readString(stateFile, StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException e) {
                log.warning("bau.yml ließ sich nicht lesen – es wird neu gebaut: " + e.getMessage());
            }
        }
        return y;
    }

    private void writeState(YamlConfiguration y) {
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, y.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.log(Level.WARNING, "bau.yml ließ sich nicht schreiben", e);
        }
    }
}
