package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Turret;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.List;

/**
 * The guns on their mounts ({@link Turret}), drawn where the world says they are: a heavy Gatling
 * gun on a pedestal, turned round and tipped up or down as its turret is, its six long barrels
 * spinning as fast as its turret says, and a flash at the muzzle while it fires. Gone with what
 * carries it, once that breaks.
 *
 * <p>Built in the gun's own frame, its barrels along +z from where it turns about, so turning it
 * round the vertical ({@code Rotate(heading, Y)}) and tipping it up ({@code Rotate(pitch, X)}, which
 * lifts +z in JavaFX's y-down frame) points it as the turret points.
 */
public final class Guns {

    /** Degrees a second its barrels turn at full speed. */
    private static final double SPIN = 1500;
    /** How far out from the gun's middle each barrel runs, and how thick it is. */
    private static final double RING = 13;
    private static final double BORE = 4.5;
    /** The heavy gun's barrels' length, as every gun is drawn before it is made bigger or smaller. */
    private static final double HEAVY = 240;
    /** How far above where a hanging gun turns the floor it hangs from is (as the airship's is hung). */
    private static final double HANG = 45;

    private final Group root = new Group();
    private final List<Mounted> mounted = new ArrayList<>();
    private long frames;

    /** One gun: where it stands, turned and tipped, its barrels spinning, and its flash. */
    private static final class Mounted {
        final Group node = new Group();
        final Rotate heading = new Rotate(0, Rotate.Y_AXIS);
        final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
        final Rotate spin = new Rotate(0, Rotate.Z_AXIS);
        final Sphere flash = new Sphere(16);
    }

    public Guns(final List<Turret> turrets) {
        final PhongMaterial metal = BoxRig.matte("#2d3136");
        final PhongMaterial olive = BoxRig.matte("#4b5334");
        final PhongMaterial steel = BoxRig.matte("#5d646b");
        for (final Turret turret : turrets) {
            final Mounted gun = new Mounted();
            // every gun is drawn as the heavy one is, and made as much bigger or smaller as its barrels are long
            final double barrel = HEAVY;
            final double size = turret.mount().barrel() / HEAVY;
            final double drop = turret.mount().pivot().up();
            final Group stand = drop >= 0 ? pedestal(drop, metal) : hanger(size, metal);
            // turned round: the cradle; tipped: the receiver, the ammunition box, the grips and the barrels
            final Group turned = new Group();
            turned.getTransforms().addAll(gun.heading, new Scale(size, size, size));
            final Box cradle = box(56, 14, 40, 0, 16, 0, metal);
            final Group tipped = new Group();
            tipped.getTransforms().add(gun.pitch);
            // the receiver hangs a little under the barrels, so that the gunner sees over it along them
            final Box receiver = box(40, 34, 86, 0, 14, -10, metal);
            final Box ammunition = box(30, 34, 46, 38, 18, -14, olive);
            final Box grips = box(46, 8, 8, 0, 22, -62, steel);
            final Group barrels = new Group();
            barrels.getTransforms().add(gun.spin);
            for (int i = 0; i < 6; i++) {
                final double angle = Math.toRadians(60 * i);
                final Cylinder tube = new Cylinder(BORE, barrel - 30);
                tube.setMaterial(metal);
                tube.getTransforms().addAll(new Rotate(90, Rotate.X_AXIS));
                tube.setTranslateX(Math.cos(angle) * RING);
                tube.setTranslateY(Math.sin(angle) * RING);
                tube.setTranslateZ(30 + (barrel - 30) / 2);
                barrels.getChildren().add(tube);
            }
            // clamps round the barrels, at the middle and the muzzle
            for (final double along : new double[] {30 + (barrel - 30) * 0.45, barrel - 8}) {
                final Cylinder clamp = new Cylinder(RING + BORE + 2, 10);
                clamp.setMaterial(steel);
                clamp.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
                clamp.setTranslateZ(along);
                barrels.getChildren().add(clamp);
            }
            gun.flash.setMaterial(BoxRig.glow("#ffd27a"));
            gun.flash.setTranslateZ(barrel + 14);
            gun.flash.setVisible(false);
            tipped.getChildren().addAll(receiver, ammunition, grips, barrels, gun.flash);
            turned.getChildren().addAll(cradle, tipped);
            gun.node.getChildren().addAll(stand, turned);
            mounted.add(gun);
            root.getChildren().add(gun.node);
        }
        root.setMouseTransparent(true);
    }

    public Node node() {
        return root;
    }

    /**
     * Each gun as {@code turrets} have it, {@code seconds} after the last frame, those on what
     * {@code damage} has broken gone; moved on by {@code lag}, by what carries it, to where that is
     * drawn now.
     */
    public void show(final List<Turret> turrets, final Damage damage, final double seconds,
                     final java.util.Map<Integer, Vec3> lag) {
        frames++;
        for (int i = 0; i < mounted.size() && i < turrets.size(); i++) {
            final Turret turret = turrets.get(i);
            final Mounted gun = mounted.get(i);
            final boolean standing = !damage.broken(turret.host());
            gun.node.setVisible(standing);
            if (!standing) {
                continue;
            }
            final Vec3 pivot = turret.pivot().add(lag.getOrDefault(turret.host(), Vec3.ZERO));
            gun.node.setTranslateX(pivot.x());
            gun.node.setTranslateY(pivot.y());
            gun.node.setTranslateZ(pivot.z());
            gun.heading.setAngle(turret.heading());
            gun.pitch.setAngle(turret.pitch());
            gun.spin.setAngle((gun.spin.getAngle() + SPIN * turret.gun().spin() * seconds) % 360);
            // the flash flickers, a frame on and a frame off, while it fires
            gun.flash.setVisible(turret.firing() && frames % 2 == 0);
        }
    }

    /** A pedestal, from the floor {@code drop} below where the gun turns up to it. */
    private static Group pedestal(final double drop, final PhongMaterial metal) {
        final Cylinder post = new Cylinder(10, drop);
        post.setTranslateY(drop / 2);
        post.setMaterial(metal);
        final Cylinder foot = new Cylinder(34, 8);
        foot.setTranslateY(drop - 4);
        foot.setMaterial(metal);
        return new Group(post, foot);
    }

    /**
     * A turret hung under what carries it, {@link #HANG} above where the gun turns: a ring against
     * the floor, a post down from it, and a ball round where the gun turns, {@code size} times the
     * heavy gun's.
     */
    private static Group hanger(final double size, final PhongMaterial metal) {
        final Cylinder ring = new Cylinder(46 * size, 10);
        ring.setTranslateY(-HANG + 5);
        ring.setMaterial(metal);
        final Cylinder post = new Cylinder(16 * size, HANG);
        post.setTranslateY(-HANG / 2);
        post.setMaterial(metal);
        final Sphere ball = new Sphere(30 * size);
        ball.setMaterial(metal);
        return new Group(ring, post, ball);
    }

    private static Box box(final double w, final double h, final double d, final double x, final double y,
                           final double z, final PhongMaterial material) {
        final Box box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }
}
