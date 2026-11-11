package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What the avatar holds and what it is doing with it: the timing of the weapons, and nothing
 * yet of what they hit. Pure.
 *
 * <ul>
 *   <li><b>Rifles</b>, one in each hand, fire in turn while the trigger is held: left, right,
 *       left. Each fires {@link #RIFLE_RATE} rounds a minute, so together twice that.</li>
 *   <li><b>The glaive</b> swings in strokes of {@link #STROKE} seconds while the trigger is held:
 *       an overhead cleave, then a sweep across, then a cleave again. Each stroke winds up first;
 *       its blade comes through at {@link #STRIKE} of the way in, which is when it is heard.
 *       Let go mid-stroke, and the stroke still finishes.</li>
 * </ul>
 *
 * @param held     what is in the hands
 * @param reload   seconds until the next round can fire
 * @param leftNext whether the next round comes from the left rifle
 * @param sinceLeft  seconds since the left rifle last fired; likewise the right
 * @param stroke   seconds into the glaive's stroke, or negative between strokes
 * @param strokes  how many strokes have been begun, the one under way among them
 */
public record Arms(Weapon held, double reload, boolean leftNext, double sinceLeft, double sinceRight,
                   double stroke, int strokes) {

    /** Rounds a minute from each rifle: as an assault rifle fires. */
    public static final double RIFLE_RATE = 600;
    /** Between rounds, the two rifles taking turns. */
    public static final double RIFLE_INTERVAL = 60 / RIFLE_RATE / 2;
    /** One stroke of the glaive, wind-up to recovery, in seconds. */
    public static final double STROKE = 0.75;
    /** How far into a stroke the blade comes through, as a share of it. */
    public static final double STRIKE = 0.42;
    /** The strokes, in the order they come. */
    public static final int CLEAVE = 0;
    public static final int SWEEP = 1;

    private static final double LONG_AGO = 1e9;

    /** Empty hands. */
    public static final Arms EMPTY = holding(Weapon.NONE);

    /** Freshly drawn, nothing fired or swung. */
    public static Arms holding(final Weapon weapon) {
        return new Arms(weapon, 0, true, LONG_AGO, LONG_AGO, -1, 0);
    }

    /** What changed, and what happened. */
    public record Stepped(Arms arms, List<Event> events) {
    }

    public Stepped step(final boolean trigger, final double seconds) {
        final List<Event> events = new ArrayList<>();
        return switch (held) {
            case NONE -> new Stepped(this, events);
            case RIFLES -> fire(trigger, seconds, events);
            case GLAIVE -> swing(trigger, seconds, events);
        };
    }

    /** How far through the stroke under way, 0 to 1; negative between strokes. */
    public double progress() {
        return stroke < 0 ? -1 : stroke / STROKE;
    }

    /** Which stroke is under way ({@link #CLEAVE} or {@link #SWEEP}), or the last one. */
    public int strokeKind() {
        return Math.max(0, strokes - 1) % 2;
    }

    private Stepped fire(final boolean trigger, final double seconds, final List<Event> events) {
        double left = sinceLeft + seconds;
        double right = sinceRight + seconds;
        double wait = reload - seconds;
        boolean nextLeft = leftNext;
        while (trigger && wait <= 0) {
            // how long ago, within this frame, the round went
            final double ago = -wait;
            if (nextLeft) {
                left = ago;
            } else {
                right = ago;
            }
            events.add(new Event.Fired(nextLeft));
            nextLeft = !nextLeft;
            wait += RIFLE_INTERVAL;
        }
        // let go, and the next squeeze fires at once, once the action has cycled
        final double next = trigger ? wait : Math.max(0, wait);
        return new Stepped(new Arms(held, next, nextLeft, left, right, stroke, strokes), events);
    }

    private Stepped swing(final boolean trigger, final double seconds, final List<Event> events) {
        double at = stroke;
        int count = strokes;
        if (at < 0 && trigger) {
            at = 0;
            count++;
            if (seconds <= 0) {
                return new Stepped(new Arms(held, reload, leftNext, sinceLeft, sinceRight, at, count), events);
            }
        }
        if (at < 0) {
            return new Stepped(this, events);
        }
        final double was = at;
        at += seconds;
        if (was < STRIKE * STROKE && at >= STRIKE * STROKE) {
            events.add(new Event.Swung((count - 1) % 2));
        }
        if (at >= STROKE) {
            if (trigger) {
                // straight into the next stroke
                at -= STROKE;
                count++;
                if (at >= STRIKE * STROKE) {
                    events.add(new Event.Swung((count - 1) % 2));
                }
            } else {
                at = -1;
            }
        }
        return new Stepped(new Arms(held, reload, leftNext, sinceLeft, sinceRight, at, count), events);
    }
}
