package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Assembly;
import com.example.fxsuite.avatar.sim.Outline;
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
     * A building as one thing in the world's way ({@link Assembly}): its {@code frame}'s solids standing for it as
     * {@link #prisms} has them, and each of its {@code panes} (each a pane's solids) a part of it that breaks on its
     * own, able to take {@code toughness}, drawn by its own key from {@code drawFrom} on, in order. In its own frame.
     */
    public static Assembly assembly(final List<Solid> frame, final List<List<Solid>> panes, final double toughness,
                                    final int drawFrom) {
        final List<Volume> breakable = new ArrayList<>(panes.size());
        for (final List<Solid> pane : panes) {
            final List<Volume> stood = prisms(pane);
            if (stood.size() != 1) {
                throw new IllegalArgumentException("a pane one solid, thick enough to stand for, not " + pane);
            }
            breakable.add(stood.getFirst());
        }
        return new Assembly(prisms(frame), breakable, toughness, drawFrom);
    }
}
