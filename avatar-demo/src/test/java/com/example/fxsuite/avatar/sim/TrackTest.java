package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The race track: a NASCAR-style oval, flat all round for now, walled all round; every land
 * vehicle on pit road; and a car driven a lap round it flat out, staying on it, level all the way.
 */
class TrackTest {

    private static final double HALF = Course.TRACK_STRAIGHT / 2;
    private static final double R = Course.TRACK_RADIUS;
    private static final double MIDDLE = Course.TRACK_X - R;
    private static final double LAP = 2 * Course.TRACK_STRAIGHT + 2 * Math.PI * R;

    /** How far round the racing line ({@code s} from the start of the front straight, going north) the point nearest ({@code x}, {@code z}) is. */
    private static double along(final double x, final double z) {
        if (Math.abs(z) <= HALF) {
            return x > MIDDLE ? z + HALF : Course.TRACK_STRAIGHT + Math.PI * R + (HALF - z);
        }
        if (z > HALF) {
            return Course.TRACK_STRAIGHT + R * Math.atan2(z - HALF, x - MIDDLE);
        }
        return 2 * Course.TRACK_STRAIGHT + Math.PI * R + R * (Math.atan2(z + HALF, x - MIDDLE) + Math.PI);
    }

    /** The point on the racing line {@code s} round it. */
    private static double[] point(final double s) {
        final double at = ((s % LAP) + LAP) % LAP;
        final double l = Course.TRACK_STRAIGHT;
        if (at < l) {
            return new double[] {Course.TRACK_X, at - HALF};
        }
        if (at < l + Math.PI * R) {
            final double phi = (at - l) / R;
            return new double[] {MIDDLE + R * Math.cos(phi), HALF + R * Math.sin(phi)};
        }
        if (at < 2 * l + Math.PI * R) {
            return new double[] {MIDDLE - R, HALF - (at - l - Math.PI * R)};
        }
        final double phi = Math.PI + (at - 2 * l - Math.PI * R) / R;
        return new double[] {MIDDLE + R * Math.cos(phi), -HALF + R * Math.sin(phi)};
    }

    /** How far off the racing line ({@code x}, {@code z}) is. */
    private static double off(final double x, final double z) {
        if (Math.abs(z) <= HALF) {
            return Math.abs(x - (x > MIDDLE ? Course.TRACK_X : MIDDLE - R));
        }
        return Math.abs(Math.hypot(x - MIDDLE, z - Math.signum(z) * HALF) - R);
    }

    @Test
    void itIsAnOvalFlatAllRoundAndWalledAllRound() {
        final Course course = new Course(Demo.TRACK);
        double most = 0;
        int walls = 0;
        for (final Course.Piece piece : course.pieces()) {
            final Block b = piece.block();
            if (piece.kind() == Course.Kind.TRACK && piece.tint() == 0 && b.sloped()) {
                most = Math.max(most, Math.toDegrees(Math.atan2(b.top() - b.toe(), 2 * b.halfDepth())));
            }
            if (piece.kind() == Course.Kind.WALL && piece.tint() == 0) {
                walls++;
            }
        }
        assertEquals(Course.TRACK_BANK, most, 0.5, "flat, as banked as it is: not at all");
        assertTrue(walls > 100, "walled all round: " + walls);
        assertTrue(Math.abs(MIDDLE - R - Course.TRACK_WIDTH) < Body.EDGE && HALF + R + Course.TRACK_WIDTH < Body.EDGE,
                "and all of it within the world");
        // every land vehicle on pit road, heading north, the way they race
        final List<Lift> lifts = Play.start(Demo.TRACK, false).lifts();
        assertEquals(Course.CHASSES.size() + 1, lifts.size(), "each kind, the car twice (roadster, van)");
        for (final Lift lift : lifts) {
            assertTrue(lift.craft() instanceof Course.Chassis);
            assertEquals(0, lift.place().yaw(), 1e-9);
            assertTrue(lift.place().x() < Course.TRACK_X - Course.TRACK_WIDTH / 2, "inside the track");
        }
    }

    @Test
    void aCarDrivenFlatOutRoundALapStaysOnItLevelAllTheWay() {
        final Play start = Play.start(Demo.TRACK, false);
        final Lift parked = start.lifts().stream().filter(l -> l.craft() == Course.CAR_CHASSIS).findFirst().orElseThrow();
        Play play = new Play(new Avatar(Body.standing(parked.pilot(), 0), false), start.damage(), start.sky(),
                start.lifts(), start.people(), start.props(), start.turrets());
        play = play.step(Controls.doing(Action.TAKE), Clock.STEP).play();
        boolean onTrack = false;
        double gone = 0;
        double last = -1;
        double fastest = 0;
        double mostRoll = 0;
        double highest = 0;
        final double ride = Course.CAR_CHASSIS.ride();
        for (int i = 0; i < 120 / Clock.STEP && gone < LAP; i++) {
            final Lift car = play.lifts().stream().filter(l -> l.piece() == parked.piece()).findFirst().orElseThrow();
            final double x = car.place().x();
            final double z = car.place().z();
            // steered at a point 25 m on round the racing line, as a driver looks ahead
            final double s = along(x, z);
            final double[] ahead = point(s + 2500);
            final double wanted = Math.toDegrees(Math.atan2(ahead[0] - x, ahead[1] - z));
            final double steer = Num.clamp(Num.wrap(wanted - car.place().yaw()) / 6, -1, 1);
            play = play.step(new Controls(1, steer, 0, 0, true, false, 0), Clock.STEP).play();
            if (!onTrack && off(x, z) < Course.TRACK_WIDTH / 4) {
                onTrack = true;
                last = s;
            }
            if (onTrack) {
                assertTrue(off(x, z) < Course.TRACK_WIDTH / 2, "on the track at " + car.place());
                gone += ((s - last) % LAP + LAP) % LAP > LAP / 2 ? 0 : ((s - last) % LAP + LAP) % LAP;
                last = s;
                fastest = Math.max(fastest, car.ahead());
                mostRoll = Math.max(mostRoll, Math.abs(car.roll()));
                highest = Math.max(highest, car.place().height());
            }
        }
        assertTrue(gone >= LAP, "round a lap: " + gone + " of " + LAP);
        assertTrue(fastest > 4000, "flat out, at real speed: " + fastest * 0.036 + " km/h");
        assertEquals(0, mostRoll, 1e-6, "level all the way: " + mostRoll);
        assertTrue(highest < ride + 10, "on the flat all the way: " + highest);
    }
}
