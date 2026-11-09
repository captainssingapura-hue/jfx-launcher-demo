package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionOrder;
import com.example.fxsuite.deployment.VersionUnit;
import com.example.fxsuite.deployment.VirtualVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Where each of a set of releases goes in the 3D view: a whole estate, or just the part
 * one application reaches. Pure: no JavaFX.
 *
 * <ul>
 *   <li><b>Depth</b>: 0 for a release with no internal dependencies, otherwise one more
 *       than its deepest dependency. A property of the release, so it is the same whichever
 *       set it is shown in.</li>
 *   <li><b>Lane</b>: one horizontal position per unit, the same in every layer. A unit's
 *       home layer is the depth of its shallowest release; the units sharing a home layer
 *       are spread out in catalogue order and centred, so the layers stack front to back.</li>
 *   <li><b>Row</b>: the release's place among its unit's releases in the set, oldest first.</li>
 * </ul>
 */
public final class EstateLayout {

    public enum Kind { BUNDLE, LIBRARY, APPLICATION }

    /** One release and where it goes. */
    public record Placed(Version<?> version, Class<?> unit, Kind kind, int depth, double lane, int row) {
        public boolean isVirtual() {
            return version instanceof VirtualVersion<?, ?>;
        }
    }

    /** A declared dependency between two placed releases. */
    public record Edge(Placed from, Placed to) {
    }

    private final Map<Version<?>, Placed> placed;
    private final List<Edge> edges;
    private final List<Class<?>> columns;
    private final int maxDepth;
    private final int maxRow;

    /**
     * @param releases the releases to show, units in the order their lanes should follow
     *                 (see {@link Releases})
     */
    public EstateLayout(final Collection<? extends Version<?>> releases) {
        final VersionUnit unitOf = new VersionUnit();
        // the releases, by unit in order of first appearance, oldest first
        final Map<Class<?>, List<Version<?>>> byUnit = new LinkedHashMap<>();
        for (final Version<?> release : releases) {
            final Class<?> unit = unitOf.of(release.getClass()).orElseThrow(() -> new IllegalArgumentException(
                    release.getClass().getName() + " does not name the unit it is a version of"));
            byUnit.computeIfAbsent(unit, u -> new ArrayList<>()).add(release);
        }
        byUnit.values().forEach(versions -> versions.sort(this::compare));

        final Map<Version<?>, Integer> depths = new HashMap<>();
        for (final List<Version<?>> versions : byUnit.values()) {
            for (final Version<?> version : versions) {
                depth(version, depths, new LinkedHashSet<>());
            }
        }

        // units by home layer, catalogue order kept within each layer
        final Map<Integer, List<Class<?>>> byHome = new TreeMap<>();
        for (final Class<?> unit : byUnit.keySet()) {
            final int home = byUnit.get(unit).stream().mapToInt(depths::get).min().orElse(0);
            byHome.computeIfAbsent(home, h -> new ArrayList<>()).add(unit);
        }
        final Map<Class<?>, Double> lanes = new HashMap<>();
        final List<Class<?>> order = new ArrayList<>();
        for (final List<Class<?>> group : byHome.values()) {
            for (int i = 0; i < group.size(); i++) {
                lanes.put(group.get(i), i - (group.size() - 1) / 2.0);
                order.add(group.get(i));
            }
        }
        this.columns = List.copyOf(order);

        final Map<Version<?>, Placed> result = new LinkedHashMap<>();
        int deepest = 0;
        int highest = 0;
        for (final Class<?> unit : order) {
            final List<Version<?>> versions = byUnit.get(unit);
            for (int row = 0; row < versions.size(); row++) {
                final Version<?> version = versions.get(row);
                final int depth = depths.get(version);
                result.put(version, new Placed(version, unit, kindOf(unit), depth, lanes.get(unit), row));
                deepest = Math.max(deepest, depth);
                highest = Math.max(highest, row);
            }
        }
        this.placed = result;
        this.maxDepth = deepest;
        this.maxRow = highest;

        final List<Edge> declared = new ArrayList<>();
        for (final Placed from : result.values()) {
            for (final Version<?> dependency : from.version().dependencies().versions()) {
                final Placed to = result.get(dependency);
                if (to != null) {
                    declared.add(new Edge(from, to));
                }
            }
        }
        this.edges = List.copyOf(declared);
    }

    public List<Placed> placed() {
        return List.copyOf(placed.values());
    }

    public Optional<Placed> of(final Version<?> version) {
        return Optional.ofNullable(placed.get(version));
    }

    public List<Edge> edges() {
        return edges;
    }

    public List<Class<?>> columns() {
        return columns;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public int maxRow() {
        return maxRow;
    }

    /**
     * @throws IllegalStateException if the releases depend on each other in a cycle, which
     *                               leaves them no depth
     */
    private int depth(final Version<?> version, final Map<Version<?>, Integer> depths,
                      final Set<Version<?>> visiting) {
        final Integer known = depths.get(version);
        if (known != null) {
            return known;
        }
        if (!visiting.add(version)) {
            throw new IllegalStateException("dependency cycle through " + version.getClass().getName());
        }
        int depth = 0;
        for (final Version<?> dependency : version.dependencies().versions()) {
            depth = Math.max(depth, depth(dependency, depths, visiting) + 1);
        }
        visiting.remove(version);
        depths.put(version, depth);
        return depth;
    }

    private Kind kindOf(final Class<?> unit) {
        if (Application.class.isAssignableFrom(unit)) {
            return Kind.APPLICATION;
        }
        return ExternalBundle.class.isAssignableFrom(unit) ? Kind.BUNDLE : Kind.LIBRARY;
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }
}
