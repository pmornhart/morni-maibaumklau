package de.moinmorni.maibaumklau;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.maibaumklau.logic.Help;
import de.moinmorni.maibaumklau.logic.Outcome;
import de.moinmorni.maibaumklau.logic.Side;
import de.moinmorni.maibaumklau.world.MapType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "sky", "au", "em", "bl", "rt", "dark_gray", "b",
            "bold");
    /** Namen von Standard-Tags: ein Platzhalter darf nie so heißen (sonst wäre er eine Farbe oder fett). */
    private static final Set<String> STANDARD = Set.of("red", "blue", "green", "gold", "yellow", "aqua", "black", "b",
            "i", "u", "st", "bold", "italic", "white", "gray", "grey", "dark_gray", "br", "reset", "obf", "newline");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    /** Text-Schlüssel als feste Zeichenkette im Quelltext, z. B. "tree.water" (nicht "map." + id). */
    // https://regex101.com/?flavor=java&regex=%22((%3F%3Ahall%7Ctree%29%5C.%5Ba-z0-9.%5C-%5D*%5Ba-z0-9%5D)%22&testString=Texts.msg(lang%2C%20%22tree.water%22)%20%22map.%22
    private static final Pattern KEY = Pattern.compile("\"((?:hall|item|menu|game|lift|carry|tree|tag|rest|fall|help|state"
            + "|time|bar|side|end|feast|spectate|leave|stats|top|board|lobby|only|unknown|map|village|howto)[.\\-]"
            + "[a-z0-9.\\-]*[a-z0-9])\"");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
            for (String p : placeholders(de)) {
                assertFalse(STANDARD.contains(p), "Platzhalter heißt wie ein Standard-Tag: " + key + " <" + p + ">");
            }
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("sky", Tag.styling(TextColor.color(Texts.SKY))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE))),
                        TagResolver.resolver("rt", Tag.styling(TextColor.color(Texts.RED)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "end.win.title"), Texts.raw("en", "end.win.title"));
        assertEquals(Texts.raw("de", "map.bachdoerfer"), Texts.raw("fr", "map.bachdoerfer"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
    }

    /** Jede Karte, jedes Dorf, jede Hilfe, jedes Kapitel und jeder Ausgang hat seine Texte in beiden Sprachen. */
    @Test
    void everyGameThingHasAName() {
        Set<String> keys = Texts.keys();
        for (MapType m : MapType.values()) {
            for (String s : new String[] {"", ".desc", ".short"}) {
                assertTrue(keys.contains("map." + m.id() + s), m.id() + s);
            }
            for (Side side : Side.values()) {
                assertTrue(keys.contains("village." + m.id() + "." + side.id()), m.id() + " " + side.id());
            }
        }
        for (Side side : Side.values()) {
            assertTrue(keys.contains("side." + side.id()), side.id());
        }
        for (Help h : Help.values()) {
            assertTrue(keys.contains("help." + h.id()), h.id());
            assertTrue(keys.contains("help." + h.id() + ".lore"), h.id());
        }
        for (String k : new String[] {"goal", "lift", "carry", "tag", "helps", "ransom"}) {
            for (String s : new String[] {"", ".text", ".chat"}) {
                assertTrue(keys.contains("howto." + k + s), k + s);
            }
        }
        for (Outcome.How how : Outcome.How.values()) {
            String k = new Outcome(how == Outcome.How.DRAW ? null : Side.BLUE, how).key();
            assertTrue(keys.contains("end." + k), k);
            if (how != Outcome.How.DRAW) {
                assertTrue(keys.contains("end.win.sub." + k), k);
                assertTrue(keys.contains("end.lose.sub." + k), k);
            }
        }
        for (String why : new String[] {"tag", "fall"}) {
            assertTrue(keys.contains(why + ".title") && keys.contains(why + ".sub"), why);
        }
    }

    /** Jeder Schlüssel, der im Quelltext steht, hat einen Text. */
    @Test
    void everyKeyUsedInTheCodeExists() throws IOException {
        Set<String> missing = new TreeSet<>();
        int found = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) {
                    found++;
                    if (!Texts.keys().contains(m.group(1))) {
                        missing.add(m.group(1) + " (" + f.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(found > 100, "nur " + found + " Schlüssel gefunden – stimmt das Muster?");
        assertEquals(Set.of(), missing);
    }

    @Test
    void numbersAndClock() {
        assertEquals("▮▮▮▯▯", Ui.bar(0.6, 5));
        assertEquals("1:05", Ui.clock(65));
        assertEquals("0:00", Ui.clock(-3));
    }
}
