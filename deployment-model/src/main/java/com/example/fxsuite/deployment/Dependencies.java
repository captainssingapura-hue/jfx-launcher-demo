package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The exact versions a release depends on: a set with at most one version per unit. Only
 * libraries and bundles can appear, since nothing may depend on an application.
 *
 * <p>Equal when the versions are equal; order is not part of the value.
 */
public final class Dependencies implements Vector<Version<? extends Dependable>> {

    private final Map<Class<?>, Version<? extends Dependable>> byUnit;

    @SafeVarargs
    public Dependencies(final Version<? extends Dependable>... versions) {
        this(List.of(versions));
    }

    public Dependencies(final Collection<? extends Version<? extends Dependable>> versions) {
        final VersionUnit unitOf = new VersionUnit();
        final Map<Class<?>, Version<? extends Dependable>> map = new HashMap<>();
        for (final Version<? extends Dependable> version : versions) {
            Objects.requireNonNull(version, "dependency");
            final Class<?> unit = unitOf.of(version.getClass()).orElseThrow(() -> new IllegalArgumentException(
                    version.getClass().getName() + " does not name the unit it is a version of"));
            final Version<? extends Dependable> earlier = map.putIfAbsent(unit, version);
            if (earlier != null && !earlier.equals(version)) {
                throw new IllegalArgumentException("two versions of " + unit.getName() + ": "
                        + earlier.getClass().getSimpleName() + " and " + version.getClass().getSimpleName());
            }
        }
        this.byUnit = Map.copyOf(map);
    }

    public Set<Version<? extends Dependable>> versions() {
        return Set.copyOf(byUnit.values());
    }

    public Set<Class<?>> units() {
        return byUnit.keySet();
    }

    /** The version this set holds for the unit, if it depends on it. */
    public Optional<Version<? extends Dependable>> on(final Class<?> unit) {
        return Optional.ofNullable(byUnit.get(unit));
    }

    public boolean isEmpty() {
        return byUnit.isEmpty();
    }

    /**
     * These dependencies with some of their versions replaced. A change can only replace a
     * unit already depended on, so nothing is added; anything not changed is kept, so
     * nothing is removed.
     *
     * @throws IllegalArgumentException if a change names a unit not depended on here
     */
    public Dependencies changedBy(final Dependencies changes) {
        final Map<Class<?>, Version<? extends Dependable>> result = new HashMap<>(byUnit);
        for (final Map.Entry<Class<?>, Version<? extends Dependable>> change : changes.byUnit.entrySet()) {
            if (!byUnit.containsKey(change.getKey())) {
                throw new IllegalArgumentException("a change can only replace an existing dependency, not add "
                        + change.getKey().getName());
            }
            result.put(change.getKey(), change.getValue());
        }
        return new Dependencies(result.values());
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Dependencies d && byUnit.equals(d.byUnit);
    }

    @Override
    public int hashCode() {
        return byUnit.hashCode();
    }

    @Override
    public String toString() {
        return byUnit.values().stream()
                .map(v -> v.getClass().getSimpleName())
                .sorted()
                .collect(Collectors.joining(", ", "Dependencies[", "]"));
    }
}
