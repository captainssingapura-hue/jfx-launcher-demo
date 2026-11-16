package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * How much of a building is drawn, by how far off it is seen: what it is shaped by always; what
 * dresses it nearer; the finest of it nearest. Far off, what lies a few centimetres proud of a face
 * is smaller than a pixel, and too close to the face for the depth it is told apart by: drawn, it
 * flickers through it; left out, nothing is missed, and there is less to draw.
 *
 * <ul>
 *   <li>{@link Level#BODY}: what gives it its shape: its walls, its base, its structure, its roofs,
 *       its spire, its glass (in a tower, its face) — drawn however far off;</li>
 *   <li>{@link Level#DETAIL}: what dresses it, and its frames, each no thinner than
 *       {@link #THIN}: sills, bands, copings, fins, balconies, doors — drawn within {@link #DETAIL_FAR};</li>
 *   <li>{@link Level#FINE}: what is thinner than that, or no bigger anyway than {@link #SMALL}:
 *       mullions, glazing bars, lattices, trims, fittings — drawn within {@link #FINE_FAR}.</li>
 * </ul>
 *
 * <p>Gone, each comes back a tenth nearer than it went ({@link #shown}), so one seen from just that far
 * does not come and go.
 */
public final class Detail {

    private Detail() {
    }

    /** How much of a building a solid is. */
    public enum Level {
        BODY, DETAIL, FINE
    }

    /** Thinner than this, what dresses a building is fine; anything no bigger than {@link #SMALL} every way, too. */
    public static final double THIN = 10;
    public static final double SMALL = 60;
    /** How far off what dresses a building is drawn, and the finest of it. */
    public static final double DETAIL_FAR = 80_000;
    public static final double FINE_FAR = 15_000;
    /** Gone, each comes back this much nearer than it went. */
    public static final double BACK = 0.9;

    /** Which level {@code solid} is: by what it does, and how thin and how big it is. */
    public static Level of(final Solid solid) {
        final double[] size = size(solid);
        final double least = Math.min(size[0], Math.min(size[1], size[2]));
        final double most = Math.max(size[0], Math.max(size[1], size[2]));
        if (most <= SMALL) {
            return Level.FINE;
        }
        return switch (solid.role()) {
            case WALL, BASE, STRUCTURE, ROOF, SPIRE, GLAZING -> Level.BODY;
            case DRESSING, FRAME -> least < THIN ? Level.FINE : Level.DETAIL;
        };
    }

    /** {@code solids}, each in its level, in order. */
    public static Map<Level, List<Solid>> split(final List<Solid> solids) {
        final Map<Level, List<Solid>> levels = new EnumMap<>(Level.class);
        for (final Level level : Level.values()) {
            levels.put(level, new ArrayList<>());
        }
        for (final Solid solid : solids) {
            levels.get(of(solid)).add(solid);
        }
        return levels;
    }

    /**
     * Whether {@code level} is drawn seen from {@code away}, it having been ({@code was}) or not: within
     * how far off it is drawn; gone, only once that much nearer again ({@link #BACK}).
     */
    public static boolean shown(final Level level, final double away, final boolean was) {
        final double far = switch (level) {
            case BODY -> Double.POSITIVE_INFINITY;
            case DETAIL -> DETAIL_FAR;
            case FINE -> FINE_FAR;
        };
        return away < (was ? far : far * BACK);
    }

    /** How long {@code solid}'s three edges from its first corner are: across its first, second and third ways. */
    private static double[] size(final Solid solid) {
        final List<Point> c = solid.corners();
        return new double[] {length(c.get(0), c.get(1)), length(c.get(0), c.get(2)), length(c.get(0), c.get(4))};
    }

    private static double length(final Point a, final Point b) {
        return Math.sqrt((a.across() - b.across()) * (a.across() - b.across())
                + (a.along() - b.along()) * (a.along() - b.along()) + (a.up() - b.up()) * (a.up() - b.up()));
    }
}
