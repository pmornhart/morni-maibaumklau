package de.moinmorni.maibaumklau.world;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.List;

/**
 * Aufbau einer Karte ohne Minecraft: wo die beiden Maibäume stehen, wo man startet, wo die Karte endet, wo es in die
 * Tiefe geht und wo Heuwagen einen in die Luft werfen. Alles relativ zum Mittelpunkt der Karte; y = 0 ist die oberste
 * Bodenschicht (man steht auf y = 1).
 *
 * @param poles     Fuß der Maibäume {x, z} (Blau, Rot)
 * @param spawns    Startplätze {x, z, Blickrichtung} (Blau, Rot) – vor dem Wirtshaus jedes Dorfs
 * @param square    Radius des Dorfplatzes: wer den fremden Baum bis hierhin bringt, hat gewonnen
 * @param voidY     wer tiefer fällt, ist abgestürzt (zurück ins eigene Dorf)
 * @param halfX     die Karte reicht bis {@code |x| = halfX} …
 * @param halfZ     … und {@code |z| = halfZ}
 * @param pads      Heuwagen
 * @param crossings Übergänge über Bach bzw. Schlucht (Mitte {x, z}) – nur zur Prüfung und für Bilder
 */
public record MapLayout(int[][] poles, double[][] spawns, double square, int voidY, int halfX, int halfZ, List<Pad> pads,
        List<int[]> crossings) {

    /**
     * Heuwagen: wer auf den Heuballen bei (x, 0, z) tritt, fliegt mit dieser Geschwindigkeit los.
     */
    public record Pad(int x, int z, double vx, double vy, double vz) {
    }

    public int poleX(Side s) {
        return poles[s.index()][0];
    }

    public int poleZ(Side s) {
        return poles[s.index()][1];
    }

    public double spawnX(Side s) {
        return spawns[s.index()][0];
    }

    public double spawnZ(Side s) {
        return spawns[s.index()][1];
    }

    public float spawnYaw(Side s) {
        return (float) spawns[s.index()][2];
    }

    /** Auf wessen Hälfte liegt (x|z)? Die Hälfte des näheren Maibaums. */
    public Side half(double x, double z) {
        double blue = Math.hypot(x - poles[0][0], z - poles[0][1]);
        double red = Math.hypot(x - poles[1][0], z - poles[1][1]);
        return blue <= red ? Side.BLUE : Side.RED;
    }

    /** Waagrechter Abstand zum Fuß eines Maibaums. */
    public double toPole(Side s, double x, double z) {
        return Math.hypot(x - (poleX(s) + 0.5), z - (poleZ(s) + 0.5));
    }

    /** Liegt (x|z) auf dem Dorfplatz dieses Dorfs? */
    public boolean onSquare(Side s, double x, double z) {
        return toPole(s, x, z) <= square;
    }
}
