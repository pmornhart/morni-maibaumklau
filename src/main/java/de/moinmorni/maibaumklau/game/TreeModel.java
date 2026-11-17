package de.moinmorni.maibaumklau.game;

import de.moinmorni.maibaumklau.logic.Side;
import de.moinmorni.maibaumklau.world.Maypole;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Der umgelegte Maibaum als Modell aus Block-Anzeigen: Streifen wie am echten Baum, Kranz und grüne Krone, in der Farbe
 * des Dorfs leuchtend (auch durch Wände sichtbar). Liegt er, liegt er auf dem Boden; wird er getragen, schwebt er
 * waagrecht über den Köpfen der Träger. Bedrock zeigt Block-Anzeigen nicht – dort helfen Partikel und das Leuchten der
 * Träger.
 */
final class TreeModel {

    /** Markierung aller Teile (zum Aufräumen nach einem Absturz des Servers). */
    static final String TAG = "maibaumklau_baum";
    /** Dicke des Stamms. */
    private static final float THICK = 0.5f;
    /** Länge eines Streifens. */
    private static final float SEGMENT = 2f;
    /** So hoch über den Füßen des vordersten Trägers schwebt der Baum. */
    static final double CARRY_HEIGHT = 2.2;
    /** So weit hinter dem vordersten Träger liegt die Mitte des Baums (die anderen tragen hinten mit). */
    static final double CARRY_BACK = 2.0;

    private record Part(BlockDisplay display, double along) {
    }

    private final List<Part> parts = new ArrayList<>();

    TreeModel(World world, Location at, Side side) {
        int segments = Maypole.LENGTH / (int) SEGMENT;
        double half = Maypole.LENGTH / 2.0;
        for (int i = 0; i < segments; i++) {
            BlockData d = Bukkit.createBlockData(stemBlock(side, Maypole.FIRST + i * (int) SEGMENT));
            double along = -half + SEGMENT * i + SEGMENT / 2;
            parts.add(new Part(spawn(world, at, d, new Vector3f(THICK, THICK, SEGMENT), side), along));
        }
        double wreath = (Maypole.WREATH - Maypole.FIRST) - half + 0.5;
        parts.add(new Part(spawn(world, at, Bukkit.createBlockData("minecraft:flowering_azalea_leaves[persistent=true]"),
                new Vector3f(1.3f, 1.3f, 0.45f), side), wreath));
        parts.add(new Part(spawn(world, at, Bukkit.createBlockData("minecraft:spruce_leaves[persistent=true]"),
                new Vector3f(1.5f, 1.5f, 2.2f), side), half + 1.1));
    }

    private static String stemBlock(Side side, int y) {
        return "minecraft:" + stem(side, y);
    }

    /** Wie {@code Maypole.stem}: unten blank, darüber Streifen. */
    private static String stem(Side side, int y) {
        if (y < Maypole.FIRST + 2) {
            return "stripped_spruce_log[axis=z]";
        }
        return ((y - Maypole.FIRST) / 2) % 2 == 0 ? side.stripe() + "_concrete" : "white_concrete";
    }

    private static BlockDisplay spawn(World world, Location at, BlockData data, Vector3f size, Side side) {
        return world.spawn(at, BlockDisplay.class, d -> {
            d.setBlock(data);
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
            d.setGlowing(true);
            d.setGlowColorOverride(Color.fromRGB(side.rgb()));
            d.setViewRange(4f);
            d.setTeleportDuration(2);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(new Transformation(new Vector3f(-size.x / 2, -size.y / 2, -size.z / 2), new AxisAngle4f(),
                    size, new AxisAngle4f()));
        });
    }

    /**
     * Baum hinlegen bzw. mitführen: Mitte bei {@code center}, Stamm in Blickrichtung {@code yaw} (Krone vorn).
     */
    void place(Location center, float yaw) {
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        for (Part p : parts) {
            if (!p.display().isValid()) {
                continue;
            }
            Location l = center.clone().add(dx * p.along(), 0, dz * p.along());
            l.setYaw(yaw);
            l.setPitch(0);
            p.display().teleport(l);
        }
    }

    boolean valid() {
        return parts.stream().allMatch(p -> p.display().isValid());
    }

    void remove() {
        parts.forEach(p -> p.display().remove());
        parts.clear();
    }
}
