package de.moinmorni.maibaumklau.logic;

/**
 * Ein Maibaum in einer Runde – nur der Zustand, ohne Minecraft. Er steht im eigenen Dorf ({@link State#HOME}), wird vom
 * anderen Dorf umgelegt (dauert, schneller mit mehr Leuten, die Wache hält es auf), dann getragen
 * ({@link State#CARRIED}). Wird ein Träger abgefangen, liegt er da ({@link State#DROPPED}) und gilt nach einer Weile
 * wieder als zurück (Wache daneben: schneller) – außer das andere Dorf hebt ihn vorher wieder auf.
 */
public final class Pole {

    public enum State {
        HOME, CARRIED, DROPPED
    }

    /** Was am Fuß des Baums gerade passiert. */
    public enum Lift {
        /** Niemand vom anderen Dorf ist da. */
        IDLE,
        /** Es wird umgelegt. */
        WORKING,
        /** Die Wache steht dabei – nichts geht voran. */
        GUARDED
    }

    /** Ohne Leute am Baum geht der Fortschritt so viel langsamer wieder zurück, als er entsteht. */
    private static final double DECAY = 0.5;

    private final Side owner;
    private State state = State.HOME;
    private double lift;
    private Lift liftState = Lift.IDLE;
    private double dropLeft;
    private double lock;
    private double best;

    public Pole(Side owner) {
        this.owner = owner;
    }

    /** Wem der Baum gehört. */
    public Side owner() {
        return owner;
    }

    public State state() {
        return state;
    }

    /** Fortschritt beim Umlegen (0 … 1). */
    public double lift() {
        return lift;
    }

    public Lift liftState() {
        return liftState;
    }

    /** Sekunden, bis ein liegender Baum wieder als zurück gilt. */
    public double dropLeft() {
        return dropLeft;
    }

    /** Wie nah ihn das andere Dorf schon an sein eigenes Dorf gebracht hat (0 … 1, bester Stand der Runde). */
    public double best() {
        return best;
    }

    /**
     * Am Fuß des Baums (nur solange er steht): {@code lifters} Leute vom anderen Dorf legen um, {@code guards} Wachen
     * stehen dabei. Jede Wache hält einen Dieb auf: stehen gleich viele (oder mehr) Wachen dabei, geht nichts voran;
     * sind mehr Diebe da, arbeiten nur die übrigen.
     *
     * @return {@code true}, wenn der Baum jetzt fällt (er gilt dann als getragen)
     */
    public boolean liftTick(int lifters, int guards, double seconds, Rules rules) {
        if (state != State.HOME) {
            return false;
        }
        int working = lifters - Math.max(0, guards);
        if (lifters > 0 && working <= 0) {
            liftState = Lift.GUARDED;
            return false;
        }
        if (lifters > 0) {
            liftState = Lift.WORKING;
            lift = Math.min(1, lift + seconds * Rules.liftRate(working) / Math.max(0.1, rules.liftSeconds()));
            if (lift >= 1) {
                state = State.CARRIED;
                lift = 0;
                liftState = Lift.IDLE;
                return true;
            }
            return false;
        }
        liftState = Lift.IDLE;
        lift = Math.max(0, lift - seconds * DECAY / Math.max(0.1, rules.liftSeconds()));
        return false;
    }

    /** Ein Träger wurde abgefangen (oder ist gegangen): der Baum liegt jetzt da. */
    public void drop(Rules rules) {
        state = State.DROPPED;
        dropLeft = rules.returnSeconds();
        lock = Rules.PICKUP_LOCK;
    }

    /**
     * Ein liegender Baum: die Zeit läuft (mit {@code guards} Wachen daneben schneller).
     *
     * @return {@code true}, wenn er jetzt wieder als zurück gilt (steht wieder im eigenen Dorf)
     */
    public boolean dropTick(int guards, double seconds) {
        if (state != State.DROPPED) {
            return false;
        }
        lock = Math.max(0, lock - seconds);
        dropLeft -= seconds * Rules.returnRate(guards);
        if (dropLeft <= 0) {
            home();
            return true;
        }
        return false;
    }

    /** Darf das andere Dorf den liegenden Baum gerade aufheben (nicht gleich nach dem Ablegen)? */
    public boolean canPickUp() {
        return state == State.DROPPED && lock <= 0;
    }

    /** Wieder aufgehoben. */
    public void pickUp() {
        if (state == State.DROPPED) {
            state = State.CARRIED;
        }
    }

    /** Zurück im eigenen Dorf (zurückgeholt, ins Wasser gefallen oder neue Runde). */
    public void home() {
        state = State.HOME;
        lift = 0;
        dropLeft = 0;
        lock = 0;
        liftState = Lift.IDLE;
    }

    /**
     * Neuer Stand auf dem Weg ins andere Dorf. Liefert, um wie viele Zehntel der bisher beste Stand übertroffen wurde
     * (dafür gibt es Punkte) – 0, wenn nicht.
     */
    public int advance(double progress) {
        double p = Math.max(0, Math.min(1, progress));
        int steps = Score.steps(best, p);
        best = Math.max(best, p);
        return steps;
    }

    /**
     * Wie weit ist der Baum auf dem Weg von seinem Dorf ({@code hx, hz}) ins andere ({@code tx, tz})? 0 = noch daheim,
     * 1 = angekommen. Geht nach der Entfernung zum Ziel (Umwege zählen nicht).
     */
    public static double progress(double hx, double hz, double tx, double tz, double x, double z) {
        double whole = Math.hypot(tx - hx, tz - hz);
        if (whole <= 0) {
            return 1;
        }
        return Math.max(0, Math.min(1, 1 - Math.hypot(tx - x, tz - z) / whole));
    }
}
