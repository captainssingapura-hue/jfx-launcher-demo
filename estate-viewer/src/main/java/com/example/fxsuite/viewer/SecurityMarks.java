package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Advisory;
import com.example.fxsuite.deployment.Exposure;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VulnerabilityScanner;
import com.example.fxsuite.viewer.EstateLayout.Placed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which releases carry known vulnerabilities. A bundle release is flagged when a jar it
 * brings is affected; an application release when anything it resolves to is. Libraries are
 * not flagged themselves, though their details list what they would carry.
 */
final class SecurityMarks {

    private final List<Advisory> advisories;
    private final String asOf;
    private final Map<Version<?>, List<Exposure>> found = new HashMap<>();

    /** @param asOf when the advisories were taken from their source */
    SecurityMarks(final List<Advisory> advisories, final String asOf) {
        this.advisories = List.copyOf(advisories);
        this.asOf = asOf;
    }

    /** For a bundle release, its own jars; otherwise everything it resolves to. */
    List<Exposure> exposures(final Version<?> release) {
        return found.computeIfAbsent(release, r -> new VulnerabilityScanner().scan(r, advisories));
    }

    boolean flagged(final Placed placed) {
        return placed.kind() != EstateLayout.Kind.LIBRARY && !exposures(placed.version()).isEmpty();
    }

    String asOf() {
        return asOf;
    }
}
