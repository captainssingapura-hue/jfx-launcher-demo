package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.geometry.Point3D;
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

import java.util.List;

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
 *     ├ shoulders ─ left arm, right arm  swing and spread at the shoulders, holding the weapons
 *     └ neck ─ head                      turns and tips at the neck
 * </pre>
 */
public final class BoxRig {

    /** The height of the joint the whole body tips about. */
    static final double MIDDLE = 105;
    /** How far the soles stand clear of the ground. */
    static final double SOLE = 3;

    static final double HIP = 80;
    private static final double SHOULDER = 144;
    private static final double NECK = 156;
    /** From the shoulder down to the elbow. */
    private static final double UPPER_ARM = 30;

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
    private final Rotate leftElbow = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rightElbow = new Rotate(0, Rotate.X_AXIS);
    private final Rotate leftTwist = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rightTwist = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate headTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate headTip = new Rotate(0, Rotate.X_AXIS);

    private final JetPack pack = new JetPack();
    private final Arsenal arsenal = new Arsenal();

    public BoxRig() {
        final PhongMaterial shell = matte("#e9e4da");
        final PhongMaterial trim = matte("#2f8f9d");
        final PhongMaterial joint = matte("#2a2e35");
        final PhongMaterial visor = glow("#7df9ff");
        final PhongMaterial beacon = glow("#ff6b6b");

        final Group body = new Group();
        body.getTransforms().addAll(inView, raise, bank, lean);

        // all of it but its arms and what they hold: out of the way when it is seen from its own eyes
        beyondView = List.of(
                limb(-15, -HIP, leftLeg, leftLegOut, leg(shell, joint)),
                limb(15, -HIP, rightLeg, rightLegOut, leg(shell, joint)),
                box(56, 16, 30, 0, -84, 0, joint),                        // pelvis
                box(64, 60, 34, 0, -120, 0, trim),                         // torso
                box(40, 22, 2, 0, -128, 17.5, shell),                      // chest plate
                pack.node(),
                box(18, 10, 18, 0, -152, 0, joint),                        // neck
                head(shell, joint, visor, beacon));
        body.getChildren().addAll(beyondView);
        body.getChildren().addAll(
                twisted(limb(-41, -SHOULDER, leftArm, leftSpread, arm(shell, joint, leftElbow, arsenal.leftHand())),
                        leftTwist),
                twisted(limb(41, -SHOULDER, rightArm, rightSpread, arm(shell, joint, rightElbow, arsenal.rightHand())),
                        rightTwist));

        root.getChildren().add(body);
        root.getTransforms().addAll(at, turn);
    }

    public Node node() {
        return root;
    }

    /** Where its eyes are in its head: its visor's middle, how far up from the neck and how far out in front. */
    static final double EYE_UP = 24;
    static final double EYE_OUT = 23;

    /**
     * Where its eyes are in the world, as it stands at {@code where} holding {@code pose}: its visor,
     * exactly where it is drawn: its head turned and tipped on its neck, the body raised, banked and
     * leaned about its middle, and turned its way. The view from them goes with its head, into a
     * dive, a run, a flight, as the figure does.
     */
    public static Vec3 eye(final Body where, final Pose pose) {
        // in the head: turned, then tipped, on the neck (see head())
        Point3D p = new Point3D(0, -EYE_UP, EYE_OUT);
        p = new Rotate(pose.headPitch(), Rotate.X_AXIS).transform(p);
        p = new Rotate(pose.headYaw(), Rotate.Y_AXIS).transform(p);
        p = p.add(0, -NECK, 0);
        // in the body: leaned, then banked, then raised (see the constructor)
        p = new Rotate(-pose.lean(), 0, -MIDDLE, 0, Rotate.X_AXIS).transform(p);
        p = new Rotate(pose.bank(), 0, -MIDDLE, 0, Rotate.Z_AXIS).transform(p);
        p = p.add(0, -(pose.lift() + SOLE), 0);
        // then turned its way, and put where it is
        p = new Rotate(where.yaw(), Rotate.Y_AXIS).transform(p);
        return where.position().add(p.getX(), p.getY(), p.getZ());
    }

    /**
     * Seen from its own eyes: its arms, and what they hold, as {@code pose} (upright, {@link Gait#upright})
     * has them, placed so its eyes, standing upright, are at {@code eye} facing {@code yaw}: steady
     * at the bottom of the view, however its body is tipped flying, diving or running, as a
     * shooter's are. (The rest of it is not drawn: {@link #fromItsEyes}.)
     */
    public void showFromEyes(final Avatar avatar, final Pose pose, final Vec3 eye, final double yaw) {
        // the arms placed from the middle of its head, not its visor: as far ahead of the view as a shooter's are
        final Point3D eyes = new Rotate(yaw, Rotate.Y_AXIS).transform(new Point3D(0, -(NECK + EYE_UP + SOLE), 0));
        posed(avatar, pose.upright());
        at.setX(eye.x() - eyes.getX());
        at.setY(eye.y() - eyes.getY());
        at.setZ(eye.z() - eyes.getZ());
        turn.setAngle(yaw);
    }

    /**
     * Seen from its own eyes, how much higher its arms are drawn, and how much further ahead, than
     * they are: held at the hip they are below the view, so they are drawn where a shooter's are seen,
     * at the bottom of it.
     */
    static final double IN_VIEW_UP = 30;
    static final double IN_VIEW_AHEAD = 30;
    private final Translate inView = new Translate();

    /** Whether it is seen from its own eyes. */
    private boolean fromEyes;

    /** Its legs, body, pack, neck and head: all but its arms and what they hold. */
    private final List<Node> beyondView;

    /**
     * Seen from its own eyes ({@code true}): only its arms and what they hold drawn, the rest of it
     * (its head first of all) out of the way of the view; or ({@code false}) whole.
     */
    public void fromItsEyes(final boolean eyes) {
        fromEyes = eyes;
        beyondView.forEach(part -> part.setVisible(!eyes));
        // and its arms drawn up into the view, as a shooter's are: the rounds still leave from where its hands are
        inView.setY(eyes ? -IN_VIEW_UP : 0);
        inView.setZ(eyes ? IN_VIEW_AHEAD : 0);
    }

    /** What the weapons throw off into the world (spent casings), to add to the scene beside the rig. */
    public Node effects() {
        return arsenal.effects();
    }

    /** Puts the figure where the avatar is, holding the pose, with the jet-pack on its back or not. */
    public void show(final Avatar avatar, final Pose pose) {
        final Body where = avatar.body();
        at.setX(where.position().x());
        at.setY(where.position().y());
        at.setZ(where.position().z());
        turn.setAngle(where.yaw());
        posed(avatar, pose);
    }

    /** Holds {@code pose}: the body raised, tipped and its limbs turned; the jet-pack and what the hands hold as the avatar has them. */
    private void posed(final Avatar avatar, final Pose pose) {
        // the soles hover a hair above the ground: a sole lying in the same
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
        leftSpread.setAngle(pose.leftSpread());
        rightSpread.setAngle(-pose.rightSpread());
        // a forearm hangs on along +y from the elbow, so it too bends forward with a positive turn
        leftElbow.setAngle(pose.leftElbow());
        rightElbow.setAngle(pose.rightElbow());
        // and a twist about the upper arm's own length turns which way the elbow bends
        leftTwist.setAngle(pose.leftTwist());
        rightTwist.setAngle(pose.rightTwist());
        headTurn.setAngle(pose.headYaw());
        headTip.setAngle(pose.headPitch());

        pack.show(avatar);
        // which shows itself when worn: not, seen from its own eyes, as nothing behind its arms is
        if (fromEyes) {
            pack.node().setVisible(false);
        }
        arsenal.show(avatar, pose);
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

    /**
     * An arm: the upper arm from the shoulder, and the forearm bending forward at the elbow, with
     * the hand at its end and what the hand holds.
     */
    private static Group arm(final PhongMaterial shell, final PhongMaterial joint, final Rotate elbow,
                             final Node held) {
        final Group forearm = new Group(
                box(17, 8, 17, 0, 0, 0, joint),                            // elbow
                box(18, 26, 18, 0, 15, 0, shell),
                box(16, 14, 16, 0, Arsenal.HAND, 0, joint),                // hand
                held);
        forearm.getTransforms().addAll(new Translate(0, UPPER_ARM, 0), elbow);
        return new Group(
                box(22, 18, 22, 0, 0, 0, joint),                           // shoulder
                box(18, 30, 18, 0, 15, 0, shell),                          // upper arm
                forearm);
    }

    /** An arm hung by {@link #limb}, then twisted about its own length. */
    private static Group twisted(final Group arm, final Rotate twist) {
        arm.getTransforms().add(twist);
        return arm;
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
