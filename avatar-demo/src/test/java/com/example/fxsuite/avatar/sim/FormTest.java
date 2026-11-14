package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A form: vertices with no size, edges with no thickness, by name; how big it is, how many pieces,
 * what is wrong with it, and kept as text.
 */
class FormTest {

    /** A metre cube, and a window's outline standing apart on its front. */
    private static Form cube() {
        final Form.Builder form = new Form.Builder("cube");
        final int[] low = new int[4];
        final int[] high = new int[4];
        final double[][] corners = {{0, 0}, {100, 0}, {100, 100}, {0, 100}};
        for (int k = 0; k < 4; k++) {
            low[k] = form.vertex("low " + k, corners[k][0], corners[k][1], 0);
            high[k] = form.vertex("high " + k, corners[k][0], corners[k][1], 100);
            form.edge(low[k], high[k]);
        }
        form.loop(low).loop(high);
        form.loop(form.vertex("window a", 30, 100, 30), form.vertex("window b", 70, 100, 30),
                form.vertex("window c", 70, 100, 70), form.vertex("window d", 30, 100, 70));
        return form.build();
    }

    @Test
    void itsSizeItsLengthAndItsPieces() {
        final Form cube = cube();
        assertEquals(16, cube.edges().size());
        assertEquals(12 * 100 + 4 * 40, cube.length(), 1e-9);
        final Form.Bounds box = cube.bounds();
        assertEquals(100, box.width(), 1e-9);
        assertEquals(100, box.depth(), 1e-9);
        assertEquals(100, box.height(), 1e-9);
        assertEquals(2, cube.pieces(), "the cube, and the window's outline apart from it");
        assertEquals(List.of(), cube.problems());
    }

    @Test
    void whatIsWrongWithItIsSaid() {
        final Form.Builder form = new Form.Builder("faulty");
        final int a = form.vertex("a", 0, 0, 0);
        final int b = form.vertex("b", 100, 0, 0);
        form.vertex("lonely", 0, 0, 500);
        final int near = form.vertex("near a", 0.05, 0, 0);
        form.edge(a, b).edge(b, a).edge(near, b);
        final List<String> problems = form.build().problems();
        assertTrue(problems.contains("a and near a are one point"), problems.toString());
        assertTrue(problems.contains("an edge twice, b to a"), problems.toString());
        assertTrue(problems.contains("lonely joins nothing"), problems.toString());
        assertEquals(3, problems.size(), problems.toString());
    }

    @Test
    void namesAreItsOwnAndEdgesJoinTwoVerticesThereAre() {
        final Form.Builder form = new Form.Builder("x");
        form.vertex("a", 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> form.vertex("a", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> form.edge("a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new Form("x", List.of(new Form.Vertex("a", 0, 0, 0)),
                List.of(new Form.Edge(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> new Form("x", List.of(new Form.Vertex("a", 0, 0, 0)),
                List.of(new Form.Edge(0, 1))));
    }

    @Test
    void keptAsTextAndReadBack() {
        final Form cube = cube();
        final String text = cube.text();
        assertTrue(text.startsWith("form \"cube\"\n"));
        assertTrue(text.contains("vertex \"window a\" 30 100 30\n"));
        assertTrue(text.contains("edge \"low 0\" \"high 0\"\n"));
        assertEquals(cube, Form.read(text));
        assertEquals(cube, Form.read("# a comment\n\n" + text + "face whatever comes later\n"),
                "comments, blank lines and lines for others passed over");
    }

    @Test
    void everyBuildingIsKeptAsTextToo() {
        // to the hundredth of a centimetre the text keeps
        for (final Form building : Buildings.all()) {
            final Form read = Form.read(building.text());
            assertEquals(building.edges(), read.edges(), building.name());
            assertEquals(building.text(), read.text());
            for (int v = 0; v < building.vertices().size(); v++) {
                assertEquals(building.vertices().get(v).name(), read.vertices().get(v).name());
                assertTrue(building.vertices().get(v).distance(read.vertices().get(v)) < 0.01);
            }
        }
    }
}
