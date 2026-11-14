package com.example.fxsuite.avatar.sim;

import com.example.fxsuite.avatar.sim.Frame.Kind;
import com.example.fxsuite.avatar.sim.Frame.Node;
import com.example.fxsuite.avatar.sim.Frame.Rod;
import com.example.fxsuite.avatar.sim.Frame.Tube;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Anything built of rigid rods joined at their ends, as a graph: its figures from its rods, what is
 * wrong with it, how far it leans on its welds, shaped a change at a time, and kept as text.
 */
class RodFrameTest {

    private static final Tube TUBE = new Tube(1, 0.2);

    /** A square, 100 on a side, flat on the ground: four nodes, four rods. */
    private static final Frame SQUARE = new Frame("a square", List.of(
            new Node("a", 0, 0, 0), new Node("b", 100, 0, 0), new Node("c", 100, 100, 0), new Node("d", 0, 100, 0)),
            List.of(new Rod(0, 1), new Rod(1, 2), new Rod(2, 3), new Rod(3, 0)), TUBE);

    @Test
    void itsFiguresComeFromItsRods() {
        assertEquals(400, SQUARE.steel(), 1e-9);
        assertEquals(400 * TUBE.area() * Frame.STEEL, SQUARE.mass(), 1e-12);
        assertEquals(Math.PI * (1 - 0.64), TUBE.area(), 1e-12, "a ring, outside less inside");
        final Frame.Point centre = SQUARE.centre();
        assertEquals(50, centre.across(), 1e-9);
        assertEquals(50, centre.along(), 1e-9);
        // a heavier rod draws the mass toward it
        final Frame heavy = SQUARE.sized(1, new Tube(3, 1));
        assertTrue(heavy.centre().across() > 50);
        assertEquals(new Tube(3, 1), heavy.tube(heavy.rods().get(1)));
        assertEquals(TUBE, heavy.tube(heavy.rods().get(0)), "the rest the frame's tube still");
        assertEquals(List.of(0, 3), SQUARE.rodsAt(0));
        assertEquals(2, SQUARE.node("c"));
        assertEquals(-1, SQUARE.node("no such"));
    }

    @Test
    void whatIsWrongWithItIsSaid() {
        assertTrue(SQUARE.problems().isEmpty());
        final Frame loose = new Frame("loose", List.of(new Node("a", 0, 0, 0), new Node("b", 1, 0, 0),
                new Node("c", 5, 5, 5)), List.of(new Rod(0, 1), new Rod(1, 0), new Rod(1, 1)), TUBE);
        final List<String> wrong = loose.problems();
        assertTrue(wrong.stream().anyMatch(p -> p.contains("to itself")), wrong.toString());
        assertTrue(wrong.stream().anyMatch(p -> p.startsWith("two rods")), wrong.toString());
        assertTrue(wrong.stream().anyMatch(p -> p.contains("no rod meets c")), wrong.toString());
        assertTrue(wrong.stream().anyMatch(p -> p.startsWith("in pieces")), wrong.toString());
        assertFalse(loose.connected());
        assertThrows(IllegalArgumentException.class, () -> new Frame("x", List.of(new Node("a", 0, 0, 0)),
                List.of(new Rod(0, 3)), TUBE), "a rod to a node there is not");
        assertThrows(IllegalArgumentException.class, () -> new Tube(1, 1), "a wall as thick as the tube");
    }

    @Test
    void howFarItLeansOnItsWeldsIsCounted() {
        // a square pinned at its corners folds; free, it moves as a whole besides: 3 x 4 - 4 - 6
        assertEquals(2, SQUARE.slack());
        // a triangle, flat, free: 9 - 3 - 6 = 0, braced by its rods alone
        final Frame triangle = new Frame("a triangle", List.of(new Node("a", 0, 0, 0), new Node("b", 100, 0, 0),
                new Node("c", 0, 100, 0)), List.of(new Rod(0, 1), new Rod(1, 2), new Rod(2, 0)), TUBE);
        assertEquals(0, triangle.slack());
        // a tripod bolted down at its feet: its top free to go three ways, three legs hold it
        final Frame tripod = new Frame("a tripod", List.of(new Node("top", 0, 0, 100),
                new Node("1", 50, 0, 0, Kind.MOUNT), new Node("2", -25, 43, 0, Kind.MOUNT),
                new Node("3", -25, -43, 0, Kind.MOUNT)), List.of(new Rod(0, 1), new Rod(0, 2), new Rod(0, 3)), TUBE);
        assertEquals(0, tripod.slack());
        // braced across a diagonal, the square holds by its rods alone
        assertEquals(1, SQUARE.joined(0, 2).slack(), "one diagonal: still a way to go");
    }

    @Test
    void shapedAChangeAtATime() {
        final Frame moved = SQUARE.moved(2, 120, 100, 10);
        assertEquals(120, moved.nodes().get(2).across(), 1e-9);
        assertEquals("c", moved.nodes().get(2).name());
        final Frame split = SQUARE.split(0, "ab");
        assertEquals(5, split.nodes().size());
        assertEquals(5, split.rods().size());
        assertEquals(50, split.nodes().get(4).across(), 1e-9, "in its middle");
        assertEquals(SQUARE.steel(), split.steel(), 1e-9, "as much tube as ever");
        assertTrue(split.problems().isEmpty(), split.problems().toString());
        final Frame braced = SQUARE.joined(0, 2);
        assertEquals(5, braced.rods().size());
        assertEquals(SQUARE.steel() + 100 * Math.sqrt(2), braced.steel(), 1e-9);
        final Frame opened = SQUARE.removed(3);
        assertEquals(3, opened.rods().size());
        assertTrue(opened.connected(), "still one piece, open");
        final Frame mirrored = SQUARE.mirrored();
        assertEquals(-100, mirrored.nodes().get(1).across(), 1e-9);
        assertEquals(SQUARE.steel(), mirrored.steel(), 1e-9);
        assertEquals(new Tube(2, 0.5), SQUARE.tubed(new Tube(2, 0.5)).tube());
    }

    @Test
    void keptAsTextItReadsBackTheSameAndCanBeEditedByHand() {
        final Frame shaped = SQUARE.sized(1, new Tube(2.5, 0.3)).split(2, "the middle of c and d")
                .moved(0, 0, 0, 12.5);
        final Frame read = Frame.read(shaped.text());
        assertEquals(shaped, read);
        // by hand: a remark, a node bolted down, a rod by its nodes' names, its own tube
        final Frame written = Frame.read("""
                # a post and its brace
                frame "a post"
                tube 1.5 0.2
                node "foot" 0 0 0 mount
                node "top" 0 0 200
                node "brace foot" 80 0 0 mount
                rod "foot" "top"
                rod "brace foot" "top" tube 1 0.15
                """);
        assertEquals("a post", written.name());
        assertEquals(Kind.MOUNT, written.nodes().get(0).kind());
        assertEquals(new Tube(1, 0.15), written.rods().get(1).tube());
        assertEquals(1, written.rods().get(1).to());
        assertThrows(IllegalArgumentException.class, () -> Frame.read("tube 1 0.1\nrod \"a\" \"b\"\n"),
                "a rod to a node there is not");
        assertThrows(IllegalArgumentException.class, () -> Frame.read("node \"a\" 0 0\n"), "a node without its height");
    }
}
