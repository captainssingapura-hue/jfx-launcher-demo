package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

/**
 * The world drawn: a ground with a metre grid, and each block of the {@link Course} as a box,
 * coloured by what it is. The course is also what blocks the avatar, so what you see is what
 * stops you.
 *
 * <p>JavaFX 3D has no shadows, so the avatar gets a blob: a dark disc on whatever is under it,
 * the ground or a block's top, that shrinks as it climbs. It is enough to tell how high
 * something is, and where it will land.
 */
public final class World {

    public static final Color SKY = Color.web("#9fb8c9");
    private static final double GROUND = 18000;
    private static final double GRID = 500;
    /** The disc lies this far above whatever it is on. */
    private static final double SHADOW_LIFT = 1.8;

    private final Group root = new Group();
    private final Cylinder shadow = new Cylinder(46, 1, 24);

    public World(final Course course) {
        final Box ground = new Box(GROUND * 2, 2, GROUND * 2);
        ground.setTranslateY(1);
        ground.setMaterial(BoxRig.matte("#66775c"));
        root.getChildren().add(ground);

        final PhongMaterial line = BoxRig.matte("#56664d");
        for (double at = -GROUND; at <= GROUND; at += GRID) {
            root.getChildren().add(flat(new Box(4, 1, GROUND * 2), at, 0, line));
            root.getChildren().add(flat(new Box(GROUND * 2, 1, 4), 0, at, line));
        }

        final PhongMaterial[] stone = materials(false, "#8d8577", "#a29a8b", "#7b8590", "#9a8f9e");
        final PhongMaterial[] crate = materials(false, "#b38b5d", "#a07a4e", "#c79f6b");
        final PhongMaterial[] lit = materials(true, "#ffd166", "#7df9ff", "#ff8fab");
        for (final Course.Piece piece : course.pieces()) {
            final PhongMaterial[] kind = switch (piece.kind()) {
                case PILLAR -> stone;
                case CRATE -> crate;
                case GATE -> lit;
            };
            root.getChildren().add(box(piece.block(), kind[piece.tint() % kind.length]));
        }

        shadow.setMaterial(BoxRig.matte("#45533e"));
        root.getChildren().add(shadow);

        final AmbientLight ambient = new AmbientLight(Color.web("#8b939c"));
        final DirectionalLight sun = new DirectionalLight(Color.web("#a8a49a"));
        sun.setDirection(new Point3D(0.35, 1, 0.45));
        root.getChildren().addAll(ambient, sun);
    }

    public Node node() {
        return root;
    }

    /** Moves the blob shadow onto whatever is under the body, smaller the higher it is above it. */
    public void shadowUnder(final Body body, final Obstacles obstacles) {
        final double x = body.position().x();
        final double z = body.position().z();
        final double floor = obstacles.floor(x, z, Body.FOOTING, body.height() + 1e-6);
        shadow.setTranslateX(x);
        shadow.setTranslateY(-floor - SHADOW_LIFT);
        shadow.setTranslateZ(z);
        final double scale = Math.max(0.25, 1 / (1 + (body.height() - floor) / 500));
        shadow.setScaleX(scale);
        shadow.setScaleZ(scale);
    }

    /** A block as a box: centred on its footprint, between its bottom and top, turned with it. */
    private static Box box(final Block block, final PhongMaterial material) {
        final Box box = new Box(block.halfWidth() * 2, block.top() - block.bottom(), block.halfDepth() * 2);
        box.setTranslateX(block.x());
        box.setTranslateY(-(block.top() + block.bottom()) / 2);
        box.setTranslateZ(block.z());
        box.getTransforms().add(new Rotate(block.yaw(), Rotate.Y_AXIS));
        box.setMaterial(material);
        return box;
    }

    private static PhongMaterial[] materials(final boolean glowing, final String... colours) {
        final PhongMaterial[] materials = new PhongMaterial[colours.length];
        for (int i = 0; i < colours.length; i++) {
            materials[i] = glowing ? BoxRig.glow(colours[i]) : BoxRig.matte(colours[i]);
        }
        return materials;
    }

    private static Box flat(final Box box, final double x, final double z, final PhongMaterial material) {
        box.setTranslateX(x);
        box.setTranslateY(-0.6);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }
}
