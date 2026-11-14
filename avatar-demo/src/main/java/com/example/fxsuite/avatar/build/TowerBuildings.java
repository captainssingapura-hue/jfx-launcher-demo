package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Facade.Pattern;
import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tower assembled into a building ({@link Building}): its skeleton from its stack ({@link
 * Tower#skeleton}); its surfaces from the same stack, made apart from its skeleton; the two checked
 * against each other ({@link #mismatches}); and the ways it can be dressed, a choice for each of its
 * sections, so a stone podium can carry a glass shaft under a finned top.
 *
 * <p>Its surfaces, for each section: a wall for each straight run of its outline, and one for each
 * straight piece of each curve, over all its floors, its columns where its plan's stand along it, a
 * parapet on it where nothing stands on it above; a floor at each of its levels between; its roof.
 */
public final class TowerBuildings {

    private TowerBuildings() {
    }

    /** How high a parapet stands round a roof, or a terrace. */
    public static final double PARAPET = 110;

    /** {@code tower}, as a building to dress. */
    public static Building of(final Tower tower) {
        final List<Surface> walls = new ArrayList<>();
        final List<Plate> plates = new ArrayList<>();
        surfaces(tower, walls, plates);
        // the columns that show through its glass: its skeleton's upright edges standing clear of its outline
        final List<Plan.Layout> layouts = tower.sections().stream().map(s -> s.plan().layout()).toList();
        final Form skeleton = tower.skeleton();
        final Building.Structure columns = Building.Structure.members("column", edge -> {
            final Form.Vertex a = skeleton.vertices().get(edge.from());
            final Form.Vertex b = skeleton.vertices().get(edge.to());
            if (a.across() != b.across() || a.along() != b.along()) {
                return false;
            }
            final int section = sectionAt(tower, Math.min(a.up(), b.up()));
            return clearOfOutline(layouts.get(section), new Plan.Pt(a.across(), a.along()));
        }, 50, Role.STRUCTURE);
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        for (final Tower.Section section : tower.sections()) {
            choices.put(section.name(), options(section));
        }
        return new Building(tower.name(), skeleton, walls, plates, List.of(columns), choices, Palette.ALL);
    }

    /** Which section stands from {@code up}. */
    private static int sectionAt(final Tower tower, final double up) {
        for (int s = tower.sections().size() - 1; s >= 0; s--) {
            if (up >= tower.base(s) - 1e-6) {
                return s;
            }
        }
        return 0;
    }

    /** Whether {@code p} stands clear of {@code plan}'s outline, inside it: half a metre at least. */
    private static boolean clearOfOutline(final Plan.Layout plan, final Plan.Pt p) {
        for (final List<Plan.Pt> loop : plan.shaped()) {
            for (int k = 0; k < loop.size(); k++) {
                if (toSegment(p, loop.get(k), loop.get((k + 1) % loop.size())) < 50) {
                    return false;
                }
            }
        }
        return plan.inside(p);
    }

    private static double toSegment(final Plan.Pt p, final Plan.Pt a, final Plan.Pt b) {
        final double dx = b.x() - a.x();
        final double dy = b.y() - a.y();
        final double length = dx * dx + dy * dy;
        final double t = length == 0 ? 0 : Math.clamp(((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / length, 0, 1);
        return Math.hypot(p.x() - (a.x() + t * dx), p.y() - (a.y() + t * dy));
    }

    /** The kind of a section's walls, floors and roof: how its options name them. */
    public static String walls(final Tower.Section section) {
        return section.name() + " walls";
    }

    public static String floors(final Tower.Section section) {
        return section.name() + " floors";
    }

    public static String roof(final Tower.Section section) {
        return section.name() + " roof";
    }

    /**
     * Its surfaces: for each section, a wall on each straight piece of each of its outlines, seen from
     * outside left to right as its outline runs (anticlockwise round its outside, so its outside on
     * its right); a floor at each level between its first and its last; a roof at its top.
     */
    static void surfaces(final Tower tower, final List<Surface> walls, final List<Plate> plates) {
        final List<Tower.Section> sections = tower.sections();
        for (int s = 0; s < sections.size(); s++) {
            final Tower.Section section = sections.get(s);
            final Plan.Layout layout = section.plan().layout();
            final Plan.Layout above = s + 1 < sections.size() ? sections.get(s + 1).plan().layout() : null;
            final double base = tower.base(s);
            final double[] floors = new double[section.floors()];
            for (int f = 0; f < floors.length; f++) {
                floors[f] = f * section.height();
            }
            final List<List<Plan.Pt>> loops = layout.shaped();
            for (int l = 0; l < loops.size(); l++) {
                final List<Plan.Pt> loop = loops.get(l);
                for (int k = 0; k < loop.size(); k++) {
                    final Plan.Pt a = loop.get(k);
                    final Plan.Pt b = loop.get((k + 1) % loop.size());
                    final double width = a.distance(b);
                    final double dx = (b.x() - a.x()) / width;
                    final double dy = (b.y() - a.y()) / width;
                    // its columns: where its plan's columns stand along it, and its ends
                    final java.util.TreeSet<Double> lines = new java.util.TreeSet<>(List.of(0.0, width));
                    for (final Plan.Pt c : layout.allColumns().values()) {
                        if (toSegment(c, a, b) < Plan.TOLERANCE) {
                            final double along = Math.round(c.distance(a) * 1000) / 1000.0;
                            if (along > Plan.TOLERANCE && along < width - Plan.TOLERANCE) {
                                lines.add(along);
                            }
                        }
                    }
                    // a parapet, unless the section above stands on it, its wall going on up
                    final Plan.Pt middle = new Plan.Pt((a.x() + b.x()) / 2, (a.y() + b.y()) / 2);
                    final boolean continued = above != null && above.shaped().stream()
                            .anyMatch(other -> onOutline(middle, other));
                    final double parapet = continued ? 0 : PARAPET;
                    walls.add(new Surface(section.name() + ", wall " + l + "." + k, walls(section),
                            new Point(a.x(), a.y(), base), new Point(dx, dy, 0), new Point(0, 0, 1),
                            new Point(dy, -dx, 0), width, section.tall() + parapet,
                            lines.stream().mapToDouble(Double::doubleValue).toArray(), floors, parapet));
                }
            }
            // its floors, between its first level and its last; its roof (the outside of its outlines only, as yet)
            final List<Plan.Pt> outside = loops.getFirst();
            for (int f = 1; f < section.floors(); f++) {
                plates.add(new Plate(section.name() + ", floor " + f, floors(section), outside,
                        base + f * section.height()));
            }
            plates.add(new Plate(section.name() + ", roof", roof(section), outside, base + section.tall()));
        }
    }

    private static boolean onOutline(final Plan.Pt p, final List<Plan.Pt> loop) {
        for (int k = 0; k < loop.size(); k++) {
            if (toSegment(p, loop.get(k), loop.get((k + 1) % loop.size())) < Plan.TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where its surfaces and its skeleton, made apart, do not agree: a wall's line of columns, at its
     * foot or its head, with no column of the skeleton there.
     */
    public static List<String> mismatches(final Building building, final Tower tower) {
        final Form skeleton = building.skeleton();
        final Map<String, Integer> at = new HashMap<>();
        for (int v = 0; v < skeleton.vertices().size(); v++) {
            final Form.Vertex p = skeleton.vertices().get(v);
            at.put(key(p.across(), p.along(), p.up()), v);
        }
        final java.util.Set<Long> upright = new java.util.HashSet<>();
        for (final Form.Edge e : skeleton.edges()) {
            final Form.Vertex a = skeleton.vertices().get(e.from());
            final Form.Vertex b = skeleton.vertices().get(e.to());
            if (a.across() == b.across() && a.along() == b.along()) {
                upright.add((long) e.from());
                upright.add((long) e.to());
            }
        }
        final List<String> mismatches = new ArrayList<>();
        for (final Surface wall : building.surfaces()) {
            final double head = wall.height() - wall.parapet();
            for (final double u : wall.columns()) {
                final boolean end = u == 0 || u == wall.width();
                for (final double v : new double[] {0, head}) {
                    final Point p = wall.at(u, v, 0);
                    final Integer vertex = at.get(key(p.across(), p.along(), p.up()));
                    if (vertex == null) {
                        mismatches.add(wall.name() + ": no point of the skeleton at its " + (v == 0 ? "foot" : "head")
                                + ", " + Math.round(u) + " along");
                    } else if (!end && !upright.contains((long) vertex)) {
                        mismatches.add(wall.name() + ": no column of the skeleton on its line " + Math.round(u)
                                + " along");
                    }
                }
            }
        }
        return mismatches;
    }

    private static String key(final double x, final double y, final double z) {
        return Math.round(x * 10) + "," + Math.round(y * 10) + "," + Math.round(z * 10);
    }

    /**
     * How a section can be dressed: a curtain wall, glass over dark spandrels; the same with fins;
     * ribbon windows in its walls; a base of shopfronts. A podium's own first is its base; anything
     * else's, its curtain wall. Its floors and roof, slabs, whatever its walls.
     */
    static List<Building.Option> options(final Tower.Section section) {
        final String walls = walls(section);
        final Map<String, Design> slabs = Map.of(
                floors(section), Design.slab(Role.STRUCTURE, -25, 0),
                roof(section), Design.slab(Role.STRUCTURE, -30, 0).and(Design.slab(Role.ROOF, 0, 4)));
        final Design curtain = new Facade("curtain wall", Role.FRAME, 12, Pattern.all(Infills.curtain(90)), List.of(),
                true);
        final Design finned = new Facade("finned curtain wall", Role.FRAME, 12, Pattern.all(Infills.curtain(90)),
                List.of(Trims.fins(Role.DRESSING, 1, 20, 60)), true);
        final Design ribbons = new Facade("ribbon windows", Role.WALL, 0, Pattern.all(Infills.ribbon(90, 260, 0)),
                List.of(Trims.floorBands(Role.DRESSING, 25, 25)), true);
        final Design base = new Facade("shopfronts", Role.BASE, 0, Pattern.all(Infills.shopfront(45, 60, 50)), List.of(),
                true);
        final List<Building.Option> options = new ArrayList<>();
        for (final Object[] o : new Object[][] {{"Curtain wall", curtain}, {"Finned curtain wall", finned},
                {"Ribbon windows", ribbons}, {"Shopfront base", base}}) {
            final Map<String, Design> designs = new HashMap<>(slabs);
            designs.put(walls, (Design) o[1]);
            options.add(new Building.Option((String) o[0], designs));
        }
        if (section.name().equals("podium")) {
            options.addFirst(options.removeLast());
        }
        return options;
    }
}
