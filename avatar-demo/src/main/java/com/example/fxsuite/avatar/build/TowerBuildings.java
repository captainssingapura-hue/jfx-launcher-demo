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

    /** {@code tower}, as a building to dress: its walls, floors and roofs; its frame behind them; its void decks' railings. */
    public static Building of(final Tower tower) {
        final List<Surface> walls = new ArrayList<>();
        final List<Plate> plates = new ArrayList<>();
        surfaces(tower, walls, plates);
        final Form skeleton = tower.skeleton();
        final List<Building.Structure> structure = new ArrayList<>();
        // what holds it up, seen through its glass: its columns and its cores, the same all the way up
        structure.add((sk, palette) -> frame(tower, palette));
        for (int s = 0; s < tower.sections().size(); s++) {
            final Tower.Section section = tower.sections().get(s);
            if (section.open()) {
                final double base = tower.base(s);
                structure.add((sk, palette) -> railings(section, base, palette));
            }
        }
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        for (final Tower.Section section : tower.sections()) {
            choices.put(section.name(), options(section));
        }
        return new Building(tower.name(), skeleton, walls, plates, structure, choices, Palette.ALL);
    }

    private static List<com.example.fxsuite.avatar.sim.Vec2> vec(final List<Plan.Pt> points) {
        return points.stream().map(p -> new com.example.fxsuite.avatar.sim.Vec2(p.x(), p.y())).toList();
    }

    /** How high a void deck's railing stands, how far in from its edge; how far its cores run on over its roof. */
    public static final double RAILING = 110;
    public static final double RAILING_IN = 15;
    public static final double OVERRUN = 400;

    /** A column of a tower's frame as it stands in one of its sections: where, its section, how big across there. */
    public record Standing(Plan.Pt at, Frame.Section section, double size) {

        /** Its footprint. */
        public List<Plan.Pt> footprint() {
            return Frame.footprint(new Frame.Column(at, section, size), size);
        }
    }

    /**
     * Section {@code s}'s columns: its frame's within its outline, as big as they are half way up it
     * (thinner the higher); any too near its wall set in behind it, its face {@link Frame#BEHIND} off
     * the glass; on a void deck, none inside its enclosed part.
     */
    public static List<Standing> columns(final Tower tower, final int s) {
        return standing(tower, s, false);
    }

    /**
     * All section {@code s}'s columns, as {@link #columns} has them, but those inside a void deck's
     * enclosed part too: walled in there, but standing all the same, as its plan shows them.
     */
    public static List<Standing> standing(final Tower tower, final int s) {
        return standing(tower, s, true);
    }

    private static List<Standing> standing(final Tower tower, final int s, final boolean walledIn) {
        final Tower.Section section = tower.sections().get(s);
        final Plan.Layout layout = section.plan().layout();
        final List<Plan.Pt> outline = layout.shaped().getFirst();
        final Plan.Layout enclosed = walledIn || section.enclosed() == null ? null : section.enclosed().layout();
        final double middle = tower.base(s) + section.tall() / 2;
        final List<Standing> standing = new ArrayList<>();
        for (final Frame.Column column : tower.structure().in(layout)) {
            if (enclosed != null && enclosed.inside(column.at())
                    && enclosed.shaped().stream().noneMatch(loop -> onOutline(column.at(), loop))) {
                continue;
            }
            final double size = Frame.size(column, middle, tower.height());
            standing.add(new Standing(clear(outline, column.at(), size / 2 + Frame.BEHIND), column.section(), size));
        }
        return standing;
    }

    /** {@code p} moved in off each piece of {@code outline} it is nearer than {@code need}, to {@code need} off it. */
    private static Plan.Pt clear(final List<Plan.Pt> outline, final Plan.Pt p, final double need) {
        double x = p.x();
        double y = p.y();
        for (int k = 0; k < outline.size(); k++) {
            final Plan.Pt a = outline.get(k);
            final Plan.Pt b = outline.get((k + 1) % outline.size());
            final Plan.Pt at = new Plan.Pt(x, y);
            final double gap = toSegment(at, a, b);
            if (gap < need) {
                final double length = a.distance(b);
                // only off a piece it is beside, not past its end; inside on its left, the outline anticlockwise
                final double along = ((x - a.x()) * (b.x() - a.x()) + (y - a.y()) * (b.y() - a.y())) / (length * length);
                if (along < 0 || along > 1) {
                    continue;
                }
                x += -(b.y() - a.y()) / length * (need - gap);
                y += (b.x() - a.x()) / length * (need - gap);
            }
        }
        return new Plan.Pt(x, y);
    }

    /** A void deck's columns standing in the open (section {@code s}'s). */
    public static List<Standing> deckColumns(final Tower tower, final int s) {
        return columns(tower, s);
    }

    /** A void deck's open edges: the pieces of its outline its enclosed part is not walled along. */
    public static List<Plan.Pt[]> openEdges(final Tower.Section section) {
        final List<Plan.Pt> outline = section.plan().layout().shaped().getFirst();
        final Plan.Layout enclosed = section.enclosed() == null ? null : section.enclosed().layout();
        final List<Plan.Pt[]> edges = new ArrayList<>();
        for (int k = 0; k < outline.size(); k++) {
            final Plan.Pt a = outline.get(k);
            final Plan.Pt b = outline.get((k + 1) % outline.size());
            final Plan.Pt middle = new Plan.Pt((a.x() + b.x()) / 2, (a.y() + b.y()) / 2);
            if (enclosed == null || enclosed.shaped().stream().noneMatch(loop -> onOutline(middle, loop))) {
                edges.add(new Plan.Pt[] {a, b});
            }
        }
        return edges;
    }

    /** The railing along an open edge from {@code a} to {@code b}: a strip just inside it, its inside on its left. */
    private static List<Plan.Pt> railing(final Plan.Pt a, final Plan.Pt b, final double in, final double thick) {
        final double width = a.distance(b);
        final double ix = -(b.y() - a.y()) / width;
        final double iy = (b.x() - a.x()) / width;
        return List.of(new Plan.Pt(a.x() + ix * in, a.y() + iy * in), new Plan.Pt(b.x() + ix * in, b.y() + iy * in),
                new Plan.Pt(b.x() + ix * (in + thick), b.y() + iy * (in + thick)),
                new Plan.Pt(a.x() + ix * (in + thick), a.y() + iy * (in + thick)));
    }

    /**
     * Its frame drawn: in each section, its columns standing from its floor to its top, as big as they
     * are there; its cores, all the way up, and on over its roof.
     */
    private static List<Solid> frame(final Tower tower, final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        for (int s = 0; s < tower.sections().size(); s++) {
            final double base = tower.base(s);
            final double top = base + tower.sections().get(s).tall();
            int k = 0;
            for (final Standing column : columns(tower, s)) {
                solids.addAll(stood(tower.sections().get(s).name() + ", column " + k++, Role.STRUCTURE,
                        palette.of(Role.STRUCTURE), column.footprint(), base, top));
            }
        }
        int k = 0;
        for (final List<Plan.Pt> core : tower.structure().cores()) {
            solids.addAll(stood("core " + k++, Role.STRUCTURE, Material.CONCRETE, core, 0, tower.height() + OVERRUN));
        }
        return solids;
    }

    /** A void deck's railing, along its open edges: glass under a metal rail. */
    private static List<Solid> railings(final Tower.Section section, final double base, final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        int k = 0;
        for (final Plan.Pt[] edge : openEdges(section)) {
            solids.add(block(section.name() + ", railing " + k, Role.GLAZING, palette.of(Role.GLAZING),
                    railing(edge[0], edge[1], RAILING_IN, 3), base, base + RAILING - 6));
            solids.add(block(section.name() + ", rail " + k++, Role.FRAME, palette.of(Role.FRAME),
                    railing(edge[0], edge[1], RAILING_IN - 2, 7), base + RAILING - 6, base + RAILING));
        }
        return solids;
    }

    /** An outline seen whole from its middle (a rectangle, an octagon, a cross, a circle) stood up: a wedge to each side. */
    private static List<Solid> stood(final String name, final Role role, final Material material, final List<Plan.Pt> outline,
                                     final double low, final double high) {
        final double mx = outline.stream().mapToDouble(Plan.Pt::x).average().orElseThrow();
        final double my = outline.stream().mapToDouble(Plan.Pt::y).average().orElseThrow();
        final List<Solid> solids = new ArrayList<>();
        for (int k = 0; k < outline.size(); k++) {
            final Plan.Pt a = outline.get(k);
            final Plan.Pt b = outline.get((k + 1) % outline.size());
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                final double up = (c & 4) == 0 ? low : high;
                final Plan.Pt at = (c & 2) == 0 ? new Plan.Pt(mx, my) : (c & 1) == 0 ? a : b;
                corners.add(new Point(at.x(), at.y(), up));
            }
            solids.add(new Solid(name + ", " + k, role, material, corners));
        }
        return solids;
    }

    /** A quadrilateral footprint stood up from {@code low} to {@code high}, as one solid. */
    private static Solid block(final String name, final Role role, final Material material, final List<Plan.Pt> quad,
                               final double low, final double high) {
        // corners: along its first side (bit 0), across to its far side (bit 1), up (bit 2)
        final Plan.Pt[] at = {quad.get(0), quad.get(1), quad.get(3), quad.get(2)};
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final Plan.Pt p = at[c & 3];
            corners.add(new Point(p.x(), p.y(), (c & 4) == 0 ? low : high));
        }
        return new Solid(name, role, material, corners);
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

    /** How thick a parapet is in the way: a roof's edge to stand behind. */
    public static final double PARAPET_THICK = 30;

    /**
     * {@code tower} in the way as it is shaped, in its own frame (x across, z along): each section its
     * plan's outline, its corners shaped and its curves as they are drawn, stood up from its floor to
     * its roof; and round each roof, where no section above stands on it, its parapet, a strip inside
     * each piece of its edge. So a round tower is round, an L is open in its corner, a setback's
     * terrace is stood on behind its parapet.
     */
    public static List<com.example.fxsuite.avatar.sim.Volume> shape(final Tower tower) {
        final List<com.example.fxsuite.avatar.sim.Volume> shape = new ArrayList<>();
        final List<Tower.Section> sections = tower.sections();
        for (int s = 0; s < sections.size(); s++) {
            final Tower.Section section = sections.get(s);
            final Plan.Layout layout = section.plan().layout();
            final Plan.Layout above = s + 1 < sections.size() ? sections.get(s + 1).plan().layout() : null;
            final double base = tower.base(s);
            final double top = base + section.tall();
            final List<Plan.Pt> outside = layout.shaped().getFirst();
            if (section.open()) {
                // a void deck: open but for its frame's columns and cores, its enclosed part and its railings
                for (final Standing column : columns(tower, s)) {
                    shape.add(new com.example.fxsuite.avatar.sim.Prism(vec(column.footprint()), base, top));
                }
                for (final List<Plan.Pt> core : tower.structure().cores()) {
                    shape.add(new com.example.fxsuite.avatar.sim.Prism(vec(core), base, top));
                }
                if (section.enclosed() != null) {
                    shape.add(new com.example.fxsuite.avatar.sim.Prism(vec(section.enclosed().layout().shaped().getFirst()),
                            base, top));
                }
                for (final Plan.Pt[] edge : openEdges(section)) {
                    shape.add(new com.example.fxsuite.avatar.sim.Prism(vec(railing(edge[0], edge[1], RAILING_IN - 2, 7)),
                            base, base + RAILING));
                }
            } else {
                shape.add(new com.example.fxsuite.avatar.sim.Prism(vec(outside), base, top));
            }
            for (int k = 0; k < outside.size(); k++) {
                final Plan.Pt a = outside.get(k);
                final Plan.Pt b = outside.get((k + 1) % outside.size());
                final Plan.Pt middle = new Plan.Pt((a.x() + b.x()) / 2, (a.y() + b.y()) / 2);
                if (above != null && above.shaped().stream().anyMatch(other -> onOutline(middle, other))) {
                    continue;
                }
                // inside the edge: its outline anticlockwise, so its inside on its left
                final double width = a.distance(b);
                final double ix = -(b.y() - a.y()) / width * PARAPET_THICK;
                final double iy = (b.x() - a.x()) / width * PARAPET_THICK;
                shape.add(new com.example.fxsuite.avatar.sim.Prism(List.of(
                        new com.example.fxsuite.avatar.sim.Vec2(a.x(), a.y()),
                        new com.example.fxsuite.avatar.sim.Vec2(b.x(), b.y()),
                        new com.example.fxsuite.avatar.sim.Vec2(b.x() + ix, b.y() + iy),
                        new com.example.fxsuite.avatar.sim.Vec2(a.x() + ix, a.y() + iy)), top, top + PARAPET));
            }
        }
        // its cores, on over its roof: their lift overrun
        for (final List<Plan.Pt> core : tower.structure().cores()) {
            shape.add(new com.example.fxsuite.avatar.sim.Prism(vec(core), tower.height(), tower.height() + OVERRUN));
        }
        return shape;
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
            // walled round its outline; a void deck only round its enclosed part, if it has one
            final Plan.Layout walled = !section.open() ? layout
                    : section.enclosed() == null ? null : section.enclosed().layout();
            final List<List<Plan.Pt>> wallLoops = walled == null ? List.of() : walled.shaped();
            for (int l = 0; l < wallLoops.size(); l++) {
                final List<Plan.Pt> loop = wallLoops.get(l);
                for (int k = 0; k < loop.size(); k++) {
                    final Plan.Pt a = loop.get(k);
                    final Plan.Pt b = loop.get((k + 1) % loop.size());
                    final double width = a.distance(b);
                    final double dx = (b.x() - a.x()) / width;
                    final double dy = (b.y() - a.y()) / width;
                    // its columns: where its plan's columns stand along it, and its ends
                    final java.util.TreeSet<Double> lines = new java.util.TreeSet<>(List.of(0.0, width));
                    for (final Plan.Pt c : walled.allColumns().values()) {
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
                    final double parapet = continued || section.open() ? 0 : PARAPET;
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
