package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Course;
import com.example.fxsuite.avatar.roads.Cover;
import com.example.fxsuite.avatar.roads.Ground;
import com.example.fxsuite.avatar.roads.Prepared;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
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
    private static final PhongMaterial WALL = matte("#c76b4f");
    private static final PhongMaterial GRASS = matte("#7da05a");
    private static final PhongMaterial SEEDED = matte("#93b86a");
    private static final PhongMaterial EARTH = matte("#9a8062");
    private static final PhongMaterial GRAVEL = matte("#a39d90");
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
    /** How thick a wall wanted is drawn. */
    private static final double WALL_THICK = 40;
    /** How far a tunnel's walls stand off the road's edges. */
    private static final double TUNNEL_SIDE = 50;
    /** The most points the ground is drawn with. */
    private static final int GROUND_POINTS = 160_000;

    static {
        // lit from above as it faces, a top seen in the sun: darker than it shows
        STUFF.put(Course.Stuff.ASPHALT, matte("#2c3033"));
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

    /** The box round roads and as far as they reach: min x, max x, min z, max z. */
    public static double[] bounds(final List<Roadway> roads) {
        double x0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double z0 = Double.POSITIVE_INFINITY;
        double z1 = Double.NEGATIVE_INFINITY;
        double reach = 0;
        for (final Roadway road : roads) {
            for (int i = 0; i < road.size(); i++) {
                final Vec2 p = road.point(i);
                x0 = Math.min(x0, p.x());
                x1 = Math.max(x1, p.x());
                z0 = Math.min(z0, p.z());
                z1 = Math.max(z1, p.z());
            }
            reach = Math.max(reach, road.reach() + 1_000);
        }
        return new double[] {x0 - reach, x1 + reach, z0 - reach, z1 + reach};
    }

    /**
     * The ground over {@code bounds}, as {@code ground} has it, coloured by its cover: grass where no road has
     * touched it; where one has prepared it, the grass it has seeded a fresher green, bare earth where it has left
     * it bare. {@code asBuilt}: all it has touched bare, before anything grows.
     */
    public static Group ground(final Ground ground, final boolean asBuilt, final double[] bounds) {
        final double w = bounds[1] - bounds[0];
        final double d = bounds[3] - bounds[2];
        final double cell = Math.max(100, Math.sqrt(w * d / GROUND_POINTS));
        final int nx = (int) Math.ceil(w / cell) + 1;
        final int nz = (int) Math.ceil(d / cell) + 1;
        final float[] points = new float[3 * nx * nz];
        final Cover[] covers = new Cover[nx * nz];
        for (int iz = 0; iz < nz; iz++) {
            final double z = Math.min(bounds[2] + iz * cell, bounds[3]);
            for (int ix = 0; ix < nx; ix++) {
                final double x = Math.min(bounds[0] + ix * cell, bounds[1]);
                final Prepared p = ground.at(x, z);
                final int k = iz * nx + ix;
                points[3 * k] = (float) x;
                points[3 * k + 1] = (float) -p.height();
                points[3 * k + 2] = (float) z;
                covers[k] = p.cover() != null && asBuilt ? Cover.EARTH : p.cover();
            }
        }
        // each square as its corners' covers say: bare if any is, gravel if any is, seeded if any is, else as it was
        final List<PhongMaterial> looks = List.of(GRASS, SEEDED, EARTH, GRAVEL);
        final int[][] faces = new int[looks.size()][6 * 2 * (nx - 1) * (nz - 1)];
        final int[] counts = new int[looks.size()];
        for (int iz = 0; iz + 1 < nz; iz++) {
            for (int ix = 0; ix + 1 < nx; ix++) {
                final int a = iz * nx + ix;
                final int b = a + 1;
                final int c = a + nx;
                final int dd = c + 1;
                int look = 0;
                for (final int k : new int[] {a, b, c, dd}) {
                    if (covers[k] != null) {
                        look = Math.max(look, switch (covers[k]) {
                            case GRASS -> 1;
                            case GRAVEL -> 2;
                            case EARTH -> 3;
                        });
                    }
                }
                final int into = look == 0 ? 0 : look == 1 ? 1 : look == 2 ? 3 : 2;
                for (final int p : new int[] {a, c, b, b, c, dd}) {
                    faces[into][counts[into]++] = p;
                    faces[into][counts[into]++] = 0;
                }
            }
        }
        final Group group = new Group();
        for (int m = 0; m < looks.size(); m++) {
            if (counts[m] > 0) {
                group.getChildren().add(mesh(points, faces[m], counts[m], looks.get(m)));
            }
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

    /**
     * Its pavement, from its start to {@code upTo} along it, as it can be seen: each paved strip's top; its side where it
     * meets no other paved strip, course by course; a step up from one strip to the next, a kerb's; and end on, course
     * by course, where it ends or is cut away. Nothing of it under its top or between its strips, which no one sees; each
     * face drawn from its outside only. Its ground strips are the ground's.
     */
    public static Group pavement(final Roadway road, final double upTo) {
        return pavement(road, upTo, NOWHERE);
    }

    /** The same, but where {@code covered} says another road's pavement already lies: drawn once, the other's. */
    public static Group pavement(final Roadway road, final double upTo, final Covered covered) {
        return pavement(road, 0, road.size() - 1, upTo, covered);
    }

    /** Its pavement from its {@code from}th point to its {@code to}th, as {@link #pavement(Roadway, double, Covered)} draws it all: a stretch of it. */
    public static Group pavement(final Roadway road, final int from, final int to, final Covered covered) {
        return pavement(road, from, to, Double.POSITIVE_INFINITY, covered);
    }

    private static Group pavement(final Roadway road, final int first, final int past, final double upTo,
                                  final Covered covered) {
        final Map<Course.Stuff, Faces> meshes = new EnumMap<>(Course.Stuff.class);
        final int last = road.size() - 2;
        for (int j = first; j < past && j + 1 < road.size() && road.along(j) < upTo; j++) {
            final double t1 = end(road, j, upTo);
            // as it is across at either end of the segment: tilted, it may be, the one more than the other
            final List<Section.Laid> here = road.section(j).laid();
            // a strip more or fewer at its far end (its kind changing there): drawn as at its near end
            final List<Section.Laid> far = road.section(j + 1).laid();
            final List<Section.Laid> there = far.size() == here.size() ? far : here;
            final Vec2 ahead = heading(road, j);
            for (int k = 0; k < here.size(); k++) {
                final Section.Laid l0 = here.get(k);
                if (!l0.strip().paved()) {
                    continue;
                }
                final Section.Laid l1 = there.get(k);
                final Course.Stuff surface = l0.strip().courses().getFirst().stuff();
                // its top
                faces(meshes, surface, covered).quad(along(road, j, 0, t1, l0.innerAcross(), l0.outerAcross(),
                        (outer, top, t) -> top(l0, l1, outer, t)), up());
                // its sides: where it meets nothing paved, course by course; where it meets a lower paved strip, the step
                for (final boolean outer : new boolean[] {false, true}) {
                    final int next = k + ((outer ? l0.outerAcross() : l0.innerAcross()) > (outer ? l0.innerAcross()
                            : l0.outerAcross()) ? 1 : -1);
                    final double edge = outer ? l0.outerAcross() : l0.innerAcross();
                    final double out = Math.signum(edge - (outer ? l0.innerAcross() : l0.outerAcross()));
                    final Section.Laid n0 = next >= 0 && next < here.size() ? here.get(next) : null;
                    final Section.Laid n1 = next >= 0 && next < there.size() ? there.get(next) : null;
                    final Vec2 facing = new Vec2(road.right(j).x() * out, road.right(j).z() * out);
                    if (n0 == null || !n0.strip().paved()) {
                        double down = 0;
                        for (final Course c : l0.strip().courses()) {
                            final double from = down;
                            final double to = down + c.thick();
                            faces(meshes, c.stuff(), covered).quad(edgeFace(road, j, t1, edge,
                                    t -> top(l0, l1, outer, t) - to, t -> top(l0, l1, outer, t) - from), flat(facing));
                            down = to;
                        }
                    } else {
                        // a step down to the next strip: its face, from the next's top up to this one's
                        final double step0 = top(l0, l1, outer, 0) - nextTop(n0, n1, edge, 0);
                        if (step0 > 0.5) {
                            faces(meshes, surface, covered).quad(edgeFace(road, j, t1, edge,
                                    t -> nextTop(n0, n1, edge, t), t -> top(l0, l1, outer, t)), flat(facing));
                        }
                    }
                }
                // end on, where it ends or is cut away: course by course
                final boolean starts = j == 0 && !road.loop();
                final boolean ends = t1 < 1 || j == last && !road.loop();
                if (starts || ends) {
                    for (final boolean atEnd : new boolean[] {false, true}) {
                        if (atEnd ? !ends : !starts) {
                            continue;
                        }
                        final double t = atEnd ? t1 : 0;
                        final Vec2 facing = atEnd ? ahead : new Vec2(-ahead.x(), -ahead.z());
                        double down = 0;
                        for (final Course c : l0.strip().courses()) {
                            final double from = down;
                            final double to = down + c.thick();
                            faces(meshes, c.stuff(), covered).quad(endFace(road, j, t, l0.innerAcross(), l0.outerAcross(),
                                    outer -> top(l0, l1, outer, t) - to, outer -> top(l0, l1, outer, t) - from),
                                    flat(facing));
                            down = to;
                        }
                    }
                }
            }
        }
        final Group group = new Group();
        meshes.forEach((stuff, mesh) -> group.getChildren().add(mesh.view(STUFF.get(stuff))));
        return group;
    }

    /** How high a strip's top is at its inner edge or its {@code outer}, {@code t} of the way along, over the line. */
    private static double top(final Section.Laid l0, final Section.Laid l1, final boolean outer, final double t) {
        return lerp(outer ? l0.outerTop() : l0.innerTop(), outer ? l1.outerTop() : l1.innerTop(), t);
    }

    /** How high the next strip's top is at its edge {@code edge} across, {@code t} of the way along, over the line. */
    private static double nextTop(final Section.Laid n0, final Section.Laid n1, final double edge, final double t) {
        final boolean outer = n0.outerAcross() == edge;
        return top(n0, n1, outer, t);
    }

    /** Its markings, painted on its surface, from its start to {@code upTo} along it: each a face on its top. */
    public static Group markings(final Roadway road, final double upTo) {
        return markings(road, upTo, NOWHERE);
    }

    /** The same, but none where {@code covered} says another road's pavement already lies, with its own. */
    public static Group markings(final Roadway road, final double upTo, final Covered covered) {
        return markings(road, 0, road.size() - 1, upTo, covered);
    }

    /** Its markings from its {@code from}th point to its {@code to}th: a stretch of them. */
    public static Group markings(final Roadway road, final int from, final int to, final Covered covered) {
        return markings(road, from, to, Double.POSITIVE_INFINITY, covered);
    }

    private static Group markings(final Roadway road, final int first, final int past, final double upTo,
                                  final Covered covered) {
        final Faces paint = new Faces(covered);
        for (int j = first; j < past && j + 1 < road.size() && road.along(j) < upTo; j++) {
            final double t1 = end(road, j, upTo);
            final double s0 = road.along(j);
            final double length = road.along(j + 1) - s0;
            final Section here = road.section(j);
            final Section there = road.section(j + 1);
            for (final Section.Marking m : here.markings()) {
                final double a = m.across() - m.width() / 2;
                final double b = m.across() + m.width() / 2;
                final Rise rise = (outer, top, t) -> lerp(here.top(outer ? b : a), there.top(outer ? b : a), t)
                        + PAINT_LIFT;
                if (m.solid()) {
                    paint.quad(along(road, j, 0, t1, a, b, rise), up());
                    continue;
                }
                final double period = m.dash() + m.gap();
                final double s1 = s0 + length * t1;
                for (long k = (long) Math.floor(s0 / period); k * period < s1; k++) {
                    final double from = Math.max(s0, k * period);
                    final double to = Math.min(s1, k * period + m.dash());
                    if (to > from) {
                        paint.quad(along(road, j, (from - s0) / length, (to - s0) / length, a, b, rise), up());
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

    /** The way the road goes along its {@code j}th segment, a unit long. */
    private static Vec2 heading(final Roadway road, final int j) {
        final Vec2 a = road.point(j);
        final Vec2 b = road.point(j + 1);
        final double len = Math.hypot(b.x() - a.x(), b.z() - a.z());
        return new Vec2((b.x() - a.x()) / len, (b.z() - a.z()) / len);
    }

    /** The way up, in the world's frame as drawn (up is -y). */
    private static Point3D up() {
        return new Point3D(0, -1, 0);
    }

    /** A way across the ground, in the world's frame as drawn. */
    private static Point3D flat(final Vec2 way) {
        return new Point3D(way.x(), 0, way.z());
    }

    /**
     * A face upright along the road's {@code j}th segment, from its start to {@code t1} of the way, {@code across} from
     * its line, from {@code low} up to {@code high} over its line there: four corners, round it.
     */
    private static Point3D[] edgeFace(final Roadway road, final int j, final double t1, final double across,
                                      final java.util.function.DoubleUnaryOperator low,
                                      final java.util.function.DoubleUnaryOperator high) {
        final Point3D[] corners = new Point3D[4];
        final double[] ts = {0, t1, t1, 0};
        for (int k = 0; k < 4; k++) {
            final double t = ts[k];
            final Vec2 p0 = road.at(j, across);
            final Vec2 p1 = road.at(j + 1, across);
            final double up = road.level(j, t) + (k < 2 ? low.applyAsDouble(t) : high.applyAsDouble(t));
            corners[k] = new Point3D(p0.x() + (p1.x() - p0.x()) * t, -up, p0.z() + (p1.z() - p0.z()) * t);
        }
        return corners;
    }

    /**
     * A face across the road's {@code j}th segment {@code t} of the way along, from {@code a} to {@code b} across, from
     * {@code low} up to {@code high} over its line there, at either edge: four corners, round it.
     */
    private static Point3D[] endFace(final Roadway road, final int j, final double t, final double a, final double b,
                                     final java.util.function.Function<Boolean, Double> low,
                                     final java.util.function.Function<Boolean, Double> high) {
        final Point3D[] corners = new Point3D[4];
        final boolean[] outers = {false, true, true, false};
        for (int k = 0; k < 4; k++) {
            final boolean outer = outers[k];
            final double across = outer ? b : a;
            final Vec2 p0 = road.at(j, across);
            final Vec2 p1 = road.at(j + 1, across);
            final double up = road.level(j, t) + (k < 2 ? low.apply(outer) : high.apply(outer));
            corners[k] = new Point3D(p0.x() + (p1.x() - p0.x()) * t, -up, p0.z() + (p1.z() - p0.z()) * t);
        }
        return corners;
    }

    /** Its structures: decks, parapets, piers and seats; tunnels' linings. {@code ground} is what the piers stand in. */
    public static Group structures(final Roadway road, final Profile ground) {
        final MeshBuilder built = new MeshBuilder();
        final Faces slab = new Faces();
        for (final Roadway.Structure s : road.structures()) {
            final Roadway.Stretch stretch = s.stretch();
            if (stretch.carrier() == Carrier.STRUCTURE) {
                deck(road, stretch, built, slab);
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
        if (!slab.empty()) {
            group.getChildren().add(slab.view(STRUCTURE));
        }
        return group;
    }

    /**
     * Faces put together a quad at a time, each its four corners its own, each wound so it is seen from its outside
     * only: drawn with the back of every face left out ({@link CullFace#BACK}).
     */
    private static final class Faces {

        /**
         * Which way round a face is wound to be seen from its outside, as JavaFX has it: its corners going round it so,
         * the turn from its first edge to its second points out of it, or (-1) into it.
         */
        private static final int WINDING = 1;

        private float[] points = new float[3 * 1_024];
        private float[] normals = new float[3 * 256];
        private int[] faces = new int[9 * 512];
        private int pointCount;
        private int normalCount;
        private int faceCount;
        /** Where no face is put: its middle there. */
        private final Covered covered;

        Faces() {
            this(NOWHERE);
        }

        Faces(final Covered covered) {
            this.covered = covered;
        }

        boolean empty() {
            return faceCount == 0;
        }

        /**
         * A face through {@code corners} round it, seen from {@code outward}: four corners; or, eight, a box's, its top
         * four (as {@link #along} makes them, the top at bit 1).
         */
        void quad(final Point3D[] corners, final Point3D outward) {
            Point3D[] c = corners.length == 8 ? new Point3D[] {corners[2], corners[3], corners[7], corners[6]} : corners;
            // where something else is drawn already, nothing
            if (covered.at((c[0].getX() + c[1].getX() + c[2].getX() + c[3].getX()) / 4,
                    (c[0].getZ() + c[1].getZ() + c[2].getZ() + c[3].getZ()) / 4)) {
                return;
            }
            final Point3D turn = c[1].subtract(c[0]).crossProduct(c[2].subtract(c[0]));
            if (turn.dotProduct(outward) * WINDING < 0) {
                c = new Point3D[] {c[0], c[3], c[2], c[1]};
            }
            if (3 * (pointCount + 4) > points.length) {
                points = java.util.Arrays.copyOf(points, points.length * 2);
            }
            if (3 * (normalCount + 1) > normals.length) {
                normals = java.util.Arrays.copyOf(normals, normals.length * 2);
            }
            if (9 * (faceCount + 2) > faces.length) {
                faces = java.util.Arrays.copyOf(faces, faces.length * 2);
            }
            // lit as it faces: outward, its own way, not as worked out from the triangles' points
            final Point3D n = outward.normalize();
            normals[3 * normalCount] = (float) n.getX();
            normals[3 * normalCount + 1] = (float) n.getY();
            normals[3 * normalCount + 2] = (float) n.getZ();
            final int normal = normalCount++;
            final int base = pointCount;
            for (final Point3D p : c) {
                points[3 * pointCount] = (float) p.getX();
                points[3 * pointCount + 1] = (float) p.getY();
                points[3 * pointCount + 2] = (float) p.getZ();
                pointCount++;
            }
            triangle(base, base + 1, base + 2, normal);
            triangle(base, base + 2, base + 3, normal);
        }

        private void triangle(final int a, final int b, final int c, final int normal) {
            final int at = 9 * faceCount;
            faces[at] = a;
            faces[at + 1] = normal;
            faces[at + 2] = a;
            faces[at + 3] = b;
            faces[at + 4] = normal;
            faces[at + 5] = b;
            faces[at + 6] = c;
            faces[at + 7] = normal;
            faces[at + 8] = c;
            faceCount++;
        }

        MeshView view(final PhongMaterial material) {
            final TriangleMesh mesh = new TriangleMesh(javafx.scene.shape.VertexFormat.POINT_NORMAL_TEXCOORD);
            mesh.getPoints().setAll(points, 0, 3 * pointCount);
            mesh.getNormals().setAll(normals, 0, 3 * normalCount);
            // a corner of its own for each point, as far into the haze as it is: none, till it is hazed
            final float[] coords = new float[2 * pointCount];
            java.util.Arrays.fill(coords, (float) Haze.CLEAR);
            mesh.getTexCoords().setAll(coords);
            mesh.getFaces().setAll(faces, 0, 9 * faceCount);
            final MeshView view = new MeshView(mesh);
            view.setMaterial(Haze.of(material));
            view.setCullFace(CullFace.BACK);
            return view;
        }
    }

    /** {@code materials}' faces for {@code stuff}, begun if there are none yet, none where {@code covered}. */
    private static Faces faces(final Map<Course.Stuff, Faces> materials, final Course.Stuff stuff,
                               final Covered covered) {
        return materials.computeIfAbsent(stuff, s -> new Faces(covered));
    }

    /** Where something else is drawn already, at (x, z): another road's pavement, say. */
    @FunctionalInterface
    public interface Covered {
        boolean at(double x, double z);
    }

    /** Nowhere. */
    public static final Covered NOWHERE = (x, z) -> false;

    /**
     * A deck under the formation of its paved strips, as deep as the road says ({@link Roadway#deck}) under its lowest edge,
     * tilted or not; a parapet at each edge of them. Its ground strips not on it: there is no ground there.
     */
    private static void deck(final Roadway road, final Roadway.Stretch stretch, final MeshBuilder built,
                             final Faces slab) {
        // a structure all round a loop has no ends to be seen
        final boolean round = road.loop() && stretch.from() == 0 && stretch.to() == road.size() - 1;
        for (int j = stretch.from(); j < stretch.to(); j++) {
            final Section here = road.section(j);
            final Section there = road.section(j + 1);
            final double under = Math.min(lowest(here), lowest(there)) - road.deck(j);
            final double west = -here.pavedEdge(-1);
            final double east = here.pavedEdge(1);
            final Vec2 right = road.right(j);
            // its slab as it is seen: its underside, its sides; its top is under the pavement
            slab.quad(along(road, j, 0, 1, west, east, (outer, top, t) -> under), new Point3D(0, 1, 0));
            for (final int side : new int[] {-1, 1}) {
                final double edge = side > 0 ? east : west;
                slab.quad(edgeFace(road, j, 1, edge, t -> under,
                        t -> lerp(here.formation(edge), there.formation(edge), t)),
                        flat(new Vec2(right.x() * side, right.z() * side)));
            }
            for (final boolean atEnd : new boolean[] {false, true}) {
                if (round || (atEnd ? j != stretch.to() - 1 : j != stretch.from())) {
                    continue;
                }
                final double t = atEnd ? 1 : 0;
                final Vec2 ahead = heading(road, j);
                slab.quad(endFace(road, j, t, west, east, outer -> under,
                        outer -> lerp(here.formation(outer ? east : west), there.formation(outer ? east : west), t)),
                        flat(atEnd ? ahead : new Vec2(-ahead.x(), -ahead.z())));
            }
            for (final int side : new int[] {-1, 1}) {
                final double edge = side * here.pavedEdge(side);
                built.box(along(road, j, 0, 1, edge, edge + side * Roadway.PARAPET_THICK, (outer, top, t) -> top
                        ? lerp(here.top(edge), there.top(edge), t) + Roadway.PARAPET : under));
            }
        }
    }

    /** How low its surface is at the lowest edge of its paved strips, against its line: its deck hangs from there. */
    private static double lowest(final Section section) {
        double lowest = 0;
        for (final Section.Laid l : section.paved()) {
            lowest = Math.min(lowest, Math.min(l.innerTop(), l.outerTop()));
        }
        return lowest;
    }

    /** A pier at its {@code i}th point: a crosshead under the deck, one column or two down into the ground. */
    private static void pier(final Roadway road, final int i, final Profile ground, final MeshBuilder built) {
        final Section section = road.section(Math.min(i, road.size() - 2));
        final double l = -section.pavedEdge(-1);
        final double r0 = section.pavedEdge(1);
        final double left = (l + r0) / 2 - (r0 - l) / 2 * 0.9;
        final double right = (l + r0) / 2 + (r0 - l) / 2 * 0.9;
        final double under = road.level(i) + lowest(road.section(i)) - road.deck(i);
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
            final double under = road.level(end) - road.deck(end == stretch.from() ? end : end - 1);
            built.box(box(road.point(end), r, h, -section.pavedEdge(-1) - Roadway.PARAPET_THICK,
                    section.pavedEdge(1) + Roadway.PARAPET_THICK, Math.min(0, back * SEAT_BACK), Math.max(0, back * SEAT_BACK),
                    under - SEAT, under));
        }
    }

    /** A tunnel's lining: a wall each side off its pavement's edges, a roof over its headroom. */
    private static void lining(final Roadway road, final Roadway.Stretch stretch, final MeshBuilder built) {
        for (int j = stretch.from(); j < stretch.to(); j++) {
            final Section section = road.section(j);
            final double l = -section.pavedEdge(-1) - TUNNEL_SIDE;
            final double r = section.pavedEdge(1) + TUNNEL_SIDE;
            final double low = section.formation(section.pavedSide()) - 100;
            final double high = Roadway.HEADROOM;
            built.box(corners(road, j, 1, l - Roadway.LINING, l, low, high, low, high));
            built.box(corners(road, j, 1, r, r + Roadway.LINING, low, high, low, high));
            built.box(corners(road, j, 1, l - Roadway.LINING, r + Roadway.LINING, high, high + Roadway.LINING,
                    high, high + Roadway.LINING));
        }
    }

    /**
     * The walls the roads want where the ground between them steps too much for a slope ({@link Roadworks#wanted}),
     * as they would stand: along a road's edge, from the lower ground to the higher, a footing deep. Drawn in a warning
     * colour: wanted, not yet built.
     */
    public static Group walls(final Roadworks works) {
        final MeshBuilder built = new MeshBuilder();
        for (final Roadworks.Wall w : works.wanted()) {
            final Roadway road = works.roads().get(w.road());
            final int from = road.nearestPoint(w.from());
            final int to = Math.min(road.nearestPoint(w.to()), road.size() - 1);
            for (int i = from; i < Math.max(to, from + 1) && i + 1 < road.size(); i++) {
                final double edge = w.side() * road.section(i).edge(w.side());
                final double[] a = works.span(w, i);
                final double[] b = works.span(w, i + 1);
                final double low = Math.min(a[0], b[0]) - FOOTING - road.level(i);
                final double high = Math.max(a[1], b[1]) - road.level(i);
                built.box(corners(road, i, 1, edge, edge + w.side() * WALL_THICK, low, high, low, high));
            }
        }
        final Group group = new Group();
        if (!built.empty()) {
            group.getChildren().add(built.view(WALL));
        }
        return group;
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
        return along(road, j, t0, t1, a, b, (outer, top, t) -> outer ? top ? bHigh : bLow : top ? aHigh : aLow);
    }

    /** How high a box's corner is over the road's line, at its inner edge or its {@code outer}, its bottom or its {@code top}, {@code t} of the way along. */
    @FunctionalInterface
    private interface Rise {
        double at(boolean outer, boolean top, double t);
    }

    /**
     * A box along the road's {@code j}th segment, from {@code t0} to {@code t1} of the way, across from {@code a} to
     * {@code b}; each corner as high over the line there as {@code rise} says.
     */
    private static Point3D[] along(final Roadway road, final int j, final double t0, final double t1, final double a,
                                   final double b, final Rise rise) {
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
            final double up = road.level(j, t) + rise.at(outer, top, t);
            corners[k] = new Point3D(x, -up, z);
        }
        return corners;
    }

    private static double lerp(final double a, final double b, final double t) {
        return a + (b - a) * t;
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
