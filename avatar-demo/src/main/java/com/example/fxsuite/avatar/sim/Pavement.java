package com.example.fxsuite.avatar.sim;

/**
 * A pavement: a road's surface as a thing of its own ({@link Volume}), whatever it lies on, the ground or a deck. Solid
 * from its underside, its formation, up to its top, wherever it is laid ({@link Laid}); stood on, driven on, shot at.
 * Not run into: its edges are buried in the ground graded up to its top beside it, or are kerbs lower than a step, so
 * it pushes nothing aside ({@link #push}).
 *
 * <p>Belonging to no piece: no one breaks it, and what hits it hits the ground ({@link Course.Layout#ways}).
 */
public record Pavement(Laid laid) implements Volume {

    /** Where a pavement is laid, and how high: what a road says of its own. */
    public interface Laid {

        /** How high its top is at ({@code x}, {@code z}); not a number off it. */
        double top(double x, double z);

        /** How high its underside is at ({@code x}, {@code z}); not a number off it. */
        double underside(double x, double z);

        /** Its box, seen from above. */
        Bounds bounds();

        /** How low and how high any of it is. */
        double low();

        double high();
    }

    /** How far apart a circle's edge is felt for, round it: eight ways, and its middle. */
    private static final int ROUND = 8;
    /** How far a ray steps, looking for where it meets the top; how finely it then finds it. */
    private static final double RAY_STEP = 25;
    private static final int RAY_HALVINGS = 12;

    @Override
    public Bounds bounds() {
        return laid.bounds();
    }

    @Override
    public double low() {
        return laid.low();
    }

    @Override
    public double high() {
        return laid.high();
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        for (int k = 0; k <= ROUND; k++) {
            if (!Double.isNaN(laid.top(x + dx(k, radius), z + dz(k, radius)))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        double best = Double.NEGATIVE_INFINITY;
        for (int k = 0; k <= ROUND; k++) {
            final double top = laid.top(x + dx(k, radius), z + dz(k, radius));
            if (!Double.isNaN(top) && top <= below && top > best) {
                best = top;
            }
        }
        return best;
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        double best = Double.POSITIVE_INFINITY;
        for (int k = 0; k <= ROUND; k++) {
            final double under = laid.underside(x + dx(k, radius), z + dz(k, radius));
            if (!Double.isNaN(under) && under >= above && under < best) {
                best = under;
            }
        }
        return best;
    }

    /** Never: its edges are buried, or lower than a step. */
    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        return null;
    }

    /** Never: its edges are buried, or lower than a step. */
    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        return null;
    }

    /** Where the ray first goes under its top, within its reach: found stepping along, then halving the step. */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        double was = 0;
        double s = 0;
        while (s < reach) {
            s = Math.min(reach, s + RAY_STEP);
            if (under(from, direction, s)) {
                double lo = was;
                double hi = s;
                for (int k = 0; k < RAY_HALVINGS; k++) {
                    final double mid = (lo + hi) / 2;
                    if (under(from, direction, mid)) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                final Vec3 at = from.add(direction.multiply(hi));
                return new Hit(at, new Vec3(0, -1, 0), Surface.GROUND, hi);
            }
            was = s;
        }
        return null;
    }

    /** Whether the point {@code s} along the ray is in it: under its top, over its underside. */
    private boolean under(final Vec3 from, final Vec3 direction, final double s) {
        final Vec3 p = from.add(direction.multiply(s));
        final double top = laid.top(p.x(), p.z());
        return !Double.isNaN(top) && -p.y() < top && -p.y() > laid.underside(p.x(), p.z());
    }

    /** Whether any of {@code block}, felt for at its middle, its corners and its sides' middles, is in it. */
    @Override
    public boolean overlaps(final Block block) {
        final double c = Math.cos(Math.toRadians(block.yaw()));
        final double s = Math.sin(Math.toRadians(block.yaw()));
        final double low = Math.min(block.bottom(), block.highest());
        final double high = Math.max(block.top(), block.highest());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                final double across = i * block.halfWidth();
                final double along = j * block.halfDepth();
                final double x = block.x() + c * across + s * along;
                final double z = block.z() - s * across + c * along;
                final double top = laid.top(x, z);
                if (!Double.isNaN(top) && low < top && high > laid.underside(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double dx(final int k, final double radius) {
        return k == ROUND ? 0 : radius * Math.cos(2 * Math.PI * k / ROUND);
    }

    private static double dz(final int k, final double radius) {
        return k == ROUND ? 0 : radius * Math.sin(2 * Math.PI * k / ROUND);
    }
}
