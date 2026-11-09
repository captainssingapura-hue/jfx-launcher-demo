package com.example.acme.core;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An amount of money, in cents.
 */
public record Amount(long cents) implements Scalar {

    public Amount {
        if (cents < 0) {
            throw new IllegalArgumentException("an amount cannot be negative: " + cents);
        }
    }
}
