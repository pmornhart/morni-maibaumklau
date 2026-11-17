package de.moinmorni.maibaumklau.logic;

/**
 * Wie eine Runde ausging.
 *
 * @param winner Gewinner-Dorf, {@code null} bei Gleichstand
 * @param how    Baum heimgebracht, nach Punkten, unentschieden oder kampflos (das andere Dorf ist leer)
 */
public record Outcome(Side winner, How how) {

    public enum How {
        CAPTURE, POINTS, DRAW, WALKOVER
    }

    public static Outcome capture(Side winner) {
        return new Outcome(winner, How.CAPTURE);
    }

    /** Nach Ablauf der Zeit: mehr Punkte gewinnen, sonst unentschieden. */
    public static Outcome byPoints(int blue, int red) {
        int w = Score.byPoints(blue, red);
        return w < 0 ? new Outcome(null, How.DRAW) : new Outcome(Side.of(w), How.POINTS);
    }

    /** Ein Dorf ist leer (alle gegangen): das andere gewinnt kampflos. */
    public static Outcome walkover(Side winner) {
        return new Outcome(winner, How.WALKOVER);
    }

    /** Muss ein Dorf seinen Baum auslösen (er wurde geklaut)? */
    public boolean ransom() {
        return how == How.CAPTURE;
    }

    public boolean draw() {
        return winner == null;
    }

    /** Text-Schlüssel-Teil („capture“, „points“, „draw“, „walkover“). */
    public String key() {
        return switch (how) {
            case CAPTURE -> "capture";
            case POINTS -> "points";
            case DRAW -> "draw";
            case WALKOVER -> "walkover";
        };
    }
}
