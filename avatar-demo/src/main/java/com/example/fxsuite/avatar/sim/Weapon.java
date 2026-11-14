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
     * A heavy Gatling gun, on a mount: six long barrels, twice the robot's size, slower to spin up,
     * slower to fire, and far heavier rounds, each five times the hand-held one's. Too heavy to
     * hold: it is manned where it is mounted ({@link Turret}).
     */
    HEAVY_GATLING("Heavy Gatling gun", 40),
    /**
     * An airship's bomb, going off: every piece that can break within {@link Sky#BLAST} of it
     * takes this. A drone or a small crate goes at once; a dummy takes two.
     */
    BOMB("Bomb", 150),
    /** Coming down harder than a loose thing can take ({@link Loose#landing}): enough to break anything. */
    FALL("A fall", 100_000),
    /**
     * A collision: a land vehicle running into something that gives, a crate or another vehicle,
     * as hard as the energy the collision takes out of the two of them, dealt as so many blows of
     * this ({@link Play}).
     */
    CRASH("A crash", 10);

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
