package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Frame;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * A {@link Frame}, drawn: each rod a tube from node to node, as thick as its own tube; and, if
 * asked, its nodes: a weld bead where rods are welded, a clamp where it is bolted to what carries
 * it, a boss where it pivots. In its owner's frame, standing on the ground: across to the right is
 * x, up is -y (y points down), along ahead is z.
 */
public final class FrameModel {

    private FrameModel() {
    }

    /** Its rods in {@code steel}; its nodes too, if {@code nodes}, node {@code picked} in {@code marked} (-1 for none). */
    public static Group of(final Frame frame, final PhongMaterial steel, final boolean nodes, final int picked,
                           final PhongMaterial marked) {
        final Group rods = new Group();
        for (final Frame.Rod rod : frame.rods()) {
            rods.getChildren().add(tube(point(frame.nodes().get(rod.from())), point(frame.nodes().get(rod.to())),
                    frame.tube(rod).radius(), steel));
        }
        final Group joints = new Group();
        if (nodes) {
            final PhongMaterial weld = BoxRig.matte("#8a8f96");
            final PhongMaterial clamp = BoxRig.matte("#2b2e33");
            final PhongMaterial boss = BoxRig.matte("#b7bdc4");
            for (int n = 0; n < frame.nodes().size(); n++) {
                final Frame.Node node = frame.nodes().get(n);
                final double r = thickest(frame, n);
                final javafx.scene.shape.Shape3D shape = switch (node.kind()) {
                    case MOUNT -> new Box(r * 3.4, r * 3.4, r * 3.4);
                    case HINGE -> {
                        final Cylinder pivot = new Cylinder(r * 1.5, r * 3);
                        pivot.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
                        yield pivot;
                    }
                    case WELD -> new Sphere(r * 1.35);
                };
                shape.setMaterial(n == picked ? marked : switch (node.kind()) {
                    case MOUNT -> clamp;
                    case HINGE -> boss;
                    case WELD -> weld;
                });
                place(shape, point(node));
                joints.getChildren().add(shape);
            }
        }
        return new Group(rods, joints);
    }

    /** As {@link #of}, its rods only: as the game draws a frame. */
    public static Group of(final Frame frame, final PhongMaterial steel) {
        return of(frame, steel, false, -1, steel);
    }

    /** The thickest rod's radius at node {@code node}: how big its weld or clamp is; the frame's tube's if none. */
    private static double thickest(final Frame frame, final int node) {
        return frame.rodsAt(node).stream().mapToDouble(r -> frame.tube(frame.rods().get(r)).radius()).max()
                .orElse(frame.tube().radius());
    }

    /** Where {@code node} is, in the model's terms. */
    public static Point3D point(final Frame.Node node) {
        return new Point3D(node.across(), -node.up(), node.along());
    }

    /** A tube of radius {@code r} from {@code a} to {@code b}. */
    public static Cylinder tube(final Point3D a, final Point3D b, final double r, final PhongMaterial material) {
        final Point3D run = b.subtract(a);
        final Cylinder tube = new Cylinder(r, run.magnitude());
        tube.setMaterial(material);
        // a cylinder stands along y: turned to lie along the run, about the axis square to both
        final Point3D up = Rotate.Y_AXIS;
        final Point3D axis = up.crossProduct(run);
        final Point3D middle = a.midpoint(b);
        tube.setTranslateX(middle.getX());
        tube.setTranslateY(middle.getY());
        tube.setTranslateZ(middle.getZ());
        if (axis.magnitude() > 1e-9) {
            tube.getTransforms().add(new Rotate(up.angle(run), axis));
        }
        return tube;
    }

    static void place(final Node node, final Point3D at) {
        node.setTranslateX(at.getX());
        node.setTranslateY(at.getY());
        node.setTranslateZ(at.getZ());
    }
}
