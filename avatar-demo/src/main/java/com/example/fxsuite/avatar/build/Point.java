package com.example.fxsuite.avatar.build;

/**
 * A point, or a direction, in a building's own frame: {@code across} to its right, {@code along}
 * ahead, {@code up} from the ground, in centimetres.
 */
public record Point(double across, double along, double up) {

    public static final Point ZERO = new Point(0, 0, 0);

    public Point plus(final Point other) {
        return new Point(across + other.across, along + other.along, up + other.up);
    }

    public Point times(final double k) {
        return new Point(across * k, along * k, up * k);
    }

    public double dot(final Point other) {
        return across * other.across + along * other.along + up * other.up;
    }

    public double length() {
        return Math.sqrt(dot(this));
    }
}
