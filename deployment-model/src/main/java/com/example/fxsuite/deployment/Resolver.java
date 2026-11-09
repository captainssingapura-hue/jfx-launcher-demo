package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Works out the whole dependency graph of a release: one version per unit.
 *
 * <p>Every version in the graph requests exact versions of its dependencies. When a unit
 * is requested at more than one version on the same line, the higher one is used, since
 * every later release in a line works for anything that worked with an earlier one. A
 * raised version brings its own requests, so this repeats until nothing changes.
 *
 * <p>Using the higher version is safe but means some release in the graph runs with a
 * version it did not declare, so each such choice is reported as a {@link Warning}. A unit
 * requested on two lines has no version that satisfies both, which is a conflict.
 */
public final class Resolver implements StatelessFunctionalObject {

    public Resolution resolve(final Version<?> root) {
        final VersionUnit unitOf = new VersionUnit();
        final VersionText text = new VersionText();
        final Class<?> rootUnit = unitOf.of(root.getClass()).orElseThrow(() -> new IllegalArgumentException(
                root.getClass().getName() + " does not name the unit it is a version of"));

        final Map<Class<?>, Version<? extends Dependable>> picks = new LinkedHashMap<>();
        // unit -> requested version -> the versions that requested it
        final Map<Class<?>, Map<Version<?>, Set<Version<?>>>> requests = new LinkedHashMap<>();
        final List<Violation> conflicts = new ArrayList<>();
        final Set<Class<?>> conflicted = new LinkedHashSet<>();

        final Deque<Version<?>> todo = new ArrayDeque<>(List.of(root));
        final Set<Version<?>> expanded = new HashSet<>();
        while (!todo.isEmpty()) {
            final Version<?> from = todo.poll();
            if (!expanded.add(from)) {
                continue;
            }
            for (final Version<? extends Dependable> wanted : from.dependencies().versions()) {
                final Class<?> unit = unitOf.of(wanted.getClass()).orElseThrow();
                requests.computeIfAbsent(unit, u -> new LinkedHashMap<>())
                        .computeIfAbsent(wanted, w -> new LinkedHashSet<>()).add(from);
                if (unit == rootUnit) {
                    conflicts.add(new Violation(text.named(root) + ": depends on its own unit through "
                            + text.named(from)));
                    continue;
                }
                final Version<? extends Dependable> current = picks.get(unit);
                if (current == null) {
                    picks.put(unit, wanted);
                    todo.add(wanted);
                } else if (!current.monotonic().equals(wanted.monotonic())) {
                    conflicted.add(unit);
                } else if (compare(wanted, current) > 0) {
                    picks.put(unit, wanted);
                    todo.add(wanted);
                }
            }
        }

        for (final Class<?> unit : conflicted) {
            conflicts.add(new Violation(text.named(root) + ": needs " + unit.getSimpleName()
                    + " on more than one line: " + describe(requests.get(unit), Set.copyOf(expanded), text)
                    + "; no version satisfies them all"));
        }

        // Only requests from versions actually in the graph count; a version that was raised
        // past is no longer part of it.
        final Set<Version<?>> graph = new HashSet<>(picks.values());
        graph.add(root);
        final List<Warning> warnings = new ArrayList<>();
        for (final Map.Entry<Class<?>, Version<? extends Dependable>> pick : picks.entrySet()) {
            final Class<?> unit = pick.getKey();
            if (conflicted.contains(unit)) {
                continue;
            }
            final Map<Version<?>, Set<Version<?>>> live = requests.get(unit);
            final long distinct = live.entrySet().stream()
                    .filter(e -> e.getValue().stream().anyMatch(graph::contains))
                    .count();
            final boolean pickedIsRequested = live.getOrDefault(pick.getValue(), Set.of()).stream()
                    .anyMatch(graph::contains);
            if (distinct > 1 || !pickedIsRequested) {
                warnings.add(new Warning(text.named(root) + ": " + unit.getSimpleName() + " is requested at "
                        + describe(live, graph, text) + "; using " + text.number(pick.getValue())
                        + ". Move the others to " + text.number(pick.getValue())
                        + " so every release runs with the version it declares"));
            }
        }

        return new Resolution(new Dependencies(picks.values()), new Sequence<>(warnings), new Sequence<>(conflicts));
    }

    /** {@code 2.20260920.0 (by Web 1.20260925.0), 2.20261105.0 (by Core 8.20261001.1)} */
    private String describe(final Map<Version<?>, Set<Version<?>>> byVersion, final Set<Version<?>> graph,
                            final VersionText text) {
        final Map<Version<?>, List<String>> live = new HashMap<>();
        byVersion.forEach((version, requesters) -> {
            final List<String> names = requesters.stream().filter(graph::contains).map(text::named).sorted().toList();
            if (!names.isEmpty()) {
                live.put(version, names);
            }
        });
        return live.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((Comparator<Version<?>>) this::compare))
                .map(e -> text.number(e.getKey()) + " (by " + String.join(", ", e.getValue()) + ")")
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }
}
