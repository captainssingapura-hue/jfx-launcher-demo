package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.Lineage;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.VersionUnit;

import java.util.Map;

/**
 * Plausible sizes that are the same on every run. Well-known external jars get roughly
 * their real size, varied a little by version; anything else gets a size spread
 * logarithmically over a range for its kind, derived from its name. A virtual release has
 * its base's binary, so its base's size; a bundle is the sum of the jars it brings.
 */
public final class SimulatedSizes implements SizeProvider {

    /** Roughly the real size of well-known jars. */
    private static final Map<String, Long> KNOWN = Map.ofEntries(
            Map.entry("org.openjfx:javafx-graphics", 4_900_000L),
            Map.entry("org.openjfx:javafx-controls", 2_600_000L),
            Map.entry("org.openjfx:javafx-base", 760_000L),
            Map.entry("com.google.guava:guava", 3_100_000L),
            Map.entry("com.google.guava:failureaccess", 4_700L),
            Map.entry("com.google.guava:listenablefuture", 2_200L),
            Map.entry("com.google.errorprone:error_prone_annotations", 19_000L),
            Map.entry("com.google.j2objc:j2objc-annotations", 9_000L),
            Map.entry("org.jspecify:jspecify", 4_000L),
            Map.entry("com.fasterxml.jackson.core:jackson-databind", 1_700_000L),
            Map.entry("com.fasterxml.jackson.core:jackson-core", 600_000L),
            Map.entry("com.fasterxml.jackson.core:jackson-annotations", 80_000L),
            Map.entry("tools.jackson.core:jackson-databind", 1_900_000L),
            Map.entry("tools.jackson.core:jackson-core", 650_000L),
            Map.entry("tools.jackson.datatype:jackson-datatype-guava", 60_000L),
            Map.entry("io.github.captainssingapura-hue.lang.ontology:core", 5_000L));

    @Override
    public long bytes(final Version<?> release) {
        if (release instanceof BundleRelease bundle) {
            return bundle.members().artifacts().stream().mapToLong(this::jar).sum();
        }
        final PhysicalVersion<?> physical = new Lineage().physicalOf(release);
        final Class<?> unit = new VersionUnit().of(physical.getClass()).orElseThrow();
        final String key = unit.getName() + " " + new VersionText().number(physical);
        return Application.class.isAssignableFrom(unit)
                ? spread(key, 60_000, 900_000)
                : spread(key, 8_000, 250_000);
    }

    @Override
    public String source() {
        return "simulated";
    }

    long jar(final ExternalArtifact jar) {
        final Long known = KNOWN.get(jar.group() + ":" + jar.artifact());
        if (known == null) {
            return spread(jar.coordinates(), 20_000, 1_000_000);
        }
        // a little drift between versions of the same jar
        return Math.round(known * (0.97 + 0.06 * unit(jar.version())));
    }

    /** A size between {@code low} and {@code high}, spread evenly on a log scale by the key. */
    private long spread(final String key, final long low, final long high) {
        return Math.round(low * Math.pow((double) high / low, unit(key)));
    }

    /** A number in [0, 1) fixed by the key. */
    private double unit(final String key) {
        return (key.hashCode() & 0x7fffffff) / (double) Integer.MAX_VALUE;
    }
}
