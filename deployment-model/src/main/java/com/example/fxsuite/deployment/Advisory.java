package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A published vulnerability in external code, as an advisory database such as OSV records
 * it: its identifier, a CVE alias if there is one, its severity, and the version ranges it
 * affects. A jar is affected when any range covers it.
 */
public record Advisory(String id, String cve, String severity, String summary, Sequence<AffectedRange> ranges)
        implements Scalar {

    /** The ranges where the jar's group and artifact are affected, at its version. */
    public Optional<AffectedRange> affecting(final ExternalArtifact jar) {
        return ranges.items().stream().filter(range -> range.affects(jar)).findFirst();
    }

    /** {@code GHSA-xxph-c9ww-hj94 (CVE-2026-102554)} */
    public String name() {
        return cve.isEmpty() ? id : id + " (" + cve + ")";
    }
}
