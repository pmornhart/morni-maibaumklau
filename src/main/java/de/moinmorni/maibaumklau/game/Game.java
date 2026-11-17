package de.moinmorni.maibaumklau.game;

import de.moinmorni.maibaumklau.Items;
import de.moinmorni.maibaumklau.MaibaumklauPlugin;
import de.moinmorni.maibaumklau.Settings;
import de.moinmorni.maibaumklau.Texts;
import de.moinmorni.maibaumklau.Ui;
import de.moinmorni.maibaumklau.data.PlayerData;
import de.moinmorni.maibaumklau.logic.Comeback;
import de.moinmorni.maibaumklau.logic.Cooldowns;
import de.moinmorni.maibaumklau.logic.Help;
import de.moinmorni.maibaumklau.logic.Outcome;
import de.moinmorni.maibaumklau.logic.Pole;
import de.moinmorni.maibaumklau.logic.Rules;
import de.moinmorni.maibaumklau.logic.Score;
import de.moinmorni.maibaumklau.logic.Side;
import de.moinmorni.maibaumklau.world.MapLayout;
import de.moinmorni.maibaumklau.world.MapType;
import de.moinmorni.maibaumklau.world.Maypole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Eine Runde Maibaumklau auf einer Karte. Zwei Dörfer, zwei Maibäume: Wer den Baum des anderen Dorfs umlegt und ins
 * eigene Dorf trägt, gewinnt. Träger sind langsam (mit mehr Leuten schneller); wer einen Träger berührt oder trifft,
 * fängt ihn ab – der Baum liegt dann da und gilt nach einer Weile wieder als zurück. Auf der eigenen Hälfte fängt man
 * Eindringlinge ab, ohne dass jemand Schaden nimmt. Läuft die Zeit ab, gewinnen die Punkte. Am Ende wird der Baum – wie
 * es der Brauch will – mit einer Brotzeit ausgelöst.
 */
public final class Game {

    public enum Phase {
        START, PLAY, OVER
    }

    /** Warum ein Baum wieder im Dorf steht. */
    private enum Back {
        TIME, WATER, GORGE
    }

    /** Ein Untergrund unter einer Stelle: wo man drauf liegt, und ob es Wasser ist. */
    private record Ground(Location at, boolean water) {
    }

    /** Eine Leiter, die jemand gesetzt hat – verschwindet zur angegebenen Zeit wieder. */
    private record Ladder(Block block, long until) {
    }

    public static final String FIREWORK_TAG = "maibaumklau_feuerwerk";
    public static final String PROP_TAG = "maibaumklau_brotzeit";
    private static final int SECOND = 20;
    /** So oft (Ticks) werden Umlegen, Tragen, Berühren und Abstürzen geprüft. */
    private static final int STEP = 5;
    private static final double STEP_SECONDS = STEP / (double) SECOND;
    private static final int COUNTDOWN = 3;
    /** Wer sich mehr bewegt, ist teleportiert worden – das zählt nicht als getragene Strecke. */
    private static final double MAX_STEP_BLOCKS = 3.0;
    private static final double SPECTATOR_RANGE = 90;
    private static final double HAY_FORWARD = 1.05;
    private static final double HAY_UP = 0.75;
    private static final int PAD_PAUSE = 20;
    private static final int WARN_MINUTE = 60;
    private static final int WARN_END = 10;
    private static final double FEAST_RADIUS = 4.0;
    /** Eine kleine eigene Melodie zum Auslösen: (Tick, Ton, Höhe). */
    private static final Object[][] TUNE = {{0, "flute", 1.0f}, {4, "flute", 1.26f}, {8, "flute", 1.5f},
        {12, "bell", 2.0f}, {20, "flute", 1.5f}, {24, "flute", 1.26f}, {28, "flute", 1.5f}, {32, "bell", 1.68f},
        {40, "flute", 1.33f}, {44, "flute", 1.12f}, {48, "flute", 1.33f}, {52, "flute", 1.5f}, {60, "bell", 2.0f},
        {64, "guitar", 1.0f}, {68, "guitar", 1.5f}, {72, "bell", 2.0f}};

    private final MaibaumklauPlugin plugin;
    private final MapType map;
    private final World world;
    private final MapLayout layout;
    private final Settings settings;
    private final Rules rules;
    private final Random random = new Random();
    private final NamespacedKey carryKey;
    private final Map<UUID, Villager> villagers = new LinkedHashMap<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Pole[] poles = {new Pole(Side.BLUE), new Pole(Side.RED)};
    private final TreeModel[] models = new TreeModel[2];
    private final Location[] treeAt = new Location[2];
    private final float[] treeYaw = new float[2];
    private final UUID[] leaders = new UUID[2];
    /** Wurde das Dorf schon gewarnt, dass sein Baum umgelegt wird (einmal je Versuch)? */
    private final boolean[] alarmed = new boolean[2];
    private final List<Set<UUID>> carriers = List.of(new LinkedHashSet<>(), new LinkedHashSet<>());
    private final int[] points = new int[2];
    private final Map<UUID, Location> lastPos = new HashMap<>();
    private final Map<UUID, Location> startSpots = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Long> padUsed = new HashMap<>();
    private final Map<UUID, Long> hinted = new HashMap<>();
    private final List<Ladder> ladders = new ArrayList<>();
    private final List<Entity> props = new ArrayList<>();
    private final Comeback comeback = new Comeback();
    private final Cooldowns cooldowns = new Cooldowns();
    /** Wem das Netzwerk gerade „spielt“ glaubt (nur Änderungen werden gemeldet). */
    private final Set<UUID> told = new HashSet<>();
    private Phase phase = Phase.START;
    private int phaseTicks;
    private long ticks;
    private int secondsLeft;
    private boolean paused;
    private Outcome outcome;
    private Location feastCenter;

    public Game(MaibaumklauPlugin plugin, MapType map, List<Player> blue, List<Player> red) {
        this.plugin = plugin;
        this.map = map;
        this.world = plugin.world();
        this.layout = map.layout();
        this.settings = plugin.settings();
        this.rules = settings.rules();
        this.carryKey = plugin.carryKey();
        this.secondsLeft = settings.roundSeconds();
        List<List<Player>> sides = List.of(blue, red);
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < sides.get(s).size(); i++) {
                Player p = sides.get(s).get(i);
                villagers.put(p.getUniqueId(), new Villager(p.getUniqueId(), p.getName(), Side.of(s), i));
                plugin.store().pin(p.getUniqueId());
            }
        }
        phaseTicks = settings.startSeconds() * SECOND;
        Map<String, Integer> colours = nameColours();
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p == null) {
                continue;
            }
            Location spot = startSpot(v);
            startSpots.put(v.uuid(), spot);
            p.teleport(spot);
            prepare(p, GameMode.ADVENTURE);
            armour(p, v);
            showBar(p);
            plugin.boards().sidebar().teams(p, colours);
            tellPlaying(p, true);
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "game.welcome", village(lang, v.side()),
                    Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id()))));
            p.sendMessage(Texts.msg(lang, "game.goal", village(lang, v.side().other())));
            p.sendMessage(Texts.msg(lang, "game.team", Placeholder.unparsed("names", names(v.side()))));
            Ui.title(p, Texts.text(lang, "game.title"), Texts.text(lang, "game.start.sub", village(lang, v.side())), 2500);
        }
        updateBars();
        plugin.boards().game(this);
    }

    // ---------- Zugriff ----------

    public MapType map() {
        return map;
    }

    public Phase phase() {
        return phase;
    }

    public int secondsLeft() {
        return secondsLeft;
    }

    public Villager villager(UUID uuid) {
        return villagers.get(uuid);
    }

    public Map<UUID, Villager> villagers() {
        return villagers;
    }

    public boolean spectating(UUID uuid) {
        return spectators.contains(uuid);
    }

    public Pole pole(Side s) {
        return poles[s.index()];
    }

    public int points(Side s) {
        return points[s.index()];
    }

    /** Wartet die Runde gerade auf diese Person (Verbindung verloren, Platz noch frei)? */
    public boolean waitingFor(UUID uuid) {
        return comeback.waitingFor(uuid);
    }

    /** Leute, die gerade mitspielen. */
    public int activeCount() {
        return (int) villagers.values().stream().filter(Villager::active).count();
    }

    /** Trägt diese Person gerade einen Baum? */
    public boolean carrying(UUID uuid) {
        return carriers.get(0).contains(uuid) || carriers.get(1).contains(uuid);
    }

    Location loc(double x, double y, double z) {
        return new Location(world, map.originX() + x, MapType.FLOOR_Y + y, map.originZ() + z);
    }

    private double relX(Location l) {
        return l.getX() - map.originX();
    }

    private double relZ(Location l) {
        return l.getZ() - map.originZ();
    }

    /** Liegt die Stelle (Weltkoordinaten) innerhalb des Zauns dieser Karte? */
    public boolean inside(Location l) {
        return l.getWorld() == world && Math.abs(relX(l)) < layout.halfX() && Math.abs(relZ(l)) < layout.halfZ();
    }

    /** Gehört die Stelle (grob) zu dieser Karte? */
    public boolean onMap(Location l) {
        return l.getWorld() == world && Math.abs(relX(l)) < 300 && Math.abs(relZ(l)) < 300;
    }

    /** Wo man startet: vor dem Wirtshaus, je vier nebeneinander, die zweite Reihe einen Schritt näher am Platz. */
    private Location startSpot(Villager v) {
        Side s = v.side();
        int dir = s == Side.BLUE ? 1 : -1;
        int i = v.index();
        double x = layout.spawnX(s) + dir * (i / 4) * 1.5;
        double z = layout.spawnZ(s) + ((i % 4) - 1.5) * 1.5;
        Location l = loc(x, 1, z);
        l.setYaw(layout.spawnYaw(s));
        if (!l.getBlock().isPassable() || !l.clone().add(0, 1, 0).getBlock().isPassable()) {
            l = loc(layout.spawnX(s), 1, layout.spawnZ(s));
            l.setYaw(layout.spawnYaw(s));
        }
        return l;
    }

    /** Wo Zuschauer anfangen: hoch über der Mitte, mit Blick über beide Dörfer. */
    public Location spectatorSpot() {
        Location l = loc(0.5, 26, 34.5);
        l.setYaw(180f);
        l.setPitch(32f);
        return l;
    }

    /** Fuß eines Maibaums (Mitte des Blocks, auf dem Sockel). */
    private Location poleBase(Side s) {
        return loc(layout.poleX(s) + 0.5, 1, layout.poleZ(s) + 0.5);
    }

    /** Für /maibaumadmin status (Tests, Verwaltung). */
    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "karte=%s phase=%s zeit=%d angehalten=%s punkte=%d:%d", map.id(), phase,
                secondsLeft, paused, points[0], points[1]));
        for (Side s : Side.values()) {
            Pole pole = poles[s.index()];
            Location at = treeAt[s.index()];
            String leader = leaders[s.index()] == null ? "-" : villagers.get(leaders[s.index()]).name();
            sb.append(String.format(Locale.ROOT, " baum%d=%s:%.2f:%s:%s:%.1f:%d:%.2f", s.index(), pole.state(), pole.lift(),
                    pole.liftState(), leader, pole.dropLeft(), carriers.get(s.index()).size(), pole.best()));
            Location base = poleBase(s);
            sb.append(String.format(Locale.ROOT, " fuss%d=%d:%d:%d", s.index(), base.getBlockX(), base.getBlockY(),
                    base.getBlockZ()));
            if (at != null) {
                sb.append(String.format(Locale.ROOT, " lage%d=%.1f:%.1f:%.1f", s.index(), at.getX(), at.getY(), at.getZ()));
            }
            Location spawn = loc(layout.spawnX(s), 1, layout.spawnZ(s));
            sb.append(String.format(Locale.ROOT, " start%d=%.1f:%.1f:%.1f", s.index(), spawn.getX(), spawn.getY(),
                    spawn.getZ()));
        }
        for (Villager v : villagers.values()) {
            String state = v.gone() ? "weg" : v.online() ? "da" : "fort";
            sb.append(String.format(Locale.ROOT, " %s=%s:%s:r%d:c%d:p%d:t%d:s%d:l%d:h%d:g%d:w%d", v.name(), v.side().id(),
                    state, v.resting(ticks) ? 1 : 0, carrying(v.uuid()) ? 1 : 0, v.points(), v.tags(), v.saves(), v.lifts(),
                    v.captures(), v.carried(), comeback.secondsLeft(v.uuid(), ticks)));
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                sb.append(' ').append(p.getName()).append("=zuschauer");
            }
        }
        return sb.toString();
    }

    /** Alle, die die Runde sehen: verbundene Mitspieler und Zuschauer. */
    public List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p != null && v.active()) {
                out.add(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Verbundene Leute eines Dorfs. */
    private List<Player> players(Side s) {
        List<Player> out = new ArrayList<>();
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p != null && v.active() && v.side() == s) {
                out.add(p);
            }
        }
        return out;
    }

    void broadcast(Function<String, Component> msg) {
        for (Player p : audience()) {
            p.sendMessage(msg.apply(plugin.lang(p)));
        }
    }

    private void soundAll(String key, float pitch) {
        for (Player p : audience()) {
            Ui.sound(p, key, pitch);
        }
    }

    /** Name eines Dorfs in seiner Farbe als Platzhalter {@code <village>} (bzw. {@code tag}). */
    public TagResolver village(String lang, Side s) {
        return village(lang, s, "village");
    }

    public TagResolver village(String lang, Side s, String tag) {
        return Placeholder.component(tag, Component.text(villageName(lang, s), TextColor.color(s.rgb())));
    }

    public String villageName(String lang, Side s) {
        return Texts.plain(lang, "village." + map.id() + "." + s.id());
    }

    private String names(Side s) {
        return villagers.values().stream().filter(v -> v.side() == s && !v.gone()).map(Villager::name)
                .collect(Collectors.joining(", "));
    }

    /** Namensfarben aller Mitspieler (Dorffarbe). */
    private Map<String, Integer> nameColours() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Villager v : villagers.values()) {
            out.put(v.name(), v.side().rgb());
        }
        return out;
    }

    /** Hinweis höchstens einmal pro Sekunde je Person (sonst flutet ein Hinweis bei jedem Schlag den Chat). */
    private boolean hintOnce(UUID u) {
        Long last = hinted.get(u);
        if (last != null && ticks - last < SECOND) {
            return false;
        }
        hinted.put(u, ticks);
        return true;
    }

    // ---------- Spieler ----------

    private void prepare(Player p, GameMode mode) {
        p.setGameMode(mode);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.getInventory().clear();
        p.clearActivePotionEffects();
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(8);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setExp(0);
        p.setLevel(0);
        p.setGlowing(false);
        speed(p, 1.0);
    }

    /** Tempo als Anteil des normalen (1 = normal). */
    private void speed(Player p, double factor) {
        AttributeInstance a = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (a == null) {
            return;
        }
        a.removeModifier(carryKey);
        if (factor < 0.999) {
            a.addModifier(new AttributeModifier(carryKey, factor - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                    EquipmentSlotGroup.ANY));
        }
    }

    /** Für Hall und Aufräumen: Tempo und Leuchten zurücksetzen. */
    public static void reset(Player p, NamespacedKey key) {
        AttributeInstance a = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (a != null) {
            a.removeModifier(key);
        }
        p.setGlowing(false);
    }

    /** Hut und Weste in der Farbe des Dorfs – so erkennt man auf einen Blick, wer zu wem gehört. */
    private void armour(Player p, Villager v) {
        String lang = plugin.lang(p);
        Component name = Texts.text(lang, "item.armour", village(lang, v.side()));
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(Items.dyed(Material.LEATHER_HELMET, v.side().rgb(), name));
        inv.setChestplate(Items.dyed(Material.LEATHER_CHESTPLATE, v.side().rgb(), name));
    }

    /** Hilfen in die Schnellleiste (nach dem Start und nach jedem Abfangen wieder voll). */
    private void kit(Player p, Villager v) {
        String lang = plugin.lang(p);
        PlayerInventory inv = p.getInventory();
        for (Help h : Help.values()) {
            int amount = h == Help.LADDER ? settings.ladders() : 1;
            if (amount <= 0) {
                inv.setItem(h.slot(), null);
                continue;
            }
            Material m = Material.matchMaterial(h.material());
            ItemStack item = Items.helpTag(Items.named(m == null ? Material.STICK : m, amount,
                    Texts.text(lang, "help." + h.id()), Texts.lines(lang, "help." + h.id() + ".lore")), h.id());
            inv.setItem(h.slot(), item);
        }
        armour(p, v);
    }

    // ---------- Verbindung weg und wieder da ----------

    /**
     * Wiederkommen nach einem Verbindungsabbruch: Ist der Platz noch frei ({@code rueckkehr-sekunden}), geht es im
     * selben Dorf weiter, genau dort, wo man war. Sonst (zu spät) zurück in den Wirtsgarten.
     */
    public void rejoin(Player p) {
        UUID u = p.getUniqueId();
        Villager v = villagers.get(u);
        if (v != null && comeback.waitingFor(u)) {
            if (comeback.back(u, ticks) && phase != Phase.OVER && !v.gone()) {
                comeBack(p, v);
                return;
            }
            tooLate(u);
            plugin.hall().enter(p);
            p.sendMessage(Texts.msg(plugin.lang(p), "game.too-late.self"));
            return;
        }
        if (v != null && !v.gone() && phase != Phase.OVER) {
            // Ohne freigehaltenen Platz (rueckkehr-sekunden = 0 kommt hier nicht an): wie zuschauen.
            spectate(p);
            return;
        }
        plugin.hall().enter(p);
    }

    /**
     * Verbindung weg. Wer einen Baum trug, legt ihn ab. Ist „Zurück ins Spiel“ an, bleibt der Platz im Dorf
     * {@code rueckkehr-sekunden} lang frei – so lange bietet die Lobby es an. Sonst ist man raus.
     */
    public void quit(Player p) {
        UUID u = p.getUniqueId();
        Villager v = villagers.get(u);
        spectators.remove(u);
        told.remove(u);
        hideBar(p);
        if (v == null || v.gone()) {
            return;
        }
        if (phase == Phase.OVER) {
            v.online(false);
            return;
        }
        letGo(p, v);
        speed(p, 1.0);
        p.setGlowing(false);
        int seconds = settings.comebackSeconds();
        v.online(false);
        if (Comeback.holds(seconds, true)) {
            comeback.away(u, ticks, seconds);
            plugin.net().holdSlot(u, seconds);
            broadcast(lang -> Texts.msg(lang, "game.away", Placeholder.unparsed("name", v.name()),
                    Placeholder.unparsed("secs", String.valueOf(seconds))));
        } else {
            v.gone(true);
            broadcast(lang -> Texts.msg(lang, "game.left", Placeholder.unparsed("name", v.name())));
            checkWalkover();
        }
        plugin.boards().game(this);
    }

    /** Rechtzeitig zurück: im selben Dorf weiter (Minecraft hat Ort und Sachen gespeichert). */
    private void comeBack(Player p, Villager v) {
        UUID u = v.uuid();
        v.online(true);
        plugin.net().holdSlot(u, 0);
        tellPlaying(p, true);
        showBar(p);
        plugin.boards().sidebar().teams(p, nameColours());
        String lang = plugin.lang(p);
        if (phase == Phase.START) {
            p.teleport(startSpots.getOrDefault(u, startSpot(v)));
            p.setGameMode(GameMode.ADVENTURE);
        } else {
            p.setGameMode(GameMode.SURVIVAL);
            if (Items.helpTag(p.getInventory().getItem(Help.STICK.slot())) == null) {
                kit(p, v);
            }
        }
        speed(p, 1.0);
        p.setGlowing(false);
        Ui.title(p, Texts.text(lang, "game.back.title"), Texts.text(lang, "game.back.sub", village(lang, v.side())), 2500);
        Ui.sound(p, "minecraft:entity.player.levelup", 1.2f);
        p.sendMessage(Texts.msg(lang, "game.back", village(lang, v.side())));
        for (Player o : audience()) {
            if (!o.equals(p)) {
                o.sendMessage(Texts.msg(plugin.lang(o), "game.back.all", Placeholder.unparsed("name", v.name())));
            }
        }
        plugin.boards().game(this);
    }

    /** Nicht rechtzeitig zurück: nicht mehr dabei (die Runde zählt trotzdem als gespielt). */
    private void tooLate(UUID u) {
        plugin.net().holdSlot(u, 0);
        Villager v = villagers.get(u);
        if (v == null || v.gone()) {
            return;
        }
        v.gone(true);
        v.late(true);
        v.online(false);
        if (phase != Phase.OVER) {
            broadcast(lang -> Texts.msg(lang, "game.too-late", Placeholder.unparsed("name", v.name())));
            checkWalkover();
        }
        plugin.boards().game(this);
    }

    /** Freiwillig gehen (/verlassen): raus aus dem Dorf; die Runde zählt als gespielt. */
    public void leave(Player p) {
        UUID u = p.getUniqueId();
        Villager v = villagers.get(u);
        spectators.remove(u);
        hideBar(p);
        tellPlaying(p, false);
        if (v == null || v.gone()) {
            return;
        }
        if (phase != Phase.OVER) {
            letGo(p, v);
            if (comeback.waitingFor(u)) {
                comeback.back(u, ticks);
                plugin.net().holdSlot(u, 0);
            }
            v.gone(true);
            v.online(false);
            broadcast(lang -> Texts.msg(lang, "game.left", Placeholder.unparsed("name", v.name())));
            checkWalkover();
        } else {
            v.online(false);
        }
        speed(p, 1.0);
        p.setGlowing(false);
    }

    /** Zuschauen (Leute aus dem Wirtsgarten oder Wiederkommende ohne Platz). */
    public void spectate(Player p) {
        if (villagers.get(p.getUniqueId()) == null) {
            spectators.add(p.getUniqueId());
        }
        prepare(p, GameMode.SPECTATOR);
        p.teleport(spectatorSpot());
        showBar(p);
        plugin.boards().sidebar().teams(p, nameColours());
        plugin.boards().game(this);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "spectate.start", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id()))));
    }

    /** Dem Netzwerk sagen, ob jemand gerade spielt (dann fragt „/lobby“ erst nach) – nur bei Änderungen. */
    private void tellPlaying(Player p, boolean playing) {
        if (playing ? told.add(p.getUniqueId()) : told.remove(p.getUniqueId())) {
            plugin.net().playing(p, playing);
        }
    }

    /** Runde vorbei oder abgebrochen: niemand muss mehr zurück, niemand spielt mehr. */
    private void releaseAll() {
        for (UUID u : comeback.clear()) {
            plugin.net().holdSlot(u, 0);
        }
        for (UUID u : new ArrayList<>(told)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                tellPlaying(p, false);
            }
        }
        told.clear();
    }

    private void checkWalkover() {
        if (phase == Phase.OVER) {
            return;
        }
        for (Side s : Side.values()) {
            boolean left = villagers.values().stream().anyMatch(v -> v.side() == s && !v.gone());
            if (!left) {
                end(Outcome.walkover(s.other()));
                return;
            }
        }
    }

    // ---------- Ablauf ----------

    /** Jeder Tick. false = Runde ist vorbei und kann weg. */
    public boolean tick() {
        ticks++;
        if (ticks % SECOND == 0 && phase != Phase.OVER && comeback.count() > 0) {
            for (UUID u : comeback.expired(ticks)) {
                tooLate(u);
            }
        }
        switch (phase) {
            case START -> tickStart();
            case PLAY -> tickPlay();
            case OVER -> {
                tickFeast();
                if (--phaseTicks <= 0) {
                    close();
                    return false;
                }
            }
        }
        return true;
    }

    private void tickStart() {
        phaseTicks--;
        if (ticks % STEP == 0) {
            for (Villager v : villagers.values()) {
                Player p = Bukkit.getPlayer(v.uuid());
                Location spot = startSpots.get(v.uuid());
                if (p == null || !v.active() || spot == null) {
                    continue;
                }
                Location l = p.getLocation();
                if (l.getWorld() != world || Math.hypot(l.getX() - spot.getX(), l.getZ() - spot.getZ()) > 0.9) {
                    Location back = spot.clone();
                    back.setYaw(l.getYaw());
                    back.setPitch(l.getPitch());
                    p.teleport(back);
                }
            }
        }
        if (phaseTicks % SECOND == 0) {
            int secs = phaseTicks / SECOND;
            if (secs > 0 && secs <= COUNTDOWN) {
                for (Player p : audience()) {
                    String lang = plugin.lang(p);
                    Ui.title(p, Texts.text(lang, "game.count", Placeholder.unparsed("n", String.valueOf(secs))),
                            Texts.text(lang, "game.count.sub"), 900);
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.0f + (COUNTDOWN - secs) * 0.15f);
                }
            }
            updateBars();
            plugin.boards().game(this);
        }
        if (phaseTicks <= 0) {
            begin();
        }
    }

    /** Los geht's: Überlebensmodus (für Leitern), Hilfen in die Schnellleiste. */
    private void begin() {
        phase = Phase.PLAY;
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p == null || !v.active()) {
                continue;
            }
            p.setGameMode(GameMode.SURVIVAL);
            kit(p, v);
            lastPos.put(v.uuid(), p.getLocation());
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "game.go"), Texts.text(lang, "game.go.sub"), 1800);
            Ui.sound(p, "minecraft:block.bell.use", 1.0f);
        }
        broadcast(lang -> Texts.msg(lang, "game.started"));
        updateBars();
        plugin.stateChanged();
    }

    private void tickPlay() {
        if (activeOnline() == 0) {
            paused = true;
            if (ticks % SECOND == 0) {
                updateBars();
            }
            return;
        }
        paused = false;
        for (Side s : Side.values()) {
            if (poles[s.index()].state() == Pole.State.CARRIED) {
                follow(s);
            }
        }
        pads();
        if (ticks % STEP == 0) {
            step();
        }
        if (phase == Phase.PLAY && ticks % SECOND == 0) {
            second();
        }
    }

    private int activeOnline() {
        return (int) villagers.values().stream().filter(Villager::active).count();
    }

    /** Alle Viertelsekunde: abstürzen, umlegen, tragen, berühren, heimbringen, aufheben, zurückholen. */
    private void step() {
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p != null && v.active() && p.getLocation().getY() < MapType.FLOOR_Y + layout.voidY()) {
                fell(p, v);
            } else if (p != null && v.active() && v.resting(ticks)) {
                long left = v.restLeft(ticks);
                p.sendActionBar(Texts.text(plugin.lang(p), "rest.bar",
                        Placeholder.unparsed("secs", String.valueOf((left + SECOND - 1) / SECOND))));
            }
        }
        for (Side s : Side.values()) {
            if (phase != Phase.PLAY) {
                return;
            }
            Pole pole = poles[s.index()];
            switch (pole.state()) {
                case HOME -> liftStep(s);
                case CARRIED -> carryStep(s);
                case DROPPED -> droppedStep(s);
            }
        }
    }

    /** Leute in der Nähe einer Stelle (waagrecht {@code radius}, senkrecht höchstens {@code height}), die mitspielen. */
    private List<Player> near(Location at, Side side, double radius, double height, boolean evenResting) {
        List<Player> out = new ArrayList<>();
        for (Villager v : villagers.values()) {
            if (v.side() != side || !v.active() || (!evenResting && v.resting(ticks))) {
                continue;
            }
            Player p = Bukkit.getPlayer(v.uuid());
            if (p == null || p.getWorld() != world || p.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            Location l = p.getLocation();
            if (Math.hypot(l.getX() - at.getX(), l.getZ() - at.getZ()) <= radius && Math.abs(l.getY() - at.getY()) <= height) {
                out.add(p);
            }
        }
        return out;
    }

    // ---------- Umlegen ----------

    private void liftStep(Side owner) {
        Pole pole = poles[owner.index()];
        Location base = poleBase(owner);
        List<Player> thieves = near(base, owner.other(), Rules.LIFT_RADIUS, 4, false);
        List<Player> guards = near(base, owner, Rules.LIFT_RADIUS, 4, false);
        if (pole.liftTick(thieves.size(), guards.size(), STEP_SECONDS, rules)) {
            lifted(owner, thieves);
            return;
        }
        if (pole.lift() == 0) {
            alarmed[owner.index()] = false;
        }
        if (pole.liftState() == Pole.Lift.WORKING) {
            int pct = (int) Math.floor(pole.lift() * 100);
            if (!alarmed[owner.index()]) {
                alarmed[owner.index()] = true;
                broadcastSide(owner, "lift.alarm.chat", village(owner.other()));
            }
            for (Player p : thieves) {
                p.sendActionBar(Texts.text(plugin.lang(p), "lift.working", Placeholder.unparsed("bar", Ui.bar(pole.lift(), 10)),
                        Placeholder.unparsed("pct", String.valueOf(pct))));
            }
            if (ticks % SECOND == 0) {
                for (Player p : players(owner)) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(Texts.text(lang, "lift.alarm", Placeholder.unparsed("pct", String.valueOf(pct))));
                    Ui.sound(p, "minecraft:block.bell.use", 1.4f);
                }
            }
        } else if (pole.liftState() == Pole.Lift.GUARDED) {
            for (Player p : thieves) {
                p.sendActionBar(Texts.text(plugin.lang(p), "lift.guarded"));
            }
        }
    }

    private Function<String, TagResolver> village(Side s) {
        return lang -> village(lang, s);
    }

    private void broadcastSide(Side side, String key, Function<String, TagResolver> resolver) {
        for (Player p : players(side)) {
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, key, resolver.apply(lang)));
        }
    }

    /** Der Baum fällt: Blöcke weg, das Modell liegt auf den Schultern der Diebe. */
    private void lifted(Side owner, List<Player> thieves) {
        Side thiefSide = owner.other();
        plugin.construction().removeNow(map.polePlan(owner), map);
        Location base = poleBase(owner);
        world.playSound(base, "minecraft:entity.zombie.break_wooden_door", 1.4f, 0.6f);
        world.playSound(base, "minecraft:block.wood.break", 1.4f, 0.8f);
        world.spawnParticle(Particle.BLOCK, base.clone().add(0, 3, 0), 60, 0.6, 3, 0.6, 0,
                Bukkit.createBlockData(Material.SPRUCE_LEAVES));
        int[] shares = Score.liftShares(thieves.size());
        for (int i = 0; i < thieves.size(); i++) {
            Villager v = villagers.get(thieves.get(i).getUniqueId());
            v.lifted(shares[i]);
            points[thiefSide.index()] += shares[i];
        }
        Player leader = thieves.get(0);
        leaders[owner.index()] = leader.getUniqueId();
        carriers.get(owner.index()).clear();
        thieves.forEach(t -> carriers.get(owner.index()).add(t.getUniqueId()));
        treeAt[owner.index()] = leader.getLocation();
        treeYaw[owner.index()] = leader.getLocation().getYaw();
        if (models[owner.index()] != null) {
            models[owner.index()].remove();
        }
        models[owner.index()] = new TreeModel(world, leader.getLocation().add(0, TreeModel.CARRY_HEIGHT, 0), owner);
        String names = thieves.stream().map(Player::getName).collect(Collectors.joining(", "));
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "lift.done", village(lang, thiefSide, "thieves"), village(lang, owner, "owners"),
                    Placeholder.unparsed("names", names)));
            Villager v = villagers.get(p.getUniqueId());
            if (v != null && v.side() == owner) {
                Ui.title(p, Texts.text(lang, "lift.title.own"), Texts.text(lang, "lift.sub.own"), 2000);
                Ui.sound(p, "minecraft:entity.ender_dragon.growl", 1.6f);
            } else if (v != null) {
                Ui.title(p, Texts.text(lang, "lift.title.thief"), Texts.text(lang, "lift.sub.thief", village(lang, thiefSide)),
                        2000);
                Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1.2f);
            }
        }
        carryStep(owner);
    }

    // ---------- Tragen ----------

    /** Jeden Tick: das Modell folgt dem vordersten Träger. */
    private void follow(Side owner) {
        int i = owner.index();
        Player leader = leaders[i] == null ? null : Bukkit.getPlayer(leaders[i]);
        if (leader == null || models[i] == null) {
            return;
        }
        Location l = leader.getLocation();
        float yaw = l.getYaw();
        treeAt[i] = l;
        treeYaw[i] = yaw;
        double rad = Math.toRadians(yaw);
        Location center = l.clone().add(Math.sin(rad) * TreeModel.CARRY_BACK, TreeModel.CARRY_HEIGHT,
                -Math.cos(rad) * TreeModel.CARRY_BACK);
        models[i].place(center, yaw);
    }

    private void carryStep(Side owner) {
        int i = owner.index();
        Side thiefSide = owner.other();
        Set<UUID> set = carriers.get(i);
        Player leader = leaders[i] == null ? null : Bukkit.getPlayer(leaders[i]);
        Villager lv = leaders[i] == null ? null : villagers.get(leaders[i]);
        if (leader == null || lv == null || !lv.active() || lv.resting(ticks)) {
            // Neuer vorderster Träger: wer sonst noch trägt; sonst fällt der Baum hin.
            Player next = null;
            for (UUID u : set) {
                Villager v = villagers.get(u);
                Player p = Bukkit.getPlayer(u);
                if (p != null && v != null && v.active() && !v.resting(ticks) && !u.equals(leaders[i])) {
                    next = p;
                    break;
                }
            }
            if (next == null) {
                drop(owner, treeAt[i] != null ? treeAt[i] : poleBase(owner));
                return;
            }
            leaders[i] = next.getUniqueId();
            leader = next;
        }
        Location at = leader.getLocation();
        // Wer nah genug am vordersten Träger ist, trägt mit.
        Set<UUID> now = new LinkedHashSet<>();
        now.add(leader.getUniqueId());
        for (Player p : near(at, thiefSide, Rules.CARRY_RADIUS, 3, false)) {
            now.add(p.getUniqueId());
        }
        for (UUID u : set) {
            if (!now.contains(u)) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    speed(p, 1.0);
                    p.setGlowing(false);
                }
            }
        }
        set.clear();
        set.addAll(now);
        double factor = rules.carrySpeed(set.size());
        int toGo = (int) Math.round(Math.max(0, layout.toPole(thiefSide, relX(at), relZ(at)) - layout.square()));
        for (UUID u : set) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) {
                continue;
            }
            speed(p, factor);
            p.setGlowing(true);
            Location last = lastPos.get(u);
            Location cur = p.getLocation();
            if (last != null && last.getWorld() == cur.getWorld()) {
                double moved = Math.hypot(cur.getX() - last.getX(), cur.getZ() - last.getZ());
                if (moved < MAX_STEP_BLOCKS) {
                    villagers.get(u).carriedFor(moved);
                }
            }
            lastPos.put(u, cur);
            p.sendActionBar(Texts.text(plugin.lang(p), "carry.bar", Placeholder.unparsed("n", String.valueOf(set.size())),
                    Placeholder.unparsed("speed", String.valueOf((int) Math.round(factor * 100))),
                    Placeholder.unparsed("dist", String.valueOf(toGo))));
        }
        // Ins Wasser gefallen? Dann treibt der Baum zurück.
        if (at.getBlock().getType() == Material.WATER && at.clone().add(0, 0.6, 0).getBlock().getType() == Material.WATER) {
            backHome(owner, Back.WATER);
            return;
        }
        // Fortschritt auf dem Weg heim: Punkte für jedes neue Zehntel.
        double progress = Pole.progress(layout.poleX(owner) + 0.5, layout.poleZ(owner) + 0.5,
                layout.poleX(thiefSide) + 0.5, layout.poleZ(thiefSide) + 0.5, relX(at), relZ(at));
        int steps = poles[i].advance(progress);
        if (steps > 0) {
            for (UUID u : set) {
                int add = Score.STEP_CARRIER * steps + (u.equals(leaders[i]) ? Score.STEP_LEADER * steps : 0);
                villagers.get(u).points(add);
                points[thiefSide.index()] += add;
            }
        }
        // Berührt eine Wache einen Träger, ist er abgefangen.
        for (Player guard : near(at, owner, Rules.CARRY_RADIUS + Rules.TOUCH_RADIUS + 1, 3, false)) {
            for (UUID u : new ArrayList<>(set)) {
                Player c = Bukkit.getPlayer(u);
                if (c == null) {
                    continue;
                }
                Location gl = guard.getLocation();
                Location cl = c.getLocation();
                if (Math.hypot(gl.getX() - cl.getX(), gl.getZ() - cl.getZ()) <= Rules.TOUCH_RADIUS
                        && Math.abs(gl.getY() - cl.getY()) < 1.6) {
                    tag(guard, c, true);
                    return;
                }
            }
        }
        // Heimgebracht?
        if (layout.onSquare(thiefSide, relX(at), relZ(at))) {
            if (rules.ownMustStand() && poles[thiefSide.index()].state() != Pole.State.HOME) {
                if (hintOnce(leader.getUniqueId())) {
                    leader.sendMessage(Texts.msg(plugin.lang(leader), "carry.own-must-stand"));
                }
                return;
            }
            capture(owner);
        }
    }

    /** Wer trägt, hört auf zu tragen (abgefangen, gegangen, abgestürzt) – war es der Letzte, fällt der Baum. */
    private void letGo(Player p, Villager v) {
        for (Side s : Side.values()) {
            Set<UUID> set = carriers.get(s.index());
            if (!set.remove(v.uuid())) {
                continue;
            }
            speed(p, 1.0);
            p.setGlowing(false);
            if (v.uuid().equals(leaders[s.index()])) {
                leaders[s.index()] = null;
                UUID next = set.stream().filter(u -> {
                    Villager o = villagers.get(u);
                    return o != null && o.active() && !o.resting(ticks) && Bukkit.getPlayer(u) != null;
                }).findFirst().orElse(null);
                if (next == null) {
                    drop(s, p.getLocation());
                } else {
                    leaders[s.index()] = next;
                }
            }
        }
    }

    /** Der Baum fällt hin (an {@code at}): liegt dort – oder treibt im Wasser bzw. stürzt in die Tiefe zurück. */
    private void drop(Side owner, Location at) {
        int i = owner.index();
        for (UUID u : carriers.get(i)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                speed(p, 1.0);
                p.setGlowing(false);
            }
        }
        carriers.get(i).clear();
        leaders[i] = null;
        Ground g = ground(at);
        if (g == null) {
            backHome(owner, Back.GORGE);
            return;
        }
        if (g.water()) {
            backHome(owner, Back.WATER);
            return;
        }
        poles[i].drop(rules);
        treeAt[i] = g.at();
        if (models[i] == null) {
            models[i] = new TreeModel(world, g.at(), owner);
        }
        models[i].place(g.at().clone().add(0, 0.3, 0), treeYaw[i]);
        world.playSound(g.at(), "minecraft:block.wood.fall", 1.4f, 0.6f);
        world.spawnParticle(Particle.BLOCK, g.at(), 30, 2, 0.2, 2, 0, Bukkit.createBlockData(Material.SPRUCE_PLANKS));
        int secs = (int) Math.ceil(rules.returnSeconds());
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "tree.dropped", village(lang, owner, "owners"),
                    Placeholder.unparsed("secs", String.valueOf(secs))));
        }
    }

    /** Erster feste Block (oder Wasser) unter der Stelle; null, wenn darunter nur die Tiefe ist. */
    private Ground ground(Location from) {
        int x = from.getBlockX();
        int z = from.getBlockZ();
        int bottom = MapType.FLOOR_Y + layout.voidY();
        for (int y = (int) Math.floor(from.getY() + 0.5); y > bottom; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType() == Material.WATER) {
                return new Ground(new Location(world, x + 0.5, y + 1, z + 0.5), true);
            }
            if (b.getType().isSolid()) {
                return new Ground(new Location(world, x + 0.5, y + 1, z + 0.5), false);
            }
        }
        return null;
    }

    // ---------- Liegender Baum ----------

    private void droppedStep(Side owner) {
        int i = owner.index();
        Pole pole = poles[i];
        Location at = treeAt[i];
        if (at == null) {
            backHome(owner, Back.TIME);
            return;
        }
        List<Player> guards = near(at, owner, Rules.PICKUP_RADIUS + 0.5, 3, false);
        if (pole.canPickUp()) {
            List<Player> thieves = near(at, owner.other(), Rules.PICKUP_RADIUS, 3, false);
            if (!thieves.isEmpty()) {
                Player picker = thieves.get(0);
                pole.pickUp();
                leaders[i] = picker.getUniqueId();
                carriers.get(i).clear();
                thieves.forEach(t -> carriers.get(i).add(t.getUniqueId()));
                world.playSound(at, "minecraft:item.armor.equip_leather", 1.4f, 0.7f);
                for (Player p : audience()) {
                    String lang = plugin.lang(p);
                    p.sendMessage(Texts.msg(lang, "tree.picked", Placeholder.unparsed("name", picker.getName()),
                            village(lang, owner, "owners")));
                }
                carryStep(owner);
                return;
            }
        }
        if (pole.dropTick(guards.size(), STEP_SECONDS)) {
            backHome(owner, Back.TIME);
            return;
        }
        if (ticks % SECOND == 0) {
            int secs = (int) Math.ceil(pole.dropLeft());
            for (Player p : guards) {
                p.sendActionBar(Texts.text(plugin.lang(p), "tree.guarding", Placeholder.unparsed("secs", String.valueOf(secs))));
            }
        }
    }

    /** Der Baum steht wieder in seinem Dorf. */
    private void backHome(Side owner, Back why) {
        int i = owner.index();
        poles[i].home();
        for (UUID u : carriers.get(i)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                speed(p, 1.0);
                p.setGlowing(false);
            }
        }
        carriers.get(i).clear();
        leaders[i] = null;
        treeAt[i] = null;
        if (models[i] != null) {
            models[i].remove();
            models[i] = null;
        }
        // Niemand soll im Stamm stecken: wer am Fuß steht, tritt einen Schritt zur Seite.
        Location base = poleBase(owner);
        for (Player p : world.getPlayers()) {
            Location l = p.getLocation();
            if (Math.abs(l.getX() - base.getX()) < 1.6 && Math.abs(l.getZ() - base.getZ()) < 1.6
                    && l.getY() > base.getY() - 1 && l.getY() < base.getY() + Maypole.CROWN_TOP) {
                Location out = base.clone().add(0, 0, 2.5);
                out.setYaw(l.getYaw());
                p.teleport(out);
            }
        }
        plugin.construction().placeNow(map.polePlan(owner), map);
        world.playSound(base, "minecraft:block.bell.use", 1.6f, 1.2f);
        String key = switch (why) {
            case TIME -> "tree.back";
            case WATER -> "tree.water";
            case GORGE -> "tree.gorge";
        };
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, key, village(lang, owner, "owners")));
        }
    }

    // ---------- Abfangen ----------

    /** Treffer mit Hand oder Stecken (ohne Schaden). */
    public void hit(Player attacker, Player victim) {
        tag(attacker, victim, false);
    }

    /**
     * Jemand fängt jemanden ab. Auf der eigenen Hälfte fängt man Eindringlinge ab, Träger überall. Wer abgefangen wird,
     * muss zurück zum Wirtshaus seines Dorfs und kurz verschnaufen; trug er den Baum, liegt der jetzt da (auch wenn
     * noch andere mittrugen – die können ihn nach kurzer Zeit wieder aufheben).
     */
    private void tag(Player tagger, Player victim, boolean touch) {
        if (phase != Phase.PLAY) {
            return;
        }
        Villager t = villagers.get(tagger.getUniqueId());
        Villager v = villagers.get(victim.getUniqueId());
        if (t == null || v == null || !t.active() || !v.active() || t.side() == v.side()) {
            return;
        }
        Location vl = victim.getLocation();
        boolean carried = carrying(v.uuid());
        Side half = layout.half(relX(vl), relZ(vl));
        if (!Rules.mayTag(t.side(), v.side(), half, carried, v.resting(ticks), t.resting(ticks))) {
            if (!touch && hintOnce(t.uuid())) {
                String lang = plugin.lang(tagger);
                String key = t.resting(ticks) ? "tag.resting-me" : v.resting(ticks) ? "tag.resting-them" : "tag.safe";
                tagger.sendActionBar(Texts.text(lang, key, Placeholder.unparsed("name", v.name())));
            }
            return;
        }
        Side owner = carried ? (carriers.get(0).contains(v.uuid()) ? Side.BLUE : Side.RED) : null;
        int add = carried ? Score.SAVE : Score.TAG;
        t.tagged(carried, add);
        points[t.side().index()] += add;
        v.wasCaught();
        Location at = vl.clone();
        if (carried) {
            // Wer einen Träger abfängt, legt den Baum ab – egal, wie viele sonst noch mittragen.
            drop(owner, at);
        }
        sendHome(victim, v, "tag");
        String tLang = plugin.lang(tagger);
        tagger.sendActionBar(Texts.text(tLang, carried ? "tag.bar.save" : "tag.bar", Placeholder.unparsed("name", v.name()),
                Placeholder.unparsed("points", String.valueOf(add))));
        Ui.sound(tagger, "minecraft:entity.experience_orb.pickup", 1.2f);
        world.playSound(at, "minecraft:entity.chicken.egg", 1f, 1.4f);
        world.spawnParticle(Particle.CLOUD, at.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            if (carried) {
                p.sendMessage(Texts.msg(lang, "tag.save", Placeholder.unparsed("tagger", t.name()),
                        Placeholder.unparsed("victim", v.name()), village(lang, owner, "owners")));
            } else {
                p.sendMessage(Texts.msg(lang, "tag.done", Placeholder.unparsed("tagger", t.name()),
                        Placeholder.unparsed("victim", v.name())));
            }
        }
    }

    /** Zurück zum Wirtshaus des eigenen Dorfs und kurz verschnaufen (Hilfen wieder voll). */
    private void sendHome(Player p, Villager v, String why) {
        Location spot = startSpot(v);
        p.teleport(spot);
        p.setFallDistance(0);
        p.setVelocity(new Vector());
        v.rest(ticks + (long) (rules.restSeconds() * SECOND));
        kit(p, v);
        lastPos.put(v.uuid(), spot);
        String lang = plugin.lang(p);
        Ui.title(p, Texts.text(lang, why + ".title"), Texts.text(lang, why + ".sub"), 1500);
        Ui.sound(p, "minecraft:entity.villager.no", 1.0f);
    }

    /** In die Tiefe gefallen: zurück ins eigene Dorf; trug man den Baum, steht er wieder zu Hause. */
    private void fell(Player p, Villager v) {
        letGo(p, v);
        sendHome(p, v, "fall");
        broadcast(lang -> Texts.msg(lang, "fall.msg", Placeholder.unparsed("name", v.name())));
    }

    // ---------- Hilfen ----------

    /** Rechtsklick mit einer Hilfe aus der Schnellleiste. true = erledigt (Klick nicht weitergeben). */
    public boolean use(Player p, Help help) {
        Villager v = villagers.get(p.getUniqueId());
        if (v == null || !v.active() || phase != Phase.PLAY || help == Help.STICK || help == Help.LADDER) {
            return help != Help.LADDER;
        }
        String lang = plugin.lang(p);
        if (help == Help.COMPASS) {
            p.sendMessage(Texts.msg(lang, "help.kompass.state", Placeholder.component("own", stateOf(lang, v.side())),
                    Placeholder.component("enemy", stateOf(lang, v.side().other()))));
            return true;
        }
        if (carrying(v.uuid())) {
            p.sendActionBar(Texts.text(lang, "carry.no-help"));
            return true;
        }
        if (v.resting(ticks)) {
            p.sendActionBar(Texts.text(lang, "tag.resting-me"));
            return true;
        }
        if (!cooldowns.ready(v.uuid(), help, ticks)) {
            p.sendActionBar(Texts.text(lang, "help.wait",
                    Placeholder.unparsed("secs", String.valueOf(cooldowns.secondsLeft(v.uuid(), help, ticks)))));
            return true;
        }
        if (help == Help.SNACK) {
            int ticksOn = settings.snackSeconds() * SECOND;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticksOn, 1, false, true, true));
            cooldowns.use(v.uuid(), help, ticks, (long) settings.snackPause() * SECOND);
            p.setCooldown(Material.BREAD, settings.snackPause() * SECOND);
            p.sendActionBar(Texts.text(lang, "help.brotzeit.used"));
            Ui.sound(p, "minecraft:entity.generic.eat", 1.1f);
        } else if (help == Help.HAY) {
            Vector dir = p.getLocation().getDirection().setY(0);
            if (dir.lengthSquared() < 1e-4) {
                dir = new Vector(0, 0, 1);
            }
            p.setVelocity(dir.normalize().multiply(HAY_FORWARD).setY(HAY_UP));
            p.setFallDistance(0);
            cooldowns.use(v.uuid(), help, ticks, (long) settings.hayPause() * SECOND);
            p.setCooldown(Material.WHEAT, settings.hayPause() * SECOND);
            world.spawnParticle(Particle.BLOCK, p.getLocation(), 25, 0.4, 0.1, 0.4, 0,
                    Bukkit.createBlockData(Material.HAY_BLOCK));
            Ui.sound(p, "minecraft:block.grass.break", 0.8f);
            Ui.sound(p, "minecraft:entity.breeze.jump", 1.2f);
        }
        return true;
    }

    /** Zustand eines Baums als Text (Kompass, Leiste). */
    public Component stateOf(String lang, Side s) {
        Pole pole = poles[s.index()];
        return switch (pole.state()) {
            case HOME -> pole.liftState() == Pole.Lift.WORKING || pole.lift() > 0
                    ? Texts.text(lang, "state.lifting", Placeholder.unparsed("pct", String.valueOf((int) (pole.lift() * 100))))
                    : Texts.text(lang, "state.home");
            case CARRIED -> Texts.text(lang, "state.carried");
            case DROPPED -> Texts.text(lang, "state.dropped",
                    Placeholder.unparsed("secs", String.valueOf((int) Math.ceil(pole.dropLeft()))));
        };
    }

    /** Darf hier eine Leiter hin? Gemerkt wird sie, damit sie nach einer Weile verschwindet. */
    public boolean placeLadder(Player p, Block block) {
        Villager v = villagers.get(p.getUniqueId());
        if (v == null || !v.active() || phase != Phase.PLAY || !inside(block.getLocation())) {
            return false;
        }
        if (carrying(v.uuid())) {
            p.sendActionBar(Texts.text(plugin.lang(p), "carry.no-help"));
            return false;
        }
        ladders.add(new Ladder(block, ticks + (long) settings.ladderSeconds() * SECOND));
        return true;
    }

    /** Ist das eine Leiter aus dieser Runde (darf abgebaut werden)? */
    public boolean ourLadder(Block block) {
        return ladders.stream().anyMatch(l -> l.block().equals(block));
    }

    public void ladderGone(Block block) {
        ladders.removeIf(l -> l.block().equals(block));
    }

    /** Heuwagen auf der Karte: wer auf den Heuballen tritt, fliegt (nicht mit dem Baum). */
    private void pads() {
        if (layout.pads().isEmpty()) {
            return;
        }
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p == null || !v.active() || carrying(v.uuid()) || !p.isOnGround()) {
                continue;
            }
            Location l = p.getLocation();
            int bx = (int) Math.floor(relX(l));
            int bz = (int) Math.floor(relZ(l));
            int by = (int) Math.floor(l.getY() - 0.2) - MapType.FLOOR_Y;
            if (by != 0) {
                continue;
            }
            for (MapLayout.Pad pad : layout.pads()) {
                if (pad.x() == bx && pad.z() == bz && ticks - padUsed.getOrDefault(v.uuid(), -100L) > PAD_PAUSE) {
                    padUsed.put(v.uuid(), ticks);
                    p.setVelocity(new Vector(pad.vx(), pad.vy(), pad.vz()));
                    p.setFallDistance(0);
                    world.playSound(l, "minecraft:entity.breeze.jump", 1f, 0.9f);
                    world.spawnParticle(Particle.BLOCK, l, 25, 0.4, 0.1, 0.4, 0, Bukkit.createBlockData(Material.HAY_BLOCK));
                }
            }
        }
    }

    // ---------- Jede Sekunde ----------

    private void second() {
        secondsLeft--;
        if (secondsLeft == WARN_MINUTE) {
            broadcast(lang -> Texts.msg(lang, "time.minute"));
            soundAll("minecraft:block.note_block.bell", 0.8f);
        } else if (secondsLeft > 0 && secondsLeft <= WARN_END) {
            for (Player p : audience()) {
                String lang = plugin.lang(p);
                Ui.title(p, Component.empty(), Texts.text(lang, "time.left", Placeholder.unparsed("n", String.valueOf(secondsLeft))),
                        900);
                Ui.sound(p, "minecraft:block.note_block.hat", 1.4f);
            }
        }
        ladders.removeIf(l -> {
            if (ticks >= l.until()) {
                if (l.block().getType() == Material.LADDER) {
                    l.block().setType(Material.AIR, false);
                    world.spawnParticle(Particle.BLOCK, l.block().getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0,
                            Bukkit.createBlockData(Material.LADDER));
                }
                return true;
            }
            return false;
        });
        for (Side s : Side.values()) {
            Location at = treeAt[s.index()];
            if (at == null) {
                continue;
            }
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(s.rgb()), 2f);
            if (poles[s.index()].state() == Pole.State.DROPPED) {
                for (int y = 0; y < 12; y++) {
                    world.spawnParticle(Particle.DUST, at.getX(), at.getY() + y * 0.6, at.getZ(), 2, 0.1, 0.1, 0.1, 0, dust);
                }
            } else {
                world.spawnParticle(Particle.DUST, at.clone().add(0, 2.4, 0), 10, 0.6, 0.3, 0.6, 0, dust);
            }
        }
        for (Villager v : villagers.values()) {
            Player p = Bukkit.getPlayer(v.uuid());
            if (p != null && v.active()) {
                Location target = treeAt[v.side().other().index()];
                p.setCompassTarget(target != null ? target : poleBase(v.side().other()));
            }
        }
        for (UUID u : spectators) {
            keepNear(Bukkit.getPlayer(u));
        }
        updateBars();
        plugin.boards().game(this);
        if (secondsLeft <= 0) {
            end(Outcome.byPoints(points[0], points[1]));
        }
    }

    private void keepNear(Player p) {
        if (p == null || p.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        Location l = p.getLocation();
        if (l.getWorld() != world || Math.hypot(relX(l), relZ(l)) > SPECTATOR_RANGE
                || l.getY() < MapType.FLOOR_Y + layout.voidY() - 20) {
            p.teleport(spectatorSpot());
        }
    }

    /** Vorspulen (Verwaltung, Tests): {@code -1} = gleich losgehen, sonst die restliche Zeit setzen. */
    public void skipTo(int second) {
        if (phase == Phase.START) {
            phaseTicks = 1;
            return;
        }
        if (second >= 0) {
            secondsLeft = Math.max(1, second);
        }
    }

    // ---------- Ende und Auslösen ----------

    /** Den fremden Baum heimgebracht: Sieg! */
    private void capture(Side owner) {
        Side winners = owner.other();
        for (UUID u : carriers.get(owner.index())) {
            villagers.get(u).captured(Score.CAPTURE);
            points[winners.index()] += Score.CAPTURE;
        }
        end(Outcome.capture(winners));
    }

    /** Runde vorbei: Statistik, dann das Auslösen (Brotzeit) auf dem Platz der Sieger. */
    private void end(Outcome result) {
        if (phase == Phase.OVER) {
            return;
        }
        phase = Phase.OVER;
        outcome = result;
        phaseTicks = settings.afterSeconds() * SECOND;
        releaseAll();
        for (Side s : Side.values()) {
            for (UUID u : carriers.get(s.index())) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    speed(p, 1.0);
                    p.setGlowing(false);
                }
            }
            carriers.get(s.index()).clear();
            leaders[s.index()] = null;
            if (models[s.index()] != null && !(result.ransom() && s == result.winner().other())) {
                models[s.index()].remove();
                models[s.index()] = null;
            }
        }
        report();
        feast();
        updateBars();
        plugin.boards().game(this);
        plugin.stateChanged();
    }

    /**
     * Das Auslösen: Alle treffen sich auf dem Platz der Sieger (unentschieden: in der Mitte). Wurde ein Baum geklaut,
     * liegt er vor dem Platz, und das andere Dorf muss ihn – wie es der Brauch will – mit einer Brotzeit auslösen.
     */
    private void feast() {
        Side w = outcome.winner();
        Location center = w == null ? stand(0.5, 0.5) : poleBase(w);
        feastCenter = center;
        List<Player> all = audience();
        for (int i = 0; i < all.size(); i++) {
            Player p = all.get(i);
            double a = 2 * Math.PI * i / Math.max(1, all.size());
            Location spot = center.clone().add(Math.cos(a) * FEAST_RADIUS, 0, Math.sin(a) * FEAST_RADIUS);
            spot = stand(relX(spot), relZ(spot));
            Vector look = center.toVector().subtract(spot.toVector());
            spot.setDirection(look.lengthSquared() < 1e-4 ? new Vector(0, 0, 1) : look);
            spot.setPitch(10f);
            Villager v = villagers.get(p.getUniqueId());
            if (v != null) {
                prepare(p, GameMode.ADVENTURE);
                armour(p, v);
            }
            p.teleport(spot);
            p.setInvulnerable(true);
        }
        if (outcome.ransom() && models[w.other().index()] != null) {
            int dir = w == Side.BLUE ? 1 : -1;
            Location lie = center.clone().add(dir * 5.5, 0.3, 0);
            models[w.other().index()].place(stand(relX(lie), relZ(lie)).add(0, 0.3, 0), 0f);
        }
        props(center);
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Villager v = villagers.get(p.getUniqueId());
            boolean won = v != null && w != null && v.side() == w;
            if (w == null) {
                Ui.title(p, Texts.text(lang, "end.draw.title"), Texts.text(lang, "end.draw.sub"), 4000);
            } else if (won) {
                Ui.title(p, Texts.text(lang, "end.win.title"), Texts.text(lang, "end.win.sub." + outcome.key(),
                        village(lang, w.other())), 4000);
                Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1f);
            } else {
                Ui.title(p, Texts.text(lang, "end.lose.title", village(lang, w)), Texts.text(lang,
                        "end.lose.sub." + outcome.key(), village(lang, w)), 4000);
            }
            TagResolver[] r = {village(lang, w == null ? Side.BLUE : w, "winners"),
                village(lang, w == null ? Side.RED : w.other(), "losers"),
                Placeholder.unparsed("pa", String.valueOf(points[0])), Placeholder.unparsed("pb", String.valueOf(points[1])),
                village(lang, Side.BLUE, "vblue"), village(lang, Side.RED, "vred")};
            p.sendMessage(Texts.msg(lang, "end." + outcome.key(), r));
            p.sendMessage(Texts.msg(lang, outcome.ransom() ? "feast.ransom" : w == null ? "feast.draw" : "feast.points", r));
            if (v != null) {
                p.sendMessage(Texts.msg(lang, "end.mine", Placeholder.unparsed("points", String.valueOf(v.points())),
                        Placeholder.unparsed("tags", String.valueOf(v.tags() + v.saves())),
                        Placeholder.unparsed("lifts", String.valueOf(v.lifts())),
                        Placeholder.unparsed("carried", String.valueOf(v.carried()))));
            }
        }
        if (w != null) {
            for (int i = 0; i < 3; i++) {
                Location l = center.clone().add(random.nextInt(9) - 4, 1, random.nextInt(9) - 4);
                int delay = 10 + i * 14;
                Bukkit.getScheduler().runTaskLater(plugin, () -> fireworks(l, w), delay);
            }
        }
    }

    /** Brotzeit zum Auslösen: Fass, Brezn, Brotzeitbrett und Krüge schweben um den Fuß des Baums. */
    private void props(Location center) {
        String[] items = {"bread", "cooked_porkchop", "honey_bottle", "baked_potato", "bread", "honey_bottle", "cookie",
            "cooked_beef"};
        for (int i = 0; i < items.length; i++) {
            double a = 2 * Math.PI * i / items.length;
            Location l = center.clone().add(Math.cos(a) * 2.2, 1.3, Math.sin(a) * 2.2);
            Material m = Material.matchMaterial(items[i]);
            props.add(world.spawn(l, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(m == null ? Material.BREAD : m));
                d.setPersistent(false);
                d.addScoreboardTag(PROP_TAG);
                d.setBillboard(Display.Billboard.VERTICAL);
                d.setBrightness(new Display.Brightness(15, 15));
            }));
        }
        Location barrel = center.clone().add(2.6, 0, -2.6);
        props.add(world.spawn(barrel, org.bukkit.entity.BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData("minecraft:barrel[facing=up,open=false]"));
            d.setPersistent(false);
            d.addScoreboardTag(PROP_TAG);
        }));
    }

    /** Jeder Tick im Nachspiel: Brotzeit dreht sich, die Musik spielt, ab und zu ein „Prost!“. */
    private void tickFeast() {
        int t = settings.afterSeconds() * SECOND - phaseTicks;
        for (int i = 0; i < props.size(); i++) {
            Entity e = props.get(i);
            if (e instanceof ItemDisplay d && d.isValid() && feastCenter != null) {
                double a = 2 * Math.PI * i / Math.max(1, props.size() - 1) + t * 0.03;
                Location l = feastCenter.clone().add(Math.cos(a) * 2.2, 1.3 + Math.sin(t * 0.15 + i) * 0.15,
                        Math.sin(a) * 2.2);
                d.teleport(l);
            }
        }
        for (Object[] note : TUNE) {
            if ((int) note[0] + SECOND == t) {
                for (Player p : audience()) {
                    Ui.sound(p, "minecraft:block.note_block." + note[1], (float) note[2]);
                }
            }
        }
        if (t == 5 * SECOND) {
            Side w = outcome.winner();
            for (Player p : audience()) {
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, "feast.cheer", village(lang, w == null ? Side.BLUE : w)));
                Ui.sound(p, "minecraft:entity.villager.celebrate", 1f);
            }
        }
    }

    /** Stelle (relativ) auf dem obersten festen Block – zum Hinstellen der Leute beim Auslösen. */
    private Location stand(double x, double z) {
        Location l = loc(x, 12, z);
        for (int y = 12; y > layout.voidY(); y--) {
            Block b = world.getBlockAt(l.getBlockX(), MapType.FLOOR_Y + y, l.getBlockZ());
            Block above = b.getRelative(0, 1, 0);
            Block above2 = b.getRelative(0, 2, 0);
            if (b.getType().isSolid() && above.isPassable() && above2.isPassable() && b.getType() != Material.BARRIER) {
                return new Location(world, l.getBlockX() + 0.5, MapType.FLOOR_Y + y + 1, l.getBlockZ() + 0.5);
            }
        }
        return loc(x, 1, z);
    }

    private void fireworks(Location at, Side s) {
        world.spawn(at, Firework.class, fw -> {
            fw.addScoreboardTag(FIREWORK_TAG);
            var meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.fromRGB(s.rgb()))
                    .withFade(Color.WHITE).trail(true).build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        });
    }

    /** Statistik: lokal und ans Netzwerk. */
    private void report() {
        Side w = outcome.winner();
        for (Villager v : villagers.values()) {
            boolean won = w != null && v.side() == w;
            boolean draw = w == null;
            PlayerData d = plugin.store().get(v.uuid(), v.name());
            boolean best = d.add(new PlayerData.Round(won, draw, v.tags(), v.saves(), v.lifts(), v.captures(), v.carried(),
                    v.points()));
            plugin.store().save(d);
            plugin.board().update(v.uuid(), d.name(), d.wins(), d.captures(), d.points(), d.games());
            Map<String, Long> add = new LinkedHashMap<>();
            add.put("games", 1L);
            add.put("wins", won ? 1L : 0L);
            add.put("draws", draw ? 1L : 0L);
            add.put("tags", (long) v.tags());
            add.put("saves", (long) v.saves());
            add.put("lifts", (long) v.lifts());
            add.put("captures", (long) v.captures());
            add.put("carried", (long) v.carried());
            add.put("points", (long) v.points());
            plugin.net().stats(v.uuid(), add, Map.of("best_points", (long) v.points()));
            Player p = Bukkit.getPlayer(v.uuid());
            if (p != null && best && v.points() > 0 && v.active()) {
                p.sendMessage(Texts.msg(plugin.lang(p), "end.best", Placeholder.unparsed("n", String.valueOf(v.points()))));
            }
        }
        plugin.saveBoard();
    }

    /** Runde abbrechen oder fertig: alle zurück in den Wirtsgarten, Karte wiederherstellen. */
    public void close() {
        phase = Phase.OVER;
        releaseAll();
        for (Player p : audience()) {
            hideBar(p);
            p.setInvulnerable(false);
            speed(p, 1.0);
            p.setGlowing(false);
            plugin.hall().enter(p);
        }
        for (Villager v : villagers.values()) {
            plugin.store().unpin(v.uuid(), Bukkit.getPlayer(v.uuid()) != null);
        }
        spectators.clear();
        for (int i = 0; i < 2; i++) {
            if (models[i] != null) {
                models[i].remove();
                models[i] = null;
            }
        }
        props.forEach(Entity::remove);
        props.clear();
        ladders.clear();
        bars.clear();
        if (plugin.isEnabled()) {
            plugin.construction().restore(map, () -> plugin.boards().refreshHall());
        }
    }

    // ---------- Leiste oben ----------

    private void showBar(Player p) {
        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), u -> BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS));
        p.showBossBar(bar);
    }

    private void hideBar(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) {
            p.hideBossBar(bar);
        }
    }

    /** Jede Person sieht, was für sie zählt: Alarm um den eigenen Baum, Weg mit dem fremden, sonst die Zeit. */
    private void updateBars() {
        for (Player p : audience()) {
            BossBar bar = bars.get(p.getUniqueId());
            if (bar == null) {
                showBar(p);
                bar = bars.get(p.getUniqueId());
            }
            String lang = plugin.lang(p);
            Villager v = villagers.get(p.getUniqueId());
            float progress = phase == Phase.PLAY ? secondsLeft / (float) Math.max(1, settings.roundSeconds()) : 1f;
            Component name;
            BossBar.Color colour = BossBar.Color.GREEN;
            TagResolver time = Placeholder.unparsed("time", Ui.clock(secondsLeft));
            if (phase == Phase.START) {
                int secs = Math.max(0, (phaseTicks + SECOND - 1) / SECOND);
                name = Texts.text(lang, "bar.start", Placeholder.unparsed("secs", String.valueOf(secs)));
                colour = BossBar.Color.YELLOW;
            } else if (phase == Phase.OVER) {
                name = Texts.text(lang, "bar.over");
                colour = BossBar.Color.PINK;
            } else if (paused) {
                name = Texts.text(lang, "bar.paused");
                colour = BossBar.Color.WHITE;
            } else if (v != null && poles[v.side().index()].state() == Pole.State.CARRIED) {
                name = Texts.text(lang, "bar.alarm", time);
                colour = BossBar.Color.RED;
            } else if (v != null && poles[v.side().other().index()].state() == Pole.State.CARRIED) {
                Location at = treeAt[v.side().other().index()];
                int dist = at == null ? 0 : (int) Math.round(Math.max(0,
                        layout.toPole(v.side(), relX(at), relZ(at)) - layout.square()));
                name = Texts.text(lang, "bar.carry", time, Placeholder.unparsed("dist", String.valueOf(dist)));
                colour = BossBar.Color.BLUE;
            } else if (v != null && poles[v.side().index()].state() == Pole.State.DROPPED) {
                name = Texts.text(lang, "bar.dropped.own", time,
                        Placeholder.unparsed("secs", String.valueOf((int) Math.ceil(poles[v.side().index()].dropLeft()))));
                colour = BossBar.Color.YELLOW;
            } else if (v != null && poles[v.side().other().index()].state() == Pole.State.DROPPED) {
                name = Texts.text(lang, "bar.dropped.enemy", time,
                        Placeholder.unparsed("secs", String.valueOf((int) Math.ceil(poles[v.side().other().index()].dropLeft()))));
                colour = BossBar.Color.YELLOW;
            } else {
                name = Texts.text(lang, "bar.time", time, village(lang, Side.BLUE, "vblue"), village(lang, Side.RED, "vred"),
                        Placeholder.unparsed("pa", String.valueOf(points[0])), Placeholder.unparsed("pb", String.valueOf(points[1])));
            }
            bar.name(name).progress(Math.max(0f, Math.min(1f, progress))).color(colour);
        }
    }
}
