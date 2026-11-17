package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A hull to lie across the wall towers' roofs, a park on its deck: built as a ship is. One shape, its section at any
 * station along it, carried from its round stern to its pointed bow; and from that shape, everything: its keel along
 * its bottom; a frame at every station, rib-like, round its section; floors across its bottom at each, an inner bottom
 * over them; stringers along its bottom and sides, through the frames; a deck beam over each frame, stanchions under
 * them every few; a bulkhead across it every so many frames, its compartments; and its plating, its outer skin in
 * strakes from its keel to its deck edge (the garboard, the bottom, the bilge, the side, the sheer), its deck over all.
 *
 * <p>Its section ({@link #section}): a flat bottom, turning up round the bilge on a radius into an upright side, to its
 * deck. Along it, as wide as its beam through its middle body, narrowing to a point at its bow, rounding in at its
 * stern ({@link #half}); its bottom rising toward each end ({@link #keel}), so it is deepest under its middle. Its
 * deck level all along.
 *
 * <p>In centimetres, in its own frame: across (x), port to starboard; along (y), stern to bow; up, from the lowest of
 * its bottom. Its middle at across 0, along 0.
 */
public final class SkyHull {

    /** What it is called. */
    public static final String NAME = "sky hull";

    /** How thick its plating is; its deck's. */
    static final double PLATE = 3;
    static final double DECK = 5;
    /** How big its members are, each way: its keel, its frames, its stringers, its deck beams, its stanchions. */
    static final double KEEL = 50;
    static final double FRAME = 25;
    static final double STRINGER = 20;
    static final double BEAM = 30;
    static final double STANCHION = 20;
    /** How thick a floor is, an inner bottom, a bulkhead. */
    static final double SHEET = 2;
    /** A stanchion under every so many deck beams. */
    static final int STANCHIONS = 4;
    /** How many segments its section is drawn in, each side: across its bottom, round its bilge, up its side. */
    static final int BOTTOM = 4;
    static final int BILGE = 4;
    static final int SIDE = 2;

    /**
     * How big it is built: {@code length} long, stern to bow, {@code beam} wide, {@code depth} deep from its bottom to
     * its deck through its middle; its bilge turned on a radius of {@code bilge}; narrowing to its bow over its last
     * {@code bow} and to its stern over its first {@code stern}, its bottom rising by {@code rise} toward each end; a
     * frame every {@code frame}, a bulkhead every {@code bulkhead} frames, its double bottom {@code bottom} deep.
     */
    public record Size(double length, double beam, double depth, double bilge, double bow, double stern, double rise,
                       double frame, int bulkhead, double bottom) {

        /** As the park on the bay's hotel towers, near enough: 340 m long, 38 m wide, 7 m deep. */
        public static final Size STANDARD = new Size(34_000, 3_800, 700, 300, 9_000, 2_500, 400, 300, 8, 120);

        public Size {
            check(length >= 2_000 && length <= 100_000, "from 20 m to 1 km long");
            check(beam >= 400 && beam <= length / 3, "from 4 m wide, a third as wide as long at most");
            check(depth >= 200 && depth <= beam, "from 2 m deep, no deeper than wide");
            check(bilge > 0 && bilge < depth && bilge < beam / 2, "its bilge on a radius inside its depth and half its beam");
            check(bow > 0 && stern > 0 && bow + stern < length, "its ends narrowing within its length");
            check(rise >= 0 && rise < depth - bottom - 50, "its bottom rising, still deeper than its double bottom at its ends");
            check(frame >= 50 && frame <= length / 10, "framed at least ten times along");
            check(bulkhead >= 2, "a bulkhead every two frames or more");
            check(bottom > KEEL && bottom < depth / 2, "its double bottom deeper than its keel, less than half its depth");
        }

        private static void check(final boolean ok, final String what) {
            if (!ok) {
                throw new IllegalArgumentException("a sky hull is built " + what);
            }
        }

        /** How many frame spaces along it: its stations, one more. */
        public int spaces() {
            return (int) Math.round(length / frame);
        }
    }

    private final Size size;

    private SkyHull(final Size size) {
        this.size = size;
    }

    public static SkyHull of(final Size size) {
        return new SkyHull(size);
    }

    public static SkyHull standard() {
        return of(Size.STANDARD);
    }

    public Size size() {
        return size;
    }

    // --- its shape: how wide, how deep, its section, at any station ------------------------------------------------

    /** Where station {@code i} is along it: 0 its stern, {@link Size#spaces} its bow. */
    double station(final int i) {
        return -size.length() / 2 + i * size.length() / size.spaces();
    }

    /** How far into its bow ({@code > 0}) or its stern ({@code < 0}) {@code along} is, 0 to 1 at its end; 0 in its middle body. */
    private double into(final double along) {
        final double bowStarts = size.length() / 2 - size.bow();
        final double sternEnds = -size.length() / 2 + size.stern();
        if (along > bowStarts) {
            return Math.min(1, (along - bowStarts) / size.bow());
        }
        if (along < sternEnds) {
            return -Math.min(1, (sternEnds - along) / size.stern());
        }
        return 0;
    }

    /**
     * How wide it is each side of its middle, {@code along} it: half its beam through its middle body; to its bow,
     * narrowing ever faster to a point; to its stern, rounding in, as a circle does.
     */
    public double half(final double along) {
        final double t = into(along);
        final double share = t >= 0 ? 1 - t * t : Math.sqrt(Math.max(0, 1 - t * t));
        return size.beam() / 2 * share;
    }

    /** How high its bottom is at its middle line, {@code along} it: 0 under its middle body, rising toward each end. */
    public double keel(final double along) {
        final double t = into(along);
        return size.rise() * t * t;
    }

    /**
     * Its section {@code along} it, its starboard half, {@code in} in from its outer skin: from its middle line on its
     * bottom out across its flat bottom, round its bilge, up its side to its deck; each point {@code {across, up}}, as
     * many always ({@link #BOTTOM}, {@link #BILGE}, {@link #SIDE} segments). In from its skin, its bottom higher, its
     * side further in, its bilge on a radius as much less.
     */
    public double[][] section(final double along, final double in) {
        final double b = Math.max(0, half(along) - in);
        final double zb = keel(along) + in;
        final double d = size.depth();
        final double r = Math.max(0, Math.min(size.bilge() - in, Math.min(b, d - zb) * 0.9));
        final double[][] points = new double[BOTTOM + BILGE + SIDE + 1][];
        int n = 0;
        for (int k = 0; k <= BOTTOM; k++) {
            points[n++] = new double[] {(b - r) * k / BOTTOM, zb};
        }
        for (int k = 1; k <= BILGE; k++) {
            final double a = Math.PI / 2 * k / BILGE;
            points[n++] = new double[] {b - r + r * Math.sin(a), zb + r - r * Math.cos(a)};
        }
        for (int k = 1; k <= SIDE; k++) {
            points[n++] = new double[] {b, zb + r + (d - zb - r) * k / SIDE};
        }
        return points;
    }

    /** What each strake of its plating is called, by its segment of the section, from its keel out. */
    static String strake(final int j) {
        if (j == 0) {
            return "garboard strake";
        }
        if (j < BOTTOM) {
            return "bottom strake";
        }
        if (j < BOTTOM + BILGE) {
            return "bilge strake";
        }
        return j < BOTTOM + BILGE + SIDE - 1 ? "side strake" : "sheer strake";
    }

    // --- as a building -------------------------------------------------------------------------------------------

    /**
     * Its palette: its skin and its bulkheads painted plate, light; its frames, keel, stringers, beams and floors dark
     * steel; its deck planked.
     */
    public static Palette palette() {
        return new Palette("painted steel", Map.of(Role.WALL, Material.PRECAST, Role.DRESSING, Material.PRECAST,
                Role.BASE, Material.PRECAST, Role.STRUCTURE, Material.METAL, Role.FRAME, Material.METAL,
                Role.ROOF, Material.TIMBER, Role.GLAZING, Material.GLASS), 40, 40, 0);
    }

    /**
     * It as a building to dress ({@link Building}): its skeleton, and all of it made as its palette says; its plating
     * steel, or glass, all of it or its starboard side and deck, cut away, to see how it is built inside.
     */
    public Building building() {
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        choices.put("Plating", List.of(
                Building.Option.choosing("Steel", Map.of(), Map.of()),
                Building.Option.choosing("Cutaway", Map.of(), Map.of(Role.DRESSING, Material.GLASS, Role.ROOF,
                        Material.GLASS)),
                Building.Option.choosing("Glass", Map.of(), Map.of(Role.WALL, Material.GLASS, Role.DRESSING,
                        Material.GLASS, Role.ROOF, Material.GLASS))));
        final Building.Structure all = (skeleton, palette) -> solids(palette);
        return new Building(NAME, skeleton(), List.of(), List.of(all), choices, List.of(palette()));
    }

    /**
     * Its skeleton: its section at each bulkhead, a loop round it; its keel's line and its deck's edges along it, from
     * its stern to its bow, and its stem and its sternpost up from its keel to its deck at each end.
     */
    public Form skeleton() {
        final Form.Builder form = new Form.Builder(NAME);
        final int n = size.spaces();
        final List<Integer> keel = new ArrayList<>();
        final List<Integer> port = new ArrayList<>();
        final List<Integer> starboard = new ArrayList<>();
        final int sternFoot = form.vertex("stern, keel", 0, station(0), keel(station(0)));
        final int sternHead = form.vertex("stern, deck", 0, station(0), size.depth());
        form.edge(sternFoot, sternHead);
        keel.add(sternFoot);
        port.add(sternHead);
        starboard.add(sternHead);
        for (int i = size.bulkhead(); i < n; i += size.bulkhead()) {
            final double y = station(i);
            final double[][] s = section(y, 0);
            final List<Integer> loop = new ArrayList<>();
            loop.add(form.vertex("bulkhead " + i + ", keel", 0, y, s[0][1]));
            for (int j = 1; j < s.length; j++) {
                loop.add(form.vertex("bulkhead " + i + ", starboard " + j, s[j][0], y, s[j][1]));
            }
            for (int j = s.length - 1; j >= 1; j--) {
                loop.add(form.vertex("bulkhead " + i + ", port " + j, -s[j][0], y, s[j][1]));
            }
            form.loop(loop.stream().mapToInt(Integer::intValue).toArray());
            keel.add(loop.getFirst());
            // its deck's edges: the last of its starboard points, the first of its port ones
            starboard.add(loop.get(s.length - 1));
            port.add(loop.get(s.length));
        }
        final int bowFoot = form.vertex("bow, keel", 0, station(n), keel(station(n)));
        final int bowHead = form.vertex("bow, deck", 0, station(n), size.depth());
        form.edge(bowFoot, bowHead);
        keel.add(bowFoot);
        port.add(bowHead);
        starboard.add(bowHead);
        for (final List<Integer> line : List.of(keel, port, starboard)) {
            for (int k = 1; k < line.size(); k++) {
                form.edge(line.get(k - 1), line.get(k));
            }
        }
        return form.build();
    }

    /**
     * All of it, made as {@code palette} says: its keel; at each station its frame, its floor and its deck beam, a
     * stanchion under every few, a bulkhead every so many; between each two its inner bottom and its stringers; and its
     * plating, its skin strake by strake and its deck.
     */
    public List<Solid> solids(final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        final Material steel = palette.of(Role.STRUCTURE);
        final int n = size.spaces();
        final double inFrame = PLATE + FRAME / 2;
        for (int i = 0; i < n; i++) {
            final double y0 = station(i);
            final double y1 = station(i + 1);
            // its keel, along its bottom
            solids.add(member("keel " + i, 0, y0, keel(y0) + PLATE + KEEL / 2, 0, y1, keel(y1) + PLATE + KEEL / 2,
                    KEEL, Role.STRUCTURE, steel));
            // its stringers, through the frames: along its bottom, round its bilge, up its side
            final double[][] a = section(y0, PLATE + STRINGER / 2);
            final double[][] b = section(y1, PLATE + STRINGER / 2);
            for (final int j : new int[] {1, 2, 3, BOTTOM + BILGE / 2, BOTTOM + BILGE + 1}) {
                for (final int side : new int[] {-1, 1}) {
                    solids.add(member((side > 0 ? "starboard" : "port") + " stringer " + j + ", " + i,
                            flip(side, a[j][0]), y0, a[j][1], flip(side, b[j][0]), y1, b[j][1], STRINGER, Role.STRUCTURE, steel));
                }
            }
            // its inner bottom, over its floors, across its flat bottom
            final double[][] in0 = section(y0, 0);
            final double[][] in1 = section(y1, 0);
            final double w0 = in0[BOTTOM][0];
            final double w1 = in1[BOTTOM][0];
            final double z0 = keel(y0) + size.bottom();
            final double z1 = keel(y1) + size.bottom();
            solids.add(hexa("inner bottom " + i, Role.STRUCTURE, steel, new double[][] {
                    {-w0, y0, z0}, {w0, y0, z0}, {-w1, y1, z1}, {w1, y1, z1},
                    {-w0, y0, z0 + SHEET}, {w0, y0, z0 + SHEET}, {-w1, y1, z1 + SHEET}, {w1, y1, z1 + SHEET}}));
            // its plating: strake by strake, each side, its skin from this station to the next
            final double[][] o0 = section(y0, 0);
            final double[][] o1 = section(y1, 0);
            final double[][] i0 = section(y0, PLATE);
            final double[][] i1 = section(y1, PLATE);
            for (int j = 0; j + 1 < o0.length; j++) {
                for (final int side : new int[] {-1, 1}) {
                    // the port side and all the bottom one role, the starboard side above its bottom another: so it
                    // can be cut away on its own
                    final Role role = side > 0 && j >= BOTTOM ? Role.DRESSING : Role.WALL;
                    solids.add(hexa((side > 0 ? "starboard " : "port ") + strake(j) + " " + j + ", " + i, role,
                            palette.of(role), new double[][] {
                                    {flip(side, o0[j][0]), y0, o0[j][1]}, {flip(side, o0[j + 1][0]), y0, o0[j + 1][1]},
                                    {flip(side, o1[j][0]), y1, o1[j][1]}, {flip(side, o1[j + 1][0]), y1, o1[j + 1][1]},
                                    {flip(side, i0[j][0]), y0, i0[j][1]}, {flip(side, i0[j + 1][0]), y0, i0[j + 1][1]},
                                    {flip(side, i1[j][0]), y1, i1[j][1]}, {flip(side, i1[j + 1][0]), y1, i1[j + 1][1]}}));
                }
            }
            // its deck, plated over its beams, from its edge to its edge
            final double d = size.depth();
            final double h0 = half(y0);
            final double h1 = half(y1);
            solids.add(hexa("deck " + i, Role.ROOF, palette.of(Role.ROOF), new double[][] {
                    {-h0, y0, d}, {h0, y0, d}, {-h1, y1, d}, {h1, y1, d},
                    {-h0, y0, d + DECK}, {h0, y0, d + DECK}, {-h1, y1, d + DECK}, {h1, y1, d + DECK}}));
        }
        for (int i = 1; i < n; i++) {
            final double y = station(i);
            // its frame, round its section, each side
            final double[][] f = section(y, inFrame);
            for (int j = 0; j + 1 < f.length; j++) {
                for (final int side : new int[] {-1, 1}) {
                    solids.add(member("frame " + i + ", " + (side > 0 ? "starboard " : "port ") + j, flip(side, f[j][0]), y,
                            f[j][1], flip(side, f[j + 1][0]), y, f[j + 1][1], FRAME, Role.STRUCTURE, steel));
                }
            }
            // its floor, a plate across its flat bottom up to its inner bottom
            final double w = section(y, 0)[BOTTOM][0];
            solids.add(hexa("floor " + i, Role.STRUCTURE, steel, box(-w, w, y - SHEET / 2, y + SHEET / 2,
                    keel(y) + PLATE, keel(y) + size.bottom())));
            // its deck beam, across under its deck
            final double h = half(y) - PLATE;
            final double under = size.depth() - BEAM / 2;
            solids.add(member("deck beam " + i, -h, y, under, h, y, under, BEAM, Role.STRUCTURE, steel));
            // a stanchion each side under every few, from its inner bottom to its beam
            if (i % STANCHIONS == 0) {
                for (final int side : new int[] {-1, 1}) {
                    final double x = side * half(y) / 3;
                    solids.add(member("stanchion " + i + ", " + (side > 0 ? "starboard" : "port"), x, y,
                            keel(y) + size.bottom() + SHEET, x, y, size.depth() - BEAM, STANCHION, Role.STRUCTURE,
                            steel));
                }
            }
            // a bulkhead every so many: a wall across it, from its skin up to its deck
            if (i % size.bulkhead() == 0) {
                final double[][] s = section(y, PLATE);
                for (int j = 0; j + 1 < s.length; j++) {
                    if (Math.abs(s[j + 1][0] - s[j][0]) < 1) {
                        continue;  // its side, upright: nothing across between
                    }
                    for (final int side : new int[] {-1, 1}) {
                        final double y0 = y - SHEET / 2;
                        final double y1 = y + SHEET / 2;
                        final double top = size.depth();
                        solids.add(hexa("bulkhead " + i + ", " + (side > 0 ? "starboard " : "port ") + j, Role.BASE,
                                palette.of(Role.BASE), new double[][] {
                                        {flip(side, s[j][0]), y0, s[j][1]}, {flip(side, s[j + 1][0]), y0, s[j + 1][1]},
                                        {flip(side, s[j][0]), y1, s[j][1]}, {flip(side, s[j + 1][0]), y1, s[j + 1][1]},
                                        {flip(side, s[j][0]), y0, top}, {flip(side, s[j + 1][0]), y0, top},
                                        {flip(side, s[j][0]), y1, top}, {flip(side, s[j + 1][0]), y1, top}}));
                    }
                }
            }
        }
        return solids;
    }

    /** {@code across} on side {@code side}, -1 port, 1 starboard: its middle line 0, never -0, so the two sides meet there. */
    private static double flip(final int side, final double across) {
        return side * across + 0.0;
    }

    /** A square member {@code size} across, from one point to another. */
    private static Solid member(final String name, final double x0, final double y0, final double z0, final double x1,
                                final double y1, final double z1, final double size, final Role role,
                                final Material material) {
        return Building.member(name, new Form.Vertex("a", x0, y0, z0), new Form.Vertex("b", x1, y1, z1), size, role,
                material);
    }

    /** A box square to its frame. */
    private static double[][] box(final double x0, final double x1, final double y0, final double y1, final double z0,
                                  final double z1) {
        final double[][] c = new double[8][];
        for (int k = 0; k < 8; k++) {
            c[k] = new double[] {(k & 1) == 0 ? x0 : x1, (k & 2) == 0 ? y0 : y1, (k & 4) == 0 ? z0 : z1};
        }
        return c;
    }

    /** A solid of eight corners, each {@code {across, along, up}}: by bits, one way, another, a third. */
    private static Solid hexa(final String name, final Role role, final Material material, final double[][] corners) {
        final List<Point> points = new ArrayList<>(8);
        for (final double[] c : corners) {
            points.add(new Point(c[0], c[1], c[2]));
        }
        return new Solid(name, role, material, points);
    }
}
