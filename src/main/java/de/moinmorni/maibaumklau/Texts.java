package de.moinmorni.maibaumklau;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <sky>} Himmelblau,
 * {@code <au>} Gold, {@code <em>} Smaragd, {@code <bl>} Blau (Dorf), {@code <rt>} Rot (Dorf). Namen von Spielern
 * kommen immer als {@code Placeholder.unparsed} hinein, Dorfnamen als Komponente in ihrer Farbe.
 */
public final class Texts {

    public static final int SKY = 0x7EC8F2;
    public static final int GOLD = 0xF2B632;
    public static final int EMERALD = 0x2BB673;
    public static final int BLUE = 0x3C8DE0;
    public static final int RED = 0xD8413A;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("sky", Tag.styling(TextColor.color(SKY))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE))),
                    TagResolver.resolver("rt", Tag.styling(TextColor.color(RED)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<em>⚑</em> <au>Maibaumklau</au> <dark_gray>»</dark_gray> ",
                "<em>⚑</em> <au>Maypole Raid</au> <dark_gray>»</dark_gray> ");
        t("mode", "Maibaumklau", "Maypole Raid");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");

        // ---------- Karten und Dörfer ----------
        t("map.bachdoerfer", "Zwei Dörfer am Bach", "Two Villages by the Brook");
        t("map.bachdoerfer.short", "Oberbach gegen Unterbach – drei Brücken über den Bach",
                "Oberbach against Unterbach – three bridges across the brook");
        t("map.bachdoerfer.desc", "<gray>Eine Wiese in den Bergen, in der Mitte ein Bach.</gray>\n<gray>Drei Holzbrücken, zwei Heuwagen, die</gray>\n<gray>einen übers Wasser werfen – oder waten.</gray>",
                "<gray>A meadow in the mountains, a brook in the middle.</gray>\n<gray>Three wooden bridges, two hay wagons that</gray>\n<gray>throw you across – or wade through.</gray>");
        t("map.almdoerfer", "Almdörfer mit Hängebrücke", "Alpine Villages and the Rope Bridge");
        t("map.almdoerfer.short", "Hinteralm gegen Vorderalm – über die Schlucht",
                "Hinteralm against Vorderalm – across the gorge");
        t("map.almdoerfer.desc", "<gray>Zwei Almen, dazwischen eine tiefe Schlucht.</gray>\n<gray>Hinüber über die Hängebrücke, einen Baumstamm</gray>\n<gray>oder den Felsbogen – nur nicht hinunterfallen!</gray>",
                "<gray>Two alpine pastures, a deep gorge between them.</gray>\n<gray>Cross the rope bridge, a fallen log</gray>\n<gray>or the rock arch – just don't fall!</gray>");
        t("village.bachdoerfer.blau", "Oberbach", "Oberbach");
        t("village.bachdoerfer.rot", "Unterbach", "Unterbach");
        t("village.almdoerfer.blau", "Hinteralm", "Hinteralm");
        t("village.almdoerfer.rot", "Vorderalm", "Vorderalm");
        t("side.blau", "Blau", "Blue");
        t("side.rot", "Rot", "Red");

        // ---------- Wirtsgarten ----------
        t("hall.title", "<em>⚑ Maibaumklau</em>", "<em>⚑ Maypole Raid</em>");
        t("hall.subtitle", "<gray>Zwei Dörfer, zwei Maibäume – wer klaut zuerst?</gray>",
                "<gray>Two villages, two maypoles – who steals first?</gray>");
        t("hall.welcome", "<gray>Servus im <em>Wirtsgarten zum Maibaum</em>! Mit dem <white>Kompass</white> stimmst du über die Karte ab, mit dem <white>grünen Farbstoff</white> sagst du „bereit“, im <white>Buch</white> steht, wie es geht.</gray>",
                "<gray>Welcome to the <em>Maypole Inn Garden</em>! Use the <white>compass</white> to vote for a map, the <white>green dye</white> to say you're ready, and the <white>book</white> explains how to play.</gray>");
        t("hall.how", "<gray>So geht's: Zwei Dörfer, jedes mit seinem <au>Maibaum</au>. Leg den Baum des anderen Dorfs um und trag ihn in dein Dorf – mit mehr Leuten geht es schneller. Wer einen Träger <white>berührt</white> oder <white>trifft</white>, fängt ihn ab, und der Baum fällt hin. Niemand nimmt Schaden!</gray>",
                "<gray>How it works: two villages, each with its own <au>maypole</au>. Fell the other village's pole and carry it home – it's faster with more people. Whoever <white>touches</white> or <white>hits</white> a carrier catches them and the pole drops. Nobody gets hurt!</gray>");
        t("hall.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da, ab <white><min></white> geht's von selbst los)</gray>",
                "<gray>Waiting for players … (<white><n></white> here, starts by itself from <white><min></white>)</gray>");
        t("hall.countdown", "<gray>Nächste Runde in <au><secs> s</au> · <sky><map></sky></gray>",
                "<gray>Next round in <au><secs> s</au> · <sky><map></sky></gray>");
        t("hall.no-map", "<gray>Gerade sind alle Karten belegt – gleich wird eine frei.</gray>",
                "<gray>All maps are busy – one will be free soon.</gray>");
        t("hall.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("hall.ready.alone", "<em>Du bist bereit.</em> <gray>Sobald noch jemand da ist, geht's los.</gray>",
                "<em>You're ready.</em> <gray>As soon as someone else joins, we start.</gray>");
        t("hall.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("hall.voted", "<gray>Deine Stimme: <sky><choice></sky>.</gray>", "<gray>Your vote: <sky><choice></sky>.</gray>");
        t("hall.starting", "<em>Auf geht's!</em> <gray>Karte: <sky><map></sky> · <white><n></white> Leute</gray>",
                "<em>Let's go!</em> <gray>Map: <sky><map></sky> · <white><n></white> players</gray>");
        t("hall.full", "<gray>Diese Runde ist voll – du bist in der nächsten dabei. Zuschauen geht mit dem Kompass.</gray>",
                "<gray>This round is full – you'll be in the next one. You can watch via the compass.</gray>");
        t("hall.only", "<gray>Das geht nur im Wirtsgarten.</gray>", "<gray>That only works in the inn garden.</gray>");
        t("hall.party-playing", "<gray>Deine Party spielt gerade – mit <white>/zuschauen</white> kannst du zuschauen.</gray>",
                "<gray>Your party is playing right now – use <white>/spectate</white> to watch.</gray>");

        // ---------- Gegenstände im Wirtsgarten ----------
        t("item.vote", "<sky>Abstimmen</sky> <gray>(Rechtsklick)</gray>", "<sky>Vote</sky> <gray>(right-click)</gray>");
        t("item.vote.lore", "<gray>Welche Karte als Nächstes?</gray>\n<gray>Läuft dort eine Runde: zuschauen.</gray>",
                "<gray>Which map next?</gray>\n<gray>If a round is running there: watch.</gray>");
        t("item.ready", "<em>Bereit</em> <gray>(Rechtsklick)</gray>", "<em>Ready</em> <gray>(right-click)</gray>");
        t("item.ready.lore", "<gray>Sind alle bereit, geht's nach 5 Sekunden los.</gray>",
                "<gray>Once everyone is ready, we start in 5 seconds.</gray>");
        t("item.howto", "<au>So geht's</au> <gray>(Rechtsklick)</gray>", "<au>How to play</au> <gray>(right-click)</gray>");
        t("item.howto.lore", "<gray>Umlegen, tragen, abfangen, auslösen.</gray>",
                "<gray>Fell, carry, catch, ransom.</gray>");
        t("item.stats", "<au>Statistik</au> <gray>(Rechtsklick)</gray>", "<au>Statistics</au> <gray>(right-click)</gray>");
        t("item.stats.lore", "<gray>Deine Werte und die besten Maibaumdiebe.</gray>",
                "<gray>Your stats and the best maypole thieves.</gray>");
        t("item.lobby", "<rt>Zurück zur Lobby</rt> <gray>(Rechtsklick)</gray>", "<rt>Back to the lobby</rt> <gray>(right-click)</gray>");
        t("item.lobby.lore", "<gray>Zurück ins Dorf von MoinMorni.</gray>", "<gray>Back to the MoinMorni village.</gray>");
        t("item.armour", "<gray>Tracht von</gray> <village>", "<gray>Costume of</gray> <village>");

        // ---------- Menüs ----------
        t("menu.close", "<gray>Schließen</gray>", "<gray>Close</gray>");
        t("menu.vote.title", "Welche Karte?", "Which map?");
        t("menu.vote.villages", "<gray>Dörfer: <bl><na></bl> gegen <rt><nb></rt></gray>",
                "<gray>Villages: <bl><na></bl> against <rt><nb></rt></gray>");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.busy", "<au>Runde läuft (<n> dabei) – klicken zum Zuschauen</au>",
                "<au>Round running (<n> playing) – click to watch</au>");
        t("menu.vote.cleaning", "<gray>Wird gerade aufgeräumt …</gray>", "<gray>Being tidied up …</gray>");
        t("menu.vote.mine", "<em>✔ Deine Stimme</em>", "<em>✔ Your vote</em>");
        t("menu.vote.click", "<sky>Klicken zum Abstimmen</sky>", "<sky>Click to vote</sky>");
        t("menu.howto.title", "So geht Maibaumklau", "How Maypole Raid works");
        t("menu.stats.title", "Statistik", "Statistics");
        t("menu.stats.me", "<sky>Deine Werte</sky>", "<sky>Your stats</sky>");
        t("menu.stats.top", "<au>Die besten Maibaumdiebe</au>", "<au>The best maypole thieves</au>");

        // ---------- So geht's ----------
        t("howto.goal", "Das Ziel", "The goal");
        t("howto.goal.text", "<gray>Zwei Dörfer, jedes mit einem Maibaum.</gray>\n<gray>Klaut den Baum des anderen Dorfs und</gray>\n<gray>bringt ihn auf euren Dorfplatz – dann</gray>\n<gray>habt ihr gewonnen. Sonst nach der Zeit:</gray>\n<gray>wer mehr Punkte hat.</gray>",
                "<gray>Two villages, each with a maypole.</gray>\n<gray>Steal the other village's pole and</gray>\n<gray>bring it to your village square – then</gray>\n<gray>you win. Otherwise, when time runs</gray>\n<gray>out: whoever has more points.</gray>");
        t("howto.goal.chat", "<gray>Ziel: Den fremden Maibaum auf den eigenen Dorfplatz bringen. Sonst gewinnt nach der Zeit, wer mehr Punkte hat.</gray>",
                "<gray>Goal: bring the other village's maypole to your own square. Otherwise the side with more points wins when time runs out.</gray>");
        t("howto.lift", "Umlegen", "Felling");
        t("howto.lift.text", "<gray>Stellt euch an den fremden Baum – er wird</gray>\n<gray>umgelegt. Zu zweit doppelt so schnell.</gray>\n<gray>Jede Wache daneben hält einen von euch</gray>\n<gray>auf – gleich viele: nichts geht voran.</gray>",
                "<gray>Stand next to the other pole – it gets</gray>\n<gray>felled. Twice as fast with two.</gray>\n<gray>Each guard next to it holds one of you</gray>\n<gray>back – equal numbers: no progress.</gray>");
        t("howto.lift.chat", "<gray>Umlegen: Am fremden Baum stehen bleiben. Mehr Leute = schneller, jede Wache hält einen auf.</gray>",
                "<gray>Felling: stay next to the other pole. More people = faster, each guard holds one back.</gray>");
        t("howto.carry", "Tragen", "Carrying");
        t("howto.carry.text", "<gray>Der Baum ist groß und schwer: Träger</gray>\n<gray>sind langsam und leuchten. Bleibt nah</gray>\n<gray>beieinander – wer mitträgt, macht</gray>\n<gray>alle schneller.</gray>",
                "<gray>The pole is big and heavy: carriers</gray>\n<gray>are slow and glow. Stay close</gray>\n<gray>together – everyone who helps</gray>\n<gray>makes you all faster.</gray>");
        t("howto.carry.chat", "<gray>Tragen: Träger sind langsam – jeder, der nah mitläuft, trägt mit und macht schneller.</gray>",
                "<gray>Carrying: carriers are slow – everyone running close helps and speeds things up.</gray>");
        t("howto.tag", "Abfangen", "Catching");
        t("howto.tag.text", "<gray>Auf eurer Hälfte fangt ihr Eindringlinge</gray>\n<gray>mit einem Schlag ab – ohne Schaden.</gray>\n<gray>Träger fängt man überall ab, schon wer</gray>\n<gray>sie berührt. Dann fällt der Baum hin und</gray>\n<gray>steht nach einer Weile wieder daheim.</gray>",
                "<gray>On your half you catch intruders</gray>\n<gray>with a hit – no damage.</gray>\n<gray>Carriers can be caught anywhere, even</gray>\n<gray>by a touch. Then the pole drops and is</gray>\n<gray>back home after a while.</gray>");
        t("howto.tag.chat", "<gray>Abfangen: Auf der eigenen Hälfte mit einem Schlag, Träger überall schon durch Berühren. Der Baum fällt hin und kehrt nach einer Weile heim.</gray>",
                "<gray>Catching: with a hit on your own half, carriers anywhere by a touch. The pole drops and returns home after a while.</gray>");
        t("howto.helps", "Hilfen", "Helpers");
        t("howto.helps.text", "<gray>Sprint-Brotzeit: kurz schneller.</gray>\n<gray>Heuballen: ein großer Satz.</gray>\n<gray>Leitern: an Wände setzen.</gray>\n<gray>Kompass: zeigt zum fremden Baum.</gray>\n<gray>Mit dem Baum geht nur der Stecken.</gray>",
                "<gray>Sprint snack: faster for a moment.</gray>\n<gray>Hay bale: a big leap.</gray>\n<gray>Ladders: place them on walls.</gray>\n<gray>Compass: points to the other pole.</gray>\n<gray>While carrying, only the stick works.</gray>");
        t("howto.helps.chat", "<gray>Hilfen: Brotzeit (schneller), Heuballen (Sprung), Leitern, Kompass – Heuwagen auf der Karte werfen einen hoch.</gray>",
                "<gray>Helpers: snack (faster), hay bale (leap), ladders, compass – hay wagons on the map throw you up.</gray>");
        t("howto.ransom", "Auslösen", "Ransom");
        t("howto.ransom.text", "<gray>Wie beim echten Brauch: Ein geklauter</gray>\n<gray>Maibaum wird ausgelöst – das Dorf, das</gray>\n<gray>ihn verloren hat, spendiert eine</gray>\n<gray>Brotzeit. Dann gibt's ihn zurück.</gray>",
                "<gray>Just like the real custom: a stolen</gray>\n<gray>maypole gets ransomed – the village</gray>\n<gray>that lost it pays for a hearty snack.</gray>\n<gray>Then it's given back.</gray>");
        t("howto.ransom.chat", "<gray>Auslösen: Wer seinen Baum verliert, spendiert den Siegern eine Brotzeit – so will es der Brauch.</gray>",
                "<gray>Ransom: whoever loses their pole treats the winners to a hearty snack – that's the custom.</gray>");

        // ---------- Runde: Start ----------
        t("game.title", "<em>⚑ Maibaumklau</em>", "<em>⚑ Maypole Raid</em>");
        t("game.start.sub", "<gray>Du gehörst zu</gray> <village>", "<gray>You belong to</gray> <village>");
        t("game.welcome", "<gray>Willkommen in</gray> <village><gray>! Karte: <sky><map></sky>.</gray>",
                "<gray>Welcome to</gray> <village><gray>! Map: <sky><map></sky>.</gray>");
        t("game.goal", "<gray>Klaut den Maibaum von</gray> <village> <gray>und bringt ihn auf euren Dorfplatz – und passt auf euren eigenen auf!</gray>",
                "<gray>Steal the maypole of</gray> <village> <gray>and bring it to your square – and guard your own!</gray>");
        t("game.team", "<gray>Dein Dorf: <white><names></white></gray>", "<gray>Your village: <white><names></white></gray>");
        t("game.count", "<au><n></au>", "<au><n></au>");
        t("game.count.sub", "<gray>Gleich geht's los …</gray>", "<gray>Get ready …</gray>");
        t("game.go", "<em>Los!</em>", "<em>Go!</em>");
        t("game.go.sub", "<gray>Holt euch den Maibaum!</gray>", "<gray>Go get that maypole!</gray>");
        t("game.started", "<gray>Die Runde läuft – in der Schnellleiste: Stecken, Brotzeit, Heuballen, Leitern, Kompass.</gray>",
                "<gray>The round is on – in your hotbar: stick, snack, hay bale, ladders, compass.</gray>");

        // ---------- Verbindung weg ----------
        t("game.away", "<gray><white><name></white> hat die Verbindung verloren – der Platz bleibt <white><secs></white> Sekunden frei.</gray>",
                "<gray><white><name></white> lost the connection – their place is kept for <white><secs></white> seconds.</gray>");
        t("game.back", "<em>Willkommen zurück!</em> <gray>Du spielst wieder für</gray> <village><gray>.</gray>",
                "<em>Welcome back!</em> <gray>You're playing for</gray> <village> <gray>again.</gray>");
        t("game.back.title", "<em>Willkommen zurück!</em>", "<em>Welcome back!</em>");
        t("game.back.sub", "<gray>Weiter geht's für</gray> <village>", "<gray>Carry on for</gray> <village>");
        t("game.back.all", "<gray><white><name></white> ist zurück in der Runde.</gray>",
                "<gray><white><name></white> is back in the round.</gray>");
        t("game.too-late", "<gray><white><name></white> ist nicht rechtzeitig zurückgekommen und spielt nicht mehr mit.</gray>",
                "<gray><white><name></white> didn't come back in time and is out of the round.</gray>");
        t("game.too-late.self", "<gray>Leider zu spät – die Runde hat nicht länger gewartet. Bis zur nächsten!</gray>",
                "<gray>Sorry, too late – the round couldn't wait any longer. See you next time!</gray>");
        t("game.left", "<gray><white><name></white> hat die Runde verlassen.</gray>",
                "<gray><white><name></white> left the round.</gray>");

        // ---------- Umlegen ----------
        t("lift.working", "<au>Umlegen …</au> <white><bar></white> <gray><pct> %</gray>",
                "<au>Felling …</au> <white><bar></white> <gray><pct> %</gray>");
        t("lift.guarded", "<rt>Die Wache steht dabei – so geht nichts voran!</rt> <gray>Bringt mehr Leute.</gray>",
                "<rt>The guard is watching – no progress!</rt> <gray>Bring more people.</gray>");
        t("lift.alarm", "<rt>⚠ Euer Maibaum wird umgelegt! <pct> %</rt>", "<rt>⚠ Your maypole is being felled! <pct> %</rt>");
        t("lift.alarm.chat", "<rt>Alarm!</rt> <village> <gray>legt euren Maibaum um – schnell zum Dorfplatz!</gray>",
                "<rt>Alarm!</rt> <village> <gray>is felling your maypole – hurry to the square!</gray>");
        t("lift.done", "<thieves> <gray>hat den Maibaum von</gray> <owners> <gray>umgelegt! (<white><names></white>)</gray>",
                "<thieves> <gray>has felled the maypole of</gray> <owners><gray>! (<white><names></white>)</gray>");
        t("lift.title.own", "<rt>Euer Maibaum!</rt>", "<rt>Your maypole!</rt>");
        t("lift.sub.own", "<gray>Er wird weggetragen – fangt die Träger ab!</gray>",
                "<gray>It's being carried away – catch the carriers!</gray>");
        t("lift.title.thief", "<em>Er fällt!</em>", "<em>Timber!</em>");
        t("lift.sub.thief", "<gray>Tragt ihn heim nach</gray> <village>", "<gray>Carry it home to</gray> <village>");

        // ---------- Tragen ----------
        t("carry.bar", "<au>Du trägst den Maibaum</au> <gray>· <white><n></white> Träger · Tempo <white><speed> %</white> · noch <white><dist></white> Blöcke</gray>",
                "<au>You're carrying the maypole</au> <gray>· <white><n></white> carriers · speed <white><speed> %</white> · <white><dist></white> blocks to go</gray>");
        t("carry.no-help", "<gray>Mit dem Baum auf der Schulter geht das nicht.</gray>",
                "<gray>Not with a maypole on your shoulder.</gray>");
        t("carry.own-must-stand", "<gray>Erst muss euer eigener Maibaum wieder stehen – dann zählt es!</gray>",
                "<gray>Your own maypole has to stand again first – then it counts!</gray>");

        // ---------- Liegender Baum ----------
        t("tree.dropped", "<gray>Der Maibaum von</gray> <owners> <gray>liegt – in <white><secs></white> Sekunden ist er wieder daheim, wenn ihn keiner aufhebt.</gray>",
                "<gray>The maypole of</gray> <owners> <gray>is down – back home in <white><secs></white> seconds unless someone picks it up.</gray>");
        t("tree.picked", "<gray><white><name></white> hat den Maibaum von</gray> <owners> <gray>wieder aufgehoben!</gray>",
                "<gray><white><name></white> picked up the maypole of</gray> <owners> <gray>again!</gray>");
        t("tree.back", "<gray>Der Maibaum von</gray> <owners> <gray>steht wieder auf dem Dorfplatz.</gray>",
                "<gray>The maypole of</gray> <owners> <gray>is standing on its square again.</gray>");
        t("tree.water", "<gray>Der Maibaum von</gray> <owners> <gray>ist ins Wasser gefallen und treibt zurück ins Dorf!</gray>",
                "<gray>The maypole of</gray> <owners> <gray>fell into the water and drifts back home!</gray>");
        t("tree.gorge", "<gray>Der Maibaum von</gray> <owners> <gray>ist in die Tiefe gestürzt – er steht wieder im Dorf!</gray>",
                "<gray>The maypole of</gray> <owners> <gray>fell into the depths – it's back home!</gray>");
        t("tree.guarding", "<em>Ihr bewacht euren Baum</em> <gray>– noch <white><secs></white> s, dann steht er wieder.</gray>",
                "<em>You're guarding your pole</em> <gray>– <white><secs></white> s until it stands again.</gray>");

        // ---------- Abfangen ----------
        t("tag.done", "<gray><white><tagger></white> hat <white><victim></white> abgefangen.</gray>",
                "<gray><white><tagger></white> caught <white><victim></white>.</gray>");
        t("tag.save", "<gray><white><tagger></white> hat <white><victim></white> abgefangen – der Maibaum von</gray> <owners> <gray>liegt!</gray>",
                "<gray><white><tagger></white> caught <white><victim></white> – the maypole of</gray> <owners> <gray>is down!</gray>");
        t("tag.bar", "<em><name> abgefangen</em> <gray>· +<points> Punkte</gray>",
                "<em><name> caught</em> <gray>· +<points> points</gray>");
        t("tag.bar.save", "<em>Baum gerettet – <name> abgefangen</em> <gray>· +<points> Punkte</gray>",
                "<em>Pole saved – <name> caught</em> <gray>· +<points> points</gray>");
        t("tag.safe", "<gray><white><name></white> ist auf der eigenen Hälfte – dort fängst du niemanden ab.</gray>",
                "<gray><white><name></white> is on their own half – you can't catch them there.</gray>");
        t("tag.resting-them", "<gray><white><name></white> verschnauft gerade.</gray>",
                "<gray><white><name></white> is catching their breath.</gray>");
        t("tag.resting-me", "<gray>Du verschnaufst noch kurz.</gray>", "<gray>You're still catching your breath.</gray>");
        t("tag.title", "<rt>Abgefangen!</rt>", "<rt>Caught!</rt>");
        t("tag.sub", "<gray>Zurück zum Wirtshaus – kurz verschnaufen.</gray>",
                "<gray>Back to the inn – catch your breath.</gray>");
        t("rest.bar", "<gray>Verschnaufen … <white><secs></white> s</gray>", "<gray>Catching your breath … <white><secs></white> s</gray>");
        t("fall.title", "<rt>Abgestürzt!</rt>", "<rt>You fell!</rt>");
        t("fall.sub", "<gray>Zurück ins Dorf – Vorsicht an der Kante!</gray>", "<gray>Back to the village – mind the edge!</gray>");
        t("fall.msg", "<gray><white><name></white> ist abgestürzt und wieder im Dorf.</gray>",
                "<gray><white><name></white> fell and is back in the village.</gray>");

        // ---------- Hilfen ----------
        t("help.stecken", "<au>Abfang-Stecken</au>", "<au>Catching Stick</au>");
        t("help.stecken.lore", "<gray>Triff damit (oder mit der Hand) jemanden</gray>\n<gray>vom anderen Dorf auf deiner Hälfte.</gray>\n<gray>Träger fängst du überall ab.</gray>\n<gray>Niemand nimmt Schaden.</gray>",
                "<gray>Hit someone from the other village</gray>\n<gray>on your half with it (or your hand).</gray>\n<gray>Carriers can be caught anywhere.</gray>\n<gray>Nobody gets hurt.</gray>");
        t("help.brotzeit", "<au>Sprint-Brotzeit</au> <gray>(Rechtsklick)</gray>", "<au>Sprint Snack</au> <gray>(right-click)</gray>");
        t("help.brotzeit.lore", "<gray>Eine Brezn auf die Hand: kurz schneller.</gray>\n<gray>Danach eine Weile Pause.</gray>",
                "<gray>A pretzel on the go: faster for a moment.</gray>\n<gray>Then a short break.</gray>");
        t("help.brotzeit.used", "<em>Mahlzeit!</em> <gray>Kurz schneller.</gray>", "<em>Enjoy!</em> <gray>Faster for a moment.</gray>");
        t("help.heusprung", "<au>Heuballen-Sprung</au> <gray>(Rechtsklick)</gray>", "<au>Hay Bale Leap</au> <gray>(right-click)</gray>");
        t("help.heusprung.lore", "<gray>Ein großer Satz nach vorn und oben.</gray>\n<gray>Danach eine Weile Pause.</gray>",
                "<gray>A big leap forward and up.</gray>\n<gray>Then a short break.</gray>");
        t("help.leiter", "<au>Leiter</au>", "<au>Ladder</au>");
        t("help.leiter.lore", "<gray>An eine Wand setzen und hochklettern.</gray>\n<gray>Verschwindet nach einer Weile wieder.</gray>",
                "<gray>Place it on a wall and climb.</gray>\n<gray>Disappears again after a while.</gray>");
        t("help.kompass", "<au>Maibaum-Kompass</au> <gray>(Rechtsklick)</gray>", "<au>Maypole Compass</au> <gray>(right-click)</gray>");
        t("help.kompass.lore", "<gray>Zeigt zum Maibaum des anderen Dorfs.</gray>\n<gray>Rechtsklick: Wie steht's um beide Bäume?</gray>",
                "<gray>Points to the other village's maypole.</gray>\n<gray>Right-click: how are both poles doing?</gray>");
        t("help.kompass.state", "<gray>Euer Baum: <own> · Der andere: <enemy></gray>",
                "<gray>Your pole: <own> · The other: <enemy></gray>");
        t("help.wait", "<gray>Noch <white><secs></white> s warten.</gray>", "<gray>Wait <white><secs></white> s.</gray>");

        // ---------- Zustand eines Baums ----------
        t("state.home", "<em>steht</em>", "<em>standing</em>");
        t("state.lifting", "<rt>wird umgelegt <pct> %</rt>", "<rt>being felled <pct> %</rt>");
        t("state.carried", "<rt>wird getragen!</rt>", "<rt>being carried!</rt>");
        t("state.dropped", "<au>liegt (<secs> s)</au>", "<au>down (<secs> s)</au>");

        // ---------- Zeit ----------
        t("time.minute", "<au>Noch eine Minute!</au> <gray>Danach zählen die Punkte.</gray>",
                "<au>One minute left!</au> <gray>After that, points decide.</gray>");
        t("time.left", "<au>Noch <n> Sekunden</au>", "<au><n> seconds left</au>");

        // ---------- Leiste oben ----------
        t("bar.start", "<gray>Gleich geht's los … <white><secs></white></gray>", "<gray>Starting soon … <white><secs></white></gray>");
        t("bar.time", "<white><time></white> <gray>·</gray> <vblue> <white><pa></white> <gray>:</gray> <white><pb></white> <vred>",
                "<white><time></white> <gray>·</gray> <vblue> <white><pa></white> <gray>:</gray> <white><pb></white> <vred>");
        t("bar.alarm", "<rt>⚠ Euer Maibaum wird weggetragen – fangt die Träger ab!</rt> <gray><time></gray>",
                "<rt>⚠ Your maypole is being carried off – catch the carriers!</rt> <gray><time></gray>");
        t("bar.carry", "<em>Tragt den Baum heim!</em> <gray>Noch <white><dist></white> Blöcke · <time></gray>",
                "<em>Carry the pole home!</em> <gray><white><dist></white> blocks to go · <time></gray>");
        t("bar.dropped.own", "<au>Euer Baum liegt – bewacht ihn! <secs> s</au> <gray>· <time></gray>",
                "<au>Your pole is down – guard it! <secs> s</au> <gray>· <time></gray>");
        t("bar.dropped.enemy", "<au>Der fremde Baum liegt – hebt ihn auf! <secs> s</au> <gray>· <time></gray>",
                "<au>The other pole is down – pick it up! <secs> s</au> <gray>· <time></gray>");
        t("bar.paused", "<gray>Angehalten – alle sind gerade weg.</gray>", "<gray>Paused – everyone is away right now.</gray>");
        t("bar.over", "<au>Auslösen auf dem Dorfplatz – gleich geht's zurück in den Wirtsgarten.</au>",
                "<au>Ransom on the square – back to the inn garden soon.</au>");

        // ---------- Leiste rechts ----------
        t("side.title", "<em><b>⚑ Maibaumklau</b></em>", "<em><b>⚑ Maypole Raid</b></em>");
        t("side.map", "<gray>Karte: <sky><map></sky></gray>", "<gray>Map: <sky><map></sky></gray>");
        t("side.start", "<au>Gleich geht's los</au>", "<au>Starting soon</au>");
        t("side.time", "<gray>Zeit: <white><time></white></gray>", "<gray>Time: <white><time></white></gray>");
        t("side.score", "<village><gray>: <white><n></white> Punkte</gray>", "<village><gray>: <white><n></white> points</gray>");
        t("side.own", "<gray>Euer Baum: <state></gray>", "<gray>Your pole: <state></gray>");
        t("side.enemy", "<gray>Der andere: <state></gray>", "<gray>The other: <state></gray>");
        t("side.tree", "<village><gray>: <state></gray>", "<village><gray>: <state></gray>");
        t("side.village", "<gray>Dein Dorf:</gray> <village>", "<gray>Your village:</gray> <village>");
        t("side.points", "<gray>Deine Punkte: <au><n></au></gray>", "<gray>Your points: <au><n></au></gray>");
        t("side.spectating", "<gray>Du schaust zu.</gray>", "<gray>You're watching.</gray>");
        t("side.site", "<au>moinmorni.net</au>", "<au>moinmorni.net</au>");
        t("side.hall.players", "<gray>Im Wirtsgarten: <white><n></white></gray>", "<gray>In the inn garden: <white><n></white></gray>");
        t("side.hall.start", "<gray>Start in <au><secs> s</au></gray>", "<gray>Start in <au><secs> s</au></gray>");
        t("side.hall.wait", "<gray>Warte auf Mitspieler</gray>", "<gray>Waiting for players</gray>");
        t("side.hall.wins", "<gray>Deine Siege: <au><n></au></gray>", "<gray>Your wins: <au><n></au></gray>");
        t("side.hall.captures", "<gray>Heimgebracht: <au><n></au></gray>", "<gray>Brought home: <au><n></au></gray>");

        // ---------- Ende und Auslösen ----------
        t("end.win.title", "<em>Gewonnen!</em>", "<em>Victory!</em>");
        t("end.win.sub.capture", "<gray>Ihr habt den Maibaum von</gray> <village> <gray>heimgebracht!</gray>",
                "<gray>You brought home the maypole of</gray> <village><gray>!</gray>");
        t("end.win.sub.points", "<gray>Mehr Punkte als</gray> <village>", "<gray>More points than</gray> <village>");
        t("end.win.sub.walkover", "<gray>Bei</gray> <village> <gray>ist niemand mehr da.</gray>",
                "<gray>Nobody is left in</gray> <village><gray>.</gray>");
        t("end.lose.title", "<village> <gray>gewinnt</gray>", "<village> <gray>wins</gray>");
        t("end.lose.sub.capture", "<gray>Euer Maibaum steht jetzt in</gray> <village> <gray>– Zeit zum Auslösen!</gray>",
                "<gray>Your maypole now stands in</gray> <village> <gray>– time to pay the ransom!</gray>");
        t("end.lose.sub.points", "<gray>Nach Punkten knapp vorbei.</gray>", "<gray>Just short on points.</gray>");
        t("end.lose.sub.walkover", "<gray>Ihr wart nicht mehr genug.</gray>", "<gray>Not enough of you were left.</gray>");
        t("end.draw.title", "<au>Unentschieden!</au>", "<au>A draw!</au>");
        t("end.draw.sub", "<gray>Beide Bäume bleiben, wo sie sind.</gray>", "<gray>Both poles stay where they are.</gray>");
        t("end.capture", "<winners> <gray>hat den Maibaum von</gray> <losers> <gray>heimgebracht und gewinnt!</gray>",
                "<winners> <gray>brought home the maypole of</gray> <losers> <gray>and wins!</gray>");
        t("end.points", "<gray>Die Zeit ist um –</gray> <winners> <gray>gewinnt nach Punkten (</gray><vblue> <white><pa></white> <gray>:</gray> <white><pb></white> <vred><gray>).</gray>",
                "<gray>Time's up –</gray> <winners> <gray>wins on points (</gray><vblue> <white><pa></white> <gray>:</gray> <white><pb></white> <vred><gray>).</gray>");
        t("end.draw", "<gray>Die Zeit ist um – unentschieden (</gray><vblue> <white><pa></white> <gray>:</gray> <white><pb></white> <vred><gray>).</gray>",
                "<gray>Time's up – a draw (</gray><vblue> <white><pa></white> <gray>:</gray> <white><pb></white> <vred><gray>).</gray>");
        t("end.walkover", "<winners> <gray>gewinnt – im anderen Dorf ist niemand mehr da.</gray>",
                "<winners> <gray>wins – nobody is left in the other village.</gray>");
        t("end.mine", "<gray>Deine Runde: <au><points></au> Punkte · <white><tags></white> abgefangen · <white><lifts></white> umgelegt · <white><carried></white> Blöcke getragen</gray>",
                "<gray>Your round: <au><points></au> points · <white><tags></white> caught · <white><lifts></white> felled · <white><carried></white> blocks carried</gray>");
        t("end.best", "<au>Neue Bestleistung:</au> <gray><white><n></white> Punkte in einer Runde!</gray>",
                "<au>New personal best:</au> <gray><white><n></white> points in one round!</gray>");
        t("feast.ransom", "<au>Wie es der Brauch will:</au> <gray>Ein geklauter Maibaum wird ausgelöst!</gray> <losers> <gray>spendiert</gray> <winners> <gray>eine zünftige Brotzeit und ein Fass Bier – dann gibt's den Baum zurück.</gray>",
                "<au>As the custom goes:</au> <gray>a stolen maypole must be ransomed!</gray> <losers> <gray>treats</gray> <winners> <gray>to a hearty snack and a barrel of beer – then the pole is given back.</gray>");
        t("feast.points", "<gray>Kein Baum kam heim – aber</gray> <losers> <gray>spendiert</gray> <winners> <gray>trotzdem eine Runde Brezn.</gray>",
                "<gray>No pole made it home – but</gray> <losers> <gray>still treats</gray> <winners> <gray>to a round of pretzels.</gray>");
        t("feast.draw", "<gray>Beide Dörfer feiern zusammen in der Mitte – eine Brotzeit für alle!</gray>",
                "<gray>Both villages celebrate together in the middle – a hearty snack for everyone!</gray>");
        t("feast.cheer", "<au>Prost!</au> <gray>Ein Hoch auf</gray> <village><gray>!</gray>",
                "<au>Cheers!</au> <gray>Three cheers for</gray> <village><gray>!</gray>");

        // ---------- Zuschauen, Verlassen ----------
        t("spectate.start", "<gray>Du schaust der Runde auf <sky><map></sky> zu. Zurück mit <white>/verlassen</white>.</gray>",
                "<gray>You're watching the round on <sky><map></sky>. Back with <white>/leave</white>.</gray>");
        t("spectate.none", "<gray>Gerade läuft keine Runde.</gray>", "<gray>No round is running right now.</gray>");
        t("spectate.playing", "<gray>Du spielst gerade selbst mit.</gray>", "<gray>You're playing yourself right now.</gray>");
        t("leave.none", "<gray>Du bist in keiner Runde.</gray>", "<gray>You're not in a round.</gray>");
        t("leave.done", "<gray>Zurück im Wirtsgarten.</gray>", "<gray>Back in the inn garden.</gray>");

        // ---------- Statistik ----------
        t("stats.head", "<au>Statistik von <white><name></white></au>", "<au>Stats of <white><name></white></au>");
        t("stats.none", "<gray>Von <white><name></white> gibt es noch keine Werte.</gray>",
                "<gray>No stats for <white><name></white> yet.</gray>");
        t("stats.lines", "<gray>Runden: <white><games></white> · Siege: <au><wins></au> · Unentschieden: <white><draws></white></gray>\n<gray>Abgefangen: <white><tags></white> · Bäume gerettet: <white><saves></white></gray>\n<gray>Umgelegt: <white><lifts></white> · Heimgebracht: <au><captures></au></gray>\n<gray>Getragen: <white><carried></white> Blöcke · Punkte: <white><points></white></gray>\n<gray>Beste Runde: <white><best></white> Punkte</gray>",
                "<gray>Rounds: <white><games></white> · Wins: <au><wins></au> · Draws: <white><draws></white></gray>\n<gray>Caught: <white><tags></white> · Poles saved: <white><saves></white></gray>\n<gray>Felled: <white><lifts></white> · Brought home: <au><captures></au></gray>\n<gray>Carried: <white><carried></white> blocks · Points: <white><points></white></gray>\n<gray>Best round: <white><best></white> points</gray>");
        t("top.head", "<au>Die besten Maibaumdiebe</au>", "<au>The best maypole thieves</au>");
        t("top.empty", "<gray>Noch niemand – sei der Erste!</gray>", "<gray>Nobody yet – be the first!</gray>");
        t("top.row", "<au><pos>.</au> <white><name></white> <gray>– <au><wins></au> Siege, <white><captures></white> heimgebracht, <white><points></white> Punkte</gray>",
                "<au><pos>.</au> <white><name></white> <gray>– <au><wins></au> wins, <white><captures></white> brought home, <white><points></white> points</gray>");

        // ---------- Tafeln im Wirtsgarten ----------
        t("board.title", "<au><b>Die besten Maibaumdiebe</b></au>", "<au><b>The best maypole thieves</b></au>");
        t("board.table", "<sky><b><map></b></sky>\n<gray><desc></gray>\n<state>", "<sky><b><map></b></sky>\n<gray><desc></gray>\n<state>");
        t("board.table.running", "<au>Runde läuft – Kompass: zuschauen</au>", "<au>Round running – compass: watch</au>");
        t("board.table.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("board.welcome", "<em><b>⚑ Maibaumklau ⚑</b></em>\n<gray>Zwei Dörfer, zwei Maibäume – wer klaut zuerst?</gray>",
                "<em><b>⚑ Maypole Raid ⚑</b></em>\n<gray>Two villages, two maypoles – who steals first?</gray>");
        t("board.howto", "<au><b>So geht's</b></au>\n<gray>1. Fremden Baum umlegen</gray>\n<gray>2. Gemeinsam heimtragen</gray>\n<gray>3. Träger abfangen – ohne Schaden</gray>\n<gray>4. Verlierer löst mit Brotzeit aus</gray>",
                "<au><b>How to play</b></au>\n<gray>1. Fell the other pole</gray>\n<gray>2. Carry it home together</gray>\n<gray>3. Catch carriers – no damage</gray>\n<gray>4. Losers pay the ransom</gray>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „⚑ Maibaumklau »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung), z. B. für Namen von Karten. */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }
}
