package de.moinmorni.maibaumklau.logic;

/**
 * Punkte. Jedes Dorf hat die Summe der Punkte seiner Leute (auch derer, die schon gegangen sind). Gewonnen hat, wer den
 * fremden Baum heimbringt – sonst nach Ablauf der Zeit das Dorf mit mehr Punkten.
 */
public final class Score {

    /** Jemanden auf der eigenen Hälfte abfangen. */
    public static final int TAG = 2;
    /** Einen Träger abfangen (der Baum fällt hin). */
    public static final int SAVE = 5;
    /** Den fremden Baum umlegen – zusammen, wird unter allen geteilt, die dabei waren. */
    public static final int LIFT = 10;
    /** Je Zehntel des Wegs, das der Baum weiter kommt als bisher in der Runde: jeder Träger … */
    public static final int STEP_CARRIER = 1;
    /** … und der vorderste Träger noch einmal so viel. */
    public static final int STEP_LEADER = 1;
    /** Den Baum heimbringen: jeder Träger. */
    public static final int CAPTURE = 15;
    /** Zehntel: ein Abschnitt des Wegs. */
    public static final double STEP = 0.1;

    private Score() {
    }

    /**
     * Punkte fürs Umlegen, geteilt unter {@code lifters} Leuten (der Rest geht an die Ersten).
     *
     * @return Punkte je Person in der Reihenfolge der Leute
     */
    public static int[] liftShares(int lifters) {
        if (lifters <= 0) {
            return new int[0];
        }
        int[] out = new int[lifters];
        for (int i = 0; i < lifters; i++) {
            out[i] = LIFT / lifters + (i < LIFT % lifters ? 1 : 0);
        }
        return out;
    }

    /** Wie viele neue Zehntel zwischen dem alten und dem neuen besten Stand liegen. */
    public static int steps(double oldBest, double newBest) {
        if (newBest <= oldBest) {
            return 0;
        }
        return (int) Math.floor(newBest / STEP + 1e-9) - (int) Math.floor(oldBest / STEP + 1e-9);
    }

    /** Gewinner nach Punkten: 0 (Blau), 1 (Rot) oder -1 (unentschieden). */
    public static int byPoints(int blue, int red) {
        return blue > red ? 0 : red > blue ? 1 : -1;
    }
}
