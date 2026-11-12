package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * The weapons, as drawn: a rifle in each hand, the Gatling gun or the glaive in the right. They
 * hang from the hands, so they move as the arms do ({@link Gait} poses the arms for each). With
 * them come their effects: a flash at a muzzle as it fires, and a brass casing thrown out to its
 * right, which falls to the ground and lies there a moment. The Gatling gun's barrels turn as
 * fast as the simulation says they spin.
 *
 * <p>Each weapon is built in its forearm's frame: the forearm hangs along +y from the elbow,
 * the hand at {@link #HAND}, and +z is the forearm's front, which is up when it is folded
 * forward.
 */
final class Arsenal {

    /** Where the hand is, down the forearm from the elbow. */
    static final double HAND = 32;
    /**
     * How far in from the middle of its arm each rifle sits: as far as the side of the body, so
     * its stock is held between the upper arm and the body.
     */
    private static final double INWARD = 9;
    /**
     * The Gatling gun: how far in from the right hand its barrels run, toward the middle of the
     * body; and where, in the forearm's frame, the lever on top is that the left hand holds.
     * Near enough the body for the short left arm to reach across to it.
     */
    static final double GUN_IN = 22;
    static final double[] LEVER = {-44, HAND + 2, 14};
    /** How fast the barrels are drawn turning at full speed, in degrees a second: fast, but not so fast they seem to stand still. */
    private static final double BARRELS_TURN = 1500;
    /** How long a muzzle flash shows, in seconds: a frame or two. */
    private static final double FLASH = 0.035;
    /** Casings in flight or lying about at once; the oldest is reused for the next. */
    private static final int CASINGS = 96;
    private static final double CASING_LIFE = 2.5;
    private static final double GRAVITY = 2600;

    private final Group leftHand = new Group();
    private final Group rightHand = new Group();
    private final Group leftRifle;
    private final Group rightRifle;
    private final Node leftFlash;
    private final Node rightFlash;
    private final Rotate leftFlashTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rightFlashTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Group leftPort;
    private final Group rightPort;
    private final Group glaive;
    private final Rotate grip = new Rotate(180, Rotate.X_AXIS);
    private final Group gatling;
    private final Node gatlingFlash;
    private final Rotate gatlingFlashTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Group gatlingPort = new Group();
    private final Rotate barrels = new Rotate(0, Rotate.Y_AXIS);

    private final Group effects = new Group();
    private final Casing[] casings = new Casing[CASINGS];
    private int nextCasing;
    private double lastTime = Double.NaN;
    private double lastLeft = Double.POSITIVE_INFINITY;
    private double lastRight = Double.POSITIVE_INFINITY;
    private int rounds;

    Arsenal() {
        final PhongMaterial steel = shiny("#2b2e33");
        final PhongMaterial wood = BoxRig.matte("#8a4b25");
        final PhongMaterial flash = BoxRig.glow("#fff3b0");
        final PhongMaterial brass = shiny("#c8a24a");

        leftFlash = flash(flash, leftFlashTurn);
        rightFlash = flash(flash, rightFlashTurn);
        leftPort = new Group();
        rightPort = new Group();
        leftRifle = rifle(steel, wood, leftFlash, leftPort, INWARD);
        rightRifle = rifle(steel, wood, rightFlash, rightPort, -INWARD);
        glaive = glaive();
        gatlingFlash = flash(flash, gatlingFlashTurn);
        gatling = gatling(steel, gatlingFlash);
        leftHand.getChildren().add(leftRifle);
        rightHand.getChildren().addAll(rightRifle, glaive, gatling);

        for (int i = 0; i < CASINGS; i++) {
            casings[i] = new Casing(box(1.8, 5, 1.8, 0, 0, 0, brass));
            effects.getChildren().add(casings[i].shape);
        }
        show(Weapon.NONE);
    }

    /** What hangs from the left hand, and the right: added to the arms by the rig. */
    Node leftHand() {
        return leftHand;
    }

    Node rightHand() {
        return rightHand;
    }

    /** The casings, in the world's frame: added to the scene beside the rig, not on it. */
    Node effects() {
        return effects;
    }

    /** Shows what is held, as the pose holds it; flashes each muzzle that fired; throws out the casings. */
    void show(final Avatar avatar, final Pose pose) {
        final Arms arms = avatar.arms();
        show(arms.held());
        grip.setAngle(pose.grip());

        final boolean rifles = arms.held() == Weapon.RIFLES;
        final boolean gun = arms.held() == Weapon.GATLING;
        leftFlash.setVisible(rifles && arms.sinceLeft() < FLASH);
        rightFlash.setVisible(rifles && arms.sinceRight() < FLASH);
        gatlingFlash.setVisible(gun && arms.sinceRight() < FLASH);
        // a round fired since last time: its rifle's "since" started again from nothing
        if (rifles && arms.sinceLeft() < lastLeft && arms.sinceLeft() < FLASH) {
            fired(leftPort, leftFlashTurn, avatar);
        }
        if (rifles && arms.sinceRight() < lastRight && arms.sinceRight() < FLASH) {
            fired(rightPort, rightFlashTurn, avatar);
        }
        if (gun && arms.sinceRight() < lastRight && arms.sinceRight() < FLASH) {
            fired(gatlingPort, gatlingFlashTurn, avatar);
        }
        lastLeft = arms.sinceLeft();
        lastRight = arms.sinceRight();

        final double time = avatar.body().time();
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, Math.min(0.1, time - lastTime));
        lastTime = time;
        // the barrels turn as fast as they spin
        barrels.setAngle((barrels.getAngle() + BARRELS_TURN * arms.spin() * seconds) % 360);
        for (final Casing casing : casings) {
            casing.fly(seconds);
        }
    }

    private void show(final Weapon held) {
        leftRifle.setVisible(held == Weapon.RIFLES);
        rightRifle.setVisible(held == Weapon.RIFLES);
        glaive.setVisible(held == Weapon.GLAIVE);
        gatling.setVisible(held == Weapon.GATLING);
    }

    /** A round went: the flash turned a new way, and a casing thrown out of the port, up and to the right. */
    private void fired(final Node port, final Rotate flashTurn, final Avatar avatar) {
        rounds++;
        flashTurn.setAngle(rounds * 37 % 90);
        final Point3D at = port.localToScene(Point3D.ZERO);
        final Vec3 right = Num.right(avatar.body().yaw());
        final Vec3 back = Num.heading(avatar.body().yaw()).multiply(-1);
        // a little different each time, from the round's count rather than chance
        final double spread = ((rounds * 7919) % 100) / 100.0 - 0.5;
        final Vec3 velocity = right.multiply(220 + 60 * spread).add(back.multiply(40 + 40 * spread))
                .add(0, -260 - 80 * spread, 0);
        casings[nextCasing].throwFrom(new Vec3(at.getX(), at.getY(), at.getZ()), velocity, rounds);
        nextCasing = (nextCasing + 1) % CASINGS;
    }

    // ---- the models

    /**
     * A Gatling gun with a motor, held at the hip: a short grip bar out of its right side in the
     * right hand; the receiver and motor running forward, nearer the middle of the body; six
     * barrels round a turning spindle, held by two clamps; the ammunition box below on the left;
     * and, on top toward the rear, a lever standing out to the left for the left hand.
     */
    private Group gatling(final PhongMaterial steel, final Node flash) {
        final PhongMaterial metal = shiny("#3a3e44");
        final PhongMaterial olive = BoxRig.matte("#4b5320");
        final double x = -GUN_IN;
        final double y = HAND;
        final Group cluster = new Group();
        for (int i = 0; i < 6; i++) {
            final double a = Math.toRadians(i * 60);
            cluster.getChildren().add(cylinder(1.5, 66, 4.2 * Math.cos(a), 0, 4.2 * Math.sin(a), steel));
        }
        cluster.getChildren().addAll(
                cylinder(1.2, 70, 0, 0, 0, metal),                          // the spindle
                cylinder(6.6, 3, 0, -26, 0, metal),                          // rear clamp
                cylinder(6.6, 3, 0, 22, 0, metal));                          // front clamp
        cluster.getTransforms().addAll(new Translate(x, y + 53, 4), barrels);
        gatlingPort.getTransforms().add(new Translate(x + 7, y + 8, 6));
        flash.getTransforms().add(0, new Translate(x, y + 88, 4));
        return new Group(
                box(18, 5, 5, -8, y, 0, metal),                              // the grip bar, out to the hand
                box(5, 6, 14, 0, y, -2, metal),                              // the grip in the hand
                box(13, 30, 14, x, y + 5, 4, steel),                         // the receiver
                cylinder(5, 14, x, y - 15, 9, metal),                        // the motor, at the back
                box(14, 18, 13, x - 2, y - 2, -11, olive),                   // the ammunition box
                box(3, 4, 12, x - 4, y + 12, -3, olive),                     // its feed chute
                box(22, 3, 3, (x + LEVER[0]) / 2, LEVER[1], LEVER[2] - 2, metal), // the lever's arm, out to the left
                box(3, 10, 3, LEVER[0], LEVER[1], LEVER[2], metal),            // the lever, held fore and aft
                cluster,
                gatlingPort,
                flash);
    }

    /**
     * An assault rifle in the style of an AK, held from the hip: the hand on its pistol grip, its
     * finger at the trigger; the barrel ahead along the forearm; the wooden stock back past the
     * elbow, where the upper arm clamps it against the body; the curved magazine below.
     * {@code across} moves it toward the body: + for the left forearm, - for the right.
     */
    private static Group rifle(final PhongMaterial steel, final PhongMaterial wood, final Node flash,
                               final Group port, final double across) {
        final double y = HAND;
        final Group magazine = new Group(
                box(5, 8, 11, 0, 0, -5.5, steel),
                box(5, 8, 10, 0, 3, -14.5, steel));
        // the magazine curves forward as it goes down
        magazine.getTransforms().addAll(new Translate(0, y + 11, 6), new Rotate(-18, Rotate.X_AXIS));
        port.getTransforms().add(new Translate(4, y + 6, 15));
        flash.getTransforms().add(0, new Translate(0, y + 58, 15));
        final Group rifle = new Group(
                box(6, 30, 9, 0, y + 5, 13, steel),                          // receiver
                box(5.4, 28, 2, 0, y + 5, 18.5, steel),                      // its cover
                cylinder(1.6, 34, 0, y + 37, 15, steel),                     // barrel
                cylinder(2.3, 6, 0, y + 54, 15, steel),                      // muzzle brake
                cylinder(1.3, 22, 0, y + 33, 18.6, steel),                   // gas tube
                box(7, 16, 8, 0, y + 30, 14, wood),                          // handguard
                box(1.6, 2, 6, 0, y + 47, 19.5, steel),                      // front sight
                box(5, 32, 8, 0, y - 25, 11, wood),                          // stock, back past the elbow
                box(5.4, 6, 12, 0, y - 2, 5, wood),                          // pistol grip
                magazine,
                port,
                flash);
        rifle.getTransforms().add(new Translate(across, 0, 0));
        return rifle;
    }

    /** A flash at the muzzle: a bright cross, as long as it is wide, turned a new way each round. */
    private static Node flash(final PhongMaterial material, final Rotate turn) {
        final Group flash = new Group(
                box(3, 16, 3, 0, 8, 0, material),
                box(12, 3, 3, 0, 4, 0, material),
                box(3, 3, 12, 0, 4, 0, material));
        flash.getTransforms().add(turn);
        flash.setVisible(false);
        return flash;
    }

    /**
     * A glaive as Guan Yu carried: a long lacquered pole, a gold collar, a broad crescent blade
     * with a bright edge and a hook on its back, a red tassel, and a spike at the butt. Built
     * along +y from the hand, the blade's edge toward +z; the wrist turns it about the hand.
     */
    private Group glaive() {
        final PhongMaterial pole = BoxRig.matte("#6b1d16");
        final PhongMaterial steel = shiny("#b9bfc8");
        final PhongMaterial edge = shiny("#eef2f6");
        final PhongMaterial gold = shiny("#c9a227");
        final PhongMaterial tassel = BoxRig.matte("#b3121b");
        final Group tip = new Group(box(2.2, 24, 12, 0, 0, 0, steel), box(2.4, 24, 2, 0, 0, 6.4, edge));
        tip.getTransforms().addAll(new Translate(0, 186, 9), new Rotate(-24, Rotate.X_AXIS));
        final Group hook = new Group(box(2, 12, 8, 0, 0, 0, steel));
        hook.getTransforms().addAll(new Translate(0, 138, -7), new Rotate(30, Rotate.X_AXIS));
        final Group weapon = new Group(
                cylinder(2.3, 175, 0, 27.5, 0, pole),                        // the pole, -60 to 115
                cylinder(1.5, 12, 0, -66, 0, steel),                         // the butt spike
                box(6, 10, 7, 0, 119, 0, gold),                              // the collar
                box(2.4, 62, 17, 0, 155, 5, steel),                          // the blade
                box(2.6, 60, 2.2, 0, 155, 13.6, edge),                       // its edge
                tip,
                hook,
                box(3, 18, 3, 0, 108, -5, tassel));                          // the tassel
        // in the hand, just in front of the fist, turned by the wrist
        weapon.getTransforms().addAll(new Translate(0, HAND, 10.5), grip);
        return weapon;
    }

    private static PhongMaterial shiny(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#ffffff", 0.8));
        material.setSpecularPower(28);
        return material;
    }

    private static Box box(final double w, final double h, final double d,
                           final double x, final double y, final double z, final PhongMaterial material) {
        return placed(new Box(w, h, d), x, y, z, material);
    }

    private static Cylinder cylinder(final double radius, final double height,
                                     final double x, final double y, final double z, final PhongMaterial material) {
        return placed(new Cylinder(radius, height, 12), x, y, z, material);
    }

    private static <S extends Shape3D> S placed(final S shape, final double x, final double y, final double z,
                                                final PhongMaterial material) {
        shape.setTranslateX(x);
        shape.setTranslateY(y);
        shape.setTranslateZ(z);
        shape.setMaterial(material);
        return shape;
    }

    /** One spent casing: thrown out, tumbling, falling to the ground, lying there, then gone. */
    private static final class Casing {
        private final Box shape;
        private final Rotate tumble = new Rotate(0, new Point3D(1, 0.3, 0.6));
        private Vec3 at = Vec3.ZERO;
        private Vec3 velocity = Vec3.ZERO;
        private double spin;
        private double age = CASING_LIFE;

        Casing(final Box shape) {
            this.shape = shape;
            shape.getTransforms().add(tumble);
            shape.setVisible(false);
        }

        void throwFrom(final Vec3 from, final Vec3 speed, final int round) {
            at = from;
            velocity = speed;
            spin = 900 + (round % 5) * 150;
            age = 0;
            shape.setVisible(true);
        }

        void fly(final double seconds) {
            if (age >= CASING_LIFE) {
                return;
            }
            age += seconds;
            if (age >= CASING_LIFE) {
                shape.setVisible(false);
                return;
            }
            velocity = velocity.add(0, GRAVITY * seconds, 0);
            at = at.add(velocity.multiply(seconds));
            // the ground: it bounces once, low, then lies still
            final double rest = -1;
            if (at.y() > rest) {
                at = new Vec3(at.x(), rest, at.z());
                velocity = velocity.y() > 300 ? new Vec3(velocity.x() * 0.4, -velocity.y() * 0.3, velocity.z() * 0.4)
                        : Vec3.ZERO;
                spin *= 0.3;
            }
            tumble.setAngle(tumble.getAngle() + spin * seconds);
            shape.setTranslateX(at.x());
            shape.setTranslateY(at.y());
            shape.setTranslateZ(at.z());
        }
    }
}
