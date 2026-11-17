package de.moinmorni.maibaumklau;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.maibaumklau.logic.Rules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Die mitgelieferte config.yml ist dasselbe wie die Vorgaben im Code; unsinnige Werte werden gestutzt. */
class SettingsTest {

    private static YamlConfiguration shipped() throws IOException, InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(Files.readString(Path.of("src/main/resources/config.yml"), StandardCharsets.UTF_8));
        return y;
    }

    @Test
    void shippedConfigEqualsTheDefaults() throws Exception {
        Settings fromFile = Settings.load(shipped());
        Settings empty = Settings.load(new YamlConfiguration());
        assertEquals(empty, fromFile);
        assertEquals(Rules.defaults(), fromFile.rules());
        assertEquals(90, fromFile.comebackSeconds());
        assertEquals(480, fromFile.roundSeconds());
    }

    @Test
    void everyKeyInTheShippedConfigIsRead() throws Exception {
        YamlConfiguration y = shipped();
        String code = Files.readString(Path.of("src/main/java/de/moinmorni/maibaumklau/Settings.java"),
                StandardCharsets.UTF_8);
        for (String key : y.getKeys(true)) {
            if (!y.isConfigurationSection(key)) {
                assertTrue(code.contains("\"" + key + "\""), "config.yml-Schlüssel wird nicht gelesen: " + key);
            }
        }
    }

    @Test
    void silllyValuesAreClamped() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("rueckkehr-sekunden", 99999);
        y.set("runde.minuten", 0);
        y.set("maibaum.tempo-allein", 5.0);
        y.set("maibaum.tempo-hoechstens", 0.1);
        y.set("wirtsgarten.mindestens-spieler", 1);
        y.set("runde.hoechstens-je-dorf", 50);
        Settings s = Settings.load(y);
        assertEquals(600, s.comebackSeconds());
        assertEquals(120, s.roundSeconds());
        assertEquals(1.0, s.rules().speedAlone(), 1e-9);
        assertEquals(1.0, s.rules().speedMax(), 1e-9, "nie langsamer als allein");
        assertEquals(2, s.hallMinPlayers());
        assertEquals(8, s.maxPerVillage());
    }
}
