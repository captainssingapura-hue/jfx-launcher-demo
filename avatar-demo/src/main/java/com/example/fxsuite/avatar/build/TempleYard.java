package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Crossing;
import com.example.fxsuite.avatar.sim.Extrusion;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A temple's courtyard, as a Chinese temple is laid out round one: at its back, on its middle line,
 * the great hall, nine bays across on a high platform, facing down the courtyard to the start; before
 * it, a smaller hall each side, five bays across, facing in across the courtyard to each other, their
 * roofs green where the great hall's are yellow. Each is one piece in the world
 * ({@link Course.Kind#BUILDING}), as the town's buildings are ({@link Town#building}). Its gate, at
 * the courtyard's front, is still to come: a building to pass through.
 */
public final class TempleYard {

    private TempleYard() {
    }

    /** A hall: what it is called, how big it is built, its roof's tiles, where it stands and which way it faces. */
    public record Hall(String name, Temple.Size size, String tiles, Place place) {
    }

    /** Its halls' keys, to draw them by, start here: past the town's. */
    public static final int KEYS = 100;

    /** The great hall: nine bays across, its middle 7.2 m, five deep, its columns 6 m on a 2.4 m platform. */
    public static final Temple.Size GREAT = new Temple.Size(9, 720, 5, 600, 240);
    /** Each side hall: five bays across, its middle 5 m, three deep, its columns 4 m on a 0.9 m platform. */
    public static final Temple.Size SIDE = new Temple.Size(5, 500, 3, 400, 90);

    /** Its halls: the great hall at the back, facing the start; the side halls either side before it, facing in. */
    public static final List<Hall> HALLS = List.of(
            new Hall("great hall", GREAT, "Yellow glazed", new Place(0, 6500, 0, 180)),
            new Hall("west hall", SIDE, "Green glazed", new Place(-3300, 2800, 0, 90)),
            new Hall("east hall", SIDE, "Green glazed", new Place(3300, 2800, 0, -90)));

    /** Its wall's units' keys, to draw them by, start here: past its halls'. */
    public static final int WALL_KEYS = 200;
    /** How long, at most, each unit of its wall: what one blow can bring down. */
    public static final double UNIT = 600;
    /** Its wall round it: its front, its sides, its back; and the openings in it, front and back. */
    public static final double FRONT = -800;
    public static final double BACK = 9000;
    public static final double FLANK = 4700;
    /** The back's corners rounded so far in. */
    public static final double ROUNDED = 800;
    /** The main entrance, on its middle line at its front, so wide; and the back door, behind the great hall. */
    public static final double ENTRANCE = 900;
    public static final double BACK_DOOR = 180;

    /**
     * Its wall: a red wall on a stone plinth, its coping green-tiled, round the courtyard and its halls;
     * the front open on its middle line for the main entrance (its gate still to come), and a small
     * door in the back behind the great hall; the back's corners rounded. In units of 6 m at most.
     */
    public static final Wall WALL = new Wall("temple wall", Wall.Style.TEMPLE, enclosure());

    private static List<Wall.Unit> enclosure() {
        final List<Wall.Unit> units = new ArrayList<>();
        final double gate = ENTRANCE / 2;
        final double door = BACK_DOOR / 2;
        final double turn = BACK - ROUNDED;
        units.addAll(Wall.run(at(-FLANK, FRONT), at(-gate, FRONT), UNIT));
        units.addAll(Wall.run(at(gate, FRONT), at(FLANK, FRONT), UNIT));
        units.addAll(Wall.run(at(-FLANK, FRONT), at(-FLANK, turn), UNIT));
        units.addAll(Wall.arc(at(-FLANK + ROUNDED, turn), ROUNDED, 270, 360, UNIT));
        units.addAll(Wall.run(at(-FLANK + ROUNDED, BACK), at(-door, BACK), UNIT));
        units.addAll(Wall.run(at(door, BACK), at(FLANK - ROUNDED, BACK), UNIT));
        units.addAll(Wall.arc(at(FLANK - ROUNDED, turn), ROUNDED, 0, 90, UNIT));
        units.addAll(Wall.run(at(FLANK, turn), at(FLANK, FRONT), UNIT));
        return units;
    }

    private static Point at(final double x, final double z) {
        return new Point(x, z, 0);
    }

    /**
     * Its main gate, in the entrance: a gatehouse, half so wide and so deep, its walls so high, an
     * arched passage through it (half so wide, springing so high, a half circle over it); its hip
     * roof out so far over it, its ridge so high and so long each way.
     */
    public static final double GATE_WIDE = 550;
    public static final double GATE_DEEP = 250;
    public static final double GATE_HIGH = 700;
    public static final double PASSAGE = 180;
    public static final double SPRING = 340;
    public static final double GATE_EAVES = 120;
    public static final double GATE_RIDGE = 1000;
    public static final double GATE_RIDGE_HALF = 300;
    /** Where it stands: on the middle line, in the front wall, its passage running in. */
    public static final Place GATE = new Place(0, FRONT, 0, 0);

    /** The passage's outline across: up its sides to its springing, round its arch. */
    public static List<Vec2> passage() {
        final List<Vec2> arch = new ArrayList<>(List.of(new Vec2(-PASSAGE, 0), new Vec2(PASSAGE, 0)));
        for (int k = 0; k <= 16; k++) {
            final double a = Math.PI * k / 16;
            arch.add(new Vec2(PASSAGE * Math.cos(a), SPRING + PASSAGE * Math.sin(a)));
        }
        return arch;
    }

    /**
     * The gate in the way, in its own frame: its body, its profile drawn through it from front to back,
     * the passage through it; its hip roof, two gables crossed.
     */
    public static List<Volume> gateShape() {
        final Extrusion body = new Extrusion(0, 0, 0, GATE_DEEP, List.of(new Vec2(-GATE_WIDE, 0), new Vec2(GATE_WIDE, 0),
                new Vec2(GATE_WIDE, GATE_HIGH), new Vec2(-GATE_WIDE, GATE_HIGH)), List.of(passage()));
        final double wide = GATE_WIDE + GATE_EAVES;
        final double deep = GATE_DEEP + GATE_EAVES;
        final Crossing roof = new Crossing(
                new Extrusion(0, 0, 90, wide, List.of(new Vec2(-deep, GATE_HIGH), new Vec2(deep, GATE_HIGH),
                        new Vec2(0, GATE_RIDGE))),
                new Extrusion(0, 0, 0, deep, List.of(new Vec2(-wide, GATE_HIGH), new Vec2(wide, GATE_HIGH),
                        new Vec2(GATE_RIDGE_HALF, GATE_RIDGE), new Vec2(-GATE_RIDGE_HALF, GATE_RIDGE))));
        return List.of(body, roof);
    }

    /** The gate drawn: its walls either side of the passage, over its arch, its roof. */
    private static List<Solid> gateSolids() {
        final List<Solid> solids = new ArrayList<>();
        final Material red = WALLED.of(Role.WALL);
        for (final double side : new double[] {-1, 1}) {
            solids.add(block("gate, " + (side < 0 ? "left" : "right") + " pier", Role.WALL, red,
                    Math.min(side * PASSAGE, side * GATE_WIDE), Math.max(side * PASSAGE, side * GATE_WIDE),
                    GATE_DEEP, 0, GATE_HIGH, 0, GATE_HIGH));
        }
        // over the arch: a slice between each two of its points, up to the top of the walls
        final List<Vec2> arch = passage().subList(2, passage().size());
        for (int k = 0; k + 1 < arch.size(); k++) {
            final Vec2 a = arch.get(k + 1);
            final Vec2 b = arch.get(k);
            solids.add(block("gate, over the arch " + k, Role.WALL, red, a.x(), b.x(), GATE_DEEP, a.z(), GATE_HIGH,
                    b.z(), GATE_HIGH));
        }
        // its hip roof: a box whose top is its ridge
        final double wide = GATE_WIDE + GATE_EAVES;
        final double deep = GATE_DEEP + GATE_EAVES;
        final List<Point> roof = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            final double side = (k & 1) == 0 ? -1 : 1;
            roof.add((k & 4) == 0 ? new Point(side * wide, ((k & 2) == 0 ? -1 : 1) * deep, GATE_HIGH)
                    : new Point(side * GATE_RIDGE_HALF, 0, GATE_RIDGE));
        }
        solids.add(new Solid("gate, roof", Role.ROOF, Material.GLAZED, roof));
        return solids;
    }

    /**
     * A block from {@code x0} to {@code x1} across, {@code deep} each way along, from {@code low0} up
     * to {@code high0} at {@code x0} and from {@code low1} to {@code high1} at {@code x1}.
     */
    private static Solid block(final String name, final Role role, final Material material, final double x0,
                               final double x1, final double deep, final double low0, final double high0,
                               final double low1, final double high1) {
        final List<Point> corners = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            final boolean far = (k & 1) != 0;
            corners.add(new Point(far ? x1 : x0, ((k & 2) == 0 ? -1 : 1) * deep,
                    (k & 4) == 0 ? (far ? low1 : low0) : (far ? high1 : high0)));
        }
        return new Solid(name, role, material, corners);
    }

    /** The main gate, one piece: in the way as it is shaped, its passage open to walk through. */
    public static Course.Piece gate() {
        final Block box = new Block(GATE.x(), GATE.z(), GATE_WIDE + GATE_EAVES, GATE_DEEP + GATE_EAVES, GATE.yaw(), 0,
                GATE_RIDGE);
        return Course.Piece.shaped(box, Course.Kind.BUILDING, KEYS + HALLS.size(), List.of(),
                gateShape().stream().map(v -> Volume.placed(v, GATE, 1)).toList(), GATE);
    }

    /** Its wall's palette: red plaster on stone, its coping's tiles green, as the side halls' roofs are. */
    private static final Palette WALLED = Temple.LACQUERED.with(Map.of(Role.ROOF, Material.GREEN_GLAZED));

    private static final Map<Integer, List<Solid>> WALL_DRAWN = new ConcurrentHashMap<>();

    private static final Map<Integer, List<Solid>> DRESSED = new ConcurrentHashMap<>();

    /**
     * Hall {@code k}, dressed: red lacquer, its roof's tiles as it says; or, past its halls, its gate.
     * Worked out once, when first wanted.
     */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            if (i == HALLS.size()) {
                return gateSolids();
            }
            final Hall hall = HALLS.get(i);
            final Building building = Temple.of(hall.size()).building();
            return building.dressed(List.of(building.option("Roof tiles", hall.tiles())), Temple.LACQUERED);
        });
    }

    /**
     * What stands in the courtyard: its halls, each one piece, in the way as each is shaped; round it
     * its wall, each unit a piece; and in its entrance, its gate, its passage open.
     */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < HALLS.size(); k++) {
            pieces.add(Town.building(solids(k), HALLS.get(k).place(), KEYS + k, Temple.of(HALLS.get(k).size()).shape(), 1));
        }
        for (int u = 0; u < WALL.units().size(); u++) {
            pieces.add(WALL.piece(u, WALL_KEYS + u));
        }
        pieces.add(gate());
        return pieces;
    }

    /** What building {@code key} is dressed as: a unit of the courtyard's wall, a hall of it, or one of the town's. */
    public static List<Solid> drawn(final int key) {
        if (key >= WALL_KEYS) {
            return WALL_DRAWN.computeIfAbsent(key - WALL_KEYS, u -> WALL.drawn(u, WALLED));
        }
        return key >= KEYS ? solids(key - KEYS) : Town.solids(key);
    }
}
