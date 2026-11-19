package com.example.fxsuite.avatar.lifts;

import java.util.Map;

/**
 * One thing about a scene that can be set: what it is ({@code key}, and {@code label} as shown), in what it is
 * measured ({@code unit}, as people say it: metres, metres a second, degrees, floors), from {@code least} to
 * {@code most} by {@code step}, {@code value} unless set otherwise.
 */
public record Knob(String key, String label, String unit, double least, double most, double step, double value) {

    public Knob {
        if (!(least <= value && value <= most && step > 0)) {
            throw new IllegalArgumentException(key + ": " + value + " outside " + least + " to " + most);
        }
    }

    /** Its value as {@code set} says, kept from {@link #least} to {@link #most}; else its own. */
    public double of(final Map<String, Double> set) {
        final Double v = set.get(key);
        return v == null ? value : Math.clamp(v, least, most);
    }
}
