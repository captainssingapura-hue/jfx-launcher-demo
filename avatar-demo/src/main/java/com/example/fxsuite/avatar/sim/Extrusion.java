package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * An outline laid on its side and drawn along: a section across, its {@code profile} (each point
 * across to the right, then up), drawn along a line through ({@code x}, {@code z}) heading
 * {@code yaw}, {@code half} each way. A gable roof is a triangle drawn along its ridge; a coping, a
 * vault, a bank. Its profile convex, either way round.
 *
 * <p>Met as a block is, its own way: between a body's feet and its head, what of it there is across
 * is a stretch of its profile, and along, its length; so it stops a body as a box that wide and
 * that long, and where its profile slopes, what is under a body's feet rises as a ramp's top does.
 */
public record Extrusion(double x, double z, double yaw, double half, List<Vec2> profile) implements Volume {

    public Extrusion {
        profile = List.copyOf(profile);
        if (profile.size() < 3 || Math.abs(Outline.twiceArea(profile)) < 1e-6 || !(half > 0)) {
            throw new IllegalArgumentException("not an extrusion: " + profile.size() + " points, " + half + " each way");
        }
    }

    /** Across, from its line: to its right. */
    private double across(final double px, final double pz) {
        final Vec3 right = Num.right(yaw);
        return (px - x) * right.x() + (pz - z) * right.z();
    }

    /** Along its line, from its middle. */
    private double along(final double px, final double pz) {
        final Vec3 ahead = Num.heading(yaw);
        return (px - x) * ahead.x() + (pz - z) * ahead.z();
    }

    @Override
    public double low() {
        return profile.stream().mapToDouble(Vec2::z).min().orElseThrow();
    }

    @Override
    public double high() {
        return profile.stream().mapToDouble(Vec2::z).max().orElseThrow();
    }

    /** Its profile, across from {@code from} to {@code to}: the part of it a circle that wide is over. */
    private List<Vec2> strip(final double from, final double to) {
        return Outline.clip(Outline.clip(profile, -1, 0, -from), 1, 0, to);
    }

    /** Its profile from {@code feet} to {@code head}: across, how far either way it reaches; null if none of it is there. */
    public double[] band(final double feet, final double head) {
        final List<Vec2> part = Outline.clip(Outline.clip(profile, 0, -1, -feet), 0, 1, head);
        if (part.size() < 3 || Math.abs(Outline.twiceArea(part)) < 1e-9) {
            return null;
        }
        return new double[] {part.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                part.stream().mapToDouble(Vec2::x).max().orElseThrow()};
    }

    /** The stretch of it from {@code feet} to {@code head}, as a box: so wide across, its length along; null if there is none. */
    public Block box(final double feet, final double head) {
        final double[] across = band(feet, head);
        if (across == null) {
            return null;
        }
        final double middle = (across[0] + across[1]) / 2;
        final Vec3 right = Num.right(yaw);
        return new Block(x + right.x() * middle, z + right.z() * middle, Math.max(1e-6, (across[1] - across[0]) / 2), half,
                yaw, feet, head);
    }

    @Override
    public boolean covers(final double px, final double pz, final double radius) {
        if (Math.abs(along(px, pz)) >= half + radius) {
            return false;
        }
        final double v = across(px, pz);
        return strip(v - radius, v + radius).size() >= 3;
    }

    @Override
    public double topAt(final double px, final double pz, final double radius) {
        if (Math.abs(along(px, pz)) >= half + radius) {
            return Double.NEGATIVE_INFINITY;
        }
        final double v = across(px, pz);
        return strip(v - radius, v + radius).stream().mapToDouble(Vec2::z).max().orElse(Double.NEGATIVE_INFINITY);
    }

    @Override
    public double undersideAt(final double px, final double pz, final double radius) {
        if (Math.abs(along(px, pz)) >= half + radius) {
            return Double.POSITIVE_INFINITY;
        }
        final double v = across(px, pz);
        return strip(v - radius, v + radius).stream().mapToDouble(Vec2::z).min().orElse(Double.POSITIVE_INFINITY);
    }

    @Override
    public Vec2 push(final double px, final double pz, final double radius, final double feet, final double head) {
        final Block box = box(feet, head);
        return box == null ? null : box.push(px, pz, radius);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        final Block box = box(feet, head);
        return box == null ? null : box.push(ax, az, bx, bz, radius);
    }

    /**
     * Where a ray enters it, and leaves: as a solid between two ends and each edge of its profile,
     * the latest it goes in through any of them and the earliest it comes out; with the way out of
     * where it went in. Null if it misses.
     */
    Span span(final Vec3 from, final Vec3 direction) {
        final Vec3 right = Num.right(yaw);
        final Vec3 ahead = Num.heading(yaw);
        // in its own frame: along, across, up (up is -y)
        final double u0 = (from.x() - x) * ahead.x() + (from.z() - z) * ahead.z();
        final double v0 = (from.x() - x) * right.x() + (from.z() - z) * right.z();
        final double y0 = -from.y();
        final double du = direction.x() * ahead.x() + direction.z() * ahead.z();
        final double dv = direction.x() * right.x() + direction.z() * right.z();
        final double dy = -direction.y();
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        Vec3 normal = null;
        // its two ends
        for (final double end : new double[] {-1, 1}) {
            // outside where end * u > half
            final double g = end * u0 - half;
            final double rate = end * du;
            if (Math.abs(rate) < 1e-12) {
                if (g > 0) {
                    return null;
                }
                continue;
            }
            final double t = -g / rate;
            if (rate < 0) {
                if (t > enter) {
                    enter = t;
                    normal = ahead.multiply(end);
                }
            } else {
                exit = Math.min(exit, t);
            }
        }
        // each edge of its profile
        final double turn = Math.signum(Outline.twiceArea(profile));
        for (int i = 0; i < profile.size(); i++) {
            final Vec2 a = profile.get(i);
            final Vec2 b = profile.get((i + 1) % profile.size());
            // out of the profile: across nv, up ny
            final double nv = (b.z() - a.z()) * turn;
            final double ny = -(b.x() - a.x()) * turn;
            final double g = nv * (v0 - a.x()) + ny * (y0 - a.z());
            final double rate = nv * dv + ny * dy;
            if (Math.abs(rate) < 1e-12) {
                if (g > 0) {
                    return null;
                }
                continue;
            }
            final double t = -g / rate;
            if (rate < 0) {
                if (t > enter) {
                    enter = t;
                    final double length = Math.hypot(nv, ny);
                    normal = right.multiply(nv / length).add(new Vec3(0, -ny / length, 0));
                }
            } else {
                exit = Math.min(exit, t);
            }
        }
        if (enter > exit || normal == null) {
            return null;
        }
        return new Span(enter, exit, normal);
    }

    /** A ray's way through a solid: when it goes in, when it comes out, and the way out of where it went in. */
    record Span(double enter, double exit, Vec3 normal) {
    }

    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final Span span = span(from, direction);
        if (span == null || span.enter() < 0 || span.enter() > reach) {
            return null;
        }
        return new Hit(from.add(direction.multiply(span.enter())), span.normal(), Surface.BLOCK, span.enter());
    }
}
