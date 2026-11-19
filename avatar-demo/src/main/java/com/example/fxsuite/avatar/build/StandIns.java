package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Outline;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;

/**
 * What a building's solids are to the simulation, worked out from them: each solid what it covers seen from above, stood
 * straight up from its lowest to its highest ({@link Prism}). So its walls, floors and glass are in the way and stood
 * on, and the gaps between them (a doorway, a stair's well) are open. A sloped solid, a roof, is as solid as its
 * highest; a solid with a hole in it, solid across its hole: such a one is made of solids round the hole, if it is to
 * be gone through.
 *
 * <p>In its frame: across as x, along as z, up as up; placed where it stands as any building's shape is.
 */
public final class StandIns {

    private StandIns() {
    }

    /** Each of {@code solids}, what it covers from above, from its lowest to its highest; none too thin to stand for. */
    public static List<Volume> prisms(final List<Solid> solids) {
        final List<Volume> shape = new ArrayList<>();
        for (final Solid s : solids) {
            double low = Double.POSITIVE_INFINITY;
            double high = Double.NEGATIVE_INFINITY;
            final List<Vec2> seen = new ArrayList<>();
            for (final Point p : s.corners()) {
                seen.add(new Vec2(p.across(), p.along()));
                low = Math.min(low, p.up());
                high = Math.max(high, p.up());
            }
            final List<Vec2> outline = Outline.hull(seen);
            if (outline.size() >= 3 && Math.abs(Outline.twiceArea(outline)) > 1 && high > low) {
                shape.add(new Prism(outline, low, high));
            }
        }
        return shape;
    }

    /**
     * A pane of a building's glass, {@code solids} as the building stands at the world's middle, as a piece of its own
     * ({@link Course.Kind#GLASS}): in the way as they stand, worn down and broken on its own, drawn by {@code key} as
     * {@link #about} has them.
     */
    public static Course.Piece pane(final List<Solid> solids, final int key) {
        final double[] box = box(solids);
        final double x = (box[0] + box[1]) / 2;
        final double z = (box[2] + box[3]) / 2;
        final Block block = new Block(x, z, Math.max(1, (box[1] - box[0]) / 2), Math.max(1, (box[3] - box[2]) / 2), 0,
                box[4], box[5]);
        return Course.Piece.shaped(block, Course.Kind.GLASS, key, List.of(), prisms(solids), new Place(x, z, box[4], 0));
    }

    /** {@code solids} about their middle at their foot, as a piece made of them ({@link #pane}) is drawn. */
    public static List<Solid> about(final List<Solid> solids) {
        final double[] box = box(solids);
        final Point back = new Point(-(box[0] + box[1]) / 2, -(box[2] + box[3]) / 2, -box[4]);
        return solids.stream().map(s -> new Solid(s.name(), s.role(), s.material(),
                s.corners().stream().map(p -> p.plus(back)).toList())).toList();
    }

    /** The box round {@code solids}: least and most across, along, up. */
    private static double[] box(final List<Solid> solids) {
        final double[] box = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (final Solid s : solids) {
            for (final Point p : s.corners()) {
                box[0] = Math.min(box[0], p.across());
                box[1] = Math.max(box[1], p.across());
                box[2] = Math.min(box[2], p.along());
                box[3] = Math.max(box[3], p.along());
                box[4] = Math.min(box[4], p.up());
                box[5] = Math.max(box[5], p.up());
            }
        }
        return box;
    }
}
