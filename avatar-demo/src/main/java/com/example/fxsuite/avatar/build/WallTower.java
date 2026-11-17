package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Lean;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Placed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tower borne by walls, not columns: walls across it, one after another along its length, each the same shape,
 * the floors spanning between them, run out past the rooms as balconies, a glass rail at their edge, the rooms'
 * sliding doors set back inside them, four leaves, the middle two sliding open over the outer two; the walls' ends showing as fins between the balconies.
 * Its shape is one section, across it, carried along it: two legs from the ground, the west one all but upright, its
 * face curving out a little as it rises, more toward the top; the east one splayed out at its foot and curving in as it rises, to meet the
 * west one over an atrium between them; above where they meet, one shaft to the top. The storey where they meet
 * holds the trusses that tie them, and is shut, a band of wall round the tower.
 *
 * <p>Everything is worked out from its size ({@link Size}): where each face is at any height ({@link #westOuter},
 * {@link #westInner}, {@link #eastInner}, {@link #eastOuter}); and from those, each wall a storey at a time, each
 * floor between each two walls, each pane of glass, as solids.
 *
 * <p>In centimetres, in its own frame: across (x) its section, west to east; along (y) its length; up. Its shaft's
 * middle at across 0, its length's middle at along 0.
 */
public final class WallTower {

    /** What it is called. */
    public static final String NAME = "wall tower";

    /** How thick its floors are; its glass; how deep its balconies, how high and thick their rails. */
    static final double FLOOR = 30;
    static final double GLASS = 10;
    static final double BALCONY = 160;
    static final double RAIL = 110;
    static final double RAILING = 5;
    /**
     * Its balconies' sliding doors: how far apart their two tracks are, how deep their sill and head, how wide each
     * leaf's stiles, how far the leaves lap over each other shut; how many of the doors stand open, some way or wide.
     */
    static final double TRACK = 12;
    static final double SILL = 6;
    static final double HEAD = 12;
    static final double STILE = 6;
    static final double LAP = 8;
    static final double OPEN = 0.3;
    /** The doorway through each wall within it, for the corridor along each leg's middle: how wide, how high. */
    static final double CORRIDOR = 160;
    static final double DOORWAY = 240;

    /**
     * How big it is built: {@code storeys} storeys of {@code storey} each, its legs meeting under storey {@code merge}
     * (counting from 0, the ground floor); {@code length} long, with {@code walls} walls across it, end ones
     * included, {@code wall} thick, the two at its ends {@code end} thick; its west leg {@code west} wide, its face
     * curving out by {@code curve} at its top, from upright at its foot; its shaft {@code shaft} wide; its east leg {@code leg} wide across,
     * its foot {@code splay} further out east than its shaft.
     */
    public record Size(int storeys, double storey, int merge, double length, int walls, double wall, double end,
                       double west, double curve, double shaft, double leg, double splay) {

        /** As the bay's hotel towers stand, near enough: 55 storeys, about 198 m; the legs meeting at the 23rd. */
        public static final Size STANDARD = new Size(55, 360, 23, 6_000, 9, 60, 80, 1_100, 350, 2_400, 1_200, 3_400);

        public Size {
            check(storeys >= 10 && storeys <= 120, "from 10 to 120 storeys");
            check(storey >= 280 && storey <= 600, "storeys from 2.8 m to 6 m");
            check(merge >= 2 && merge < storeys - 1, "its legs meeting above the ground and below its top");
            check(walls >= 2 && walls <= 40, "from 2 to 40 walls along it");
            check(length >= 1_000 && (length - end) / (walls - 1) > wall + 200, "long enough for its walls, 2 m apart at least");
            check(wall >= 20 && wall <= 150 && end >= wall && end <= 200, "its walls from 20 cm to 1.5 m thick, its end walls no thinner");
            check(west >= 400 && west < shaft, "its west leg from 4 m wide, narrower than its shaft");
            check(curve >= 0 && curve <= 1_000, "its west face curving out up to 10 m");
            check(leg >= 400 && leg < shaft + splay - west, "its east leg from 4 m wide, room for an atrium at its foot");
            check(splay > 0, "its east leg splayed out");
        }

        private static void check(final boolean ok, final String what) {
            if (!ok) {
                throw new IllegalArgumentException("a wall tower is built " + what);
            }
        }

        /** How high it is, to its roof. */
        public double high() {
            return storeys * storey;
        }

        /** How high its legs meet: the floor of storey {@link #merge}. */
        public double meet() {
            return merge * storey;
        }
    }

    private final Size size;

    private WallTower(final Size size) {
        this.size = size;
    }

    public static WallTower of(final Size size) {
        return new WallTower(size);
    }

    public static WallTower standard() {
        return of(Size.STANDARD);
    }

    public Size size() {
        return size;
    }

    // --- its section: where each face is, at any height ---------------------------------------------------------

    /** Its west face, {@code up} high: upright at its foot, curving out west as it rises, more toward its top. */
    public double westOuter(final double up) {
        final double s = clamp(up / size.high());
        return -size.shaft() / 2 - size.curve() * s * s;
    }

    /** The west leg's inner face, toward the atrium, {@code up} high: as wide as the leg from its west face. */
    public double westInner(final double up) {
        return westOuter(up) + size.west();
    }

    /**
     * Its east face, {@code up} high: splayed out at its foot, curving in as it rises, steepening as it does, till it
     * stands straight where the legs meet; above, the shaft's.
     */
    public double eastOuter(final double up) {
        final double s = clamp(up / size.meet());
        return size.shaft() / 2 + size.splay() * (1 - s) * (1 - s);
    }

    /** The east leg's inner face, toward the atrium, {@code up} high: as wide as the leg in from its east face. */
    public double eastInner(final double up) {
        return eastOuter(up) - size.leg();
    }

    /** Whether, {@code up} high, there are two legs with the atrium between them, not yet one shaft. */
    public boolean apart(final double up) {
        return up < size.meet() && eastInner(up) > westInner(up);
    }

    private static double clamp(final double t) {
        return Math.clamp(t, 0, 1);
    }

    // --- along it ----------------------------------------------------------------------------------------------

    /** Where its wall {@code j} stands along it, its middle. */
    double wallAt(final int j) {
        final double first = -size.length() / 2 + size.end() / 2;
        return first + j * (size.length() - size.end()) / (size.walls() - 1);
    }

    /** How thick its wall {@code j} is: its end walls thicker. */
    double thick(final int j) {
        return j == 0 || j == size.walls() - 1 ? size.end() : size.wall();
    }

    // --- as a building ---------------------------------------------------------------------------------------

    /** Its palette: concrete walls and floors, glass between them, a stone foot. */
    public static Palette palette() {
        return new Palette("concrete and glass", Map.of(Role.WALL, Material.CONCRETE, Role.STRUCTURE, Material.CONCRETE,
                Role.BASE, Material.STONE, Role.GLAZING, Material.GLASS, Role.FRAME, Material.METAL,
                Role.ROOF, Material.CONCRETE, Role.DRESSING, Material.CONCRETE), 40, 40, 0);
    }

    /** It as a building to dress ({@link Building}): its skeleton, and all of it made as its palette says. */
    public Building building() {
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        choices.put("Walls", List.of(
                Building.Option.choosing("Concrete", Map.of(), Map.of(Role.WALL, Material.CONCRETE, Role.STRUCTURE, Material.CONCRETE)),
                Building.Option.choosing("Precast", Map.of(), Map.of(Role.WALL, Material.PRECAST, Role.STRUCTURE, Material.PRECAST)),
                Building.Option.choosing("Limestone", Map.of(), Map.of(Role.WALL, Material.LIMESTONE))));
        final Building.Structure all = (skeleton, palette) -> solids(palette);
        return new Building(NAME, skeleton(), List.of(), List.of(all), choices, List.of(palette()));
    }

    /**
     * Its skeleton: its section's outline at each end wall, a point every few storeys up each face; and the lines
     * along it, its four faces' feet, where its legs meet, its roof.
     */
    public Form skeleton() {
        final Form.Builder form = new Form.Builder(NAME);
        final int every = Math.max(1, size.storeys() / 11);
        for (final int j : new int[] {0, size.walls() - 1}) {
            final double y = wallAt(j);
            final List<Integer> outline = new ArrayList<>();
            // up the west face, down the east, then up the atrium's faces
            for (int k = 0; k < size.storeys(); k += every) {
                outline.add(form.vertex("end " + j + ", west " + k, westOuter(k * size.storey()), y, k * size.storey()));
            }
            outline.add(form.vertex("end " + j + ", roof west", westOuter(size.high()), y, size.high()));
            outline.add(form.vertex("end " + j + ", roof east", eastOuter(size.high()), y, size.high()));
            for (int k = size.merge(); k > 0; k -= every) {
                outline.add(form.vertex("end " + j + ", east " + k, eastOuter(k * size.storey()), y, k * size.storey()));
            }
            outline.add(form.vertex("end " + j + ", east foot", eastOuter(0), y, 0));
            outline.add(form.vertex("end " + j + ", east leg foot", eastInner(0), y, 0));
            outline.add(form.vertex("end " + j + ", atrium top", westInner(atriumTop()), y, atriumTop()));
            outline.add(form.vertex("end " + j + ", west leg foot", westInner(0), y, 0));
            form.loop(outline.stream().mapToInt(Integer::intValue).toArray());
        }
        // along it: where its faces stand on the ground, and its roof's edges
        for (final String corner : new String[] {"west 0", "east foot", "roof west", "roof east"}) {
            form.edge("end 0, " + corner, "end " + (size.walls() - 1) + ", " + corner);
        }
        return form.build();
    }

    /** How high the atrium reaches: where its two faces meet, found to a centimetre. */
    double atriumTop() {
        double low = 0;
        double high = size.meet();
        while (high - low > 1) {
            final double mid = (low + high) / 2;
            if (eastInner(mid) > westInner(mid)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /**
     * All of it, made as {@code palette} says: storey by storey, each leg's walls (or the shaft's), its floors between
     * each two walls, its glass on each face between them; the storey where its legs meet shut, wall where the glass
     * would be; its roof.
     */
    public List<Solid> solids(final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        final Material concrete = palette.of(Role.WALL);
        final Material floors = palette.of(Role.STRUCTURE);
        final Material glass = palette.of(Role.GLAZING);
        for (int k = 0; k < size.storeys(); k++) {
            final double h0 = k * size.storey();
            final double h1 = h0 + size.storey();
            final boolean shut = k == size.merge() - 1 || k == size.merge();
            for (final double[][] leg : legs(h0, h1)) {
                // leg[0]: its west face at h0, h1; leg[1]: its east face at h0, h1
                for (int j = 0; j < size.walls(); j++) {
                    final double y = wallAt(j);
                    final double t = thick(j);
                    final String name = "wall " + j + ", storey " + k;
                    if (j == 0 || j == size.walls() - 1) {
                        solids.add(box(name, Role.WALL, concrete, leg, y - t / 2, y + t / 2, h0, h1));
                        continue;
                    }
                    // a wall within it: a doorway through its middle, for the corridor along the leg
                    final double c = corridor(leg);
                    final double[] a0 = {c - CORRIDOR / 2, c - CORRIDOR / 2};
                    final double[] a1 = {c + CORRIDOR / 2, c + CORRIDOR / 2};
                    final double door = h0 + FLOOR + DOORWAY;
                    solids.add(box(name + ", west of the corridor", Role.WALL, concrete, new double[][] {leg[0], a0},
                            y - t / 2, y + t / 2, h0, h1));
                    solids.add(box(name + ", east of the corridor", Role.WALL, concrete, new double[][] {a1, leg[1]},
                            y - t / 2, y + t / 2, h0, h1));
                    solids.add(box(name + ", under the corridor", Role.WALL, concrete, new double[][] {a0, a1},
                            y - t / 2, y + t / 2, h0, h0 + FLOOR));
                    solids.add(box(name + ", over the corridor", Role.WALL, concrete, new double[][] {a0, a1},
                            y - t / 2, y + t / 2, door, h1));
                }
                for (int j = 0; j + 1 < size.walls(); j++) {
                    final double y0 = wallAt(j) + thick(j) / 2;
                    final double y1 = wallAt(j + 1) - thick(j + 1) / 2;
                    solids.add(box("floor " + j + ", storey " + k, Role.STRUCTURE, floors,
                            new double[][] {at(leg[0], h0, h0 + FLOOR, h0, h1), at(leg[1], h0, h0 + FLOOR, h0, h1)},
                            y0, y1, h0, h0 + FLOOR));
                    // its faces: a balcony, open, a glass rail at its edge and the rooms' glass and doors set back
                    // behind it; or, where it is shut, wall flush with its face
                    final double p0 = h0 + FLOOR;
                    for (final boolean east : new boolean[] {false, true}) {
                        final String side = east ? "east" : "west";
                        final double[] face = at(east ? leg[1] : leg[0], p0, h1, h0, h1);
                        if (shut) {
                            solids.add(box(side + " wall " + j + ", storey " + k, Role.WALL, concrete,
                                    layer(face, 0, GLASS, east), y0, y1, p0, h1));
                            continue;
                        }
                        solids.addAll(door(side + " door " + j + ", storey " + k, face, east, y0, y1, p0, h1,
                                opening(k, j, east), glass, palette.of(Role.FRAME)));
                        final double[] rail = at(face, p0, p0 + RAIL, p0, h1);
                        solids.add(box(side + " rail " + j + ", storey " + k, Role.GLAZING, glass,
                                layer(rail, 0, RAILING, east), y0, y1, p0, p0 + RAIL));
                    }
                }
            }
        }
        // its roof, over the shaft
        final double top = size.high();
        final double[][] roof = {{westOuter(top), westOuter(top)}, {eastOuter(top), eastOuter(top)}};
        solids.add(box("roof", Role.ROOF, palette.of(Role.ROOF), roof, -size.length() / 2, size.length() / 2, top, top + FLOOR));
        return solids;
    }

    /** Where the corridor runs along a leg ({@link #legs}), across: its middle, halfway up the storey. */
    static double corridor(final double[][] leg) {
        return (leg[0][0] + leg[0][1] + leg[1][0] + leg[1][1]) / 4;
    }

    /**
     * Its inside, cut into rooms by arithmetic: by storey, by leg, by the bay between two of its walls; the atrium
     * outside them all ({@link TowerInside}).
     */
    public TowerInside inside() {
        return new TowerInside(this);
    }

    /**
     * It set {@code at} a place in the world, as one thing in the world's way: every wall, floor, rail and pane of it
     * solid, as it leans, each filed in the rooms it reaches, and found through them ({@link Interior}).
     */
    public Interior interior(final Place at) {
        return new Interior(new Placed(at, inside()), solids(palette()).stream().map(WallTower::lean).toList());
    }

    /** A solid of it as what is in the way ({@link Lean}): each is a box, its two faces across it leaning as it does. */
    static Lean lean(final Solid solid) {
        final List<Point> c = solid.corners();
        // as {@link #box} lays its corners out: the first bit west or east, the second along, the third low or high
        return new Lean(0, 0, 0, c.get(0).along(), c.get(2).along(), c.get(0).across(), c.get(1).across(),
                c.get(4).across(), c.get(5).across(), c.get(0).up(), c.get(4).up());
    }

    /**
     * The sliding door at the back of a balcony, set back {@link #BALCONY} from face {@code face} (its across from
     * {@code p0} to {@code h1}), from {@code y0} to {@code y1} along: in a frame, a sill and a head, four leaves of
     * glass each between two stiles; the outer two fixed, on the track on the room's side, the middle two on the track
     * on the balcony's side, slid out over them toward the sides by {@code open} of a leaf's width.
     */
    List<Solid> door(final String name, final double[] face, final boolean east, final double y0, final double y1,
                     final double p0, final double h1, final double open, final Material glass, final Material metal) {
        final List<Solid> door = new ArrayList<>();
        final double back = BALCONY + TRACK + GLASS;
        door.add(box(name + ", sill", Role.FRAME, metal, layer(at(face, p0, p0 + SILL, p0, h1), BALCONY, back, east),
                y0, y1, p0, p0 + SILL));
        door.add(box(name + ", head", Role.FRAME, metal, layer(at(face, h1 - HEAD, h1, p0, h1), BALCONY, back, east),
                y0, y1, h1 - HEAD, h1));
        final double q = (y1 - y0) / 4;
        final double slide = Math.clamp(open, 0, 1) * q;
        // each leaf: from, to along; how far in from the face its track is
        final double[][] leaves = {
                {y0, y0 + q + LAP, BALCONY + TRACK},
                {y0 + q - slide, y0 + 2 * q + LAP / 2 - slide, BALCONY},
                {y0 + 2 * q - LAP / 2 + slide, y1 - q + slide, BALCONY},
                {y1 - q - LAP, y1, BALCONY + TRACK}};
        final double l0 = p0 + SILL;
        final double l1 = h1 - HEAD;
        final double[] lit = at(face, l0, l1, p0, h1);
        final String[] called = {"fixed left", "sliding left", "sliding right", "fixed right"};
        for (int n = 0; n < leaves.length; n++) {
            final double[][] track = layer(lit, leaves[n][2], leaves[n][2] + GLASS, east);
            final double a = leaves[n][0];
            final double b = leaves[n][1];
            door.add(box(name + ", " + called[n] + " stile", Role.FRAME, metal, track, a, a + STILE, l0, l1));
            door.add(box(name + ", " + called[n] + " glass", Role.GLAZING, glass, track, a + STILE, b - STILE, l0, l1));
            door.add(box(name + ", " + called[n] + " far stile", Role.FRAME, metal, track, b - STILE, b, l0, l1));
        }
        return door;
    }

    /**
     * How far open the door of balcony {@code j} of storey {@code k} is, on its east face or its west: most shut, some
     * open a little or wide, as the rooms behind them are lived in; the same every time, worked out from where it is.
     */
    static double opening(final int k, final int j, final boolean east) {
        final double h = com.example.fxsuite.avatar.sim.Num.hash(k, j, east ? 1 : 0, 0x3A11);
        return h < 1 - OPEN ? 0 : (h - (1 - OPEN)) / OPEN;
    }

    /**
     * Its legs from {@code h0} up to {@code h1}, west to east: each its west face and its east face, each at the two
     * heights; two, with the atrium between them, where they stand apart; one, the shaft, where they have met.
     */
    List<double[][]> legs(final double h0, final double h1) {
        final double middle = (h0 + h1) / 2;
        if (apart(middle)) {
            return List.of(
                    new double[][] {{westOuter(h0), westOuter(h1)}, {westInner(h0), westInner(h1)}},
                    new double[][] {{eastInner(h0), eastInner(h1)}, {eastOuter(h0), eastOuter(h1)}});
        }
        return List.<double[][]>of(new double[][] {{westOuter(h0), westOuter(h1)}, {eastOuter(h0), eastOuter(h1)}});
    }

    /**
     * A layer inside face {@code face} (its across at two heights), from {@code from} to {@code to} in from it: into
     * the tower, west from an east face ({@code east}), east from a west one; as a box's west face and east face.
     */
    private static double[][] layer(final double[] face, final double from, final double to, final boolean east) {
        final double sign = east ? -1 : 1;
        final double[] near = {face[0] + sign * from, face[1] + sign * from};
        final double[] far = {face[0] + sign * to, face[1] + sign * to};
        return east ? new double[][] {far, near} : new double[][] {near, far};
    }

    /** A face's across at {@code h0} and {@code h1}, from where it is at {@code f0} and {@code f1} ({@code face}). */
    private static double[] at(final double[] face, final double h0, final double h1, final double f0, final double f1) {
        final double s0 = (h0 - f0) / (f1 - f0);
        final double s1 = (h1 - f0) / (f1 - f0);
        return new double[] {face[0] + (face[1] - face[0]) * s0, face[0] + (face[1] - face[0]) * s1};
    }

    /**
     * A box from its west face ({@code faces[0]}, across at {@code h0} and {@code h1}) to its east face
     * ({@code faces[1]}), from {@code y0} to {@code y1} along it, {@code h0} to {@code h1} up: leaning as its faces do.
     */
    private static Solid box(final String name, final Role role, final Material material, final double[][] faces,
                             final double y0, final double y1, final double h0, final double h1) {
        final List<Point> corners = new ArrayList<>(8);
        for (int c = 0; c < 8; c++) {
            final double[] face = (c & 1) == 0 ? faces[0] : faces[1];
            final boolean high = (c & 4) != 0;
            corners.add(new Point(high ? face[1] : face[0], (c & 2) == 0 ? y0 : y1, high ? h1 : h0));
        }
        return new Solid(name, role, material, corners);
    }
}
