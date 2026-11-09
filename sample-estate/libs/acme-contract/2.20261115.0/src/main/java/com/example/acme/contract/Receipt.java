package com.example.acme.contract;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * The answer to a {@link Transfer}. Added in 1.20261030.0.
 */
public record Receipt(String from, String to, boolean accepted) implements Scalar {
}
