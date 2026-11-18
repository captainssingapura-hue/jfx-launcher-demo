package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Course;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.sim.Vec2;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A road as designed ({@link Roadway}), drawn as it is built, each part apart so each can be shown or not:
 *
 * <ul>
 *   <li><b>the ground</b>, as it was or as the road leaves it: grass where it is untouched, bare earth where it is
 *       cut or filled;</li>
 *   <li><b>the pavement</b>: every strip's every course, one on another on the formation, over every carrier
 *       alike; stopped short at a point along, if asked, to show its courses end on;</li>
 *   <li><b>its markings</b>, painted on its surface, over every carrier alike;</li>
 *   <li><b>its structures</b>: a deck under the formation, parapets at its edges, piers down to the ground, a seat
 *       at each end it comes off a bank; a tunnel's lining.</li>
 * </ul>
 *
 * <p>In the world's frame: x as it is, up as -y, z as it is.
 */
public final class RoadworkModel {

    private static final Map<Course.Stuff, PhongMaterial> STUFF = new EnumMap<>(Course.Stuff.class);
    private static final PhongMaterial PAINT = matte("#f2f2ec");
    private static final PhongMaterial STRUCTURE = matte("#a9afb6");
    private static final PhongMaterial GRASS = matte("#7da05a");
    private static final PhongMaterial EARTH = matte("#9a8062");
    private static final PhongMaterial WATER = new PhongMaterial(Color.web("#3d6f8a", 0.62));

    /** How far over the surface its markings lie, and how thick they are. */
    private static final double PAINT_LIFT = 0.4;
    private static final double PAINT_THICK = 0.6;
    /** A pier's crosshead's depth and length along; a column's side; how far a column goes into the ground. */
    private static final double CROSSHEAD = 100;
    private static final double CROSSHEAD_ALONG = 160;
    private static final double COLUMN = 120;
    private static final double FOOTING = 100;
    /** A seat's depth under a deck, and how far it runs back into the bank. */
    private static final double SEAT = 150;
    private static final double SEAT_BACK = 150;
    /** How far a tunnel's walls stand off the road's edges. */
    private static final double TUNNEL_SIDE = 50;
    /** The most points the ground is drawn with. */
    private static final int GROUND_POINTS = 160_000;

    static {
        STUFF.put(Course.Stuff.ASPHALT, matte("#4b5053"));
        STUFF.put(Course.Stuff.BINDER, matte("#34373a"));
        STUFF.put(Course.Stuff.BASE, matte("#988f80"));
        STUFF.put(Course.Stuff.SUBBASE, matte("#bcae93"));
        STUFF.put(Course.Stuff.CONCRETE, matte("#cbc8bf"));
        STUFF.put(Course.Stuff.PAVING, matte("#b7a58e"));
        STUFF.put(Course.Stuff.SAND, matte("#dcc88f"));
        STUFF.put(Course.Stuff.GRAVEL, matte("#a39d90"));
        STUFF.put(Course.Stuff.TOPSOIL, matte("#5c4632"));
        STUFF.put(Course.Stuff.TURF, matte("#6f9a4e"));
    }

    private RoadworkModel() {
    }

    /** How {@code stuff} is drawn: its colour. */
    public static Color colour(final Course.Stuff stuff) {
        return STUFF.get(stuff).getDiffuseColor();
    }

    /** The box round a road and as far as it reaches: min x, max x, min z, max z. */
    public static double[] bounds(final Roadway road) {
        double x0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double z0 = Double.POSITIVE_INFINITY;
        double z1 = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < road.size(); i++) {
            final Vec2 p = road.point(i);
            x0 = Math.min(x0, p.x());
            x1 = Math.max(x1, p.x());
            z0 = Math.min(z0, p.z());
            z1 = Math.max(z1, p.z());
        }
        final double reach = road.reach() + 1_000;
        return new double[] {x0 - reach, x1 + reach, z0 - reach, z1 + reach};
    }

    /**
     * The ground over {@code bounds}, as {@code ground} has it: grass where it is as {@code was} had it, bare earth
     * where it is not.
     */
    public static Group ground(final Profile was, final Profile ground, final double[] bounds) {
        final double w = bounds[1] - bounds[0];
        final double d = bounds[3] - bounds[2];
        final double cell = Math.max(100, Math.sqrt(w * d / GROUND_POINTS));
        final int nx = (int) Math.ceil(w / cell) + 1;
        final int nz = (int) Math.ceil(d / cell) + 1;
        final float[] points = new float[3 * nx * nz];
        final boolean[] moved = new boolean[nx * nz];
        for (int iz = 0; iz < nz; iz++) {
            final double z = Math.min(bounds[2] + iz * cell, bounds[3]);
            for (int ix = 0; ix < nx; ix++) {
                final double x = Math.min(bounds[0] + ix * cell, bounds[1]);
                final double h = ground.height(x, z);
                final int k = iz * nx + ix;
                points[3 * k] = (float) x;
                points[3 * k + 1] = (float) -h;
                points[3 * k + 2] = (float) z;
                moved[k] = Math.abs(h - was.height(x, z)) > 1;
            }
        }
        final int[] grass = new int[6 * 2 * (nx - 1) * (nz - 1)];
        final int[] earth = new int[grass.length];
        int g = 0;
        int e = 0;
        for (int iz = 0; iz + 1 < nz; iz++) {
            for (int ix = 0; ix + 1 < nx; ix++) {
                final int a = iz * nx + ix;
                final int b = a + 1;
                final int c = a + nx;
                final int dd = c + 1;
                final boolean bare = moved[a] || moved[b] || moved[c] || moved[dd];
                final int[] into = bare ? earth : grass;
                int at = bare ? e : g;
                for (final int p : new int[] {a, c, b, b, c, dd}) {
                    into[at] = p;
                    into[at + 1] = 0;
                    at += 2;
                }
                if (bare) {
                    e = at;
                } else {
                    g = at;
                }
            }
        }
        final Group group = new Group();
        if (g > 0) {
            group.getChildren().add(mesh(points, grass, g, GRASS));
        }
        if (e > 0) {
            group.getChildren().add(mesh(points, earth, e, EARTH));
        }
        return group;
    }

    /** Still water {@code level} high over {@code bounds}: see-through, to be drawn last. */
    public static MeshView water(final double level, final double[] bounds) {
        final float y = (float) -level;
        final float[] points = {
            (float) bounds[0], y, (float) bounds[2], (float) bounds[1], y, (float) bounds[2],
            (float) bounds[0], y, (float) bounds[3], (float) bounds[1], y, (float) bounds[3]};
        return mesh(points, new int[] {0, 0, 2, 0, 1, 0, 1, 0, 2, 0, 3, 0}, 12, WATER);
    }

    /** Its pavement: every strip's every course, from its start to {@code upTo} along it. */
    public static Group pavement(final Roadway road, final double upTo) {
        final Map<Course.Stuff, MeshBuilder> meshes = new EnumMap<>(Course.Stuff.class);
        for (int j = 0; j + 1 < road.size() && road.along(j) < upTo; j++) {
            final double t1 = end(road, j, upTo);
            for (final Section.Laid l : road.section(j).laid()) {
                double down = 0;
                for (final Course c : l.strip().courses()) {
                    final double from = down;
                    final double to = down + c.thick();
                    meshes.computeIfAbsent(c.stuff(), s -> new MeshBuilder()).box(corners(road, j, t1,
                            l.innerAcross(), l.outerAcross(),
                            l.innerTop() - to, l.innerTop() - from, l.outerTop() - to, l.outerTop() - from));
                    down = to;
                }
            }
        }
        final Group group = new Group();
        meshes.forEach((stuff, mesh) -> group.getChildren().add(mesh.view(STUFF.get(stuff))));
        return group;
    }

    /** Its markings, painted on its surface, from its start to {@code upTo} along it. */
    public static Group markings(final Roadway road, final double upTo) {
        final MeshBuilder paint = new MeshBuilder();
        for (int j = 0; j + 1 < road.size() && road.along(j) < upTo; j++) {
            final double t1 = end(road, j, upTo);
            final double s0 = road.along(j);
            final double length = road.along(j + 1) - s0;
            final Section section = road.section(j);
            for (final Section.Marking m : section.markings()) {
                final double a = m.across() - m.width() / 2;
                final double b = m.across() + m.width() / 2;
                final double ta = section.top(a);
                final double tb = section.top(b);
                if (m.solid()) {
                    paint.box(cornersBetween(road, j, 0, t1, a, b, ta + PAINT_LIFT, ta + PAINT_LIFT + PAINT_THICK,
                            tb + PAINT_LIFT, tb + PAINT_LIFT + PAINT_THICK));
                    continue;
                }
                final double period = m.dash() + m.gap();
                final double s1 = s0 + length * t1;
                for (long k = (long) Math.floor(s0 / period); k * period < s1; k++) {
                    final double from = Math.max(s0, k * period);
                    final double to = Math.min(s1, k * period + m.dash());
                    if (to > from) {
                        paint.box(cornersBetween(road, j, (from - s0) / length, (to - s0) / length, a, b,
                                ta + PAINT_LIFT, ta + PAINT_LIFT + PAINT_THICK, tb + PAINT_LIFT, tb + PAINT_LIFT + PAINT_THICK));
                    }
                }
            }
        }
        final Group group = new Group();
        if (!paint.empty()) {
            group.getChildren().add(paint.view(PAINT));
        }
        return group;
    }

    /** Its structures: decks, parapets, piers and seats; tunnels' linings. {@code ground} is what the piers stand in. */
    public static Group structures(final Roadway road, final Profile ground) {
        final MeshBuilder built = new MeshBuilder();
        for (final Roadway.Structure s : road.structures()) {
            final Roadway.Stretch stretch = s.stretch();
            if (stretch.carrier() == Carrier.STRUCTURE) {
                deck(road, stretch, built);
                for (final int i : s.piers()) {
                    pier(road, i, ground, built);
                }
                seats(road, stretch, built);
            } else if (stretch.carrier() == Carrier.TUNNEL) {
                lining(road, stretch, built);
            }
        }
        final Group group = new Group();
        if (!built.empty()) {
            group.getChildren().add(built.view(STRUCTURE));
        }
        return group;
    }

    /** A deck under the formation, down to {@link Roadway#DECK} under the line; parapets at its edges. */
    private static void deck(final Roadway road, final Roadway.Stretch stretch, final MeshBuilder built) {
        for (int j = stretch.from(); j < stretch.to(); j++) {
            final Section section = road.section(j);
            for (final Section.Laid l : section.laid()) {
                final double depth = l.strip().depth();
                built.box(corners(road, j, 1, l.innerAcross(), l.outerAcross(),
                        -Roadway.DECK, l.innerTop() - depth, -Roadway.DECK, l.outerTop() - depth));
            }
            for (final int side : new int[] {-1, 1}) {
                final List<Section.Laid> laid = section.laid(side);
                if (laid.isEmpty()) {
                    continue;
                }
                final Section.Laid outer = laid.getLast();
                final double edge = outer.outerAcross();
                final double top = outer.outerTop() + Roadway.PARAPET;
                built.box(corners(road, j, 1, edge, edge + side * Roadway.PARAPET_THICK,
                        -Roadway.DECK, top, -Roadway.DECK, top));
            }
        }
    }

    /** A pier at its {@code i}th point: a crosshead under the deck, one column or two down into the ground. */
    private static void pier(final Roadway road, final int i, final Profile ground, final MeshBuilder built) {
        final Section section = road.section(Math.min(i, road.size() - 2));
        final double left = -section.edge(-1) * 0.9;
        final double right = section.edge(1) * 0.9;
        final double under = road.level(i) - Roadway.DECK;
        final Vec2 r = road.right(i);
        final Vec2 h = new Vec2(-r.z(), r.x());
        final Vec2 p = road.point(i);
        built.box(box(p, r, h, left, right, -CROSSHEAD_ALONG / 2, CROSSHEAD_ALONG / 2, under - CROSSHEAD, under));
        final double width = right - left;
        final double middle = (left + right) / 2;
        final double[] columns = width > 1_200 ? new double[] {middle - width / 4, middle + width / 4} : new double[] {middle};
        for (final double c : columns) {
            final Vec2 at = road.at(i, c);
            final double foot = ground.height(at.x(), at.z()) - FOOTING;
            built.box(box(p, r, h, c - COLUMN / 2, c + COLUMN / 2, -COLUMN / 2, COLUMN / 2, foot, under - CROSSHEAD));
        }
    }

    /** A seat under each end of a deck that comes off a bank, running back into it. */
    private static void seats(final Roadway road, final Roadway.Stretch stretch, final MeshBuilder built) {
        for (final int end : new int[] {stretch.from(), stretch.to()}) {
            final boolean grounded = end == stretch.from() ? end > 0 && road.carrier(end - 1) == Carrier.GROUND
                    : end < road.size() - 1 && road.carrier(end) == Carrier.GROUND;
            if (!grounded) {
                continue;
            }
            final Section section = road.section(end == stretch.from() ? end : end - 1);
            final Vec2 r = road.right(end);
            final Vec2 h = new Vec2(-r.z(), r.x());
            final double back = end == stretch.from() ? -1 : 1;
            final double under = road.level(end) - Roadway.DECK;
            built.box(box(road.point(end), r, h, -section.edge(-1) - Roadway.PARAPET_THICK,
                    section.edge(1) + Roadway.PARAPET_THICK, Math.min(0, back * SEAT_BACK), Math.max(0, back * SEAT_BACK),
                    under - SEAT, under));
        }
    }

    /** A tunnel's lining: a wall each side off the road's edges, a roof over its headroom. */
    private static void lining(final Roadway road, final Roadway.Stretch stretch, final MeshBuilder built) {
        for (int j = stretch.from(); j < stretch.to(); j++) {
            final Section section = road.section(j);
            final double l = -section.edge(-1) - TUNNEL_SIDE;
            final double r = section.edge(1) + TUNNEL_SIDE;
            final double low = section.formation(0) - 100;
            final double high = Roadway.HEADROOM;
            built.box(corners(road, j, 1, l - Roadway.LINING, l, low, high, low, high));
            built.box(corners(road, j, 1, r, r + Roadway.LINING, low, high, low, high));
            built.box(corners(road, j, 1, l - Roadway.LINING, r + Roadway.LINING, high, high + Roadway.LINING,
                    high, high + Roadway.LINING));
        }
    }

    /** How far along its {@code j}th segment it is drawn: all of it, or up to {@code upTo}. */
    private static double end(final Roadway road, final int j, final double upTo) {
        final double length = road.along(j + 1) - road.along(j);
        return Math.min(1, (upTo - road.along(j)) / length);
    }

    /**
     * A box along its {@code j}th segment, from its start to {@code t1} of the way, across from {@code a} to
     * {@code b}; its bottom and top at {@code a} and at {@code b} so high over the line there.
     */
    private static Point3D[] corners(final Roadway road, final int j, final double t1, final double a, final double b,
                                     final double aLow, final double aHigh, final double bLow, final double bHigh) {
        return cornersBetween(road, j, 0, t1, a, b, aLow, aHigh, bLow, bHigh);
    }

    private static Point3D[] cornersBetween(final Roadway road, final int j, final double t0, final double t1,
                                            final double a, final double b, final double aLow, final double aHigh,
                                            final double bLow, final double bHigh) {
        final Point3D[] corners = new Point3D[8];
        for (int k = 0; k < 8; k++) {
            final boolean outer = (k & 1) != 0;
            final boolean top = (k & 2) != 0;
            final double t = (k & 4) != 0 ? t1 : t0;
            final double across = outer ? b : a;
            final Vec2 p0 = road.at(j, across);
            final Vec2 p1 = road.at(j + 1, across);
            final double x = p0.x() + (p1.x() - p0.x()) * t;
            final double z = p0.z() + (p1.z() - p0.z()) * t;
            final double up = road.level(j, t) + (outer ? top ? bHigh : bLow : top ? aHigh : aLow);
            corners[k] = new Point3D(x, -up, z);
        }
        return corners;
    }

    /** A box about {@code p}: across ({@code r}) from {@code a0} to {@code a1}, along ({@code h}) from {@code s0} to {@code s1}, from {@code low} up to {@code high}. */
    private static Point3D[] box(final Vec2 p, final Vec2 r, final Vec2 h, final double a0, final double a1,
                                 final double s0, final double s1, final double low, final double high) {
        final Point3D[] corners = new Point3D[8];
        for (int k = 0; k < 8; k++) {
            final double a = (k & 1) != 0 ? a1 : a0;
            final double up = (k & 2) != 0 ? high : low;
            final double s = (k & 4) != 0 ? s1 : s0;
            corners[k] = new Point3D(p.x() + r.x() * a + h.x() * s, -up, p.z() + r.z() * a + h.z() * s);
        }
        return corners;
    }

    private static MeshView mesh(final float[] points, final int[] faces, final int length, final PhongMaterial material) {
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(0, 0);
        mesh.getFaces().setAll(faces, 0, length);
        final int[] groups = new int[length / 6];
        java.util.Arrays.fill(groups, 1);
        mesh.getFaceSmoothingGroups().setAll(groups);
        final MeshView view = new MeshView(mesh);
        view.setMaterial(material);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    private static PhongMaterial matte(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#2a2d31"));
        return material;
    }
}
