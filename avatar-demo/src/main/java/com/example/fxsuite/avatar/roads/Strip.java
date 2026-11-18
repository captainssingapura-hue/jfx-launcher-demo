package com.example.fxsuite.avatar.roads;

import java.util.List;

/**
 * A strip of a road's cross-section, running along it: what it is for ({@link Use}), how wide ({@code width}, cm),
 * what it is built of top down ({@code courses}), how much its surface falls going away from the road's line
 * ({@code fall}, rise over run; less than nothing rises), and how far its inner edge steps up from the strip
 * inside it ({@code rise}, cm: a kerb's upstand).
 */
public record Strip(Use use, double width, List<Course> courses, double fall, double rise) {

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
        if (use == null || !(width > 0) || courses == null || courses.isEmpty() || !Double.isFinite(fall)
                || !Double.isFinite(rise)) {
            throw new IllegalArgumentException("a strip is for something, some width, built of something");
        }
        courses = List.copyOf(courses);
    }

    /** How deep its courses go, together: its surface over its formation. */
    public double depth() {
        double depth = 0;
        for (final Course c : courses) {
            depth += c.thick();
        }
        return depth;
    }

    /** What its surface is: its top course's stuff. */
    public Course.Stuff surface() {
        return courses.getFirst().stuff();
    }
}
