package de.moinmorni.maibaumklau.game;

import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Leiste oben in zwei Sprachen: jeder sieht seine. */
final class LangBar {

    private final BossBar de;
    private final BossBar en;

    LangBar(BossBar.Color color, BossBar.Overlay overlay) {
        de = BossBar.bossBar(Component.empty(), 1f, color, overlay);
        en = BossBar.bossBar(Component.empty(), 1f, color, overlay);
    }

    void update(Function<String, Component> name, float progress, BossBar.Color color) {
        float p = Math.max(0f, Math.min(1f, progress));
        de.name(name.apply("de")).progress(p).color(color);
        en.name(name.apply("en")).progress(p).color(color);
    }

    void show(Player p, String lang) {
        if ("en".equals(lang)) {
            p.hideBossBar(de);
            p.showBossBar(en);
        } else {
            p.hideBossBar(en);
            p.showBossBar(de);
        }
    }

    void hide(Player p) {
        p.hideBossBar(de);
        p.hideBossBar(en);
    }
}
