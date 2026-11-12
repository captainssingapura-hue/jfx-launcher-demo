package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * The crosshair, out in the world: a small bright cross on the line the rifles shoot along,
 * where it meets a block or the ground ({@link Arms#sight}). It only shows where the rifles are
 * shooting; it does not steer them. Being in the world rather than painted over the screen, it
 * is always in front of the avatar, and the avatar is never hidden behind it. Turned to face the
 * camera, and sized by its distance from it, so it looks the same size wherever it is.
 */
public final class Reticle {

    /** The cross's size against its distance from the camera. */
    private static final double SCALE = 1.0 / 650;

    private final Group root = new Group();
    private final Translate at = new Translate();
    private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
    private final Scale size = new Scale(1, 1, 1);

    public Reticle() {
        final PhongMaterial bright = BoxRig.glow("#f5f7fa");
        final PhongMaterial edge = BoxRig.matte("#14181d");
        // four arms round an open middle, each on a darker edge just behind it, to show on any ground
        for (final double[] arm : new double[][] {{-9, 0, 10, 2}, {9, 0, 10, 2}, {0, -9, 2, 10}, {0, 9, 2, 10}}) {
            root.getChildren().addAll(
                    box(arm[2] + 2, arm[3] + 2, 0.4, arm[0], arm[1], -0.5, edge),
                    box(arm[2], arm[3], 0.4, arm[0], arm[1], 0, bright));
        }
        root.getTransforms().addAll(at, yaw, pitch, size);
        root.setMouseTransparent(true);
        root.setVisible(false);
    }

    public Node node() {
        return root;
    }

    /** Shown with the rifles drawn: on their line ahead, among {@code world}, facing a camera at {@code eye}. */
    public void show(final Avatar avatar, final Obstacles world, final Vec3 eye) {
        final boolean rifles = avatar.holding() == Weapon.RIFLES;
        root.setVisible(rifles);
        if (!rifles) {
            return;
        }
        final Vec3 point = Arms.sight(avatar.body(), world, avatar.arms().pitch());
        final Vec3 toEye = eye.subtract(point);
        final double distance = toEye.magnitude();
        at.setX(point.x());
        at.setY(point.y());
        at.setZ(point.z());
        // turn its face (+z) toward the camera: round, then up or down (y down: up is -y)
        yaw.setAngle(Math.toDegrees(Math.atan2(toEye.x(), toEye.z())));
        pitch.setAngle(Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -toEye.y() / distance)))));
        final double scale = distance * SCALE;
        size.setX(scale);
        size.setY(scale);
        size.setZ(scale);
    }

    private static Box box(final double w, final double h, final double d,
                           final double x, final double y, final double z, final PhongMaterial material) {
        final Box box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }
}
