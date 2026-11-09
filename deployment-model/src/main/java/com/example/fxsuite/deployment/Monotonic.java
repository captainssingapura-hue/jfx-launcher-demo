package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * The first part of a version: it only ever rises, and a rise starts a new line.
 */
public record Monotonic(int value) implements Scalar, Comparable<Monotonic> {

    public Monotonic {
        if (value < 0) {
            throw new IllegalArgumentException("monotonic part must not be negative: " + value);
        }
    }

    @Override
    public int compareTo(final Monotonic other) {
        return Integer.compare(value, other.value);
    }
}
