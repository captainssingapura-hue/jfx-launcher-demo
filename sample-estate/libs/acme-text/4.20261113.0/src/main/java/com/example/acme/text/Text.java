package com.example.acme.text;

import com.example.acme.core.Core;
import com.google.common.base.Joiner;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import java.util.List;

/**
 * AcmeText 4.20261113.0: the words around the numbers. Uses Guava.
 */
public final class Text implements StatelessFunctionalObject {

    public String release() {
        return "AcmeText 4.20261113.0";
    }

    public String coreRelease() {
        return new Core().release();
    }

    /** {@code === Ledger ===} */
    public String banner(final String title) {
        return Joiner.on(' ').join("===", title, "===");
    }

    /** Where this text comes from. Line 4 no longer needs the method AcmeCore 9 removed. */
    public String origin() {
        return "text for " + new Core().release();
    }

    /** {@code cash, bank and card}. Added in 3.20261020.0. */
    public String list(final List<String> items) {
        if (items.size() < 2) {
            return Joiner.on("").join(items);
        }
        return Joiner.on(", ").join(items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }
}
