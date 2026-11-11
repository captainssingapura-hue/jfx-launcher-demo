package com.example.fxsuite.avatar;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * The jet-pack, worn on the avatar's back: a harness with straps over the chest, twin jet
 * engines side by side, and a pair of small wings. The wings lie folded down along the engines
 * until it flies, then swing out level, a little swept back and tipped up. Each engine has its
 * own flame, as long as the thrust. Hidden entirely when not worn.
 *
 * <p>In the figure's frame: it faces +z, so the pack hangs behind the torso, toward -z, with up
 * as -y.
 */
final class JetPack {

    /** Across the back, the engines' centres, and how far behind the body they sit. */
    private static final double ENGINE_X = 17;
    private static final double ENGINE_Z = -36;
    private static final double ENGINE_TOP = 148;
    private static final double ENGINE_LENGTH = 58;
    private static final double ENGINE_RADIUS = 11;
    /** Where the flames start: just under the nozzles. */
    private static final double EXHAUST = ENGINE_TOP - ENGINE_LENGTH - 12;
    private static final double MAX_FLAME = 80;
    /** Where each wing is hinged, on its engine's outer side. */
    private static final double HINGE_Y = 132;
    private static final double WING_SPAN = 62;
    /** Folded, the wings hang this far down from level; open, they tip up this much. */
    private static final double FOLDED = 88;
    private static final double DIHEDRAL = 8;
    private static final double SWEEP = 16;
    /** How quickly the wings open or fold: about two thirds of a second. */
    private static final double UNFOLD = 6;

    private final Group root = new Group();
    private final Rotate leftFold = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate rightFold = new Rotate(0, Rotate.Z_AXIS);
    private final Group flames = new Group();
    private final Scale flameLength = new Scale(1, 1, 1);
    private double open;
    private double lastTime = Double.NaN;

    JetPack() {
        final PhongMaterial casing = BoxRig.matte("#5b6470");
        final PhongMaterial dark = BoxRig.matte("#2a2e35");
        final PhongMaterial strap = BoxRig.matte("#3b3f46");
        final PhongMaterial wing = BoxRig.matte("#d9822b");
        final PhongMaterial flame = BoxRig.glow("#ffb347");

        root.getChildren().addAll(
                // the harness: a plate across the back, straps down the chest, a belt
                box(46, 58, 6, 0, -122, -21, strap),
                box(8, 52, 2, -15, -124, 18, strap),
                box(8, 52, 2, 15, -124, 18, strap),
                box(66, 7, 36, 0, -97, 0, strap),
                // a cross-member holding the two engines together
                box(40, 10, 12, 0, -130, ENGINE_Z + 5.5, dark));
        for (final double side : new double[] {-ENGINE_X, ENGINE_X}) {
            root.getChildren().addAll(
                    cylinder(ENGINE_RADIUS, ENGINE_LENGTH, side, -(ENGINE_TOP - ENGINE_LENGTH / 2), ENGINE_Z, casing),
                    cylinder(ENGINE_RADIUS + 1.5, 6, side, -(ENGINE_TOP - 3), ENGINE_Z, dark),      // intake ring
                    cylinder(ENGINE_RADIUS - 3, 12, side, -(ENGINE_TOP - ENGINE_LENGTH - 6), ENGINE_Z, dark));
            // each flame is a unit long until scaled, hanging down from its nozzle
            flames.getChildren().add(cylinder(6.5, 1, side, 0.5, 0, flame));
        }
        root.getChildren().addAll(
                wing(-1, leftFold, wing),
                wing(1, rightFold, wing),
                flames);
        flames.getTransforms().addAll(new Translate(0, -EXHAUST, ENGINE_Z), flameLength);
        flames.setVisible(false);
        root.setVisible(false);
    }

    Node node() {
        return root;
    }

    /**
     * Shows or hides the pack; opens the wings while flying and folds them otherwise, easing
     * between; and lights the engines as hard as the pose's thrust.
     */
    void show(final boolean worn, final Body body, final Pose pose) {
        root.setVisible(worn);
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, body.time() - lastTime);
        lastTime = body.time();
        open = Body.ease(open, worn && body.flying() ? 1 : 0, UNFOLD, seconds);
        // folded, the left wing's tip hangs down (+y), a negative turn about z for a tip at -x;
        // open, it tips up a little, a positive one. The right wing mirrors it.
        final double angle = -FOLDED + (FOLDED + DIHEDRAL) * open;
        leftFold.setAngle(angle);
        rightFold.setAngle(-angle);

        flames.setVisible(worn && pose.thrust() > 0);
        // a little flicker, from the clock, so the flames look alive
        final double flicker = 1 + 0.12 * Math.sin(body.time() * 31) + 0.06 * Math.sin(body.time() * 53);
        flameLength.setY(Math.max(1, MAX_FLAME * pose.thrust() * flicker));
    }

    /** How far the wings are open, 0 folded to 1 level. */
    double open() {
        return open;
    }

    /**
     * One wing, hinged on its engine's outer side: on the left for {@code side} -1, the right
     * for 1. Swept back about the vertical, then folded about the body's length.
     */
    private static Group wing(final int side, final Rotate fold, final PhongMaterial material) {
        // a tip at -x sweeps back (-z) with a negative turn about y; at +x, a positive one
        final Group wing = new Group(
                box(WING_SPAN, 4, 26, side * WING_SPAN / 2, 0, 0, material),
                box(10, 5, 28, side * (WING_SPAN - 5), 0, 0, BoxRig.matte("#2a2e35")));     // wingtip
        wing.getTransforms().addAll(
                new Translate(side * (ENGINE_X + ENGINE_RADIUS), -HINGE_Y, ENGINE_Z),
                new Rotate(side * SWEEP, Rotate.Y_AXIS),
                fold);
        return wing;
    }

    private static Box box(final double w, final double h, final double d,
                           final double x, final double y, final double z, final PhongMaterial material) {
        return placed(new Box(w, h, d), x, y, z, material);
    }

    private static Cylinder cylinder(final double radius, final double height,
                                     final double x, final double y, final double z, final PhongMaterial material) {
        return placed(new Cylinder(radius, height, 20), x, y, z, material);
    }

    private static <S extends Shape3D> S placed(final S shape, final double x, final double y, final double z,
                                                final PhongMaterial material) {
        shape.setTranslateX(x);
        shape.setTranslateY(y);
        shape.setTranslateZ(z);
        shape.setMaterial(material);
        return shape;
    }
}
