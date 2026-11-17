# Stand – Morni Maibaumklau

**Was es ist:** Der Brauch-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniMaibaumklau`, Java 25, Paper 26.x): zwei
Dörfer (Blau und Rot, je 1–8 Leute), jedes mit geschmücktem Maibaum. Wer den Baum des anderen Dorfs umlegt und auf den
eigenen Dorfplatz trägt, gewinnt; sonst nach Ablauf der Zeit die Punkte. Am Ende wird der Baum – wie beim echten
Brauch – mit einer Brotzeit ausgelöst. Kein Schaden, keine Waffen, keine Währung. Netzwerk-Kennung `maibaumklau`,
englisch „Maypole Raid“. Philip legt das Repo `pmornhart/morni-maibaumklau` noch an – bis dahin nur lokal.

## Aufbau (`src/main/java/de/moinmorni/maibaumklau/`)

- `MaibaumklauPlugin` (Start, Welt, Takt), `Settings`, `Texts` (de/en), `Ui`, `Items`, `Holo`, `Sidebar`, `Boards`,
  `Commands` (`/karte`, `/bereit`, `/zuschauen`, `/verlassen`, `/stats`, `/top`, `/maibaumadmin`).
- `logic/` (ohne Server, mit Tests): `Side`, `Pole` (Zustand eines Baums: umlegen, Wache, tragen, liegen, zurück,
  Fortschritt), `Rules` (Abstände, Tempo der Träger, wer wen abfangen darf), `Score`, `Outcome`, `Teams` (Dörfer
  einteilen, Partys zusammen), `Groups`, `Votes`, `Comeback`, `Cooldowns`, `Help`.
- `data/`: `PlayerData`, `Store`, `Leaderboard`.
- `world/` (Baupläne als Text, ohne Server prüfbar): `Plan`, `Blocks`, `Decor`, `Kit` (Häuser), `Maypole`, `Village`,
  `BachPlan`, `AlmPlan`, `HallPlan` (Wirtsgarten), `MapLayout`, `MapType`, `Construction` (Bau-Version, Neubau,
  Wiederherstellung nach jeder Runde), `VoidGenerator`.
- `game/`: `Game` (Ablauf, Umlegen, Tragen, Abfangen, Hilfen, Heuwagen, Zurück ins Spiel, Auslösen, Statistik),
  `Games`, `Villager`, `TreeModel` (getragener Baum aus Block-Anzeigen).
- `hall/`: `Hall`, `HallMenus`. `menu/`, `listen/` (`PlayerListener`, `GameListener`, `GuardListener`), `net/`.
- `test/`: `run.sh` (Ports 29000/29010), `maibaumklau-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (17.11.2026)

- Version 0.1.0 (Branch `claude/erster-stand`, nur lokal, wartet auf das Repo und Philips Go).
- Regeln: Umlegen am fremden Baum (allein 8 s, zu zweit doppelt, ab drei dreimal so schnell; jede Wache hält einen
  Dieb auf), Träger langsam (allein 55 %, je weiterer +12 %, höchstens 90 %) und leuchtend, Abfangen ohne Schaden (eigene
  Hälfte: Schlag; Träger überall, schon durch Berühren – dann liegt der Baum, auch wenn andere mittrugen), liegender
  Baum zurück nach 20 s (Wache daneben 2×/3× so schnell), ins Wasser/in die Tiefe → sofort zurück, Sieg durch
  Heimbringen oder nach 8 Minuten nach Punkten, kampflos wenn ein Dorf leer ist. Auslösen: alle auf dem Platz der
  Sieger, Brotzeit, Musik, Feuerwerk, „Prost!“. Hilfen: Stecken, Sprint-Brotzeit, Heubündel-Sprung, Leitern
  (verschwinden wieder), Kompass; Heuwagen auf den Karten.
- Karten: „Zwei Dörfer am Bach“ (Oberbach/Unterbach, drei Brücken, Heuwagen übers Wasser) und „Almdörfer mit
  Hängebrücke“ (Hinteralm/Vorderalm, Schlucht mit Hängebrücke, Baumstamm, Felsbogen). Neubau ≈ 810 000 Blöcke in 36 s.
- Geprüft: 51 Unit-Tests (u. a. Spielbarkeit jeder Karte zu Fuß, saubere Maibäume, freie Startplätze, nur 26.x-Blöcke,
  config.yml = Vorgaben), übersetzt gegen Paper 26.3 und 26.1. Echter Test (`test/run.sh`, Paper 26.1.2, Test-Netzwerk,
  sechs Test-Spieler): **108/108**, Server-Log ohne Fehler.
- Gefunden beim echten Test: mineflayer (bis 26.1) wendet Geschwindigkeiten vom Server nicht an – der Heubündel-Sprung
  wird darum über das Paket geprüft (Server schickt 1,05 vor / 0,75 hoch). Heuballen als Gegenstand löste beim
  Rechtsklick in die Luft nichts aus → Heubündel (Weizen); Leitern ließen sich nicht an Wände setzen, weil das Klicken
  auf jeden Block gesperrt war → nur noch Blöcke sperren, die selbst etwas tun.
- Bilder mit echtem Client 26.3 (über ViaVersion, nur lokal unter `~/mm-cache/bilder-maibaumklau/`): Wirtsgarten,
  beide Karten von oben, Dorfplatz mit Maibaum, Mittelbrücke, Heuwagen, Hängebrücke, Schlucht, Almdorf, getragener
  Baum, leerer Dorfplatz nach dem Umlegen, Auslösen mit Brotzeit.

## Offen

- Philip: Repo `pmornhart/morni-maibaumklau` anlegen; dann Branch `claude/erster-stand` pushen; Go für `main`.
- Netzwerk-Repo: `DEFAULT_MODES`-Eintrag, `MODE_REPOS` (`maibaumklau: 'pmornhart/morni-maibaumklau'`), `MODE_JARS`
  (`Morni-Maibaumklau.jar`), Lobby-Figur, Webseite (Modus-Seite, Statistik `games`, `wins`, `draws`, `tags`, `saves`,
  `lifts`, `captures`, `carried`, `points`, Bestwert `best_points`), Einstellungs-Katalog (alle Schlüssel der
  config.yml, Befehl z. B. `/maibaumconfig`).
- Nicht echt geprüft: Bedrock (Geyser zeigt das Baum-Modell nicht – dafür Leuchten und Partikel), echte Spieler
  (Gleichgewicht, Tempo, Zeiten), Heubündel-Sprung und Heuwagen im echten Client, Paper 26.3 als Server (nur gebaut).
