package com.example.fxsuite.avatar.sim;

/**
 * How an aircraft flies (a gunship lifted by two big ducted rotors, say), and its flight a step on.
 * Pure: numbers in, numbers out, so it can be flown in a test as on screen.
 *
 * <p>It flies as a body does: it has momentum, the air drags on it, and what holds it up is its
 * rotors' thrust, along their axis, and its wings' lift once it is going fast enough. Its rotors
 * push only the way they point, so to go anywhere it has to tip that way: its nose down to go
 * ahead, a wing down to slide aside. And they tilt ({@code nacelle}): upright to hover, forward to
 * fly fast, as a tilt-rotor's do, its wings then carrying its weight.
 *
 * <p>Its keys are not its controls themselves but what is wanted of it, as a fly-by-wire
 * aircraft's are ({@link #step}): W and S a speed ahead or back, A and D a rate of turn, as a land
 * vehicle's are, Q and E a speed aside (hovering: less and less as it speeds up, none once it flies
 * as an aeroplane does), Space and Ctrl a rate of climb. Its assist works out the attitude and the thrust
 * that would get it there, and tips it toward that attitude no faster than it can turn; what it
 * then does is what its thrust, lift, drag and weight make it do. So it takes time to get going and
 * to stop, overshoots a little, and cannot climb faster than its thrust allows. Let go, it is
 * wanted still: it pitches up to stop and hangs where it is, as a helicopter's hover hold does.
 * Its engine cut, it has no thrust: it glides as far as its wings carry it, and drops.
 *
 * <p>All in centimetres, seconds and degrees; accelerations a share of nothing, centimetres a
 * second a second (its mass never matters, only what it can do with it).
 *
 * @param power      its thrust at most, a share of its weight
 * @param ahead      the most it is wanted going ahead (W held)
 * @param back       and back (S held)
 * @param aside      and aside (Q or E held), hovering; none by {@link #SLIDES}
 * @param climb      the most it is wanted climbing or coming down (Space or Ctrl held)
 * @param drag       how hard the air drags it going ahead, per speed squared: its top speed is
 *                   {@code sqrt(power * g / drag)}, its weight on its wings
 * @param broadside  how much harder going aside than ahead
 * @param edgeways   and going up or down
 * @param carried    how fast it goes ahead for its wings to carry all its weight
 * @param turn       how fast it turns at most (A or D held), degrees a second, hovering
 * @param cruiseTurn and going fast, its rotors forward
 * @param nose       how far it pitches at most, nose down or up, degrees
 * @param bank       how far it banks at most
 * @param attitude   how fast it pitches or banks at most, degrees a second
 * @param converts   from how fast ahead its rotors start tilting forward
 * @param converted  and by how fast they are as far forward as they go ({@code forward})
 * @param forward    how far forward its rotors tilt at most, degrees from upright
 * @param tilting    how fast they tilt, degrees a second
 */
public record Flight(double power, double ahead, double back, double aside, double climb, double drag,
                     double broadside, double edgeways, double carried, double turn, double cruiseTurn, double nose,
                     double bank, double attitude, double converts, double converted, double forward,
                     double tilting) {

    /** How briskly its assist goes after the speed it is wanted at, a second. */
    public static final double GAIN = 1.2;
    /** And the climb. */
    public static final double CLIMB_GAIN = 2;
    /** The most it asks of itself across the ground, and up or down: shares of {@link Body#GRAVITY}. */
    public static final double URGE = 0.5;
    public static final double LIFT_URGE = 0.6;
    /** The most its wings can carry, a share of its weight. */
    public static final double MOST_LIFT = 1.6;
    /** On its wheels, how hard its brakes stop it rolling. */
    public static final double BRAKES = 1500;
    /** How fast it comes down at most onto the ground, and how much faster for each centimetre higher over it. */
    public static final double SETTLE = 120;
    public static final double FLARE = 0.5;
    /** How fast ahead it no longer slides aside at all: it flies as an aeroplane does, not as a helicopter. */
    public static final double SLIDES = 2500;
    /** How quickly its turning builds up or dies away, degrees a second, a second: a tap turns it a little. */
    public static final double YAW_SWING = 150;

    /**
     * Where it is and how it is going: {@code yaw} its heading; {@code ahead}, {@code aside} and
     * {@code climb} its velocity in the world, along its heading, to its right and up;
     * {@code pitch} its nose up, {@code roll} its right wing down, and {@code nacelle} its rotors
     * tilted forward from upright, all in degrees; {@code height} where it is; {@code thrust} how
     * hard its rotors push, a share of its most; {@code grounded} on its wheels; {@code turning} how fast
     * it turns, degrees a second, right when positive.
     */
    public record State(double height, double yaw, double ahead, double aside, double climb, double pitch,
                        double roll, double nacelle, double thrust, boolean grounded, double turning) {
    }

    /**
     * What it is asked, for a step: {@code forward} (W, S), {@code aside} (Q, E), {@code lift} (Space,
     * Ctrl) and {@code turn} (A, D) from -1 to 1 as the keys give them, right and up when positive;
     * {@code pace} 1 or less to go carefully; nothing if it is not {@code held}; and its
     * {@code engine} running or not.
     */
    public record Input(double forward, double aside, double lift, double turn, double pace, boolean held,
                        boolean engine) {

        /** Let go: no one at its controls, its engine running or not. */
        public static Input none(final boolean engine) {
            return new Input(0, 0, 0, 0, 1, false, engine);
        }
    }

    /** Its top speed ahead, its wings carrying its weight, its rotors forward and at full. */
    public double top() {
        return Math.sqrt(power * Body.GRAVITY / drag);
    }

    /** How far forward its rotors are wanted, going {@code ahead}: upright slow, forward fast. */
    public double nacelleFor(final double ahead) {
        final double t = Num.clamp((ahead - converts) / (converted - converts), 0, 1);
        return forward * t * t * (3 - 2 * t);
    }

    /** How far its wings hold it up, going {@code ahead}: a share of its weight, at most {@link #MOST_LIFT}. */
    public double wingLift(final double ahead) {
        final double share = Math.max(0, ahead) / carried;
        return Body.GRAVITY * Math.min(MOST_LIFT, share * share);
    }

    /**
     * {@code state}, {@code seconds} on, as asked by {@code input}; its wheels no lower than
     * {@code floor} (where it would stand on what is under it).
     */
    public State step(final State state, final Input input, final double seconds, final double floor) {
        final double g = Body.GRAVITY;
        final boolean engine = input.engine();
        final boolean held = input.held() && engine;
        // its heading first, as fast as it turns: slower, its rotors forward; and going fast, no faster than
        // its bank can swing its way round with it, so it turns as an aeroplane does, not skidding
        final double rate = turn + (cruiseTurn - turn) * state.nacelle() / Math.max(1, forward);
        final double banked = Math.toDegrees(g * Math.tan(Math.toRadians(bank)) / Math.max(Math.abs(state.ahead()), 1));
        final double pace = Num.clamp(input.pace(), 0, 1);
        final double wantTurning = held && !state.grounded() ? Num.clamp(input.turn(), -1, 1) * Math.min(rate, banked) * pace : 0;
        // (on its wheels it does not turn: it has to be flying to)
        // its turning built up, or dying away, no faster than it swings
        final double turning = toward(state.turning(), wantTurning, YAW_SWING * seconds);
        final double turned = (state.turning() + turning) / 2 * seconds;
        final double yaw = Num.wrap(state.yaw() + turned);
        // its velocity is the world's: seen from its new heading
        final double r = Math.toRadians(turned);
        final double a0 = state.ahead() * Math.cos(r) + state.aside() * Math.sin(r);
        final double s0 = state.aside() * Math.cos(r) - state.ahead() * Math.sin(r);
        final double c0 = state.climb();

        // what is wanted of it, and so what it asks of itself, as its assist reckons it
        final double forward = held ? Num.clamp(input.forward(), -1, 1) : 0;
        final double wantAhead = forward * (forward > 0 ? ahead : back) * pace;
        // aside, hovering; less and less as it speeds up and its wings take over
        final double slides = 1 - Num.clamp((Math.abs(a0) - converts) / (SLIDES - converts), 0, 1);
        final double wantAside = held ? Num.clamp(input.aside(), -1, 1) * aside * pace * slides : 0;
        // and coming down, no faster near the ground than lets it settle gently onto its wheels
        final double wantClimb = held ? Math.max(Num.clamp(input.lift(), -1, 1) * climb,
                -(SETTLE + FLARE * Math.max(0, state.height() - floor))) : 0;
        final double urgeAhead = Num.clamp(GAIN * (wantAhead - a0), -URGE * g, URGE * g);
        final double urgeAside = Num.clamp(GAIN * (wantAside - s0), -URGE * g, URGE * g);
        final double urgeUp = Num.clamp(CLIMB_GAIN * (wantClimb - c0), -LIFT_URGE * g, LIFT_URGE * g);

        // the air's drag on it
        final double dragAhead = -drag * a0 * Math.abs(a0);
        final double dragAside = -drag * broadside * s0 * Math.abs(s0);
        final double dragUp = -drag * edgeways * c0 * Math.abs(c0);

        // what it needs, all told, besides the drag, against its weight: from its rotors and its wings
        final double needAhead = urgeAhead - dragAhead;
        // and turning, what swings its way round with it, as it goes
        final double swing = seconds > 0 ? a0 * Math.toRadians(turned) / seconds : 0;
        final double needAside = urgeAside - dragAside + swing;
        final double needUp = urgeUp - dragUp + g;
        final double available = wingLift(a0);
        // sat on its wheels, not asked to climb: its rotors idling, its weight on its wheels
        final boolean sitting = state.grounded() && wantClimb <= 0;

        // its rotors tilted as its speed calls for, or the speed it is wanted at, slowing: back upright
        // as soon as it is to slow down, to brake with them as it pitches up
        final double nacelle = engine
                ? toward(state.nacelle(), nacelleFor(Math.min(a0, Math.max(0, wantAhead))), tilting * seconds)
                : state.nacelle();
        // the attitude that would get it: banked as what it needs leans, its wings carrying what they
        // can of it, its rotors the rest, pitched so they point that way
        final double wantRoll;
        final double wantPitch;
        if (!held || sitting) {
            // let go, or sat on its wheels: level
            wantRoll = 0;
            wantPitch = 0;
        } else {
            wantRoll = Num.clamp(Math.toDegrees(Math.atan2(needAside, Math.max(needUp, 0.05 * g))), -bank, bank);
            final double rolledUp = Math.hypot(needAside, Math.max(needUp, 0));
            final double fromRotors = Math.max(rolledUp - Math.min(available, rolledUp), 0.02 * g);
            final double alpha = Math.toDegrees(Math.atan2(needAhead, fromRotors));
            wantPitch = Num.clamp(nacelle - alpha, -nose, nose);
        }
        final double step = attitude * seconds;
        final double pitch = toward(state.pitch(), wantPitch, step);
        final double roll = toward(state.roll(), wantRoll, step * 1.3);

        // and as it is: its rotors pushing along their axis, as hard as asked and no harder than they can
        final double alpha = Math.toRadians(nacelle - pitch);
        final double phi = Math.toRadians(roll);
        final double up = needAside * Math.sin(phi) + needUp * Math.cos(phi);
        final double guess = engine && !sitting ? Math.min(available, Math.max(up, 0)) : available;
        final double asked = needAhead * Math.sin(alpha) + (up - guess) * Math.cos(alpha);
        final double thrust = engine && !sitting ? Num.clamp(asked, 0, power * g) : 0;
        // its wings trimmed to carry what its rotors do not, and no more: it does not balloon
        final double lift = engine && !sitting
                ? Math.min(available, Math.max(up - thrust * Math.cos(alpha), 0)) : available;
        final double raised = thrust * Math.cos(alpha) + lift;
        final double accAhead = thrust * Math.sin(alpha) + dragAhead;
        final double accAside = raised * Math.sin(phi) + dragAside;
        final double accUp = raised * Math.cos(phi) + dragUp - g;

        double a1 = a0 + accAhead * seconds;
        double s1 = s0 + accAside * seconds;
        double c1 = c0 + accUp * seconds;
        double height = state.height() + (c0 + c1) / 2 * seconds;
        boolean grounded = false;
        if (height <= floor) {
            // on its wheels: no lower, braked to a stop across the ground but for what lifts it off
            height = floor;
            c1 = Math.max(0, c1);
            grounded = c1 == 0;
            if (grounded) {
                final double speed = Math.hypot(a1, s1);
                final double kept = speed > 0 ? Math.max(0, speed - BRAKES * seconds) / speed : 0;
                a1 *= kept;
                s1 *= kept;
            }
        }
        return new State(height, yaw, a1, s1, c1, grounded ? toward(pitch, 0, step) : pitch,
                grounded ? toward(roll, 0, step) : roll, nacelle, thrust / (power * g), grounded,
                grounded ? 0 : turning);
    }

    /** {@code value} moved toward {@code target} by {@code most} at most. */
    private static double toward(final double value, final double target, final double most) {
        return value + Num.clamp(target - value, -most, most);
    }
}
