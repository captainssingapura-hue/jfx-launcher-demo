package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Outline;
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
 * near the open part's middle as there is room for; round it, plants in planters, sparing, as tall
 * as the deck lets them be. All of it clear of the columns, the enclosed part, the railing and each
 * other; and of two spots kept open, one for a bar, one for a melon stall ({@link Spot}): what is
 * set out there, and the people at them, are game objects, put on it in the game ({@link DeckScene}),
 * not built into it.
 */
public final class DeckGarden {

    /** How far in from the deck's edge anything stands; how far clear of a column, of each other. */
    public static final double EDGE = 120;
    public static final double CLEAR = 40;
    public static final double APART = 150;
    /** The pool: so long and so wide (in a bay). */
    public static final double POOL_LONG = 560;
    public static final double POOL_WIDE = 320;
    /** How high a planter is, and how far under the deck's soffit the plants keep. */
    private static final double PLANTER = 50;
    private static final double HEADROOM = 40;
    /** The room a bar takes, and a melon stall, each way from its middle: across, along (its front, -z, to the deck). */
    public static final double BAR_ACROSS = 150;
    public static final double BAR_ALONG = 130;
    public static final double STALL_ACROSS = 175;
    public static final double STALL_ALONG = 150;

    /**
     * A spot kept open on the deck, in the tower's frame: its middle ({@code x} across, {@code z}
     * along), and which way it is turned ({@code yaw}, as a place's), its front (its own -z) toward
     * the deck's middle, where people come from.
     */
    public record Spot(double x, double z, double yaw) {
    }

    private final Tower tower;
    private final int deck;
    private final double floor;
    private final double height;
    private final List<Vec2> outline;
    private final RoofGarden.Pool pool;
    private final List<RoofGarden.Plant> plants;
    private final Spot bar;
    private final Spot stall;

    private DeckGarden(final Tower tower, final int deck, final RoofGarden.Pool pool, final List<RoofGarden.Plant> plants,
                       final Spot bar, final Spot stall) {
        this.bar = bar;
        this.stall = stall;
        this.tower = tower;
        this.deck = deck;
        this.floor = tower.base(deck);
        this.height = tower.sections().get(deck).tall();
        this.outline = vec(tower.sections().get(deck).plan().layout().shaped().getFirst());
        this.pool = pool;
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
        // kept clear of: its enclosed part, its cores, its columns
        final List<List<Vec2>> enclosed = new ArrayList<>();
        if (section.enclosed() != null) {
            enclosed.add(vec(section.enclosed().layout().shaped().getFirst()));
        }
        tower.structure().cores().forEach(core -> enclosed.add(vec(core)));
        final Room room = new Room(vec(section.plan().layout().shaped().getFirst()), enclosed,
                TowerBuildings.deckColumns(tower, deck).stream()
                        .map(c -> new double[] {c.at().x(), c.at().y(), c.size()}).toList());
        final Vec2 middle = room.middle();
        // the pool: in a bay, as near the open part's middle as it fits
        final RoofGarden.Pool pool = room.nearest(middle, POOL_LONG / 2 + RoofGarden.RIM, POOL_WIDE / 2 + RoofGarden.RIM,
                List.of()).map(r -> new RoofGarden.Pool(r.x(), r.z(), r.halfAcross() - RoofGarden.RIM,
                        r.halfAlong() - RoofGarden.RIM)).orElseThrow(() -> new IllegalStateException(
                        tower.name() + ": no room on its void deck for a pool"));
        final List<RoofGarden.Pool> taken = new ArrayList<>(List.of(pool.grown(RoofGarden.RIM)));
        // the bar by the pool, as near it as there is room; the stall as near the open part's middle; either left out if there is none
        final RoofGarden.Pool barRoom = room.nearest(new Vec2(pool.x(), pool.z()), BAR_ACROSS, BAR_ALONG, taken).orElse(null);
        if (barRoom != null) {
            taken.add(barRoom);
        }
        final RoofGarden.Pool stallRoom = room.nearest(middle, STALL_ACROSS, STALL_ALONG, taken).orElse(null);
        if (stallRoom != null) {
            taken.add(stallRoom);
        }
        return new DeckGarden(tower, deck, pool, room.plant(tower, section.tall() - HEADROOM - PLANTER, taken),
                spot(barRoom, BAR_ACROSS, middle), spot(stallRoom, STALL_ACROSS, middle));
    }

    /**
     * The spot {@code room} is, if any, {@code across} each way across it as it is turned: square to
     * the deck's sides, its front toward {@code middle}.
     */
    private static Spot spot(final RoofGarden.Pool room, final double across, final Vec2 middle) {
        if (room == null) {
            return null;
        }
        if (Math.abs(room.halfAcross() - across) < 1e-9) {
            return new Spot(room.x(), room.z(), middle.z() < room.z() ? 0 : 180);
        }
        return new Spot(room.x(), room.z(), middle.x() < room.x() ? 90 : 270);
    }

    /** The deck's open part: its outline; what is enclosed in it (its enclosed part, its cores); its columns, each x, z and size. */
    private record Room(List<Vec2> outline, List<List<Vec2>> enclosed, List<double[]> columns) {

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
                    final double px = x;
                    final double pz = z;
                    if (Outline.inside(outline, px, pz) && enclosed.stream().noneMatch(e -> Outline.inside(e, px, pz))) {
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
            final List<Vec2> near = r.outline(CLEAR + 30);
            for (final List<Vec2> part : enclosed) {
                if (near.stream().anyMatch(c -> Outline.inside(part, c.x(), c.z()))
                        || part.stream().anyMatch(p -> Outline.inside(near, p.x(), p.z()))
                        || Outline.inside(part, x, z)) {
                    return false;
                }
            }
            if (columns.stream().anyMatch(c -> r.gap(c[0], c[1]) < c[2] * 0.75 + CLEAR)) {
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

    public List<RoofGarden.Plant> plants() {
        return plants;
    }

    /** Where its bar is to be set out; its melon stall: empty, if there was no room. */
    public java.util.Optional<Spot> bar() {
        return java.util.Optional.ofNullable(bar);
    }

    public java.util.Optional<Spot> stall() {
        return java.util.Optional.ofNullable(stall);
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

    /** The garden's solids, in the tower's frame: its pool, its plants in their planters. */
    public List<Solid> solids() {
        final List<Solid> solids = new ArrayList<>(RoofGarden.poolSolids(pool, floor));
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

    /**
     * The garden in the way: the pool's rim; each planter. The water is not, as yet.
     */
    public List<Volume> shape() {
        final List<Volume> shape = new ArrayList<>();
        shape.add(new Prism(pool.outline(RoofGarden.RIM), List.of(pool.outline(0)), floor,
                floor + RoofGarden.LAWN + RoofGarden.RIM_HIGH));
        for (final RoofGarden.Plant plant : plants) {
            final double r = plant.size() * 0.8;
            shape.add(new Prism(List.of(new Vec2(plant.x() - r, plant.z() - r), new Vec2(plant.x() + r, plant.z() - r),
                    new Vec2(plant.x() + r, plant.z() + r), new Vec2(plant.x() - r, plant.z() + r)), floor, floor + PLANTER));
        }
        return shape;
    }
}
