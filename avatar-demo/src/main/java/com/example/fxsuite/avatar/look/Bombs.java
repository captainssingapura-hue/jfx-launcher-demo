package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Sky;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.List;

/**
 * The bombs falling from the airships ({@link Sky#bombs}): each a dark casing with a rounded
 * nose and a cross of fins, nose along the way it is falling. Drawn where it was a moment before
 * its latest step, so it falls smoothly between steps, as the avatar does.
 */
public final class Bombs {

    /** At most this many in the air at once: three airships a bomb each every few seconds is a handful. */
    private static final int POOL = 24;

    private final Group root = new Group();
    private final Shell[] shells = new Shell[POOL];

    public Bombs() {
        final PhongMaterial casing = BoxRig.matte("#2b2f33");
        final PhongMaterial band = BoxRig.matte("#c9a227");
        for (int i = 0; i < POOL; i++) {
            shells[i] = new Shell(casing, band);
            root.getChildren().add(shells[i].node);
        }
    }

    public Node node() {
        return root;
    }

    /** Shows {@code sky}'s bombs as they were {@code behind} seconds before its latest step. */
    public void show(final Sky sky, final double behind) {
        final List<Sky.Bomb> bombs = sky.bombs();
        for (int i = 0; i < POOL; i++) {
            if (i < bombs.size()) {
                final Sky.Bomb bomb = bombs.get(i);
                shells[i].place(bomb.at().subtract(bomb.velocity().multiply(behind)), bomb.velocity());
            } else {
                shells[i].node.setVisible(false);
            }
        }
    }

    private static final class Shell {
        private final Group node = new Group();
        private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
        private final Rotate tilt = new Rotate(0, Rotate.X_AXIS);

        Shell(final PhongMaterial casing, final PhongMaterial band) {
            // along its own y, nose down (y is down): the casing, the nose below it, the fins above
            final Cylinder body = new Cylinder(14, 56);
            body.setMaterial(casing);
            final Sphere nose = new Sphere(14, 12);
            nose.setMaterial(casing);
            nose.setTranslateY(28);
            final Cylinder stripe = new Cylinder(14.6, 6);
            stripe.setMaterial(band);
            stripe.setTranslateY(10);
            final Box finA = new Box(40, 18, 2);
            final Box finB = new Box(2, 18, 40);
            finA.setMaterial(casing);
            finB.setMaterial(casing);
            finA.setTranslateY(-30);
            finB.setTranslateY(-30);
            node.getChildren().addAll(body, nose, stripe, finA, finB);
            node.getTransforms().addAll(yaw, tilt);
            node.setVisible(false);
        }

        /** At {@code at}, its nose along {@code velocity}. */
        void place(final Vec3 at, final Vec3 velocity) {
            node.setTranslateX(at.x());
            node.setTranslateY(at.y());
            node.setTranslateZ(at.z());
            final double along = Math.hypot(velocity.x(), velocity.z());
            // turned to face the way it drifts, then tipped from straight down toward it
            yaw.setAngle(Math.toDegrees(Math.atan2(velocity.x(), velocity.z())));
            tilt.setAngle(Math.toDegrees(Math.atan2(along, Math.max(velocity.y(), 1e-9))));
            node.setVisible(true);
        }
    }
}
