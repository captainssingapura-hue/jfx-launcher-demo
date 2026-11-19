package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.PlayWorld;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.MarinaBay;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.look.RoadScenery;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;

/**
 * Marina Bay as it is laid ({@link MarinaBay}), its land, its water, its roads and what stands on it, with office
 * towers standing in it ({@link Standing}), each as built ({@code built}, in the same order): walked into, broken into,
 * ridden up. The avatar starts before the way in of the one {@code first} says; vehicles parked and people standing
 * before it, in rows along its front.
 *
 * @param towers where each tower stands
 * @param built  each tower as built, in its own frame
 * @param first  which tower the avatar starts at
 */
record BayWorld(List<Standing> towers, List<Built> built, int first) implements PlayWorld {

    /** How far apart people stand in a row; the room between vehicles parked. */
    static final double PEOPLE_APART = 150;
    static final double ROOM = 200;

    BayWorld {
        towers = List.copyOf(towers);
        built = List.copyOf(built);
        if (towers.size() != built.size() || first < 0 || first >= towers.size()) {
            throw new IllegalArgumentException("each tower as built, and one of them to start at");
        }
    }

    @Override
    public String title() {
        return "Marina Bay, its office towers";
    }

    @Override
    public String description() {
        return "The bay as it lies, its office towers standing in it: "
                + String.join(", ", towers.stream().map(t -> t.tower().name()).toList());
    }

    @Override
    public void lay(final Course.Layout world) {
        final List<Course.Piece> pieces = new ArrayList<>(MarinaBay.pieces());
        for (int k = 0; k < towers.size(); k++) {
            towers.get(k).register(built.get(k));
            pieces.addAll(towers.get(k).pieces(built.get(k)));
        }
        world.pieces(pieces).ponds(MarinaBay.ponds()).land(MarinaBay.land()).ruled(false)
                .ways(MarinaBay.roads().roads().stream().flatMap(r -> r.pavements().stream()).map(p -> (Volume) p)
                        .toList());
    }

    @Override
    public Scenery scenery() {
        return RoadScenery.of(MarinaBay.roads());
    }

    @Override
    public Spot start() {
        return towers.get(first).placed(built.get(first).start());
    }

    /** In a row along the first tower's front from where it parks its first, nose to tail with room between. */
    @Override
    public List<Spot> parked(final List<Double> lengths) {
        final Standing t = towers.get(first);
        final Place p = built.get(first).parking();
        final List<Spot> spots = new ArrayList<>();
        double at = 0;
        for (int k = 0; k < lengths.size(); k++) {
            if (k > 0) {
                at += (lengths.get(k - 1) + lengths.get(k)) / 2 + ROOM;
            }
            spots.add(new Spot("parked " + (k + 1), t.placed(new Place(p.x() + at, p.z(), p.height(), p.yaw()))));
        }
        return spots;
    }

    /** In a row along the first tower's front, facing as it says. */
    @Override
    public List<Spot> standing(final int count) {
        final Standing t = towers.get(first);
        final Place p = built.get(first).people();
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            spots.add(new Spot("standing " + (k + 1),
                    t.placed(new Place(p.x() + k * PEOPLE_APART, p.z(), p.height(), p.yaw()))));
        }
        return spots;
    }
}
