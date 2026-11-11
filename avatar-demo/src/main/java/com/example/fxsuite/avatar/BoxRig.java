package com.example.fxsuite.avatar;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * The avatar as a scene graph: a small courier robot built from boxes, which can wear a
 * {@link JetPack} on its back. It has no mesh and no skin: each limb is a rigid part hung from
 * a joint, and {@link #show} moves it by turning the joints. That is all the animation there is.
 *
 * <p>The figure faces +z with its feet at the origin, about 2 m (200 units) tall. Its right is
 * +x, and up is -y, as JavaFX's y axis points down.
 *
 * <pre>
 * root    at, turn                       where it stands, which way it faces
 *  └ body raise, bank, lean              tipped about its middle
 *     ├ hips ─ left leg, right leg       swing and spread at the hips
 *     ├ torso, and the jet-pack when worn
 *     ├ shoulders ─ left arm, right arm  swing and spread at the shoulders
 *     └ neck ─ head                      turns and tips at the neck
 * </pre>
 */
final class BoxRig {

    /** The height of the joint the whole body tips about. */
    static final double MIDDLE = 105;
    /** Where the chase camera looks: about the head. */
    static final double EYE_HEIGHT = 160;
    /** How far the soles stand clear of the ground, above the blob shadow on it. */
    static final double SOLE = 3;

    private static final double HIP = 80;
    private static final double SHOULDER = 144;
    private static final double NECK = 156;

    private final Group root = new Group();
    private final Translate at = new Translate();
    private final Rotate turn = new Rotate(0, Rotate.Y_AXIS);
    private final Translate raise = new Translate();
    private final Rotate bank = new Rotate(0, 0, -MIDDLE, 0, Rotate.Z_AXIS);
    private final Rotate lean = new Rotate(0, 0, -MIDDLE, 0, Rotate.X_AXIS);

    private final Rotate leftLeg = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rightLeg = new Rotate(0, Rotate.X_AXIS);
    private final Rotate leftLegOut = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate rightLegOut = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate leftArm = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rightArm = new Rotate(0, Rotate.X_AXIS);
    private final Rotate leftSpread = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate rightSpread = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate headTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate headTip = new Rotate(0, Rotate.X_AXIS);

    private final JetPack pack = new JetPack();

    BoxRig() {
        final PhongMaterial shell = matte("#e9e4da");
        final PhongMaterial trim = matte("#2f8f9d");
        final PhongMaterial joint = matte("#2a2e35");
        final PhongMaterial visor = glow("#7df9ff");
        final PhongMaterial beacon = glow("#ff6b6b");

        final Group body = new Group();
        body.getTransforms().addAll(raise, bank, lean);

        body.getChildren().addAll(
                limb(-15, -HIP, leftLeg, leftLegOut, leg(shell, joint)),
                limb(15, -HIP, rightLeg, rightLegOut, leg(shell, joint)),
                box(56, 16, 30, 0, -84, 0, joint),                        // pelvis
                box(64, 60, 34, 0, -120, 0, trim),                         // torso
                box(40, 22, 2, 0, -128, 17.5, shell),                      // chest plate
                pack.node(),
                limb(-41, -SHOULDER, leftArm, leftSpread, arm(shell, joint)),
                limb(41, -SHOULDER, rightArm, rightSpread, arm(shell, joint)),
                box(18, 10, 18, 0, -152, 0, joint),                        // neck
                head(shell, joint, visor, beacon));

        root.getChildren().add(body);
        root.getTransforms().addAll(at, turn);
    }

    Node node() {
        return root;
    }

    /** Puts the figure where the body is, holding the pose, with the jet-pack on its back or not. */
    void show(final Body where, final Pose pose, final boolean wearingPack) {
        at.setX(where.position().getX());
        at.setY(where.position().getY());
        at.setZ(where.position().getZ());
        turn.setAngle(where.yaw());
        // the soles hover a hair above the ground and its blob shadow: a sole lying in the same
        // plane as the ground flickers, as the renderer cannot tell which is in front
        raise.setY(-(pose.lift() + SOLE));
        // forward lean tips the top toward +z, a negative turn about x; right bank drops +x, positive about z
        lean.setAngle(-pose.lean());
        bank.setAngle(pose.bank());
        // a limb hangs along +y, so a positive turn about x swings it forward
        leftLeg.setAngle(pose.leftLeg());
        rightLeg.setAngle(pose.rightLeg());
        leftLegOut.setAngle(pose.legSpread());
        rightLegOut.setAngle(-pose.legSpread());
        leftArm.setAngle(pose.leftArm());
        rightArm.setAngle(pose.rightArm());
        // (for the legs above and the arms below, out to the side is toward -x on the left, a
        // positive turn about z, and the mirror on the right)
        leftSpread.setAngle(pose.armSpread());
        rightSpread.setAngle(-pose.armSpread());
        headTurn.setAngle(pose.headYaw());
        headTip.setAngle(pose.headPitch());

        pack.show(wearingPack, where, pose);
    }

    private Group head(final PhongMaterial shell, final PhongMaterial joint, final PhongMaterial visor,
                       final PhongMaterial beacon) {
        final Group head = new Group(
                box(46, 40, 42, 0, -22, 0, shell),
                box(38, 12, 3, 0, -24, 21.5, visor),
                box(48, 6, 44, 0, -3, 0, joint),                           // jaw line
                cylinder(2, 26, 14, -53, 0, joint),                        // antenna
                sphere(5, 14, -68, 0, beacon));
        head.getTransforms().addAll(new Translate(0, -NECK, 0), headTurn, headTip);
        return head;
    }

    private static Group leg(final PhongMaterial shell, final PhongMaterial joint) {
        return new Group(
                box(24, 72, 26, 0, 38, 0, shell),
                box(26, 8, 28, 0, 40, 0, joint),                           // knee band
                box(26, 12, 34, 0, 74, 5, joint));                         // foot, its heel inside the leg
    }

    private static Group arm(final PhongMaterial shell, final PhongMaterial joint) {
        return new Group(
                box(22, 18, 22, 0, 0, 0, joint),                           // shoulder
                box(18, 56, 18, 0, 30, 0, shell),
                box(16, 14, 16, 0, 62, 0, joint));                         // hand
    }

    /** A part hung from a joint at (x, y): it swings about x, then spreads about z. */
    private static Group limb(final double x, final double y, final Rotate swing, final Rotate spread,
                              final Group part) {
        part.getTransforms().add(new Translate(x, y, 0));
        part.getTransforms().add(swing);
        if (spread != null) {
            part.getTransforms().add(spread);
        }
        return part;
    }

    private static Box box(final double w, final double h, final double d,
                           final double x, final double y, final double z, final PhongMaterial material) {
        return placed(new Box(w, h, d), x, y, z, material);
    }

    private static Cylinder cylinder(final double radius, final double height,
                                     final double x, final double y, final double z, final PhongMaterial material) {
        return placed(new Cylinder(radius, height, 16), x, y, z, material);
    }

    private static Sphere sphere(final double radius, final double x, final double y, final double z,
                                 final PhongMaterial material) {
        return placed(new Sphere(radius, 12), x, y, z, material);
    }

    private static <S extends Shape3D> S placed(final S shape, final double x, final double y, final double z,
                                                final PhongMaterial material) {
        shape.setTranslateX(x);
        shape.setTranslateY(y);
        shape.setTranslateZ(z);
        shape.setMaterial(material);
        return shape;
    }

    static PhongMaterial matte(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#3a3a3a"));
        return material;
    }

    /** Lit from within: it shows its colour whatever the lights. */
    static PhongMaterial glow(final String colour) {
        final Color c = Color.web(colour);
        final PhongMaterial material = new PhongMaterial(c);
        final WritableImage lit = new WritableImage(1, 1);
        lit.getPixelWriter().setColor(0, 0, c);
        material.setSelfIlluminationMap(lit);
        return material;
    }
}
