package com.example.fxsuite.avatar.sound;

import java.util.Arrays;
import java.util.List;

/**
 * The catalogue: every sound by name, and what its one number means. What the game picks from
 * as things happen, and what the bench lists. How each one sounds is up to the {@link Profile}.
 */
public enum Sound {

    STEP_GROUND("Footstep on the ground", "weight", false),
    STEP_BLOCK("Footstep on a block", "weight", false),
    JUMP("Jump", null, false),
    LANDING("Landing on the ground", "impact", false),
    LANDING_BLOCK("Landing on a block", "impact", false),
    BUMP("Bump", "impact", false),
    PACK_ON("Jet-pack on", null, false),
    PACK_OFF("Jet-pack off", null, false),
    WINGS_OPEN("Wings open", null, false),
    WINGS_FOLD("Wings fold", null, false),
    IGNITION("Take-off", null, false),
    CUT("Engines cut", null, false),
    GATE("Through a gate", "speed", false),
    RIFLE_SHOT("Rifle shot", null, false),
    RIFLES_DRAWN("Rifles drawn", null, false),
    GLAIVE_CLEAVE("Glaive cleave", null, false),
    GLAIVE_SWEEP("Glaive sweep", null, false),
    GLAIVE_DRAWN("Glaive drawn", null, false),
    ROUND_ON_BLOCK("Round strikes a block", null, false),
    ROUND_ON_GROUND("Round strikes the ground", null, false),
    BLADE_ON_BLOCK("Blade strikes a block", null, false),
    BLADE_ON_GROUND("Blade strikes the ground", null, false),
    GATLING_DRAWN("Gatling gun drawn", null, false),
    BREAK_WOOD("A crate or dummy breaks", "size", false, 600, 0.3),
    BREAK_DRONE("A drone blows up", null, false, 1500, 0.55),
    BREAK_AIRSHIP("An airship comes down", null, false, 6000, 0.75),
    BOMB("A bomb goes off", null, false, 3000, 0.5),
    JET_RUMBLE("Jet rumble", null, true),
    JET_HISS("Jet hiss", null, true),
    JET_WHINE("Jet whine", null, true),
    WIND("Wind", null, true),
    GATLING_SPIN("Gatling gun spinning", null, true),
    GATLING_FIRE("Gatling gun firing", null, true),
    AIRSHIP_ENGINES("Airship engines", null, true);

    /** How far most sounds are heard at full loudness: 3 m, as the outputs fade them. */
    public static final double NEAR = 300;

    private final String title;
    private final String amount;
    private final boolean loop;
    private final double reach;
    private final double duck;

    Sound(final String title, final String amount, final boolean loop) {
        this(title, amount, loop, NEAR, 0);
    }

    Sound(final String title, final String amount, final boolean loop, final double reach, final double duck) {
        this.title = title;
        this.amount = amount;
        this.loop = loop;
        this.reach = reach;
        this.duck = duck;
    }

    /**
     * How far off it is still heard at full loudness, before it fades with distance: {@link #NEAR}
     * for most, much further for an explosion, as a blast carries.
     */
    public double reach() {
        return reach;
    }

    /**
     * How far, 0 to 1, it pushes the loops (the jets, the wind, the Gatling gun) down for a moment
     * when heard at full loudness: an explosion's, so it punches through; nothing for the rest.
     */
    public double duck() {
        return duck;
    }

    public String title() {
        return title;
    }

    /** What the sound's one number means ("weight", "impact"), or null if it has none. */
    public String amount() {
        return amount;
    }

    public boolean loop() {
        return loop;
    }

    public static List<Sound> oneShots() {
        return Arrays.stream(values()).filter(s -> !s.loop).toList();
    }

    public static List<Sound> loops() {
        return Arrays.stream(values()).filter(s -> s.loop).toList();
    }

    @Override
    public String toString() {
        return title;
    }
}
