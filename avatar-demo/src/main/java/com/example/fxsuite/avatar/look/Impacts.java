package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

/**
 * What the weapons do to the world, as seen: a tracer along each round's flight, sparks where a
 * round or the blade strikes a block, a puff of dust off the ground, and marks left behind, a
 * bullet hole or a gash, which fade after a while. All from the simulation's events; nothing
 * here changes the world.
 *
 * <p>Every effect comes from a fixed pool of shapes, reused oldest first. Making a shape for each
 * spark of a burst at twenty rounds a second would stutter.
 */
public final class Impacts {

    private static final double TRACER = 0.045;
    private static final double SPARK = 0.35;
    private static final double DUST = 0.6;
    private static final double MARK = 20;
    private static final double GRAVITY = 2600;

    private final Group root = new Group();
    private final Pool tracers;
    private final Pool sparks;
    private final Pool dust;
    private final Pool holes;
    private final Pool gashes;
    private double lastTime = Double.NaN;
    private int count;

    public Impacts() {
        final PhongMaterial light = BoxRig.glow("#fff1a8");
        final PhongMaterial spark = BoxRig.glow("#ffb347");
        final PhongMaterial earth = BoxRig.matte("#7a6a4f");
        final PhongMaterial hole = BoxRig.matte("#1b1c1f");
        tracers = new Pool(16, () -> new Box(1.2, 1, 1.2), light);
        sparks = new Pool(120, () -> new Box(3, 3, 3), spark);
        dust = new Pool(60, () -> new Box(8, 8, 8), earth);
        holes = new Pool(200, () -> new Box(8, 0.5, 8), hole);
        gashes = new Pool(30, () -> new Box(26, 0.6, 4.5), hole);
    }

    public Node node() {
        return root;
    }

    /** Adds what this frame's events made, and moves on what is already flying or fading. */
    public void show(final Avatar avatar) {
        final double time = avatar.body().time();
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, Math.min(0.1, time - lastTime));
        lastTime = time;
        for (final Event event : avatar.events()) {
            if (event instanceof Event.Fired fired) {
                tracer(fired.from(), fired.to());
            } else if (event instanceof Event.Struck struck) {
                struck(struck, avatar.body().yaw());
            }
        }
        for (final Pool pool : new Pool[] {tracers, sparks, dust, holes, gashes}) {
            pool.age(seconds);
        }
    }

    private void tracer(final Vec3 from, final Vec3 to) {
        final Vec3 along = to.subtract(from);
        final Particle tracer = tracers.next(TRACER);
        ((Box) tracer.shape).setHeight(along.magnitude());
        tracer.place(from.add(along.multiply(0.5)), Vec3.ZERO, along, false);
    }

    private void struck(final Event.Struck struck, final double yaw) {
        final boolean blade = struck.by() == Weapon.GLAIVE;
        final Vec3 off = struck.at().add(struck.normal().multiply(0.4));
        if (struck.on() == Surface.BLOCK) {
            for (int i = 0; i < (blade ? 16 : 6); i++) {
                final Vec3 fly = struck.normal().multiply(blade ? 450 : 320).add(scatter(blade ? 500 : 300));
                sparks.next(SPARK * (0.6 + 0.4 * unit())).place(off, fly, null, true);
            }
        } else {
            for (int i = 0; i < (blade ? 10 : 3); i++) {
                final Vec3 fly = new Vec3(0, -(blade ? 260 : 140), 0).add(scatter(blade ? 160 : 70));
                dust.next(DUST * (0.6 + 0.4 * unit())).place(off, fly, null, true);
            }
        }
        if (blade) {
            if (struck.on() == Surface.BLOCK) {
                // a gash across the face, turned with the avatar
                gashes.next(MARK).mark(off, struck.normal(), yaw);
            }
        } else {
            holes.next(MARK).mark(off, struck.normal(), count * 47.0);
        }
    }

    /** -0.5 to 0.5 of {@code size} each way, by count, so a run looks the same each time. */
    private Vec3 scatter(final double size) {
        return new Vec3((unit() - 0.5) * size, (unit() - 0.5) * size, (unit() - 0.5) * size);
    }

    private double unit() {
        count++;
        final long mixed = count * 0x9E3779B97F4A7C15L * 0x2545F4914F6CDD1DL;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    /** A fixed number of one kind of shape, handed out oldest first. */
    private final class Pool {
        private final Particle[] items;
        private int next;

        Pool(final int size, final java.util.function.Supplier<Box> make, final PhongMaterial material) {
            items = new Particle[size];
            for (int i = 0; i < size; i++) {
                final Box shape = make.get();
                shape.setMaterial(material);
                items[i] = new Particle(shape);
                root.getChildren().add(shape);
            }
        }

        Particle next(final double life) {
            final Particle particle = items[next];
            next = (next + 1) % items.length;
            particle.life = life;
            particle.age = 0;
            return particle;
        }

        void age(final double seconds) {
            for (final Particle particle : items) {
                particle.fly(seconds);
            }
        }
    }

    /** One shape: where it is, how it moves, and how long it has. */
    private static final class Particle {
        private final Box shape;
        private final Rotate turn = new Rotate(0, Rotate.X_AXIS);
        private final Rotate spin = new Rotate(0, Rotate.Y_AXIS);
        private Vec3 at = Vec3.ZERO;
        private Vec3 velocity = Vec3.ZERO;
        private boolean falls;
        private double life;
        private double age;

        Particle(final Box shape) {
            this.shape = shape;
            // the spin about its own y axis first, then turned to lie along the direction
            shape.getTransforms().addAll(turn, spin);
            shape.setVisible(false);
        }

        /** At {@code at}, moving at {@code velocity}; its long axis along {@code along}, if given. */
        void place(final Vec3 where, final Vec3 speed, final Vec3 along, final boolean fall) {
            at = where;
            velocity = speed;
            falls = fall;
            align(along == null ? new Vec3(0, -1, 0) : along, 0);
            show();
        }

        /** Flat on a surface facing {@code normal}, turned {@code spin} degrees about it. */
        void mark(final Vec3 where, final Vec3 normal, final double spin) {
            at = where;
            velocity = Vec3.ZERO;
            falls = false;
            align(normal, spin);
            show();
        }

        private void show() {
            shape.setTranslateX(at.x());
            shape.setTranslateY(at.y());
            shape.setTranslateZ(at.z());
            shape.setVisible(true);
        }

        /** Turns the shape so its y axis points along {@code direction}, then about it by {@code spin}. */
        private void align(final Vec3 direction, final double around) {
            final Vec3 d = direction.normalize();
            final Point3D up = new Point3D(0, 1, 0);
            final Point3D to = new Point3D(d.x(), d.y(), d.z());
            final Point3D axis = up.crossProduct(to);
            final double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, up.dotProduct(to)))));
            turn.setAxis(axis.magnitude() < 1e-9 ? Rotate.X_AXIS : axis);
            turn.setAngle(angle);
            spin.setAngle(around);
        }

        void fly(final double seconds) {
            if (!shape.isVisible()) {
                return;
            }
            age += seconds;
            if (age >= life) {
                shape.setVisible(false);
                return;
            }
            if (falls) {
                velocity = velocity.add(0, GRAVITY * 0.35 * seconds, 0);
                at = at.add(velocity.multiply(seconds));
                if (at.y() > -1) {
                    at = new Vec3(at.x(), -1, at.z());
                    velocity = Vec3.ZERO;
                }
                shape.setTranslateX(at.x());
                shape.setTranslateY(at.y());
                shape.setTranslateZ(at.z());
            }
        }
    }
}
