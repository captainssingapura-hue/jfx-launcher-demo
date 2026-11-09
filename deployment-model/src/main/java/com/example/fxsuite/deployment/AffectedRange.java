package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * The versions of one external jar an advisory affects: from {@code introduced}, up to but
 * not including {@code fixed}, in Maven's version order. Group and artifact only, so the
 * range covers every classifier of the jar.
 */
public record AffectedRange(String group, String artifact, String introduced, String fixed) implements Scalar {

    public AffectedRange {
        if (group == null || artifact == null || introduced == null || fixed == null) {
            throw new IllegalArgumentException("an affected range needs a group, artifact, introduced and fixed");
        }
    }

    public boolean affects(final ExternalArtifact jar) {
        final ArtifactVersionOrder order = new ArtifactVersionOrder();
        return jar.group().equals(group) && jar.artifact().equals(artifact)
                && order.compare(introduced, jar.version()) <= 0 && order.compare(jar.version(), fixed) < 0;
    }
}
