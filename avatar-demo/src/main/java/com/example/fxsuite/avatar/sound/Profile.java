package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Num;

/**
 * A sound profile: one whole way for the avatar to sound. The {@link Sound} catalogue says
 * <em>what</em> can be heard (a footfall, a landing, the jets); a profile says how each one
 * sounds, and how its loops follow the thrust and the speed. Switch the profile and everything
 * changes character together, while the game, which only picks sounds from the catalogue,
 * changes not at all.
 *
 * <p>Every profile makes every sound in the catalogue, and keeps to the same promises, which
 * the tests hold each to:
 * <ul>
 *   <li>Pure: the same arguments, the same samples.</li>
 *   <li>Within full scale, and audible.</li>
 *   <li>One-shots start and end in silence; loops run on into their start without a click.</li>
 *   <li>A sound with an amount (weight, impact) is louder for more; another {@code variant}
 *       is another take.</li>
 *   <li>The jets are silent while not firing; there is no wind at a walk.</li>
 * </ul>
 */
public interface Profile {

    /** A short name for files and settings, such as "robot". */
    String id();

    String title();

    /** One line on what it sounds like. */
    String about();

    /** Makes a sound: {@code amount} 0 to 1 where it has one, {@code variant} for a different take. */
    Clip render(Sound sound, double amount, int variant);

    /** How a loop plays, for the jets firing at {@code thrust} (0 to 1) and moving at {@code speed}. */
    Level level(Sound loop, double thrust, double speed);

    /** Speed as a share of full boost, from 0, capped at 1.5: what the wind follows. */
    static double pace(final double speed) {
        return Num.clamp(speed / Body.BOOST, 0, 1.5);
    }
}
