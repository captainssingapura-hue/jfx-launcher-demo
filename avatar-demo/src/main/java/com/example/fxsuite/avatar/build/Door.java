package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * A door, or a gate: one leaf, or a pair meeting in the middle, each turning on a hinge at its outer
 * edge, from shut (0°) to open ({@code most}); for whatever turns so, an aircraft's cabin door or a
 * temple's gate. Pure: where its parts are, at any angle, for whoever draws it (an aircraft, a
 * building) and for what it is in the way of.
 *
 * <p>Each leaf framed as a ledged and braced door is: a stile up each edge, a rail across its foot,
 * its middle and its head, and a brace across each of its two panels, rising from its hinge side,
 * where it hangs, to its free side; an infill in each panel. So its frame is its skeleton
 * ({@link #skeleton}): a node where its members meet, a member between each two.
 *
 * <p>In its own frame: across its width (from its left jamb, 0, to its right, {@code width}), along
 * out of its face ({@code +} the way it opens), up from its sill.
 *
 * @param name   what it is
 * @param width  how wide its opening is, both leaves together
 * @param height how high
 * @param thick  how thick its leaves are
 * @param leaves one, hung at its left jamb; or two, a pair, one at each jamb, meeting in the middle
 * @param most   how far it opens, degrees
 */
public record Door(String name, double width, double height, double thick, int leaves, double most) {

    /** How wide its stiles and rails are, a share of a leaf's width or its height, whichever is less; its braces, a share of that. */
    public static final double FRAME = 0.1;
    public static final double BRACE = 0.8;
    /** How high its middle rail is, a share of its height. */
    public static final double MIDDLE = 0.45;
    /** How thick its infill is, a share of its frame's thickness: set back in it. */
    public static final double INFILL = 0.4;

    public Door {
        if (!(width > 0 && height > 0 && thick > 0) || leaves < 1 || leaves > 2 || most <= 0 || most > 180) {
            throw new IllegalArgumentException(name + ": a door " + width + " by " + height + ", " + thick + " thick, "
                    + leaves + " leaves, opening " + most);
        }
    }

    /** One leaf, hung at its left jamb. */
    public static Door single(final String name, final double width, final double height, final double thick,
                              final double most) {
        return new Door(name, width, height, thick, 1, most);
    }

    /** A pair of leaves, one at each jamb, meeting in the middle: a gate. */
    public static Door pair(final String name, final double width, final double height, final double thick,
                            final double most) {
        return new Door(name, width, height, thick, 2, most);
    }

    /** How wide each leaf is. */
    public double leaf() {
        return width / leaves;
    }

    /** Where leaf {@code k}'s hinge is across: at its jamb, the left (0) or the right ({@code width}). */
    public double hinge(final int k) {
        return k == 0 ? 0 : width;
    }

    /**
     * Which way leaf {@code k} turns about +up as it opens: the left one the right-hand way, its free
     * edge swinging out (+along); the right one the other way, so it swings out too.
     */
    public double sense(final int k) {
        return k == 0 ? 1 : -1;
    }

    /** A member of a leaf's frame: its centre line, {@code from} one node {@code to} another, so {@code wide} in its face. */
    public record Member(String name, Point from, Point to, double wide) {
    }

    /** How wide its stiles and rails are. */
    public double frame() {
        return FRAME * Math.min(leaf(), height);
    }

    /**
     * Leaf {@code k}'s nodes, where its members meet, shut: at its hinge side and its free side, its
     * foot, its middle and its head, each on its members' centre lines; named.
     */
    public List<Point> nodes(final int k) {
        final double f = frame();
        final double out = k == 0 ? 1 : -1;
        final double[] us = {f / 2, leaf() - f / 2};
        final double[] zs = {f / 2, MIDDLE * height, height - f / 2};
        final List<Point> nodes = new ArrayList<>();
        for (final double z : zs) {
            for (final double u : us) {
                nodes.add(new Point(hinge(k) + out * u, 0, z));
            }
        }
        return nodes;
    }

    /** The names of a leaf's nodes, in order: its hinge side, then its free side, at its foot, middle and head. */
    private static final String[] NODES = {"hinge foot", "free foot", "hinge middle", "free middle", "hinge head",
            "free head"};

    /**
     * Leaf {@code k}'s frame, shut: its two stiles (each in two, at its middle rail), its three rails,
     * and its two braces, each rising from its hinge side to its free side.
     */
    public List<Member> members(final int k) {
        final List<Point> n = nodes(k);
        final double f = frame();
        final double b = BRACE * f;
        return List.of(
                new Member("hinge stile, low", n.get(0), n.get(2), f), new Member("hinge stile, high", n.get(2), n.get(4), f),
                new Member("free stile, low", n.get(1), n.get(3), f), new Member("free stile, high", n.get(3), n.get(5), f),
                new Member("foot rail", n.get(0), n.get(1), f), new Member("middle rail", n.get(2), n.get(3), f),
                new Member("head rail", n.get(4), n.get(5), f),
                new Member("low brace", n.get(0), n.get(3), b), new Member("high brace", n.get(2), n.get(5), b));
    }

    /** Leaf {@code k}'s infills, shut: its lower panel's corners and its upper's, round, in its middle plane. */
    public List<List<Point>> infills(final int k) {
        final List<Point> n = nodes(k);
        final List<List<Point>> panels = new ArrayList<>();
        for (final int[] panel : new int[][] {{0, 1, 3, 2}, {2, 3, 5, 4}}) {
            panels.add(List.of(n.get(panel[0]), n.get(panel[1]), n.get(panel[2]), n.get(panel[3])));
        }
        return panels;
    }

    /** {@code point}, of leaf {@code k} shut, as it is opened {@code degrees}: turned about its hinge, upright through its jamb. */
    public Point opened(final int k, final Point point, final double degrees) {
        final double a = Math.toRadians(Math.clamp(degrees, 0, most) * sense(k));
        final double x = point.across() - hinge(k);
        final double y = point.along();
        // the right-hand way about +up: across turning toward along
        return new Point(hinge(k) + x * Math.cos(a) - y * Math.sin(a), x * Math.sin(a) + y * Math.cos(a), point.up());
    }

    /**
     * A member's eight corners, as it is opened {@code degrees}: a box about its centre line, so
     * {@code wide} in its leaf's face, {@code deep} through it; corner k at its {@code from} or
     * {@code to} end as bit 0 says, its back or front face as bit 1 does, one edge or the other as bit 2
     * does: a hexahedron, for a piece or a solid.
     */
    public List<Point> corners(final int k, final Member member, final double deep, final double degrees) {
        final Point run = member.to().plus(member.from().times(-1));
        final Point unit = run.times(1 / run.length());
        // its face's normal, and across the member in its face: square to both, the right-hand way
        final Point face = new Point(0, 1, 0);
        final Point side = new Point(unit.along() * face.up() - unit.up() * face.along(),
                unit.up() * face.across() - unit.across() * face.up(), unit.across() * face.along() - unit.along() * face.across());
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final Point end = (c & 1) == 0 ? member.from() : member.to();
            final Point at = end.plus(face.times(((c & 2) == 0 ? -0.5 : 0.5) * deep))
                    .plus(side.times(((c & 4) == 0 ? -0.5 : 0.5) * member.wide()));
            corners.add(opened(k, at, degrees));
        }
        return corners;
    }

    /**
     * An infill's eight corners ({@link #infills}), as it is opened {@code degrees}: its panel so
     * {@code deep} through, corner k at one side of it or the other as bit 0 says (from its first corner
     * toward its second), its back or front face as bit 1 does, its foot or its head as bit 2 does,
     * the right-hand way round, as {@link #corners(int, Member, double, double)} are.
     */
    public List<Point> corners(final int k, final List<Point> panel, final double deep, final double degrees) {
        final Point run = panel.get(1).plus(panel.get(0).times(-1));
        // the right-hand way: running +across its head is up, running -across (the right leaf's) its foot
        final boolean upward = run.across() > 0;
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final boolean far = (c & 1) != 0;
            final boolean high = ((c & 4) != 0) == upward;
            final Point at = high ? panel.get(far ? 2 : 3) : panel.get(far ? 1 : 0);
            corners.add(opened(k, at.plus(new Point(0, ((c & 2) == 0 ? -0.5 : 0.5) * deep, 0)), degrees));
        }
        return corners;
    }

    /** Leaf {@code k}'s skeleton, as it is opened {@code degrees}: a vertex at each node, an edge along each member. */
    public Form skeleton(final int k, final double degrees) {
        final Form.Builder form = new Form.Builder(name + (leaves == 1 ? "" : k == 0 ? ", left" : ", right"));
        final List<Point> n = nodes(k);
        for (int i = 0; i < n.size(); i++) {
            final Point p = opened(k, n.get(i), degrees);
            form.vertex(NODES[i], p.across(), p.along(), p.up());
        }
        for (final int[] edge : new int[][] {{0, 2}, {2, 4}, {1, 3}, {3, 5}, {0, 1}, {2, 3}, {4, 5}, {0, 3}, {2, 5}}) {
            form.edge(NODES[edge[0]], NODES[edge[1]]);
        }
        return form.build();
    }
}
