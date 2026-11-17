package de.moinmorni.maibaumklau.hall;

import de.moinmorni.maibaumklau.Holo;
import de.moinmorni.maibaumklau.Items;
import de.moinmorni.maibaumklau.MaibaumklauPlugin;
import de.moinmorni.maibaumklau.Texts;
import de.moinmorni.maibaumklau.Ui;
import de.moinmorni.maibaumklau.game.Game;
import de.moinmorni.maibaumklau.logic.Groups;
import de.moinmorni.maibaumklau.logic.Teams;
import de.moinmorni.maibaumklau.logic.Votes;
import de.moinmorni.maibaumklau.world.HallPlan;
import de.moinmorni.maibaumklau.world.MapType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Der Wirtsgarten: hier wartet man, stimmt über die Karte ab und sagt „bereit“. Ein Countdown startet die nächste
 * Runde; die Leute werden auf zwei gleich große Dörfer verteilt – Partys bleiben zusammen.
 */
public final class Hall {

    public static final int SLOT_VOTE = 0;
    public static final int SLOT_READY = 1;
    public static final int SLOT_HOWTO = 2;
    public static final int SLOT_STATS = 4;
    public static final int SLOT_LOBBY = 8;
    private static final int ALL_READY_SECONDS = 5;
    private static final int MIN_PLAYERS = 2;

    private final MaibaumklauPlugin plugin;
    private final Set<UUID> waiting = new LinkedHashSet<>();
    private final Set<UUID> ready = new HashSet<>();
    private final Map<UUID, UUID> leaders = new HashMap<>();
    private final Votes<MapType> mapVotes = new Votes<>(MapType.class);
    private int countdown = -1;

    public Hall(MaibaumklauPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawn() {
        return new Location(plugin.world(), HallPlan.SPAWN_X, MapType.FLOOR_Y + HallPlan.SPAWN_Y, HallPlan.SPAWN_Z,
                HallPlan.SPAWN_YAW, 0f);
    }

    public boolean inHall(UUID uuid) {
        return waiting.contains(uuid);
    }

    public Set<UUID> waiting() {
        return waiting;
    }

    public Votes<MapType> mapVotes() {
        return mapVotes;
    }

    public int countdown() {
        return countdown;
    }

    public boolean ready(UUID uuid) {
        return ready.contains(uuid);
    }

    /** Karte, die als Nächstes drankäme (null, wenn keine frei ist). */
    public MapType nextMap() {
        return mapVotes.winner(plugin.games().free());
    }

    /** In den Wirtsgarten (Betreten, nach der Runde, /verlassen). */
    public void enter(Player p) {
        String lang = plugin.lang(p);
        p.teleport(spawn());
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.clearActivePotionEffects();
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setExp(0);
        p.setLevel(0);
        p.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
        p.resetPlayerTime();
        Game.reset(p, plugin.carryKey());
        items(p, lang);
        waiting.add(p.getUniqueId());
        ready.remove(p.getUniqueId());
        Holo.refresh(plugin, p, lang);
        plugin.boards().sidebar().teams(p, Map.of());
        plugin.boards().hall(p);
        refreshParty(p.getUniqueId());
    }

    public void items(Player p, String lang) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_VOTE, Items.hallTag(Items.named(Material.COMPASS, 1, Texts.text(lang, "item.vote"),
                Texts.lines(lang, "item.vote.lore")), "vote"));
        inv.setItem(SLOT_READY, readyItem(p, lang));
        inv.setItem(SLOT_HOWTO, Items.hallTag(Items.named(Material.WRITABLE_BOOK, 1, Texts.text(lang, "item.howto"),
                Texts.lines(lang, "item.howto.lore")), "howto"));
        inv.setItem(SLOT_STATS, Items.hallTag(Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "item.stats"),
                Texts.lines(lang, "item.stats.lore")), "stats"));
        inv.setItem(SLOT_LOBBY, Items.hallTag(Items.named(Material.RED_BED, 1, Texts.text(lang, "item.lobby"),
                Texts.lines(lang, "item.lobby.lore")), "lobby"));
    }

    private ItemStack readyItem(Player p, String lang) {
        boolean r = ready.contains(p.getUniqueId());
        return Items.glow(Items.hallTag(Items.named(r ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                Texts.text(lang, "item.ready"), Texts.lines(lang, "item.ready.lore")), "ready"), r);
    }

    public void leave(UUID uuid) {
        waiting.remove(uuid);
        ready.remove(uuid);
        mapVotes.forget(uuid);
        leaders.remove(uuid);
    }

    public void toggleReady(Player p) {
        if (!waiting.contains(p.getUniqueId())) {
            return;
        }
        String lang = plugin.lang(p);
        if (ready.remove(p.getUniqueId())) {
            p.sendMessage(Texts.msg(lang, "hall.not-ready"));
        } else {
            ready.add(p.getUniqueId());
            p.sendMessage(Texts.msg(lang, waiting.size() < MIN_PLAYERS ? "hall.ready.alone" : "hall.ready"));
            Ui.sound(p, "minecraft:block.note_block.pling", 1.4f);
        }
        p.getInventory().setItem(SLOT_READY, readyItem(p, lang));
    }

    public void vote(Player p, MapType map) {
        mapVotes.vote(p.getUniqueId(), map);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.voted", Placeholder.unparsed("choice", Texts.plain(lang, "map." + map.id()))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
        plugin.boards().refreshHall();
    }

    /** Party-Leitung nachschlagen (kommt auf einem anderen Thread zurück). */
    public void refreshParty(UUID uuid) {
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (party.isPresent()) {
                leaders.put(uuid, party.get().leader());
            } else {
                leaders.remove(uuid);
            }
        }));
    }

    /** Jede Sekunde: Countdown, Start. */
    public void tick() {
        waiting.removeIf(u -> Bukkit.getPlayer(u) == null);
        ready.retainAll(waiting);
        mapVotes.retain(waiting);
        if (!plugin.ready()) {
            return;
        }
        MapType next = nextMap();
        boolean anyReady = !ready.isEmpty();
        boolean allReady = !waiting.isEmpty() && ready.containsAll(waiting);
        if (waiting.size() < MIN_PLAYERS || next == null
                || (!anyReady && waiting.size() < plugin.settings().hallMinPlayers())) {
            countdown = -1;
            for (UUID u : waiting) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(next == null ? Texts.text(lang, "hall.no-map")
                            : Texts.text(lang, "hall.waiting", Placeholder.unparsed("n", String.valueOf(waiting.size())),
                                    Placeholder.unparsed("min", String.valueOf(plugin.settings().hallMinPlayers()))));
                }
            }
            return;
        }
        if (countdown < 0) {
            countdown = plugin.settings().hallCountdown();
        }
        if (allReady && countdown > ALL_READY_SECONDS) {
            countdown = ALL_READY_SECONDS;
        }
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                String lang = plugin.lang(p);
                p.sendActionBar(Texts.text(lang, "hall.countdown", Placeholder.unparsed("secs", String.valueOf(countdown)),
                        Placeholder.unparsed("map", Texts.plain(lang, "map." + next.id()))));
                if (countdown <= 3 && countdown > 0) {
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.2f);
                }
            }
        }
        if (countdown-- <= 0) {
            start(next);
            countdown = -1;
        }
    }

    /** Runde starten (Countdown abgelaufen oder /maibaumadmin start). null, wenn es nicht geht. */
    public Game start(MapType map) {
        Map<UUID, List<UUID>> groups = new LinkedHashMap<>();
        for (UUID u : waiting) {
            UUID leader = leaders.getOrDefault(u, u);
            if (!waiting.contains(leader)) {
                leader = u;
            }
            groups.computeIfAbsent(leader, k -> new ArrayList<>()).add(u);
        }
        List<List<UUID>> online = new ArrayList<>();
        for (List<UUID> g : groups.values()) {
            List<UUID> in = g.stream().filter(u -> Bukkit.getPlayer(u) != null).toList();
            if (!in.isEmpty()) {
                online.add(in);
            }
        }
        List<UUID> picked = Groups.pick(online, plugin.settings().maxPerVillage() * 2);
        List<List<UUID>> pickedGroups = new ArrayList<>();
        for (List<UUID> g : online) {
            List<UUID> in = g.stream().filter(picked::contains).toList();
            if (!in.isEmpty()) {
                pickedGroups.add(in);
            }
        }
        List<List<UUID>> sides = Teams.split(pickedGroups);
        if (sides.get(0).isEmpty() || sides.get(1).isEmpty()) {
            return null;
        }
        List<Player> blue = sides.get(0).stream().map(Bukkit::getPlayer).toList();
        List<Player> red = sides.get(1).stream().map(Bukkit::getPlayer).toList();
        int count = blue.size() + red.size();
        for (Player p : blue) {
            leave(p.getUniqueId());
        }
        for (Player p : red) {
            leave(p.getUniqueId());
        }
        mapVotes.played(map, System.currentTimeMillis());
        for (List<Player> side : List.of(blue, red)) {
            for (Player p : side) {
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, "hall.starting", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id())),
                        Placeholder.unparsed("n", String.valueOf(count))));
            }
        }
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "hall.full"));
            }
        }
        return plugin.games().start(map, blue, red);
    }
}
