package com.example.fxsuite.avatar.build;

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
 * A tower and a garden on its roof, composed: one building put on another. On the tower's roof, a
 * lawn; on it a pavilion ({@link Pavilion}), about half as wide as the roof if there is room, facing
 * the tower's front; beside it a swimming pool sunk into the roof; round them, trees and shrubs, as
 * many as the roof has room for, clear of the pavilion, its steps, the pool and the parapet.
 *
 * <p>Composed by placing: the pavilion's parts, its solids and its shape, put in the tower's own frame
 * on its roof, as the tower is put in the world; the plants likewise. The pool is cut down into the
 * tower's top: its basin out of the top of the tower's shape (a hole in it), its outline out of the
 * tower's roof as drawn. So the tower's piece carries all of it, placed and scaled together.
 */
public final class RoofGarden {

    /** A plant: a broad tree, a pine, or a shrub; where, and how big (its crown's radius). */
    public enum Kind { TREE, PINE, SHRUB }

    public record Plant(Kind kind, double x, double z, double size) {
    }

    /** A pool: its middle, half its length and width across ({@code x}) and along ({@code z}). */
    public record Pool(double x, double z, double halfAcross, double halfAlong) {

        /** Its outline, at {@code out} beyond its edges, anticlockwise. */
        public List<Vec2> outline(final double out) {
            final double a = halfAcross + out;
            final double b = halfAlong + out;
            return List.of(new Vec2(x - a, z - b), new Vec2(x + a, z - b), new Vec2(x + a, z + b), new Vec2(x - a, z + b));
        }

        /** It and its edges grown by {@code by} all round. */
        public Pool grown(final double by) {
            return new Pool(x, z, halfAcross + by, halfAlong + by);
        }

        /** How far ({@code px}, {@code pz}) is from it: 0 in it. */
        public double gap(final double px, final double pz) {
            return Math.hypot(Math.max(0, Math.abs(px - x) - halfAcross), Math.max(0, Math.abs(pz - z) - halfAlong));
        }
    }

    /** How thick the lawn is; how far in from the roof's edge anything stands (the parapet, and room by it). */
    public static final double LAWN = 5;
    public static final double EDGE = 120;
    /** The pool: how deep, how far under its rim its water is; its rim, how wide and how far over the lawn. */
    public static final double DEEP = 140;
    public static final double WATER = 25;
    public static final double RIM = 40;
    public static final double RIM_HIGH = 10;
    /** How far apart, at the least, the pavilion (its steps and all) and the pool's rim are. */
    private static final double APART = 150;

    private final Tower tower;
    private final Pavilion pavilion;
    private final Place at;
    private final Pool pool;
    private final List<Plant> plants;
    private final List<Vec2> roof;

    private RoofGarden(final Tower tower, final Pavilion pavilion, final Place at, final Pool pool,
                       final List<Plant> plants, final List<Vec2> roof) {
        this.tower = tower;
        this.pavilion = pavilion;
        this.at = at;
        this.pool = pool;
        this.plants = List.copyOf(plants);
        this.roof = List.copyOf(roof);
    }

    /**
     * A garden on {@code tower}'s roof, its pavilion of {@code kind} under a roof of {@code roofing},
     * tiled in {@code tiles} on pillars of {@code columns}, and its pool. The pavilion about half as
     * wide as the roof, the pool about two fifths as long and a fifth as wide; both smaller together,
     * a little at a time, until they fit side by side, each clear of the edge and of the other.
     */
    public static RoofGarden of(final Tower tower, final Pavilion.Footprint kind, final Pavilion.Roof roofing,
                                final Material tiles, final Material columns) {
        final List<Vec2> roof = tower.sections().getLast().plan().layout().shaped().getFirst().stream()
                .map(p -> new Vec2(p.x(), p.y())).toList();
        final double[] box = bounds(roof);
        final double narrowest = Math.min(box[2] - box[0], box[3] - box[1]);
        // its cores, standing up through the roof: kept clear of, as its edge is
        final List<List<Vec2>> cores = tower.structure().cores().stream()
                .map(core -> core.stream().map(p -> new Vec2(p.x(), p.y())).toList()).toList();
        // the points to stand them at, and how far each is from the roof's edge
        final List<double[]> spots = new ArrayList<>();
        for (double x = box[0]; x <= box[2]; x += 150) {
            for (double z = box[1]; z <= box[3]; z += 150) {
                if (Outline.inside(roof, x, z) && gap(cores, x, z) > 0) {
                    final Vec2 edge = Outline.nearest(roof, x, z);
                    spots.add(new double[] {x, z, Math.min(Math.hypot(edge.x() - x, edge.z() - z), gap(cores, x, z))});
                }
            }
        }
        for (double scale = 1; ; scale *= 0.92) {
            final double width = narrowest / 2 * scale;
            final Pavilion pavilion = Pavilion.of(spec(tower.name(), kind, roofing, tiles, columns, Math.max(300, width)));
            final double reach = pavilion.reach();
            final double length = narrowest * 0.42 * scale;
            final double wide = narrowest * 0.18 * scale;
            // the pools that fit, either way round, clear of the roof's edge
            final List<Pool> pools = new ArrayList<>();
            for (final double[] spot : spots) {
                for (final boolean across : new boolean[] {true, false}) {
                    final Pool pool = across ? new Pool(spot[0], spot[1], length / 2, wide / 2)
                            : new Pool(spot[0], spot[1], wide / 2, length / 2);
                    if (fits(roof, pool) && clearOf(cores, pool)) {
                        pools.add(pool);
                    }
                }
            }
            // the pavilion where there is room for it, and a pool clear of it: the one with most room to spare
            double best = Double.NEGATIVE_INFINITY;
            double[] where = null;
            Pool beside = null;
            for (final double[] spot : spots) {
                final double spare = spot[2] - reach - EDGE;
                if (spare < 0) {
                    continue;
                }
                for (final Pool pool : pools) {
                    final double apart = pool.gap(spot[0], spot[1]) - reach - RIM - APART;
                    if (apart < 0) {
                        continue;
                    }
                    final double score = Math.min(spare, apart) + spare * 1e-3;
                    if (score > best) {
                        best = score;
                        where = spot;
                        beside = pool;
                    }
                }
            }
            if (where != null || width <= 300) {
                if (where == null) {
                    throw new IllegalStateException(tower.name() + ": no room on its roof for a pavilion and a pool");
                }
                final Place at = new Place(where[0], where[1], tower.height() + LAWN, 0);
                return new RoofGarden(tower, pavilion, at, beside, plant(tower, roof, cores, new Vec2(where[0], where[1]), reach,
                        beside), roof);
            }
        }
    }

    /** How far ({@code x}, {@code z}) is from the nearest of {@code cores}: 0 in one. */
    private static double gap(final List<List<Vec2>> cores, final double x, final double z) {
        double gap = Double.POSITIVE_INFINITY;
        for (final List<Vec2> core : cores) {
            if (Outline.inside(core, x, z)) {
                return 0;
            }
            final Vec2 near = Outline.nearest(core, x, z);
            gap = Math.min(gap, Math.hypot(near.x() - x, near.z() - z));
        }
        return gap;
    }

    /** Whether {@code pool}, its rim and room round it, is clear of {@code cores}. */
    private static boolean clearOf(final List<List<Vec2>> cores, final Pool pool) {
        final List<Vec2> clear = pool.outline(RIM + EDGE);
        for (final List<Vec2> core : cores) {
            if (clear.stream().anyMatch(c -> Outline.inside(core, c.x(), c.z()))
                    || core.stream().anyMatch(p -> Outline.inside(clear, p.x(), p.z()))) {
                return false;
            }
        }
        return true;
    }

    /** Whether {@code pool}, its rim and room round it, is on the roof clear of its edge: none of its corners off it, none of the roof's in it. */
    private static boolean fits(final List<Vec2> roof, final Pool pool) {
        final List<Vec2> clear = pool.outline(RIM + EDGE);
        for (final Vec2 c : clear) {
            if (!Outline.inside(roof, c.x(), c.z())) {
                return false;
            }
        }
        for (int k = 0; k < clear.size(); k++) {
            final Vec2 a = clear.get(k);
            final Vec2 b = clear.get((k + 1) % clear.size());
            if (!Outline.inside(roof, (a.x() + b.x()) / 2, (a.z() + b.z()) / 2)) {
                return false;
            }
        }
        return roof.stream().noneMatch(p -> Outline.inside(clear, p.x(), p.z()));
    }

    /** A pavilion of {@code kind}, {@code width} across, proportioned to it: its pillars, its roof, its eaves. */
    private static Pavilion.Spec spec(final String tower, final Pavilion.Footprint kind, final Pavilion.Roof roofing,
                                      final Material tiles, final Material columns, final double width) {
        final boolean rectangle = kind == Pavilion.Footprint.RECTANGLE;
        final double depth = rectangle ? width * 0.55 : width;
        final int bays = rectangle ? (width > 1600 ? 5 : 3) : 1;
        final double thick = Math.clamp(width / 24, 25, 60);
        final double tall = Math.clamp(width * 0.28, 280, 650);
        final double pitch = roofing == Pavilion.Roof.FLAT ? 0 : 0.75;
        final double upturn = roofing == Pavilion.Roof.PYRAMID || roofing == Pavilion.Roof.HIP ? width * 0.06 : 0;
        return new Pavilion.Spec(tower + "'s roof pavilion", kind, width, depth, bays, Pavilion.Pillar.ROUND, thick, tall,
                roofing, pitch, width * 0.14, 30, tiles, columns, upturn);
    }

    /**
     * Plants round them, as the roof has room for: tried at points a few metres apart, shaken a little,
     * each kept if it is on the roof clear of its edge, of the pavilion, of the pool, and of the plants
     * before it. The same for the same tower, every time.
     */
    private static List<Plant> plant(final Tower tower, final List<Vec2> roof, final List<List<Vec2>> cores,
                                     final Vec2 middle, final double reach,
                                     final Pool pool) {
        final Random random = new Random(tower.name().hashCode());
        final double[] box = bounds(roof);
        final List<Plant> plants = new ArrayList<>();
        for (double x = box[0] + 200; x < box[2]; x += 350) {
            for (double z = box[1] + 200; z < box[3]; z += 350) {
                final double px = x + (random.nextDouble() - 0.5) * 200;
                final double pz = z + (random.nextDouble() - 0.5) * 200;
                final double pick = random.nextDouble();
                final Kind kind = pick < 0.45 ? Kind.TREE : pick < 0.7 ? Kind.PINE : Kind.SHRUB;
                final double size = switch (kind) {
                    case TREE -> 110 + random.nextDouble() * 60;
                    case PINE -> 80 + random.nextDouble() * 30;
                    case SHRUB -> 50 + random.nextDouble() * 30;
                };
                if (!Outline.inside(roof, px, pz)) {
                    continue;
                }
                final Vec2 edge = Outline.nearest(roof, px, pz);
                if (Math.hypot(edge.x() - px, edge.z() - pz) < EDGE + size * 0.6) {
                    continue;
                }
                if (Math.hypot(px - middle.x(), pz - middle.z()) < reach + 150 + size) {
                    continue;
                }
                if (pool.gap(px, pz) < RIM + 100 + size) {
                    continue;
                }
                if (gap(cores, px, pz) < EDGE + size * 0.6) {
                    continue;
                }
                if (plants.stream().anyMatch(p -> Math.hypot(p.x() - px, p.z() - pz) < (p.size() + size) * 1.1)) {
                    continue;
                }
                plants.add(new Plant(kind, px, pz, size));
            }
        }
        return plants;
    }

    private static double[] bounds(final List<Vec2> outline) {
        return new double[] {outline.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                outline.stream().mapToDouble(Vec2::z).min().orElseThrow(),
                outline.stream().mapToDouble(Vec2::x).max().orElseThrow(),
                outline.stream().mapToDouble(Vec2::z).max().orElseThrow()};
    }

    public Tower tower() {
        return tower;
    }

    public Pavilion pavilion() {
        return pavilion;
    }

    /** Where the pavilion stands, in the tower's frame: on the lawn on its roof. */
    public Place place() {
        return at;
    }

    public Pool pool() {
        return pool;
    }

    public List<Plant> plants() {
        return plants;
    }

    /** The roof's top: the lawn's. */
    public double lawn() {
        return tower.height() + LAWN;
    }

    /** The pool's floor, and its water's surface. */
    public double poolFloor() {
        return tower.height() - DEEP;
    }

    public double water() {
        return tower.height() - WATER;
    }

    // ---------------------------------------------------------------- composed

    /** The tower, dressed as {@code tower} dresses it, its roof cut round the pool, with the garden on its roof. */
    public Building on(final Building tower) {
        final String roofPlate = this.tower.sections().getLast().name() + ", roof";
        final List<Plate> plates = tower.plates().stream().filter(p -> !p.name().equals(roofPlate)).toList();
        // its own members (its columns showing through its glass), but none rising up into the pool
        final List<Building.Structure> structure = new ArrayList<>();
        for (final Building.Structure part : tower.structure()) {
            structure.add((skeleton, palette) -> part.of(skeleton, palette).stream().filter(s -> !inPool(s)).toList());
        }
        structure.add((skeleton, palette) -> roof(palette));
        structure.add((skeleton, palette) -> solids());
        return new Building(tower.name(), tower.skeleton(), tower.surfaces(), plates, tower.sheets(), structure,
                tower.choices(), tower.palettes());
    }

    /** Whether {@code solid} stands in the pool's basin: its middle within the pool's outline, rising above its floor. */
    private boolean inPool(final Solid solid) {
        final double x = solid.corners().stream().mapToDouble(Point::across).average().orElseThrow();
        final double z = solid.corners().stream().mapToDouble(Point::along).average().orElseThrow();
        final double high = solid.corners().stream().mapToDouble(Point::up).max().orElseThrow();
        return high > poolFloor() && pool.gap(x, z) == 0;
    }

    /** The tower's roof, as its palette has it, the pool's opening cut out of it. */
    private List<Solid> roof(final Palette palette) {
        return Sheet.flat("roof", "roof", roof.stream().map(p -> new Vec3(p.x(), p.z(), tower.height())).toList(),
                List.of(ring(pool.outline(0), tower.height())), new Vec3(0, 0, 1)).slab("roof", palette, Role.ROOF, -30, 0);
    }

    /**
     * The tower in the way as it is shaped, the pool's basin cut down into its top, with the garden on
     * its roof in the way as it is.
     */
    public List<Volume> on(final List<Volume> tower) {
        final List<Volume> all = new ArrayList<>();
        for (final Volume volume : tower) {
            if (volume instanceof Prism p && p.top() == this.tower.height() && p.outline().equals(roof)) {
                // its top section: solid up to the pool's floor; above that, round the pool
                all.add(new Prism(roof, p.bottom(), poolFloor()));
                all.add(new Prism(roof, List.of(pool.outline(0)), poolFloor(), p.top()));
            } else {
                all.add(volume);
            }
        }
        all.addAll(shape());
        return all;
    }

    private static List<Vec3> ring(final List<Vec2> outline, final double up) {
        return outline.stream().map(p -> new Vec3(p.x(), p.z(), up)).toList();
    }

    /** The garden's solids, in the tower's frame: its lawn, its pool, its pavilion dressed its own way, its plants. */
    public List<Solid> solids() {
        final List<Solid> solids = new ArrayList<>();
        final Palette lawn = Palette.STONE.with(Map.of(Role.DRESSING, Material.GRASS));
        solids.addAll(Sheet.flat("lawn", "lawn", ring(roof, tower.height()), List.of(ring(pool.outline(RIM), tower.height())),
                new Vec3(0, 0, 1)).slab("lawn", lawn, Role.DRESSING, 0, LAWN));
        solids.addAll(poolSolids(pool, tower.height()));
        final Building building = pavilion.building();
        for (final Solid s : building.dressed(building.own(), building.palettes().getFirst())) {
            solids.add(new Solid("pavilion, " + s.name(), s.role(), s.material(), s.corners().stream()
                    .map(p -> new Point(p.across() + at.x(), p.along() + at.z(), p.up() + at.height())).toList()));
        }
        int k = 0;
        for (final Plant plant : plants) {
            solids.addAll(plantSolids(plant, "plant " + k++, lawn()));
        }
        return solids;
    }

    /** {@code pool} drawn, sunk into a floor {@code top} high: its rim of stone round it, its tiled floor and sides, its water. */
    static List<Solid> poolSolids(final Pool pool, final double top) {
        final List<Solid> solids = new ArrayList<>();
        final double bottom = top - DEEP;
        final double surface = top - WATER;
        final double ax = pool.halfAcross();
        final double az = pool.halfAlong();
        final double x = pool.x();
        final double z = pool.z();
        // its rim: four slabs round it, the long ones running past the short ones' ends
        solids.add(box("pool, rim front", Material.LIMESTONE, x - ax - RIM, x + ax + RIM, z + az, z + az + RIM, top,
                top + LAWN + RIM_HIGH));
        solids.add(box("pool, rim back", Material.LIMESTONE, x - ax - RIM, x + ax + RIM, z - az - RIM, z - az, top,
                top + LAWN + RIM_HIGH));
        solids.add(box("pool, rim left", Material.LIMESTONE, x - ax - RIM, x - ax, z - az, z + az, top, top + LAWN + RIM_HIGH));
        solids.add(box("pool, rim right", Material.LIMESTONE, x + ax, x + ax + RIM, z - az, z + az, top, top + LAWN + RIM_HIGH));
        // its basin: its floor, and its sides lining the cut down into the roof
        final double tile = 6;
        solids.add(box("pool, floor", Material.POOL_TILE, x - ax, x + ax, z - az, z + az, bottom, bottom + tile));
        solids.add(box("pool, side front", Material.POOL_TILE, x - ax, x + ax, z + az - tile, z + az, bottom, top));
        solids.add(box("pool, side back", Material.POOL_TILE, x - ax, x + ax, z - az, z - az + tile, bottom, top));
        solids.add(box("pool, side left", Material.POOL_TILE, x - ax, x - ax + tile, z - az, z + az, bottom, top));
        solids.add(box("pool, side right", Material.POOL_TILE, x + ax - tile, x + ax, z - az, z + az, bottom, top));
        // its water, under its rim
        solids.add(box("pool, water", Material.WATER, x - ax + tile, x + ax - tile, z - az + tile, z + az - tile,
                surface - 2, surface));
        return solids;
    }

    static Solid box(final String name, final Material material, final double x0, final double x1, final double z0,
                             final double z1, final double low, final double high) {
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            corners.add(new Point((c & 1) == 0 ? x0 : x1, (c & 2) == 0 ? z0 : z1, (c & 4) == 0 ? low : high));
        }
        return new Solid(name, Role.DRESSING, material, corners);
    }

    /** A plant drawn: its trunk, and its crown of leaves (a ball for a tree, a cone for a pine, a mound for a shrub). */
    static List<Solid> plantSolids(final Plant plant, final String name, final double base) {
        final double r = plant.size();
        final List<Solid> solids = new ArrayList<>();
        switch (plant.kind()) {
            case TREE -> {
                final double trunk = r * 1.4;
                solids.addAll(frustum(name + ", trunk", Material.BARK, plant, r * 0.12, base, r * 0.1, base + trunk));
                solids.addAll(frustum(name + ", crown", Material.LEAVES, plant, r * 0.55, base + trunk - r * 0.2, r,
                        base + trunk + r * 0.5));
                solids.addAll(frustum(name + ", crown top", Material.LEAVES, plant, r, base + trunk + r * 0.5, r * 0.35,
                        base + trunk + r * 1.5));
            }
            case PINE -> {
                final double trunk = r * 0.8;
                solids.addAll(frustum(name + ", trunk", Material.BARK, plant, r * 0.12, base, r * 0.1, base + trunk));
                solids.addAll(frustum(name + ", crown", Material.LEAVES, plant, r, base + trunk, 2, base + trunk + r * 3.2));
            }
            case SHRUB -> solids.addAll(frustum(name + ", leaves", Material.LEAVES, plant, r * 0.8, base, r * 0.3,
                    base + r * 1.1));
        }
        return solids;
    }

    /** A frustum round the plant, from radius {@code r0} at {@code low} to {@code r1} at {@code high}: wedges from its middle. */
    static List<Solid> frustum(final String name, final Material material, final Plant plant, final double r0,
                                       final double low, final double r1, final double high) {
        final List<Solid> solids = new ArrayList<>();
        final int sides = 8;
        for (int k = 0; k < sides; k++) {
            final double a = 2 * Math.PI * k / sides;
            final double b = 2 * Math.PI * (k + 1) / sides;
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                final boolean top = (c & 4) != 0;
                final double radius = top ? r1 : r0;
                final double angle = (c & 1) == 0 ? a : b;
                final double out = (c & 2) == 0 ? 0 : radius;
                corners.add(new Point(plant.x() + out * Math.cos(angle), plant.z() + out * Math.sin(angle),
                        top ? high : low));
            }
            solids.add(new Solid(name + " " + k, Role.DRESSING, material, corners));
        }
        return solids;
    }

    /**
     * The garden in the way as it is shaped, in the tower's frame: its lawn, round the pool's rim; the
     * rim; its pavilion's parts on it; each tree's and pine's trunk, each shrub. The crowns are not:
     * flown through. The water is not either, as yet: walked into, the pool's floor under it.
     */
    public List<Volume> shape() {
        final List<Volume> shape = new ArrayList<>();
        shape.add(new Prism(roof, List.of(pool.outline(RIM)), tower.height(), lawn()));
        shape.add(new Prism(pool.outline(RIM), List.of(pool.outline(0)), tower.height(), lawn() + RIM_HIGH));
        pavilion.shape().forEach(v -> shape.add(Volume.placed(v, at, 1)));
        for (final Plant plant : plants) {
            final double r = plant.kind() == Kind.SHRUB ? plant.size() * 0.7 : plant.size() * 0.12;
            final double high = plant.kind() == Kind.SHRUB ? plant.size() * 1.1
                    : plant.kind() == Kind.TREE ? plant.size() * 1.4 : plant.size() * 0.8;
            final List<Vec2> round = new ArrayList<>();
            for (int k = 0; k < 8; k++) {
                final double a = 2 * Math.PI * k / 8;
                round.add(new Vec2(plant.x() + r * Math.cos(a), plant.z() + r * Math.sin(a)));
            }
            shape.add(new Prism(round, lawn(), lawn() + high));
        }
        return shape;
    }
}
