package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>An artificial offset, for drawing only: not the land.</b> The roads' strips ({@link RoadSurface}) lie just over
 * the true surface; the land is drawn in pieces that may be so far out ({@link TerrainModel#TOLERANCE}), and a coarse
 * one's triangle may span a road with no point of it on the road, so passing over the road, as high as the land
 * may be out, and hiding it till the camera comes nearer and the piece finer, when the road appears from the grass.
 * So the land is drawn lowered under and beside every road: by as much as its piece's level may be out and
 * {@link TerrainModel#SUNK} more, out to as far from the road's edge as a triangle crossing it may reach
 * ({@link #BAND} of the biggest square a feature lies in at that level, {@link TerrainModel#FEATURED}), then easing
 * back to the land over as far again. No triangle that crosses a road can then lie above it, at any level.
 *
 * <p>It is what is drawn alone: walked on, driven on, the land is as it is. And it is not how a road is built:
 * that, the courses of a road laid on the earth, would be the road's own ({@code docs/road-model.md}). Here it is
 * the land drawn coarse, made to keep out of the road's way. Near, it is centimetres, out to a few metres from the
 * road; far off, metres, out to hundreds; too far off to be seen either way.
 *
 * <p>The roads' lines, coarsely ({@link #EVERY}), off their bridges, where nothing is drawn under them.
 */
final class RoadBand {

    /**
     * How many of a road's points apart its line is taken here: 10 m, at 2 m apart; off the road by under 30 cm round
     * the tightest bend there is (40 m round), near enough for this.
     */
    static final int EVERY = 5;
    /** How far out from a road's edge, as a share of the biggest square a feature lies in, the land is fully lowered. */
    static final double BAND = Math.sqrt(2);

    /** A stretch of a road's line, from (ax, az) to (bx, bz), so wide either side of it at each end. */
    record Segment(double ax, double az, double bx, double bz, double ha, double hb) {
    }

    private final List<Segment> segments = new ArrayList<>();

    RoadBand(final List<Road> roads) {
        this(roads, List.of());
    }

    /** The same, and the pavements of features that say where theirs run ({@link Land.Paved}). */
    RoadBand(final List<Road> roads, final List<Land.Band> bands) {
        for (final Land.Band b : bands) {
            segments.add(new Segment(b.ax(), b.az(), b.bx(), b.bz(), b.ha(), b.hb()));
        }
        for (final Road road : roads) {
            final List<Vec2> line = road.line();
            final int end = line.size() - 1;
            for (int from = 0; from < end; from += EVERY) {
                final int to = Math.min(end, from + EVERY);
                if (!bridged(road, from, to)) {
                    final Vec2 a = line.get(from);
                    final Vec2 b = line.get(to);
                    segments.add(new Segment(a.x(), a.z(), b.x(), b.z(), road.wide(from) / 2, road.wide(to) / 2));
                }
            }
        }
    }

    private static boolean bridged(final Road road, final int from, final int to) {
        for (int i = from; i <= to; i++) {
            if (road.bridged(i)) {
                return true;
            }
        }
        return false;
    }

    /** Those stretches that come within {@code reach} of their edges of the ground from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}). */
    List<Segment> near(final double x0, final double z0, final double x1, final double z1, final double reach) {
        final List<Segment> near = new ArrayList<>();
        for (final Segment s : segments) {
            final double out = reach + Math.max(s.ha(), s.hb());
            if (Math.max(s.ax(), s.bx()) >= x0 - out && Math.min(s.ax(), s.bx()) <= x1 + out
                    && Math.max(s.az(), s.bz()) >= z0 - out && Math.min(s.az(), s.bz()) <= z1 + out) {
                near.add(s);
            }
        }
        return near;
    }

    /** How far ({@code x}, {@code z}) is outside the nearest road's edge, among {@code near}: less than 0 on it; infinity if none. */
    static double edge(final List<Segment> near, final double x, final double z) {
        double best = Double.POSITIVE_INFINITY;
        for (final Segment s : near) {
            final double dx = s.bx() - s.ax();
            final double dz = s.bz() - s.az();
            final double length = dx * dx + dz * dz;
            final double t = length == 0 ? 0 : Math.clamp(((x - s.ax()) * dx + (z - s.az()) * dz) / length, 0, 1);
            final double d = Math.hypot(x - s.ax() - t * dx, z - s.az() - t * dz) - (s.ha() + (s.hb() - s.ha()) * t);
            best = Math.min(best, d);
        }
        return best;
    }

    /**
     * How far the land is drawn lowered {@code edge} outside a road's edge, in a piece that may be {@code out} out whose
     * biggest square where a feature lies is {@code featured}: all of {@code out} and {@link TerrainModel#SUNK} more,
     * on the road and out to {@link #BAND} of {@code featured}; easing to none at twice that.
     */
    static double lowered(final double edge, final double out, final double featured) {
        final double full = BAND * featured;
        final double t = Math.clamp((edge - full) / full, 0, 1);
        return (out + TerrainModel.SUNK) * (1 - t * t * (3 - 2 * t));
    }
}
