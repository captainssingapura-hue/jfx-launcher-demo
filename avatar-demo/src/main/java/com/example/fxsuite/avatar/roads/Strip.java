package com.example.fxsuite.avatar.roads;

import java.util.List;

/**
 * A strip of a road's cross-section, running along it: what it is for ({@link Use}), how wide ({@code width}, cm),
 * how much its surface falls going away from the road's line ({@code fall}, rise over run; less than nothing
 * rises), and how far its inner edge steps up from the strip inside it ({@code rise}, cm: a kerb's upstand).
 *
 * <p>Either <b>paved</b>, built of its {@code courses} top down on a formation: the road builds it. Or
 * <b>ground</b>, its surface the ground's own, graded to it and {@code cover}ed: the ground prepared for the road
 * makes it, where the ground carries the road, and nothing does anywhere else.
 */
public record Strip(Use use, double width, List<Course> courses, Cover cover, double fall, double rise) {

    /** What a strip is for. */
    public enum Use {
        /** A traffic lane. */
        LANE,
        /** A hard strip beside the lanes, not to drive in. */
        SHOULDER,
        /** The edge stone between a carriageway and a footway. */
        KERB,
        /** For people on foot, beside a road. */
        FOOTWAY,
        /** Grass beside a road. */
        VERGE,
        /** Between two carriageways. */
        MEDIAN,
        /** A path: for feet and bicycles, on its own. */
        PATH;

        /** As it is written: lower case. */
        public String written() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public Strip {
        if (use == null || !(width > 0) || courses == null || courses.isEmpty() == (cover == null)
                || !Double.isFinite(fall) || !Double.isFinite(rise)) {
            throw new IllegalArgumentException("a strip is for something, some width, and paved or ground: one or the other");
        }
        courses = List.copyOf(courses);
    }

    /** A paved strip, built of {@code courses}. */
    public static Strip paved(final Use use, final double width, final List<Course> courses, final double fall,
                              final double rise) {
        return new Strip(use, width, courses, null, fall, rise);
    }

    /** A ground strip, graded and {@code cover}ed. */
    public static Strip ground(final Use use, final double width, final Cover cover, final double fall,
                               final double rise) {
        return new Strip(use, width, List.of(), cover, fall, rise);
    }

    /** Whether it is built of courses: the road's to build. */
    public boolean paved() {
        return !courses.isEmpty();
    }

    /** How deep its courses go, together: its surface over its formation. None, on the ground. */
    public double depth() {
        double depth = 0;
        for (final Course c : courses) {
            depth += c.thick();
        }
        return depth;
    }
}
