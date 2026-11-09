package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Resolution;
import com.example.fxsuite.deployment.Resolver;
import com.example.fxsuite.deployment.Version;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What to emphasise when a release is selected: the releases its resolution actually
 * uses, and the declared dependencies that resolution had to raise past. Pure: no JavaFX.
 */
public final class Highlighter {

    /**
     * @param members    the selected release and every release its graph resolves to
     * @param passedOver releases some member asks for but which resolution raised past
     * @param lit        declared edges between two members
     * @param raised     declared edges from a member to a release that was raised past
     */
    public record Highlight(Version<?> root, Resolution resolution, Set<Version<?>> members,
                            Set<Version<?>> passedOver, Set<EstateLayout.Edge> lit,
                            Set<EstateLayout.Edge> raised) {
    }

    public Highlight of(final EstateLayout layout, final Version<?> root) {
        final Resolution resolution = new Resolver().resolve(root);
        final Set<Version<?>> members = new LinkedHashSet<>();
        members.add(root);
        members.addAll(resolution.resolved().versions());

        final Set<Version<?>> passedOver = new HashSet<>();
        final Set<EstateLayout.Edge> lit = new HashSet<>();
        final Set<EstateLayout.Edge> raised = new HashSet<>();
        for (final EstateLayout.Edge edge : layout.edges()) {
            if (!members.contains(edge.from().version())) {
                continue;
            }
            if (members.contains(edge.to().version())) {
                lit.add(edge);
            } else {
                raised.add(edge);
                passedOver.add(edge.to().version());
            }
        }
        return new Highlight(root, resolution, Set.copyOf(members), Set.copyOf(passedOver),
                Set.copyOf(lit), Set.copyOf(raised));
    }
}
