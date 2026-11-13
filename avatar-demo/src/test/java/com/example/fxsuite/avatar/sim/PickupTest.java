package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pickup: a chassis with a heavy Gatling gun on a post at the back, aimed freely. Whoever sits
 * at the wheel drives it and fires the gun as one: the keys drive, the mouse aims the gun, which
 * holds its aim on the world as the pickup turns under it.
 */
class PickupTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Lift pickup(final Play play) {
        return play.lifts().stream().filter(l -> l.craft() == Course.PICKUP_CHASSIS).findFirst().orElseThrow();
    }

    private static Turret gun(final Play play) {
        return play.turrets().stream().filter(t -> t.mount() == Course.REAR_GUN).findFirst().orElseThrow();
    }

    /** The roads demo, the avatar at the pickup's wheel, and taking it. */
    private static Play atTheWheel(final List<Event> events) {
        final Play start = Play.start(Demo.FLYOVER, false);
        final Lift pickup = pickup(start);
        final Play at = new Play(new Avatar(Body.standing(pickup.pilot(), 0), false), start.damage(), start.sky(),
                start.lifts(), start.people(), start.props(), start.turrets());
        assertTrue(at.gunWithinReach().isPresent(), "its gun is taken at its wheel");
        assertTrue(at.withinReach().isEmpty(), "and its wheel with it, not on its own");
        return run(at, TAKE, Clock.STEP, events);
    }

    @Test
    void thePickupHasAGunThatTurnsAtItsBack() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final Turret gun = gun(start);
        assertEquals(pickup(start).piece(), gun.host());
        assertFalse(gun.mount().fixed());
        assertTrue(gun.mount().stabilized());
        assertTrue(gun.mount().pivot().along() < Course.PICKUP_CHASSIS.driverAlong(), "behind the seat");
        assertTrue(gun.mount().pivot().up() > Body.TALL - 80 + 40, "over the seated driver's head");
    }

    @Test
    void takenAtTheWheelItIsDrivenAndItsGunAimedAsOne() {
        final List<Event> events = new ArrayList<>();
        Play play = atTheWheel(events);
        assertTrue(gun(play).held());
        assertTrue(pickup(play).held());
        assertEquals(List.of(new Event.TookControls(pickup(play).piece())), events);
        assertFalse(play.hidden(), "the driver in plain sight");
        // the keys drive it, A and D steering it as ever
        play = run(play, new Controls(0.5, 1, 0, 0, true, false, 0), 1.5, events);
        assertTrue(pickup(play).ahead() > 0);
        assertTrue(pickup(play).place().yaw() > 5, "steered right");
        // the mouse swings the gun round, not the pickup
        final double yaw = pickup(play).place().yaw();
        play = run(play, new Controls(0, 0, 0, 0, true, false, 0), 0.5, events);
        final double still = pickup(play).place().yaw();
        play = run(play, new Controls(0, 0, 1, 0, true, false, 0), 1, events);
        assertEquals(still, pickup(play).place().yaw(), 1, "the pickup as it was, rolling on");
        assertTrue(Math.abs(gun(play).yaw()) > 60, "the gun swung round: " + gun(play).yaw());
        assertTrue(yaw > 0);
    }

    @Test
    void itsGunHoldsItsAimAsThePickupTurnsUnderIt() {
        final List<Event> events = new ArrayList<>();
        Play play = atTheWheel(events);
        final double aim = gun(play).heading();
        play = run(play, new Controls(0.6, -1, 0, 0, true, false, 0), 2, events);
        assertTrue(Math.abs(Num.wrap(pickup(play).place().yaw())) > 20, "turned");
        assertEquals(aim, gun(play).heading(), 1e-6, "the gun still pointing where it was");
        assertEquals(pickup(play).place().yaw(), play.avatar().body().yaw(), 90, "the driver as it was, seated");
    }

    @Test
    void itsRoundsGoOverTheDriverAndBreakTheCratesItIsTurnedOn() {
        final List<Event> events = new ArrayList<>();
        Play play = atTheWheel(events);
        // the nearest crate, and the gun turned and tipped onto it
        final Turret gun = gun(play);
        final Vec3 pivot = gun.pivot();
        final Damage damage = play.damage();
        final int piece = java.util.stream.IntStream.range(0, damage.size())
                .filter(i -> damage.piece(i).kind() == Course.Kind.CRATE).boxed()
                .min((a, b) -> Double.compare(
                        Math.hypot(damage.piece(a).block().x() - pivot.x(), damage.piece(a).block().z() - pivot.z()),
                        Math.hypot(damage.piece(b).block().x() - pivot.x(), damage.piece(b).block().z() - pivot.z())))
                .orElseThrow();
        final Block box = damage.piece(piece).block();
        final double across = Math.hypot(box.x() - pivot.x(), box.z() - pivot.z());
        final double round = Num.wrap(Math.toDegrees(Math.atan2(box.x() - pivot.x(), box.z() - pivot.z())));
        final double down = Math.toDegrees(Math.atan2((box.bottom() + box.top()) / 2 + pivot.y(), across));
        play = run(play, new Controls(0, 0, round, 0, true, false, down), Clock.STEP, events);
        play = run(play, new Controls(0, 0, 0, 0, true, false, down), 2.5, events);
        assertEquals(round, gun(play).heading(), 1e-6);
        events.clear();
        play = run(play, new Controls(0, 0, 0, 0, true, true, down), 4, events);
        assertTrue(play.damage().broken(piece), "the crate broken");
        assertEquals(1, play.damage().left(pickup(play).piece()), 1e-12, "the pickup unharmed by its own gun");
    }

    @Test
    void letGoItRollsToAStop() {
        final List<Event> events = new ArrayList<>();
        Play play = atTheWheel(events);
        play = run(play, new Controls(1, 0, 0, 0, true, false, 0), 1.5, events);
        events.clear();
        play = run(play, TAKE, Clock.STEP, events);
        assertEquals(List.of(new Event.LetGo(pickup(play).piece())), events);
        assertFalse(gun(play).held());
        assertFalse(pickup(play).held());
        play = run(play, Controls.NONE, 8, events);
        assertEquals(0, pickup(play).ahead(), 1e-9);
    }
}
