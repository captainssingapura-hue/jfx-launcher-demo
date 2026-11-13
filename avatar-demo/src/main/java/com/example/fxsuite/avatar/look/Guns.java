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
            final double barrel = turret.mount().barrel();
            final double drop = turret.mount().pivot().up();
            // the pedestal, from the floor up to where it turns
            final Cylinder post = new Cylinder(10, drop);
            post.setTranslateY(drop / 2);
            post.setMaterial(metal);
            final Cylinder foot = new Cylinder(34, 8);
            foot.setTranslateY(drop - 4);
            foot.setMaterial(metal);
            // turned round: the cradle; tipped: the receiver, the ammunition box, the grips and the barrels
            final Group turned = new Group();
            turned.getTransforms().add(gun.heading);
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
            gun.node.getChildren().addAll(post, foot, turned);
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
     * {@code damage} has broken gone.
     */
    public void show(final List<Turret> turrets, final Damage damage, final double seconds) {
        frames++;
        for (int i = 0; i < mounted.size() && i < turrets.size(); i++) {
            final Turret turret = turrets.get(i);
            final Mounted gun = mounted.get(i);
            final boolean standing = !damage.broken(turret.host());
            gun.node.setVisible(standing);
            if (!standing) {
                continue;
            }
            final Vec3 pivot = turret.pivot();
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
