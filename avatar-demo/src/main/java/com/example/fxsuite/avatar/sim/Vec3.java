package com.example.fxsuite.avatar.sim;

/**
 * A point or direction in the world, in world units (100 to the metre). The axes are JavaFX's,
 * so y points down and the height above the ground is {@code -y}, but the simulation does not
 * depend on JavaFX: the graphics and the sound each read these numbers in their own way.
 */
public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public Vec3 add(final Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 add(final double dx, final double dy, final double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    public Vec3 subtract(final Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 multiply(final double factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }

    public double dotProduct(final Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distance(final Vec3 other) {
        return subtract(other).magnitude();
    }

    /** The same direction, a unit long; zero stays zero. */
    public Vec3 normalize() {
        final double length = magnitude();
        return length == 0 ? this : multiply(1 / length);
    }
}
