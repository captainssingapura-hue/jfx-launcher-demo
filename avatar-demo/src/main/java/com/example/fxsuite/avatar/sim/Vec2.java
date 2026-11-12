package com.example.fxsuite.avatar.sim;

/** A distance or direction across the ground: x and z, the world's level axes. */
public record Vec2(double x, double z) {

    public double magnitude() {
        return Math.hypot(x, z);
    }
}
