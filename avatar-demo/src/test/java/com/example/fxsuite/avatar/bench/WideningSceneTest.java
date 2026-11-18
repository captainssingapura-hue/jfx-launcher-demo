package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.look.RoadworkModel;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Sections;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The widening: a road a lane wider each way along its middle, its kind changing along it as set; laid and drawn, the
 * strips more where it widens drawn as at the near end of the segment they begin in.
 */
class WideningSceneTest {

    private static final RoadScenes.Scene WIDENING = RoadScenes.all().get("widening");

    @Test
    void widerAlongItsMiddleAsSetAndDrawn() {
        final Section kind = Sections.standard().get(WIDENING.section());
        for (final double more : new double[] {1, 2}) {
            final Roadworks works = Roadworks.design(WIDENING.profile(), WIDENING.roads(kind,
                    Map.of(RoadScenes.WIDER_LANES, more, RoadScenes.WIDER_TRANSITION, 60.0)));
            final Roadway road = works.roads().getFirst();
            assertEquals(kind.lanes(), road.section(0).lanes());
            assertEquals(kind.lanes() + (int) more, road.section(road.size() / 2).lanes(), "wider in its middle");
            assertEquals(kind.lanes(), road.section(road.size() - 1).lanes());
            assertFalse(RoadworkModel.pavement(road, Double.POSITIVE_INFINITY).getChildren().isEmpty());
            assertFalse(RoadworkModel.markings(road, Double.POSITIVE_INFINITY).getChildren().isEmpty());
        }
    }
}
