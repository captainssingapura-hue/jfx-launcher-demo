package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * One rule a unit's versions break, found by {@link VersionChecker}.
 */
public record Violation(String message) implements Scalar {
}
