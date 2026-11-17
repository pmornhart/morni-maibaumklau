package de.moinmorni.maibaumklau.world;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.Locale;

/**
 * Die Karten. Jede liegt an einer eigenen Stelle der Welt (Wirtsgarten bei 0|0), mit eigenem Aufbau
 * ({@link MapLayout}) und Bauplan; auf jeder Karte läuft höchstens eine Runde gleichzeitig.
 */
public enum MapType {
    BACHDOERFER("bachdoerfer", 1000, "water_bucket"),
    ALMDOERFER("almdoerfer", 2000, "spruce_sapling");

    /** Höhe der obersten Bodenschicht in der Welt (man steht eins darüber). */
    public static final int FLOOR_Y = 100;
    /** So tief und so hoch reicht die Spielfläche (Wiederherstellung nach jeder Runde). */
    private static final int PLAY_BOTTOM = -32;
    private static final int PLAY_TOP = 26;
    private static final int MARGIN = 6;

    private final String id;
    private final int originX;
    private final String icon;
    private MapLayout layout;
    private Plan plan;
    private Plan playPlan;

    MapType(String id, int originX, String icon) {
        this.id = id;
        this.originX = originX;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return 0;
    }

    /** Gegenstand fürs Menü. */
    public String icon() {
        return icon;
    }

    public synchronized MapLayout layout() {
        if (layout == null) {
            layout = switch (this) {
                case BACHDOERFER -> BachPlan.layout();
                case ALMDOERFER -> AlmPlan.layout();
            };
        }
        return layout;
    }

    /** Ganzer Bauplan mit Kulisse (Berge, Wolken). */
    public synchronized Plan plan() {
        if (plan == null) {
            plan = switch (this) {
                case BACHDOERFER -> BachPlan.build();
                case ALMDOERFER -> AlmPlan.build();
            };
        }
        return plan;
    }

    /** Nur die Spielfläche (wird nach jeder Runde wiederhergestellt). */
    public synchronized Plan playPlan() {
        if (playPlan == null) {
            int[] b = playBox();
            playPlan = plan().crop(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        return playPlan;
    }

    /** Was beim Umlegen des Baums dieses Dorfs verschwindet (und beim Zurückholen wieder steht). */
    public Plan polePlan(Side side) {
        MapLayout l = layout();
        int[] b = Maypole.removable(l.poleX(side), l.poleZ(side));
        return playPlan().crop(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    /** Ganzen Bauplan vergessen (nach dem Bauen; die Spielfläche bleibt im Speicher). */
    public synchronized void forgetFullPlan() {
        playPlan();
        plan = null;
    }

    /** Box der Spielfläche (relativ): minX, minY, minZ, maxX, maxY, maxZ. */
    public int[] playBox() {
        MapLayout l = layout();
        return new int[] {-l.halfX() - MARGIN, PLAY_BOTTOM, -l.halfZ() - MARGIN, l.halfX() + MARGIN, PLAY_TOP,
            l.halfZ() + MARGIN};
    }

    public static MapType byId(String s) {
        if (s == null) {
            return null;
        }
        String k = s.toLowerCase(Locale.ROOT);
        for (MapType m : values()) {
            if (m.id.equals(k)) {
                return m;
            }
        }
        return switch (k) {
            case "bach", "brook", "doerfer", "dörfer", "bachdörfer" -> BACHDOERFER;
            case "alm", "alpine", "bruecke", "brücke", "bridge", "almdörfer" -> ALMDOERFER;
            default -> null;
        };
    }
}
