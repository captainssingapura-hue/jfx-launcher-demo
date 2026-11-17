package com.example.fxsuite.avatar.air;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How a body's lines are styled: a few broad strokes laid over a base plan of them, so the same
 * plan can be drawn lean or muscular, and the same strokes style any body (an aircraft's, a car's).
 * Its plan's longerons, keel up round to spine, are taken as a hull's are: a keel, a chine, its widest
 * line (its beltline), a shoulder, a rail (where its cabin, its canopy, stands on it), a spine.
 *
 * @param width      how much wider than its plan, a share (1 as drawn)
 * @param height     how much taller, a share
 * @param haunch     how far its flanks swell out over its haunch ({@code haunchAt} of the way from its
 *                   tail to its nose), a share of its length: a muscle car's hips over its rear wheels
 * @param haunchAt   where its haunch is, 0 its tail, 1 its nose
 * @param tumblehome how far its cabin is drawn in over its beltline, a share of its width there
 * @param crease     how far its beltline stands out from its flanks, a share of its length: sharper, more
 * @param bluntness  how much broader its nose is kept, toward its point, a share
 */
public record BodyStyle(double width, double height, double haunch, double haunchAt, double tumblehome, double crease,
                        double bluntness) {

    /** As its plan is drawn. */
    public static final BodyStyle PLAIN = new BodyStyle(1, 1, 0, 0.56, 0, 0, 0);

    /** The controls it is made by, each its name, least, as drawn, and most; for a bench's sliders. */
    public record Control(String name, double least, double usual, double most) {
    }

    public static final List<Control> CONTROLS = List.of(
            new Control("width", 0.7, 1, 1.5),
            new Control("height", 0.7, 1, 1.4),
            new Control("haunch", -0.02, 0, 0.04),
            new Control("haunch at", 0.2, 0.56, 0.8),
            new Control("tumblehome", 0, 0, 0.45),
            new Control("crease", 0, 0, 0.02),
            new Control("bluntness", 0, 0, 0.6));

    /** As {@code values} say (by their controls' names), the rest as drawn. */
    public static BodyStyle of(final Map<String, Double> values) {
        return new BodyStyle(values.getOrDefault("width", 1.0), values.getOrDefault("height", 1.0),
                values.getOrDefault("haunch", 0.0), values.getOrDefault("haunch at", 0.56),
                values.getOrDefault("tumblehome", 0.0), values.getOrDefault("crease", 0.0),
                values.getOrDefault("bluntness", 0.0));
    }

    public BodyStyle {
        if (!(width > 0 && height > 0) || tumblehome < 0 || tumblehome >= 1 || bluntness < 0) {
            throw new IllegalArgumentException("a body styled " + width + " wide, " + height + " high, drawn in "
                    + tumblehome);
        }
    }

    /**
     * {@code plan}'s lines, styled, {@code length} long: each row {t, then across and up where each of
     * the six longerons (keel, chine, beltline, shoulder, rail, spine) crosses its section, and, if 1 after
     * them, its section sharp ({@link Lines.Section#sharp})}, t from its tail (0) to its nose (1), all
     * shares of its length; a section all at one point its end.
     */
    public Lines lines(final String name, final List<String> longerons, final double[][] plan, final double length,
                       final int steps) {
        if (longerons.size() != 6) {
            throw new IllegalArgumentException(name + ": a styled body has six longerons, not " + longerons.size());
        }
        final List<Lines.Section> sections = new ArrayList<>();
        for (final double[] row : plan) {
            final double t = row[0];
            final List<double[]> points = new ArrayList<>();
            // an end drawn to a point stays one point, however it is styled: only raised as it is
            boolean point = true;
            for (int i = 1; i < longerons.size(); i++) {
                point &= row[1 + 2 * i] == row[1] && row[2 + 2 * i] == row[2];
            }
            if (point) {
                for (int i = 0; i < longerons.size(); i++) {
                    points.add(new double[] {row[1] * width * length, row[2] * height * length});
                }
                sections.add(new Lines.Section((t - 0.5) * length, points, row.length > 13 && row[13] != 0));
                continue;
            }
            // how much its beltline stands out here: not at its ends, which close to a point
            final double body = bump(t, 0.6, 0.45);
            for (int i = 0; i < longerons.size(); i++) {
                double across = row[1 + 2 * i] * width;
                final double up = row[2 + 2 * i] * height;
                if (i == 1 || i == 2) {
                    // its chine and its beltline swell over its haunch
                    across += haunch * bump(t, haunchAt, 0.14) * (i == 2 ? 1 : 0.7);
                }
                if (i == 2) {
                    across += crease * body;
                }
                if (i == 3 || i == 4) {
                    // its shoulder, and its cabin's rail, drawn in over its beltline
                    across *= 1 - tumblehome * (i == 4 ? 1 : 0.5);
                }
                // toward its nose, kept broad
                across *= 1 + bluntness * bump(t, 0.95, 0.12);
                points.add(new double[] {across * length, up * length});
            }
            sections.add(new Lines.Section((t - 0.5) * length, points, row.length > 13 && row[13] != 0));
        }
        return new Lines(name, longerons, sections, true, steps);
    }

    /** A smooth bump: 1 at {@code at}, down to 0 {@code half} either side of it. */
    private static double bump(final double t, final double at, final double half) {
        final double d = Math.abs(t - at) / half;
        return d >= 1 ? 0 : 0.5 * (1 + Math.cos(Math.PI * d));
    }
}
