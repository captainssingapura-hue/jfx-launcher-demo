package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * A river across the world: its middle {@code line} (x, z, in order along it), so {@code wide}, its water
 * {@code top} over the ground; shallow, waded through. Where water is, for what is drawn of it and, in
 * time, for what moves in it and through it.
 */
public record River(List<Vec2> line, double wide, double top) {

    public River {
        line = List.copyOf(line);
        if (line.size() < 2 || !(wide > 0) || top < 0) {
            throw new IllegalArgumentException("a river runs somewhere, some width, its water at or over the ground");
        }
    }

    /** How far ({@code x}, {@code z}) is from its middle line. */
    public double distance(final double x, final double z) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int i = 0; i + 1 < line.size(); i++) {
            final Vec2 a = line.get(i);
            final Vec2 b = line.get(i + 1);
            final double dx = b.x() - a.x();
            final double dz = b.z() - a.z();
            final double length = dx * dx + dz * dz;
            final double t = length == 0 ? 0 : Math.clamp(((x - a.x()) * dx + (z - a.z()) * dz) / length, 0, 1);
            nearest = Math.min(nearest, Math.hypot(x - a.x() - t * dx, z - a.z() - t * dz));
        }
        return nearest;
    }

    /** Whether ({@code x}, {@code z}) is in its water. */
    public boolean wet(final double x, final double z) {
        return distance(x, z) < wide / 2;
    }
}
