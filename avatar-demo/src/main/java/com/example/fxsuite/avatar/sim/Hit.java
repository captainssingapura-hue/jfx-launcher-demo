package com.example.fxsuite.avatar.sim;

/**
 * Where a ray met something: the point, the outward direction of the surface there, what it
 * was, and how far along the ray. See {@link Obstacles#ray}.
 */
public record Hit(Vec3 at, Vec3 normal, Surface on, double distance) {
}
