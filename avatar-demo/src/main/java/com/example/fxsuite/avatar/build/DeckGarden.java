package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Outline;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A garden on a tower's void deck ({@link Tower.Section#open()}): the roof garden's parts but its
 * pavilion, put among the deck's columns. A swimming pool sunk into the deck's floor, in a bay as
 * near the open part's middle as there is room for; beside it a bar (its counter, the shelves behind
 * it, its stools) and a watermelon stall (its table of melons and pineapples under a striped
 * awning); round them, plants in planters, as tall as the deck lets them be. All of it clear of the
 * columns, the enclosed part, the railing and each other.
 */
public final class DeckGarden {

    /** How far in from the deck's edge anything stands; how far clear of a column, of each other. */
    public static final double EDGE = 120;
    public static final double CLEAR = 40;
    public static final double APART = 150;
    /** The pool: so long and so wide (in a bay). */
    public static final double POOL_LONG = 560;
    public static final double POOL_WIDE = 320;
    /** The bar's room, and the stall's: what they stand on, counter, stools and all. */
    private static final double BAR_WIDE = 440;
    private static final double BAR_DEEP = 320;
    private static final double STALL_WIDE = 320;
    private static final double STALL_DEEP = 260;
    /** How high a planter is, and how far under the deck's soffit the plants keep. */
    private static final double PLANTER = 50;
    private static final double HEADROOM = 40;

    private final Tower tower;
    private final int deck;
    private final double floor;
    private final double height;
    private final List<Vec2> outline;
    private final RoofGarden.Pool pool;
    private final Place bar;
    private final Place stall;
    private final List<RoofGarden.Plant> plants;

    private DeckGarden(final Tower tower, final int deck, final RoofGarden.Pool pool, final Place bar, final Place stall,
                       final List<RoofGarden.Plant> plants) {
        this.tower = tower;
        this.deck = deck;
        this.floor = tower.base(deck);
        this.height = tower.sections().get(deck).tall();
        this.outline = vec(tower.sections().get(deck).plan().layout().shaped().getFirst());
        this.pool = pool;
        this.bar = bar;
        this.stall = stall;
        this.plants = List.copyOf(plants);
    }

    private static List<Vec2> vec(final List<Plan.Pt> points) {
        return points.stream().map(p -> new Vec2(p.x(), p.y())).toList();
    }

    /** A garden on {@code tower}'s first void deck. */
    public static DeckGarden of(final Tower tower) {
        int deck = -1;
        for (int s = 0; s < tower.sections().size() && deck < 0; s++) {
            if (tower.sections().get(s).open()) {
                deck = s;
            }
        }
        if (deck < 1) {
            throw new IllegalArgumentException(tower.name() + " has no void deck over a section to sink a pool into");
        }
        final Tower.Section section = tower.sections().get(deck);
        final Room room = new Room(vec(section.plan().layout().shaped().getFirst()),
                section.enclosed() == null ? null : vec(section.enclosed().layout().shaped().getFirst()),
                TowerBuildings.deckColumns(section).stream().map(c -> new Vec2(c.x(), c.y())).toList());
        final Vec2 middle = room.middle();
        // the pool: in a bay, as near the open part's middle as it fits
        final RoofGarden.Pool pool = room.nearest(middle, POOL_LONG / 2 + RoofGarden.RIM, POOL_WIDE / 2 + RoofGarden.RIM,
                List.of()).map(r -> new RoofGarden.Pool(r.x(), r.z(), r.halfAcross() - RoofGarden.RIM,
                        r.halfAlong() - RoofGarden.RIM)).orElseThrow(() -> new IllegalStateException(
                        tower.name() + ": no room on its void deck for a pool"));
        final List<RoofGarden.Pool> taken = new ArrayList<>(List.of(pool.grown(RoofGarden.RIM)));
        // the bar and the stall, each as near the pool as there is room, clear of it and of each other
        final Place bar = room.place(new Vec2(pool.x(), pool.z()), BAR_WIDE / 2, BAR_DEEP / 2, taken, pool);
        taken.add(rect(bar, BAR_WIDE / 2, BAR_DEEP / 2));
        final Place stall = room.place(new Vec2(pool.x(), pool.z()), STALL_WIDE / 2, STALL_DEEP / 2, taken, pool);
        taken.add(rect(stall, STALL_WIDE / 2, STALL_DEEP / 2));
        return new DeckGarden(tower, deck, pool, bar, stall, room.plant(tower, section.tall() - HEADROOM - PLANTER, taken));
    }

    /** What a thing at {@code at}, {@code wide} across and {@code deep} along it each way, takes up: as a pool would. */
    private static RoofGarden.Pool rect(final Place at, final double wide, final double deep) {
        final boolean turned = Math.abs(Math.sin(Math.toRadians(at.yaw()))) > 0.5;
        return new RoofGarden.Pool(at.x(), at.z(), turned ? deep : wide, turned ? wide : deep);
    }

    /** The deck's open part: its outline, its enclosed part's (if it has one), its columns. */
    private record Room(List<Vec2> outline, List<Vec2> enclosed, List<Vec2> columns) {

        /** Where in it is roughly its middle: the middle of its open points. */
        Vec2 middle() {
            double x = 0;
            double z = 0;
            int n = 0;
            for (final double[] p : spots(100)) {
                x += p[0];
                z += p[1];
                n++;
            }
            return new Vec2(x / n, z / n);
        }

        /** Its open points, {@code step} apart. */
        List<double[]> spots(final double step) {
            final List<double[]> spots = new ArrayList<>();
            final double[] box = {outline.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                    outline.stream().mapToDouble(Vec2::z).min().orElseThrow(),
                    outline.stream().mapToDouble(Vec2::x).max().orElseThrow(),
                    outline.stream().mapToDouble(Vec2::z).max().orElseThrow()};
            for (double x = box[0]; x <= box[2]; x += step) {
                for (double z = box[1]; z <= box[3]; z += step) {
                    if (Outline.inside(outline, x, z) && (enclosed == null || !Outline.inside(enclosed, x, z))) {
                        spots.add(new double[] {x, z});
                    }
                }
            }
            return spots;
        }

        /**
         * Whether a rectangle about ({@code x}, {@code z}), {@code a} across and {@code b} along each way,
         * stands in it clear of its edge, its enclosed part, its columns and {@code taken}.
         */
        boolean fits(final double x, final double z, final double a, final double b, final List<RoofGarden.Pool> taken) {
            final RoofGarden.Pool r = new RoofGarden.Pool(x, z, a, b);
            final List<Vec2> clear = r.outline(EDGE);
            for (final Vec2 c : clear) {
                if (!Outline.inside(outline, c.x(), c.z())) {
                    return false;
                }
            }
            if (outline.stream().anyMatch(p -> r.gap(p.x(), p.z()) < EDGE)) {
                return false;
            }
            if (enclosed != null) {
                final List<Vec2> near = r.outline(CLEAR + 30);
                if (near.stream().anyMatch(c -> Outline.inside(enclosed, c.x(), c.z()))
                        || enclosed.stream().anyMatch(p -> Outline.inside(near, p.x(), p.z()))
                        || Outline.inside(enclosed, x, z)) {
                    return false;
                }
            }
            if (columns.stream().anyMatch(c -> r.gap(c.x(), c.z()) < TowerBuildings.DECK_COLUMN / 2 + CLEAR)) {
                return false;
            }
            return taken.stream().noneMatch(t -> overlap(r, t, APART));
        }

        /** The rectangle {@code a} by {@code b} (either way round) that fits nearest {@code to}, clear of {@code taken}. */
        java.util.Optional<RoofGarden.Pool> nearest(final Vec2 to, final double a, final double b,
                                                    final List<RoofGarden.Pool> taken) {
            RoofGarden.Pool best = null;
            double least = Double.POSITIVE_INFINITY;
            for (final double[] p : spots(50)) {
                for (final boolean across : new boolean[] {true, false}) {
                    final double ha = across ? a : b;
                    final double hb = across ? b : a;
                    final double d = Math.hypot(p[0] - to.x(), p[1] - to.z());
                    if (d < least && fits(p[0], p[1], ha, hb, taken)) {
                        least = d;
                        best = new RoofGarden.Pool(p[0], p[1], ha, hb);
                    }
                }
            }
            return java.util.Optional.ofNullable(best);
        }

        /**
         * Where a thing {@code wide} across its front and {@code deep} goes: as near {@code to} as it
         * fits, turned to face {@code facing} (the pool) square on.
         */
        Place place(final Vec2 to, final double wide, final double deep, final List<RoofGarden.Pool> taken,
                    final RoofGarden.Pool facing) {
            final RoofGarden.Pool r = nearest(to, wide, deep, taken).orElseThrow(() -> new IllegalStateException(
                    "no room on the void deck"));
            final boolean turned = Math.abs(r.halfAcross() - wide) > 1e-6;
            final double dx = facing.x() - r.x();
            final double dz = facing.z() - r.z();
            // facing along whichever of its own two ways points nearer the pool
            final double yaw = turned ? (dx >= 0 ? 90 : 270) : (dz >= 0 ? 0 : 180);
            return new Place(r.x(), r.z(), 0, yaw);
        }

        /** Plants in planters round the rest of it, each no taller than {@code headroom}; the same every time. */
        List<RoofGarden.Plant> plant(final Tower tower, final double headroom, final List<RoofGarden.Pool> taken) {
            final Random random = new Random(tower.name().hashCode() * 31 + 7);
            final List<RoofGarden.Plant> plants = new ArrayList<>();
            for (final double[] p : spots(600)) {
                // sparing: room to walk between them, the deck open
                if (random.nextDouble() < 0.35) {
                    continue;
                }
                final double px = p[0] + (random.nextDouble() - 0.5) * 120;
                final double pz = p[1] + (random.nextDouble() - 0.5) * 120;
                final double pick = random.nextDouble();
                RoofGarden.Kind kind = pick < 0.4 ? RoofGarden.Kind.TREE : pick < 0.6 ? RoofGarden.Kind.PINE
                        : RoofGarden.Kind.SHRUB;
                double size = switch (kind) {
                    case TREE -> 90 + random.nextDouble() * 50;
                    case PINE -> 60 + random.nextDouble() * 25;
                    case SHRUB -> 40 + random.nextDouble() * 25;
                };
                // as tall as the deck lets it be: a tree's crown tops out at three of its sizes, a pine's at four
                final double tall = kind == RoofGarden.Kind.TREE ? 2.9 : kind == RoofGarden.Kind.PINE ? 4 : 1.1;
                if (size * tall > headroom) {
                    kind = RoofGarden.Kind.SHRUB;
                    size = Math.min(size, headroom / 1.1);
                }
                final double reach = size * 0.8 + 20;
                final RoofGarden.Plant plant = new RoofGarden.Plant(kind, px, pz, size);
                if (!fits(px, pz, reach, reach, taken)
                        || plants.stream().anyMatch(q -> Math.hypot(q.x() - px, q.z() - pz) < (q.size() + plant.size()) * 1.2)) {
                    continue;
                }
                plants.add(plant);
            }
            return plants;
        }
    }

    /** Whether rectangles {@code a} and {@code b}, each grown by {@code apart}, overlap. */
    private static boolean overlap(final RoofGarden.Pool a, final RoofGarden.Pool b, final double apart) {
        return Math.abs(a.x() - b.x()) < a.halfAcross() + b.halfAcross() + apart
                && Math.abs(a.z() - b.z()) < a.halfAlong() + b.halfAlong() + apart;
    }

    public Tower tower() {
        return tower;
    }

    public RoofGarden.Pool pool() {
        return pool;
    }

    public Place bar() {
        return bar;
    }

    public Place stall() {
        return stall;
    }

    public List<RoofGarden.Plant> plants() {
        return plants;
    }

    /** The deck's floor; its pool's floor. */
    public double floor() {
        return floor;
    }

    public double poolFloor() {
        return floor - RoofGarden.DEEP;
    }

    // ---------------------------------------------------------------- composed

    /** The tower, dressed as {@code tower} dresses it, its deck's floor paved round its pool, with the garden on it. */
    public Building on(final Building tower) {
        final String under = this.tower.sections().get(deck - 1).name() + ", roof";
        final List<Plate> plates = tower.plates().stream().filter(p -> !p.name().equals(under)).toList();
        final List<Building.Structure> structure = new ArrayList<>(tower.structure());
        structure.add((skeleton, palette) -> paving(palette));
        structure.add((skeleton, palette) -> solids());
        return new Building(tower.name(), tower.skeleton(), tower.surfaces(), plates, tower.sheets(), structure,
                tower.choices(), tower.palettes());
    }

    /** The deck's floor: the slab under it, paved, the pool's opening cut out of it. */
    private List<Solid> paving(final Palette palette) {
        final List<Vec3> around = outline.stream().map(p -> new Vec3(p.x(), p.z(), floor)).toList();
        final List<List<Vec3>> holes = List.of(pool.outline(0).stream().map(p -> new Vec3(p.x(), p.z(), floor)).toList());
        final Sheet deckFloor = Sheet.flat("deck floor", "deck floor", around, holes, new Vec3(0, 0, 1));
        final List<Solid> solids = new ArrayList<>(deckFloor.slab("deck slab", palette, Role.STRUCTURE, -30, -3));
        solids.addAll(deckFloor.slab("deck paving", Palette.STONE.with(Map.of(Role.BASE, Material.LIMESTONE)), Role.BASE,
                -3, 0));
        return solids;
    }

    /** The tower in the way as it is shaped, the pool's basin cut down into the section under its deck, with the garden. */
    public List<Volume> on(final List<Volume> tower) {
        final List<Vec2> under = vec(this.tower.sections().get(deck - 1).plan().layout().shaped().getFirst());
        final List<Volume> all = new ArrayList<>();
        for (final Volume volume : tower) {
            if (volume instanceof Prism p && p.top() == floor && p.outline().equals(under)) {
                all.add(new Prism(under, p.bottom(), poolFloor()));
                all.add(new Prism(under, List.of(pool.outline(0)), poolFloor(), p.top()));
            } else {
                all.add(volume);
            }
        }
        all.addAll(shape());
        return all;
    }

    /** The garden's solids, in the tower's frame: its pool, its bar, its stall, its plants in their planters. */
    public List<Solid> solids() {
        final List<Solid> solids = new ArrayList<>(RoofGarden.poolSolids(pool, floor));
        solids.addAll(barSolids());
        solids.addAll(stallSolids());
        int k = 0;
        for (final RoofGarden.Plant plant : plants) {
            final double r = plant.size() * 0.8;
            solids.add(RoofGarden.box("planter " + k, Material.CONCRETE, plant.x() - r, plant.x() + r, plant.z() - r,
                    plant.z() + r, floor, floor + PLANTER));
            solids.add(RoofGarden.box("planter " + k + ", soil", Material.GRASS, plant.x() - r + 6, plant.x() + r - 6,
                    plant.z() - r + 6, plant.z() + r - 6, floor + PLANTER, floor + PLANTER + 1));
            solids.addAll(RoofGarden.plantSolids(plant, "plant " + k++, floor + PLANTER));
        }
        return solids;
    }

    /** A box at {@code at}, from {@code u0} to {@code u1} across its front and {@code v0} to {@code v1} back to front, up from the deck. */
    private Solid local(final String name, final Material material, final Place at, final double u0, final double u1,
                        final double v0, final double v1, final double low, final double high) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final double u = (c & 1) == 0 ? u0 : u1;
            final double v = (c & 2) == 0 ? v0 : v1;
            corners.add(new Point(at.x() + right.x() * u + ahead.x() * v, at.z() + right.z() * u + ahead.z() * v,
                    floor + ((c & 4) == 0 ? low : high)));
        }
        return new Solid(name, Role.DRESSING, material, corners);
    }

    private Prism localPrism(final Place at, final double u0, final double u1, final double v0, final double v1,
                             final double low, final double high) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        final List<Vec2> footprint = new ArrayList<>();
        for (final double[] c : new double[][] {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}}) {
            footprint.add(new Vec2(at.x() + right.x() * c[0] + ahead.x() * c[1], at.z() + right.z() * c[0] + ahead.z() * c[1]));
        }
        return new Prism(Outline.twiceArea(footprint) < 0 ? footprint.reversed() : footprint, floor + low, floor + high);
    }

    /**
     * The bar, facing the pool (its front, where its stools are, ahead of it): its counter, timber under a
     * stone top; behind it, its shelves of bottles; before it, four stools.
     */
    private List<Solid> barSolids() {
        final List<Solid> solids = new ArrayList<>();
        solids.add(local("bar, counter", Material.TIMBER, bar, -200, 200, -10, 60, 0, 106));
        solids.add(local("bar, counter top", Material.LIMESTONE, bar, -205, 205, -15, 65, 106, 112));
        solids.add(local("bar, shelves", Material.TIMBER, bar, -180, 180, -160, -120, 0, 210));
        for (int shelf = 0; shelf < 3; shelf++) {
            final double up = 100 + shelf * 40;
            for (int b = 0; b < 9; b++) {
                final double u = -160 + b * 40;
                solids.add(local("bar, bottle " + shelf + "." + b, b % 3 == 0 ? Material.GLASS
                        : b % 3 == 1 ? Material.GREEN_GLAZED : Material.GLAZED, bar, u - 5, u + 5, -140, -130, up, up + 28));
            }
        }
        for (int s = 0; s < 4; s++) {
            final double u = -135 + s * 90;
            solids.add(local("bar, stool " + s + " post", Material.METAL, bar, u - 3, u + 3, 92, 98, 0, 72));
            solids.add(local("bar, stool " + s + " seat", Material.LACQUER, bar, u - 18, u + 18, 77, 113, 72, 78));
        }
        return solids;
    }

    /** The watermelon stall, facing the pool: its table of melons and pineapples, under a striped awning on two posts. */
    private List<Solid> stallSolids() {
        final List<Solid> solids = new ArrayList<>();
        solids.add(local("stall, table", Material.TIMBER, stall, -120, 120, -50, 50, 66, 72));
        for (final double[] leg : new double[][] {{-112, -42}, {112, -42}, {-112, 42}, {112, 42}}) {
            solids.add(local("stall, leg", Material.TIMBER, stall, leg[0] - 4, leg[0] + 4, leg[1] - 4, leg[1] + 4, 0, 66));
        }
        // the melons, in rows on the table; the pineapples at its end
        int m = 0;
        for (int row = 0; row < 3; row++) {
            for (int k = 0; k < 6; k++) {
                final double u = -100 + k * 30;
                final double v = -30 + row * 30;
                solids.add(local("stall, melon " + m, Material.LEAVES, stall, u - 13, u + 13, v - 11, v + 11, 72, 92));
                solids.add(local("stall, melon " + m++ + " stripe", Material.GREEN_GLAZED, stall, u - 4, u + 4, v - 11.5,
                        v + 11.5, 72.5, 92.5));
            }
        }
        for (int p = 0; p < 3; p++) {
            final double v = -30 + p * 30;
            solids.add(local("stall, pineapple " + p, Material.GLAZED, stall, 85, 101, v - 8, v + 8, 72, 98));
            solids.add(local("stall, pineapple " + p + " crown", Material.LEAVES, stall, 88, 98, v - 5, v + 5, 98, 112));
        }
        // its awning: two posts at its back, stripes red and white over the table
        for (final double u : new double[] {-135, 135}) {
            solids.add(local("stall, post", Material.METAL, stall, u - 3, u + 3, -78, -72, 0, 240));
        }
        for (int s = 0; s < 8; s++) {
            final double u = -140 + s * 35;
            solids.add(local("stall, awning " + s, s % 2 == 0 ? Material.LACQUER : Material.LIMESTONE, stall, u, u + 35,
                    -80, 80, 230, 236));
        }
        return solids;
    }

    /**
     * The garden in the way: the pool's rim; the bar's counter, its shelves and its stools; the stall's
     * table and its posts; each planter, and each tree's and pine's trunk out of it. The water is not, as yet.
     */
    public List<Volume> shape() {
        final List<Volume> shape = new ArrayList<>();
        shape.add(new Prism(pool.outline(RoofGarden.RIM), List.of(pool.outline(0)), floor,
                floor + RoofGarden.LAWN + RoofGarden.RIM_HIGH));
        shape.add(localPrism(bar, -205, 205, -15, 65, 0, 112));
        shape.add(localPrism(bar, -180, 180, -160, -120, 0, 210));
        for (int s = 0; s < 4; s++) {
            final double u = -135 + s * 90;
            shape.add(localPrism(bar, u - 18, u + 18, 77, 113, 0, 78));
        }
        shape.add(localPrism(stall, -120, 120, -50, 50, 0, 98));
        for (final double u : new double[] {-135, 135}) {
            shape.add(localPrism(stall, u - 3, u + 3, -78, -72, 0, 240));
        }
        for (final RoofGarden.Plant plant : plants) {
            final double r = plant.size() * 0.8;
            shape.add(new Prism(List.of(new Vec2(plant.x() - r, plant.z() - r), new Vec2(plant.x() + r, plant.z() - r),
                    new Vec2(plant.x() + r, plant.z() + r), new Vec2(plant.x() - r, plant.z() + r)), floor, floor + PLANTER));
        }
        return shape;
    }
}
