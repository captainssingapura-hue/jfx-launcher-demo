package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Facade.Pattern;
import com.example.fxsuite.avatar.sim.Buildings;
import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The brutalist apartments, composed: their structure ({@link Buildings#apartments}); their
 * surfaces (a front and a back, 36 m long, and two ends, 12 m, each over the four upper storeys and
 * their parapet; the flat roof; the underside of the first floor, over the open ground storey; a
 * glazed lobby in among the pilotis; its ground storey all round, to close it in; the stair tower
 * on the roof); the columns of the open ground
 * storey, worked out from the skeleton; and the ways it can be dressed, each a design for each
 * kind of surface: the same building, many facades.
 */
public final class Apartments {

    private Apartments() {
    }

    /** The grid: its lines of columns across and along; its floors, the ground storey taller. */
    private static final double HALF_LENGTH = 1800;
    private static final double HALF_DEPTH = 600;
    private static final double FIRST = 420;
    private static final double STOREY = 300;
    private static final int UPPER = 4;
    private static final double PARAPET = 100;
    private static final double ROOF = FIRST + UPPER * STOREY;
    private static final double[] BAYS = {0, 600, 1200, 1800, 2400, 3000, 3600};
    private static final double[] DEEP = {0, 600, 1200};
    private static final double[] FLOORS = {0, 300, 600, 900};

    private static final Point UP = new Point(0, 0, 1);

    /** What its choices are about: its facade, above; and its ground floor. */
    public static final String FACADE = "Facade";
    public static final String GROUND = "Ground floor";
    public static final String CORNERS = "Corners";
    public static final String TOP = "Top";

    /** The building, its own way of being dressed first. */
    public static Building building() {
        final double wallHeight = UPPER * STOREY + PARAPET;
        final List<Surface> surfaces = new ArrayList<>();
        // its long walls, front and back, and its ends: each seen from outside, u left to right
        surfaces.add(new Surface("front", "front", new Point(HALF_LENGTH, HALF_DEPTH, FIRST), new Point(-1, 0, 0), UP,
                new Point(0, 1, 0), 2 * HALF_LENGTH, wallHeight, BAYS, FLOORS, PARAPET));
        surfaces.add(new Surface("back", "back", new Point(-HALF_LENGTH, -HALF_DEPTH, FIRST), new Point(1, 0, 0), UP,
                new Point(0, -1, 0), 2 * HALF_LENGTH, wallHeight, BAYS, FLOORS, PARAPET));
        surfaces.add(new Surface("left end", "end", new Point(-HALF_LENGTH, HALF_DEPTH, FIRST), new Point(0, -1, 0), UP,
                new Point(-1, 0, 0), 2 * HALF_DEPTH, wallHeight, DEEP, FLOORS, PARAPET));
        surfaces.add(new Surface("right end", "end", new Point(HALF_LENGTH, -HALF_DEPTH, FIRST), new Point(0, 1, 0), UP,
                new Point(1, 0, 0), 2 * HALF_DEPTH, wallHeight, DEEP, FLOORS, PARAPET));
        // its ground storey, round it on the same lines: open on its pilotis, or closed in, as its ground floor's option says
        final double[] ground = {0};
        surfaces.add(new Surface("ground front", "ground front", new Point(HALF_LENGTH, HALF_DEPTH, 0), new Point(-1, 0, 0),
                UP, new Point(0, 1, 0), 2 * HALF_LENGTH, FIRST, BAYS, ground, 0));
        surfaces.add(new Surface("ground back", "ground back", new Point(-HALF_LENGTH, -HALF_DEPTH, 0), new Point(1, 0, 0),
                UP, new Point(0, -1, 0), 2 * HALF_LENGTH, FIRST, BAYS, ground, 0));
        surfaces.add(new Surface("ground left end", "ground end", new Point(-HALF_LENGTH, HALF_DEPTH, 0),
                new Point(0, -1, 0), UP, new Point(-1, 0, 0), 2 * HALF_DEPTH, FIRST, DEEP, ground, 0));
        surfaces.add(new Surface("ground right end", "ground end", new Point(HALF_LENGTH, -HALF_DEPTH, 0),
                new Point(0, 1, 0), UP, new Point(1, 0, 0), 2 * HALF_DEPTH, FIRST, DEEP, ground, 0));
        // its roof, and the underside of its first floor over the open ground storey
        surfaces.add(new Surface("roof", "roof", new Point(-HALF_LENGTH, -HALF_DEPTH, ROOF), new Point(1, 0, 0),
                new Point(0, 1, 0), UP, 2 * HALF_LENGTH, 2 * HALF_DEPTH, BAYS, DEEP, 0));
        surfaces.add(new Surface("first floor's underside", "underside", new Point(-HALF_LENGTH, HALF_DEPTH, FIRST),
                new Point(1, 0, 0), new Point(0, -1, 0), new Point(0, 0, -1), 2 * HALF_LENGTH, 2 * HALF_DEPTH, BAYS,
                DEEP, 0));
        // the lobby: a glass box in the middle two bays of the open ground storey, set back from the front
        surfaces.addAll(box("lobby", "lobby", 600, 300, 0, FIRST));
        // the stair tower on the roof
        surfaces.addAll(box("roof tower", "tower", 300, 300, ROOF, 350));
        surfaces.add(new Surface("roof tower's top", "tower top", new Point(-300, -300, ROOF + 350), new Point(1, 0, 0),
                new Point(0, 1, 0), UP, 600, 600, new double[] {0, 600}, new double[] {0}, 0));
        // the columns of the open ground storey: the skeleton's upright edges below the first floor
        final Form skeleton = Buildings.apartments();
        final Building.Structure pilotis = Building.Structure.members("pilotis", edge -> {
            final Form.Vertex a = skeleton.vertices().get(edge.from());
            final Form.Vertex b = skeleton.vertices().get(edge.to());
            return a.across() == b.across() && a.along() == b.along() && Math.max(a.up(), b.up()) <= FIRST;
        }, 50, Role.STRUCTURE);
        final Map<String, List<Building.Option>> choices = new java.util.LinkedHashMap<>();
        choices.put(FACADE, options());
        choices.put(GROUND, groundFloors());
        // what is added to its walls, whatever their facade: corner stones up its corners; a cornice along its top
        final String[] walls = {"front", "back", "end"};
        choices.put(CORNERS, List.of(new Building.Option("Plain", Map.of()), Building.Option.adding("Corner stones",
                Dressings.cornerStones(Role.WALL, 30, 60, 30, 3), walls)));
        choices.put(TOP, List.of(new Building.Option("Coping", Map.of()),
                Building.Option.adding("Cornice", Dressings.cornice(Role.WALL), walls)));
        return new Building("brutalist apartments", skeleton, surfaces, List.of(pilotis), choices, Palette.ALL);
    }

    /** A box's four walls, {@code halfWide} by {@code halfDeep} about the middle, from {@code low}, {@code high} high. */
    private static List<Surface> box(final String name, final String kind, final double halfWide, final double halfDeep,
                                     final double low, final double high) {
        final double[] floors = {0};
        final double[] across = {0, 2 * halfWide};
        final double[] along = {0, 2 * halfDeep};
        return List.of(
                new Surface(name + ", front", kind, new Point(halfWide, halfDeep, low), new Point(-1, 0, 0), UP,
                        new Point(0, 1, 0), 2 * halfWide, high, across, floors, 0),
                new Surface(name + ", back", kind, new Point(-halfWide, -halfDeep, low), new Point(1, 0, 0), UP,
                        new Point(0, -1, 0), 2 * halfWide, high, across, floors, 0),
                new Surface(name + ", left", kind, new Point(-halfWide, halfDeep, low), new Point(0, -1, 0), UP,
                        new Point(-1, 0, 0), 2 * halfDeep, high, along, floors, 0),
                new Surface(name + ", right", kind, new Point(halfWide, -halfDeep, low), new Point(0, 1, 0), UP,
                        new Point(1, 0, 0), 2 * halfDeep, high, along, floors, 0));
    }

    /** What every facade shares: the ends blank, the roof and the floor's underside slabs, the tower blank. */
    private static Map<String, Design> common() {
        final Design blank = new Facade("blank", Role.WALL, 0, Pattern.all(Infills.blank()),
                List.of(Trims.coping(Role.DRESSING, 8, 20)), false);
        return Map.of(
                "end", blank,
                "roof", Design.slab(Role.STRUCTURE, -30, 0).and(Design.slab(Role.ROOF, 0, 4)),
                "underside", Design.slab(Role.STRUCTURE, -30, 0),
                "tower", new Facade("tower", Role.WALL, 0, Pattern.all(Infills.blank()), List.of(), true),
                "tower top", Design.slab(Role.STRUCTURE, -25, 0));
    }

    /** {@code common} with a front and a back of their own. */
    private static Building.Option option(final String name, final Design front, final Design back) {
        final java.util.HashMap<String, Design> designs = new java.util.HashMap<>(common());
        designs.put("front", front);
        designs.put("back", back);
        return new Building.Option(name, designs);
    }

    /**
     * The ways its ground storey can be: open, on its pilotis, the glass box of its lobby among them;
     * closed in with shopfronts, its entrance in the middle of its front under a canopy; or closed
     * in with a solid base, a pair of high windows in each bay, its entrance glazed. Its walls its base:
     * as its palette says, concrete, or rusticated stone.
     */
    public static List<Building.Option> groundFloors() {
        final Design blank = new Facade("blank", Role.BASE, 0, Pattern.all(Infills.blank()), List.of(),
                false);
        final Facade.Infill entrance = Infills.entrance(240, 280, 150, Role.DRESSING);
        final Pattern middle = cell -> cell.bay() == 2 || cell.bay() == 3 ? entrance : null;
        final Facade.Infill shop = Infills.shopfront(45, 60, 50);
        final Facade.Infill high = Infills.windows(2, 200, 270, 360);
        return List.of(
                new Building.Option("Open, on pilotis", Map.of("lobby", new Facade("glazed lobby", Role.FRAME, 10,
                        Pattern.all(Infills.glazing(0, 0)), List.of(), false))),
                new Building.Option("Shopfronts", Map.of(
                        "ground front", new Facade("shopfronts", Role.BASE, 0,
                                cell -> middle.at(cell) != null ? entrance : shop, List.of(), true),
                        "ground back", new Facade("shopfronts", Role.BASE, 0, Pattern.all(shop), List.of(), true),
                        "ground end", blank)),
                new Building.Option("Solid, high windows", Map.of(
                        "ground front", new Facade("solid", Role.BASE, 0,
                                cell -> middle.at(cell) != null ? entrance : high, List.of(), true),
                        "ground back", new Facade("solid", Role.BASE, 0, Pattern.all(high), List.of(), true),
                        "ground end", blank)));
    }

    /** The ways its facade, above its ground storey, can be dressed. */
    public static List<Building.Option> options() {
        final Design coping = Trims.coping(Role.DRESSING, 8, 22);
        final Design ribbons = new Facade("ribbon windows", Role.WALL, 0,
                Pattern.all(Infills.ribbon(90, 240, 40)), List.of(Trims.floorBands(Role.WALL, 0, 25), coping),
                true);
        final Design balconies = new Facade("balconies", Role.WALL, 0,
                Pattern.checker(Infills.balcony(150, 60, 120, Role.DRESSING), Infills.window(70, 90, 250)),
                List.of(coping), true);
        final Design eggCrate = new Facade("egg-crate", Role.WALL, 0, Pattern.all(Infills.glazing(30, 45)),
                List.of(Trims.fins(Role.DRESSING, 2, 20, 90), Trims.floorBands(Role.WALL, 0, 90), coping),
                true);
        final Design punched = new Facade("punched", Role.WALL, 0,
                Pattern.all(Infills.punched(2, 130, 12, Role.DRESSING)), List.of(coping), true);
        return List.of(
                option("Balconies front, ribbons back", balconies, ribbons),
                option("Ribbon windows", ribbons, ribbons),
                option("Egg-crate fins", eggCrate, eggCrate),
                option("Punched windows", punched, punched),
                option("Balconies both sides", balconies, balconies));
    }
}
