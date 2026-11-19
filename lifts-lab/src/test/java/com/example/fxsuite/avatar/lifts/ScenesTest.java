package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Play;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lifts bench's scenes: each built as its knobs are, and at each knob's least and most at once, its moving parts
 * each drawn by a key it draws; laid as a world, its pieces all there; the avatar stood at its start a second, on the
 * ground still. A shuttle lift's floors no nearer than its car is high, its doorway no wider than half its car, however
 * they are set.
 */
class ScenesTest {

    /** Each knob of {@code scene} at its least ({@code most} false) or its most. */
    private static Map<String, Double> all(final LiftScene scene, final boolean most) {
        final Map<String, Double> set = new HashMap<>();
        scene.knobs().forEach(k -> set.put(k.key(), most ? k.most() : k.least()));
        return set;
    }

    @Test
    void eachSceneIsBuiltAsSetEvenAtItsKnobsEnds() {
        for (final LiftScene scene : LiftScene.all()) {
            for (final Map<String, Double> set : List.of(Map.<String, Double>of(), all(scene, false), all(scene, true))) {
                final Built built = scene.built(set);
                assertFalse(built.movers().isEmpty(), scene.name() + ": something moves");
                for (final Mover m : built.movers()) {
                    final int key = m.key() - SceneWorld.KEY;
                    assertTrue(key > 0 && key < built.keyed().size(), scene.name() + ": " + m.name() + " drawn");
                }
                final Course course = new Course(new Played(new SceneWorld(built), Played.Guests.of(List.of())));
                assertEquals(1 + built.movers().size(), course.pieces().size(), scene.name() + ": its frame and movers");
            }
        }
    }

    @Test
    void theAvatarStoodAtEachScenesStartIsOnTheGroundStill() {
        for (final LiftScene scene : LiftScene.all()) {
            Play play = Play.start(new Played(new SceneWorld(scene.built()), Played.Guests.of(List.of())), false);
            for (int n = 0; n < 120; n++) {
                play = play.step(Controls.NONE, Clock.STEP).play();
            }
            assertEquals(0, play.avatar().body().height(), 1, scene.name());
        }
    }

    @Test
    void aShuttleLiftsFloorsAndDoorwayKeepToWhatItsCarNeeds() {
        final ShuttleScene scene = new ShuttleScene();
        final Built tight = scene.built(Map.of("storey", 3.0, "height", 3.0, "width", 1.2, "doorway", 1.3));
        assertTrue(tight.figures().contains("as near as its car lets them"), tight.figures());
        assertTrue(tight.figures().contains("half its car"), tight.figures());
    }
}
