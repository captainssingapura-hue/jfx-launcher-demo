package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * An outline across the ground ({@code outline}, its points' x and z in the world), stood straight
 * up from {@code bottom} to {@code top}: a round tower's shaft, a wall's run, a pyramid's step. Any
 * outline that does not cross itself, convex or not; its top flat. With {@code holes} cut down
 * through all of it, if it has any (each inside its outline, apart from the others): a pool sunk in
 * a roof, a courtyard, a light well.
 */
public record Prism(List<Vec2> outline, List<List<Vec2>> holes, double bottom, double top) implements Volume {

    public Prism {
        outline = List.copyOf(outline);
        holes = holes.stream().map(List::copyOf).toList();
        if (outline.size() < 3 || Math.abs(Outline.twiceArea(outline)) < 1e-6 || !(top > bottom)) {
            throw new IllegalArgumentException("not a prism: " + outline.size() + " points, " + bottom + " to " + top);
        }
        for (final List<Vec2> hole : holes) {
            if (hole.size() < 3 || Math.abs(Outline.twiceArea(hole)) < 1e-6) {
                throw new IllegalArgumentException("not a hole: " + hole);
            }
        }
    }

    /** Solid all through: no holes. */
    public Prism(final List<Vec2> outline, final double bottom, final double top) {
        this(outline, List.of(), bottom, top);
    }

    /** Its outline and its holes, the outline first. */
    private List<List<Vec2>> rings() {
        if (holes.isEmpty()) {
            return List.of(outline);
        }
        final List<List<Vec2>> rings = new ArrayList<>(List.of(outline));
        rings.addAll(holes);
        return rings;
    }

    @Override
    public double low() {
        return bottom;
    }

    @Override
    public double high() {
        return top;
    }

    /** Whether a circle at ({@code x}, {@code z}) is anywhere near it: its outline's box, grown by the radius. */
    private boolean near(final double x, final double z, final double radius) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final Vec2 p : outline) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        return x > minX - radius && x < maxX + radius && z > minZ - radius && z < maxZ + radius;
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        return near(x, z, radius) && Outline.depthOf(rings(), x, z, radius) > 0;
    }

    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        return top <= below && covers(x, z, radius) ? top : Double.NEGATIVE_INFINITY;
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        return bottom >= above && covers(x, z, radius) ? bottom : Double.POSITIVE_INFINITY;
    }

    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        if (top <= feet || bottom >= head || !near(x, z, radius)) {
            return null;
        }
        return Outline.pushOut(rings(), x, z, radius);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        if (top <= feet || bottom >= head
                || !near((ax + bx) / 2, (az + bz) / 2, radius + Math.hypot(bx - ax, bz - az) / 2)) {
            return null;
        }
        return Outline.pushOut(rings(), ax, az, bx, bz, radius);
    }

    /**
     * Where a ray first meets it: through one of its sides (its outline's, facing out; a hole's,
     * facing into the hole), or down onto its top, or up into its underside; {@code null} if it
     * misses, or starts inside.
     */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final double up0 = -from.y();
        final double rise = -direction.y();
        final List<List<Vec2>> rings = rings();
        if (up0 >= bottom && up0 <= top && Outline.solid(rings, from.x(), from.z())) {
            return null;
        }
        double best = Double.POSITIVE_INFINITY;
        Vec3 normal = null;
        // its top and its underside
        if (Math.abs(rise) > 1e-12) {
            for (final double level : new double[] {top, bottom}) {
                final double t = (level - up0) / rise;
                if (t >= 0 && t < best && t <= reach) {
                    final double px = from.x() + direction.x() * t;
                    final double pz = from.z() + direction.z() * t;
                    if (Outline.solid(rings, px, pz)) {
                        best = t;
                        normal = new Vec3(0, level == top ? -1 : 1, 0);
                    }
                }
            }
        }
        // its sides: each edge of its outline and of its holes, stood up between its bottom and its top
        for (int r = 0; r < rings.size(); r++) {
            final List<Vec2> ring = rings.get(r);
            // out of the solid: out of its outline; into a hole
            final double turn = Math.signum(Outline.twiceArea(ring)) * (r == 0 ? 1 : -1);
            for (int i = 0; i < ring.size(); i++) {
                final Vec2 a = ring.get(i);
                final Vec2 b = ring.get((i + 1) % ring.size());
                final double ex = b.x() - a.x();
                final double ez = b.z() - a.z();
                final double det = direction.x() * ez - direction.z() * ex;
                if (Math.abs(det) < 1e-12) {
                    continue;
                }
                final double t = ((a.x() - from.x()) * ez - (a.z() - from.z()) * ex) / det;
                final double s = ((a.x() - from.x()) * direction.z() - (a.z() - from.z()) * direction.x()) / det;
                if (t < 0 || t >= best || t > reach || s < 0 || s > 1) {
                    continue;
                }
                final double up = up0 + rise * t;
                if (up < bottom || up > top) {
                    continue;
                }
                final double length = Math.hypot(ex, ez);
                final Vec3 out = new Vec3(ez / length * turn + 0.0, 0, -ex / length * turn + 0.0);
                // only through its outside: going in, against the way it faces
                if (out.x() * direction.x() + out.z() * direction.z() < 0) {
                    best = t;
                    normal = out;
                }
            }
        }
        return normal == null ? null : new Hit(from.add(direction.multiply(best)), normal, Surface.BLOCK, best);
    }

    /** Whether it and {@code block} share any space: their heights cross, and the block is on some of its solid. */
    @Override
    public boolean overlaps(final Block block) {
        if (bottom >= block.top() || top <= block.bottom()) {
            return false;
        }
        final List<Vec2> box = Outline.corners(block);
        final List<List<Vec2>> rings = rings();
        // an edge of it crosses the block's, or a corner of the block is on its solid, or a point of it is in the block
        for (final List<Vec2> ring : rings) {
            if (Outline.intersects(ring, box) && crosses(ring, box)) {
                return true;
            }
        }
        if (box.stream().anyMatch(c -> Outline.solid(rings, c.x(), c.z()))) {
            return true;
        }
        return Outline.inside(box, outline.getFirst().x(), outline.getFirst().z());
    }

    /** Whether any edge of {@code a} crosses any edge of {@code b}. */
    private static boolean crosses(final List<Vec2> a, final List<Vec2> b) {
        for (int i = 0; i < a.size(); i++) {
            final Vec2 p = a.get(i);
            final Vec2 q = a.get((i + 1) % a.size());
            for (int j = 0; j < b.size(); j++) {
                final Vec2 r = b.get(j);
                final Vec2 s = b.get((j + 1) % b.size());
                final double ex = q.x() - p.x();
                final double ez = q.z() - p.z();
                final double fx = s.x() - r.x();
                final double fz = s.z() - r.z();
                final double det = ex * fz - ez * fx;
                if (Math.abs(det) < 1e-12) {
                    continue;
                }
                final double t = ((r.x() - p.x()) * fz - (r.z() - p.z()) * fx) / det;
                final double u = ((r.x() - p.x()) * ez - (r.z() - p.z()) * ex) / det;
                if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
