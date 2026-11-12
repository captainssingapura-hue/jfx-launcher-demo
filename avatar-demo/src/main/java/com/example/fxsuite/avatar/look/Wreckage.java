package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.List;

/**
 * What is left when something breaks ({@link Event.Broke}): its pieces, thrown out from where
 * the last blow struck, tumbling, bouncing and coming to rest on whatever is under them, then
 * after a while sinking away. A drone goes up in a fireball, and throws out glowing embers
 * with its parts; an airship goes up in blast after blast along its hull, and rains wreckage
 * from the sky. Only a sight: the pieces are in nobody's way.
 *
 * <p>Every piece comes from a fixed pool, reused oldest first, as {@link Impacts} does.
 */
public final class Wreckage {

    private static final int POOL = 240;
    private static final double GRAVITY = 1800;
    /** Seconds a piece lies before it starts to go, and how long it takes to shrink away. */
    private static final double LIES = 8;
    private static final double GOES = 1.5;
    private static final double FIREBALL = 0.35;

    private final Group root = new Group();
    private final Chunk[] chunks = new Chunk[POOL];
    private final Blast[] blasts = new Blast[8];
    private int nextBlast;
    private final PhongMaterial ember = BoxRig.glow("#ffb347");
    /** Earth thrown up by a bomb going off on the ground. */
    private final PhongMaterial earth = BoxRig.matte("#4f5a43");
    /** What a watermelon is, burst: its flesh, its rind, its seeds. */
    private final PhongMaterial flesh = BoxRig.matte("#e23b4a");
    private final PhongMaterial rind = BoxRig.matte("#2e6a2a");
    private final PhongMaterial seed = BoxRig.matte("#141210");
    private final PhongMaterial[] flames = {flame("#ffc23a"), flame("#ff8a1e"), flame("#e8571a"), flame("#9c3416")};
    private int next;
    private int count;
    private double lastTime = Double.NaN;

    public Wreckage() {
        for (int i = 0; i < POOL; i++) {
            chunks[i] = new Chunk();
            root.getChildren().add(chunks[i].box);
        }
        for (int i = 0; i < blasts.length; i++) {
            blasts[i] = new Blast();
            root.getChildren().add(blasts[i].ball);
        }
    }

    public Node node() {
        return root;
    }

    /**
     * Throws out the pieces of whatever broke this frame, in the colours {@code world} drew it,
     * and moves on what is already flying, onto what is under it in {@code standing}.
     */
    public void show(final Avatar avatar, final List<Event> events, final World world, final Obstacles standing) {
        final double time = avatar.body().time();
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, Math.min(0.1, time - lastTime));
        lastTime = time;
        for (final Event event : events) {
            if (event instanceof Event.Broke broke) {
                burst(broke, world.material(broke.piece()));
            } else if (event instanceof Event.Exploded exploded) {
                explode(exploded);
            }
        }
        for (final Chunk chunk : chunks) {
            chunk.fly(seconds, standing);
        }
        for (final Blast blast : blasts) {
            blast.burn(seconds, flames);
        }
    }

    /**
     * A watermelon bursting: lumps of red flesh and green rind flung out from the blow, black
     * seeds, and a spray of juice that is gone in a moment.
     */
    private void splatter(final Event.Broke broke) {
        final Vec3 middle = broke.middle();
        final Vec3 away = middle.subtract(broke.struck());
        final Vec3 out = away.magnitude() < 1e-6 ? Vec3.ZERO : away.normalize();
        for (int i = 0; i < 26; i++) {
            final boolean skin = i % 3 == 0;
            final Vec3 fly = out.multiply(260 * (0.4 + unit()))
                    .add((unit() - 0.5) * 320, -(220 + 260 * unit()), (unit() - 0.5) * 320);
            final double size = skin ? 6 + 6 * unit() : 5 + 8 * unit();
            chunk().throwFrom(middle, fly, size, skin ? size * 0.35 : size * 0.8, size, skin ? rind : flesh, unit());
        }
        for (int i = 0; i < 10; i++) {
            final Vec3 fly = new Vec3((unit() - 0.5) * 380, -(200 + 200 * unit()), (unit() - 0.5) * 380);
            chunk().throwFrom(middle, fly, 2, 1.2, 1.4, seed, unit());
        }
        for (int i = 0; i < 18; i++) {
            final Vec3 fly = out.multiply(200).add((unit() - 0.5) * 600, -(150 + 350 * unit()), (unit() - 0.5) * 600);
            chunk().throwFrom(middle, fly, 3, 3, 3, flesh, unit()).burnsOut(0.35 + 0.3 * unit());
        }
    }

    private void burst(final Event.Broke broke, final PhongMaterial material) {
        if (broke.kind() == Course.Kind.MELON) {
            splatter(broke);
            return;
        }
        final Block block = broke.block();
        final boolean airship = broke.kind() == Course.Kind.AIRSHIP;
        final Course.Section hull = Course.HULL.get(3);
        // an airship comes apart along its hull, not out to its fins' tips
        final double width = airship ? hull.halfAcross() * 2 : block.halfWidth() * 2;
        final double depth = block.halfDepth() * 2;
        final double tall = airship ? hull.halfUp() * 2 : block.top() - block.bottom();
        final boolean drone = broke.kind() == Course.Kind.DRONE;
        final double volume = width * depth * tall;
        final int pieces = airship ? 48 : drone ? 10 : (int) Num.clamp(8 + volume / 600_000, 10, 26);
        final double biggest = airship ? 130 : 60;
        // about the size the pieces would be, cut that many ways
        final double cut = Math.cbrt(volume / pieces);
        for (int i = 0; i < pieces; i++) {
            // somewhere inside the block, in its own frame, turned with it into the world's
            final Vec3 inside = turned(new Vec3((unit() - 0.5) * width * 0.8, -(unit() - 0.5) * tall * 0.8,
                    (unit() - 0.5) * depth * 0.8), block.yaw());
            final Vec3 at = broke.middle().add(inside);
            // away from the last blow, and up
            final Vec3 away = at.subtract(broke.struck());
            final Vec3 out = away.magnitude() < 1e-6 ? Vec3.ZERO : away.normalize();
            final double speed = drone ? 650 : 380;
            final Vec3 fly = out.multiply(speed * (0.5 + unit()))
                    .add((unit() - 0.5) * 240, -(drone ? 380 : 300) * (0.6 + unit()), (unit() - 0.5) * 240);
            final double size = Num.clamp(cut * (0.45 + 0.5 * unit()), 6, biggest);
            chunk().throwFrom(at, fly, size * (0.6 + 0.8 * unit()), size * (0.5 + 0.6 * unit()),
                    size * (0.6 + 0.8 * unit()), material, unit());
        }
        if (drone) {
            embers(broke.middle(), 12, 1400);
            blast(broke.middle(), 120, 0);
        } else if (airship) {
            // blast after blast along the hull, as it is heard
            final double[] along = {150, -420, 560, -80};
            final double[] after = {0, 0.45, 0.95, 1.6};
            for (int k = 0; k < along.length; k++) {
                final Vec3 at = broke.middle().add(Num.heading(block.yaw()).multiply(along[k]));
                blast(at, 340 - 40 * k, after[k]);
            }
            embers(broke.middle(), 36, 2200);
        }
    }

    /** A bomb going off: a fireball, embers, and, on the ground, clods of earth thrown up. */
    private void explode(final Event.Exploded exploded) {
        final Vec3 at = exploded.at();
        blast(at, 260, 0);
        embers(at, 14, 1600);
        if (exploded.on() == Surface.GROUND) {
            for (int i = 0; i < 12; i++) {
                final Vec3 fly = new Vec3((unit() - 0.5) * 900, -(500 + 700 * unit()), (unit() - 0.5) * 900);
                final double size = 10 + 22 * unit();
                chunk().throwFrom(at.add(0, -size, 0), fly, size, size * 0.7, size, earth, unit());
            }
        }
    }

    /** {@code count} glowing embers flung out from {@code from}, as fast as {@code speed}. */
    private void embers(final Vec3 from, final int count, final double speed) {
        for (int i = 0; i < count; i++) {
            final Vec3 fly = new Vec3((unit() - 0.5) * speed, -(300 + speed / 2 * unit()), (unit() - 0.5) * speed);
            chunk().throwFrom(from, fly, 3 + speed / 700, 3 + speed / 700, 3 + speed / 700, ember, unit())
                    .burnsOut(0.5 + 0.6 * unit());
        }
    }

    /** A fireball at {@code at}, as big as {@code size} across, {@code after} seconds from now. */
    private void blast(final Vec3 at, final double size, final double after) {
        final Blast blast = blasts[nextBlast];
        nextBlast = (nextBlast + 1) % blasts.length;
        blast.light(at, size, after);
    }

    /** A fireball: waiting its turn, then swelling and dimming from yellow through orange to a dull red. */
    private static final class Blast {
        private final Sphere ball = new Sphere(1, 16);
        private double age = Double.POSITIVE_INFINITY;
        private double size;

        Blast() {
            ball.setVisible(false);
        }

        void light(final Vec3 at, final double across, final double after) {
            ball.setTranslateX(at.x());
            ball.setTranslateY(at.y());
            ball.setTranslateZ(at.z());
            size = across;
            age = -after;
        }

        void burn(final double seconds, final PhongMaterial[] flames) {
            age += seconds;
            final double life = FIREBALL * Math.sqrt(size / 120);
            if (age < 0 || age >= life) {
                ball.setVisible(false);
                return;
            }
            final double share = age / life;
            final double radius = size * (0.17 + 0.83 * Math.sqrt(share));
            ball.setScaleX(radius);
            ball.setScaleY(radius * 0.85);
            ball.setScaleZ(radius);
            ball.setMaterial(flames[Math.min(flames.length - 1, (int) (share * flames.length))]);
            ball.setVisible(true);
        }
    }

    private Chunk chunk() {
        final Chunk chunk = chunks[next];
        next = (next + 1) % POOL;
        return chunk;
    }

    /** Lit only from within, so the lights do not wash it out to white. */
    private static PhongMaterial flame(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.BLACK);
        final WritableImage lit = new WritableImage(1, 1);
        lit.getPixelWriter().setColor(0, 0, Color.web(colour));
        material.setSelfIlluminationMap(lit);
        material.setSpecularColor(Color.BLACK);
        return material;
    }

    /** Turned about the vertical as {@code Rotate(yaw, Y_AXIS)} turns it. */
    private static Vec3 turned(final Vec3 v, final double yaw) {
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        return new Vec3(v.x() * c + v.z() * s, v.y(), -v.x() * s + v.z() * c);
    }

    /** 0 to 1, by count, so a run looks the same each time. */
    private double unit() {
        count++;
        return Num.hash(count, 0x51ED27L);
    }

    /** One piece: tumbling through the air, then lying where it came down. */
    private static final class Chunk {
        private final Box box = new Box(1, 1, 1);
        private final Rotate tumble = new Rotate(0, Rotate.X_AXIS);
        private Vec3 at = Vec3.ZERO;
        private Vec3 velocity = Vec3.ZERO;
        private double spin;
        private double age;
        private double life;
        private double half;
        private boolean resting;

        Chunk() {
            box.getTransforms().add(tumble);
            box.setVisible(false);
        }

        Chunk throwFrom(final Vec3 where, final Vec3 fly, final double width, final double height,
                        final double depth, final PhongMaterial material, final double turn) {
            at = where;
            velocity = fly;
            box.setWidth(width);
            box.setHeight(height);
            box.setDepth(depth);
            box.setMaterial(material);
            box.setScaleX(1);
            box.setScaleY(1);
            box.setScaleZ(1);
            half = height / 2;
            tumble.setAxis(new Point3D(turn - 0.5, 0.6, 0.5 - turn * turn).normalize());
            tumble.setAngle(turn * 360);
            spin = 300 + 600 * turn;
            age = 0;
            life = LIES + GOES;
            resting = false;
            box.setVisible(true);
            place();
            return this;
        }

        /** An ember: gone in a moment, without coming to rest. */
        void burnsOut(final double seconds) {
            life = seconds;
        }

        void fly(final double seconds, final Obstacles standing) {
            if (!box.isVisible()) {
                return;
            }
            age += seconds;
            if (age >= life) {
                box.setVisible(false);
                return;
            }
            if (!resting) {
                velocity = velocity.add(0, GRAVITY * seconds, 0);
                at = at.add(velocity.multiply(seconds));
                tumble.setAngle(tumble.getAngle() + spin * seconds);
                // the floor under it: the ground, or the top of what still stands there
                final double floor = standing.floor(at.x(), at.z(), 0, -at.y() + half);
                if (-at.y() - half <= floor && velocity.y() > 0) {
                    at = new Vec3(at.x(), -(floor + half), at.z());
                    if (velocity.y() < 260) {
                        // settled: lying flat, as a block on its face does
                        resting = true;
                        tumble.setAngle(Math.round(tumble.getAngle() / 90) * 90);
                        tumble.setAxis(Rotate.Y_AXIS);
                    } else {
                        velocity = new Vec3(velocity.x() * 0.45, -velocity.y() * 0.35, velocity.z() * 0.45);
                        spin *= 0.5;
                    }
                }
                place();
            }
            final double going = (age - (life - GOES)) / GOES;
            if (going > 0 && life > GOES) {
                final double scale = 1 - going;
                box.setScaleX(scale);
                box.setScaleY(scale);
                box.setScaleZ(scale);
            }
        }

        private void place() {
            box.setTranslateX(at.x());
            box.setTranslateY(at.y());
            box.setTranslateZ(at.z());
        }
    }
}
