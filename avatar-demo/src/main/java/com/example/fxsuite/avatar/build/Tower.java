package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tower as a stack of sections, bottom to top: each a homogeneous stack of floors, one plan
 * ({@link Plan}) over and over, so many floors so high; a podium under a shaft, a shaft under a
 * set-back top. From it, its skeleton ({@link Form}): at every floor level, its plan's graph (its
 * columns, its beams, its slab's edge); its columns up from each level to the next; where one section
 * stands on another, the level between them shared, a column of the one above standing on a column of
 * the one below where there is one there. And what is wrong with it: a plan's own faults, a section
 * whose columns stand on no column below (they need a transfer structure), one overhanging the one
 * under it.
 *
 * <p>In centimetres: across x, along y, up from the ground.
 *
 * @param name     what it is
 * @param sections its sections, bottom to top
 */
public record Tower(String name, List<Section> sections) {

    /** One plan, stacked {@code floors} high, each floor {@code height} high, floor to floor. */
    public record Section(String name, Plan plan, int floors, double height) {

        public Section {
            if (floors < 1 || height <= 0) {
                throw new IllegalArgumentException(name + ": " + floors + " floors " + height + " high");
            }
        }

        public double tall() {
            return floors * height;
        }
    }

    public Tower {
        sections = List.copyOf(sections);
        if (sections.isEmpty()) {
            throw new IllegalArgumentException(name + " has no sections");
        }
    }

    /** One plan, {@code floors} high. */
    public static Tower of(final String name, final Plan plan, final int floors, final double height) {
        return new Tower(name, List.of(new Section(name, plan, floors, height)));
    }

    /** How high it stands, to its roof. */
    public double height() {
        return sections.stream().mapToDouble(Section::tall).sum();
    }

    /** How many floors it has, all told. */
    public int floors() {
        return sections.stream().mapToInt(Section::floors).sum();
    }

    /** Where section {@code index} starts, up from the ground. */
    public double base(final int index) {
        double base = 0;
        for (int s = 0; s < index; s++) {
            base += sections.get(s).tall();
        }
        return base;
    }

    /**
     * Its skeleton: each section's plan's graph at each of its levels, its first level shared with the
     * last of the section below; a column up from each of its plan's columns, level to level. Each
     * vertex named for its level, its section and its place in its plan.
     */
    public Form skeleton() {
        return skeleton(false);
    }

    /** As {@link #skeleton()}; its plans' curves as curved edges, if {@code curved}, not in straight pieces. */
    public Form skeleton(final boolean curved) {
        final Form.Builder form = new Form.Builder(name);
        final Map<String, Integer> at = new HashMap<>();
        final Set<Long> joined = new HashSet<>();
        int level = 0;
        double base = 0;
        for (final Section section : sections) {
            final Plan.Layout layout = section.plan().layout();
            final Form plan = layout.graph(curved);
            final Set<String> columns = layout.allColumns().keySet();
            int[] below = null;
            for (int k = 0; k <= section.floors(); k++) {
                final double up = base + k * section.height();
                final int[] here = new int[plan.vertices().size()];
                for (int v = 0; v < here.length; v++) {
                    final Form.Vertex p = plan.vertices().get(v);
                    final String key = Math.round(p.across() * 10) + "," + Math.round(p.along() * 10) + ","
                            + Math.round(up * 10);
                    final int lv = level + k;
                    here[v] = at.computeIfAbsent(key, x -> form.vertex("level " + lv + ", " + section.name() + ", "
                            + p.name(), p.across(), p.along(), up));
                }
                for (final Form.Edge edge : plan.edges()) {
                    join(form, joined, here[edge.from()], here[edge.to()],
                            edge.curved() ? edge.curve().moved(new com.example.fxsuite.avatar.sim.Vec3(0, 0, up)) : null);
                }
                if (below != null) {
                    for (int v = 0; v < here.length; v++) {
                        if (columns.contains(plan.vertices().get(v).name())) {
                            join(form, joined, below[v], here[v], null);
                        }
                    }
                }
                below = here;
            }
            level += section.floors();
            base += section.tall();
        }
        return form.build();
    }

    private static void join(final Form.Builder form, final Set<Long> joined, final int a, final int b,
                             final com.example.fxsuite.avatar.sim.Curve curve) {
        if (a != b && joined.add(((long) Math.min(a, b) << 32) | Math.max(a, b))) {
            form.edge(a, b, curve);
        }
    }

    /**
     * What is wrong with it: each section's plan's faults; a section whose columns stand on no column
     * of the one below, so many of them; a section reaching beyond the one below.
     */
    public List<String> problems() {
        final List<String> problems = new ArrayList<>();
        Plan.Layout below = null;
        for (final Section section : sections) {
            final Plan.Layout layout = section.plan().layout();
            layout.problems().forEach(p -> problems.add(section.name() + ": " + p));
            if (below != null) {
                final Plan.Layout under = below;
                final long unsupported = layout.allColumns().values().stream()
                        .filter(c -> under.allColumns().values().stream().noneMatch(d -> d.distance(c) < Plan.TOLERANCE))
                        .count();
                if (unsupported > 0) {
                    problems.add(section.name() + ": " + unsupported + " of its " + layout.allColumns().size()
                            + " columns stand on no column below: they need a transfer structure");
                }
                if (layout.shaped().stream().flatMap(List::stream).anyMatch(p -> !under.inside(p))) {
                    problems.add(section.name() + ": it reaches beyond the section under it");
                }
            }
            below = layout;
        }
        return problems;
    }
}
