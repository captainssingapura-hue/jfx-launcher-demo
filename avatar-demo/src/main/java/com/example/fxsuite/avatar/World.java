package com.example.fxsuite.avatar;

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

import java.util.Random;

/**
 * Somewhere to move around: a ground with a metre grid, pillars of different heights to walk
 * between and fly over, and glowing gates up in the air to fly through. The same every run.
 *
 * <p>JavaFX 3D has no shadows, so the avatar gets a blob: a dark disc on the ground under it
 * that shrinks as it climbs. It is enough to tell how high something is.
 */
final class World {

    static final Color SKY = Color.web("#9fb8c9");
    private static final double GROUND = 18000;
    private static final double GRID = 500;

    private final Group root = new Group();
    private final Cylinder shadow = new Cylinder(46, 1, 24);

    World() {
        final Box ground = new Box(GROUND * 2, 2, GROUND * 2);
        ground.setTranslateY(1);
        ground.setMaterial(BoxRig.matte("#66775c"));
        root.getChildren().add(ground);

        final PhongMaterial line = BoxRig.matte("#56664d");
        for (double at = -GROUND; at <= GROUND; at += GRID) {
            root.getChildren().add(flat(new Box(4, 1, GROUND * 2), at, 0, line));
            root.getChildren().add(flat(new Box(GROUND * 2, 1, 4), 0, at, line));
        }

        final Random random = new Random(7);
        final String[] stone = {"#8d8577", "#a29a8b", "#7b8590", "#9a8f9e"};
        for (int i = 0; i < 170; i++) {
            final double x = (random.nextDouble() * 2 - 1) * (Body.EDGE - 400);
            final double z = (random.nextDouble() * 2 - 1) * (Body.EDGE - 400);
            if (Math.hypot(x, z) < 900) {
                continue;  // keep the start clear
            }
            final double size = 120 + random.nextDouble() * 200;
            final double height = 150 + Math.pow(random.nextDouble(), 2) * 2400;
            final Box pillar = new Box(size, height, size);
            pillar.setTranslateX(x);
            pillar.setTranslateY(-height / 2);
            pillar.setTranslateZ(z);
            pillar.setMaterial(BoxRig.matte(stone[random.nextInt(stone.length)]));
            root.getChildren().add(pillar);
        }

        final String[] lit = {"#ffd166", "#7df9ff", "#ff8fab"};
        for (int i = 0; i < 14; i++) {
            final double angle = i * 2 * Math.PI / 14 + 0.4;
            final double reach = 1800 + random.nextDouble() * 8000;
            root.getChildren().add(gate(Math.sin(angle) * reach, 500 + random.nextDouble() * 1500,
                    Math.cos(angle) * reach, random.nextDouble() * 180, BoxRig.glow(lit[i % lit.length])));
        }

        shadow.setMaterial(BoxRig.matte("#45533e"));
        shadow.setTranslateY(-1.8);
        root.getChildren().add(shadow);

        final AmbientLight ambient = new AmbientLight(Color.web("#8b939c"));
        final DirectionalLight sun = new DirectionalLight(Color.web("#a8a49a"));
        sun.setDirection(new Point3D(0.35, 1, 0.45));
        root.getChildren().addAll(ambient, sun);
    }

    Node node() {
        return root;
    }

    /** Moves the blob shadow under the body, smaller the higher it is. */
    void shadowUnder(final Body body) {
        shadow.setTranslateX(body.position().getX());
        shadow.setTranslateZ(body.position().getZ());
        final double scale = Math.max(0.25, 1 / (1 + body.height() / 500));
        shadow.setScaleX(scale);
        shadow.setScaleZ(scale);
    }

    /** A square frame standing up in the air, to fly through. */
    private static Group gate(final double x, final double height, final double z, final double yaw,
                              final PhongMaterial material) {
        final double span = 600;
        final double bar = 36;
        final Group gate = new Group(
                at(new Box(span + bar, bar, bar), 0, -span / 2, material),
                at(new Box(span + bar, bar, bar), 0, span / 2, material),
                at(new Box(bar, span, bar), -span / 2, 0, material),
                at(new Box(bar, span, bar), span / 2, 0, material));
        gate.setTranslateX(x);
        gate.setTranslateY(-height);
        gate.setTranslateZ(z);
        gate.getTransforms().add(new Rotate(yaw, Rotate.Y_AXIS));
        return gate;
    }

    private static Box at(final Box box, final double x, final double y, final PhongMaterial material) {
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setMaterial(material);
        return box;
    }

    private static Box flat(final Box box, final double x, final double z, final PhongMaterial material) {
        box.setTranslateX(x);
        box.setTranslateY(-0.6);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }
}
