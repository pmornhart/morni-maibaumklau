package de.moinmorni.maibaumklau.net;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Verbindung zum MoinMorni-Netzwerk. Läuft das Netzwerk-Plugin („MoinMorni“), geht alles über dessen Schnittstelle
 * ({@code de.moinmorni.api.MoinMorni}); sonst läuft Maibaumklau allein: Sprache aus dem Client, keine Netzwerk-Statistik, „zur
 * Lobby“ trennt die Verbindung mit einem Hinweis.
 *
 * <p>Nur {@link Connected} kennt die Netzwerk-Klassen – ohne Netzwerk-Plugin wird sie nie geladen.
 */
public interface Network {

    /** Name dieses Modus im Netzwerk (Statistiken, Zustand). */
    String MODE = "maibaumklau";

    /** Läuft das Netzwerk-Plugin? */
    boolean connected();

    /** „de“ oder „en“. */
    String language(UUID player);

    /** Statistiken nach einem Spiel melden (zählen + Bestwerte). */
    void stats(UUID player, Map<String, Long> add, Map<String, Long> best);

    /** Zustand für Lobby-Figur und Webseite: „waiting“ oder „running“. */
    void state(String state, int players);

    /** Zurück in die Lobby (ohne Netzwerk: Verbindung trennen mit {@code message}). */
    void toLobby(Player player, Component message);

    /**
     * Der Spieler will selbst in die Lobby (Knopf, Gegenstand): Spielt er gerade ({@link #playing}), fragt das Netzwerk
     * erst nach. Ohne Netzwerk wie {@link #toLobby}; ein älteres Netzwerk-Plugin ohne Nachfrage schickt sofort.
     */
    void askToLobby(Player player, Component message);

    /** Spielt der Spieler gerade eine Runde? Solange fragt „/lobby“ erst nach. Ohne Netzwerk: nichts. */
    void playing(Player player, boolean playing);

    /**
     * Platz nach einem Verbindungsabbruch so viele Sekunden freihalten – so lange bietet die Lobby „Zurück ins Spiel“
     * an; {@code 0} nimmt das Angebot zurück. Ohne Netzwerk: nichts (man verbindet sich direkt wieder).
     */
    void holdSlot(UUID player, int seconds);

    /** Party des Spielers (leer ohne Party oder ohne Netzwerk). Kommt auf irgendeinem Thread zurück. */
    CompletableFuture<Optional<Party>> party(UUID player);

    /** Party: Anführer und Mitglieder (Server = wo sie gerade sind, null = beim Wechseln). */
    record Party(UUID leader, List<Member> members) {
        public record Member(UUID uuid, String name, String server) {
        }
    }

    /** Name dieses Servers im Netzwerk (z. B. „maibaumklau“), ohne Netzwerk leer. */
    String serverName();

    /** Netzwerk suchen: mit Plugin „MoinMorni“ die echte Verbindung, sonst allein. */
    static Network connect(Plugin plugin) {
        Logger log = plugin.getLogger();
        if (plugin.getServer().getPluginManager().getPlugin("MoinMorni") != null) {
            try {
                Network n = Connected.create(log);
                if (n != null) {
                    return n;
                }
                log.warning("Netzwerk-Plugin gefunden, aber ohne Schnittstelle für Spielmodi – Maibaumklau läuft allein.");
            } catch (LinkageError | RuntimeException e) {
                log.warning("Netzwerk-Schnittstelle passt nicht (" + e + ") – Maibaumklau läuft allein.");
            }
        }
        return new Standalone();
    }
}
