package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.PlayWorld;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.build.StandIns;
import com.example.fxsuite.avatar.build.Town;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Terrain;

import javafx.scene.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The lifts yard: a frame of four floors (the ground, and three over it on columns), open all round but for a rail at
 * each upper floor's edge, and one of each lift at work in it: a paternoster on its left, a shuttle lift on its right,
 * an escalator from the ground up to its first floor at its back. Each floor cut where a lift goes through it. On level
 * ground ruled every 10 m; to ride, step off, walk and ride again, with whoever is brought.
 */
final class LiftYard implements PlayWorld {

    /** The keys its parts are drawn by: the frame, a cabin, the car, a car's door, a landing's door, a step. */
    static final int KEY = 970_000;
    static final int FRAME = KEY;
    static final int CABIN = KEY + 1;
    static final int CAR = KEY + 2;
    static final int CAR_DOOR = KEY + 3;
    static final int LANDING_DOOR = KEY + 4;
    static final int STEP = KEY + 5;
    static final int KEYS = 6;

    /** Its floors' tops, from the ground up; how thick a floor is; how far its frame reaches across and along. */
    static final List<Double> FLOORS = List.of(0.0, 400.0, 800.0, 1200.0);
    static final double SLAB = 25;
    static final double HALF_ACROSS = 900;
    static final double HALF_ALONG = 500;
    /** Its rails: so high, so thick. */
    static final double RAIL = 100;
    static final double RAIL_THICK = 5;
    /** How far the ground reaches out from its middle. */
    static final double GROUND = 4_000;

    final Paternoster paternoster = new Paternoster(-590, -390, 0, -300, FLOORS.getLast() + 300, 14, 30);
    final ShuttleLift shuttle = new ShuttleLift(260, 0, FLOORS, 160, 100);
    final Escalator escalator = new Escalator(0, -HALF_ALONG, FLOORS.get(1), 30, 50);

    @Override
    public String title() {
        return "Lifts yard";
    }

    @Override
    public String description() {
        return "A frame of four floors with a paternoster, a shuttle lift and an escalator at work in it.";
    }

    /** Everything that moves in it, each where its way has it. */
    List<Mover> movers() {
        final List<Mover> movers = new ArrayList<>(paternoster.movers(CABIN));
        movers.addAll(shuttle.movers(CAR, CAR_DOOR, LANDING_DOOR));
        movers.addAll(escalator.movers(STEP));
        return movers;
    }

    /** What of its frame is in the way: its floors, columns, rails, shafts and the escalator's balustrades. */
    List<Solid> standing() {
        final List<Solid> solids = new ArrayList<>();
        final List<double[]> holes = List.of(paternoster.hole(), shuttle.hole());
        for (int f = 1; f < FLOORS.size(); f++) {
            final double top = FLOORS.get(f);
            solids.addAll(cut("floor " + f, -HALF_ACROSS, HALF_ACROSS, -HALF_ALONG, HALF_ALONG, top - SLAB, top, holes));
            solids.addAll(rails(f));
        }
        final double roof = FLOORS.getLast();
        for (final double x : new double[] {-HALF_ACROSS + 20, HALF_ACROSS - 20}) {
            for (final double z : new double[] {-HALF_ALONG + 20, HALF_ALONG - 20}) {
                solids.add(new Box("column", Role.STRUCTURE, Material.CONCRETE, x - 20, x + 20, z - 20, z + 20, 0,
                        roof - SLAB).solid());
            }
        }
        // the shuttle's shaft, storey by storey, a doorway at each landing; over its top floor, its machine room
        for (int f = 0; f < FLOORS.size(); f++) {
            final double from = FLOORS.get(f);
            final double to = f + 1 < FLOORS.size() ? FLOORS.get(f + 1) - SLAB : from + 300;
            shuttle.shaft(from, to, true).forEach(b -> solids.add(b.solid()));
        }
        final double[] h = shuttle.hole();
        solids.add(new Box("shaft, cap", Role.ROOF, Material.CONCRETE, h[0] - ShuttleLift.SHAFT_WALL,
                h[1] + ShuttleLift.SHAFT_WALL, h[2] - ShuttleLift.SHAFT_WALL, h[3] + ShuttleLift.SHAFT_WALL, roof + 300,
                roof + 300 + SLAB).solid());
        // the paternoster's shafts: behind and either side, open in front, up past its turn over the top
        final double[] p = paternoster.hole();
        final double over = paternoster.top() + paternoster.radius() + Paternoster.HIGH + 40;
        solids.add(new Box("paternoster, back", Role.WALL, Material.CONCRETE, p[0] - 10, p[1] + 10, p[2] - 10, p[2], 0,
                over).solid());
        solids.add(new Box("paternoster, left", Role.WALL, Material.CONCRETE, p[0] - 10, p[0], p[2], p[3], 0, over)
                .solid());
        solids.add(new Box("paternoster, right", Role.WALL, Material.CONCRETE, p[1], p[1] + 10, p[2], p[3], 0, over)
                .solid());
        solids.add(new Box("paternoster, cap", Role.ROOF, Material.CONCRETE, p[0] - 10, p[1] + 10, p[2] - 10, p[3], over,
                over + SLAB).solid());
        solids.addAll(escalator.balustrades());
        solids.addAll(escalator.combs());
        return solids;
    }

    /** All of its frame that is drawn: what is in the way, and the escalator's truss under its stair. */
    List<Solid> drawn() {
        final List<Solid> solids = new ArrayList<>(standing());
        solids.addAll(escalator.truss());
        return solids;
    }

    /** Floor {@code f}'s rail round its edge: open at the first floor where the escalator comes up to it. */
    private List<Solid> rails(final int f) {
        final double top = FLOORS.get(f);
        final double a = HALF_ACROSS;
        final double l = HALF_ALONG;
        final double t = RAIL_THICK;
        final List<Solid> rails = new ArrayList<>(List.of(
                new Box("rail, front", Role.FRAME, Material.METAL, -a, a, l - t, l, top, top + RAIL).solid(),
                new Box("rail, left", Role.FRAME, Material.METAL, -a, -a + t, -l, l, top, top + RAIL).solid(),
                new Box("rail, right", Role.FRAME, Material.METAL, a - t, a, -l, l, top, top + RAIL).solid()));
        if (f == 1) {
            final double gap = Escalator.WIDE / 2 + 10;
            rails.add(new Box("rail, back, left", Role.FRAME, Material.METAL, -a, escalator.x() - gap, -l, -l + t, top,
                    top + RAIL).solid());
            rails.add(new Box("rail, back, right", Role.FRAME, Material.METAL, escalator.x() + gap, a, -l, -l + t, top,
                    top + RAIL).solid());
        } else {
            rails.add(new Box("rail, back", Role.FRAME, Material.METAL, -a, a, -l, -l + t, top, top + RAIL).solid());
        }
        return rails;
    }

    /**
     * A floor from {@code x0} to {@code x1} across, {@code z0} to {@code z1} along, {@code u0} to {@code u1} up, cut
     * where {@code holes} (each min x, max x, min z, max z) go through it: in boxes, each square, round the holes.
     */
    static List<Solid> cut(final String name, final double x0, final double x1, final double z0, final double z1,
                           final double u0, final double u1, final List<double[]> holes) {
        final List<Double> xs = new ArrayList<>(List.of(x0, x1));
        for (final double[] h : holes) {
            xs.add(Math.clamp(h[0], x0, x1));
            xs.add(Math.clamp(h[1], x0, x1));
        }
        final List<Double> across = xs.stream().distinct().sorted().toList();
        final List<Solid> boxes = new ArrayList<>();
        for (int i = 0; i + 1 < across.size(); i++) {
            final double a = across.get(i);
            final double b = across.get(i + 1);
            final double mid = (a + b) / 2;
            // along this strip, what the holes crossing it leave
            final List<double[]> cuts = holes.stream().filter(h -> h[0] < mid && mid < h[1])
                    .sorted(Comparator.comparingDouble(h -> h[2])).toList();
            double from = z0;
            for (final double[] h : cuts) {
                if (h[2] > from) {
                    boxes.add(new Box(name, Role.BASE, Material.CONCRETE, a, b, from, Math.min(h[2], z1), u0, u1)
                            .solid());
                }
                from = Math.max(from, h[3]);
            }
            if (z1 > from) {
                boxes.add(new Box(name, Role.BASE, Material.CONCRETE, a, b, from, z1, u0, u1).solid());
            }
        }
        return boxes;
    }

    /** Its parts drawn by their keys, as buildings are. */
    void register() {
        final List<List<Solid>> drawn = List.of(drawn(), solids(Paternoster.cabin()), solids(ShuttleLift.car()),
                solids(ShuttleLift.leaf("car door", ShuttleLift.DOORWAY / 2, ShuttleLift.DOOR_HIGH)),
                solids(ShuttleLift.leaf("landing door", ShuttleLift.LANDING / 2, ShuttleLift.LANDING_HIGH)),
                solids(escalator.stepBoxes()));
        Sites.register(KEY, KEY + KEYS, k -> drawn.get(k));
    }

    private static List<Solid> solids(final List<Box> boxes) {
        return boxes.stream().map(Box::solid).toList();
    }

    @Override
    public void lay(final Course.Layout world) {
        register();
        final Terrain flat = new Terrain("lifts yard", -GROUND, -GROUND, GROUND, GROUND, 1_000, 0, 0, Terrain.Noise.NONE,
                List.of(), List.of());
        final List<Course.Piece> pieces = new ArrayList<>();
        pieces.add(Town.building(drawn(), new Place(0, 0, 0, 0), FRAME, StandIns.prisms(standing()), 1));
        movers().forEach(m -> pieces.add(m.piece()));
        world.land(new Land(flat.survey(), List.of())).ruled(true).pieces(pieces);
    }

    @Override
    public Scenery scenery() {
        return Scenery.still(new Group());
    }

    /** Before its front, on the ground, facing it. */
    @Override
    public Spot start() {
        return new Spot("before it", new Place(0, HALF_ALONG + 400, 0, 180));
    }

    /** In a row along its front, further out, nose to tail, facing across. */
    @Override
    public List<Spot> parked(final List<Double> lengths) {
        double total = 0;
        for (final double l : lengths) {
            total += l + 200;
        }
        double at = -total / 2;
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < lengths.size(); k++) {
            at += lengths.get(k) / 2;
            spots.add(new Spot("parked " + (k + 1), new Place(at, HALF_ALONG + 1_100, 0, 90)));
            at += lengths.get(k) / 2 + 200;
        }
        return spots;
    }

    /** Along its front, a little out from it, facing it. */
    @Override
    public List<Spot> standing(final int count) {
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            spots.add(new Spot("standing " + (k + 1), new Place((k - (count - 1) / 2.0) * 150, HALF_ALONG + 250, 0, 180)));
        }
        return spots;
    }
}
