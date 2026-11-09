package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * Everything a release runs with, worked out from its dependencies and theirs.
 *
 * @param resolved  one version per unit in the release's whole dependency graph
 * @param warnings  each unit requested at more than one version, where the higher was used
 * @param conflicts requests that cannot be reconciled, such as two lines of one unit; a
 *                  release with conflicts has no valid resolution
 */
public record Resolution(Dependencies resolved, Sequence<Warning> warnings, Sequence<Violation> conflicts)
        implements Scalar {

    public boolean isValid() {
        return conflicts.isEmpty();
    }
}
