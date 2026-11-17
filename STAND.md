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
