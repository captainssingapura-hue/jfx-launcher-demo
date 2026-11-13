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
 *   <li>The Gatling gun's loops are silent while it is still, and not firing.</li>
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

    /**
     * What the loops follow: how hard the jets fire ({@code thrust}, 0 to 1), how fast the avatar
     * moves ({@code speed}), how fast the Gatling gun's barrels spin ({@code spin}, 0 to 1), and
     * whether it is firing; and the engine of the vehicle it drives, if any: how fast it turns
     * ({@code rev}, 0 at idle to 1 at the limit; below 0 for none running) and how hard it is
     * worked ({@code load}, 0 to 1, the throttle).
     */
    record Drive(double thrust, double speed, double spin, boolean firing, double rev, double load) {

        /** With no engine running. */
        public Drive(final double thrust, final double speed, final double spin, final boolean firing) {
            this(thrust, speed, spin, firing, -1, 0);
        }
    }

    /** How a loop plays, for what drives it. */
    Level level(Sound loop, Drive drive);

    /** How a loop plays for the jets firing at {@code thrust} and moving at {@code speed}, the gun still. */
    default Level level(final Sound loop, final double thrust, final double speed) {
        return level(loop, new Drive(thrust, speed, 0, false));
    }

    /** Speed as a share of full boost, from 0, capped at 1.5: what the wind follows. */
    static double pace(final double speed) {
        return Num.clamp(speed / Body.BOOST, 0, 1.5);
    }
}
