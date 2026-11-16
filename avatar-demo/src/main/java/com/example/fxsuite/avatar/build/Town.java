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
 * The town: its buildings, each on a site of its own along streets, a grid of three by three 150 m
 * apart, its middle the world's, the start ({@link #start}) before it, each facing back down the way to the
 * start; and in the middle of the grid, where
 * a building would stand, the temple ({@link TempleYard}), its gate to the start, in a round park:
 *
 * <ul>
 *   <li><b>the park</b>, a lawn 100 m round the temple's middle, a path across it from its edge to the
 *       temple's gate; a pond either side of the temple, its water in a stone rim, an island in its middle
 *       and a pavilion on that, a bridge out to it; and round the park, the rest of the pavilions, one of
 *       each kind ({@link Pavilions}), each facing the temple;</li>
 *   <li><b>in the temple's courtyard</b>, the monks at their stand-up; and parked along its sides, facing in,
 *       every land vehicle, the cars and the truck on one side and the motorcycles on the other; and before
 *       the monks, the gunship, its nose to the gate;</li>
 *   <li><b>at the edge of the town</b>, east of its middle, the pyramids ({@link Pyramids});</li>
 *   <li><b>on top of each tower</b>, on each of its cores, a mounted Gatling gun ({@link #guns}).</li>
 * </ul>
 *
 * <p>Each building in the world is one piece ({@link Course.Kind#BUILDING}), dressed in its own way and
 * its first palette; its solid blocks its parts' boxes (its nave, its tower, its roof; its podium, its
 * shaft), so it is solid where it stands and open between; broken as a whole.
 *
 * <p>Its buildings can be scaled ({@link #scale(double)}): every building so many times as big every
 * way, its doors and windows with it, and the streets wider, enough to keep them apart and no nearer
 * the start, but not as much wider: a town built bigger looks it. The temple, its park and the pyramids
 * stay as they are, the streets round them wider.
 */
public final class Town {

    private Town() {
    }

    /** A site: which building, how it is dressed, and where on the grid (its column, 0 to 2, and its row from the start). */
    public record Site(String name, Supplier<Building> building, Palette palette, int column, int row,
                       Supplier<List<Volume>> shape) {
    }

    /** Its streets' grid: so far apart, the first row so far ahead, at its own scale. */
    private static final double APART = 15_000;
    private static final double AHEAD = 8_000;
    /** Where on the grid the temple stands, in its park: its middle. */
    public static final int TEMPLE_COLUMN = 1;
    public static final int TEMPLE_ROW = 1;
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
     * footprint, its roof, what each is of), its palette, and where on the grid.
     */
    private record TowerSite(String name, Supplier<Tower> tower, Pavilion.Footprint footprint, Pavilion.Roof roof,
                             Material roofing, Material pillars, Palette palette, int column, int row) {
    }

    /** Where its towers come in its sites: after the church and the apartments. */
    public static final int FIRST_TOWER = 2;

    /**
     * Its towers, each with a void deck part way up: the round, oval and rounded ones open all round
     * (full); the setback tower's, the L block's and the bow-front tower's with a wing at one end
     * enclosed (partial), the L block's its north arm. The round tower, the tallest, at the front
     * corner, where it is seen from the start; the rest round the temple.
     */
    private static final List<TowerSite> TOWERS = List.of(
            new TowerSite("setback tower", () -> Towers.withDeck(Towers.setback(), 1, 12, 760,
                    Towers.enclosed(-2000, -2000, 2 * Towers.BAY, 4000)), Pavilion.Footprint.SQUARE, Pavilion.Roof.PYRAMID,
                    Material.GLAZED, Material.LACQUER, Palette.STONE, 0, 1),
            new TowerSite("round tower", () -> Towers.withDeck(Towers.round(), 0, 22, 800, null), Pavilion.Footprint.ROUND,
                    Pavilion.Roof.PYRAMID, Material.GREEN_GLAZED, Material.LACQUER, Palette.CONCRETE, 0, 0),
            new TowerSite("oval tower", () -> Towers.withDeck(Towers.oval(), 0, 20, 760, null), Pavilion.Footprint.OCTAGON,
                    Pavilion.Roof.PYRAMID, Material.GREY_TILE, Material.STONE, Palette.CONCRETE, 2, 1),
            new TowerSite("rounded tower", () -> Towers.withDeck(Towers.rounded(), 0, 18, 840, null),
                    Pavilion.Footprint.RECTANGLE, Pavilion.Roof.HIP, Material.GREEN_GLAZED, Material.LACQUER, Palette.PRECAST,
                    0, 2),
            new TowerSite("L block on a podium", () -> Towers.withDeck(Towers.lBlock(), 1, 8, 720,
                    Towers.enclosed(-2400, -400, 2 * Towers.BAY, 2400)), Pavilion.Footprint.HEXAGON, Pavilion.Roof.PYRAMID,
                    Material.GLAZED, Material.LACQUER, Palette.CONCRETE, 1, 2),
            new TowerSite("bow-front tower", () -> Towers.withDeck(Towers.bowFront(), 0, 15, 760,
                    Towers.enclosed(-2400, -1600, 2 * Towers.BAY, 3200)), Pavilion.Footprint.RECTANGLE, Pavilion.Roof.FLAT,
                    Material.CONCRETE, Material.METAL, Palette.CONCRETE, 2, 2));

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

    /** Its sites: the church, on the front row's middle, before the temple; the apartments; and the towers. */
    public static final List<Site> SITES = sites();

    private static List<Site> sites() {
        final List<Site> sites = new ArrayList<>();
        sites.add(new Site("town church", () -> Church.standard().building(), Palette.STONE, 1, 0,
                () -> Church.standard().shape()));
        sites.add(new Site("brutalist apartments", Apartments::building, Palette.CONCRETE, 2, 0, null));
        // and the towers, each with its garden on its roof and its void deck, and a garden on that
        for (int t = 0; t < TOWERS.size(); t++) {
            final int k = FIRST_TOWER + t;
            final TowerSite tower = TOWERS.get(t);
            sites.add(new Site(tower.name(), () -> deckGarden(k).on(roofGarden(k).on(TowerBuildings.of(tower(k)))),
                    tower.palette(), tower.column(), tower.row(),
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

    /** How much wider its streets are, at its scale. */
    private static double streets() {
        return Math.max(1, STREETS * scale);
    }

    /**
     * Where on the grid {@code column}, {@code row} is, at the town's scale: its middle the world's,
     * facing back toward the start.
     */
    private static Place grid(final int column, final int row) {
        return new Place((column - 1) * APART * streets(), (row - 1) * APART * streets(), 0, 180);
    }

    /**
     * Where the avatar starts, at the town's scale: on its middle line, before its first row, as far
     * ({@link #AHEAD}) as that from the start: the town's middle the world's, the start south of it.
     */
    public static Vec3 start() {
        return new Vec3(0, 0, -(AHEAD + APART) * streets());
    }

    /**
     * Tells the simulation, which knows nothing of how a building is designed, all of the town: what
     * stands in it, what is set out on its decks and who is there, where the avatar starts, and where
     * the guns on its towers stand.
     */
    public static void tell() {
        Course.town(Town::pieces, Town::things, Town::people);
        Course.townStart(Town::start);
        Course.guns(Town::guns);
    }

    /** Where site {@code k} is, at the town's scale: turned to face back toward the start, its front to the street. */
    public static Place place(final int k) {
        final Site site = SITES.get(k);
        return grid(site.column(), site.row());
    }

    /** The town's middle, at its scale: the middle of the temple's courtyard, and of its park. */
    public static Place middle() {
        final Place at = grid(TEMPLE_COLUMN, TEMPLE_ROW);
        return new Place(at.x(), at.z(), 0, 0);
    }

    /** Where the temple's courtyard is laid out from ({@link TempleYard}'s own origin): its middle on the town's, its gate to the start. */
    public static Place temple() {
        final Place middle = middle();
        return new Place(middle.x(), middle.z() - (TempleYard.FRONT + TempleYard.BACK) / 2, 0, 0);
    }

    /** Where the pyramids are laid out from ({@link Pyramids}' own origin): east of the town, the great one level with its middle. */
    public static Place pyramids() {
        return new Place(PYRAMIDS_OUT * streets(), middle().z() - Pyramids.GREAT.z(), 0, 0);
    }

    /** How far east of the town's middle line the great pyramid stands: past its east column's widest tower and some. */
    public static final double PYRAMIDS_OUT = 31_500;

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
    public static List<Course.Cast> onDecks() {
        final List<Course.Cast> people = new ArrayList<>();
        for (int k = FIRST_TOWER; k < SITES.size(); k++) {
            people.addAll(DeckScene.at(deckGarden(k), place(k), scale).people());
        }
        return people;
    }

    /** Who is in it: those on its towers' void decks ({@link #onDecks}); and in the temple's courtyard, the monks at their stand-up. */
    public static List<Course.Cast> people() {
        final List<Course.Cast> people = new ArrayList<>(onDecks());
        final Place temple = temple();
        Course.MONKS.forEach(monk -> people.add(monk.within(temple)));
        return people;
    }

    /**
     * What stands in the town: each building, one piece; the temple's halls, its wall and its gate; its
     * park, its lawn, its path, its ponds and its pavilions; the pyramids; and the vehicles parked in the
     * temple's courtyard.
     */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < SITES.size(); k++) {
            pieces.add(piece(k));
        }
        final Place temple = temple();
        TempleYard.pieces().forEach(p -> pieces.add(p.within(temple)));
        pieces.addAll(park());
        final Place pyramids = pyramids();
        Pyramids.pieces().forEach(p -> pieces.add(p.within(pyramids)));
        pieces.addAll(parked());
        return pieces;
    }

    /** The park round the temple: so far round its middle; a path across it to the gate, so wide. */
    public static final double PARK = 10_000;
    public static final double PATH = 600;
    /** Each pond: its middle so far out from the temple's, either side; its water so far round; its kerb so wide. */
    public static final double POND_OUT = 7_600;
    public static final double POND = 1_800;
    public static final double RIM = 100;
    /** The island in its middle, so far round; the bridge out to it, so wide; its railings so high and thick. */
    public static final double ISLAND = 750;
    public static final double BRIDGE = 300;
    public static final double RAILING = 100;
    public static final double RAILING_THICK = 8;
    /**
     * How high each lies. The lawn at the ground, open where the courtyard's paving, the path and the
     * ponds are; the paving and the path, a hair over it, beside it. Each pond's kerb, a low stone wall
     * round its water, to step over ({@link com.example.fxsuite.avatar.sim.Body#STEP}); its water a little
     * lower within it, shallow, to wade; its island as high as the kerb; the bridge out to it a little over
     * both, to walk out on. What lies over another (the bridge over the water) at least {@link #LAID_APART} over it.
     */
    private static final double LAWN_TOP = 0;
    private static final double PAVED_TOP = 1;
    public static final double KERB_TOP = 20;
    public static final double WATER_TOP = 12;
    public static final double LAID_APART = 6;
    /** The pavilions round the park, but those on the islands: so far out from its middle. */
    public static final double PAVILIONS_OUT = 8_300;

    /**
     * Which pavilion ({@link Pavilions#PRESETS}) stands where: on the west pond's island, the round
     * one; on the east's, the octagonal one; behind the temple, the long gallery; and round the
     * corners, the square, the hexagonal, the gable and the modern ones. Headings from the park's
     * middle, as {@link Num#heading} has them, 0 straight back from the start.
     */
    private static final int[][] PAVILION_AT = {{3, -90}, {2, 90}, {4, 0}, {1, 45}, {5, -45}, {0, 135}, {6, -135}};

    /** Its trees, round its edge, so far out, so far apart round; a tree's trunk so thick and so tall, and kept so far from the rest. */
    public static final double TREES_OUT = 9_300;
    private static final double TREES_EVERY = 10;
    private static final double TRUNK = 36;
    private static final double TRUNK_TALL = 320;
    private static final double TREE_CLEAR = 1_100;
    /** Its lamps along the path, so far out from its middle line, so far apart; a lamp post so thick and so tall. */
    private static final double LAMPS_OUT = PATH / 2 + 80;
    private static final double LAMPS_EVERY = 1_200;
    private static final double POST = 12;
    private static final double POST_TALL = 420;
    /** Its benches along the path, so far out from its middle line; by each pond, so far out from its kerb. */
    private static final double BENCHES_OUT = PATH / 2 + 160;
    private static final double BY_THE_WATER = 220;

    /**
     * The park: its floor, nothing over anything else ({@link Course.Kind#GARDEN}): its lawn, the temple's
     * courtyard paved, its path; each pond, its kerb and its island in the way as they are shaped, its
     * water within, a bridge out to the island, railed; its pavilions, each facing the temple; and what
     * stands about in it, each in the way, each to be broken: its trees round its edge, its lamp posts
     * along the path and at each bridge; and loose ({@link com.example.fxsuite.avatar.sim.Furniture}), to be
     * pushed about and knocked over: benches along the path and by the water, a litter bin by some,
     * planters at each end of the path.
     */
    public static List<Course.Piece> park() {
        final Place middle = middle();
        final List<Course.Piece> park = new ArrayList<>();
        park.add(garden(middle.x(), middle.z(), PARK, PARK, 0, LAWN_TOP, Course.GARDEN_LAWN));
        // the temple's courtyard paved within its wall, up to where its back corners round (behind the great
        // hall, the lawn): one box, the lawn open where it is
        final Place temple = temple();
        final double round = TempleYard.BACK - TempleYard.ROUNDED;
        park.add(garden(temple.x(), temple.z() + (TempleYard.FRONT + round) / 2, TempleYard.FLANK,
                (round - TempleYard.FRONT) / 2, 0, PAVED_TOP, Course.GARDEN_PAVING));
        // the path, from the park's edge before the temple up to its gate: the lawn open where it is
        final double edge = middle.z() - PARK;
        final double gate = temple.z() + TempleYard.GATE.z();
        park.add(garden(middle.x(), (edge + gate) / 2, PATH / 2, (gate - edge) / 2, 0, PAVED_TOP, Course.GARDEN_PAVING));
        for (final double side : new double[] {-1, 1}) {
            pond(park, middle.x() + side * POND_OUT, middle.z(), side);
        }
        for (final int[] at : PAVILION_AT) {
            final boolean island = Math.abs(at[1]) == 90;
            final double out = island ? POND_OUT : PAVILIONS_OUT;
            final Vec3 way = Num.heading(at[1]);
            final double x = middle.x() + way.x() * out;
            final double z = middle.z() + way.z() * out;
            park.add(Pavilions.piece(at[0], new Place(x, z, island ? KERB_TOP : 0, Num.wrap(at[1] + 180))));
        }
        park.addAll(trees(middle));
        park.addAll(alongThePath(middle, edge, gate));
        for (final double side : new double[] {-1, 1}) {
            // by each pond, a bench either side of it, on the lawn, facing the water; at the bridge's foot, a lamp either side
            final double x = middle.x() + side * POND_OUT;
            for (final double end : new double[] {-1, 1}) {
                park.add(com.example.fxsuite.avatar.sim.Course.Piece.loose(com.example.fxsuite.avatar.sim.Furniture.BENCH,
                        Course.Kind.FURNITURE, new Place(x, middle.z() + end * (POND + RIM + BY_THE_WATER), 0,
                                end > 0 ? 180 : 0), 0));
                park.add(lamp(x + side * (POND + RIM + 150), middle.z() + end * (BRIDGE / 2 + 120)));
            }
        }
        return park;
    }

    /**
     * A pond about ({@code x}, {@code z}): its kerb, a ring of stone in the way as it is shaped, low enough
     * to step over; its water within, shallow, to wade; its island in its middle, as high as the kerb; and a
     * bridge out to it from the kerb on the side away from the temple ({@code side}), railed over the water.
     */
    private static void pond(final List<Course.Piece> park, final double x, final double z, final double side) {
        final double outer = POND + RIM;
        park.add(Course.Piece.shaped(new Block(x, z, outer, outer, 0, 0, KERB_TOP), Course.Kind.GARDEN,
                Course.GARDEN_STONE, List.of(), List.of(new com.example.fxsuite.avatar.sim.Prism(circle(x, z, outer),
                        List.of(circle(x, z, POND)), 0, KERB_TOP)), new Place(x, z, 0, 0)));
        park.add(garden(x, z, POND, POND, 0, WATER_TOP, Course.GARDEN_WATER));
        park.add(Course.Piece.shaped(new Block(x, z, ISLAND, ISLAND, 0, 0, KERB_TOP), Course.Kind.GARDEN,
                Course.GARDEN_STONE, List.of(), List.of(new com.example.fxsuite.avatar.sim.Prism(circle(x, z, ISLAND), 0,
                        KERB_TOP)), new Place(x, z, 0, 0)));
        // the bridge, over the water from the island's edge to the kerb's, level with both: walked on from one to the other
        // (its corners within the kerb's inside, so it lies on none of it)
        final double inside = POND * Math.cos(Math.PI / CIRCLE);
        final double reach = Math.sqrt(inside * inside - BRIDGE * BRIDGE / 4) - 1;
        final double start = ISLAND + 1;
        final Block deck = new Block(x + side * (start + reach) / 2, z, (reach - start) / 2, BRIDGE / 2, 0, 0, KERB_TOP);
        park.add(new Course.Piece(deck, Course.Kind.GARDEN, Course.GARDEN_PAVING, List.of(deck)));
        // its railings, along both its sides, over the water
        final double railed = (POND - ISLAND) / 2;
        final double over = x + side * (ISLAND + POND) / 2;
        for (final double edge : new double[] {-1, 1}) {
            final Block railing = new Block(over, z + edge * (BRIDGE / 2 - RAILING_THICK / 2), railed, RAILING_THICK / 2,
                    0, KERB_TOP, KERB_TOP + RAILING);
            park.add(new Course.Piece(railing, Course.Kind.WALL, 0, List.of(railing)));
        }
    }

    /** How many sides a circle is shaped with, in the way. */
    private static final int CIRCLE = 48;

    /** A circle about ({@code x}, {@code z}), {@code radius} round, as an outline: so many sides ({@link #CIRCLE}), its corners on it. */
    private static List<com.example.fxsuite.avatar.sim.Vec2> circle(final double x, final double z, final double radius) {
        final List<com.example.fxsuite.avatar.sim.Vec2> outline = new ArrayList<>();
        final int sides = CIRCLE;
        for (int k = 0; k < sides; k++) {
            final double a = 2 * Math.PI * k / sides;
            outline.add(new com.example.fxsuite.avatar.sim.Vec2(x + radius * Math.sin(a), z + radius * Math.cos(a)));
        }
        return outline;
    }

    /**
     * Its trees, round its edge, every so far round, but none where the path comes in, by a pond, or by a
     * pavilion: each its trunk, in the way; its crown over it, drawn; of two kinds, by turns.
     */
    private static List<Course.Piece> trees(final Place middle) {
        final List<Vec3> clear = new ArrayList<>();
        for (final int[] at : PAVILION_AT) {
            final double out = Math.abs(at[1]) == 90 ? POND_OUT : PAVILIONS_OUT;
            clear.add(Num.heading(at[1]).multiply(out));
        }
        final List<Course.Piece> trees = new ArrayList<>();
        int k = 0;
        for (double heading = 0; heading < 360; heading += TREES_EVERY) {
            final Vec3 at = Num.heading(heading).multiply(TREES_OUT);
            final boolean byThePath = at.z() < 0 && Math.abs(at.x()) < PATH / 2 + TREE_CLEAR;
            final boolean byAPond = Math.abs(Math.abs(at.x()) - POND_OUT) < POND + RIM + TREE_CLEAR
                    && Math.abs(at.z()) < POND + RIM + TREE_CLEAR;
            final boolean byAPavilion = clear.stream().anyMatch(c -> Math.hypot(c.x() - at.x(), c.z() - at.z()) < TREE_CLEAR);
            if (byThePath || byAPond || byAPavilion) {
                continue;
            }
            final Block trunk = Block.standing(middle.x() + at.x(), middle.z() + at.z(), TRUNK, TRUNK, TRUNK_TALL, 0);
            trees.add(new Course.Piece(trunk, Course.Kind.TREE, k++ % 2, List.of(trunk)));
        }
        return trees;
    }

    /** A lamp post at ({@code x}, {@code z}): its post, in the way; its lamp on top. */
    private static Course.Piece lamp(final double x, final double z) {
        final Block post = Block.standing(x, z, POST, POST, POST_TALL, 0);
        return new Course.Piece(post, Course.Kind.LAMP, 0, List.of(post));
    }

    /**
     * Along the path, either side: its lamp posts, every so far from the park's edge to the gate; between
     * them, benches facing the path, a litter bin by every other; and at each end of it, a planter either side.
     */
    private static List<Course.Piece> alongThePath(final Place middle, final double edge, final double gate) {
        final List<Course.Piece> along = new ArrayList<>();
        for (final double side : new double[] {-1, 1}) {
            int k = 0;
            for (double z = edge + LAMPS_EVERY / 2; z < gate - 200; z += LAMPS_EVERY) {
                along.add(lamp(middle.x() + side * LAMPS_OUT, z));
                final double bench = z + LAMPS_EVERY / 2;
                if (bench < gate - 400) {
                    along.add(Course.Piece.loose(com.example.fxsuite.avatar.sim.Furniture.BENCH, Course.Kind.FURNITURE,
                            new Place(middle.x() + side * BENCHES_OUT, bench, 0, side > 0 ? -90 : 90), 0));
                    if (k++ % 2 == 0) {
                        along.add(Course.Piece.loose(com.example.fxsuite.avatar.sim.Furniture.BIN, Course.Kind.FURNITURE,
                                new Place(middle.x() + side * BENCHES_OUT, bench + 150, 0, 0), 0));
                    }
                }
            }
            for (final double end : new double[] {edge + 250, gate - TempleYard.GATE_DEEP - TempleYard.GATE_EAVES - 150}) {
                along.add(Course.Piece.loose(com.example.fxsuite.avatar.sim.Furniture.PLANTER, Course.Kind.FURNITURE,
                        new Place(middle.x() + side * (PATH / 2 + 120), end, 0, 0), 0));
            }
        }
        return along;
    }

    /** A garden's piece, {@code tint} its kind, about ({@code x}, {@code z}), so far each way, turned {@code yaw}, so high: in no one's way. */
    private static Course.Piece garden(final double x, final double z, final double across, final double along,
                                       final double yaw, final double top, final int tint) {
        return new Course.Piece(new Block(x, z, across, along, yaw, 0, top), Course.Kind.GARDEN, tint, List.of());
    }

    /** How far in from the temple's side walls the parked vehicles' backs are; how far apart side by side; where the first is. */
    private static final double PARKED_IN = 2_200;
    private static final double PARKED_APART = 150;
    private static final double PARKED_FROM = 300;
    /** Where the gunship stands in the courtyard, in its frame: on its middle line, before the monks. */
    public static final Place GUNSHIP = new Place(0, 1_100, 0, 180);

    /**
     * The vehicles parked in the temple's courtyard, in its frame and then where it is: along its west
     * side, facing in, the truck, the pickup, the van and the roadster; along its east side, facing in, the
     * motorcycles, the scooter, the sport bike, the cruiser, the boxer and the boxers with a sidecar and with
     * an ice cream cart; and on its middle line before the monks, the gunship, its nose to the gate.
     */
    public static List<Course.Piece> parked() {
        final Place temple = temple();
        final List<Course.Piece> parked = new ArrayList<>();
        final Object[][] west = {{Course.TRUCK_CHASSIS, Course.TRUCK_CAB, 1}, {Course.PICKUP_CHASSIS, Course.PICKUP_BODY, 2},
                {Course.CAR_CHASSIS, Course.VAN, 3}, {Course.CAR_CHASSIS, Course.ROADSTER, 0}};
        final Object[][] east = {{Course.SCOOTER, null, 0}, {Course.SPORT_BIKE, null, 0}, {Course.CRUISER, null, 0},
                {Course.BOXER, null, 0}, {Course.BOXER_SIDECAR, null, 0}, {Course.BOXER_ICE_CREAM, null, 0}};
        for (final double side : new double[] {-1, 1}) {
            double along = PARKED_FROM;
            for (final Object[] v : side < 0 ? west : east) {
                final Course.Chassis chassis = (Course.Chassis) v[0];
                final Course.Bodywork body = (Course.Bodywork) v[1];
                final Course.Section box = chassis.shape(body).box();
                final double wide = box.halfAcross() + Math.abs(box.across());
                along += wide;
                final Place at = new Place(side * (PARKED_IN - chassis.length() / 2), along, chassis.ride(), -side * 90);
                parked.add(Course.Piece.vehicle(chassis, body, Course.Piece.within(at, temple), (Integer) v[2]));
                along += wide + PARKED_APART;
            }
        }
        final Place gunship = Course.Piece.within(new Place(GUNSHIP.x(), GUNSHIP.z(), Course.GUNSHIP.standing(),
                GUNSHIP.yaw()), temple);
        parked.add(Course.Piece.moving(Course.GUNSHIP.shape(), gunship, Course.Kind.AIRCRAFT, 0, null));
        return parked;
    }

    /**
     * Where the guns mounted on {@code building} stand ({@link Course#guns(Course.Piece)}): if it is one of
     * the town's towers, on top of each of its cores, over its roof, where its lift overrun ends
     * ({@link TowerBuildings#OVERRUN}), in the middle of it, facing out from the town's middle; on
     * anything else, none.
     */
    public static List<Place> guns(final Course.Piece building) {
        final int k = building.tint();
        if (building.kind() != Course.Kind.BUILDING || k < FIRST_TOWER || k >= SITES.size() || building.place() == null) {
            return List.of();
        }
        final Tower tower = tower(k);
        final Place at = building.place();
        final Place middle = middle();
        final List<Place> guns = new ArrayList<>();
        for (final List<Plan.Pt> core : tower.structure().cores()) {
            final double across = core.stream().mapToDouble(Plan.Pt::x).average().orElse(0) * scale;
            final double along = core.stream().mapToDouble(Plan.Pt::y).average().orElse(0) * scale;
            final Vec3 there = place(at, across, along);
            final double out = Math.toDegrees(Math.atan2(there.x() - middle.x(), there.z() - middle.z()));
            guns.add(new Place(there.x(), there.z(), (tower.height() + TowerBuildings.OVERRUN) * scale, out));
        }
        return guns;
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
                    Math.max(1, (box[3] - box[2]) / 2), at.yaw(), at.height() + Math.max(0, box[4]),
                    at.height() + Math.max(box[4] + 1, box[5])));
        }
        // as high as it stands: on the ground, or on what it is put on (a pavilion on its island)
        final Block block = new Block(at.x(), at.z(), whole[0], whole[1], at.yaw(), at.height(), at.height() + whole[2]);
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
