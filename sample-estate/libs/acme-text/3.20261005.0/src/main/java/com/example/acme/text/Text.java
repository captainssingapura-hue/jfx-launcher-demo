package com.example.acme.text;

import com.example.acme.core.Core;
import com.google.common.base.Joiner;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * AcmeText 3.20261005.0: the words around the numbers. Uses Guava.
 */
public final class Text implements StatelessFunctionalObject {

    public String release() {
        return "AcmeText 3.20261005.0";
    }

    public String coreRelease() {
        return new Core().release();
    }

    /** {@code === Ledger ===} */
    public String banner(final String title) {
        return Joiner.on(' ').join("===", title, "===");
    }

    /** Where this text comes from. Line 3 uses an AcmeCore method that line 9 removes. */
    public String origin() {
        return "text for " + new Core().legacyName();
    }
}
