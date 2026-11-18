package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Demos;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The temple's courtyard: its great hall at the back, a smaller hall each side facing in, each one
 * piece, clear of each other, of the start and of the monks; and the monks at their stand-up on the
 * ground between.
 */
class TempleYardTest {

    @Test
    void aGreatHallAndASmallerHallEachSideFacingIn() {
        final List<Course.Piece> halls = TempleYard.pieces().subList(0, TempleYard.HALLS.size());
        assertEquals(3, halls.size());
        final Block great = halls.get(0).block();
        for (int k = 0; k < halls.size(); k++) {
            final Course.Piece hall = halls.get(k);
            assertEquals(Course.Kind.BUILDING, hall.kind());
            assertEquals(TempleYard.KEYS + k, hall.tint(), "drawn by its own key");
            assertFalse(TempleYard.drawn(hall.tint()).isEmpty());
            assertFalse(hall.volumes().isEmpty(), "in the way as it is shaped");
            if (k > 0) {
                assertTrue(hall.block().top() < great.top(), "the side halls lower");
                assertTrue(hall.block().halfWidth() < great.halfWidth(), "and narrower");
                // facing in, toward the courtyard's middle line
                final Vec3 facing = Num.heading(hall.place().yaw());
                assertTrue(facing.x() * -Math.signum(hall.place().x()) > 0.99, TempleYard.HALLS.get(k).name() + " faces in");
            }
            for (int other = k + 1; other < halls.size(); other++) {
                assertFalse(hall.block().overlaps(halls.get(other).block()), k + " and " + other);
            }
            assertFalse(hall.block().covers(0, 0, 200), "clear of the start");
            for (final Course.Cast monk : Course.MONKS) {
                assertFalse(hall.block().covers(monk.across(), monk.along(), Body.RADIUS + 200), "clear of the monks");
            }
        }
        // the great hall at the back on the middle line, facing the start
        assertEquals(0, great.x(), 1e-6);
        assertTrue(Num.heading(halls.get(0).place().yaw()).z() < -0.99);
    }

    @Test
    void threeMonksAtTheirStandUpInTheCourtyardInTheMiddleOfTown() {
        Town.tell();
        final Set<Person.Role> monks = Set.of(Person.Role.SMALL_MONK, Person.Role.TALL_MONK, Person.Role.STOUT_MONK);
        final List<Person> there = Play.start(Demos.TOWN, false).people().stream()
                .filter(p -> monks.contains(p.role())).toList();
        assertEquals(3, there.size());
        assertEquals(monks, there.stream().map(Person::role).collect(Collectors.toSet()));
        // on the ground, in a ring, each facing its middle: in the courtyard, where it is in the town
        final Vec3 middle = new Vec3(Town.temple().x(), 0, Town.temple().z() + Course.MONKS_AHEAD);
        for (final Person monk : there) {
            assertEquals(0, monk.body().height(), 1e-6, "on the ground");
            final Vec3 toward = middle.subtract(monk.body().position().multiply(1)).normalize();
            final Vec3 facing = Num.heading(monk.body().yaw());
            assertTrue(facing.x() * toward.x() + facing.z() * toward.z() > 0.99, monk.role() + " faces the middle");
        }
        // and nobody on the ground in the playground
        assertTrue(new Course(Demos.PLAYGROUND).ground().isEmpty());
    }
}
