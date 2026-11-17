package de.moinmorni.maibaumklau;

import de.moinmorni.maibaumklau.data.Leaderboard;
import de.moinmorni.maibaumklau.data.PlayerData;
import de.moinmorni.maibaumklau.game.Game;
import de.moinmorni.maibaumklau.hall.HallMenus;
import de.moinmorni.maibaumklau.world.MapType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Alle Befehle. Einstellungen gibt es keine als Befehl – die stehen in der config.yml (später im Panel). */
final class Commands implements TabExecutor {

    private static final String ADMIN = "moinmorni.maibaumklau.admin";
    private final MaibaumklauPlugin plugin;

    Commands(MaibaumklauPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("maibaumadmin")) {
            return admin(sender, args);
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        String lang = plugin.lang(p);
        boolean inHall = plugin.hall().inHall(p.getUniqueId());
        switch (name) {
            case "bereit" -> {
                if (inHall) {
                    plugin.hall().toggleReady(p);
                } else {
                    p.sendMessage(Texts.msg(lang, "hall.only"));
                }
            }
            case "karte" -> {
                MapType m = args.length > 0 ? MapType.byId(args[0]) : null;
                if (!inHall) {
                    p.sendMessage(Texts.msg(lang, "hall.only"));
                } else if (m == null) {
                    HallMenus.vote(plugin, p);
                } else {
                    plugin.hall().vote(p, m);
                }
            }
            case "zuschauen" -> spectate(p, args);
            case "verlassen" -> {
                Game g = plugin.games().of(p.getUniqueId());
                if (g == null) {
                    p.sendMessage(Texts.msg(lang, "leave.none"));
                } else {
                    g.leave(p);
                    plugin.hall().enter(p);
                    p.sendMessage(Texts.msg(lang, "leave.done"));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spectate(Player p, String[] args) {
        String lang = plugin.lang(p);
        Game g = null;
        if (args.length > 0) {
            MapType m = MapType.byId(args[0]);
            if (m != null) {
                g = plugin.games().on(m);
            } else {
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) {
                    p.sendMessage(Texts.msg(lang, "unknown-player", Placeholder.unparsed("name", args[0])));
                    return;
                }
                g = plugin.games().of(t.getUniqueId());
            }
        } else if (!plugin.games().all().isEmpty()) {
            g = plugin.games().all().iterator().next();
        }
        if (g == null) {
            p.sendMessage(Texts.msg(lang, "spectate.none"));
            return;
        }
        if (plugin.games().playing(p.getUniqueId()) != null) {
            p.sendMessage(Texts.msg(lang, "spectate.playing"));
            return;
        }
        Game current = plugin.games().of(p.getUniqueId());
        if (current != null && current != g) {
            current.leave(p);
        }
        plugin.spectate(p, g);
    }

    private void stats(CommandSender sender, String[] args) {
        String lang = lang(sender);
        UUID uuid;
        String who;
        if (args.length > 0) {
            Player online = Bukkit.getPlayerExact(args[0]);
            OfflinePlayer op = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[0]);
            if (op == null) {
                sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", args[0])));
                return;
            }
            uuid = op.getUniqueId();
            who = op.getName() == null ? args[0] : op.getName();
        } else if (sender instanceof Player p) {
            uuid = p.getUniqueId();
            who = p.getName();
        } else {
            sender.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        Optional<PlayerData> d = plugin.store().read(uuid);
        if (d.isEmpty()) {
            sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", who)));
            return;
        }
        sender.sendMessage(Texts.msg(lang, "stats.head", Placeholder.unparsed("name", who)));
        HallMenus.statLines(lang, d.get()).forEach(sender::sendMessage);
    }

    private void top(CommandSender sender) {
        String lang = lang(sender);
        sender.sendMessage(Texts.msg(lang, "top.head"));
        List<Leaderboard.Entry> top = plugin.board().top(10);
        if (top.isEmpty()) {
            sender.sendMessage(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            sender.sendMessage(HallMenus.row(lang, i + 1, top.get(i)));
        }
    }

    /** Verwaltung: info, status, start, vorspulen, stopp, wiederherstellen, neubauen, neuladen. */
    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "info";
        MapType chosen = null;
        for (int i = 1; i < args.length; i++) {
            if (MapType.byId(args[i]) != null) {
                chosen = MapType.byId(args[i]);
            }
        }
        Game g = chosen != null ? plugin.games().on(chosen)
                : plugin.games().all().isEmpty() ? null : plugin.games().all().iterator().next();
        switch (sub) {
            case "info", "status" -> {
                sender.sendMessage(Component.text("Maibaumklau: bereit=" + plugin.ready() + " netzwerk="
                        + plugin.net().connected() + " wirtsgarten=" + plugin.hall().waiting().size() + " countdown="
                        + plugin.hall().countdown() + " runden=" + plugin.games().all().size() + " frei="
                        + String.join(",", plugin.games().free().stream().map(MapType::id).toList()) + " zurueckgesetzt="
                        + plugin.construction().lastRestored()));
                for (Game x : plugin.games().all()) {
                    sender.sendMessage(Component.text(x.status()));
                }
            }
            case "start" -> {
                MapType m = chosen != null ? chosen : plugin.hall().nextMap();
                if (m == null || !plugin.games().free().contains(m)) {
                    sender.sendMessage(Component.text("Keine freie Karte."));
                } else {
                    Game started = plugin.hall().start(m);
                    sender.sendMessage(Component.text(started == null ? "Zu wenige Leute im Wirtsgarten."
                            : "Gestartet: " + m.id()));
                }
            }
            case "vorspulen" -> {
                if (g == null || args.length < 2) {
                    sender.sendMessage(Component.text("/maibaumadmin vorspulen <los|ende|sekunden> [karte]"));
                } else {
                    int to = switch (args[1].toLowerCase(Locale.ROOT)) {
                        case "los", "go" -> -1;
                        case "ende", "end" -> 1;
                        default -> parse(args[1], g.secondsLeft());
                    };
                    g.skipTo(to);
                    sender.sendMessage(Component.text("Zeit: " + (to < 0 ? "los" : String.valueOf(to))));
                }
            }
            case "stopp" -> {
                if (g != null) {
                    plugin.games().stop(g);
                    sender.sendMessage(Component.text("Runde abgebrochen."));
                }
            }
            case "wiederherstellen" -> {
                for (MapType m : chosen != null ? List.of(chosen) : Arrays.asList(MapType.values())) {
                    if (plugin.games().on(m) == null) {
                        plugin.construction().restore(m, () -> sender.sendMessage(Component.text("Wiederhergestellt: "
                                + m.id() + " (" + plugin.construction().lastRestored() + " Blöcke)")));
                    }
                }
            }
            case "neubauen" -> plugin.rebuild(() -> sender.sendMessage(Component.text("Neu gebaut.")));
            case "neuladen" -> {
                plugin.reloadSettings();
                sender.sendMessage(Component.text("Einstellungen neu geladen."));
            }
            default -> sender.sendMessage(Component.text(
                    "/maibaumadmin info|status|start|vorspulen|stopp|wiederherstellen|neubauen|neuladen"));
        }
        return true;
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            switch (name) {
                case "maibaumadmin" -> out.addAll(List.of("info", "status", "start", "vorspulen", "stopp",
                        "wiederherstellen", "neubauen", "neuladen"));
                case "karte" -> Arrays.stream(MapType.values()).forEach(m -> out.add(m.id()));
                case "zuschauen", "stats" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 2 && name.equals("maibaumadmin")) {
            if (args[0].equalsIgnoreCase("vorspulen")) {
                out.addAll(List.of("los", "ende"));
            } else if (List.of("start", "stopp", "wiederherstellen").contains(args[0].toLowerCase(Locale.ROOT))) {
                Arrays.stream(MapType.values()).forEach(m -> out.add(m.id()));
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
