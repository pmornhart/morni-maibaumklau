package de.moinmorni.maibaumklau.listen;

import de.moinmorni.maibaumklau.MaibaumklauPlugin;
import de.moinmorni.maibaumklau.game.Game;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Kein Schaden in der ganzen Welt: Wer in einer Runde jemanden trifft, fängt ihn (wenn er darf) ab – mit Hand oder
 * Stecken, ohne dass es weh tut.
 */
public final class GameListener implements Listener {

    private final MaibaumklauPlugin plugin;

    public GameListener(MaibaumklauPlugin plugin) {
        this.plugin = plugin;
    }

    private static Entity source(Entity damager) {
        if (damager instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            return s instanceof Entity e ? e : null;
        }
        return damager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getEntity().getWorld() != plugin.world()) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getEntity() instanceof Player victim) || !(source(e.getDamager()) instanceof Player attacker)) {
            return;
        }
        Game g = plugin.games().playing(victim.getUniqueId());
        if (g != null && plugin.games().playing(attacker.getUniqueId()) == g) {
            g.hit(attacker, victim);
        }
    }

    /** Kein Schaden – weder Fall noch sonst etwas. Wer in die Tiefe fällt, merkt die Runde selbst. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && p.getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }

    /** Falls doch jemand stirbt (z. B. /kill): Sachen behalten, keine Meldung. */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getEntity().getWorld() != plugin.world()) {
            return;
        }
        e.deathMessage(null);
        e.setKeepInventory(true);
        e.getDrops().clear();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Game g = plugin.games().of(p.getUniqueId());
        if (g != null && g.phase() != Game.Phase.OVER && g.villager(p.getUniqueId()) == null) {
            e.setRespawnLocation(g.spectatorSpot());
            plugin.getServer().getScheduler().runTask(plugin, () -> g.spectate(p));
        } else if (g == null) {
            e.setRespawnLocation(plugin.hall().spawn());
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.hall().enter(p));
        }
    }
}
