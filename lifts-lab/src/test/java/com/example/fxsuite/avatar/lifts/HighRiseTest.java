package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Place;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A high-rise building's bank of lifts: each car level with its floor wherever it stands, and standing only at the
 * lobby and its zone's floors, going past the rest; its doors shut while it goes; never faster than its speed; a
 * landing's doors open only while a car stands there; its cars a share of its timetable apart. And ridden: stood in a
 * car at the lobby, carried up to its zone with it.
 */
class HighRiseTest {

    private static HighRiseBank bank() {
        final List<Double> levels = new ArrayList<>();
        for (int f = 0; f <= 40; f++) {
            levels.add(f * 420.0);
        }
        return new HighRiseBank(0, 0, levels, 30, 40, 4, new Profile(600, 100, 150), 200, 170, 260, 100, 3.5);
    }

    @Test
    void eachCarStandsOnlyWhereItServesLevelWithItsFloorItsDoorsShutWhileItGoes() {
        final HighRiseBank b = bank();
        final HighRiseBank.Timetable table = b.timetable();
        final double step = 0.02;
        for (double t = 0; t < table.cycle(); t += step) {
            final ShuttleLift.State s = table.at(0, t);
            if (s.at() >= 0) {
                assertTrue(b.served().contains(s.at()), "standing only where it serves, not " + s.at());
                assertEquals(b.levels().get(s.at()), s.level(), 1e-9, "level with its floor");
            } else {
                assertEquals(0, s.open(), 1e-9, "its doors shut while it goes, at " + t);
            }
            assertTrue(Math.abs(table.at(0, t + step).level() - s.level()) <= b.profile().speed() * step + 1e-6,
                    "never faster than its speed, at " + t);
            for (final int floor : b.served()) {
                assertTrue(table.landingOpen(0, floor, t) == 0 || s.at() == floor, "landing " + floor + " at " + t);
            }
        }
    }

    @Test
    void itsCarsAreAShareOfItsTimetableApart() {
        final HighRiseBank b = bank();
        final HighRiseBank.Timetable table = b.timetable();
        for (int k = 1; k < b.cars(); k++) {
            for (final double t : new double[] {0, 17.3, 95.1}) {
                assertEquals(table.at(0, t + k * b.interval()).level(), table.at(k, t).level(), 1e-6,
                        "car " + k + " at " + t);
            }
        }
    }

    @Test
    void stoodInACarAtTheLobbyItIsCarriedUpToItsZone() {
        final HighRiseScene scene = new HighRiseScene();
        final Built built = scene.built(Map.of());
        final HighRiseBank b = bank();
        // car 0 at the lobby at the start, its doors opening; its first stop over it, the zone's lowest floor
        final double seconds = b.dwell() + b.profile().time(b.levels().get(30)) + 1;
        final Place car = built.movers().getFirst().route().at(0);
        Play play = Play.start(new Played(new SceneWorld(built), new Played.Guests(List.of(), 0, false,
                new Spot("in car 1", new Place(car.x(), car.z(), 0, 180)))), false);
        for (int n = 0; n < seconds / Clock.STEP; n++) {
            play = play.step(Controls.NONE, Clock.STEP).play();
        }
        assertEquals(b.levels().get(30), play.avatar().body().height(), 3, "at the zone's lowest floor, 126 m up");
        assertEquals(car.x(), play.avatar().body().position().x(), 5, "in its car still");
    }
}
