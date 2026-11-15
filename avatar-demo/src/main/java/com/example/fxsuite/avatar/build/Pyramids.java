package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Crossing;
import com.example.fxsuite.avatar.sim.Extrusion;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;

/**
 * Pyramids, to fly round and land on, each in the way as it is shaped, not as a box round it:
 *
 * <ul>
 *   <li>the <b>great pyramid</b>, 60 m square and 40 m high, its faces smooth and too steep to walk
 *       up (53°): two triangles, each drawn along across the other, crossed ({@link Crossing});</li>
 *   <li>a <b>low pyramid</b>, 24 m square, turned 30°, its faces gentle enough (25°) to walk up to
 *       its point: crossed the same way, turned;</li>
 *   <li>a <b>stepped pyramid</b>, five tiers of 3 m, each a square stood up ({@link Prism}), a shrine
 *       on its top, and a stairway up its front to it, a ramp ({@link Block}): what is shaped closely
 *       and what is a box, in one piece.</li>
 * </ul>
 *
 * <p>Each is one piece in the world ({@link Course.Kind#BUILDING}), broken as a whole.
 */
public final class Pyramids {

    private Pyramids() {
    }

    /** Their keys, to draw them by, start here: past the temple's wall's. */
    public static final int KEYS = 300;

    /** The great pyramid: half its side, its height, where it is. */
    public static final double GREAT_HALF = 3000;
    public static final double GREAT_HIGH = 4000;
    public static final Place GREAT = new Place(0, 9000, 0, 0);
    /** The low one: half its side, its faces' slope, where it is and how it is turned. */
    public static final double LOW_HALF = 1200;
    public static final double LOW_SLOPE = 25;
    public static final double LOW_HIGH = LOW_HALF * Math.tan(Math.toRadians(LOW_SLOPE));
    public static final Place LOW = new Place(6000, 4500, 0, 30);
    /** The stepped one: its tiers, how high each, half the side of each; where it is, its front to the start. */
    public static final double TIER = 300;
    public static final double[] TIERS = {2200, 1850, 1500, 1150, 800};
    public static final double TOP = TIER * TIERS.length;
    /** Its stairway: so wide, so steep (degrees); its shrine, half its side and how high. */
    public static final double STAIRS = 600;
    public static final double STAIR_SLOPE = 30;
    public static final double SHRINE = 400;
    public static final double SHRINE_HIGH = 400;
    public static final Place STEPPED = new Place(-7500, 5500, 0, 180);

    /** What stands there: the great pyramid, the low one, the stepped one. */
    public static List<Course.Piece> pieces() {
        return List.of(great(), low(), stepped());
    }

    /** Pyramid {@code k}'s solids, about its middle, as drawn. */
    public static List<Solid> solids(final int k) {
        return switch (k) {
            case 0 -> List.of(point("great pyramid", GREAT_HALF, GREAT_HIGH, Material.LIMESTONE));
            case 1 -> List.of(point("low pyramid", LOW_HALF, LOW_HIGH, Material.SANDSTONE));
            default -> steppedSolids();
        };
    }

    private static Course.Piece great() {
        return smooth(GREAT, GREAT_HALF, GREAT_HIGH, 0);
    }

    private static Course.Piece low() {
        return smooth(LOW, LOW_HALF, LOW_HIGH, 1);
    }

    /** A smooth pyramid at {@code at}, {@code half} its side each way, {@code high}: two triangles crossed. */
    private static Course.Piece smooth(final Place at, final double half, final double high, final int k) {
        final List<Vec2> triangle = List.of(new Vec2(-half, 0), new Vec2(half, 0), new Vec2(0, high));
        final Volume shape = new Crossing(new Extrusion(at.x(), at.z(), at.yaw(), half, triangle),
                new Extrusion(at.x(), at.z(), at.yaw() + 90, half, triangle));
        final Block box = new Block(at.x(), at.z(), half, half, at.yaw(), 0, high);
        return Course.Piece.shaped(box, Course.Kind.BUILDING, KEYS + k, List.of(), List.of(shape), at);
    }

    /** The stepped pyramid: its tiers and its shrine stood up as squares; its stairway a ramp up its front. */
    private static Course.Piece stepped() {
        final List<Volume> volumes = new ArrayList<>();
        for (int t = 0; t < TIERS.length; t++) {
            volumes.add(square(STEPPED, TIERS[t], TIERS[t], t * TIER, (t + 1) * TIER));
        }
        volumes.add(square(STEPPED, SHRINE, SHRINE, TOP, TOP + SHRINE_HIGH));
        // the stairway: from its foot on the ground out before the front, up to the top tier's edge
        final double head = TIERS[TIERS.length - 1];
        final double foot = head + TOP / Math.tan(Math.toRadians(STAIR_SLOPE));
        final Vec3 from = world(STEPPED, 0, foot);
        final Vec3 to = world(STEPPED, 0, head);
        final Block stairs = Block.ramp(from.x(), from.z(), 0, to.x(), to.z(), TOP, STAIRS);
        // its box: round its tiers and its stairway, its middle where the pyramid's is
        final Block box = new Block(STEPPED.x(), STEPPED.z(), TIERS[0], foot, STEPPED.yaw(), 0, TOP + SHRINE_HIGH);
        return Course.Piece.shaped(box, Course.Kind.BUILDING, KEYS + 2, List.of(stairs), volumes, STEPPED);
    }

    /** A square stood up at {@code at}, {@code across} and {@code along} each way from its middle, from {@code low} to {@code high}. */
    private static Prism square(final Place at, final double across, final double along, final double low,
                                final double high) {
        final List<Vec2> outline = new ArrayList<>();
        for (final double[] c : new double[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            final Vec3 p = world(at, c[0] * across, c[1] * along);
            outline.add(new Vec2(p.x(), p.z()));
        }
        return new Prism(outline, low, high);
    }

    /** Where {@code across}, {@code along} from {@code at} is in the world (x, 0, z). */
    private static Vec3 world(final Place at, final double across, final double along) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        return new Vec3(at.x() + right.x() * across + ahead.x() * along, 0, at.z() + right.z() * across + ahead.z() * along);
    }

    /** A pyramid drawn: a box whose four top corners are all its point. */
    private static Solid point(final String name, final double half, final double high, final Material stone) {
        final List<Point> corners = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            corners.add((k & 4) == 0 ? new Point((k & 1) == 0 ? -half : half, (k & 2) == 0 ? -half : half, 0)
                    : new Point(0, 0, high));
        }
        return new Solid(name, Role.WALL, stone, corners);
    }

    /** The stepped pyramid drawn: its tiers, its shrine and its roof, its stairway. */
    private static List<Solid> steppedSolids() {
        final List<Solid> solids = new ArrayList<>();
        for (int t = 0; t < TIERS.length; t++) {
            solids.add(box("tier " + t, Material.SANDSTONE, TIERS[t], TIERS[t], t * TIER, (t + 1) * TIER));
        }
        solids.add(box("shrine", Material.LIMESTONE, SHRINE, SHRINE, TOP, TOP + SHRINE_HIGH));
        solids.add(box("shrine roof", Material.SANDSTONE, SHRINE + 60, SHRINE + 60, TOP + SHRINE_HIGH,
                TOP + SHRINE_HIGH + 40));
        // the stairway: a wedge, from its foot up to the top tier's edge
        final double head = TIERS[TIERS.length - 1];
        final double foot = head + TOP / Math.tan(Math.toRadians(STAIR_SLOPE));
        final double w = STAIRS / 2;
        solids.add(new Solid("stairway", Role.BASE, Material.LIMESTONE, List.of(
                new Point(-w, head, 0), new Point(w, head, 0), new Point(-w, foot, 0), new Point(w, foot, 0),
                new Point(-w, head, TOP), new Point(w, head, TOP), new Point(-w, foot, 0), new Point(w, foot, 0))));
        return solids;
    }

    /** A box about the middle, {@code across} and {@code along} each way, from {@code low} to {@code high}. */
    private static Solid box(final String name, final Material stone, final double across, final double along,
                             final double low, final double high) {
        final List<Point> corners = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            corners.add(new Point((k & 1) == 0 ? -across : across, (k & 2) == 0 ? -along : along,
                    (k & 4) == 0 ? low : high));
        }
        return new Solid(name, Role.WALL, stone, corners);
    }
}
