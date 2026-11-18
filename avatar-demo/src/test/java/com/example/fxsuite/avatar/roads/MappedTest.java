package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.RoadMap;
import com.example.fxsuite.avatar.sim.Vec2;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A road map as plans: a way's kind changing along it a part, eased over no more than the way to the next change; a
 * one-way road's line at its pavement's edge, half its paved width aside; a bridge given as a structure, held over a
 * road it crosses, not one it meets.
 */
class MappedTest {

    private static final Map<String, Section> KINDS = Sections.standard();
    private static final Profile FLAT = Profile.level(0);

    private static List<Roadworks.Plan> plans(final String text) {
        return Mapped.plans(RoadMap.parse(text), (way, kind) -> KINDS.get(kind.replace('_', ' ')), FLAT, null);
    }

    private static final String KIND_LINES = """
            kind two-lane_road width=9m surface=asphalt grade=6% curve=1/50m slope=1:2 camber=2% transition=12m vertical=60m
            kind carriageway width=9m surface=asphalt grade=5% curve=1/200m slope=1:2 camber=2% transition=12m vertical=100m
            kind town_street width=7m surface=asphalt grade=8% curve=1/20m slope=1:2 camber=2% transition=6m vertical=30m
            """;

    @Test
    void itsKindChangingAlongItAPartEasedNoLongerThanTheWayToTheNextChange() {
        final List<Roadworks.Plan> plans = plans(KIND_LINES + """
                road "main" kind=two-lane_road
                  at 0,0
                  to 0,100
                  to 0,120 kind=town_street
                  to 0,300 kind=two-lane_road
                """);
        final Roadworks.Plan main = plans.getFirst();
        assertEquals(KINDS.get("two-lane road"), main.section());
        assertEquals(2, main.parts().size());
        assertEquals(10_000, main.parts().get(0).from(), 1e-6);
        assertEquals(KINDS.get("town street"), main.parts().get(0).section());
        // the next change but 20 m on: eased over no more than that
        assertEquals(2_000, main.parts().get(0).transition(), 1e-6);
        assertEquals(12_000, main.parts().get(1).from(), 1e-6);
        assertEquals(KINDS.get("two-lane road"), main.parts().get(1).section());
        assertTrue(main.parts().get(1).transition() <= Mapped.TRANSITION);
    }

    @Test
    void aOneWayRoadsLineAtItsPavementsEdgeHalfItsPavedWidthAside() {
        final List<Roadworks.Plan> plans = plans(KIND_LINES + """
                road "one way" kind=carriageway
                  at 0,0
                  to 0,200
                """);
        final Section carriageway = KINDS.get("carriageway");
        final int side = carriageway.pavedSide();
        final Vec2 p = plans.getFirst().line().get(10);
        // going north, its right to the east: moved to its paved side's far edge, so its pavement lies on its middle
        assertEquals(-side * carriageway.pavedEdge(side) / 2, p.x(), 1e-6);
    }

    @Test
    void aBridgeGivenAsAStructureHeldOverARoadItCrossesNotOneItMeets() {
        final List<Roadworks.Plan> plans = plans(KIND_LINES + """
                node mid at=0,150
                road "over" kind=two-lane_road
                  at 0,0
                  to 0,100
                  to mid bridge
                  to 0,200
                  to 0,300 ground
                road "under" kind=two-lane_road
                  at -100,170
                  to 100,170
                road "meeting" kind=two-lane_road
                  from mid
                  to 100,150
                """);
        final Roadworks.Plan over = plans.getFirst();
        assertEquals(List.of(new Roadway.Given(10_000, 20_000, Carrier.STRUCTURE)), over.given());
        assertEquals(1, over.clearances().size(), "its two bridge segments one structure, held over the road it crosses, not the one it meets");
        final Roadway.Clearance c = over.clearances().getFirst();
        assertEquals(17_000 - Mapped.OVER, c.from(), 1e-6);
        assertEquals(17_000 + Mapped.OVER, c.to(), 1e-6);
        assertEquals(Mapped.HEADROOM + Roadway.DECK, c.over(), 1e-6);
    }
}
