package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * A river's channel cut into the land ({@link Land.Layer}): along its middle {@code line}, a bed
 * {@code bed} wide, level, {@code depth} below the ground beside it; each side, a bank rising from the
 * bed to the ground over {@code bank}, easing out of the one and into the other, so there is no edge
 * at its foot or its top to trip on. Worked out exactly from the line wherever it is asked
 * ({@link Polyline}). Its bed and banks bare earth, the grass thinning out over their tops.
 */
public final class Channel implements Land.Layer {

    /** How far down from the top of its banks the grass thins out to bare earth. */
    static final double FRINGE = 150;

    private final Polyline line;
    private final double bed;
    private final double bank;
    private final double depth;

    public Channel(final List<Vec2> line, final double bed, final double bank, final double depth) {
        if (!(bed >= 0) || !(bank > 0) || !(depth > 0)) {
            throw new IllegalArgumentException("a channel's banks go some way across, it some depth");
        }
        this.line = new Polyline(line, bed / 2 + bank);
        this.bed = bed;
        this.bank = bank;
        this.depth = depth;
    }

    /** How far from its middle line it reaches: half its bed, and a bank. */
    public double reach() {
        return line.reach();
    }

    public List<Vec2> line() {
        return line.points();
    }

    public double depth() {
        return depth;
    }

    @Override
    public double shape(final double x, final double z, final double under) {
        return under - cut(distance(x, z));
    }

    /** How far below the ground beside it the channel is, {@code away} from its middle line. */
    public double cut(final double away) {
        if (away >= reach()) {
            return 0;
        }
        if (away <= bed / 2) {
            return depth;
        }
        final double t = (away - bed / 2) / bank;
        return depth * (1 - t * t * (3 - 2 * t));
    }

    /**
     * How far from its middle line its banks come down to {@code below} under the ground beside it: where
     * water that deep over its bed meets them.
     */
    public double across(final double below) {
        if (below >= depth) {
            return bed / 2;
        }
        double low = bed / 2;
        double high = reach();
        for (int k = 0; k < 40; k++) {
            final double mid = (low + high) / 2;
            if (cut(mid) > below) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) / 2;
    }

    /** How far ({@code x}, {@code z}) is from its middle line; infinity if beyond its reach. */
    public double distance(final double x, final double z) {
        return line.distance(x, z);
    }

    @Override
    public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
        return line.touches(minX, minZ, maxX, maxZ);
    }

    @Override
    public double deepest() {
        return depth;
    }

    /** Its bed and its banks, bare earth: no grass grows where the water runs, or comes up to. */
    @Override
    public double cover(final Land.Cover cover, final double x, final double z) {
        if (cover != Land.Cover.EARTH) {
            return 0;
        }
        final double t = (reach() - distance(x, z)) / FRINGE;
        return t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
    }
}
