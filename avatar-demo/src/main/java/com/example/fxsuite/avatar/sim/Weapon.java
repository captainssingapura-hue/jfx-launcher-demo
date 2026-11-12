package com.example.fxsuite.avatar.sim;

/**
 * What deals blows, and how much harm one does to what it strikes ({@link Damage}): what the
 * avatar holds in its hands, a round or a stroke of the blade; and the airships' bombs, which no
 * one holds.
 */
public enum Weapon {
    /** Empty hands. */
    NONE("Bare hands", 0),
    /** Two assault rifles, one in each hand, fired in turn: 20 rounds a second between them. */
    RIFLES("Twin rifles", 10),
    /** A glaive: a long pole with a broad curved blade, as Guan Yu carried, in the right hand. */
    GLAIVE("Glaive", 100),
    /**
     * A Gatling gun with a motor: six barrels that spin up before it fires. Held at the right
     * hip by its rear grip, the left hand on a lever on top, in front. Its rounds are a little
     * lighter than the rifles', but there are 40 of them a second.
     */
    GATLING("Gatling gun", 8),
    /**
     * An airship's bomb, going off: every piece that can break within {@link Sky#BLAST} of it
     * takes this. A drone or a small crate goes at once; a dummy takes two.
     */
    BOMB("Bomb", 150),
    /** Coming down harder than a loose thing can take ({@link Loose#landing}): enough to break anything. */
    FALL("A fall", 100_000);

    private final String title;
    private final double blow;

    Weapon(final String title, final double blow) {
        this.title = title;
        this.blow = blow;
    }

    public String title() {
        return title;
    }

    /** The harm one round or stroke does to what it strikes. */
    public double blow() {
        return blow;
    }
}
