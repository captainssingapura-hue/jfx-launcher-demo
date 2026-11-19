package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.build.StandIns;
import com.example.fxsuite.avatar.build.Town;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Staged;

import java.util.List;

/**
 * An office tower standing in a world: its middle {@code at} a place, turned as that place faces (its plan's along
 * toward the place's yaw), its foot at its height; drawn by keys from {@code key} on, as many as it needs, which no other
 * thing may draw by. One piece of the world, as finely as the focus wants it ({@link #pieces}).
 *
 * @param tower what it is
 * @param at    where its middle stands, which way it faces
 * @param key   the first key it is drawn by
 */
record Standing(OfficeTower tower, Place at, int key) {

    /** How many keys a tower may take: its frame, its moving parts' kinds, its panes. */
    static final int KEYS = 5_000;

    /** It as built, whole, in its own frame. */
    Built built() {
        return tower.built(tower.storeys(), true);
    }

    /** What it is drawn by: told to the drawing, each key what it draws; its frame with its landings' doors shut. */
    void register(final Built built) {
        final int keyed = built.keyed().size();
        if (keyed + built.panes().size() > KEYS) {
            throw new IllegalStateException(tower.name() + ": more to draw than its keys");
        }
        final List<Solid> frame = new java.util.ArrayList<>(built.keyed().getFirst());
        frame.addAll(tower.landingsShut());
        Sites.register(key, key + keyed + built.panes().size(),
                k -> k == 0 ? frame : k < keyed ? built.keyed().get(k) : built.panes().get(k - keyed));
    }

    /**
     * It as pieces of the world, staged ({@link Staged}): one piece, its coarse shape ({@link OfficeTower#coarse}) and
     * its fine model (its frame's parts, its landings' doors standing shut, its panes each breaking on its own) as one
     * volume, its storeys its cells, as finely as the focus wants it. Its lifts' cars no pieces of the world: they
     * belong to its fine model, to come; till then its lifts stand shut. {@link #register} tells how it is drawn.
     */
    List<Course.Piece> pieces(final Built built) {
        final List<Solid> frame = new java.util.ArrayList<>(built.standing());
        frame.addAll(tower.landingsShut());
        final Staged staged = new Staged(tower.coarse(),
                StandIns.assembly(frame, built.panes(), Damage.GLASS, key + built.keyed().size()), tower.cells());
        return List.of(Town.building(built.drawn(), at, key, List.of(staged), 1));
    }

    /** A spot in its own frame, where it stands. */
    Spot placed(final Spot spot) {
        return new Spot(spot.name(), placed(spot.place()));
    }

    /** A place in its own frame (its middle across and along, its height, which way it faces), where it stands. */
    Place placed(final Place p) {
        final double[] w = world(p.x(), p.z());
        return new Place(w[0], w[1], p.height() + at.height(), p.yaw() + at.yaw());
    }

    /** Solids in its own frame, where it stands. */
    List<Solid> placed(final List<Solid> solids) {
        return solids.stream().map(s -> new Solid(s.name(), s.role(), s.material(), s.corners().stream().map(p -> {
            final double[] w = world(p.across(), p.along());
            return new Point(w[0], w[1], p.up() + at.height());
        }).toList())).toList();
    }

    /** Where ({@code across}, {@code along}) in its own frame is in the world, as the game turns a thing. */
    double[] world(final double across, final double along) {
        final double r = Math.toRadians(at.yaw());
        final double c = Math.cos(r);
        final double s = Math.sin(r);
        return new double[] {at.x() + c * across + s * along, at.z() - s * across + c * along};
    }
}
