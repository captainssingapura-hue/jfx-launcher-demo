package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The town: every building put up on a site of its own along streets, a grid of three by three
 * 120 m apart, ahead of the start, each facing back down the way to it; and the boxer motorcycle by
 * the start to ride between them. Each building in the world is one piece ({@link Course.Kind#BUILDING}),
 * dressed in its own way and its first palette; its solid blocks its parts' boxes (its nave, its
 * tower, its roof; its podium, its shaft), so it is solid where it stands and open between;
 * broken as a whole.
 *
 * <p>All of it can be scaled ({@link #scale(double)}): every building so many times as big every
 * way, its doors and windows with it, and the streets wider, enough to keep them apart and no
 * nearer the start, but not as much wider: a town built bigger looks it.
 */
public final class Town {

    private Town() {
    }

    /** A site: which building, how it is dressed, and where on the grid (its column, 0 to 2, and its row from the start). */
    public record Site(String name, Supplier<Building> building, Palette palette, int column, int row,
                       Supplier<List<Volume>> shape) {
    }

    /** Its streets' grid: so far apart, the first row so far ahead, at its own scale. */
    private static final double APART = 12_000;
    private static final double AHEAD = 8_000;
    /** How much it may be scaled: from a quarter as big, to twice as big. */
    public static final double SMALLEST = 0.25;
    public static final double LARGEST = 2;
    /**
     * How much wider the streets are for each time bigger the buildings: less than as much, so a
     * town built bigger looks it, its buildings no further off for their size; enough to keep them
     * apart, even twice as big.
     */
    private static final double STREETS = 0.75;

    /**
     * One of its towers: which ({@code tower}, with its void deck), the pavilion on its roof (its
     * footprint, its roof, what each is of), and its palette.
     */
    private record TowerSite(String name, Supplier<Tower> tower, Pavilion.Footprint footprint, Pavilion.Roof roof,
                             Material roofing, Material pillars, Palette palette) {
    }

    /** Where its towers come in its sites: after the church, the temple hall and the apartments. */
    public static final int FIRST_TOWER = 3;

    /**
     * Its towers, each with a void deck part way up: the round, oval and rounded ones open all round
     * (full); the setback tower's, the L block's and the bow-front tower's with a wing at one end
     * enclosed (partial), the L block's its north arm.
     */
    private static final List<TowerSite> TOWERS = List.of(
            new TowerSite("setback tower", () -> Towers.withDeck(Towers.setback(), 1, 12, 760,
                    Towers.enclosed(-2000, -2000, 2 * Towers.BAY, 4000)), Pavilion.Footprint.SQUARE, Pavilion.Roof.PYRAMID,
                    Material.GLAZED, Material.LACQUER, Palette.STONE),
            new TowerSite("round tower", () -> Towers.withDeck(Towers.round(), 0, 22, 800, null), Pavilion.Footprint.ROUND,
                    Pavilion.Roof.PYRAMID, Material.GREEN_GLAZED, Material.LACQUER, Palette.CONCRETE),
            new TowerSite("oval tower", () -> Towers.withDeck(Towers.oval(), 0, 20, 760, null), Pavilion.Footprint.OCTAGON,
                    Pavilion.Roof.PYRAMID, Material.GREY_TILE, Material.STONE, Palette.CONCRETE),
            new TowerSite("rounded tower", () -> Towers.withDeck(Towers.rounded(), 0, 18, 840, null),
                    Pavilion.Footprint.RECTANGLE, Pavilion.Roof.HIP, Material.GREEN_GLAZED, Material.LACQUER, Palette.PRECAST),
            new TowerSite("L block on a podium", () -> Towers.withDeck(Towers.lBlock(), 1, 8, 720,
                    Towers.enclosed(-2400, -400, 2 * Towers.BAY, 2400)), Pavilion.Footprint.HEXAGON, Pavilion.Roof.PYRAMID,
                    Material.GLAZED, Material.LACQUER, Palette.CONCRETE),
            new TowerSite("bow-front tower", () -> Towers.withDeck(Towers.bowFront(), 0, 15, 760,
                    Towers.enclosed(-2400, -1600, 2 * Towers.BAY, 3200)), Pavilion.Footprint.RECTANGLE, Pavilion.Roof.FLAT,
                    Material.CONCRETE, Material.METAL, Palette.CONCRETE));

    /** Each tower, its roof garden and its deck's garden, by its site: worked out once, when first wanted. */
    private static final Map<Integer, Tower> TOWER = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Integer, RoofGarden> ROOF = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Integer, DeckGarden> DECK = new java.util.concurrent.ConcurrentHashMap<>();

    /** Site {@code k}'s tower, with its void deck. */
    public static Tower tower(final int k) {
        return TOWER.computeIfAbsent(k, i -> TOWERS.get(i - FIRST_TOWER).tower().get());
    }

    /** Site {@code k}'s tower's roof garden. */
    public static RoofGarden roofGarden(final int k) {
        return ROOF.computeIfAbsent(k, i -> {
            final TowerSite site = TOWERS.get(i - FIRST_TOWER);
            return RoofGarden.of(tower(i), site.footprint(), site.roof(), site.roofing(), site.pillars());
        });
    }

    /** Site {@code k}'s tower's void deck's garden, its bar's and its stall's spots kept. */
    public static DeckGarden deckGarden(final int k) {
        return DECK.computeIfAbsent(k, i -> DeckGarden.of(tower(i)));
    }

    /** Its sites, row by row from the start. */
    public static final List<Site> SITES = sites();

    private static List<Site> sites() {
        final List<Site> sites = new ArrayList<>();
        final List<Object[]> buildings = List.of(
                new Object[] {"town church", (Supplier<Building>) () -> Church.standard().building(), Palette.STONE,
                        (Supplier<List<Volume>>) () -> Church.standard().shape()},
                new Object[] {"temple hall", (Supplier<Building>) () -> Temple.standard().building(), Temple.LACQUERED,
                        (Supplier<List<Volume>>) () -> Temple.standard().shape()},
                new Object[] {"brutalist apartments", (Supplier<Building>) Apartments::building, Palette.CONCRETE});
        for (int k = 0; k < buildings.size(); k++) {
            final Object[] b = buildings.get(k);
            @SuppressWarnings("unchecked")
            final Supplier<Building> make = (Supplier<Building>) b[1];
            @SuppressWarnings("unchecked")
            final Supplier<List<Volume>> shape = b.length > 3 ? (Supplier<List<Volume>>) b[3] : null;
            sites.add(new Site((String) b[0], make, (Palette) b[2], k % 3, k / 3, shape));
        }
        // and the towers, each with its garden on its roof and its void deck, and a garden on that
        for (int t = 0; t < TOWERS.size(); t++) {
            final int k = buildings.size() + t;
            final TowerSite tower = TOWERS.get(t);
            sites.add(new Site(tower.name(), () -> deckGarden(k).on(roofGarden(k).on(TowerBuildings.of(tower(k)))),
                    tower.palette(), k % 3, k / 3,
                    () -> deckGarden(k).on(roofGarden(k).on(TowerBuildings.shape(tower(k))))));
        }
        return List.copyOf(sites);
    }

    /** How big it is built: 1, life size; set before it is first wanted. */
    private static volatile double scale = 1;

    /** Each site's building, dressed at its life size: worked out once, when first wanted. */
    private static final Map<Integer, List<Solid>> DRESSED = new java.util.concurrent.ConcurrentHashMap<>();

    /** How big it is built: 1, life size. */
    public static double scale() {
        return scale;
    }

    /**
     * Builds it {@code k} times as big every way: every building; and the streets between them
     * wider by three quarters as much (never narrower than at life size, so nothing smaller comes
     * nearer the start).
     */
    public static void scale(final double k) {
        if (!(k >= SMALLEST && k <= LARGEST)) {
            throw new IllegalArgumentException("the town scaled by " + k + ": from " + SMALLEST + " to " + LARGEST);
        }
        scale = k;
    }

    /** Where site {@code k} is, at the town's scale: turned to face back toward the start, its front to the street. */
    public static Place place(final int k) {
        final Site site = SITES.get(k);
        final double streets = Math.max(1, STREETS * scale);
        return new Place((site.column() - 1) * APART * streets, (AHEAD + site.row() * APART) * streets, 0, 180);
    }

    /** Site {@code k}'s building, dressed as it is: its own way, in its palette, at the town's scale. */
    public static List<Solid> solids(final int k) {
        return Solid.scaled(DRESSED.computeIfAbsent(k, i -> {
            final Site site = SITES.get(i);
            final Building building = site.building().get();
            return building.dressed(building.own(), site.palette());
        }), scale);
    }

    /** What is set out on its towers' void decks, at its scale: each one's bar and melon stall, loose ({@link DeckScene}). */
    public static List<Course.Piece> things() {
        final List<Course.Piece> things = new ArrayList<>();
        for (int k = FIRST_TOWER; k < SITES.size(); k++) {
            things.addAll(DeckScene.at(deckGarden(k), place(k), scale).things());
        }
        return things;
    }

    /** Who is on its towers' void decks: at each bar, its bartender and two sat at it; at each stall, its stallholder and a customer. */
    public static List<Course.Cast> people() {
        final List<Course.Cast> people = new ArrayList<>();
        for (int k = FIRST_TOWER; k < SITES.size(); k++) {
            people.addAll(DeckScene.at(deckGarden(k), place(k), scale).people());
        }
        return people;
    }

    /** What stands in the town: each building, one piece, and the boxer motorcycle by the start. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < SITES.size(); k++) {
            pieces.add(piece(k));
        }
        pieces.add(Course.Piece.vehicle(Course.BOXER, null, new Place(300, 600, Course.BOXER.ride(), 0), 0));
        return pieces;
    }

    /**
     * Site {@code k}'s building, one piece: its box about its middle, as high as it stands; its solid
     * blocks a box round each of its parts, as its solids name them (its nave, its tower, its roof, its
     * columns, all of them together each), turned and placed as it is.
     */
    static Course.Piece piece(final int k) {
        final Supplier<List<Volume>> shape = SITES.get(k).shape();
        return building(solids(k), place(k), k, shape == null ? List.of() : shape.get(), scale);
    }

    /**
     * A building dressed as {@code solids}, standing at {@code at}, one piece: its box about its
     * middle, as high as it stands; its solid blocks a box round each of its parts, as its solids name
     * them, turned and placed as it is. Drawn by its {@code key} ({@link Course.Piece#tint}).
     */
    public static Course.Piece building(final List<Solid> solids, final Place at, final int key) {
        return building(solids, at, key, List.of(), 1);
    }

    /**
     * As {@link #building(List, Place, int)}; but if it says how it is {@code shape}d (in its own
     * frame, life size), in the way as that is, {@code scale}d, rather than as its parts' boxes.
     */
    public static Course.Piece building(final List<Solid> solids, final Place at, final int key,
                                        final List<Volume> shape, final double scale) {
        final Map<String, double[]> parts = new LinkedHashMap<>();
        final double[] whole = {0, 0, 0};
        for (final Solid solid : solids) {
            final double[] box = parts.computeIfAbsent(part(solid.name()), n -> new double[] {
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY});
            for (final Point p : solid.corners()) {
                box[0] = Math.min(box[0], p.across());
                box[1] = Math.max(box[1], p.across());
                box[2] = Math.min(box[2], p.along());
                box[3] = Math.max(box[3], p.along());
                box[4] = Math.min(box[4], p.up());
                box[5] = Math.max(box[5], p.up());
                whole[0] = Math.max(whole[0], Math.abs(p.across()));
                whole[1] = Math.max(whole[1], Math.abs(p.along()));
                whole[2] = Math.max(whole[2], p.up());
            }
        }
        final List<Block> solid = new ArrayList<>();
        for (final double[] box : parts.values()) {
            final Vec3 middle = place(at, (box[0] + box[1]) / 2, (box[2] + box[3]) / 2);
            solid.add(new Block(middle.x(), middle.z(), Math.max(1, (box[1] - box[0]) / 2),
                    Math.max(1, (box[3] - box[2]) / 2), at.yaw(), Math.max(0, box[4]), Math.max(box[4] + 1, box[5])));
        }
        final Block block = new Block(at.x(), at.z(), whole[0], whole[1], at.yaw(), 0, whole[2]);
        if (!shape.isEmpty()) {
            return Course.Piece.shaped(block, Course.Kind.BUILDING, key, List.of(),
                    shape.stream().map(v -> Volume.placed(v, at, scale)).toList(), at);
        }
        return new Course.Piece(block, Course.Kind.BUILDING, key, solid, null, at, null, -1, null);
    }

    /** Which part a solid is of, by its name: up to its first comma, any number at its end left off. */
    static String part(final String name) {
        final int comma = name.indexOf(", ");
        return (comma < 0 ? name : name.substring(0, comma)).replaceAll(" \\d+$", "");
    }

    /** Where {@code across}, {@code along} in a building at {@code at} is in the world (x, 0, z). */
    private static Vec3 place(final Place at, final double across, final double along) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        return new Vec3(at.x() + right.x() * across + ahead.x() * along, 0, at.z() + right.z() * across + ahead.z() * along);
    }
}
