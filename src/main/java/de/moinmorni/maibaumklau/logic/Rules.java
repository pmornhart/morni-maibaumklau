package de.moinmorni.maibaumklau.logic;

/**
 * Spielregeln, die man ohne Server prüfen kann: Abstände, Tempo der Träger, wer wen abfangen darf.
 *
 * @param liftSeconds   so lange braucht einer allein, um den Baum umzulegen
 * @param returnSeconds so lange liegt ein abgelegter Baum, bis er wieder als zurück gilt
 * @param speedAlone    Tempo eines Trägers allein (Anteil des normalen Tempos)
 * @param speedPerCarrier so viel schneller wird es mit jedem weiteren Träger
 * @param speedMax      schneller geht es auch mit vielen Trägern nicht
 * @param ownMustStand  den fremden Baum nur heimbringen, wenn der eigene gerade steht
 * @param restSeconds   so lange verschnauft, wer abgefangen wurde (nicht abfangbar, fängt nicht ab, trägt nicht)
 */
public record Rules(double liftSeconds, double returnSeconds, double speedAlone, double speedPerCarrier, double speedMax,
        boolean ownMustStand, double restSeconds) {

    /** So nah am Fuß des Baums wird umgelegt (und hält die Wache). */
    public static final double LIFT_RADIUS = 3.2;
    /** So nah beim vordersten Träger trägt man mit. */
    public static final double CARRY_RADIUS = 4.0;
    /** So nah muss man an einen liegenden Baum, um ihn aufzuheben (und so nah hilft die Wache beim Zurückholen). */
    public static final double PICKUP_RADIUS = 2.6;
    /** Berührt eine Wache einen Träger (so nah), ist er abgefangen. */
    public static final double TOUCH_RADIUS = 1.1;
    /** Ab drei Leuten wird nicht mehr schneller umgelegt. */
    public static final int MAX_LIFTERS = 3;
    /** Ab zwei Wachen wird ein liegender Baum nicht mehr schneller zurückgeholt. */
    public static final int MAX_GUARDS = 2;
    /** Nach dem Ablegen kann der Baum so lange nicht gleich wieder aufgehoben werden (Sekunden). */
    public static final double PICKUP_LOCK = 1.5;

    /** Vorgaben (wie in der mitgelieferten config.yml). */
    public static Rules defaults() {
        return new Rules(8, 20, 0.55, 0.12, 0.9, false, 3);
    }

    /** Tempo der Träger (Anteil des normalen Tempos): allein langsam, mit mehr Leuten schneller, nie schneller als normal. */
    public double carrySpeed(int carriers) {
        if (carriers <= 0) {
            return 1.0;
        }
        return Math.min(Math.min(1.0, speedMax), speedAlone + speedPerCarrier * (carriers - 1));
    }

    /** Wie schnell umgelegt wird: einer 1-fach, zwei 2-fach, ab drei 3-fach. */
    public static double liftRate(int lifters) {
        return Math.min(Math.max(0, lifters), MAX_LIFTERS);
    }

    /** Wie schnell ein liegender Baum zurückgeht: allein 1-fach, mit einer Wache daneben 2-fach, ab zwei 3-fach. */
    public static double returnRate(int guards) {
        return 1 + Math.min(Math.max(0, guards), MAX_GUARDS);
    }

    /**
     * Darf {@code tagger} den anderen abfangen?
     *
     * @param taggerSide     Dorf dessen, der abfängt
     * @param victimSide     Dorf dessen, der abgefangen werden soll
     * @param victimHalf     auf welcher Hälfte der Karte der andere gerade steht
     * @param victimCarries  trägt er gerade einen Baum? Dann geht es überall.
     * @param victimResting  verschnauft er gerade (nach dem Abfangen)?
     * @param taggerResting  verschnauft der, der abfangen will?
     */
    public static boolean mayTag(Side taggerSide, Side victimSide, Side victimHalf, boolean victimCarries,
            boolean victimResting, boolean taggerResting) {
        if (taggerSide == victimSide || victimResting || taggerResting) {
            return false;
        }
        return victimCarries || victimHalf == taggerSide;
    }
}
