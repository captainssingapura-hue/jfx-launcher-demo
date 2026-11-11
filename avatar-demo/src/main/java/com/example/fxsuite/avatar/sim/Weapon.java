package com.example.fxsuite.avatar.sim;

/** What the avatar holds in its hands. */
public enum Weapon {
    /** Empty hands. */
    NONE("Bare hands"),
    /** Two assault rifles, one in each hand, fired in turn. */
    RIFLES("Twin rifles"),
    /** A glaive: a long pole with a broad curved blade, as Guan Yu carried, in the right hand. */
    GLAIVE("Glaive");

    private final String title;

    Weapon(final String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
