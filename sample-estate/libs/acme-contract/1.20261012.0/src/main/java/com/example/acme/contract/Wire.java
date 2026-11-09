package com.example.acme.contract;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * AcmeContract 1.20261012.0: the wire format apps use to talk to each other.
 */
public final class Wire implements StatelessFunctionalObject {

    /** Callers copy this constant into their own code when they compile. */
    public static final int WIRE_VERSION = 1;

    public String release() {
        return "AcmeContract 1.20261012.0";
    }

    /** The wire version of the AcmeContract that is running, read at run time. */
    public int version() {
        return WIRE_VERSION;
    }
}
