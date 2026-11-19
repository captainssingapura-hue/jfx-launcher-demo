package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.lifts.Box;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.lifts.HighRiseBank;
import com.example.fxsuite.avatar.lifts.Mover;
import com.example.fxsuite.avatar.lifts.Profile;
import com.example.fxsuite.avatar.lifts.SceneWorld;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An office tower, as a description: the outline of its floors ({@code outline}, any that does not cross itself, cm
 * about its middle, across and along); each storey's floor, from the lobby's on the ground up, the roof last
 * ({@code levels}); what each storey is ({@code uses}: lobby, office, plant, or anything else said of it); its core's
 * solid parts (stairs, risers, toilets: {@code service}, each a square from the ground to the roof); its lifts, each
 * bank set where it stands in the core and turned to face its lift lobby ({@code banks}); and its crown over its roof.
 *
 * <p>Everything else is worked out from that, storey by storey ({@link #built}): each floor on the outline, cut where
 * a bank's shafts go through it; its glass round each storey over a spandrel hiding the floor's edge, the lobby's
 * front open in the middle as its way in; its columns round its edge; its core; its banks' shafts and their moving
 * cars and doors; its crown. Storey by storey, so it can be cut away over any floor to be looked into.
 *
 * <p>A tower made from a few figures, the same for any: {@link #generic}. A real one is its own description, its
 * outline and its core as its plans have them.
 *
 * @param name    what it is called
 * @param outline its floors' outline, about its middle
 * @param levels  each storey's floor, the lobby's first (on the ground), the roof's last
 * @param uses    what each storey is, the roof's left out
 * @param service its core's solid parts, each a square (least, most across; least, most along)
 * @param banks   its lifts, each bank where it stands
 * @param crown   how high its crown stands over its roof
 */
public record OfficeTower(String name, List<double[]> outline, List<Double> levels, List<String> uses,
                          List<double[]> service, List<Placed> banks, double crown) {

    /** A floor so thick; the spandrel over each storey's glass so deep, so thick; the glass so thick. */
    static final double SLAB = 30;
    static final double SPANDREL = 90;
    static final double SPANDREL_THICK = 10;
    static final double GLASS = 4;
    /** Its glass in panes no wider than this, each broken on its own. */
    static final double PANE = 450;
    /** Its columns: so wide, so far in from its edge, so far apart along it. */
    static final double COLUMN = 80;
    static final double INSET = 150;
    static final double SPACING = 900;
    /** The lobby's way in: so wide, in the middle of its front. */
    static final double ENTRANCE = 400;
    /** A lift lobby between two banks facing each other, so wide. */
    static final double LIFT_LOBBY = 330;

    public OfficeTower {
        outline = outline.stream().map(double[]::clone).toList();
        levels = List.copyOf(levels);
        uses = List.copyOf(uses);
        service = service.stream().map(double[]::clone).toList();
        banks = List.copyOf(banks);
        if (outline.size() < 3 || levels.size() < 2 || uses.size() != levels.size() - 1) {
            throw new IllegalArgumentException(name + ": an outline, storeys and a roof, each storey's use");
        }
    }

    /** How many storeys it has. */
    public int storeys() {
        return levels.size() - 1;
    }

    /** How high its roof is. */
    public double roof() {
        return levels.getLast();
    }

    /**
     * A tower from a few figures: its floors {@code wide} across and {@code deep} along, two opposite corners cut
     * ({@code cut} each way), another notched ({@code notch} each way); a lobby {@code lobby} high and {@code floors}
     * storeys over it {@code storey} apart; its office floors in {@code zones} zones, each served by a bank of
     * {@code cars} cars, half facing half across its own lift lobby, the zones' lobbies side by side across its core,
     * the higher zones faster, up to {@code fastest}; its core's stairs and services along its south side.
     */
    public static OfficeTower generic(final double wide, final double deep, final double cut, final double notch,
                                      final double lobby, final int floors, final double storey, final int zones,
                                      final int cars, final double fastest) {
        final double a = wide / 2;
        final double l = deep / 2;
        final List<double[]> outline = new ArrayList<>();
        // anticlockwise from the south-west, x east, z north: the south-west and north-east corners cut, the
        // north-west notched
        outline.add(new double[] {-a + cut, -l});
        outline.add(new double[] {a, -l});
        outline.add(new double[] {a, l - cut});
        outline.add(new double[] {a - cut, l});
        if (notch > 0) {
            outline.add(new double[] {-a + notch, l});
            outline.add(new double[] {-a + notch, l - notch});
            outline.add(new double[] {-a, l - notch});
        } else {
            outline.add(new double[] {-a, l});
        }
        outline.add(new double[] {-a, -l + cut});
        if (cut <= 0) {
            outline.removeLast();
            outline.set(0, new double[] {-a, -l});
        }
        final List<Double> levels = new ArrayList<>(List.of(0.0));
        final List<String> uses = new ArrayList<>(List.of("lobby"));
        for (int f = 1; f <= floors; f++) {
            levels.add(lobby + (f - 1) * storey);
            uses.add("office");
        }
        levels.add(lobby + floors * storey);
        // the office floors split evenly among the zones, the lowest from the first over the lobby
        final List<Placed> banks = new ArrayList<>();
        final HighRiseBank probe = bank(levels, 1, 1, Math.max(1, cars / 2), 400);
        final Placed shape = new Placed(probe, 0, 0, 90);
        final double block = 2 * shape.depth() + LIFT_LOBBY;
        final double coreWide = zones * block;
        for (int k = 0; k < zones; k++) {
            final int from = 1 + k * floors / zones;
            final int to = (k + 1) * floors / zones;
            final double speed = Math.max(250, fastest * (k + 1) / zones);
            final double middle = -coreWide / 2 + (k + 0.5) * block;
            final int west = (cars + 1) / 2;
            final int east = cars / 2;
            banks.add(new Placed(bank(levels.subList(0, to + 1), from, to, west, speed),
                    middle - LIFT_LOBBY / 2 - shape.depth() / 2, 0, 90));
            if (east > 0) {
                banks.add(new Placed(bank(levels.subList(0, to + 1), from, to, east, speed),
                        middle + LIFT_LOBBY / 2 + shape.depth() / 2, 0, -90));
            }
        }
        final double longest = banks.stream().mapToDouble(Placed::length).max().orElse(0);
        final List<double[]> service = List.of(new double[] {-coreWide / 2, coreWide / 2, -longest / 2 - 700,
            -longest / 2});
        return new OfficeTower("office tower", outline, levels, uses, service, banks, 600);
    }

    /** A bank of {@code cars} cars serving the lobby and floors {@code from} to {@code to}, at {@code speed}. */
    static HighRiseBank bank(final List<Double> levels, final int from, final int to, final int cars,
                             final double speed) {
        return new HighRiseBank(0, 0, levels, from, to, cars, new Profile(speed, 100, 150), 200, 170, 260, 100, 3.5);
    }

    /** Its floor at storey {@code f} (1 the first over the lobby; the roof the last): cut where its banks go through. */
    List<Solid> floor(final int f) {
        final List<double[]> holes = new ArrayList<>();
        for (final Placed b : banks) {
            if (f <= b.bank().zoneTo()) {
                holes.add(b.hole());
            }
        }
        final double top = levels.get(f);
        return Plates.floor(f == storeys() ? "roof" : "floor " + f, outline, holes, top - SLAB, top, Role.BASE,
                Material.PRECAST);
    }

    /**
     * Storey {@code f}'s own solids (0 the lobby's): its glass and spandrels round it, its columns, its core's service,
     * the floor over it; the lobby's front left open in its middle as its way in.
     */
    List<Solid> storey(final int f) {
        final List<Solid> solids = new ArrayList<>();
        final double from = levels.get(f);
        final double to = levels.get(f + 1);
        final int front = front();
        for (int e = 0; e < outline.size(); e++) {
            final double glassTop = to - SLAB - SPANDREL;
            if (f == 0 && e == front) {
                solids.addAll(entrance(e, from, glassTop));
            } else {
                solids.addAll(Plates.panes("glass", outline, e, 0, 1, PANE, GLASS, from, glassTop, Role.GLAZING,
                        Material.GLASS));
            }
            solids.add(Plates.edge("spandrel", outline, e, SPANDREL_THICK, glassTop, to, Role.WALL, Material.METAL));
        }
        for (final double[] c : columns()) {
            solids.add(new Box("column", Role.STRUCTURE, Material.PRECAST, c[0] - COLUMN / 2, c[0] + COLUMN / 2,
                    c[1] - COLUMN / 2, c[1] + COLUMN / 2, from, to - SLAB).solid());
        }
        for (final double[] s : service) {
            solids.add(new Box("core", Role.WALL, Material.PRECAST, s[0], s[1], s[2], s[3], from, to - SLAB).solid());
        }
        solids.addAll(floor(f + 1));
        return solids;
    }

    /** The lobby's front, glass either side of its way in. */
    private List<Solid> entrance(final int e, final double from, final double top) {
        final double[] p = outline.get(e);
        final double[] q = outline.get((e + 1) % outline.size());
        final double length = Math.hypot(q[0] - p[0], q[1] - p[1]);
        final double t = (length - ENTRANCE) / 2 / length;
        final List<Solid> glass = new ArrayList<>(
                Plates.panes("glass", outline, e, 0, t, PANE, GLASS, from, top, Role.GLAZING, Material.GLASS));
        glass.addAll(Plates.panes("glass", outline, e, 1 - t, 1, PANE, GLASS, from, top, Role.GLAZING, Material.GLASS));
        return glass;
    }

    /** Which edge of its outline is its front: the one furthest along, its way in in the lobby there. */
    int front() {
        int best = 0;
        double most = Double.NEGATIVE_INFINITY;
        for (int e = 0; e < outline.size(); e++) {
            final double[] p = outline.get(e);
            final double[] q = outline.get((e + 1) % outline.size());
            final double length = Math.hypot(q[0] - p[0], q[1] - p[1]);
            final double score = (p[1] + q[1]) / 2 + length / 100;
            if (Math.abs(q[1] - p[1]) < 1 && length > ENTRANCE + 200 && score > most) {
                most = score;
                best = e;
            }
        }
        return best;
    }

    /** Where its columns stand: along each edge, {@link #SPACING} apart, {@link #INSET} in from it. */
    List<double[]> columns() {
        final List<double[]> columns = new ArrayList<>();
        final double round = Math.signum(Plates.area(outline));
        for (int e = 0; e < outline.size(); e++) {
            final double[] p = outline.get(e);
            final double[] q = outline.get((e + 1) % outline.size());
            final double length = Math.hypot(q[0] - p[0], q[1] - p[1]);
            if (length < SPACING) {
                continue;
            }
            final double nx = -(q[1] - p[1]) / length * INSET * round;
            final double nz = (q[0] - p[0]) / length * INSET * round;
            final int count = (int) Math.floor(length / SPACING);
            final double start = (length - (count - 1) * SPACING) / 2;
            for (int k = 0; k < count; k++) {
                final double s = (start + k * SPACING) / length;
                columns.add(new double[] {p[0] + (q[0] - p[0]) * s + nx, p[1] + (q[1] - p[1]) * s + nz});
            }
        }
        return columns;
    }

    /** Its crown: a parapet round its roof, so high. */
    List<Solid> crownSolids() {
        final List<Solid> solids = new ArrayList<>();
        for (int e = 0; e < outline.size(); e++) {
            solids.add(Plates.edge("crown", outline, e, SPANDREL_THICK, roof(), roof() + crown, Role.WALL,
                    Material.METAL));
        }
        return solids;
    }

    /** Its banks' shafts, all of them. */
    List<Solid> shafts() {
        final List<Solid> solids = new ArrayList<>();
        banks.forEach(b -> solids.addAll(b.shafts(SLAB)));
        return solids;
    }

    /** All its moving parts: its banks' cars and doors, drawn by the scene's keys. */
    List<Mover> movers() {
        final List<Mover> movers = new ArrayList<>();
        banks.forEach(b -> movers.addAll(b.movers(SceneWorld.KEY + 1, SceneWorld.KEY + 2, SceneWorld.KEY + 3)));
        return movers;
    }

    /**
     * All of it up to storey {@code upTo}, the floor over that left off so it is looked into from over it (all of it,
     * roof and crown, if that is its last or more); glass if asked.
     */
    List<Solid> solids(final int upTo, final boolean glass) {
        final List<Solid> solids = new ArrayList<>();
        final int last = Math.min(upTo, storeys() - 1);
        final List<Solid> lid = last < storeys() - 1 ? floor(last + 1) : List.of();
        for (int f = 0; f <= last; f++) {
            storey(f).stream().filter(s -> (glass || s.material() != Material.GLASS) && !lid.contains(s))
                    .forEach(solids::add);
        }
        final double cut = levels.get(last + 1);
        shafts().stream().filter(s -> s.corners().stream().allMatch(p -> p.up() <= cut + 1)).forEach(solids::add);
        if (last == storeys() - 1) {
            solids.addAll(crownSolids());
            shafts().stream().filter(s -> s.corners().stream().anyMatch(p -> p.up() > cut + 1)).forEach(solids::add);
        }
        return solids;
    }

    /** How big its floor at storey {@code f} is, m², its holes left out. */
    double area(final int f) {
        return Plates.area(floor(f), true);
    }

    /** What it comes to, read out. */
    String figures() {
        final StringBuilder out = new StringBuilder(String.format(Locale.ROOT,
                "%s: %d storeys, its roof %.1f m up, its crown %.1f m over it; a typical floor %.0f m² (gross, the"
                        + " core's shafts left out)",
                name, storeys(), roof() / 100, crown / 100, area(Math.max(1, storeys() / 2))));
        for (final Placed p : banks) {
            final HighRiseBank b = p.bank();
            out.append(String.format(Locale.ROOT, "%n  %d cars, floors %d to %d, %.1f m/s: the lobby to floor %d in"
                            + " %.0f s, a car every %.0f s", b.cars(), b.zoneFrom(), b.zoneTo(),
                    b.profile().speed() / 100, b.zoneFrom(), b.profile().time(levels.get(b.zoneFrom())),
                    b.interval()));
        }
        return out.toString();
    }

    /**
     * It as a scene: drawn up to the floor over storey {@code upTo}, glass if asked; played whole, its glass pane by
     * pane apart from its frame, to be broken into.
     */
    Built built(final int upTo, final boolean glass) {
        final List<Solid> drawn = solids(upTo, glass);
        final List<Solid> whole = solids(storeys(), false);
        final List<List<Solid>> panes = solids(storeys(), true).stream().filter(s -> s.material() == Material.GLASS)
                .map(List::of).toList();
        final HighRiseBank any = banks.getFirst().bank();
        final List<Solid> car = any.car().stream().map(Box::solid).toList();
        final List<Solid> carLeaf = any.carLeaf().stream().map(Box::solid).toList();
        final List<Solid> landingLeaf = any.landingLeaf().stream().map(Box::solid).toList();
        double reach = 0;
        for (final double[] p : outline) {
            reach = Math.max(reach, Math.max(Math.abs(p[0]), Math.abs(p[1])));
        }
        final double[] f = outline.get(front());
        final double[] g = outline.get((front() + 1) % outline.size());
        final double frontZ = (f[1] + g[1]) / 2;
        final double frontX = (f[0] + g[0]) / 2;
        final double size = Math.max(2 * reach, roof() + crown);
        return new Built(name, figures(), drawn, whole, movers(), List.of(whole, car, carLeaf, landingLeaf),
                new Spot("before its way in", new Place(frontX, frontZ + 600, 0, 180)),
                new Place(-reach, frontZ + 1_400, 0, 90), new Place(frontX + ENTRANCE, frontZ + 300, 0, 180),
                size + 4_000, new double[] {0, roof() / 2, 0}, size, 145, panes);
    }
}
