package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * A level surface of a building, any shape its plan has: a floor, a roof, a terrace. Its outline
 * {@code outline} (anticlockwise, seen from above), at {@code up}; facing up. A design dresses it as
 * a slab ({@link Design#plate}), cut into triangles ({@link #triangles}) each a prism, so a round
 * floor is as easily laid as a square one.
 *
 * @param name    which it is
 * @param kind    what sort it is ("shaft floor", "top roof"): how a set of designs says which it dresses
 * @param outline its outline, anticlockwise
 * @param up      how high it is
 */
public record Plate(String name, String kind, List<Plan.Pt> outline, double up) {

    public Plate {
        outline = List.copyOf(outline);
        if (outline.size() < 3) {
            throw new IllegalArgumentException(name + " has no area");
        }
    }

    /** How much floor it is, in square centimetres. */
    public double area() {
        return Plan.Layout.signedArea(outline);
    }

    /**
     * Its outline cut into triangles, by clipping ears: a corner whose triangle with its neighbours
     * turns the way the outline does and holds no other corner of it is cut off, and so on round.
     * Each triangle anticlockwise.
     */
    public List<Plan.Pt[]> triangles() {
        final List<Plan.Pt> left = new ArrayList<>(outline);
        final List<Plan.Pt[]> triangles = new ArrayList<>();
        int guard = 0;
        while (left.size() > 3 && guard < outline.size() * outline.size()) {
            guard++;
            boolean cut = false;
            for (int k = 0; k < left.size(); k++) {
                final Plan.Pt a = left.get((k + left.size() - 1) % left.size());
                final Plan.Pt b = left.get(k);
                final Plan.Pt c = left.get((k + 1) % left.size());
                if (cross(a, b, c) <= 1e-9) {
                    continue;  // a corner turning back: not an ear
                }
                boolean empty = true;
                for (final Plan.Pt p : left) {
                    if (p != a && p != b && p != c && within(p, a, b, c)) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    triangles.add(new Plan.Pt[] {a, b, c});
                    left.remove(k);
                    cut = true;
                    break;
                }
            }
            if (!cut) {
                break;
            }
        }
        if (left.size() == 3) {
            triangles.add(new Plan.Pt[] {left.get(0), left.get(1), left.get(2)});
        }
        return triangles;
    }

    private static double cross(final Plan.Pt a, final Plan.Pt b, final Plan.Pt c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    private static boolean within(final Plan.Pt p, final Plan.Pt a, final Plan.Pt b, final Plan.Pt c) {
        return cross(a, b, p) >= -1e-9 && cross(b, c, p) >= -1e-9 && cross(c, a, p) >= -1e-9;
    }

    /**
     * A slab of it, from {@code w0} to {@code w1} up from its level, doing {@code role}: a prism on each
     * of its triangles, each a box two of whose corners are one ({@link Solid}).
     */
    public List<Solid> slab(final String called, final Palette palette, final Role role, final double w0,
                            final double w1) {
        final List<Solid> slab = new ArrayList<>();
        int n = 0;
        for (final Plan.Pt[] t : triangles()) {
            // a quad with its last two corners the same: corners 0 and 1 a, b; 2 and 3 both c
            final Plan.Pt[] quad = {t[0], t[1], t[2], t[2]};
            final List<Point> corners = new ArrayList<>(8);
            for (int k = 0; k < 8; k++) {
                final Plan.Pt p = quad[(k & 1) + ((k & 2) == 0 ? 0 : 2)];
                corners.add(new Point(p.x(), p.y(), up + ((k & 4) == 0 ? w0 : w1)));
            }
            slab.add(new Solid(name + ", " + called + " " + n++, role, palette.of(role), corners));
        }
        return slab;
    }
}
