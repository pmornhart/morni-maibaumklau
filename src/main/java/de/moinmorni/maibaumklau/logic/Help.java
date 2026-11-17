package de.moinmorni.maibaumklau.logic;

/**
 * Hilfen in der Schnellleiste (alle ohne Waffe, nichts zu kaufen). Wer den Baum trägt, kann sie nicht benutzen – der Baum
 * ist zu schwer.
 */
public enum Help {
    /** Abfang-Stecken: wer damit (oder mit der Hand) jemanden trifft, fängt ihn ab – ohne Schaden. */
    STICK("stecken", "stick", 0),
    /** Sprint-Brotzeit: kurz schneller. */
    SNACK("brotzeit", "bread", 1),
    /** Heuballen-Sprung (ein Heubündel in der Hand): ein großer Satz nach vorn und oben. */
    HAY("heusprung", "wheat", 2),
    /** Leitern: an Wände setzen und hochklettern (verschwinden nach einer Weile). */
    LADDER("leiter", "ladder", 3),
    /** Maibaum-Kompass: zeigt zum fremden Baum. */
    COMPASS("kompass", "compass", 4);

    private final String id;
    private final String material;
    private final int slot;

    Help(String id, String material, int slot) {
        this.id = id;
        this.material = material;
        this.slot = slot;
    }

    public String id() {
        return id;
    }

    /** Minecraft-Gegenstand (Name). */
    public String material() {
        return material;
    }

    /** Platz in der Schnellleiste. */
    public int slot() {
        return slot;
    }

    /** Darf man das auch mit dem Baum auf der Schulter? */
    public boolean whileCarrying() {
        return this == STICK || this == COMPASS;
    }

    public static Help byId(String id) {
        for (Help h : values()) {
            if (h.id.equals(id)) {
                return h;
            }
        }
        return null;
    }
}
