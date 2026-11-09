package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The external jars a bundle release brings, at most one version of each. Equal when the
 * jars are equal; order is not part of the value.
 */
public final class Members implements Vector<ExternalArtifact> {

    private final Map<String, ExternalArtifact> byKey;

    public Members(final ExternalArtifact... artifacts) {
        this(List.of(artifacts));
    }

    public Members(final Collection<ExternalArtifact> artifacts) {
        final Map<String, ExternalArtifact> map = new LinkedHashMap<>();
        for (final ExternalArtifact artifact : artifacts) {
            final ExternalArtifact earlier = map.putIfAbsent(artifact.key(), artifact);
            if (earlier != null && !earlier.equals(artifact)) {
                throw new IllegalArgumentException("two versions of " + artifact.key() + ": "
                        + earlier.version() + " and " + artifact.version());
            }
        }
        this.byKey = Map.copyOf(map);
    }

    public Set<ExternalArtifact> artifacts() {
        return Set.copyOf(byKey.values());
    }

    public boolean isEmpty() {
        return byKey.isEmpty();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Members m && byKey.equals(m.byKey);
    }

    @Override
    public int hashCode() {
        return byKey.hashCode();
    }

    @Override
    public String toString() {
        return byKey.values().stream().map(ExternalArtifact::coordinates).sorted()
                .collect(Collectors.joining(", ", "Members[", "]"));
    }
}
