package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A gun, and what is common to firing every gun, whoever or whatever holds it and however it is
 * aimed: the rounds it fires, how often, how far off true, how its barrels spin up and down, and
 * what each round strikes. Pure.
 *
 * <p>How a gun is aimed is not its own business. What holds it says where each round leaves from
 * and along which line ({@link Bore}), every step: a hand-held gun goes where the hands go, turned
 * with the body ({@link Arms}); a gun on a mount, wherever the mount has turned it. From there it
 * is all the same: {@link #fire} runs the trigger, the barrels and the rate of fire, and
 * {@link #shoot} flies each round to whatever it meets, a {@link Blow} for the world to take.
 *
 * <ul>
 *   <li><b>A pair</b> ({@code pair}), one gun in each hand, fire in turn while the trigger is
 *       held: left, right, left, a round each {@code interval}.</li>
 *   <li><b>A gun that spins</b> ({@code spinUp} above 0) spins its barrels up first, over
 *       {@code spinUp} seconds, and fires only at full speed, a round each {@code interval}. Let
 *       go, it stops firing at once and spins down over {@code spinDown}; squeezed again before it
 *       has stopped, it is back up to speed the sooner.</li>
 *   <li><b>Each round</b> flies along the line, off true by up to {@code spread} degrees each way,
 *       by its count rather than by chance, so a run repeats exactly; and strikes the first thing
 *       in its way within {@code range}, a block or the ground.</li>
 * </ul>
 *
 * @param by       what its rounds count as, for the damage they do and how they are seen and heard
 * @param interval seconds between rounds, at full speed
 * @param spread   degrees off true a round may go, each way
 * @param spinUp   seconds for its barrels to spin up from still; 0 for a gun that does not spin
 * @param spinDown seconds for them to spin down from full speed
 * @param pair     whether it is two guns firing in turn, left and right
 * @param range    how far a round can fly
 */
public record Gun(Weapon by, double interval, double spread, double spinUp, double spinDown, boolean pair,
                  double range) {

    /** The twin rifles, fired from the hip, in turn. */
    public static final Gun RIFLES = new Gun(Weapon.RIFLES, Arms.RIFLE_INTERVAL, Arms.SPREAD, 0, 0, true, Arms.RANGE);
    /** The hand-held Gatling gun, held dead steady. */
    public static final Gun GATLING = new Gun(Weapon.GATLING, Arms.GATLING_INTERVAL, Arms.GATLING_SPREAD,
            Arms.SPIN_UP, Arms.SPIN_DOWN, false, Arms.RANGE);
    /**
     * The heavy Gatling gun, on a mount: 1800 rounds a minute once its long barrels are up to
     * speed, which takes twice as long; nearly steady on its mount; and reaching further.
     */
    public static final Gun HEAVY_GATLING = new Gun(Weapon.HEAVY_GATLING, 60.0 / 1800, 0.35, 1.2, 2.5, false, 40_000);

    /** Whether its barrels spin up before it fires. */
    public boolean spins() {
        return spinUp > 0;
    }

    /**
     * Where a round leaves from, {@code from}, and the line it flies along, {@code line} (a unit
     * vector), with {@code across}, the way that is to the line's right, level: which way a round
     * off true goes sideways. Up and down off true is always the world's own up and down.
     */
    public record Bore(Vec3 from, Vec3 line, Vec3 across) {
    }

    /** Where its rounds leave from now: from the left gun of a pair, or the right (or the only one). */
    @FunctionalInterface
    public interface Muzzles {
        Bore bore(boolean left);
    }

    /**
     * How a gun is getting on: seconds until its next round can go ({@code reload}), whether the
     * left of a pair fires next, seconds since each of the pair last fired (a single gun counts as
     * the right), how many rounds it has fired, and how fast its barrels turn, 0 still to 1 full
     * speed.
     */
    public record State(double reload, boolean leftNext, double sinceLeft, double sinceRight, int rounds,
                        double spin) {

        private static final double LONG_AGO = 1e9;

        /** Freshly drawn or mounted: nothing fired, still. */
        public static final State READY = new State(0, true, LONG_AGO, LONG_AGO, 0, 0);
    }

    /** A gun a step on, what happened (each round {@link Event.Fired}), and the {@link Blow}s its rounds dealt. */
    public record Fired(State state, List<Event> events, List<Blow> blows) {

        public Fired {
            events = List.copyOf(events);
            blows = List.copyOf(blows);
        }
    }

    /**
     * Fires for {@code seconds} from {@code state}, the trigger held or not, the rounds leaving
     * from {@code muzzles} into {@code world}: as a pair does, or as a gun that spins does.
     */
    public Fired fire(final State state, final boolean trigger, final double seconds, final Muzzles muzzles,
                      final Obstacles world) {
        final List<Event> events = new ArrayList<>();
        final List<Blow> blows = new ArrayList<>();
        final State next = spins() ? crank(state, trigger, seconds, muzzles, world, events, blows)
                : cycle(state, trigger, seconds, muzzles, world, events, blows);
        return new Fired(next, events, blows);
    }

    /** A pair, or any gun that does not spin: a round each interval while the trigger is held. */
    private State cycle(final State state, final boolean trigger, final double seconds, final Muzzles muzzles,
                        final Obstacles world, final List<Event> events, final List<Blow> blows) {
        double left = state.sinceLeft() + seconds;
        double right = state.sinceRight() + seconds;
        double wait = state.reload() - seconds;
        boolean nextLeft = pair && state.leftNext();
        int count = state.rounds();
        while (trigger && wait <= 0) {
            // how long ago, within this frame, the round went
            final double ago = -wait;
            if (nextLeft) {
                left = ago;
            } else {
                right = ago;
            }
            count++;
            shoot(muzzles.bore(nextLeft), nextLeft, count, events, blows, world);
            nextLeft = pair && !nextLeft;
            wait += interval;
        }
        // let go, and the next squeeze fires at once, once the action has cycled
        final double next = trigger ? wait : Math.max(0, wait);
        return new State(next, pair ? nextLeft : state.leftNext(), left, right, count, state.spin());
    }

    /** A gun that spins: spinning up or down, and firing once up to speed with the trigger held. */
    private State crank(final State state, final boolean trigger, final double seconds, final Muzzles muzzles,
                        final Obstacles world, final List<Event> events, final List<Blow> blows) {
        final double spin = state.spin();
        double since = state.sinceRight() + seconds;
        int count = state.rounds();
        double turning;
        double wait;
        if (!trigger) {
            turning = Math.max(0, spin - seconds / spinDown);
            wait = 0;
        } else {
            // the part of the frame spent still spinning up, then the rest firing
            final double spinning = Math.min(seconds, (1 - spin) * spinUp);
            turning = Math.min(1, spin + seconds / spinUp);
            wait = (spin < 1 ? 0 : state.reload()) - (seconds - spinning);
            while (turning >= 1 && wait <= 0) {
                since = -wait;
                count++;
                shoot(muzzles.bore(false), false, count, events, blows, world);
                wait += interval;
            }
        }
        return new State(wait, state.leftNext(), state.sinceLeft(), since, count, turning);
    }

    /** One round, the {@code round}th, along {@code bore}, a little off true, to whatever it strikes. */
    private void shoot(final Bore bore, final boolean left, final int round, final List<Event> events,
                       final List<Blow> blows, final Obstacles world) {
        shoot(bore, left, round, spread, by, range, world, events, blows);
    }

    /**
     * One round, the {@code round}th of its gun, along {@code bore}, off true by up to
     * {@code spread} degrees each way by its count, to the first thing in {@code world} within
     * {@code range}: heard and seen as {@link Event.Fired}, and, if it struck anything, a
     * {@link Blow} by {@code by}.
     */
    public static void shoot(final Bore bore, final boolean left, final int round, final double spread,
                             final Weapon by, final double range, final Obstacles world, final List<Event> events,
                             final List<Blow> blows) {
        final Vec3 from = bore.from();
        // off true by up to the spread each way, by the round's count
        final double tan = Math.tan(Math.toRadians(spread));
        final Vec3 direction = bore.line().add(bore.across().multiply(tan * scatter(round, 1)))
                .add(0, tan * scatter(round, 2), 0).normalize();
        final Hit hit = world.ray(from, direction, range);
        events.add(new Event.Fired(left, from, hit == null ? from.add(direction.multiply(range)) : hit.at(), by));
        if (hit != null) {
            blows.add(new Blow(hit.at(), hit.normal(), hit.on(), by, hit.piece()));
        }
    }

    /** -1 to 1, the same for the same round and salt: a spread that repeats. */
    private static double scatter(final int round, final int salt) {
        return Num.hash(round, salt) * 2 - 1;
    }
}
