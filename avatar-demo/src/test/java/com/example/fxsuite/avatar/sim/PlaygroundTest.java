package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The playground, with everything in it: besides the course, its targets, airships, stall and
 * flyovers, the four vehicles parked either side of the street, the bunker, a mounted gun on each pillar, and an
 * airship moored to be flown. Each in the open, clear of everything else, and each to be used.
 */
class PlaygroundTest {

    private final Play start = Play.start(false);
    private final List<Course.Piece> pieces = java.util.stream.IntStream.range(0, start.damage().size())
            .mapToObj(i -> start.damage().piece(i)).toList();

    @Test
    void theVehiclesTheBunkerTheMountedGunsAndAnAirshipToFlyAreAllThere() {
        // the cars by their bodies, the two-wheelers by their chassis
        final Set<String> vehicles = start.lifts().stream().filter(l -> l.craft() instanceof Course.Chassis)
                .map(l -> pieces.get(l.piece()).body() != null ? pieces.get(l.piece()).body().name()
                        : ((Course.Chassis) l.craft()).name())
                .collect(Collectors.toSet());
        assertEquals(Set.of("roadster", "van", "pickup", "truck", "scooter", "sport bike", "cruiser", "boxer"),
                vehicles);
        assertEquals(1, pieces.stream().filter(p -> p.kind() == Course.Kind.BUNKER).count());
        assertEquals(pieces.stream().filter(p -> p.kind() == Course.Kind.PILLAR).count(),
                start.turrets().stream().filter(t -> t.mount() == Course.MOUNTED_GUN).count(), "one on each pillar");
        assertTrue(start.lifts().stream().anyMatch(l -> l.craft() instanceof Course.Ship), "an airship to fly");
        assertEquals(Set.of(Course.BUNKER_GUN, Course.MOUNTED_GUN, Course.CAR_GUN, Course.REAR_GUN, Course.AIRSHIP_GUN),
                start.turrets().stream().map(Turret::mount).collect(Collectors.toSet()), "a gun on each that has one");
    }

    @Test
    void eachOfThemStandsClearOfEverythingElse() {
        for (final Lift lift : start.lifts()) {
            clear(lift.piece());
        }
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).kind() == Course.Kind.BUNKER) {
                clear(i);
            }
        }
    }

    private void clear(final int piece) {
        final Block box = pieces.get(piece).block();
        for (int i = 0; i < pieces.size(); i++) {
            final int other = i;
            final boolean aboard = start.damage().riding(piece).contains(i)
                    || start.props().stream().anyMatch(p -> p.piece() == other);
            if (i != piece && !aboard) {
                assertFalse(box.overlaps(pieces.get(i).block()), pieces.get(piece).kind() + " " + piece
                        + " overlaps " + pieces.get(i).kind() + " " + i);
            }
        }
    }

    /** Sat at each vehicle's wheel and taken, it drives off: none of them hemmed in where it is parked. */
    @Test
    void eachVehicleIsDrivenOffFromWhereItIsParked() {
        for (final Lift parked : start.lifts()) {
            if (!(parked.craft() instanceof Course.Chassis)) {
                continue;
            }
            Play play = new Play(new Avatar(Body.standing(parked.pilot(), 0), false), start.damage(), start.sky(),
                    start.lifts(), start.people(), start.props(), start.turrets());
            assertTrue(play.withinReach().isPresent() || play.gunWithinReach().isPresent(), "its wheel in reach");
            play = play.step(Controls.doing(Action.TAKE), Clock.STEP).play();
            for (int step = 0; step < 120; step++) {
                play = play.step(new Controls(0.6, 0, 0, 0, false, false, 0), Clock.STEP).play();
            }
            final Lift driven = play.lifts().stream().filter(l -> l.piece() == parked.piece()).findFirst().orElseThrow();
            assertTrue(driven.held(), "taken");
            assertTrue(driven.place().z() - parked.place().z() > 50, "and off: " + driven.place());
        }
    }
}
