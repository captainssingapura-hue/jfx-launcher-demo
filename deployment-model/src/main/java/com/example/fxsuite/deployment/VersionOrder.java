package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Orders two versions of the same unit: by monotonic part, then date, then patch.
 */
public final class VersionOrder implements StatelessFunctionalObject {

    public <U extends DeploymentUnit> int compare(final Version<U> a, final Version<U> b) {
        int byLine = a.monotonic().compareTo(b.monotonic());
        if (byLine != 0) {
            return byLine;
        }
        int byDate = a.date().compareTo(b.date());
        return byDate != 0 ? byDate : a.patch().compareTo(b.patch());
    }

    public <U extends DeploymentUnit> boolean sameLine(final Version<U> a, final Version<U> b) {
        return a.monotonic().equals(b.monotonic());
    }
}
