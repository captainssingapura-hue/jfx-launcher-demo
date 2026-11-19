package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.PlayWorld;
import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Blow;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Staged;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tower standing in a world, staged: one piece, far off answering as its coarse shape does, its outline stood up,
 * nothing inside it; its stage following the focus, far, near and in focus, as the avatar comes to it; in focus, the
 * storeys the focus meets fine, walked into, their glass shot out, the rest of it coarse; a pane shot out kept so, gone
 * from and come back to.
 */
class StagedTowerTest {

    /** A generic tower, 52 m square, its middle at the world's middle, its front to the north. */
    private static final OfficeTower TOWER = OfficeTower.generic(5200, 5200, 800, 600, 950, 12, 465, 1, 4, 400);
    private static final Standing STANDING = new Standing(TOWER, new Place(0, 0, 0, 0), 975_000);
    private static final Built BUILT = STANDING.built();

    /** Level ground, a kilometre each way, the tower on it; the avatar starting at {@code start}. */
    private record OneTower(Spot start) implements PlayWorld {

        @Override
        public String title() {
            return "a tower";
        }

        @Override
        public String description() {
            return "a tower on level ground";
        }

        @Override
        public void lay(final Course.Layout world) {
            STANDING.register(BUILT);
            final Terrain flat = new Terrain("level", -100_000, -100_000, 100_000, 100_000, 1_000, 0, 0,
                    Terrain.Noise.NONE, List.of(), List.of());
            world.land(new Land(flat.survey(), List.of())).ruled(true).pieces(STANDING.pieces(BUILT));
        }

        @Override
        public Scenery scenery() {
            return Scenery.still(new Group());
        }

        @Override
        public List<Spot> parked(final List<Double> lengths) {
            return List.of();
        }

        @Override
        public List<Spot> standing(final int count) {
            return List.of();
        }
    }

    /** The world, the avatar standing {@code south} cm south of the tower's middle, a step on. */
    private static Play from(final double south) {
        final Spot at = new Spot("there", new Place(0, -south, 0, 0));
        final Play start = Play.start(new Played(new OneTower(at), new Played.Guests(List.of(), 0, false, at)), false);
        return start.step(Controls.NONE, Clock.STEP).play();
    }

    @Test
    void farOffItIsOnePieceInTheWayAsItsCoarseShapeIs() {
        final Play play = from(30_000);
        assertEquals(1, new Course(new Played(new OneTower(new Spot("x", new Place(0, -30_000, 0, 0))),
                Played.Guests.of(List.of()))).pieces().size(), "the tower, and nothing else of it");
        assertFalse(staged(play).fineAnswering(), "nothing of it fine");
        // a round at its south face, halfway up its fifth storey: met at its outline, as its glass would be
        final double height = (TOWER.levels().get(5) + TOWER.levels().get(6)) / 2;
        final Hit hit = play.obstacles().ray(new Vec3(0, -height, -3_500), new Vec3(0, 0, 1), 5_000);
        assertNotNull(hit);
        assertEquals(0, hit.piece());
        assertEquals(900, hit.distance(), 1, "at its face");
        // its way in solid: nothing inside it is built yet
        final Vec3 door = new Vec3(0, 0, 2_600 - 50);
        assertNotNull(play.obstacles().volumes().getFirst().push(door.x(), door.z(), 30, 0, 180), "its door shut");
        assertEquals(TOWER.roof(), play.obstacles().volumes().getFirst().topAt(0, 0, 30, 100_000), 1e-6,
                "its roof the top of it");
    }

    @Test
    void whatIsInsideItIsKnownWhateverItsStage() {
        final double height = (TOWER.levels().get(5) + TOWER.levels().get(6)) / 2;
        for (final double south : new double[] {40_000, 2_600 + 300}) {
            final Damage damage = from(south).damage();
            assertEquals(0, damage.stagedAround(new Vec3(1_000, -height, 1_000)), "in its fifth storey");
            assertEquals(0, damage.stagedAround(new Vec3(0, -200, 2_000)), "in its lobby");
            assertEquals(-1, damage.stagedAround(new Vec3(0, -height, -2_600 - 300)), "3 m off its face");
            assertEquals(-1, damage.stagedAround(new Vec3(0, -TOWER.roof() - TOWER.crown() - 500, 0)), "over it");
        }
    }

    @Test
    void itsStageFollowsTheFocus() {
        assertEquals(Staged.Stage.FAR, from(40_000).stage(0), "400 m off");
        assertEquals(Staged.Stage.NEAR, from(10_000).stage(0), "100 m off");
        assertEquals(Staged.Stage.FOCUS, from(2_600 + 300).stage(0), "3 m from its face");
    }

    @Test
    void comeToItTheStageIsKeptALittleFurtherThanItIsComeTo() {
        assertEquals(Staged.Stage.FOCUS, Play.stage(Staged.Stage.FOCUS, Play.FOCUS_LEAVING - 1));
        assertEquals(Staged.Stage.NEAR, Play.stage(Staged.Stage.NEAR, Play.FOCUS_LEAVING - 1), "not yet in focus");
        assertEquals(Staged.Stage.NEAR, Play.stage(Staged.Stage.NEAR, Play.NEAR + Play.NEAR_LEAVING - 1));
        assertEquals(Staged.Stage.FAR, Play.stage(Staged.Stage.FAR, Play.NEAR + 1), "not yet near");
        assertEquals(Staged.Stage.FAR, Play.stage(Staged.Stage.NEAR, Play.NEAR + Play.NEAR_LEAVING + 1));
    }

    @Test
    void inFocusTheStoreysTheFocusMeetsAreFineAndNoOthers() {
        final Play play = from(2_600 + 300);
        assertEquals(Staged.Stage.FOCUS, play.stage(0));
        final Staged staged = staged(play);
        assertTrue(staged.fineAnswering());
        assertEquals(0, staged.from(), 1e-9, "from the ground");
        assertEquals(TOWER.levels().get(1), staged.to(), 1e-9, "to the lobby's ceiling: the lobby, its storey");
        // a round at the lobby's glass meets a pane of it; one at the fifth storey's, the coarse shape over the band
        final Hit low = play.obstacles().ray(new Vec3(1_000, -200, -3_500), new Vec3(0, 0, 1), 5_000);
        assertTrue(staged.paneAt(low.at()) >= 0, "a pane of the lobby's glass");
        final double height = (TOWER.levels().get(5) + TOWER.levels().get(6)) / 2;
        final Hit high = play.obstacles().ray(new Vec3(1_000, -height, -3_500), new Vec3(0, 0, 1), 5_000);
        assertEquals(900, high.distance(), 1, "at its face");
        assertEquals(-1, staged.paneAt(high.at()), "the coarse shape there: nothing to break");
    }

    @Test
    void inFocusItIsWalkedIntoThroughItsWayIn() {
        // before its way in, on its north side, facing it
        final Spot door = new Spot("before its way in", new Place(0, 2_600 + 300, 0, 180));
        Play play = Play.start(new Played(new OneTower(door), new Played.Guests(List.of(), 0, false, door)), false);
        for (int n = 0; n < 6 / Clock.STEP; n++) {
            play = play.step(new Controls(1, 0, 0, 0, false), Clock.STEP).play();
        }
        assertTrue(play.avatar().body().position().z() < 2_600 - 300, "in its lobby: "
                + play.avatar().body().position());
    }

    @Test
    void aPaneShotOutWhereItIsFineStaysBrokenGoneFromAndComeBackTo() {
        Play play = from(2_600 + 300);
        final Vec3 at = new Vec3(1_000, -200, -3_500);
        final Vec3 north = new Vec3(0, 0, 1);
        final int pane = staged(play).paneAt(play.obstacles().ray(at, north, 5_000).at());
        Damage damage = play.damage();
        for (int rounds = 0; rounds < 12 && !staged(damage).fine().broken(pane); rounds++) {
            final Hit hit = damage.obstacles().ray(at, north, 5_000);
            damage = damage.took(List.of(new Blow(hit.at(), hit.normal(), hit.on(), Weapon.RIFLES, hit.piece())))
                    .damage();
        }
        assertTrue(staged(damage).fine().broken(pane), "shot out");
        final Hit through = damage.obstacles().ray(at, north, 5_000);
        assertTrue(through == null || through.distance() > 900 + 100, "a round goes on in");
        // gone from, 400 m off: far; come back: in focus, the pane still gone
        play = new Play(new Avatar(Body.standing(new Vec3(0, 0, -40_000), 0), false), damage)
                .step(Controls.NONE, Clock.STEP).play();
        assertEquals(Staged.Stage.FAR, play.stage(0));
        play = new Play(new Avatar(Body.standing(new Vec3(0, 0, -2_900), 0), false), play.damage())
                .step(Controls.NONE, Clock.STEP).play();
        assertEquals(Staged.Stage.FOCUS, play.stage(0));
        assertTrue(staged(play).fine().broken(pane), "still gone");
        final Hit again = play.obstacles().ray(at, north, 5_000);
        assertTrue(again == null || again.distance() > 900 + 100, "still a way in");
    }

    private static Staged staged(final Play play) {
        return staged(play.damage());
    }

    private static Staged staged(final Damage damage) {
        return Damage.staged(damage.piece(0));
    }
}
