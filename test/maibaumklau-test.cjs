/**
 * Echter Test von Morni Maibaumklau mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node maibaumklau-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl, Dora; in Runde 2 kommt Emil dazu, Fritz
 * schaut zu. Sprache und Party kommen vom Test-Netzwerk (test/fake-network), das Statistik, Zustand, „spielt gerade“,
 * „Platz freihalten“ und „zur Lobby“ in calls.log mitschreibt.
 *
 * Runde 1 (Zwei Dörfer am Bach): Wirtsgarten, Abstimmung, Dörfer (Party zusammen), Start vor dem Wirtshaus, Hilfen,
 * Umlegen mit und ohne Wache, Tragen allein und zu zweit (Tempo), Abfangen auf der eigenen Hälfte und eines Trägers
 * durch Berühren, liegender Baum mit Wache zurück, Leiter, Verbindung weg und rechtzeitig zurück, Heimbringen, Sieg,
 * Auslösen, Statistik, Karte wieder frisch. Runde 2 (Almdörfer): fünf Leute, Zuschauer, Hängebrücke, Absturz in die
 * Schlucht (auch mit Baum), Aufheben nach dem Abfangen, zu spät zurück, Zeit um (nach Punkten), Lobby-Knopf.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '29000', RCON = '127.0.0.1:29010', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 12.5 };
const COMEBACK = 20;
const FLOOR = 100;

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const inWorld = (c) => `execute in minecraft:maibaumklau run ${c}`;
const passed = async (c) => /passed/i.test((await cmd(inWorld(c)))[0]);
const playerFile = (name) => path.join(SERVER, 'plugins/MorniMaibaumklau/spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const callsOf = (t, name, from = 0) => calls().slice(from).filter((c) => c.t === t && c.name === name);

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot._client.on('set_title_subtitle', (p) => bot.titles.push(plain(p.text)));
  bot.leaps = [];
  bot._client.on('entity_velocity', (p) => {
    if (p.entityId === bot.entity?.id && p.velocity) bot.leaps.push(p.velocity);
  });
  bot.bossPackets = 0;
  bot._client.on('boss_bar', () => bot.bossPackets++);
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 25000);
  });
  await sleep(1500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const flat = (p, q) => Math.hypot(p.x - q.x, p.z - q.z);
const inHall = (bot) => near(bot.entity.position, HALL, 5) && bot.game.gameMode === 'adventure';
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const sawBar = (bot, re) => bot.bars.some((t) => re.test(t));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });
const invHas = (bot, name) => bot.inventory.items().some((i) => i.name === name);
const invList = (bot) => bot.inventory.items().map((i) => `${i.name}×${i.count}`).sort().join(',');

/**
 * /maibaumadmin status als Werte: {info, <karte>: {phase, zeit, punkte, trees: [{state, lift, liftState, leader, left,
 * carriers, best, base, at, start}], players: {Anna: {side, state, resting, carrying, points, tags, saves, lifts,
 * captures, carried, waiting}}}}
 */
async function status() {
  const lines = (await cmd('maibaumadmin status'))[0].split('\n');
  const out = { info: {} };
  for (const part of lines[0].split(' ')) {
    const [k, v] = part.split('=');
    if (v !== undefined) out.info[k] = v;
  }
  for (const line of lines.slice(1)) {
    if (!line.startsWith('karte=')) continue;
    const s = { players: {}, trees: [{}, {}] };
    for (const part of line.split(' ')) {
      const [k, v] = part.split('=');
      if (v === undefined) continue;
      let m;
      if ((m = /^baum(\d)$/.exec(k))) {
        const [state, lift, liftState, leader, left, carriers, best] = v.split(':');
        Object.assign(s.trees[m[1]], { state, lift: Number(lift), liftState, leader, left: Number(left),
          carriers: Number(carriers), best: Number(best) });
      } else if ((m = /^(fuss|lage|start)(\d)$/.exec(k))) {
        const [x, y, z] = v.split(':').map(Number);
        s.trees[m[2]][{ fuss: 'base', lage: 'at', start: 'start' }[m[1]]] = { x, y, z };
      } else if (/^[A-Z]/.test(k)) {
        if (v === 'zuschauer') {
          s.players[k] = { state: 'zuschauer' };
        } else {
          const [side, state, r, c, p, t, sv, l, h, g, w] = v.split(':');
          const n = (x) => Number(x.slice(1));
          s.players[k] = { side, state, resting: n(r), carrying: n(c), points: n(p), tags: n(t), saves: n(sv),
            lifts: n(l), captures: n(h), carried: n(g), waiting: n(w) };
        }
      } else if (k === 'punkte') {
        s.punkte = v.split(':').map(Number);
      } else {
        s[k] = /^-?\d+(\.\d+)?$/.test(v) ? Number(v) : v;
      }
    }
    out[s.karte] = s;
  }
  return out;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function useItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(200);
  bot.activateItem();
  await sleep(200);
  bot.deactivateItem();
}

/** Bis ein Spieler im Ziel-Bereich ist (Teleport angekommen). */
async function tp(bot, x, y, z, yaw = 0, pitch = 0) {
  await cmd(inWorld(`tp ${bot.username} ${x} ${y} ${z} ${yaw} ${pitch}`));
  await waitFor(() => near(bot.entity.position, { x, y, z }, 1.5), 3000, 100);
  await sleep(250);
}

async function speedOf(name) {
  const out = (await cmd(`attribute ${name} minecraft:movement_speed get`))[0];
  const m = /(-?\d+(\.\d+)?)/.exec(out.replace(name, ''));
  return m ? Number(m[1]) : NaN;
}

async function attack(attacker, victim, tries = 3) {
  for (let i = 0; i < tries; i++) {
    const target = attacker.players[victim.username]?.entity;
    if (target) {
      await attacker.lookAt(target.position.offset(0, 1.4, 0), true);
      attacker.attack(target);
    }
    await sleep(500);
  }
}

/** Steht ein Stamm des Maibaums (Block über dem Sockel)? */
const poleStands = (t) => passed(`execute unless block ${t.base.x} ${t.base.y + 3} ${t.base.z} air`);

async function main() {
  // ------------------------------------------------------------------ Wirtsgarten
  await sleep(6000);
  const holos = String(await cmd(inWorld('execute positioned 0 101 0 if entity @e[type=text_display,distance=..40]')));
  check('Tafeln im Wirtsgarten stehen (5 Tafeln × 2 Sprachen)', /: 10\b/.test(holos), holos.trim());
  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft im Wirtsgarten (Abenteuermodus)', inHall(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Kompass, Farbstoff, Buch, Gold, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 2)?.name === 'writable_book' &&
      slot(anna, 4)?.name === 'gold_ingot' && slot(anna, 8)?.name === 'red_bed', [0, 1, 2, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Abstimmen“', plain(slot(anna, 0)?.customName).includes('Abstimmen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Vote“', plain(slot(bert, 0)?.customName).includes('Vote'), plain(slot(bert, 0)?.customName));
  check('Begrüßung + Erklärung im Chat', sees(anna, /Wirtsgarten zum Maibaum/) && sees(anna, /Maibaum/) &&
    sees(bert, /Maypole Inn Garden/));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 3000));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.state === 'waiting'));
  await tp(anna, 0.5, 101, 18.5, 0, 0);
  check('Unsichtbarer Rand: niemand fällt vom Wirtsgarten', await (async () => {
    anna.setControlState('forward', true);
    await sleep(2500);
    anna.setControlState('forward', false);
    return anna.entity.position.y > 99;
  })(), JSON.stringify(anna.entity.position));
  await tp(anna, 0.5, 101, 12.5, 180, 0);

  // ------------------------------------------------------------------ Abstimmen, So geht's
  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Welche Karte?“', win && plain(win.title).includes('Welche Karte'), win && plain(win.title));
  check('Beide Karten im Menü', plain(win?.slots[11]?.customName).includes('Zwei Dörfer am Bach') &&
    plain(win?.slots[15]?.customName).includes('Almdörfer'), [11, 15].map((i) => plain(win?.slots[i]?.customName)).join(' / '));
  await anna.clickWindow(11, 0, 0);
  await sleep(600);
  check('Stimme für „Zwei Dörfer am Bach“', sees(anna, /Deine Stimme: Zwei Dörfer am Bach/));
  bert.chat('/karte bachdoerfer');
  await sleep(800);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Two Villages by the Brook/), bert.chats.slice(-1)[0]);
  win = await openWithItem(anna, 2);
  check('Buch öffnet „So geht Maibaumklau“ mit sechs Kapiteln', win && /So geht Maibaumklau/.test(plain(win.title)) &&
    [10, 11, 12, 14, 15, 16].every((i) => win.slots[i]), win && plain(win.title));
  await anna.clickWindow(14, 0, 0);
  await sleep(500);
  check('Kapitel „Abfangen“ erklärt sich im Chat', sees(anna, /Abfangen: Auf der eigenen Hälfte/), anna.chats.slice(-1)[0]);
  if (anna.currentWindow) anna.closeWindow(anna.currentWindow);

  // ------------------------------------------------------------------ Countdown → Runde 1
  clear(anna, bert);
  await sleep(1500);
  check('Kein Countdown mit 2 von 4 nötigen Leuten', !anna.bars.some((b) => /Nächste Runde/.test(b)), anna.bars.slice(-1)[0]);
  let carl = await join('Carl');
  let dora = await join('Dora');
  check('Mit 4 Leuten läuft der Countdown', await waitFor(() => anna.bars.some((b) => /Nächste Runde in \d+ s · Zwei Dörfer am Bach/.test(b)), 4000),
    anna.bars.slice(-1)[0]);
  for (const b of [anna, bert, carl, dora]) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
  }
  check('Alle bereit (Farbstoff)', await waitFor(() => sees(anna, /Du bist bereit/) && sees(bert, /You're ready/), 3000));
  check('Start-Nachricht', await waitFor(() => sees(anna, /Auf geht's! Karte: Zwei Dörfer am Bach · 4 Leute/), 12000),
    anna.chats.slice(-2).join(' | '));
  let s = (await status()).bachdoerfer;
  const four = [anna, bert, carl, dora];
  check('Zwei Dörfer zu zweit: Party Anna + Carl zusammen, Bert + Dora im anderen', s?.phase === 'START' &&
    s.players.Anna.side === s.players.Carl.side && s.players.Bert.side === s.players.Dora.side &&
    s.players.Anna.side !== s.players.Bert.side, JSON.stringify(s?.players));
  const A = s.players.Anna.side === 'blau' ? 0 : 1; // Annas Dorf
  const B = 1 - A;
  const treeA = s.trees[A];
  const treeB = s.trees[B];
  const villageA = A === 0 ? 'Oberbach' : 'Unterbach';
  const villageB = B === 0 ? 'Oberbach' : 'Unterbach';
  check('Alle stehen vor dem Wirtshaus ihres Dorfs', await waitFor(() => four.every((b) => {
    const t = s.trees[s.players[b.username].side === 'blau' ? 0 : 1];
    return flat(b.entity.position, t.start) < 4 && Math.abs(b.entity.position.y - t.start.y) < 1.5;
  }), 5000), four.map((b) => `${b.username}:${b.entity.position.x.toFixed(1)}`).join(' '));
  check('Begrüßung mit Dorf (de/en) und Ziel', sees(anna, new RegExp(`Willkommen in ${villageA}`)) &&
    sees(anna, new RegExp(`Klaut den Maibaum von ${villageB}`)) && sees(bert, new RegExp(`Welcome to ${villageB}`)),
  anna.chats.slice(-3).join(' | '));
  check('Abenteuermodus und Tracht in der Dorffarbe', anna.game.gameMode === 'adventure' &&
    anna.inventory.slots[5]?.name === 'leather_helmet' && anna.inventory.slots[6]?.name === 'leather_chestplate',
  `${anna.game.gameMode} ${anna.inventory.slots[5]?.name}`);
  const startPos = anna.entity.position.clone();
  anna.setControlState('forward', true);
  await sleep(1500);
  anna.setControlState('forward', false);
  await sleep(500);
  check('Vor dem Start hält einen das Dorf fest', flat(anna.entity.position, startPos) < 1.5, JSON.stringify(anna.entity.position));
  await cmd('maibaumadmin vorspulen los bachdoerfer');
  check('Countdown im Titel und „Los!“', await waitFor(() => sawTitle(anna, /Los!/) && sawTitle(bert, /Go!/), 6000),
    anna.titles.slice(-3).join(' | '));
  check('Überlebensmodus, Hilfen in der Schnellleiste', await waitFor(() => anna.game.gameMode === 'survival' &&
    slot(anna, 0)?.name === 'stick' && slot(anna, 1)?.name === 'bread' && slot(anna, 2)?.name === 'wheat' &&
    slot(anna, 3)?.name === 'ladder' && slot(anna, 3)?.count === 4 && slot(anna, 4)?.name === 'compass', 3000),
  [0, 1, 2, 3, 4].map((i) => `${slot(anna, i)?.name}×${slot(anna, i)?.count}`).join(','));
  check('Netzwerk: Zustand „running“ und alle vier „spielen“', calls().some((c) => c.t === 'state' && c.state === 'running') &&
    ['Anna', 'Bert', 'Carl', 'Dora'].every((n) => callsOf('playing', n).some((c) => c.playing === true)));
  check('Leiste oben (Zeit und Punkte beider Dörfer)', await waitFor(() => anna.bossPackets > 0, 3000), `${anna.bossPackets}`);

  // ------------------------------------------------------------------ Hilfen
  clear(anna);
  await useItem(anna, 1);
  check('Sprint-Brotzeit: „Mahlzeit!“ und schneller (Effekt)', await waitFor(() => sawBar(anna, /Mahlzeit/), 2000) &&
    /speed/.test((await cmd('data get entity Anna active_effects'))[0]), anna.bars.slice(-1)[0]);
  await useItem(anna, 1);
  check('Brotzeit gleich nochmal: erst warten', await waitFor(() => sawBar(anna, /Noch \d+ s warten/), 2000), anna.bars.slice(-1)[0]);
  // mineflayer (bis 26.1) wendet die Geschwindigkeit vom Server nicht an – geprüft wird, was der Server schickt.
  anna.leaps.length = 0;
  await useItem(anna, 2);
  check('Heuballen-Sprung: der Server wirft Anna nach vorn und oben', await waitFor(() => anna.leaps.some((v) => v.y > 0.7 &&
    Math.hypot(v.x, v.z) > 0.9), 2000), JSON.stringify(anna.leaps));
  await useItem(anna, 2);
  check('Heuballen gleich nochmal: erst warten', await waitFor(() => sawBar(anna, /Noch \d+ s warten/), 2000), anna.bars.slice(-1)[0]);
  
  // Leiter an die Wand des Wirtshauses (die Tür liegt in der Mitte der Wand, daneben ist Putz).
  const dirA = A === 0 ? 1 : -1; // Richtung zur Kartenmitte
  const wallX = treeA.base.x - dirA * 10;
  const ladderSpot = { x: wallX + dirA, y: FLOOR + 1, z: treeA.base.z - 2 };
  await tp(anna, ladderSpot.x + 0.5 + dirA * 1.5, FLOOR + 1, ladderSpot.z + 0.5, dirA > 0 ? 90 : -90, 10);
  let placed = false;
  try {
    await anna.equip(anna.inventory.items().find((i) => i.name === 'ladder'), 'hand');
    await anna.placeBlock(anna.blockAt(new Vec3(wallX, FLOOR + 1, ladderSpot.z)), new Vec3(dirA, 0, 0));
    placed = true;
  } catch (e) {
    console.log(`  (Leiter: ${e.message})`);
  }
  check('Leiter an die Wand gesetzt', placed && await waitFor(() => passed(`execute if block ${ladderSpot.x} ${ladderSpot.y} ${ladderSpot.z} ladder`), 2000),
    JSON.stringify(ladderSpot));

  // ------------------------------------------------------------------ Abfangen auf der eigenen Hälfte
  // Carl (Dorf A) läuft in Dorf B hinein; dort schlägt er Bert – Bert ist auf seiner Hälfte sicher.
  const inB = { x: treeB.base.x - dirA * 10, z: treeB.base.z + 3 }; // vor dem Dorfplatz B, Richtung Mitte
  await tp(carl, inB.x + 0.5, FLOOR + 1, inB.z + 0.5, 0, 0);
  await tp(bert, inB.x + 1.7, FLOOR + 1, inB.z + 0.5, 90, 0);
  clear(carl, bert);
  await attack(carl, bert, 2);
  s = (await status()).bachdoerfer;
  check('Carl kann Bert auf Berts Hälfte nicht abfangen', s.players.Bert.resting === 0 && s.players.Carl.tags === 0 &&
    sawBar(carl, /eigenen Hälfte/), `${JSON.stringify(s.players.Bert)} ${carl.bars.slice(-1)[0]}`);
  await attack(bert, carl, 2);
  s = (await status()).bachdoerfer;
  check('Bert fängt Carl auf seiner Hälfte ab (+2, ohne Schaden)', s.players.Bert.tags === 1 && s.players.Bert.points === 2 &&
    carl.health === 20, `${JSON.stringify(s.players.Bert)} Carl ${carl.health}`);
  check('Carl muss zurück vors eigene Wirtshaus und verschnaufen', await waitFor(() => flat(carl.entity.position, treeA.start) < 4, 3000) &&
    s.players.Carl.resting === 1 && await waitFor(() => sawTitle(carl, /Abgefangen/), 2000),
  JSON.stringify(carl.entity.position));
  check('Alle erfahren es (de/en)', await waitFor(() => sees(anna, /Bert hat Carl abgefangen/) && sees(bert, /Bert caught Carl/), 2000));
  check('Abfangen tut nicht weh', four.every((b) => b.health === 20), four.map((b) => b.health).join(','));

  // ------------------------------------------------------------------ Umlegen: Wache hält auf
  await tp(bert, treeB.start.x, treeB.start.y, treeB.start.z);
  clear(anna, bert, dora);
  await tp(dora, treeB.base.x + 0.5 + dirA * 2, FLOOR + 1, treeB.base.z + 0.5, 0, 0);
  await tp(anna, treeB.base.x + 0.5 - dirA * 2, FLOOR + 1, treeB.base.z + 0.5, 0, 0);
  await sleep(1500);
  s = (await status()).bachdoerfer;
  check('Wache am Baum: nichts geht voran („Die Wache steht dabei“)', s.trees[B].liftState === 'GUARDED' &&
    s.trees[B].lift === 0 && sawBar(anna, /Die Wache steht dabei/), `${JSON.stringify(s.trees[B])} ${anna.bars.slice(-1)[0]}`);
  await tp(dora, treeB.start.x + 0.5, treeB.start.y, treeB.start.z - 2);
  check('Ohne Wache wird umgelegt (Fortschritt in der Aktionsleiste)', await waitFor(() => sawBar(anna, /Umlegen … .* \d+ %/), 3000),
    anna.bars.slice(-1)[0]);
  check('Dorf B wird gewarnt (de/en)', await waitFor(() => sawBar(bert, /Your maypole is being felled/) &&
    sees(dora, /Alarm!/), 3000), `${bert.bars.slice(-1)[0]} / ${dora.chats.slice(-1)[0]}`);
  check('Der Baum fällt: Anna trägt ihn', await waitFor(async () => (await status()).bachdoerfer.trees[B].state === 'CARRIED', 8000),
    JSON.stringify((await status()).bachdoerfer.trees[B]));
  s = (await status()).bachdoerfer;
  check('Baum-Blöcke sind weg vom Dorfplatz', !(await poleStands(treeB)), JSON.stringify(treeB.base));
  check(`Meldung „${villageA} hat den Maibaum von ${villageB} umgelegt“ und Titel`, await waitFor(() =>
    sees(dora, new RegExp(`${villageA} hat den Maibaum von ${villageB} umgelegt`)) && sawTitle(anna, /Er fällt/) &&
    sawTitle(bert, /Your maypole/), 3000), dora.chats.slice(-1)[0]);
  check('Umlegen bringt Anna 10 Punkte', s.players.Anna.lifts === 1 && s.players.Anna.points >= 10, JSON.stringify(s.players.Anna));
  check('Das Baum-Modell liegt auf den Schultern (Block-Anzeigen)', /: 1\d\b/.test(String(await cmd(inWorld(
    `execute positioned ${anna.entity.position.x} ${anna.entity.position.y} ${anna.entity.position.z} if entity @e[type=block_display,tag=maibaumklau_baum,distance=..14]`)))));
  const alone = await speedOf('Anna');
  check('Allein tragen: langsam (55 %)', Math.abs(alone - 0.055) < 0.004, `${alone}`);

  // ------------------------------------------------------------------ Tragen zu zweit
  await waitFor(async () => (await status()).bachdoerfer.players.Carl.resting === 0, 4000);
  const ap = anna.entity.position;
  await tp(carl, ap.x + dirA * 1.5, ap.y, ap.z + 1, 0, 0);
  await sleep(800);
  s = (await status()).bachdoerfer;
  const two = await speedOf('Anna');
  check('Carl trägt mit: zwei Träger, beide schneller (67 %)', s.trees[B].carriers === 2 && Math.abs(two - 0.067) < 0.004 &&
    Math.abs((await speedOf('Carl')) - 0.067) < 0.004, `${s.trees[B].carriers} ${two}`);
  await anna.look(dirA > 0 ? Math.PI / 2 : -Math.PI / 2, 0, true);
  anna.setControlState('forward', true);
  await sleep(2500);
  anna.setControlState('forward', false);
  s = (await status()).bachdoerfer;
  check('Getragene Strecke wird gezählt', s.players.Anna.carried >= 1, JSON.stringify(s.players.Anna));
  check('Aktionsleiste der Träger: „Du trägst den Maibaum · 2 Träger“', sawBar(anna, /Du trägst den Maibaum · 2 Träger/),
    anna.bars.slice(-1)[0]);

  await tp(carl, treeA.start.x, treeA.start.y, treeA.start.z);
  await sleep(600);
  check('Carl geht weg: Anna trägt wieder allein (55 %), Carl normal', Math.abs((await speedOf('Anna')) - 0.055) < 0.004 &&
    Math.abs((await speedOf('Carl')) - 0.1) < 0.002 && (await status()).bachdoerfer.trees[B].carriers === 1);

  // ------------------------------------------------------------------ Träger berühren → Baum liegt
  clear(anna, bert, carl, dora);
  const bp = anna.entity.position;
  await cmd(inWorld(`tp Bert ${bp.x + 0.4} ${bp.y} ${bp.z}`));
  check('Bert berührt Anna: abgefangen, der Baum liegt', await waitFor(async () =>
    (await status()).bachdoerfer.trees[B].state === 'DROPPED', 3000), JSON.stringify((await status()).bachdoerfer.trees[B]));
  s = (await status()).bachdoerfer;
  check('Baum gerettet: +5 für Bert, Anna verschnauft daheim, niemand trägt mehr', s.players.Bert.saves === 1 &&
    s.players.Anna.resting === 1 && s.trees[B].carriers === 0 && await waitFor(() => flat(anna.entity.position, treeA.start) < 4, 2000),
  `${JSON.stringify(s.players.Bert)} ${JSON.stringify(s.players.Anna)}`);
  check('Meldung „der Maibaum liegt“ (de/en)', await waitFor(() => sees(carl, /hat Anna abgefangen – der Maibaum von .* liegt/) &&
    sees(bert, /caught Anna – the maypole of .* is down/), 2000), carl.chats.slice(-2).join(' | '));
  check('Tempo der Träger wieder normal', Math.abs((await speedOf('Anna')) - 0.1) < 0.002 &&
    Math.abs((await speedOf('Carl')) - 0.1) < 0.002);
  // Dora bewacht den liegenden Baum.
  const lying = s.trees[B].at;
  await tp(dora, lying.x + 1, lying.y, lying.z);
  const left1 = (await status()).bachdoerfer.trees[B].left;
  await sleep(2000);
  const left2 = (await status()).bachdoerfer.trees[B].left;
  check('Mit Wache daneben geht er doppelt so schnell zurück', left1 - left2 >= 3, `${left1} → ${left2}`);
  check('Der Baum steht wieder auf dem Dorfplatz', await waitFor(async () => (await status()).bachdoerfer.trees[B].state === 'HOME', 12000) &&
    await poleStands(treeB) && await waitFor(() => sees(anna, /steht wieder auf dem Dorfplatz/), 2000), anna.chats.slice(-1)[0]);

  // ------------------------------------------------------------------ Leiter verschwindet wieder
  check('Die Leiter ist nach 8 s wieder weg', await waitFor(() => passed(`execute if block ${ladderSpot.x} ${ladderSpot.y} ${ladderSpot.z} air`), 6000));

  // ------------------------------------------------------------------ Verbindung weg und rechtzeitig zurück
  await tp(dora, treeB.start.x + 0.5, treeB.start.y, treeB.start.z + 3);
  await waitFor(() => dora.entity.onGround, 2000);
  const doraBefore = { pos: dora.entity.position.clone(), items: invList(dora), side: (await status()).bachdoerfer.players.Dora.side };
  let mark = calls().length;
  clear(anna);
  dora.quit();
  check(`Dora verliert die Verbindung: Platz bleibt ${COMEBACK} s frei (holdSlot ${COMEBACK})`,
    await waitFor(() => callsOf('hold-slot', 'Dora', mark).some((c) => c.seconds === COMEBACK), 5000), JSON.stringify(calls().slice(mark)));
  check('Die anderen erfahren es', await waitFor(() => sees(anna, new RegExp(`Dora hat die Verbindung verloren – der Platz bleibt ${COMEBACK} Sekunden frei`)), 3000));
  s = (await status()).bachdoerfer;
  check('Dora gehört noch zum Dorf, die Runde wartet', s.players.Dora.state === 'fort' && s.players.Dora.waiting > 0,
    JSON.stringify(s.players.Dora));
  await sleep(3000);
  dora = await join('Dora');
  check('Dora ist rechtzeitig zurück: „Willkommen zurück!“', await waitFor(() => sawTitle(dora, /Willkommen zurück/) &&
    sees(dora, /Willkommen zurück! Du spielst wieder für/), 4000), `${dora.titles.join(' | ')} / ${dora.chats.slice(-2).join(' | ')}`);
  check('Zurück dort, wo sie war, mit denselben Sachen', near(dora.entity.position, doraBefore.pos, 1.5) &&
    await waitFor(() => invList(dora) === doraBefore.items, 3000) && dora.game.gameMode === 'survival',
  `${JSON.stringify(dora.entity.position)} ${invList(dora)}`);
  s = (await status()).bachdoerfer;
  check('Gleiches Dorf, wieder da; Netzwerk: Platz frei (holdSlot 0) und „spielt“', s.players.Dora.side === doraBefore.side &&
    s.players.Dora.state === 'da' && s.players.Dora.waiting === 0 && callsOf('hold-slot', 'Dora', mark).some((c) => c.seconds === 0) &&
    callsOf('playing', 'Dora', mark).some((c) => c.playing === true), JSON.stringify(s.players.Dora));
  check('Die anderen sehen „Dora ist zurück in der Runde“', await waitFor(() => sees(anna, /Dora ist zurück in der Runde/), 3000));
  four[3] = dora;

  // ------------------------------------------------------------------ Heimbringen → Sieg und Auslösen
  await tp(bert, treeB.start.x, treeB.start.y, treeB.start.z);
  await tp(dora, treeB.start.x, treeB.start.y, treeB.start.z + 2);
  await waitFor(async () => (await status()).bachdoerfer.players.Anna.resting === 0, 4000);
  await tp(anna, treeB.base.x + 0.5 - dirA * 2, FLOOR + 1, treeB.base.z + 0.5, 0, 0);
  check('Anna legt den Baum ein zweites Mal um', await waitFor(async () => (await status()).bachdoerfer.trees[B].state === 'CARRIED', 9000));
  clear(anna, bert, carl, dora);
  await tp(anna, treeA.base.x + 0.5 + dirA * 3, FLOOR + 1, treeA.base.z + 3.5, 0, 0);
  check('Anna bringt ihn auf den eigenen Dorfplatz: Sieg!', await waitFor(() => sawTitle(anna, /Gewonnen/), 4000), anna.titles.join(' | '));
  check(`Alle anderen sehen den Sieger (englisch: „${villageA} wins“)`, await waitFor(() => sawTitle(bert, new RegExp(`${villageA} wins`)) &&
    sawTitle(carl, /Gewonnen/), 3000), `Bert: ${bert.titles.join(' | ')}`);
  check('Brauch im Chat: der Baum wird ausgelöst (de/en)', await waitFor(() => sees(anna, /Wie es der Brauch will/) &&
    sees(dora, new RegExp(`${villageB} spendiert ${villageA} eine zünftige Brotzeit`)) && sees(bert, /As the custom goes/), 3000),
  anna.chats.slice(-3).join(' | '));
  check('Alle stehen zum Auslösen auf dem Platz der Sieger', await waitFor(() => four.every((b) => flat(b.entity.position, treeA.base) < 6), 4000),
    four.map((b) => `${b.username}:${flat(b.entity.position, treeA.base).toFixed(1)}`).join(' '));
  check('Brotzeit schwebt um den Maibaum (Brezn, Krüge, Fass)', /: 9\b/.test(String(await cmd(inWorld(
    `execute positioned ${treeA.base.x} ${treeA.base.y} ${treeA.base.z} if entity @e[tag=maibaumklau_brotzeit,distance=..8]`)))));
  check('Der geklaute Baum liegt vor dem Platz', /: 1\d\b/.test(String(await cmd(inWorld(
    `execute positioned ${treeA.base.x} ${treeA.base.y} ${treeA.base.z} if entity @e[type=block_display,tag=maibaumklau_baum,distance=..20]`)))));
  check('„Prost!“ nach ein paar Sekunden', await waitFor(() => sees(anna, /Prost! Ein Hoch auf/) && sees(bert, /Cheers!/), 7000));
  const stats = calls().filter((c) => c.t === 'stats');
  const st = (n) => stats.filter((c) => c.name === n).slice(-1)[0];
  check('Netzwerk-Statistik „maibaumklau“ für Anna: games 1, wins 1, lifts 2, captures 1, best_points',
    st('Anna')?.mode === 'maibaumklau' && st('Anna').add.games === 1 && st('Anna').add.wins === 1 && st('Anna').add.lifts === 2 &&
      st('Anna').add.captures === 1 && st('Anna').add.points > 20 && st('Anna').best.best_points === st('Anna').add.points &&
      st('Anna').lower === false, JSON.stringify(st('Anna')));
  check('Statistik für alle vier (Bert: wins 0, tags 1, saves 1)', ['Anna', 'Bert', 'Carl', 'Dora'].every((n) => st(n)) &&
    st('Bert').add.wins === 0 && st('Bert').add.tags === 1 && st('Bert').add.saves === 1 && st('Anna').add.carried >= 1,
  stats.map((c) => `${c.name}:${JSON.stringify(c.add)}`).join(' '));
  check('Netzwerk: nach dem Sieg spielt niemand mehr', await waitFor(() => ['Anna', 'Bert', 'Carl', 'Dora'].every((n) =>
    callsOf('playing', n).slice(-1)[0]?.playing === false), 3000));
  check('Zurück in den Wirtsgarten (alle)', await waitFor(() => four.every(inHall), 15000),
    four.map((b) => `${b.username}:${b.game.gameMode}`).join(' '));
  check('Karte wird wiederhergestellt', await waitFor(async () => (await status()).info.frei?.includes('bachdoerfer'), 30000, 1000));
  check('Karte frisch: der geklaute Baum steht wieder in seinem Dorf, Leiter weg',
    await poleStands(treeB) && await poleStands(treeA) && await passed(`execute if block ${ladderSpot.x} ${ladderSpot.y} ${ladderSpot.z} air`));
  check('Keine Reste: kein Baum-Modell, keine Brotzeit', !(await passed('execute positioned 1000 100 0 if entity @e[type=!player,tag=maibaumklau_baum,distance=..150]')) &&
    !(await passed('execute positioned 1000 100 0 if entity @e[tag=maibaumklau_brotzeit,distance=..150]')));
  check('Tempo wieder normal im Wirtsgarten', Math.abs((await speedOf('Anna')) - 0.1) < 0.002);
  const board = readFile(path.join(SERVER, 'plugins/MorniMaibaumklau/bestenliste.yml'));
  check('Bestenliste: Anna mit 1 Sieg und 1 heimgebracht', /name: Anna[\s\S]*?siege: 1[\s\S]*?heimgebracht: 1/.test(board), board.trim().slice(0, 200));
  check('Spielerdatei: 1 Runde, 1 Sieg, 2 umgelegt', /runden: 1/.test(readFile(playerFile('Anna'))) &&
    /siege: 1/.test(readFile(playerFile('Anna'))) && /umgelegt: 2/.test(readFile(playerFile('Anna'))),
  readFile(playerFile('Anna')).trim().replace(/\n/g, ' '));
  const top = (await cmd('top'))[0].replace(/§./g, '');
  check('/top zeigt Anna oder Carl vorn', /1\. (Anna|Carl) – 1 Siege/.test(top), top.replace(/\n/g, ' | ').slice(0, 200));
  clear(anna);
  anna.chat('/stats');
  check('/stats zeigt die eigenen Werte', await waitFor(() => sees(anna, /Heimgebracht: 1/), 3000), anna.chats.slice(-3).join(' | '));
  check('Netzwerk: Zustand wieder „waiting“', await waitFor(() => calls().filter((c) => c.t === 'state').slice(-1)[0]?.state === 'waiting', 4000));

  // ------------------------------------------------------------------ Runde 2: Almdörfer, fünf Leute
  const emil = await join('Emil');
  const five = [anna, bert, carl, dora, emil];
  clear(...five);
  for (const b of five) b.chat('/karte almdoerfer');
  await sleep(800);
  for (const b of five) b.chat('/bereit');
  check('Zweite Runde: Almdörfer mit 5 Leuten', await waitFor(() => sees(anna, /Auf geht's! Karte: Almdörfer mit Hängebrücke · 5 Leute/), 15000),
    anna.chats.slice(-2).join(' | '));
  s = (await status()).almdoerfer;
  const sides = Object.values(s.players).map((p) => p.side);
  check('Drei gegen zwei, Party wieder zusammen', s.players.Anna.side === s.players.Carl.side &&
    Math.abs(sides.filter((x) => x === 'blau').length - sides.filter((x) => x === 'rot').length) === 1, JSON.stringify(s.players));
  const A2 = s.players.Anna.side === 'blau' ? 0 : 1;
  const B2 = 1 - A2;
  const t2A = s.trees[A2];
  const t2B = s.trees[B2];
  const dir2 = A2 === 0 ? 1 : -1;
  const defenders = five.filter((b) => s.players[b.username].side !== s.players.Anna.side);
  await cmd('maibaumadmin vorspulen los almdoerfer');
  await waitFor(() => anna.game.gameMode === 'survival', 6000);
  const fritz = await join('Fritz');
  fritz.chat('/zuschauen Anna');
  check('Fritz schaut zu (Zuschauermodus über der Karte)', await waitFor(() => fritz.game.gameMode === 'spectator' &&
    Math.abs(fritz.entity.position.x - 2000) < 100, 5000), `${fritz.game.gameMode} ${JSON.stringify(fritz.entity.position)}`);
  check('Status kennt Fritz als Zuschauer', (await status()).almdoerfer.players.Fritz?.state === 'zuschauer');
  check('Hängebrücke hängt über der Schlucht, darunter ist es tief', await passed('execute unless block 2000 98 0 air') &&
    await passed('execute if block 2000 95 6 air') && await passed('execute if block 2000 92 10 air'));

  // Absturz in die Schlucht → zurück ins eigene Dorf.
  const faller = defenders[0];
  clear(anna, faller);
  await cmd(inWorld(`tp ${faller.username} 2000.5 86 10.5`));
  check(`${faller.username} stürzt in die Schlucht: zurück ins eigene Dorf`, await waitFor(() => flat(faller.entity.position, t2B.start) < 4 &&
    faller.entity.position.y > 99, 4000) && await waitFor(() => sees(anna, new RegExp(`${faller.username} ist abgestürzt`)), 2000),
  JSON.stringify(faller.entity.position));

  // Anna legt um, trägt mit Carl auf die Brücke; dort wird Carl berührt – der Baum liegt, obwohl Anna noch trägt.
  for (const d of defenders) await tp(d, t2B.start.x, t2B.start.y, t2B.start.z);
  await tp(anna, t2B.base.x + 0.5 - dir2 * 2, FLOOR + 1, t2B.base.z + 0.5, 0, 0);
  check('Anna legt den Baum der Alm um', await waitFor(async () => (await status()).almdoerfer.trees[B2].state === 'CARRIED', 9000));
  await tp(anna, 2000.5 - dir2 * 4, FLOOR, 0.5, 0, 0);
  await waitFor(() => anna.entity.onGround, 2000);
  await tp(carl, anna.entity.position.x + dir2 * 1.8, anna.entity.position.y, anna.entity.position.z);
  check('Anna und Carl tragen auf der Hängebrücke', await waitFor(async () => (await status()).almdoerfer.trees[B2].carriers === 2, 3000),
    JSON.stringify((await status()).almdoerfer.trees[B2]));
  const hunter = defenders[1];
  clear(anna, carl, hunter);
  await cmd(inWorld(`tp ${hunter.username} ${carl.entity.position.x + dir2 * 0.6} ${carl.entity.position.y} ${carl.entity.position.z}`));
  check(`${hunter.username} berührt Carl: der Baum liegt auf der Brücke (obwohl Anna noch trug)`, await waitFor(async () =>
    (await status()).almdoerfer.trees[B2].state === 'DROPPED', 3000), JSON.stringify((await status()).almdoerfer.trees[B2]));
  await tp(hunter, t2B.start.x, t2B.start.y, t2B.start.z);
  check('Anna hebt den liegenden Baum wieder auf', await waitFor(async () => (await status()).almdoerfer.trees[B2].state === 'CARRIED', 4000) &&
    await waitFor(() => sees(carl, /Anna hat den Maibaum von .* wieder aufgehoben/), 2000), carl.chats.slice(-1)[0]);
  clear(anna, carl);
  await cmd(inWorld(`tp Anna 2000.5 86 -10.5`));
  check('Anna stürzt mit dem Baum in die Schlucht: der Baum steht wieder im Dorf', await waitFor(async () =>
    (await status()).almdoerfer.trees[B2].state === 'HOME', 4000) && await poleStands(t2B) &&
    await waitFor(() => sees(carl, /in die Tiefe gestürzt – er steht wieder im Dorf/), 2000), carl.chats.slice(-2).join(' | '));

  // Zu spät: Emil verliert die Verbindung und kommt nicht rechtzeitig.
  mark = calls().length;
  clear(anna);
  emil.quit();
  check(`Emil weg: Platz bleibt ${COMEBACK} s frei`, await waitFor(() => callsOf('hold-slot', 'Emil', mark).some((c) => c.seconds === COMEBACK), 5000));
  check(`Nach ${COMEBACK} s: Emil ist nicht rechtzeitig zurück`, await waitFor(() => sees(anna, /Emil ist nicht rechtzeitig zurückgekommen/),
    (COMEBACK + 15) * 1000, 500), anna.chats.slice(-2).join(' | '));
  check('Netzwerk: Angebot „Zurück ins Spiel“ zurückgenommen (holdSlot 0)', callsOf('hold-slot', 'Emil', mark).some((c) => c.seconds === 0));
  check('Die Runde läuft weiter', (await status()).almdoerfer.phase === 'PLAY' && (await status()).almdoerfer.players.Emil.state === 'weg');
  const emil2 = await join('Emil');
  check('Emil kommt zu spät: Wirtsgarten und Hinweis', await waitFor(() => inHall(emil2) && sees(emil2, /Leider zu spät/), 6000),
    `${emil2.game.gameMode} ${emil2.chats.slice(-1)[0]}`);

  // Zeit um: nach Punkten.
  clear(anna, bert, carl, dora, fritz);
  const before = (await status()).almdoerfer.punkte;
  await cmd('maibaumadmin vorspulen ende almdoerfer');
  const pointsWinner = before[0] === before[1] ? null : (before[0] > before[1] ? 'blau' : 'rot');
  check('Zeit um: Ergebnis nach Punkten', await waitFor(() => sees(anna, /Die Zeit ist um/), 5000), `${before} ${anna.chats.slice(-2).join(' | ')}`);
  check('Titel passt zu den Punkten', pointsWinner === null ? sawTitle(anna, /Unentschieden/)
    : (pointsWinner === s.players.Anna.side ? sawTitle(anna, /Gewonnen/) : sawTitle(anna, /gewinnt/)), `${before} ${anna.titles.join(' | ')}`);
  const stats2 = calls().filter((c) => c.t === 'stats');
  check('Statistik zählt auch Emil (zu spät, Runde gespielt): 9 Meldungen insgesamt', stats2.length === 9 &&
    stats2.filter((c) => c.name === 'Emil').length === 1, `${stats2.length} Meldungen`);
  check('Alle zurück im Wirtsgarten (auch Fritz)', await waitFor(() => [anna, bert, carl, dora, fritz].every(inHall), 15000),
    [anna, bert, carl, dora, fritz].map((b) => `${b.username}:${b.game.gameMode}`).join(' '));
  check('Almdörfer werden wiederhergestellt', await waitFor(async () => (await status()).info.frei?.includes('almdoerfer'), 30000, 1000));

  // ------------------------------------------------------------------ Lobby-Knopf
  clear(anna);
  anna.setQuickBarSlot(8);
  await sleep(200);
  anna.activateItem();
  check('Bett: zur Lobby – fragt das Netzwerk (im Wirtsgarten ohne Nachfrage)', await waitFor(() =>
    calls().some((c) => c.t === 'ask-lobby' && c.name === 'Anna' && c.asked === false), 4000), JSON.stringify(calls().slice(-2)));

  for (const b of [bert, carl, dora, emil2, fritz]) b.quit();
  await sleep(800);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\n${ok}/${results.length} Prüfungen bestanden`);
  process.exit(ok === results.length ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\nAbgebrochen – ${ok}/${results.length} Prüfungen bestanden`);
  process.exit(1);
});
