# Morni Maibaumklau

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter.
- Der Modus heißt **Maibaumklau** (englisch „Maypole Raid“), das Repo `pmornhart/morni-maibaumklau`, die Jar
  `Morni-Maibaumklau.jar`, das Plugin `MorniMaibaumklau`, die Kennung im Netzwerk `maibaumklau`.
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen); `TextsTest` prüft Platzhalter, Zeichen, dass jeder Schlüssel aus dem
  Quelltext einen Text hat, dass jede Karte, jedes Dorf, jede Hilfe und jeder Ausgang einen Namen hat und dass kein
  Platzhalter wie ein Standard-Tag heißt (`<red>`, `<b>` wären Farbe bzw. fett – darum `<vred>`, `<pb>`).
  Eigene Farb-Tags: `<sky>` Himmelblau, `<au>` Gold, `<em>` Smaragd, `<bl>` Blau (Dorf), `<rt>` Rot (Dorf).
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits auf Deutsch mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (nichts von Mojang, Nintendo, Hypixel oder anderen Servern
  und Spielen): nur der bayerische Brauch als Vorbild, eigene Dörfer, eigene Namen, eigene Karten, eigene Bauten.
- **Ohne Waffen-Gemetzel:** In der ganzen Welt nimmt niemand Schaden; abgefangen wird mit einem Schlag oder durch
  Berühren. Nichts für echtes Geld (es gibt keine Währung).
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Maibaumklau muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`; Sprache dann aus dem Client).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
- Regeln ohne Server (Maibaum-Zustand, Umlegen, Wache, Tempo der Träger, wer wen abfangen darf, Punkte, Dörfer
  einteilen, Abstimmung, Zurück ins Spiel, Wartezeiten der Hilfen) gehören in `logic/`, Daten in `data/` – ohne Bukkit,
  mit Tests. Werte dort nur zusammen mit den Tests ändern.
- Baupläne (`world/`) arbeiten nur mit Blocknamen als Text – so prüft `MapPlanTest` ohne Server, dass jede Karte zu Fuß
  spielbar ist (von jedem Wirtshaus zum anderen Dorfplatz), dass sich die Maibäume sauber wegnehmen lassen, dass
  Startplätze frei sind und dass es nur Blöcke von 26.x gibt (z. B. `iron_chain`, nicht `chain`).
- Ändern sich Wirtsgarten oder Karten: `Construction.VERSION` erhöhen – dann baut der Server beim nächsten Start neu.
  Nach jeder Runde stellt `Construction.restore` die Karte Block für Block nach dem Bauplan wieder her.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Maibaumklau.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echter Client aus `moinmorni.net/tools/mc-client`,
  Texturen und Bilder nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Maibaumklau nur die
  `config.yml` und Verwaltungs-Befehle zum Testen (`/maibaumadmin`); die Anbindung an den Einstellungs-Katalog gehört
  ins Netzwerk-Repo.
- Bedrock-Spieler (Geyser) spielen mit: alle Menüs sind Truhen-Fenster, alle Hilfen per Rechtsklick in der Schnellleiste,
  Abfangen geht auch durch bloßes Berühren. Block-Anzeigen (das getragene Baum-Modell) sieht Bedrock nicht – dafür
  leuchten die Träger, und Partikel in der Dorffarbe zeigen, wo der Baum ist.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README).
- `STAND.md` nach jedem größeren Schritt aktualisieren.
