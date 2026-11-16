package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Block;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;

/**
 * What stands about in a park, drawn, each in its piece's part (its middle the middle of its box, up
 * the part's -y): a tree, its trunk and its crown; a lamp post and its lamp; a bench, a litter bin, a
 * planter and its shrub. Each simply: a few shapes, few sides, many of them drawn. Each returns what
 * it is worn on: its trunk, its post, its seat, its body.
 */
final class ParkModels {

    private ParkModels() {
    }

    private static final PhongMaterial BARK = BoxRig.matte("#5b4433");
    private static final PhongMaterial[] LEAVES = {BoxRig.matte("#4d7a37"), BoxRig.matte("#3f6b3c")};
    private static final PhongMaterial IRON = BoxRig.matte("#2f3436");
    private static final PhongMaterial SLATS = BoxRig.matte("#8a6440");
    private static final PhongMaterial STONE = BoxRig.matte("#b9b1a2");
    private static final PhongMaterial SHRUB = BoxRig.matte("#557f3d");
    private static final PhongMaterial LIGHT = BoxRig.glow("#fff1c8");

    /** A material of its own, as {@code shared} is: what is worn on darkens alone. */
    private static PhongMaterial own(final PhongMaterial shared) {
        final PhongMaterial own = new PhongMaterial(shared.getDiffuseColor());
        own.setSpecularColor(shared.getSpecularColor());
        own.setSpecularPower(shared.getSpecularPower());
        return own;
    }

    /**
     * A tree on its trunk ({@code block}): of {@code kind} 0, broad, its crown three rounds clustered over the
     * trunk; of kind 1, tall and narrow, one round drawn out upward.
     */
    static Shape3D tree(final Group part, final Block block, final int kind) {
        final double tall = block.top() - block.bottom();
        final Cylinder trunk = new Cylinder(block.halfWidth(), tall, 8);
        trunk.setMaterial(own(BARK));
        part.getChildren().add(trunk);
        final PhongMaterial leaves = LEAVES[kind % LEAVES.length];
        final double top = -tall / 2;
        if (kind % 2 == 0) {
            final double[][] rounds = {{0, 230, 0, 230}, {-110, 160, 60, 170}, {100, 170, -70, 160}};
            for (final double[] r : rounds) {
                final Sphere crown = new Sphere(r[3], 10);
                crown.setTranslateX(r[0]);
                crown.setTranslateY(top - r[1]);
                crown.setTranslateZ(r[2]);
                crown.setMaterial(leaves);
                part.getChildren().add(crown);
            }
        } else {
            final Sphere crown = new Sphere(130, 10);
            crown.setScaleY(2.6);
            crown.setTranslateY(top - 260);
            crown.setMaterial(leaves);
            part.getChildren().add(crown);
        }
        return trunk;
    }

    /** A lamp post ({@code block}): a dark iron post, and a lamp on top, lit. */
    static Shape3D lamp(final Group part, final Block block) {
        final double tall = block.top() - block.bottom();
        final Cylinder post = new Cylinder(block.halfWidth(), tall, 8);
        post.setMaterial(own(IRON));
        final Box head = new Box(34, 46, 34);
        head.setTranslateY(-tall / 2 - 10);
        head.setMaterial(LIGHT);
        final Box cap = new Box(46, 8, 46);
        cap.setTranslateY(-tall / 2 - 36);
        cap.setMaterial(IRON);
        part.getChildren().addAll(post, head, cap);
        return post;
    }

    /** A park bench ({@code block}, its front ahead): slats as high as its seat on iron ends, and its back behind them. */
    static Shape3D bench(final Group part, final Block block) {
        final double tall = block.top() - block.bottom();
        final double wide = block.halfWidth() * 2;
        final double deep = block.halfDepth() * 2;
        final Box seat = new Box(wide, 6, deep);
        seat.setTranslateY(-tall / 2 + 3);
        seat.setMaterial(own(SLATS));
        final Box back = new Box(wide, 40, 5);
        back.setTranslateY(-tall / 2 - 22);
        back.setTranslateZ(-block.halfDepth() + 3);
        back.getTransforms().add(new javafx.scene.transform.Rotate(-12, javafx.scene.transform.Rotate.X_AXIS));
        back.setMaterial(SLATS);
        part.getChildren().addAll(seat, back);
        for (final double end : new double[] {-1, 1}) {
            final Box leg = new Box(6, tall - 6, deep * 0.9);
            leg.setTranslateX(end * (block.halfWidth() - 12));
            leg.setTranslateY(3);
            leg.setMaterial(IRON);
            part.getChildren().add(leg);
        }
        return seat;
    }

    /** A litter bin ({@code block}): a dark round bin, its rim paler. */
    static Shape3D bin(final Group part, final Block block) {
        final double tall = block.top() - block.bottom();
        final Cylinder body = new Cylinder(block.halfWidth(), tall, 12);
        body.setMaterial(own(IRON));
        final Cylinder rim = new Cylinder(block.halfWidth() + 2, 6, 12);
        rim.setTranslateY(-tall / 2 + 3);
        rim.setMaterial(STONE);
        part.getChildren().addAll(body, rim);
        return body;
    }

    /** A stone planter ({@code block}), a shrub growing out of it. */
    static Shape3D planter(final Group part, final Block block) {
        final double tall = block.top() - block.bottom();
        final Box body = new Box(block.halfWidth() * 2, tall, block.halfDepth() * 2);
        body.setMaterial(own(STONE));
        final Sphere shrub = new Sphere(Math.min(block.halfWidth(), block.halfDepth()) * 0.95, 10);
        shrub.setScaleY(0.8);
        shrub.setTranslateY(-tall / 2 - 20);
        shrub.setMaterial(SHRUB);
        part.getChildren().addAll(body, shrub);
        return body;
    }
}
