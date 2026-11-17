#!/usr/bin/env bash
# Echter Test von Morni Maibaumklau: baut das Plugin und das Test-Netzwerk, richtet einen Paper-Server ein (offline,
# eigener Port, RCON an), startet ihn, schickt Test-Spieler (mineflayer) durch alle Abläufe und hält ihn wieder an.
#
#   test/run.sh                 # alles
#   KEEP=1 test/run.sh          # Server danach laufen lassen (zum Nachschauen, z. B. für Bilder)
#   KEEP=1 SKIP_TEST=1 test/run.sh   # nur Server starten
#   FRESH=1 test/run.sh         # Welt vorher löschen (alles wird neu gebaut)
#
# Erwartet (Vorgaben für die MoinMorni-Sandbox, alles per Umgebungsvariable änderbar):
#   MB_JAVA       Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR     Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE      ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert,
#                 damit nichts heruntergeladen werden muss) – optional
#   MB_NODE_MODULES  node_modules mit mineflayer
#   EXTRA_PLUGINS weitere Jars für den Server (z. B. ViaVersion + ViaBackwards, damit ein Client 26.3 hineinkommt)
#   PORT / RCON_PORT  Vorgabe 29000 / 29010
#   MEMORY        höchster Arbeitsspeicher des Servers (Vorgabe 2G)
#   MIN_FREE_MB   gestartet wird erst, wenn so viel Arbeitsspeicher frei ist (Vorgabe 3500)
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${MB_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
SERVER="${SERVER_DIR:-$CACHE/servers/maibaumklau-test}"
PORT="${PORT:-29000}"
RCON_PORT="${RCON_PORT:-29010}"
MEMORY="${MEMORY:-2G}"
MIN_FREE_MB="${MIN_FREE_MB:-3500}"
export NODE_PATH="${MB_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"
NICE="nice -n ${NICE_LEVEL:-15}"

echo "== Bauen"
(cd "$REPO" && $NICE mvn -q -B -o -DskipTests package)
(cd "$REPO" && $NICE mvn -q -B -o -f test/fake-network/pom.xml package)

echo "== Server einrichten: $SERVER"
if [ "${FRESH:-0}" = "1" ]; then
  # Welt und Bau-Stand vergessen: alles wird neu gebaut (Paper 26 legt eigene Welten unter world/dimensions/ ab).
  rm -rf "$SERVER/maibaumklau" "$SERVER/world/dimensions/minecraft/maibaumklau" "$SERVER/plugins/MorniMaibaumklau/bau.yml"
fi
mkdir -p "$SERVER/plugins/MorniMaibaumklau" "$SERVER/plugins/MoinMorni"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<EOP
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1}],"biome"\:"minecraft\:plains"}
generate-structures=false
spawn-protection=0
view-distance=8
simulation-distance=6
max-players=20
white-list=false
enforce-whitelist=false
motd=Morni Maibaumklau Test
allow-flight=true
EOP
# Testwerte: kurzer Countdown, schnelles Umlegen und Zurückholen, Leitern verschwinden schnell, 20 s Zurück ins Spiel.
cat > "$SERVER/plugins/MorniMaibaumklau/config.yml" <<'EOC'
welt: maibaumklau
rueckkehr-sekunden: 20
wirtsgarten:
  countdown-sekunden: 8
  mindestens-spieler: 4
runde:
  hoechstens-je-dorf: 8
  start-sekunden: 30
  minuten: 6
  nachspiel-sekunden: 8
maibaum:
  umlegen-sekunden: 4
  zurueck-sekunden: 12
  tempo-allein: 0.55
  tempo-je-traeger: 0.12
  tempo-hoechstens: 0.9
  eigener-baum-muss-stehen: false
hilfen:
  brotzeit-sekunden: 4
  brotzeit-pause: 20
  heusprung-pause: 12
  leitern: 4
  leiter-sekunden: 8
  verschnauf-sekunden: 2
EOC
# Test-Netzwerk: Bert spricht Englisch, Anna und Carl sind eine Party (Anna führt).
cat > "$SERVER/plugins/MoinMorni/fake.properties" <<'EOF2'
lang.Bert=en
party.Anna=Anna,Carl
EOF2
rm -f "$SERVER/plugins/MoinMorni/calls.log"
rm -rf "$SERVER/plugins/MorniMaibaumklau/spieler" "$SERVER/plugins/MorniMaibaumklau/bestenliste.yml"
rm -f "$SERVER/plugins/"Morni-Maibaumklau*.jar "$SERVER/plugins/"MoinMorni-FakeNetwork*.jar
cp "$REPO/target/Morni-Maibaumklau.jar" "$SERVER/plugins/"
cp "$REPO/test/fake-network/target/MoinMorni-FakeNetwork.jar" "$SERVER/plugins/"
for jar in ${EXTRA_PLUGINS:-}; do
  cp "$jar" "$SERVER/plugins/"
done

echo "== Warten auf freien Arbeitsspeicher (mindestens $MIN_FREE_MB MB)"
until [ "$(free -m | awk '/^Mem:/ {print $7}')" -ge "$MIN_FREE_MB" ]; do
  echo "   frei: $(free -m | awk '/^Mem:/ {print $7}') MB – warte …"
  sleep 45
done

echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
cd "$SERVER"
: > console.log
$NICE "$JAVA" -Xms256M -Xmx"$MEMORY" ${JAVA_OPTS:-} -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
PID=$!
echo "$PID" > "$SERVER/.pid"
stop_server() {
  if kill -0 "$PID" 2>/dev/null; then
    node "$HERE/rcon.cjs" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" stop >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi

for _ in $(seq 1 600); do
  if grep -q "Maibaumklau ist bereit" console.log; then break; fi
  if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
  sleep 1
done
grep -q "Maibaumklau ist bereit" console.log || { echo "Maibaumklau wurde nicht bereit:"; tail -40 console.log; exit 1; }
grep -E "Fertig gebaut|wiederhergestellt|Maibaumklau ist bereit|Netzwerk|Unbekannter Block" console.log || true

if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "Server läuft (PID $PID) – anhalten mit: node $HERE/rcon.cjs 127.0.0.1:$RCON_PORT $SERVER/.rcon-pass stop"
  exit 0
fi

echo "== Test-Spieler"
STATUS=0
node "$HERE/maibaumklau-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?

echo "== Fehler im Server-Log:"
if grep -qE "ERROR|Exception" console.log; then
  grep -E "ERROR|Exception" console.log | head -20
else
  echo "keine"
fi
exit $STATUS
