package de.moinmorni.maibaumklau.game;

import de.moinmorni.maibaumklau.MaibaumklauPlugin;
import de.moinmorni.maibaumklau.world.MapType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/** Alle laufenden Runden (höchstens eine je Karte). */
public final class Games {

    private final MaibaumklauPlugin plugin;
    private final Map<MapType, Game> running = new EnumMap<>(MapType.class);

    public Games(MaibaumklauPlugin plugin) {
        this.plugin = plugin;
    }

    public Game start(MapType map, List<Player> blue, List<Player> red) {
        Game g = new Game(plugin, map, blue, red);
        running.put(map, g);
        plugin.stateChanged();
        return g;
    }

    public Collection<Game> all() {
        return running.values();
    }

    public Game on(MapType map) {
        return running.get(map);
    }

    /** Karten ohne laufende Runde, die gerade nicht wiederhergestellt werden. */
    public Set<MapType> free() {
        Set<MapType> out = EnumSet.allOf(MapType.class);
        out.removeAll(running.keySet());
        out.removeIf(m -> plugin.construction().restoring(m));
        return out;
    }

    /** Runde, in der jemand mitspielt (auch gerade nicht verbunden) oder zuschaut. */
    public Game of(UUID uuid) {
        for (Game g : running.values()) {
            Villager v = g.villager(uuid);
            if ((v != null && !v.gone()) || g.spectating(uuid)) {
                return g;
            }
        }
        return null;
    }

    /** Kam diese Person in einer laufenden Runde nicht rechtzeitig zurück? */
    public boolean late(UUID uuid) {
        for (Game g : running.values()) {
            Villager v = g.villager(uuid);
            if (v != null && v.late()) {
                return true;
            }
        }
        return false;
    }

    /** Runde, in der jemand gerade mitspielt (da und nicht gegangen). */
    public Game playing(UUID uuid) {
        for (Game g : running.values()) {
            Villager v = g.villager(uuid);
            if (v != null && v.active()) {
                return g;
            }
        }
        return null;
    }

    public void tick() {
        for (Game g : new ArrayList<>(running.values())) {
            boolean alive;
            try {
                alive = g.tick();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler in der Runde " + g.map().id() + " – sie wird beendet", e);
                g.close();
                alive = false;
            }
            if (!alive) {
                running.remove(g.map());
                plugin.stateChanged();
                plugin.boards().refreshHall();
            }
        }
    }

    /** Runde abbrechen (Verwaltung): ohne Wertung, alle zurück in den Wirtsgarten. */
    public void stop(Game g) {
        g.close();
        running.remove(g.map());
        plugin.stateChanged();
        plugin.boards().refreshHall();
    }

    public void abortAll() {
        for (Game g : new ArrayList<>(running.values())) {
            g.close();
        }
        running.clear();
    }

    /** Leute in Runden (gerade mitspielend). */
    public int players() {
        return running.values().stream().mapToInt(Game::activeCount).sum();
    }
}
