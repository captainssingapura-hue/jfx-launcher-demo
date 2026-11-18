package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.GridInside;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Lean;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Placed;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * The structures roads are carried on, in the way: each one piece, its inside its parts ({@link Interior}). Along each
 * of its segments, its deck, its top the pavement's along the road's line, from its paved strips' one edge to the
 * other, {@link Roadway#DECK} deep; a parapet at each edge; at each of its piers, a crosshead and its columns down to
 * the ground. Drawn by its key as concrete: its deck down from the formation, under the pavement the roads draw
 * themselves; its parapets; its piers.
 */
public final class Decks {

    /** How wide the cells a deck's parts are filed in. */
    static final double CELL = 1_000;
    /** A pier's crosshead's depth and length along; a column's side; how far a column goes into the ground. */
    static final double CROSSHEAD = 100;
    static final double CROSSHEAD_ALONG = 160;
    static final double COLUMN = 120;
    static final double FOOTING = 100;
    /** How many keys are kept for the decks: from the first asked for. */
    static final int KEYS = 1_000;

    private Decks() {
    }

    /** The structures {@code works}' roads are carried on, each one piece, its key from {@code keys} on, drawn so. */
    public static List<Course.Piece> pieces(final Roadworks works, final int keys) {
        final List<List<Solid>> drawn = new ArrayList<>();
        Sites.register(keys, keys + KEYS, k -> k < drawn.size() ? drawn.get(k) : List.of());
        final Ground ground = works.ground();
        final List<Course.Piece> pieces = new ArrayList<>();
        for (final Roadway road : works.roads()) {
            for (final Roadway.Structure s : road.structures()) {
                if (s.stretch().carrier() == Carrier.STRUCTURE && pieces.size() < KEYS) {
                    final List<Solid> solids = new ArrayList<>();
                    pieces.add(piece(road, s, ground, keys + pieces.size(), solids));
                    drawn.add(solids);
                }
            }
        }
        return pieces;
    }

    private static Course.Piece piece(final Roadway road, final Roadway.Structure s, final Ground ground, final int key,
                                      final List<Solid> solids) {
        final List<Lean> parts = new ArrayList<>();
        final Section section = road.section();
        // what is drawn of each part: as it is, but the deck, drawn down from the formation, under the pavement
        final java.util.Map<Lean, Double> decks = new java.util.HashMap<>();
        final double paved = section.paved().stream().mapToDouble(l -> l.strip().depth()).max().orElse(0);
        final double west = -section.pavedEdge(-1);
        final double east = section.pavedEdge(1);
        final double t = Roadway.PARAPET_THICK;
        for (int j = s.stretch().from(); j < s.stretch().to(); j++) {
            final Vec2 a = road.point(j);
            final Vec2 b = road.point(j + 1);
            final double len = Math.hypot(b.x() - a.x(), b.z() - a.z());
            final double yaw = Math.toDegrees(Math.atan2(b.x() - a.x(), b.z() - a.z()));
            final double top = (road.level(j) + road.level(j + 1)) / 2;
            // a little longer than its segment, so where the road bends there is no gap between one and the next
            final double reach = len / 2 + COLUMN / 2;
            final double mx = (a.x() + b.x()) / 2;
            final double mz = (a.z() + b.z()) / 2;
            final Lean deck = new Lean(mx, mz, yaw, -reach, reach, west, east, west, east, top - Roadway.DECK, top);
            parts.add(deck);
            decks.put(deck, top - paved);
            parts.add(new Lean(mx, mz, yaw, -reach, reach, west - t, west, west - t, west, top - Roadway.DECK,
                    top + Roadway.PARAPET));
            parts.add(new Lean(mx, mz, yaw, -reach, reach, east, east + t, east, east + t, top - Roadway.DECK,
                    top + Roadway.PARAPET));
        }
        for (final int i : s.piers()) {
            final Vec2 p = road.point(i);
            final Vec2 r = road.right(i);
            final double yaw = Math.toDegrees(Math.atan2(-r.z(), r.x()));
            final double under = road.level(i) - Roadway.DECK;
            final double middle = (west + east) / 2;
            final double half = (east - west) / 2 * 0.9;
            parts.add(new Lean(p.x(), p.z(), yaw, -CROSSHEAD_ALONG / 2, CROSSHEAD_ALONG / 2, middle - half, middle + half,
                    middle - half, middle + half, under - CROSSHEAD, under));
            final double[] columns = 2 * half > 1_200 ? new double[] {middle - half / 2, middle + half / 2}
                    : new double[] {middle};
            for (final double c : columns) {
                final Vec2 at = road.at(i, c);
                final double foot = Math.min(ground.at(at.x(), at.z()).height() - FOOTING, under - CROSSHEAD);
                parts.add(new Lean(p.x(), p.z(), yaw, -COLUMN / 2, COLUMN / 2, c - COLUMN / 2, c + COLUMN / 2,
                        c - COLUMN / 2, c + COLUMN / 2, foot, under - CROSSHEAD));
            }
        }
        final Box box = bounds(parts);
        final double cx = (box.minX() + box.maxX()) / 2;
        final double cz = (box.minZ() + box.maxZ()) / 2;
        for (final Lean part : parts) {
            solids.add(solid(part, decks.getOrDefault(part, part.high()), cx, cz, box.low()));
        }
        final Interior inside = new Interior(new Placed(new Place(0, 0, 0, 0), new GridInside(box, CELL, CELL,
                Math.max(1, box.high() - box.low()))), parts);
        final Block block = new Block(cx, cz, (box.maxX() - box.minX()) / 2, (box.maxZ() - box.minZ()) / 2, 0,
                box.low(), box.high());
        return Course.Piece.shaped(block, Course.Kind.BUILDING, key, List.of(), List.of(inside),
                new Place(cx, cz, box.low(), 0));
    }

    /**
     * {@code part} as a concrete solid, its top {@code high}, about ({@code cx}, {@code cz}) and up from {@code low}: as a
     * building is drawn.
     */
    private static Solid solid(final Lean part, final double high, final double cx, final double cz, final double low) {
        final double r = Math.toRadians(part.yaw());
        final double ax = Math.sin(r);
        final double az = Math.cos(r);
        final List<Point> corners = new ArrayList<>(8);
        for (int bit = 0; bit < 8; bit++) {
            final boolean top = (bit & 4) != 0;
            final double across = (bit & 1) == 0 ? (top ? part.westHigh() : part.westLow())
                    : (top ? part.eastHigh() : part.eastLow());
            final double along = (bit & 2) == 0 ? part.along0() : part.along1();
            final double x = part.x() + az * across + ax * along;
            final double z = part.z() - ax * across + az * along;
            corners.add(new Point(x - cx, z - cz, (top ? high : part.low()) - low));
        }
        return new Solid("deck", Role.STRUCTURE, Material.CONCRETE, corners);
    }

    /** The box round {@code parts}. */
    private static Box bounds(final List<Lean> parts) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (final Lean l : parts) {
            final Box b = l.placed();
            minX = Math.min(minX, b.minX());
            maxX = Math.max(maxX, b.maxX());
            minZ = Math.min(minZ, b.minZ());
            maxZ = Math.max(maxZ, b.maxZ());
            low = Math.min(low, b.low());
            high = Math.max(high, b.high());
        }
        return new Box(minX, maxX, minZ, maxZ, low, high);
    }
}
