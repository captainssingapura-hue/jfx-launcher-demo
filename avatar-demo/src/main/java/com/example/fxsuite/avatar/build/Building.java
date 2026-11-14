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
 * many ways as its choices' options can be put together.
 *
 * @param name      what it is
 * @param skeleton  its lines
 * @param surfaces  its surfaces
 * @param structure what of its structure shows, from its skeleton
 * @param choices   by what each is about, the options to choose from, the first of each its own
 */
public record Building(String name, Form skeleton, List<Surface> surfaces, List<Structure> structure,
                       Map<String, List<Option>> choices) {

    public Building {
        surfaces = List.copyOf(surfaces);
        structure = List.copyOf(structure);
        // in the order given, each choice's options in theirs
        final java.util.LinkedHashMap<String, List<Option>> kept = new java.util.LinkedHashMap<>();
        choices.forEach((choice, options) -> kept.put(choice, List.copyOf(options)));
        choices = java.util.Collections.unmodifiableMap(kept);
    }

    /** Its own way of being dressed: the first option of each choice. */
    public List<Option> own() {
        return choices.values().stream().map(List::getFirst).toList();
    }

    /** Structure worked out from a skeleton: its edges given bodies, those it picks. */
    @FunctionalInterface
    public interface Structure {
        List<Solid> of(Form skeleton);

        /**
         * The edges {@code which} picks, each a square member {@code size} across of {@code material},
         * centred on its line: columns, beams.
         */
        static Structure members(final String called, final Predicate<Form.Edge> which, final double size,
                                 final Material material) {
            return skeleton -> {
                final List<Solid> members = new ArrayList<>();
                int k = 0;
                for (final Form.Edge edge : skeleton.edges()) {
                    if (which.test(edge)) {
                        members.add(member(called + " " + k++, skeleton.vertices().get(edge.from()),
                                skeleton.vertices().get(edge.to()), size, material));
                    }
                }
                return members;
            };
        }
    }

    /** A square member {@code size} across from {@code a} to {@code b}. */
    static Solid member(final String name, final Form.Vertex a, final Form.Vertex b, final double size,
                        final Material material) {
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
        return new Solid(name, material, corners);
    }

    private static Point cross(final Point a, final Point b) {
        return new Point(a.along() * b.up() - a.up() * b.along(), a.up() * b.across() - a.across() * b.up(),
                a.across() * b.along() - a.along() * b.across());
    }

    /**
     * A way to dress a building: for each kind of surface, its design; a surface whose kind it does
     * not name is left open.
     *
     * @param name    what it is called
     * @param designs by the kind of surface each dresses
     */
    public record Option(String name, Map<String, Design> designs) {

        public Option {
            designs = Map.copyOf(designs);
        }

        /** This, and {@code more}'s designs over it: for the kinds of surface both name, {@code more}'s. */
        public Option and(final Option more) {
            final java.util.HashMap<String, Design> both = new java.util.HashMap<>(designs);
            both.putAll(more.designs());
            return new Option(name + "; " + more.name(), both);
        }

        /** The same, but {@code kind} dressed with {@code design}: one surface changed, the rest kept. */
        public Option with(final String called, final String kind, final Design design) {
            final java.util.HashMap<String, Design> changed = new java.util.HashMap<>(designs);
            changed.put(kind, design);
            return new Option(called, changed);
        }
    }

    /** Dressed as {@code options} say together, each over those before it. */
    public List<Solid> dressed(final List<Option> options) {
        Option all = new Option("", Map.of());
        for (final Option option : options) {
            all = all.and(option);
        }
        return dressed(all);
    }

    /** Dressed as {@code option} says: its structure, and each surface's solids. */
    public List<Solid> dressed(final Option option) {
        final List<Solid> solids = new ArrayList<>();
        for (final Structure part : structure) {
            solids.addAll(part.of(skeleton));
        }
        for (final Surface surface : surfaces) {
            solids.addAll(option.designs().getOrDefault(surface.kind(), Design.none()).dress(surface));
        }
        return solids;
    }

    /** Of choice {@code choice}, the option called {@code called}; or the first, if there is none of that name. */
    public Option option(final String choice, final String called) {
        final List<Option> options = choices.get(choice);
        return options.stream().filter(o -> o.name().equalsIgnoreCase(called)).findFirst().orElse(options.getFirst());
    }
}
