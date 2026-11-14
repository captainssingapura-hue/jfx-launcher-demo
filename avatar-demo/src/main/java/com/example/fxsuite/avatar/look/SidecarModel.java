package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Sidecar;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * A {@link Sidecar}, drawn: its frame ({@link FrameModel}), a weld at each node and a clamp where it
 * is bolted to the motorcycle, and its wheel on its stub axle. In the motorcycle's frame, standing on
 * the ground, on its side, right or left (as {@link Sidecar#placed} has it). Drawn again whenever it
 * changes ({@link #show}), as the vehicle bench shapes it.
 */
public final class SidecarModel {

    private final Group root = new Group();
    private final Group frame = new Group();
    private final Group wheel = new Group();
    private final PhongMaterial steel;
    private final PhongMaterial marked = BoxRig.glow("#ffb347");
    private final PhongMaterial tyre = BoxRig.matte("#1b1d20");
    private final PhongMaterial hub = BoxRig.matte("#a2a9b0");
    private boolean tubes = true;
    private boolean joints = true;

    public SidecarModel() {
        steel = BoxRig.matte("#3d4249");
        steel.setSpecularColor(Color.web("#c9cfd6"));
        steel.setSpecularPower(24);
        root.getChildren().addAll(frame, wheel);
    }

    public Node node() {
        return root;
    }

    /** Whether its tubes, its welds and clamps, and its wheel are seen. */
    public void showing(final boolean tubes, final boolean joints, final boolean wheeled) {
        this.tubes = tubes;
        this.joints = joints;
        seen();
        wheel.setVisible(wheeled);
    }

    private void seen() {
        if (frame.getChildren().isEmpty()) {
            return;
        }
        final Group drawn = (Group) frame.getChildren().getFirst();
        drawn.getChildren().get(0).setVisible(tubes);
        drawn.getChildren().get(1).setVisible(joints);
    }

    /** Drawn as {@code sidecar} now is, node {@code picked} marked (-1 for none). */
    public void show(final Sidecar sidecar, final int picked) {
        frame.getChildren().setAll(FrameModel.of(sidecar.placed(), steel, true, picked, marked));
        seen();
        // the wheel, upright, its axle across, on the stub axle's end
        final Point3D centre = FrameModel.point(sidecar.placed(sidecar.hub()));
        final Cylinder rubber = new Cylinder(sidecar.radius(), sidecar.width());
        rubber.setMaterial(tyre);
        rubber.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        final Cylinder boss = new Cylinder(sidecar.radius() * 0.4, sidecar.width() + 2);
        boss.setMaterial(hub);
        boss.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        final Group turning = new Group(rubber, boss);
        FrameModel.place(turning, centre);
        wheel.getChildren().setAll(turning);
        if (picked == sidecar.frame().nodes().size()) {
            final Sphere mark = new Sphere(sidecar.frame().tube().radius() * 1.6);
            mark.setMaterial(marked);
            FrameModel.place(mark, centre);
            wheel.getChildren().add(mark);
        }
    }
}
