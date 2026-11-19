package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.PlayWorld;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.StandIns;
import com.example.fxsuite.avatar.build.Town;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Terrain;

import javafx.scene.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * A scene, as built ({@link Built}), as a world to play ({@link PlayWorld}): on level ground ruled every 10 m, its frame
 * a building in the way as its solids are, what moves in it each a piece going its own way, each drawn by its key; who
 * comes put where it says.
 */
public final class SceneWorld implements PlayWorld {

    /** The first key a scene's parts are drawn by: its frame's. */
    public static final int KEY = 970_000;
    /** How far apart people stand in a row; the room between vehicles parked. */
    public static final double PEOPLE_APART = 150;
    public static final double ROOM = 200;

    private final Built built;

    public SceneWorld(final Built built) {
        this.built = built;
    }

    @Override
    public String title() {
        return built.title();
    }

    @Override
    public String description() {
        return built.figures();
    }

    @Override
    public void lay(final Course.Layout world) {
        // its frame's key, then each kind of moving part's, then each pane's, drawn about its own foot
        final int keyed = built.keyed().size();
        Sites.register(KEY, KEY + keyed + built.panes().size(),
                k -> k < keyed ? built.keyed().get(k) : StandIns.about(built.panes().get(k - keyed)));
        final double reach = Math.ceil(built.ground() / 1_000) * 1_000;
        final Terrain flat = new Terrain("lifts scene", -reach, -reach, reach, reach, 1_000, 0, 0, Terrain.Noise.NONE,
                List.of(), List.of());
        final List<Course.Piece> pieces = new ArrayList<>();
        pieces.add(Town.building(built.drawn(), new Place(0, 0, 0, 0), KEY, StandIns.prisms(built.standing()), 1));
        built.movers().forEach(m -> pieces.add(m.piece()));
        for (int p = 0; p < built.panes().size(); p++) {
            pieces.add(StandIns.pane(built.panes().get(p), KEY + keyed + p));
        }
        world.land(new Land(flat.survey(), List.of())).ruled(true).pieces(pieces);
    }

    @Override
    public Scenery scenery() {
        return Scenery.still(new Group());
    }

    @Override
    public Spot start() {
        return built.start();
    }

    /** In a row across from where the scene parks its first, nose to tail with room between, facing across. */
    @Override
    public List<Spot> parked(final List<Double> lengths) {
        final Place p = built.parking();
        final List<Spot> spots = new ArrayList<>();
        double at = 0;
        for (int k = 0; k < lengths.size(); k++) {
            if (k > 0) {
                at += (lengths.get(k - 1) + lengths.get(k)) / 2 + ROOM;
            }
            spots.add(new Spot("parked " + (k + 1), new Place(p.x() + at, p.z(), p.height(), 90)));
        }
        return spots;
    }

    /** In a row across from where the scene stands its first, facing as it says. */
    @Override
    public List<Spot> standing(final int count) {
        final Place p = built.people();
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            spots.add(new Spot("standing " + (k + 1), new Place(p.x() + k * PEOPLE_APART, p.z(), p.height(), p.yaw())));
        }
        return spots;
    }
}
