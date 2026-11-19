package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.lifts.HighRiseBank;
import com.example.fxsuite.avatar.lifts.SceneWorld;
import com.example.fxsuite.avatar.sim.Assembly;
import com.example.fxsuite.avatar.sim.Blow;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An office tower: its floors on their outline, all of it but the holes cut for its shafts; its office floors each
 * served by one bank of lifts, a floor cut only where a bank's shafts go through it; laid as a world, stood in; and a
 * car ridden from the lobby up to its zone.
 */
class OfficeTowerTest {

    private static final List<double[]> SQUARE = List.of(new double[] {-1000, -1000}, new double[] {1000, -1000},
            new double[] {1000, 1000}, new double[] {-1000, 1000});

    @Test
    void aFloorIsAllOfItsOutlineButItsHoles() {
        final List<Solid> whole = Plates.floor("f", SQUARE, List.of(), 0, 30, Role.BASE, Material.PRECAST);
        assertEquals(400, Plates.area(whole, true), 1e-6, "20 m square");
        final List<Solid> cut = Plates.floor("f", SQUARE, List.of(new double[] {-200, 300, -100, 400}), 0, 30, Role.BASE,
                Material.PRECAST);
        assertEquals(400 - 5 * 5, Plates.area(cut, true), 1e-6, "less a 5 m square");
        // corners cut and notched: as much as its outline goes round
        final OfficeTower t = OfficeTower.generic(5200, 5200, 800, 600, 950, 12, 465, 1, 4, 400);
        final double outline = Math.abs(Plates.area(t.outline())) / 2 / 10_000;
        assertEquals(52 * 52 - 2 * 8 * 8 / 2.0 - 6 * 6, outline, 1e-6);
        assertEquals(outline, Plates.area(t.floor(t.storeys()), true), 1e-6, "its roof, nothing through it");
    }

    @Test
    void eachOfficeFloorIsServedByOneBankAndCutOnlyWhereShaftsGoThrough() {
        final OfficeTower t = OfficeTower.generic(5200, 5200, 800, 600, 950, 32, 465, 3, 6, 600);
        for (int f = 1; f < t.storeys(); f++) {
            final int floor = f;
            final long serving = t.banks().stream().map(Placed::bank)
                    .filter(b -> b.zoneFrom() <= floor && floor <= b.zoneTo()).map(HighRiseBank::zoneFrom).distinct()
                    .count();
            assertEquals(1, serving, "floor " + f + " served by one zone");
        }
        // over the lowest zone, its shafts gone: more floor
        final int over = t.banks().getFirst().bank().zoneTo() + 1;
        assertTrue(t.area(over) > t.area(1), "floor " + over + " " + t.area(over) + " against " + t.area(1));
    }

    @Test
    void laidAsAWorldACarRiddenFromTheLobbyUpToItsZone() {
        final OfficeTower t = OfficeTower.generic(5200, 5200, 800, 600, 950, 20, 465, 1, 4, 600);
        final Built built = t.built(t.storeys(), true);
        final Course course = new Course(new Played(new SceneWorld(built), Played.Guests.of(List.of())));
        assertEquals(1 + built.movers().size(), course.pieces().size(),
                "its frame one thing, its glass in it; its lifts' parts");
        final HighRiseBank b = t.banks().getFirst().bank();
        final Place car = built.movers().getFirst().route().at(0);
        final double seconds = b.dwell() + b.profile().time(t.levels().get(b.zoneFrom())) + 1;
        Play play = Play.start(new Played(new SceneWorld(built), new Played.Guests(List.of(), 0, false,
                new Spot("in a car", new Place(car.x(), car.z(), 0, 0)))), false);
        for (int n = 0; n < seconds / Clock.STEP; n++) {
            play = play.step(Controls.NONE, Clock.STEP).play();
        }
        assertEquals(t.levels().get(b.zoneFrom()), play.avatar().body().height(), 3, "at its zone's lowest floor");
    }

    @Test
    void aPaneShotThroughIsGoneTheTowerStanding() {
        final OfficeTower t = OfficeTower.generic(5200, 5200, 800, 600, 950, 12, 465, 1, 4, 400);
        final Built built = t.built(t.storeys(), true);
        assertTrue(built.panes().stream().allMatch(p -> p.getFirst().material() == Material.GLASS));
        assertTrue(built.standing().stream().noneMatch(s -> s.material() == Material.GLASS), "its frame no glass");
        final Course course = new Course(new Played(new SceneWorld(built), Played.Guests.of(List.of())));
        Damage damage = Damage.of(course);
        // a rifle round at a time, from outside its west face, halfway up its fifth storey, between two columns
        final double height = (t.levels().get(5) + t.levels().get(6)) / 2;
        final Vec3 from = new Vec3(-3_500, -height, OfficeTower.SPACING / 2);
        final Vec3 east = new Vec3(1, 0, 0);
        final Hit first = damage.obstacles().ray(from, east, 5_000);
        assertEquals(0, first.piece(), "the tower's");
        assertEquals(900, first.distance(), 1, "at its face");
        final int pane = assembly(damage).breakableAt(first.at());
        assertTrue(pane >= 0, "a pane of its glass");
        int rounds = 0;
        while (!assembly(damage).broken(pane)) {
            final Hit hit = damage.obstacles().ray(from, east, 5_000);
            damage = damage.took(List.of(new Blow(hit.at(), hit.normal(), hit.on(), Weapon.RIFLES, hit.piece())))
                    .damage();
            rounds++;
            assertTrue(rounds <= 12);
        }
        assertEquals(Damage.GLASS / Weapon.RIFLES.blow(), rounds, 1e-9);
        assertEquals(1, damage.left(0), 1e-9, "the tower none the worse");
        // gone: the round goes on in, past where the glass was
        final Hit through = damage.obstacles().ray(from, east, 5_000);
        assertTrue(through == null || through.distance() > first.distance() + 100, "into the storey");
        // all else whole
        final Assembly after = assembly(damage);
        assertEquals(1, java.util.stream.IntStream.range(0, after.breakable()).filter(after::broken).count());
        assertEquals(1, after.whole(pane == 0 ? 1 : 0), 1e-12);
    }

    @Test
    void itsLiftsAtWorkOnlyWithTheAvatarNearIt() {
        final OfficeTower t = OfficeTower.generic(5200, 5200, 800, 600, 950, 12, 465, 1, 4, 400);
        final Built built = t.built(t.storeys(), true);
        final int movers = built.movers().size();
        // before its way in: at work, each car and door where its timetable has it
        final Play near = Play.start(new Played(new SceneWorld(built), Played.Guests.of(List.of())), false);
        assertEquals(movers, near.moving().size());
        // 200 m off: at rest where they were laid, not worked out, not drawn
        final Play far = Play.start(new Played(new SceneWorld(built), new Played.Guests(List.of(), 0, false,
                new Spot("far off", new Place(20_000, 0, 0, 0)))), false);
        assertEquals(0, far.moving().size());
        assertTrue(java.util.stream.IntStream.range(1, 1 + movers).allMatch(far::dormant));
    }

    /** The tower's frame, piece 0, as it now stands: its parts, its panes as broken as they are. */
    private static Assembly assembly(final Damage damage) {
        return (Assembly) damage.piece(0).volumes().getFirst();
    }
}
