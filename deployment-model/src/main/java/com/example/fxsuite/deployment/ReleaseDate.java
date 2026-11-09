package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The date the release's code was cut. Written {@code YYYYMMDD} in a version string.
 */
public record ReleaseDate(LocalDate value) implements Scalar, Comparable<ReleaseDate> {

    public ReleaseDate {
        Objects.requireNonNull(value, "release date");
    }

    @Override
    public int compareTo(final ReleaseDate other) {
        return value.compareTo(other.value);
    }
}
