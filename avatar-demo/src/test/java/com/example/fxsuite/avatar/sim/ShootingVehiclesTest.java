package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The robot at the start of the roads demo, the hand-held Gatling gun drawn, turned on each of the
 * three vehicles in turn and firing until it breaks: each takes as many rounds as its body's armour
 * and plating make it, the roadster fewest and the pickup most, and each is gone once broken.
 */
class ShootingVehiclesTest {

    /** Where the Gatling gun's rounds leave from, up from the feet: at the hip. */
    private static final double HIP = Arms.GATLING_HEIGHT;

    private record Shot(int rounds, Play play) {
    }

    private static int vehicle(final Play play, final Course.Bodywork body) {
        for (int i = 0; i < play.damage().size(); i++) {
            if (play.damage().piece(i).kind() == Course.Kind.CAR && play.damage().piece(i).body() == body) {
                return i;
            }
        }
        throw new AssertionError("no vehicle wearing " + body);
    }

    /**
     * {@code play}, the robot turned where it stands to face the middle of vehicle {@code piece}'s bed
     * and aimed at it, then firing until it breaks (or a minute is up): how many rounds struck it.
     */
    private static Shot shoot(Play play, final int piece) {
        final Course.Piece what = play.damage().piece(piece);
        final Lift lift = play.lifts().stream().filter(l -> l.piece() == piece).findFirst().orElseThrow();
        final Course.Chassis chassis = (Course.Chassis) lift.craft();
        final Vec3 at = play.avatar().body().position();
        final double dx = lift.place().x() - at.x();
        final double dz = lift.place().z() - at.z();
        final double yaw = Math.toDegrees(Math.atan2(dx, dz));
        // at the middle of the bed's side, from the hip
        final double bed = lift.place().height() - Course.Chassis.BED / 2;
        final double aim = Math.toDegrees(Math.atan2(bed - HIP, Math.hypot(dx, dz) - chassis.width() / 2));
        play = new Play(new Avatar(Body.standing(at, yaw), false).drawn(Weapon.GATLING), play.damage(), play.sky(),
                play.lifts(), play.people(), play.props(), play.turrets());
        final Controls fire = new Controls(0, 0, 0, 0, true, true, aim);
        int rounds = 0;
        for (int i = 0; i < 120 * 60 && !play.damage().broken(piece); i++) {
            final Play.Stepped stepped = play.step(fire, Clock.STEP);
            play = stepped.play();
            for (final Event event : stepped.events()) {
                if (event instanceof Event.Struck s && s.piece() == piece) {
                    rounds++;
                }
            }
        }
        assertTrue(play.damage().broken(piece), what.body().name() + " broken");
        return new Shot(rounds, play);
    }

    /** How many rounds of the hand-held Gatling gun a vehicle wearing {@code body} should take. */
    private static int expected(final Course.Bodywork body) {
        return (int) Math.ceil(Damage.CAR * body.armour() / (Weapon.GATLING.blow() - body.plating()));
    }

    @Test
    void theRobotBreaksAllThreeEachTakingAsMuchAsItsBodyMakesIt() {
        Play play = Play.start(Demo.FLYOVER, false);
        final List<Integer> taken = new ArrayList<>();
        for (final Course.Bodywork body : List.of(Course.ROADSTER, Course.TRUCK_CAB, Course.PICKUP_BODY)) {
            final int piece = vehicle(play, body);
            final Shot shot = shoot(play, piece);
            play = shot.play();
            assertEquals(expected(body), shot.rounds(), body.name());
            taken.add(shot.rounds());
            // gone from the world: rounds fly on through where it stood
            assertFalse(play.obstacles().pieces().contains(piece), body.name() + " gone");
        }
        assertTrue(taken.get(0) < taken.get(1) && taken.get(1) < taken.get(2),
                "the roadster fewest, the pickup most: " + taken);
        // and every one of them more than a bare chassis takes
        final int bare = (int) Math.ceil(Damage.CAR / Weapon.GATLING.blow());
        assertTrue(taken.stream().allMatch(n -> n > bare), "more than a bare chassis's " + bare);
    }

    @Test
    void aBrokenVehicleIsLetGoAndCannotBeTaken() {
        Play play = Play.start(Demo.FLYOVER, false);
        final int pickup = vehicle(play, Course.PICKUP_BODY);
        play = shoot(play, pickup).play();
        final Lift lift = play.lifts().stream().filter(l -> l.piece() == pickup).findFirst().orElseThrow();
        final Play beside = new Play(new Avatar(Body.standing(lift.pilot(), 0), false), play.damage(), play.sky(),
                play.lifts(), play.people(), play.props(), play.turrets());
        assertTrue(beside.withinReach().isEmpty());
        assertTrue(beside.gunWithinReach().isEmpty(), "nor its gun manned");
    }
}
