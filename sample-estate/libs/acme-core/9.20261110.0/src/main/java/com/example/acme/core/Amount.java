package com.example.acme.core;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An amount of money, in cents.
 */
public record Amount(long cents) implements Scalar {

    // Accepts a negative amount: the flaw fixed in 8.20261106.1 and 9.20261120.0.
}
