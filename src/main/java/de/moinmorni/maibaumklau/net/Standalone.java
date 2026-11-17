package de.moinmorni.maibaumklau.net;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Maibaumklau ohne Netzwerk: Sprache aus dem Client (Deutsch, sonst Englisch, wenn der Client Englisch spricht), keine
 * Party, keine Statistik nach außen (lokal zählt Maibaumklau trotzdem).
 */
final class Standalone implements Network {

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public String language(UUID player) {
        Player p = Bukkit.getPlayer(player);
        if (p == null) {
            return "de";
        }
        Locale l = p.locale();
        return l != null && "en".equals(l.getLanguage()) ? "en" : "de";
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        // ohne Netzwerk nur lokal
    }

    @Override
    public void state(String state, int players) {
        // niemand fragt
    }

    @Override
    public void toLobby(Player player, Component message) {
        player.kick(message);
    }

    @Override
    public void askToLobby(Player player, Component message) {
        player.kick(message);
    }

    @Override
    public void playing(Player player, boolean playing) {
        // niemand fragt nach
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        // ohne Netzwerk verbindet man sich direkt wieder mit diesem Server
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public String serverName() {
        return "";
    }
}
