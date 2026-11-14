package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Facade.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * What can be added to a wall once it is dressed, whatever its design ({@link Building.Addition}):
 * corner stones up its corners, a cornice along its top. Each a dressing ({@link Role#DRESSING}),
 * of what the palette makes that of; each reaching round the building's corners, so the walls
 * meeting there meet in it.
 *
 * <p>A wall's corners, in its own frame, are at u {@code -t/2} and {@code width + t/2} ({@code t}
 * its thickness): its plane runs along the middle of the walls it meets there. Seen from outside,
 * one wall's right-hand corner is always the next one's left-hand corner.
 */
public final class Dressings {

    /** The joints between stones. */
    private static final double JOINT = 2;
    /** Kept this far clear of a window, all round. */
    private static final double CLEAR = 6;

    private Dressings() {
    }

    /**
     * Corner stones (quoins) up each corner of a wall doing {@code role}: courses as near
     * {@code course} high as fit it evenly, each stone standing {@code proud} of the wall's face and wrapping its corner, long
     * ({@code longer} along the wall) and short ({@code shorter}) by turns; long at its left-hand
     * corner on even courses, at its right-hand on odd ones, so that round each corner a long
     * stone on one face meets a short one on the other, as they are laid. A stone that would come
     * over a window is left out.
     */
    public static Building.Addition cornerStones(final Role role, final double course, final double longer,
                                                 final double shorter, final double proud) {
        return (surface, palette, dressed) -> {
            final double half = palette.thickness(role) / 2;
            final List<Rect> windows = openings(surface, dressed);
            final List<Solid> stones = new ArrayList<>();
            // as near the course asked for as fits the wall a whole number of times: no sliver at its top
            final int courses = Math.max(1, (int) Math.round(surface.height() / course));
            final double high = surface.height() / courses;
            for (int c = 0; c < courses; c++) {
                final double v0 = c * high + JOINT / 2;
                final double v1 = (c + 1) * high - JOINT / 2;
                for (final boolean left : new boolean[] {true, false}) {
                    final double length = (c % 2 == 0) == left ? longer : shorter;
                    final Rect stone = left ? new Rect(-half - proud, -half + length, v0, v1)
                            : new Rect(surface.width() + half - length, surface.width() + half + proud, v0, v1);
                    if (windows.stream().noneMatch(w -> overlap(w, stone))) {
                        stones.add(surface.box("corner stone, " + (left ? "left" : "right") + ", course " + c, palette,
                                Role.DRESSING, stone.u0(), stone.u1(), v0, v1, 0, half + proud));
                    }
                }
            }
            return stones;
        };
    }

    /**
     * A cornice along the top of a wall doing {@code role}: a moulding of four steps, each standing
     * further out than the one under it, the corona (its deepest, square face) over them and a cap
     * on that; over the wall's coping, and round its corners, each step as far beyond them as it
     * stands out.
     */
    public static Building.Addition cornice(final Role role) {
        // each step: from, to (up from the wall's top), and how far beyond the wall's face it stands out
        final double[][] steps = {{-34, -22, 8}, {-22, -10, 18}, {-10, 12, 34}, {12, 17, 28}};
        return (surface, palette, dressed) -> {
            final double half = palette.thickness(role) / 2;
            final List<Solid> cornice = new ArrayList<>();
            final String[] names = {"bed", "ovolo", "corona", "cap"};
            for (int k = 0; k < steps.length; k++) {
                final double out = half + steps[k][2];
                cornice.add(surface.box("cornice, " + names[k], palette, Role.DRESSING, -out, surface.width() + out,
                        surface.height() + steps[k][0], surface.height() + steps[k][1], -half - 10, out));
            }
            return cornice;
        };
    }

    /** Where {@code dressed}'s windows are on {@code surface}: its glass's extent there, and a little clear of it. */
    static List<Rect> openings(final Surface surface, final List<Solid> dressed) {
        final List<Rect> openings = new ArrayList<>();
        for (final Solid solid : dressed) {
            if (solid.role() != Role.GLAZING || !solid.name().startsWith(surface.name() + ", ")) {
                continue;
            }
            double u0 = Double.POSITIVE_INFINITY;
            double u1 = Double.NEGATIVE_INFINITY;
            double v0 = Double.POSITIVE_INFINITY;
            double v1 = Double.NEGATIVE_INFINITY;
            for (final Point corner : solid.corners()) {
                final Point from = corner.plus(surface.origin().times(-1));
                u0 = Math.min(u0, from.dot(surface.u()));
                u1 = Math.max(u1, from.dot(surface.u()));
                v0 = Math.min(v0, from.dot(surface.v()));
                v1 = Math.max(v1, from.dot(surface.v()));
            }
            openings.add(new Rect(u0 - CLEAR, u1 + CLEAR, v0 - CLEAR, v1 + CLEAR));
        }
        return openings;
    }

    private static boolean overlap(final Rect a, final Rect b) {
        return a.u0() < b.u1() && b.u0() < a.u1() && a.v0() < b.v1() && b.v0() < a.v1();
    }
}
