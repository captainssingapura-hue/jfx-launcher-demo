package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Sidecar;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * A {@link Sidecar}'s chassis, drawn: each tube a steel cylinder from joint to joint, a weld at
 * each joint, a clamp where it is bolted to the motorcycle, and its wheel on its stub axle. In the
 * motorcycle's frame, standing on the ground, on its side, right or left (as {@link Sidecar#placed}
 * has it): across to the right is x, {@code up} is -y (y points down), {@code along} ahead is z.
 * Drawn again whenever the frame changes ({@link #show}), as the vehicle bench shapes it.
 */
public final class SidecarModel {

    private final Group root = new Group();
    private final Group tubes = new Group();
    private final Group welds = new Group();
    private final Group wheel = new Group();
    private final PhongMaterial steel;
    private final PhongMaterial weld = BoxRig.matte("#8a8f96");
    private final PhongMaterial clamp = BoxRig.matte("#2b2e33");
    private final PhongMaterial marked = BoxRig.glow("#ffb347");
    private final PhongMaterial tyre = BoxRig.matte("#1b1d20");
    private final PhongMaterial hub = BoxRig.matte("#a2a9b0");

    public SidecarModel() {
        steel = BoxRig.matte("#3d4249");
        steel.setSpecularColor(Color.web("#c9cfd6"));
        steel.setSpecularPower(24);
        root.getChildren().addAll(tubes, welds, wheel);
    }

    public Node node() {
        return root;
    }

    /** Whether its tubes, its welds and clamps, and its wheel are seen. */
    public void showing(final boolean frame, final boolean joints, final boolean wheeled) {
        tubes.setVisible(frame);
        welds.setVisible(joints);
        wheel.setVisible(wheeled);
    }

    /** Drawn as {@code sidecar} now is, joint {@code picked} marked (-1 for none). */
    public void show(final Sidecar sidecar, final int picked) {
        tubes.getChildren().clear();
        welds.getChildren().clear();
        wheel.getChildren().clear();
        // on its side of the motorcycle, right or left
        final java.util.List<Sidecar.Joint> placed = sidecar.placed();
        for (final Sidecar.Bar bar : sidecar.bars()) {
            tubes.getChildren().add(tube(point(placed.get(bar.from())), point(placed.get(bar.to())), sidecar.tube(),
                    steel));
        }
        for (int j = 0; j < placed.size(); j++) {
            final Sidecar.Joint joint = placed.get(j);
            final Point3D at = point(joint);
            if (joint.mount()) {
                // a clamp round the motorcycle's frame, bolted through
                final Box held = new Box(sidecar.tube() * 3.4, sidecar.tube() * 3.4, sidecar.tube() * 3.4);
                held.setMaterial(j == picked ? marked : clamp);
                place(held, at);
                welds.getChildren().add(held);
            } else {
                final Sphere bead = new Sphere(sidecar.tube() * 1.35);
                bead.setMaterial(j == picked ? marked : weld);
                place(bead, at);
                welds.getChildren().add(bead);
            }
        }
        // the wheel, upright, its axle across, on the stub axle's end
        final Point3D centre = point(sidecar.placed(sidecar.hub()));
        final Cylinder rubber = new Cylinder(sidecar.radius(), sidecar.width());
        rubber.setMaterial(tyre);
        rubber.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        final Cylinder boss = new Cylinder(sidecar.radius() * 0.4, sidecar.width() + 2);
        boss.setMaterial(hub);
        boss.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        final Group turning = new Group(rubber, boss);
        place(turning, centre);
        wheel.getChildren().add(turning);
        if (picked == sidecar.joints().size()) {
            final Sphere mark = new Sphere(sidecar.tube() * 1.6);
            mark.setMaterial(marked);
            place(mark, centre);
            wheel.getChildren().add(mark);
        }
    }

    /** Where {@code joint} is, in the model's terms. */
    static Point3D point(final Sidecar.Joint joint) {
        return new Point3D(joint.out(), -joint.up(), joint.along());
    }

    /** A tube of radius {@code r} from {@code a} to {@code b}. */
    static Cylinder tube(final Point3D a, final Point3D b, final double r, final PhongMaterial material) {
        final Point3D run = b.subtract(a);
        final Cylinder tube = new Cylinder(r, run.magnitude());
        tube.setMaterial(material);
        // a cylinder stands along y: turned to lie along the run, about the axis square to both
        final Point3D up = Rotate.Y_AXIS;
        final Point3D axis = up.crossProduct(run);
        final double angle = up.angle(run);
        final Point3D middle = a.midpoint(b);
        tube.setTranslateX(middle.getX());
        tube.setTranslateY(middle.getY());
        tube.setTranslateZ(middle.getZ());
        if (axis.magnitude() > 1e-9) {
            tube.getTransforms().add(new Rotate(angle, axis));
        } else if (run.getY() < 0) {
            tube.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        }
        return tube;
    }

    private static void place(final Node node, final Point3D at) {
        node.setTranslateX(at.getX());
        node.setTranslateY(at.getY());
        node.setTranslateZ(at.getZ());
    }
}
