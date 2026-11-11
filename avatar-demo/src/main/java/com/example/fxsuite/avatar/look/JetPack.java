package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Wings;

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
 * engines side by side, and a pair of swing wings. Each engine has its own flame, as long as
 * the thrust. Hidden entirely when not worn.
 *
 * <p>Avatar and pack together are drawn as a fighter jet standing on its tail: the nose at the
 * head, the engines pointing down, held up by their thrust rather than by its wings. So the wings
 * lie in the plane of the back, leading edges up, and swing in that plane about pivots in fixed
 * gloves beside the engines, as a variable-sweep fighter's do. Like that fighter's, their sweep
 * follows the speed: tucked down along the engines on the ground, spread wide to hover and fly
 * slowly, and swept back further the faster it flies. The wings' sizes, pivots and sweep are
 * the simulation's ({@link Wings}), so what is drawn is exactly what collides.
 *
 * <p>In the figure's frame: it faces +z, so the pack hangs behind the torso, toward -z, with up
 * as -y.
 */
final class JetPack {

    /** Across the back, the engines' centres, and how far behind the body they sit. */
    private static final double ENGINE_X = 17;
    private static final double ENGINE_Z = -Wings.PIVOT_BEHIND;
    private static final double ENGINE_TOP = 148;
    private static final double ENGINE_LENGTH = 58;
    private static final double ENGINE_RADIUS = 11;
    /** Where the flames start: just under the nozzles. */
    private static final double EXHAUST = ENGINE_TOP - ENGINE_LENGTH - 12;
    private static final double MAX_FLAME = 80;
    /** Where each wing is hinged, on its engine's outer side. */
    private static final double HINGE_Y = Wings.PIVOT_HEIGHT;
    /** How far out from the engine's side the pivot sits, in its glove. */
    private static final double GLOVE = Wings.PIVOT_OUT - ENGINE_X - ENGINE_RADIUS;
    /** The wing: a broad inner panel, a narrower outer one, their leading edges in line. */
    private static final double INNER_SPAN = 38;
    private static final double OUTER_SPAN = Wings.SPAN - INNER_SPAN + 2;
    private static final double ROOT_CHORD = Wings.ROOT_CHORD;
    private static final double TIP_CHORD = 18;

    private final Group root = new Group();
    private final Rotate leftSweep = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate rightSweep = new Rotate(0, Rotate.Z_AXIS);
    private final Group flames = new Group();
    private final Scale flameLength = new Scale(1, 1, 1);

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
                    // the nozzle reaches up into the engine, so their ends never lie flush
                    cylinder(ENGINE_RADIUS - 3, 14, side, -(ENGINE_TOP - ENGINE_LENGTH - 5), ENGINE_Z, dark));
            // each flame is a unit long until scaled, hanging down from its nozzle
            flames.getChildren().add(cylinder(6.5, 1, side, 0.5, 0, flame));
        }
        for (final int side : new int[] {-1, 1}) {
            // the glove: a fixed fairing on the engine's outer side, holding the wing's pivot
            root.getChildren().add(box(GLOVE + 8, ROOT_CHORD + 6, 8,
                    side * (ENGINE_X + ENGINE_RADIUS + GLOVE / 2 - 2), -HINGE_Y, ENGINE_Z, casing));
        }
        root.getChildren().addAll(
                wing(-1, leftSweep, wing, dark),
                wing(1, rightSweep, wing, dark),
                flames);
        flames.getTransforms().addAll(new Translate(0, -EXHAUST, ENGINE_Z), flameLength);
        flames.setVisible(false);
        root.setVisible(false);
    }

    Node node() {
        return root;
    }

    /** Shows or hides the pack; sets the wings to the avatar's sweep; lights the engines as hard as they fire. */
    void show(final Avatar avatar) {
        final boolean worn = avatar.wearingPack();
        final double thrust = avatar.thrust();
        final double time = avatar.body().time();
        root.setVisible(worn);
        final double sweep = avatar.sweep();
        // swept back, the left wing's tip goes down (+y): a negative turn about z for a tip at
        // -x. The right wing mirrors it.
        leftSweep.setAngle(-sweep);
        rightSweep.setAngle(sweep);

        flames.setVisible(thrust > 0);
        // a little flicker, from the clock, so the flames look alive
        final double flicker = 1 + 0.12 * Math.sin(time * 31) + 0.06 * Math.sin(time * 53);
        flameLength.setY(Math.max(1, MAX_FLAME * thrust * flicker));
    }

    /**
     * One wing, lying in the plane of the back with its leading edge up: on the left for
     * {@code side} -1, the right for 1. A broad inner panel and a narrower outer one with a
     * dark tip, all turning about the pivot in the glove.
     */
    private static Group wing(final int side, final Rotate sweep, final PhongMaterial material,
                              final PhongMaterial dark) {
        // in the wing's own frame: span along x, chord along y (leading edge at -y, up), thin in z
        final double lead = -ROOT_CHORD / 2;
        final double outerFrom = INNER_SPAN - 2;          // the panels overlap a little, never flush
        final double tip = outerFrom + OUTER_SPAN;
        final Group wing = new Group(
                box(INNER_SPAN, ROOT_CHORD, 4, side * INNER_SPAN / 2, 0, 0, material),
                // its leading edge half a unit below the inner panel's, so the tops never lie flush
                box(OUTER_SPAN, TIP_CHORD, 3.6, side * (outerFrom + OUTER_SPAN / 2),
                        lead + 0.5 + TIP_CHORD / 2, 0, material),
                // the tip cap stands 2 past the wing's end: flush, the two end faces would flicker
                box(4, TIP_CHORD + 1, 4.4, side * tip, lead + (TIP_CHORD + 1) / 2, 0, dark));
        wing.getTransforms().addAll(
                new Translate(side * (ENGINE_X + ENGINE_RADIUS + GLOVE), -HINGE_Y, ENGINE_Z),
                sweep);
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
