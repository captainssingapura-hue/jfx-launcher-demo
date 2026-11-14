package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Fruit;
import com.example.fxsuite.avatar.sim.Journal;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.List;

/**
 * What the weapons do to the world, as seen: a tracer along each round's flight, sparks where a
 * round or the blade strikes a block, a puff of dust off the ground, and marks left behind, a
 * bullet hole or a gash, which fade after a while. A mark is part of what it is on: put in that
 * piece's own group, it goes with an airship as it flies, and is gone when the piece breaks. All
 * from the simulation's events; nothing here changes the world.
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
    /** How far apart two bullet holes must be, and how many of the latest are looked through. */
    private static final double HOLE_APART = 4;
    private static final int RECENT = 12;

    private final Group root = new Group();
    private final Pool tracers;
    private final Pool sparks;
    /** Juice, off fruit struck: a watermelon's red, a pineapple's gold. */
    private final Pool juice;
    private final Pool nectar;
    private final Pool dust;
    private final Pool holes;
    private final Pool gashes;
    private double lastTime = Double.NaN;
    private int count;
    private int gatlingRounds;
    /**
     * The world as drawn: a mark on a piece is put in that piece's own group ({@link World#group}),
     * so it is part of the piece, going where it goes, and gone when it is broken.
     */
    private final World world;

    /** The effects of the weapons on {@code world}. */
    public Impacts(final World world) {
        this.world = world;
        final PhongMaterial light = BoxRig.glow("#fff1a8");
        final PhongMaterial spark = BoxRig.glow("#ffb347");
        final PhongMaterial earth = BoxRig.matte("#7a6a4f");
        final PhongMaterial hole = BoxRig.matte("#1b1c1f");
        tracers = new Pool(16, () -> new Box(1.2, 1, 1.2), light);
        sparks = new Pool(120, () -> new Box(3, 3, 3), spark);
        juice = new Pool(90, () -> new Box(3.5, 3.5, 3.5), BoxRig.matte("#d8283a"));
        nectar = new Pool(40, () -> new Box(3, 3, 3), BoxRig.matte("#f0c52e"));
        dust = new Pool(60, () -> new Box(8, 8, 8), earth);
        holes = new Pool(300, () -> new Box(8, 0.5, 8), hole);
        gashes = new Pool(30, () -> new Box(26, 0.6, 4.5), hole);
    }

    public Node node() {
        return root;
    }

    /**
     * Adds what this frame's {@code entries} made, and moves on what is already flying or fading.
     * Each is stamped with its step, so a mark on what moves goes where that was when it was struck.
     */
    public void show(final Avatar avatar, final List<Journal.Logged> entries) {
        final double time = avatar.body().time();
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, Math.min(0.1, time - lastTime));
        lastTime = time;
        for (final Journal.Logged entry : entries) {
            final Event event = entry.event();
            if (event instanceof Event.Fired fired) {
                // a Gatling gun's belt carries a tracer every third round, as real belts do: forty a
                // second would be one solid beam; the heavy one's, every other
                final int every = switch (fired.by()) {
                    case GATLING -> 3;
                    case HEAVY_GATLING, GIANT_GATLING, RAPID_GATLING -> 2;
                    default -> 1;
                };
                if (every == 1 || ++gatlingRounds % every == 0) {
                    tracer(fired.from(), fired.to());
                }
            } else if (event instanceof Event.Struck struck && struck.by() != Weapon.BOMB && struck.by() != Weapon.FALL
                    && struck.by() != Weapon.CRASH) {
                // a bomb, or a fall, leaves no mark of its own on what it reaches: what comes of it is the wreckage's
                struck(struck, avatar.body().yaw(), entry.step());
            } else if (event instanceof Event.Broke broke) {
                // what was marked is gone, and its marks with it
                holes.clear(broke.piece());
                gashes.clear(broke.piece());
            }
        }
        for (final Pool pool : new Pool[] {tracers, sparks, juice, nectar, dust, holes, gashes}) {
            pool.age(seconds);
        }
    }

    private void tracer(final Vec3 from, final Vec3 to) {
        final Vec3 along = to.subtract(from);
        final Particle tracer = tracers.next(TRACER);
        ((Box) tracer.shape).setHeight(along.magnitude());
        tracer.place(from.add(along.multiply(0.5)), Vec3.ZERO, along, false);
    }

    /** What a round or the blade does where it struck, in step {@code step}, the avatar facing {@code yaw}. */
    private void struck(final Event.Struck struck, final double yaw, final long step) {
        final boolean blade = struck.by() == Weapon.GLAIVE;
        final Vec3 off = struck.at().add(struck.normal().multiply(0.4));
        if (Person.is(struck.piece())) {
            // someone hit: a puff off their clothes, and no mark left on them
            for (int i = 0; i < 4; i++) {
                dust.next(DUST * 0.6).place(off, struck.normal().multiply(90).add(scatter(80)), null, true);
            }
            return;
        }
        if (struck.piece() >= 0 && world.kind(struck.piece()) == Course.Kind.FRUIT) {
            // fruit struck splashes juice, not sparks
            final Pool splash = world.fruit(struck.piece()) == Fruit.PINEAPPLE ? nectar : juice;
            for (int i = 0; i < (blade ? 18 : 8); i++) {
                final Vec3 fly = struck.normal().multiply(blade ? 260 : 200).add(scatter(blade ? 420 : 260))
                        .add(0, -120, 0);
                splash.next(SPARK * 2 * (0.6 + 0.4 * unit())).place(off, fly, null, true);
            }
        } else if (struck.on() == Surface.BLOCK) {
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
        // the mark is part of what it is on: in that piece's own frame, as the piece was when struck;
        // on the ground, in the world's
        final int piece = struck.piece();
        final Group on;
        final Vec3 at;
        final Vec3 facing;
        final double turned;
        if (piece >= 0) {
            final Block block = world.at(piece, step);
            on = world.group(piece);
            at = block.toLocal(off);
            facing = block.turnToLocal(struck.normal());
            turned = block.yaw();
        } else {
            on = root;
            at = off;
            facing = struck.normal();
            turned = 0;
        }
        if (blade) {
            if (struck.on() == Surface.BLOCK) {
                // a gash across the face, turned with the avatar
                gashes.next(MARK).mark(on, at, facing, yaw - turned, piece);
            }
        } else {
            // a steady stream lands in one place: one hole there, not one on top of another, which would
            // lie in the same plane and flicker
            if (!holes.anyNear(at, HOLE_APART, piece)) {
                holes.next(MARK).mark(on, at, facing, count * 47.0, piece);
            }
        }
    }

    /** -0.5 to 0.5 of {@code size} each way, by count, so a run looks the same each time. */
    private Vec3 scatter(final double size) {
        return new Vec3((unit() - 0.5) * size, (unit() - 0.5) * size, (unit() - 0.5) * size);
    }

    private double unit() {
        count++;
        return Num.hash(count);
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

        /**
         * Whether one of the few last handed out is showing on piece {@code piece} (-1, the ground)
         * within {@code apart} of {@code at}, in that piece's own frame.
         */
        boolean anyNear(final Vec3 at, final double apart, final int piece) {
            for (int back = 1; back <= Math.min(RECENT, items.length); back++) {
                final Particle recent = items[(next - back + items.length) % items.length];
                if (recent.shape.isVisible() && recent.piece == piece && recent.at.distance(at) < apart) {
                    return true;
                }
            }
            return false;
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

        /** Takes away every mark on piece {@code piece}. */
        void clear(final int piece) {
            for (final Particle particle : items) {
                if (particle.piece == piece) {
                    particle.shape.setVisible(false);
                    particle.piece = -1;
                }
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
        /** The piece of the course it is marked on; -1 for none (the ground, or not a mark). */
        private int piece = -1;

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

        /**
         * Flat on a surface facing {@code normal}, turned {@code spin} degrees about it, on piece
         * {@code on} (-1, the ground): put in {@code group}, that piece's own, and {@code where} and
         * {@code normal} in its frame.
         */
        void mark(final Group group, final Vec3 where, final Vec3 normal, final double spin, final int on) {
            if (shape.getParent() != group) {
                ((Group) shape.getParent()).getChildren().remove(shape);
                group.getChildren().add(shape);
            }
            at = where;
            velocity = Vec3.ZERO;
            falls = false;
            piece = on;
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
