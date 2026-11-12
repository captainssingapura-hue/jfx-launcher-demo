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
    JET_RUMBLE("Jet rumble", null, true),
    JET_HISS("Jet hiss", null, true),
    JET_WHINE("Jet whine", null, true),
    WIND("Wind", null, true);

    private final String title;
    private final String amount;
    private final boolean loop;

    Sound(final String title, final String amount, final boolean loop) {
        this.title = title;
        this.amount = amount;
        this.loop = loop;
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
