# Morni Maibaumklau

Der Brauch-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Zwei Dörfer, zwei geschmückte Maibäume – wer
den Baum des anderen Dorfs umlegt und ins eigene Dorf trägt, gewinnt.** Und wie es der Brauch will, muss das andere Dorf
seinen Baum am Ende mit einer Brotzeit auslösen. 2 bis 16 Leute in zwei gleich großen Dörfern, ganz ohne Waffen. Ein
Paper-Plugin für Paper 26.x (Java 25), im Netzwerk und auch ganz allein.

Englischer Name: **Maypole Raid**.

## So wird gespielt

1. Im **Wirtsgarten „Zum Maibaum“** stimmt man mit dem **Kompass** über die Karte ab, liest im **Buch**, wie es geht,
   und sagt mit dem **grünen Farbstoff** „bereit“. Ab 4 Leuten (einstellbar) läuft ein Countdown; sind alle bereit,
   geht es nach 5 Sekunden los. Die Leute werden auf zwei gleich große Dörfer verteilt (Blau und Rot), Partys bleiben
   zusammen, solange es gerecht bleibt.
2. Jedes Dorf startet vor seinem **Wirtshaus**, gleich hinter dem eigenen Dorfplatz mit dem Maibaum (weiß-blau bzw.
   rot-weiß gestreift, mit Kranz, Zunftzeichen und grüner Krone) und dem Wachhäusl der Maibaumwache.
3. **Umlegen:** Wer am fremden Baum steht, legt ihn um (Vorgabe 8 Sekunden allein, zu zweit doppelt, ab drei Leuten
   dreimal so schnell). Jede Wache des anderen Dorfs am Baum hält einen Dieb auf – stehen gleich viele dabei, geht
   nichts voran. Das Dorf bekommt eine Warnung, sobald es losgeht.
4. **Tragen:** Der umgelegte Baum schwebt waagrecht über den Köpfen der Träger. Träger sind langsam (allein 55 % des
   normalen Tempos) und leuchten in ihrer Dorffarbe; jeder aus dem eigenen Dorf, der nah mitläuft, trägt mit und macht
   alle schneller (je Träger +12 %, höchstens 90 %). Mit dem Baum auf der Schulter gehen keine Hilfen.
5. **Abfangen, ohne dass es weh tut:** Auf der eigenen Hälfte fängt man Eindringlinge mit einem Schlag (Hand oder
   Abfang-Stecken) ab. Träger fängt man überall ab – schon wer sie berührt. Wer abgefangen wird, muss zurück vors
   eigene Wirtshaus und kurz verschnaufen. Trug er den Baum, liegt der jetzt da.
6. **Liegender Baum:** Das andere Dorf kann ihn nach kurzer Zeit wieder aufheben. Sonst gilt er nach 20 Sekunden wieder
   als zurück und steht auf seinem Platz – steht die eigene Wache daneben, geht es doppelt (ab zwei Wachen dreimal) so
   schnell. Fällt der Baum ins Wasser oder in die Tiefe, ist er sofort wieder daheim.
7. **Gewonnen** hat das Dorf, das den fremden Baum auf seinen Dorfplatz bringt. Bringt bis zum Ende der Zeit (Vorgabe
   8 Minuten) keines den Baum heim, gewinnen die Punkte.
8. **Auslösen:** Am Ende treffen sich alle auf dem Platz der Sieger. Der geklaute Baum liegt davor, Brezn, Brotzeit,
   Krüge und ein Fass schweben um den Maibaum, es gibt eine kleine Blasmusik-Melodie, Feuerwerk und ein „Prost!“ – und
   das Dorf, das seinen Baum verloren hat, spendiert die Brotzeit. Danach geht es zurück in den Wirtsgarten, und die
   Karte wird Block für Block wiederhergestellt.

**Punkte:** jemanden abfangen +2, einen Träger abfangen (Baum gerettet) +5, umlegen +10 (geteilt unter allen, die
dabei waren), jedes neue Zehntel des Wegs mit dem Baum +1 je Träger (+1 für den vordersten), heimbringen +15 je
Träger. Die Punkte eines Dorfs sind die Summe seiner Leute.

**Hilfen in der Schnellleiste** (alles kostenlos, nichts zu kaufen, alles per Rechtsklick – auch auf Bedrock):
Abfang-Stecken, **Sprint-Brotzeit** (kurz schneller, dann Pause), **Heuballen-Sprung** (großer Satz nach vorn und oben),
**Leitern** (an Wände setzen, verschwinden nach 30 Sekunden wieder, jeder kann sie abbauen), **Maibaum-Kompass**
(zeigt zum fremden Baum, Rechtsklick: Zustand beider Bäume). Nach dem Abfangen gibt es alles wieder voll.

**Zurück ins Spiel:** Verliert jemand mitten in der Runde die Verbindung (zum Beispiel, weil das Spiel abstürzt), bleibt
sein Platz im Dorf eine Weile frei (`rueckkehr-sekunden`, Vorgabe 90). Kommt er rechtzeitig wieder – direkt oder über
„Zurück ins Spiel“ in der Lobby –, spielt er dort weiter, wo er war (gleiches Dorf, gleiche Sachen). Trug er den Baum,
liegt der inzwischen da. Kommt er zu spät, ist er raus (die Runde zählt als gespielt). Sind alle gerade weg, hält die
Runde an.

## Karten (eigene Entwürfe, aus dem Code gebaut)

| Karte | Dörfer | Dazwischen |
|---|---|---|
| **Zwei Dörfer am Bach** – eine Wiese in den Bergen | Oberbach (Blau, Westen), Unterbach (Rot, Osten): Dorfplatz mit Maibaum, Wachhäusl, Wirtshaus, vier Häuser, Brunnen, Gärten, Heuballen als Deckung | ein Bach, der sich von Nord nach Süd schlängelt (zum Waten zwei Blöcke tief), drei Holzbrücken (die mittlere mit Dach), zwei Heuwagen an jedem Ufer werfen einen übers Wasser; Weizenfelder, Heuschober, Obstbäume, Mühle, Wegkreuz |
| **Almdörfer mit Hängebrücke** – zwei Almen auf hohen Felsen | Hinteralm (Blau), Vorderalm (Rot): Almhütten aus Holz, Aussichtsfels mit Heuwagen | eine tiefe Schlucht mit Wildbach und Wasserfall; hinüber nur über die Hängebrücke (hängt in der Mitte durch), einen schmalen Baumstamm oder den Felsbogen – wer fällt, ist wieder im Dorf, ein getragener Baum ebenso |

Wirtsgarten und Karten entstehen beim ersten Start in einer eigenen leeren Welt (`maibaumklau`); auf jeder Karte läuft
höchstens eine Runde gleichzeitig. Ein Weidezaun mit unsichtbarer Wand hält alle auf der Karte.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/karte [bachdoerfer\|almdoerfer]` | über die Karte abstimmen (ohne Angabe: Menü) – auch `/map`, `/vote` |
| `/bereit` | bereit melden – auch `/ready` |
| `/zuschauen [spieler\|karte]` | einer Runde zuschauen – auch `/spectate` |
| `/verlassen` | Runde oder Zuschauen verlassen – auch `/leave` |
| `/stats [spieler]` · `/top` | Statistik und die besten Maibaumdiebe |
| `/maibaumadmin info\|status\|start [karte]\|vorspulen <los\|ende\|sekunden> [karte]\|stopp [karte]\|wiederherstellen\|neubauen\|neuladen` | Verwaltung und Tests (`moinmorni.maibaumklau.admin`) – auch `/mbadmin` |

Einstellungen stehen nur in der `config.yml` (mit Erklärungen): Zurück-ins-Spiel-Zeit, Countdown, Mindestzahl, Leute je
Dorf, Start- und Spielzeit, Nachspiel, Umlegen, Zurück, Tempo der Träger, „eigener Baum muss stehen“, Hilfen. Es gibt
keine Einstellungs-Befehle – im Netzwerk kommen sie über den Einstellungs-Katalog des Panels.

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Maibaumklau nutzt sie,
wenn sie da ist (`softdepend`): Sprache, Party (zusammen ins selbe Dorf), Statistik, Zustand, zurück zur Lobby. Ohne
Netzwerk kommt die Sprache aus dem Client.

Ab Version 2 der Schnittstelle meldet Maibaumklau, wer gerade **spielt** (`playing`: vom Rundenstart bis zum Ende oder
Verlassen – so lange fragt „/lobby“ erst nach), schickt das Bett im Wirtsgarten über `askToLobby` in die Lobby und hält
nach einem Verbindungsabbruch den Platz frei (`holdSlot(uuid, sekunden)`, nach der Rückkehr, bei Rundenende oder zu spät
`holdSlot(uuid, 0)`). Ein älteres Netzwerk-Plugin kennt diese Methoden nicht – dann geht es ohne Nachfrage und ohne
Angebot in der Lobby weiter wie bisher.

Statistik nach jeder Runde (`stats("maibaumklau", …)`, höher ist besser):

- hochgezählt: `games`, `wins`, `draws`, `tags` (abgefangen), `saves` (Träger abgefangen), `lifts` (umgelegt),
  `captures` (heimgebracht), `carried` (Blöcke mit dem Baum), `points`
- Bestwert: `best_points` (meiste Punkte in einer Runde)

Zustand: `waiting` oder `running`, dazu die Spielerzahl. Lokal: `spieler/<uuid>.yml` und `bestenliste.yml`.

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Maibaumklau.jar` (Version aus `pom.xml`).

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Maibaumklau.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
FRESH=1 test/run.sh              # dasselbe, Welt vorher neu bauen
KEEP=1 SKIP_TEST=1 test/run.sh   # nur den Test-Server starten (z. B. für Bilder)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: Bert spricht Englisch, Anna und Carl sind
eine Party, alle Meldungen landen in `calls.log`), richtet einen Paper-26.1.2-Server ein (offline, Port 29000, RCON
29010, kurze Zeiten, 20 s Zurück ins Spiel), wartet auf genug freien Arbeitsspeicher und spielt zwei Runden durch
(`test/maibaumklau-test.cjs`): am Bach zu viert (Wirtsgarten, Abstimmung, Dörfer, Hilfen, Leiter, Abfangen auf der
eigenen Hälfte, Umlegen mit und ohne Wache, Tragen allein und zu zweit, Träger berühren, liegender Baum mit Wache
zurück, Verbindung weg und rechtzeitig zurück, Heimbringen, Sieg, Auslösen, Statistik, Karte wieder frisch) und auf
der Alm zu fünft mit Zuschauer (Hängebrücke, Absturz in die Schlucht, einen von zwei Trägern berühren, wieder
aufheben, mit dem Baum in die Tiefe, zu spät zurück, Zeit um, Lobby-Knopf). Mit
`EXTRA_PLUGINS="…/ViaVersion.jar …/ViaBackwards.jar"` kommen ViaVersion und ViaBackwards dazu.

---

Verfasst mit Claude 🤖
