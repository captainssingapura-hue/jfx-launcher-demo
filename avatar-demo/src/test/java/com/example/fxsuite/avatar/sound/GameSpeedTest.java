package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The game's land vehicles go as their engine bench vehicles do: as fast at the top, and as
 * quick to make most of their speed. So what the bench drives, and is heard doing, is what the
 * game does.
 */
class GameSpeedTest {

    @Test
    void eachGoesAsFastAndGetsThereAsQuicklyAsOnTheBench() {
        for (final Course.Chassis chassis : Course.CHASSES) {
            final Vehicle vehicle = Vehicle.of(chassis);
            final Course.Drive drive = chassis.drive();
            // the bench: flat out, as fast as it goes, and how long to make 60% of that
            Driving driving = Driving.stopped(vehicle);
            double top = 0;
            for (int i = 0; i < 150 * 120; i++) {
                driving = driving.step(vehicle, 1, 0, 0, true, 1.0 / 120);
                top = Math.max(top, driving.speed());
            }
            driving = Driving.stopped(vehicle);
            double bench = 0;
            while (driving.speed() < 0.6 * top) {
                driving = driving.step(vehicle, 1, 0, 0, true, 1.0 / 120);
                bench += 1.0 / 120;
            }
            // as heavy as on the bench, what it shoves and is shoved by in a collision
            assertEquals(vehicle.mass(), drive.mass(), 1e-9, chassis.name() + "'s mass");
            // the game: its top speed, in centimetres a second, and its own time to 60% of it
            assertEquals(top * 100, drive.top(), 0.02 * drive.top(), chassis.name() + "'s top speed");
            double speed = 0;
            double game = 0;
            while (speed < 0.6 * drive.top()) {
                speed += drive.accelAt(speed) * Clock.STEP;
                game += Clock.STEP;
            }
            assertEquals(bench, game, 0.1 * bench, chassis.name() + " to 60% of its top speed");
        }
    }
}
