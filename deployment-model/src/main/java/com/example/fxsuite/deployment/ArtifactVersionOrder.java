package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Orders external version strings the way Maven does for the versions the estate meets:
 * dot- or hyphen-separated parts, numbers compared as numbers, and a release before any
 * qualified build of the same numbers ({@code 33.7.2} before {@code 33.7.2-jre}). Missing
 * numeric parts count as zero, so {@code 2.22} equals {@code 2.22.0}.
 */
public final class ArtifactVersionOrder implements StatelessFunctionalObject {

    public int compare(final String a, final String b) {
        final String[] left = a.split("[.-]");
        final String[] right = b.split("[.-]");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            final String x = i < left.length ? left[i] : null;
            final String y = i < right.length ? right[i] : null;
            final int byPart = comparePart(x, y);
            if (byPart != 0) {
                return byPart;
            }
        }
        return 0;
    }

    /** A missing part is zero against a number, and comes first against a qualifier. */
    private int comparePart(final String x, final String y) {
        final boolean xNumber = x == null || x.chars().allMatch(Character::isDigit);
        final boolean yNumber = y == null || y.chars().allMatch(Character::isDigit);
        if (xNumber && yNumber) {
            return Long.compare(x == null ? 0 : Long.parseLong(x), y == null ? 0 : Long.parseLong(y));
        }
        if (x == null) {
            return -1;
        }
        if (y == null) {
            return 1;
        }
        if (xNumber != yNumber) {
            return xNumber ? 1 : -1;
        }
        return x.compareToIgnoreCase(y);
    }
}
