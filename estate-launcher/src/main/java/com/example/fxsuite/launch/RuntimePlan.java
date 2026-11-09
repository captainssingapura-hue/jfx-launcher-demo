package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.Lineage;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionOrder;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.VersionUnit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The jars a set of releases runs with: the application's own jar first, then each
 * internal library's, then every bundle's external jars.
 *
 * <ul>
 *   <li>An internal library or application maps to the jar of the physical release it
 *       runs: a virtual release has no jar of its own and uses its base's.</li>
 *   <li>Internal jars live at {@code com.example.acme:<unit>:<version>}, where {@code <unit>}
 *       is the unit's name in lower case with hyphens: {@code AcmeCore} is {@code acme-core}.</li>
 *   <li>A bundle maps to its members.</li>
 * </ul>
 */
public final class RuntimePlan {

    public static final String GROUP = "com.example.acme";

    /**
     * @param app      the release being launched
     * @param releases everything it runs with, normally its resolution
     */
    public List<Coordinates> jars(final Version<?> app, final Collection<? extends Version<?>> releases) {
        final List<Coordinates> internal = new ArrayList<>();
        final List<Coordinates> external = new ArrayList<>();
        for (final Version<?> release : releases) {
            if (release.equals(app)) {
                continue;
            }
            if (release instanceof BundleRelease bundle) {
                for (final ExternalArtifact jar : bundle.members().artifacts()) {
                    external.add(new Coordinates(jar.group(), jar.artifact(), jar.version(), jar.classifier()));
                }
            } else {
                internal.add(internalJar(release));
            }
        }
        internal.sort(Comparator.comparing(Coordinates::toString));
        external.sort(Comparator.comparing(Coordinates::toString));

        final List<Coordinates> all = new ArrayList<>();
        all.add(internalJar(app));
        all.addAll(internal);
        all.addAll(external);
        return List.copyOf(all);
    }

    /** The jar a library or application release runs: its own, or its physical base's. */
    public Coordinates internalJar(final Version<?> release) {
        final PhysicalVersion<?> physical = new Lineage().physicalOf(release);
        final Class<?> unit = new VersionUnit().of(physical.getClass()).orElseThrow();
        return new Coordinates(GROUP, artifactOf(unit), new VersionText().number(physical), "");
    }

    /** {@code AcmeCore} to {@code acme-core}. */
    public String artifactOf(final Class<?> unit) {
        return unit.getSimpleName().replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }

    /**
     * What a build tool's "nearest declaration wins" rule would put on the classpath: the
     * version of each unit closest to the application wins, whatever version anything deeper
     * asks for, and only winners bring their own dependencies. At equal depth a build tool
     * goes by declaration order, which the model does not record; this takes the lowest
     * version, which is what Maven picks for Ledger, whose pom declares acme-text first.
     *
     * <p>Not what the estate runs; it is here to show what goes wrong without the model's
     * resolution.
     */
    public List<Version<?>> nearestWins(final Version<?> app) {
        final VersionUnit unitOf = new VersionUnit();
        final Map<Class<?>, Version<?>> picks = new LinkedHashMap<>();
        List<Version<?>> level = List.of(app);
        while (!level.isEmpty()) {
            final Map<Class<?>, Version<?>> lowest = new LinkedHashMap<>();
            for (final Version<?> from : level) {
                for (final Version<?> dependency : from.dependencies().versions()) {
                    final Class<?> unit = unitOf.of(dependency.getClass()).orElseThrow();
                    if (!picks.containsKey(unit)) {
                        lowest.merge(unit, dependency, (x, y) -> compare(x, y) <= 0 ? x : y);
                    }
                }
            }
            picks.putAll(lowest);
            level = List.copyOf(lowest.values());
        }
        return List.copyOf(picks.values());
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }
}
