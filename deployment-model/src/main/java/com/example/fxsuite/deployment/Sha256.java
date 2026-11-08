package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A SHA-256 digest, as 64 lower-case hex digits.
 */
public record Sha256(String hex) implements Scalar {

    public Sha256 {
        if (hex == null || !hex.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("not a lower-case SHA-256 hex digest: " + hex);
        }
    }
}
