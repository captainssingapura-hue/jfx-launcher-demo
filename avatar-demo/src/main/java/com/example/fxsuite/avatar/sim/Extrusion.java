package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * An outline laid on its side and drawn along: a section across, its {@code profile} (each point
 * across to the right, then up), drawn along a line through ({@code x}, {@code z}) heading
 * {@code yaw}, {@code half} each way. A gable roof is a triangle drawn along its ridge; a coping, a
 * vault, a bank. Its profile convex, either way round; and through it, if it has any, passages
 * ({@code holes}: each convex, inside its profile, apart from the others), drawn along all its
 * length: a gateway's arch, a tunnel.
 *
 * <p>Met as a block is, its own way: between a body's feet and its head, what of it there is across
 * is a stretch of its profile (or two, either side of a passage the body fits up through), and along,
 * its length; so it stops a body as boxes that wide and that long. Where its profile slopes, what is
 * under a body's feet rises as a ramp's top does; in a passage, its floor is under it and its soffit
 * over it.
 */
public record Extrusion(double x, double z, double yaw, double half, List<Vec2> profile, List<List<Vec2>> holes)
        implements Volume {

    public Extrusion {
        profile = List.copyOf(profile);
        holes = holes.stream().map(List::copyOf).toList();
        if (profile.size() < 3 || Math.abs(Outline.twiceArea(profile)) < 1e-6 || !(half > 0)) {
            throw new IllegalArgumentException("not an extrusion: " + profile.size() + " points, " + half + " each way");
        }
        for (final List<Vec2> hole : holes) {
            if (hole.size() < 3 || Math.abs(Outline.twiceArea(hole)) < 1e-6) {
                throw new IllegalArgumentException("not a passage: " + hole);
            }
        }
    }

    /** Solid all through: no passage. */
    public Extrusion(final double x, final double z, final double yaw, final double half, final List<Vec2> profile) {
        this(x, z, yaw, half, profile, List.of());
    }

    /** Across, from its line: to its right. */
    private double across(final double px, final double pz) {
        final Vec3 right = Num.right(yaw);
        return (px - x) * right.x() + (pz - z) * right.z();
    }

    /** Along its line, from its middle. */
    private double along(final double px, final double pz) {
        final Vec3 ahead = Num.heading(yaw);
        return (px - x) * ahead.x() + (pz - z) * ahead.z();
    }

    @Override
    public double low() {
        return profile.stream().mapToDouble(Vec2::z).min().orElseThrow();
    }

    @Override
    public double high() {
        return profile.stream().mapToDouble(Vec2::z).max().orElseThrow();
    }

    /** Outline {@code points}, across from {@code from} to {@code to}. */
    private static List<Vec2> strip(final List<Vec2> points, final double from, final double to) {
        return Outline.clip(Outline.clip(points, -1, 0, -from), 1, 0, to);
    }

    /** Outline {@code points}, from {@code low} to {@code high} up. */
    private static List<Vec2> level(final List<Vec2> points, final double low, final double high) {
        return Outline.clip(Outline.clip(points, 0, -1, -low), 0, 1, high);
    }

    private static boolean some(final List<Vec2> points) {
        return points.size() >= 3 && Math.abs(Outline.twiceArea(points)) > 1e-9;
    }

    /** How far across convex outline {@code points} reaches at height {@code up}: from, to; null if not there. */
    private static double[] reach(final List<Vec2> points, final double up) {
        final List<Vec2> thin = level(points, up - 1e-6, up + 1e-6);
        if (thin.isEmpty()) {
            return null;
        }
        return new double[] {thin.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                thin.stream().mapToDouble(Vec2::x).max().orElseThrow()};
    }

    /** How low and how high convex outline {@code points} reaches at {@code across}: null if not there. */
    private static double[] span(final List<Vec2> points, final double across) {
        final List<Vec2> thin = strip(points, across - 1e-6, across + 1e-6);
        if (thin.isEmpty()) {
            return null;
        }
        return new double[] {thin.stream().mapToDouble(Vec2::z).min().orElseThrow(),
                thin.stream().mapToDouble(Vec2::z).max().orElseThrow()};
    }

    /**
     * Its profile from {@code feet} to {@code head}, across: the stretches of it there, each from,
     * to; none if none of it is there. A passage parts it where a body that tall fits up through it.
     */
    public List<double[]> bands(final double feet, final double head) {
        final List<Vec2> part = level(profile, feet, head);
        if (!some(part)) {
            return List.of();
        }
        List<double[]> stretches = List.of(new double[] {part.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                part.stream().mapToDouble(Vec2::x).max().orElseThrow()});
        for (final List<Vec2> hole : holes) {
            // where the passage is open all the way from feet to head: convex, so where it is open at both
            final double[] low = reach(hole, feet);
            final double[] high = reach(hole, head);
            if (low == null || high == null) {
                continue;
            }
            final double from = Math.max(low[0], high[0]);
            final double to = Math.min(low[1], high[1]);
            if (from >= to) {
                continue;
            }
            final List<double[]> left = new ArrayList<>();
            for (final double[] s : stretches) {
                if (s[0] < from) {
                    left.add(new double[] {s[0], Math.min(s[1], from)});
                }
                if (s[1] > to) {
                    left.add(new double[] {Math.max(s[0], to), s[1]});
                }
            }
            stretches = left;
        }
        return stretches;
    }

    /** The stretch of it from {@code feet} to {@code head}, as a box, if it is in one piece there: null if none, or parted. */
    public Block box(final double feet, final double head) {
        final List<double[]> stretches = bands(feet, head);
        return stretches.size() == 1 ? box(stretches.getFirst(), feet, head) : null;
    }

    private Block box(final double[] across, final double feet, final double head) {
        final double middle = (across[0] + across[1]) / 2;
        final Vec3 right = Num.right(yaw);
        return new Block(x + right.x() * middle, z + right.z() * middle, Math.max(1e-6, (across[1] - across[0]) / 2), half,
                yaw, feet, head);
    }

    @Override
    public boolean covers(final double px, final double pz, final double radius) {
        if (Math.abs(along(px, pz)) >= half + radius) {
            return false;
        }
        final double v = across(px, pz);
        return some(strip(profile, v - radius, v + radius));
    }

    /**
     * Its highest top no higher than {@code below} under a circle: its own top over the circle's
     * stretch across; or, in a passage, the passage's floor, where the circle stands within it.
     */
    @Override
    public double topAt(final double px, final double pz, final double radius, final double below) {
        if (Math.abs(along(px, pz)) >= half + radius) {
            return Double.NEGATIVE_INFINITY;
        }
        final double v = across(px, pz);
        final List<Vec2> under = strip(profile, v - radius, v + radius);
        if (!some(under)) {
            return Double.NEGATIVE_INFINITY;
        }
        double best = Double.NEGATIVE_INFINITY;
        final double top = under.stream().mapToDouble(Vec2::z).max().orElseThrow();
        if (top <= below) {
            best = top;
        }
        final double bottom = under.stream().mapToDouble(Vec2::z).min().orElseThrow();
        for (final List<Vec2> hole : holes) {
            // its floor under the circle: highest at one side or the other, the floor being convex
            final double floor = Math.max(edge(hole, v - radius, true), edge(hole, v + radius, true));
            if (floor > bottom + 1e-6 && floor <= below && floor > best) {
                best = floor;
            }
        }
        return best;
    }

    /**
     * Its lowest underside no lower than {@code above} over a circle: its own bottom; or, in a
     * passage, the passage's soffit, lowest at one side or the other of the circle.
     */
    @Override
    public double undersideAt(final double px, final double pz, final double radius, final double above) {
        if (Math.abs(along(px, pz)) >= half + radius) {
            return Double.POSITIVE_INFINITY;
        }
        final double v = across(px, pz);
        final List<Vec2> over = strip(profile, v - radius, v + radius);
        if (!some(over)) {
            return Double.POSITIVE_INFINITY;
        }
        double best = Double.POSITIVE_INFINITY;
        final double bottom = over.stream().mapToDouble(Vec2::z).min().orElseThrow();
        if (bottom >= above) {
            best = bottom;
        }
        for (final List<Vec2> hole : holes) {
            final double soffit = Math.min(edge(hole, v - radius, false), edge(hole, v + radius, false));
            if (soffit >= above && soffit < best) {
                best = soffit;
            }
        }
        return best;
    }

    /**
     * How high passage {@code hole}'s floor ({@code floor}) or soffit is at {@code across}, the
     * nearest of it if that is beyond it: minus infinity (or infinity) if it is not there at all.
     */
    private static double edge(final List<Vec2> hole, final double across, final boolean floor) {
        final double from = hole.stream().mapToDouble(Vec2::x).min().orElseThrow();
        final double to = hole.stream().mapToDouble(Vec2::x).max().orElseThrow();
        if (across <= from || across >= to) {
            return floor ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        final double[] up = span(hole, across);
        if (up == null) {
            return floor ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        return floor ? up[0] : up[1];
    }

    @Override
    public Vec2 push(final double px, final double pz, final double radius, final double feet, final double head) {
        Vec2 deepest = null;
        for (final double[] stretch : bands(feet, head)) {
            final Vec2 push = box(stretch, feet, head).push(px, pz, radius);
            if (push != null && (deepest == null || push.magnitude() > deepest.magnitude())) {
                deepest = push;
            }
        }
        return deepest;
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        Vec2 deepest = null;
        for (final double[] stretch : bands(feet, head)) {
            final Vec2 push = box(stretch, feet, head).push(ax, az, bx, bz, radius);
            if (push != null && (deepest == null || push.magnitude() > deepest.magnitude())) {
                deepest = push;
            }
        }
        return deepest;
    }

    /** A ray's way through a convex solid: when it goes in, when it comes out, and the way out of each. */
    record Span(double enter, double exit, Vec3 in, Vec3 out) {
    }

    /**
     * Where a ray goes into and out of a convex outline {@code points} drawn along this one's line:
     * {@code ends}, between its two ends, or (a passage) all along. Null if it misses.
     */
    private Span span(final List<Vec2> points, final boolean ends, final Vec3 from, final Vec3 direction) {
        final Vec3 right = Num.right(yaw);
        final Vec3 ahead = Num.heading(yaw);
        // in its own frame: along, across, up (up is -y)
        final double u0 = (from.x() - x) * ahead.x() + (from.z() - z) * ahead.z();
        final double v0 = (from.x() - x) * right.x() + (from.z() - z) * right.z();
        final double y0 = -from.y();
        final double du = direction.x() * ahead.x() + direction.z() * ahead.z();
        final double dv = direction.x() * right.x() + direction.z() * right.z();
        final double dy = -direction.y();
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        Vec3 in = null;
        Vec3 out = null;
        if (ends) {
            for (final double end : new double[] {-1, 1}) {
                // outside where end * u > half
                final double g = end * u0 - half;
                final double rate = end * du;
                if (Math.abs(rate) < 1e-12) {
                    if (g > 0) {
                        return null;
                    }
                    continue;
                }
                final double t = -g / rate;
                if (rate < 0) {
                    if (t > enter) {
                        enter = t;
                        in = ahead.multiply(end);
                    }
                } else if (t < exit) {
                    exit = t;
                    out = ahead.multiply(end);
                }
            }
        }
        final double turn = Math.signum(Outline.twiceArea(points));
        for (int i = 0; i < points.size(); i++) {
            final Vec2 a = points.get(i);
            final Vec2 b = points.get((i + 1) % points.size());
            // out of the outline: across nv, up ny
            final double nv = (b.z() - a.z()) * turn;
            final double ny = -(b.x() - a.x()) * turn;
            final double g = nv * (v0 - a.x()) + ny * (y0 - a.z());
            final double rate = nv * dv + ny * dy;
            if (Math.abs(rate) < 1e-12) {
                if (g > 0) {
                    return null;
                }
                continue;
            }
            final double t = -g / rate;
            final double length = Math.hypot(nv, ny);
            final Vec3 normal = right.multiply(nv / length).add(new Vec3(0, -ny / length, 0));
            if (rate < 0) {
                if (t > enter) {
                    enter = t;
                    in = normal;
                }
            } else if (t < exit) {
                exit = t;
                out = normal;
            }
        }
        if (enter > exit) {
            return null;
        }
        return new Span(enter, exit, in, out);
    }

    /**
     * Where a ray first meets its solid, and its way out of there: in through its outside, unless
     * that is into a passage, then on to where it comes out of the passage, if that is still in it.
     * Null if it misses.
     */
    Span span(final Vec3 from, final Vec3 direction) {
        final Span whole = span(profile, true, from, direction);
        if (whole == null || whole.in() == null) {
            return null;
        }
        double at = whole.enter();
        Vec3 normal = whole.in();
        // starting within its outline: from where it starts, which must be in a passage, or it meets nothing
        if (at < 0) {
            at = 0;
            normal = null;
        }
        boolean moved = true;
        while (moved) {
            moved = false;
            for (final List<Vec2> hole : holes) {
                final Span passage = span(hole, false, from, direction);
                if (passage != null && passage.enter() <= at + 1e-9 && passage.exit() > at + 1e-9) {
                    // in the passage there: through it, to where it comes out into the solid; never
                    // out of it (straight down it), it meets nothing of the solid at all
                    if (passage.out() == null) {
                        return null;
                    }
                    at = passage.exit();
                    normal = passage.out().multiply(-1);
                    moved = true;
                }
            }
        }
        if (at > whole.exit() || normal == null) {
            return null;
        }
        return new Span(at, whole.exit(), normal, whole.out());
    }

    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final Span span = span(from, direction);
        if (span == null || span.enter() < 0 || span.enter() > reach) {
            return null;
        }
        return new Hit(from.add(direction.multiply(span.enter())), span.in(), Surface.BLOCK, span.enter());
    }

    /** Whether any stretch of it between the block's bottom and its top, as a box, overlaps it. */
    @Override
    public boolean overlaps(final Block block) {
        final double feet = Math.max(low(), block.bottom());
        final double head = Math.min(high(), block.top());
        if (feet >= head) {
            return false;
        }
        return bands(feet, head).stream().anyMatch(stretch -> box(stretch, feet, head).overlaps(block));
    }
}
