package com.example.acme.core;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * AcmeCore 8.20261015.0: amounts of money and how they are written.
 */
public final class Core implements StatelessFunctionalObject {

    /** The release whose code is running. A virtual release runs, and reports, its physical base. */
    public String release() {
        return "AcmeCore 8.20261015.0";
    }

    public Amount amount(final long cents) {
        return new Amount(cents);
    }

    /** {@code 12.50}. Added in 8.20261015.0. */
    public String format(final Amount amount) {
        return amount.cents() / 100 + "." + String.format("%02d", Math.abs(amount.cents() % 100));
    }

    /** Kept on line 8 for older callers; line 9 removes it. */
    @Deprecated
    public String legacyName() {
        return "acme-core";
    }
}
