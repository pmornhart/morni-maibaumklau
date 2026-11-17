package de.moinmorni.maibaumklau.game;

import de.moinmorni.maibaumklau.logic.Side;
import java.util.UUID;

/**
 * Eine Person in einer Runde: ihr Dorf, ob sie gerade da ist, ob sie verschnauft, und was sie geschafft hat (für Punkte
 * und Statistik).
 */
public final class Villager {

    private final UUID uuid;
    private final String name;
    private final Side side;
    private boolean online = true;
    private boolean gone;
    private boolean late;
    private long restUntil;
    private int tags;
    private int saves;
    private int lifts;
    private int captures;
    private int caught;
    private double carried;
    private int points;
    private int index;

    Villager(UUID uuid, String name, Side side, int index) {
        this.uuid = uuid;
        this.name = name;
        this.side = side;
        this.index = index;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Side side() {
        return side;
    }

    /** Nummer im Dorf (Startplatz). */
    int index() {
        return index;
    }

    /** Gerade verbunden? */
    public boolean online() {
        return online;
    }

    void online(boolean o) {
        online = o;
    }

    /** Nicht mehr dabei (selbst gegangen oder nicht rechtzeitig zurückgekommen). */
    public boolean gone() {
        return gone;
    }

    void gone(boolean g) {
        gone = g;
    }

    /** Nicht rechtzeitig zurückgekommen (bekommt beim Wiederkommen einen Hinweis). */
    public boolean late() {
        return late;
    }

    void late(boolean l) {
        late = l;
    }

    /** Spielt gerade mit (da und nicht gegangen)? */
    public boolean active() {
        return online && !gone;
    }

    /** Verschnauft bis zu diesem Spieltick (nach dem Abfangen). */
    public boolean resting(long now) {
        return now < restUntil;
    }

    void rest(long until) {
        restUntil = until;
    }

    /** Restliche Ticks des Verschnaufens. */
    long restLeft(long now) {
        return Math.max(0, restUntil - now);
    }

    public int tags() {
        return tags;
    }

    public int saves() {
        return saves;
    }

    public int lifts() {
        return lifts;
    }

    public int captures() {
        return captures;
    }

    /** Wie oft diese Person selbst abgefangen wurde. */
    public int caught() {
        return caught;
    }

    /** Blöcke mit dem Baum auf der Schulter. */
    public int carried() {
        return (int) Math.floor(carried);
    }

    public int points() {
        return points;
    }

    void tagged(boolean save, int points) {
        if (save) {
            saves++;
        } else {
            tags++;
        }
        this.points += points;
    }

    void lifted(int points) {
        lifts++;
        this.points += points;
    }

    void captured(int points) {
        captures++;
        this.points += points;
    }

    void wasCaught() {
        caught++;
    }

    void carriedFor(double blocks) {
        carried += Math.max(0, blocks);
    }

    void points(int add) {
        points += add;
    }
}
