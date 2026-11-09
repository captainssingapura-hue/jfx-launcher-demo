package com.example.acme.net;

import com.example.acme.core.Amount;
import com.example.acme.core.Core;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * AcmeNet 6.20261112.0: sends amounts. Exposes AcmeCore's {@link Amount} to its callers.
 */
public final class Net implements StatelessFunctionalObject {

    public String release() {
        return "AcmeNet 6.20261112.0";
    }

    public String coreRelease() {
        return new Core().release();
    }

    /** Uses {@code Core.format}, which AcmeCore added in 8.20261015.0. */
    public String send(final Amount amount) {
        return "sent " + new Core().format(amount);
    }

    /** Whether the AcmeCore that is running still accepts a negative amount: the flaw its fix closes. */
    public boolean acceptsNegativeAmounts() {
        try {
            new Amount(-1);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
