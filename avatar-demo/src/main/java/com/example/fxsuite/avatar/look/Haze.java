package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What is drawn lit by the lights, faded into the haze as the land is ({@link Sight#haze}): as it is, lit, so much less
 * as there is haze; the haze itself, so much more. Each point of its mesh a corner of its picture of its own, as far
 * down it as there is haze there: a picture whose diffuse half fades from white to black down it, and whose glow comes
 * up from nothing to the haze's colour ({@link Horizon#HAZY}).
 */
final class Haze {

    /** How many steps of haze, top to bottom of its pictures. */
    static final int LEVELS = 64;
    /** The corner of its pictures where there is none. */
    static final double CLEAR = corner(0);

    private static final Map<PhongMaterial, PhongMaterial> HAZED = new ConcurrentHashMap<>();

    private Haze() {
    }

    /** The corner of its pictures as far down as {@code haze} is. */
    static double corner(final double haze) {
        return (haze * (LEVELS - 1) + 0.5) / LEVELS;
    }

    /** {@code material} fading into the haze as its points' corners say: made once for each. */
    static PhongMaterial of(final PhongMaterial material) {
        return HAZED.computeIfAbsent(material, m -> {
            final PhongMaterial hazed = new PhongMaterial(m.getDiffuseColor());
            hazed.setSpecularColor(m.getSpecularColor());
            hazed.setSpecularPower(m.getSpecularPower());
            hazed.setDiffuseMap(Pictures.FADE);
            hazed.setSelfIlluminationMap(Pictures.GLOW);
            return hazed;
        });
    }

    /**
     * Each of {@code mesh}'s points' corners as far into the haze as {@code view} has its point: as each point is far
     * off across the ground. Whether any of it is hazed at all.
     */
    static boolean haze(final TriangleMesh mesh, final View view) {
        final Vec3 eye = view.eye();
        final float[] points = mesh.getPoints().toArray(null);
        final float[] coords = new float[2 * (points.length / 3)];
        boolean any = false;
        for (int v = 0; v < points.length / 3; v++) {
            final double haze = view.haze(Math.hypot(points[3 * v] - eye.x(), points[3 * v + 2] - eye.z()));
            coords[2 * v] = (float) CLEAR;
            coords[2 * v + 1] = (float) corner(haze);
            any |= haze > 0;
        }
        mesh.getTexCoords().setAll(coords);
        return any;
    }

    /**
     * Whether what lies in {@code box} (min x, min z, max x, max z), hazed or not as {@code hazed} says, last hazed
     * from {@code from} seeing {@code reach}, is to be hazed again as {@code view} sees: as the land's would be, once
     * the camera has moved far enough for it to show.
     */
    static boolean stale(final double[] box, final boolean hazed, final Vec3 from, final double reach, final View view) {
        final Vec3 eye = view.eye();
        final double far = Math.hypot(Math.max(Math.abs(eye.x() - box[0]), Math.abs(eye.x() - box[2])),
                Math.max(Math.abs(eye.z() - box[1]), Math.abs(eye.z() - box[3])));
        if (far <= view.clear() && !hazed) {
            return false;
        }
        if (far <= view.clear() || from == null) {
            return true;
        }
        final double moved = Math.hypot(eye.x() - from.x(), eye.z() - from.z());
        return moved > Math.max(TerrainModel.MOVED, TerrainModel.SHARE * near(box, eye))
                || Math.abs(view.reach() - reach) > TerrainModel.SHARE * view.reach();
    }

    /** How near {@code eye} {@code box} is across the ground, at its nearest. */
    static double near(final double[] box, final Vec3 eye) {
        return Math.hypot(Math.max(0, Math.max(box[0] - eye.x(), eye.x() - box[2])),
                Math.max(0, Math.max(box[1] - eye.z(), eye.z() - box[3])));
    }

    /** Its pictures, made when first wanted. */
    private static final class Pictures {
        static final WritableImage FADE = picture(false);
        static final WritableImage GLOW = picture(true);

        private static WritableImage picture(final boolean glow) {
            final WritableImage picture = new WritableImage(1, LEVELS);
            final PixelWriter pixels = picture.getPixelWriter();
            for (int y = 0; y < LEVELS; y++) {
                final double haze = y / (LEVELS - 1.0);
                pixels.setColor(0, y, glow ? Color.BLACK.interpolate(Horizon.HAZY, haze)
                        : Color.gray(1 - haze));
            }
            return picture;
        }
    }
}
