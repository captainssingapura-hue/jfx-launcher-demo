package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * One known vulnerability a release carries: a jar a bundle brings, an advisory affecting
 * it, and the version that fixes it on the jar's branch.
 *
 * @param bundle the bundle release that brings the jar
 */
public record Exposure(Version<?> bundle, ExternalArtifact jar, Advisory advisory, String fixedIn) implements Scalar {
}
