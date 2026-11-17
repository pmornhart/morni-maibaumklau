package de.moinmorni.maibaumklau.logic;

/**
 * Die beiden Dörfer einer Runde. Jedes hat seine Farbe: Blau (weiß-blauer Maibaum) und Rot (rot-weißer Maibaum). Wie
 * die Dörfer heißen, hängt von der Karte ab (Oberbach und Unterbach, Hinteralm und Vorderalm).
 */
public enum Side {
    BLUE("blau", 0x3C8DE0, "light_blue", "blue"),
    RED("rot", 0xD8413A, "red", "red");

    private final String id;
    private final int rgb;
    private final String stripe;
    private final String dye;

    Side(String id, int rgb, String stripe, String dye) {
        this.id = id;
        this.rgb = rgb;
        this.stripe = stripe;
        this.dye = dye;
    }

    public String id() {
        return id;
    }

    /** Farbe als RGB (Namen, Rüstung, Leuchten). */
    public int rgb() {
        return rgb;
    }

    /** Farbe der Streifen am Maibaum (Minecraft-Farbname, z. B. für {@code _concrete}). */
    public String stripe() {
        return stripe;
    }

    /** Farbstoff für Wolle, Banner und Glas. */
    public String dye() {
        return dye;
    }

    /** Nummer (0 oder 1). */
    public int index() {
        return ordinal();
    }

    public Side other() {
        return this == BLUE ? RED : BLUE;
    }

    public static Side of(int index) {
        return index == 0 ? BLUE : RED;
    }
}
