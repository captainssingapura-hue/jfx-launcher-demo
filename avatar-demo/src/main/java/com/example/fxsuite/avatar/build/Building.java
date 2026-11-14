package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A building, composed: its skeleton ({@link Form}), the lines its shape is drawn by; its
 * high-level surfaces ({@link Surface}), each a rectangle on it with the lines of its structure
 * across it; and the structure that shows of itself ({@link #structure}), worked out from its
 * skeleton. How its surfaces are dressed is not its to say: an {@link Option} says, a design for
 * each kind of surface it names. Its options come in choices, each about some of its surfaces (its
 * facade; its ground floor), one option taken from each; so the same building can be dressed as
 * many ways as its choices' options can be put together; and each way built in any of its palettes
 * ({@link Palette}): concrete, brick, stone.
 *
 * @param name      what it is
 * @param skeleton  its lines
 * @param surfaces  its surfaces
 * @param plates    its level surfaces of any shape: floors, roofs, terraces
 * @param sheets    its faces of any shape, flat or curved: walls with arched windows, swept roofs
 * @param structure what of its structure shows, from its skeleton
 * @param choices   by what each is about, the options to choose from, the first of each its own
 * @param palettes  what it can be built in, the first its own
 */
public record Building(String name, Form skeleton, List<Surface> surfaces, List<Plate> plates, List<Sheet> sheets,
                       List<Structure> structure, Map<String, List<Option>> choices, List<Palette> palettes) {

    /** One with no faces but its rectangles and its level surfaces. */
    public Building(final String name, final Form skeleton, final List<Surface> surfaces, final List<Plate> plates,
                    final List<Structure> structure, final Map<String, List<Option>> choices, final List<Palette> palettes) {
        this(name, skeleton, surfaces, plates, List.of(), structure, choices, palettes);
    }

    /** One with no level surfaces but its rectangles. */
    public Building(final String name, final Form skeleton, final List<Surface> surfaces, final List<Structure> structure,
                    final Map<String, List<Option>> choices, final List<Palette> palettes) {
        this(name, skeleton, surfaces, List.of(), structure, choices, palettes);
    }

    public Building {
        surfaces = List.copyOf(surfaces);
        plates = List.copyOf(plates);
        sheets = List.copyOf(sheets);
        structure = List.copyOf(structure);
        // in the order given, each choice's options in theirs
        final java.util.LinkedHashMap<String, List<Option>> kept = new java.util.LinkedHashMap<>();
        choices.forEach((choice, options) -> kept.put(choice, List.copyOf(options)));
        choices = java.util.Collections.unmodifiableMap(kept);
        palettes = List.copyOf(palettes);
    }

    /** Its own way of being dressed: the first option of each choice. */
    public List<Option> own() {
        return choices.values().stream().map(List::getFirst).toList();
    }

    /** Structure worked out from a skeleton: its edges given bodies, those it picks. */
    @FunctionalInterface
    public interface Structure {
        List<Solid> of(Form skeleton, Palette palette);

        /**
         * The edges {@code which} picks, each a square member {@code size} across doing {@code role},
         * centred on its line: columns, beams.
         */
        static Structure members(final String called, final Predicate<Form.Edge> which, final double size,
                                 final Role role) {
            return (skeleton, palette) -> {
                final List<Solid> members = new ArrayList<>();
                int k = 0;
                for (final Form.Edge edge : skeleton.edges()) {
                    if (!which.test(edge)) {
                        continue;
                    }
                    // a curved edge, member by member along its curve
                    final List<com.example.fxsuite.avatar.sim.Vec3> along = skeleton.points(edge, 80);
                    for (int p = 1; p < along.size(); p++) {
                        final com.example.fxsuite.avatar.sim.Vec3 a = along.get(p - 1);
                        final com.example.fxsuite.avatar.sim.Vec3 b = along.get(p);
                        members.add(member(called + " " + k++, new Form.Vertex("a", a.x(), a.y(), a.z()),
                                new Form.Vertex("b", b.x(), b.y(), b.z()), size, role, palette.of(role)));
                    }
                }
                return members;
            };
        }
    }

    /** A square member {@code size} across from {@code a} to {@code b}. */
    static Solid member(final String name, final Form.Vertex a, final Form.Vertex b, final double size,
                        final Role role, final Material material) {
        final Point from = new Point(a.across(), a.along(), a.up());
        final Point run = new Point(b.across() - a.across(), b.along() - a.along(), b.up() - a.up());
        final Point d = run.times(1 / run.length());
        // two directions square to it and to each other
        final Point other = Math.abs(d.up()) < 0.9 ? new Point(0, 0, 1) : new Point(1, 0, 0);
        final Point p = cross(d, other);
        final Point pn = p.times(1 / p.length());
        final Point q = cross(d, pn);
        final List<Point> corners = new ArrayList<>(8);
        for (int k = 0; k < 8; k++) {
            corners.add(from.plus(run.times(k & 1)).plus(pn.times(((k & 2) == 0 ? -0.5 : 0.5) * size))
                    .plus(q.times(((k & 4) == 0 ? -0.5 : 0.5) * size)));
        }
        return new Solid(name, role, material, corners);
    }

    private static Point cross(final Point a, final Point b) {
        return new Point(a.along() * b.up() - a.up() * b.along(), a.up() * b.across() - a.across() * b.up(),
                a.across() * b.along() - a.along() * b.across());
    }

    /**
     * What is added to a surface once it is dressed, whatever its design: corner stones, a cornice.
     * Given what its design made of it ({@code dressed}), so it can keep clear of its openings.
     */
    @FunctionalInterface
    public interface Addition {
        List<Solid> add(Surface surface, Palette palette, List<Solid> dressed);
    }

    /**
     * A way to dress a building: for each kind of surface, its design; a surface whose kind it does
     * not name is left open. And, for each kind, what is added to it once dressed, whatever its
     * design ({@link Addition}). And what it makes some roles of, over the palette (a roof's covering).
     *
     * @param name      what it is called
     * @param designs   by the kind of surface each dresses
     * @param additions by the kind of surface each is added to
     * @param materials what it makes some roles of, whatever the palette says
     */
    public record Option(String name, Map<String, Design> designs, Map<String, List<Addition>> additions,
                         Map<Role, Material> materials) {

        /** One that dresses and adds, choosing no materials. */
        public Option(final String name, final Map<String, Design> designs, final Map<String, List<Addition>> additions) {
            this(name, designs, additions, Map.of());
        }

        /** One that dresses, as {@code designs} say, and chooses what {@code materials} say. */
        public static Option choosing(final String name, final Map<String, Design> designs,
                                      final Map<Role, Material> materials) {
            return new Option(name, designs, Map.of(), materials);
        }

        public Option {
            materials = Map.copyOf(materials);
            designs = Map.copyOf(designs);
            final java.util.HashMap<String, List<Addition>> kept = new java.util.HashMap<>();
            additions.forEach((kind, added) -> kept.put(kind, List.copyOf(added)));
            additions = Map.copyOf(kept);
        }

        /** One that dresses, adding nothing. */
        public Option(final String name, final Map<String, Design> designs) {
            this(name, designs, Map.of());
        }

        /** One that only adds {@code added} to each of {@code kinds}, whatever they are dressed in. */
        public static Option adding(final String name, final Addition added, final String... kinds) {
            final java.util.HashMap<String, List<Addition>> to = new java.util.HashMap<>();
            for (final String kind : kinds) {
                to.put(kind, List.of(added));
            }
            return new Option(name, Map.of(), to);
        }

        /**
         * This, and {@code more} over it: for the kinds of surface both dress, {@code more}'s design;
         * and what both add, all of it.
         */
        public Option and(final Option more) {
            final java.util.HashMap<String, Design> both = new java.util.HashMap<>(designs);
            both.putAll(more.designs());
            final java.util.HashMap<String, List<Addition>> added = new java.util.HashMap<>();
            additions.forEach((kind, list) -> added.put(kind, new ArrayList<>(list)));
            more.additions().forEach((kind, list) -> added.computeIfAbsent(kind, k -> new ArrayList<>()).addAll(list));
            final java.util.HashMap<Role, Material> chosen = new java.util.HashMap<>(materials);
            chosen.putAll(more.materials());
            return new Option(name + "; " + more.name(), both, added, chosen);
        }

        /** The same, but {@code kind} dressed with {@code design}: one surface changed, the rest kept. */
        public Option with(final String called, final String kind, final Design design) {
            final java.util.HashMap<String, Design> changed = new java.util.HashMap<>(designs);
            changed.put(kind, design);
            return new Option(called, changed, additions, materials);
        }
    }

    /** {@code options} put together, each over those before it. */
    public static Option together(final List<Option> options) {
        Option all = new Option("", Map.of());
        for (final Option option : options) {
            all = all.and(option);
        }
        return all;
    }

    /** Dressed as {@code options} say together, each over those before it, built in {@code palette}. */
    public List<Solid> dressed(final List<Option> options, final Palette palette) {
        return dressed(together(options), palette);
    }

    /** Dressed as {@code option} says, built in {@code palette}: its structure, and each surface's solids. */
    public List<Solid> dressed(final Option option, final Palette asked) {
        final Palette palette = asked.with(option.materials());
        final List<Solid> solids = new ArrayList<>();
        for (final Structure part : structure) {
            solids.addAll(part.of(skeleton, palette));
        }
        for (final Surface surface : surfaces) {
            final List<Solid> dressed = new ArrayList<>(
                    option.designs().getOrDefault(surface.kind(), Design.none()).dress(surface, palette));
            for (final Addition added : option.additions().getOrDefault(surface.kind(), List.of())) {
                dressed.addAll(added.add(surface, palette, List.copyOf(dressed)));
            }
            solids.addAll(dressed);
        }
        for (final Plate plate : plates) {
            solids.addAll(option.designs().getOrDefault(plate.kind(), Design.none()).plate(plate, palette));
        }
        for (final Sheet sheet : sheets) {
            solids.addAll(option.designs().getOrDefault(sheet.kind(), Design.none()).sheet(sheet, palette));
        }
        return solids;
    }

    /** What is doubtful about it dressed as {@code options} say, built in {@code palette}: each surface's doubts. */
    public List<String> doubts(final List<Option> options, final Palette palette) {
        final Option all = together(options);
        final List<String> doubts = new ArrayList<>();
        for (final Surface surface : surfaces) {
            doubts.addAll(all.designs().getOrDefault(surface.kind(), Design.none()).doubts(surface, palette));
        }
        return doubts;
    }

    /** The palette called {@code called}, or its own if there is none of that name. */
    public Palette palette(final String called) {
        return palettes.stream().filter(p -> p.name().equalsIgnoreCase(called)).findFirst().orElse(palettes.getFirst());
    }

    /** Of choice {@code choice}, the option called {@code called}; or the first, if there is none of that name. */
    public Option option(final String choice, final String called) {
        final List<Option> options = choices.get(choice);
        return options.stream().filter(o -> o.name().equalsIgnoreCase(called)).findFirst().orElse(options.getFirst());
    }
}
