package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
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

/**
 * What is left when something breaks ({@link Event.Broke}): its pieces, thrown out from where
 * the last blow struck, tumbling, bouncing and coming to rest on whatever is under them, then
 * after a while sinking away. A drone goes up in a fireball, and throws out glowing embers
 * with its parts. Only a sight: the pieces are in nobody's way.
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
    private final Sphere fireball = new Sphere(1, 16);
    private final PhongMaterial ember = BoxRig.glow("#ffb347");
    private final PhongMaterial[] flames = {flame("#ffc23a"), flame("#ff8a1e"), flame("#e8571a"), flame("#9c3416")};
    private int next;
    private int count;
    private double blasted = Double.POSITIVE_INFINITY;
    private double lastTime = Double.NaN;

    public Wreckage() {
        for (int i = 0; i < POOL; i++) {
            chunks[i] = new Chunk();
            root.getChildren().add(chunks[i].box);
        }
        fireball.setVisible(false);
        root.getChildren().add(fireball);
    }

    public Node node() {
        return root;
    }

    /**
     * Throws out the pieces of whatever broke this frame, in the colours {@code world} drew it,
     * and moves on what is already flying, onto what is under it in {@code standing}.
     */
    public void show(final Avatar avatar, final World world, final Obstacles standing) {
        final double time = avatar.body().time();
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, Math.min(0.1, time - lastTime));
        lastTime = time;
        for (final Event event : avatar.events()) {
            if (event instanceof Event.Broke broke) {
                burst(broke, world.material(broke.piece()));
            }
        }
        for (final Chunk chunk : chunks) {
            chunk.fly(seconds, standing);
        }
        blasted += seconds;
        if (blasted < FIREBALL) {
            // swelling, and dimming from yellow through orange to a dull red as it goes
            final double share = blasted / FIREBALL;
            final double radius = 20 + 100 * Math.sqrt(share);
            fireball.setScaleX(radius);
            fireball.setScaleY(radius * 0.85);
            fireball.setScaleZ(radius);
            fireball.setMaterial(flames[Math.min(flames.length - 1, (int) (share * flames.length))]);
            fireball.setVisible(true);
        } else {
            fireball.setVisible(false);
        }
    }

    private void burst(final Event.Broke broke, final PhongMaterial material) {
        final Block block = broke.block();
        final double width = block.halfWidth() * 2;
        final double depth = block.halfDepth() * 2;
        final double tall = block.top() - block.bottom();
        final boolean drone = broke.kind() == Course.Kind.DRONE;
        final double volume = width * depth * tall;
        final int pieces = drone ? 10 : (int) Num.clamp(8 + volume / 600_000, 10, 26);
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
            final double size = Num.clamp(cut * (0.45 + 0.5 * unit()), 6, 60);
            chunk().throwFrom(at, fly, size * (0.6 + 0.8 * unit()), size * (0.5 + 0.6 * unit()),
                    size * (0.6 + 0.8 * unit()), material, unit());
        }
        if (drone) {
            for (int i = 0; i < 12; i++) {
                final Vec3 fly = new Vec3((unit() - 0.5) * 1400, -(300 + 700 * unit()), (unit() - 0.5) * 1400);
                chunk().throwFrom(broke.middle(), fly, 3, 3, 3, ember, unit()).burnsOut(0.5 + 0.6 * unit());
            }
            fireball.setTranslateX(broke.middle().x());
            fireball.setTranslateY(broke.middle().y());
            fireball.setTranslateZ(broke.middle().z());
            blasted = 0;
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
        final long mixed = (count * 0x9E3779B97F4A7C15L + 0x51ED27L) * 0x2545F4914F6CDD1DL;
        return (mixed >>> 11) * 0x1.0p-53;
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
