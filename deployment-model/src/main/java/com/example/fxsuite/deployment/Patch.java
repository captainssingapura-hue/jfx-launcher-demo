package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * The last part of a version: a later revision of the same dated release.
 */
public record Patch(int value) implements Scalar, Comparable<Patch> {

    public Patch {
        if (value < 0) {
            throw new IllegalArgumentException("patch must not be negative: " + value);
        }
    }

    @Override
    public int compareTo(final Patch other) {
        return Integer.compare(value, other.value);
    }
}
