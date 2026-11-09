package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionOrder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The releases of an estate, in catalogue order and oldest first within each unit, and
 * the parts of it a view may want to show. Pure: no JavaFX.
 */
public final class Releases {

    private final Map<Class<? extends DeploymentUnit>, List<Version<?>>> byUnit = new LinkedHashMap<>();

    public Releases(final List<Class<? extends DeploymentUnit>> units) {
        for (final Class<? extends DeploymentUnit> unit : units) {
            final List<Version<?>> versions = new ArrayList<>();
            for (final Class<?> type : unit.getDeclaredClasses()) {
                if (Version.class.isAssignableFrom(type)) {
                    versions.add(instantiate(type));
                }
            }
            versions.sort(this::compare);
            byUnit.put(unit, List.copyOf(versions));
        }
    }

    /** Every release: units in catalogue order, each unit's releases oldest first. */
    public List<Version<?>> all() {
        return byUnit.values().stream().flatMap(List::stream).toList();
    }

    /** The units that are applications, in catalogue order. */
    public List<Class<? extends DeploymentUnit>> applications() {
        return byUnit.keySet().stream().filter(Application.class::isAssignableFrom).toList();
    }

    /** A unit's releases, oldest first. */
    public List<Version<?>> of(final Class<?> unit) {
        return byUnit.getOrDefault(unit, List.of());
    }

    /**
     * The release and everything it reaches through declared dependencies, including
     * releases that resolution would raise past. In catalogue order.
     */
    public List<Version<?>> closure(final Version<?> root) {
        final Set<Version<?>> reached = new LinkedHashSet<>();
        final Deque<Version<?>> todo = new ArrayDeque<>(List.of(root));
        while (!todo.isEmpty()) {
            final Version<?> next = todo.poll();
            if (reached.add(next)) {
                todo.addAll(next.dependencies().versions());
            }
        }
        final List<Version<?>> ordered = new ArrayList<>(all().stream().filter(reached::contains).toList());
        reached.stream().filter(v -> !ordered.contains(v)).forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private Version<?> instantiate(final Class<?> type) {
        try {
            final var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Version<?>) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot create " + type.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }
}
