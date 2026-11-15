package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Plans.Shape;
import com.example.fxsuite.avatar.build.Plans.Size;
import com.example.fxsuite.avatar.build.Tower.Section;

import java.util.List;

/**
 * Typical towers, stacked from common plans ({@link Plans}) on an 8 m grid: each section's lines
 * those of the one below it, so its columns stand on columns. A tower set back twice from a podium;
 * a round tower; an oval one; a rounded one; an L-shaped block on a podium; a tower with a bow front.
 */
public final class Towers {

    private Towers() {
    }

    /** The grid they are all laid out on. */
    public static final double BAY = 800;

    private static Plan plan(final Shape shape, final double width, final double depth, final double extra) {
        return Plans.of(shape, new Size(width, depth, BAY, extra));
    }

    /**
     * Set back twice: a podium 56 m square, two floors of 5 m; a shaft 40 m square, 28 floors of 4 m;
     * a top 24 m square, 8 more. 154 m to its roof.
     */
    public static Tower setback() {
        return new Tower("setback tower", List.of(
                new Section("podium", plan(Shape.SQUARE, 5600, 0, 0), 2, 500),
                new Section("shaft", plan(Shape.SQUARE, 4000, 0, 0), 28, 400),
                new Section("top", plan(Shape.SQUARE, 2400, 0, 0), 8, 400)));
    }

    /** Round: 40 m across, 45 floors of 3.8 m; a ring of columns round its edge, tied in to its grid. */
    public static Tower round() {
        return Tower.of("round tower", plan(Shape.CIRCLE, 4000, 0, 0), 45, 380);
    }

    /** Oval: 48 m by 32 m, a little fuller than an ellipse, 40 floors of 3.8 m. */
    public static Tower oval() {
        return Tower.of("oval tower", plan(Shape.OVAL, 4800, 3200, 2.5), 40, 380);
    }

    /** A rectangle 48 m by 32 m, its corners rounded 8 m, 36 floors of 3.8 m. */
    public static Tower rounded() {
        return Tower.of("rounded tower", plan(Shape.ROUNDED, 4800, 3200, 800), 36, 380);
    }

    /**
     * An L-shaped block, 48 m by 40 m, its arms 16 m, 16 floors of 3.6 m, on a podium a bay wider all
     * round, 64 m by 56 m (a bay, not half of one: its lines are the block's), two floors of 5 m.
     */
    public static Tower lBlock() {
        return new Tower("L block on a podium", List.of(
                new Section("podium", plan(Shape.RECTANGLE, 6400, 5600, 0), 2, 500),
                new Section("block", plan(Shape.L_SHAPE, 4800, 4000, 1600), 16, 360)));
    }

    /** A rectangle 48 m by 32 m, a half round bow of 12 m out of its front, 30 floors of 3.8 m. */
    public static Tower bowFront() {
        return Tower.of("bow-front tower", plan(Shape.BOW, 4800, 3200, 1200), 30, 380);
    }

    /** The void deck tower's floors all told, and its deck's place, height and enclosed wing as first drawn. */
    public static final int DECK_TOWER_FLOORS = 30;
    public static final int DECK_FLOOR = 15;
    public static final double DECK_HEIGHT = 760;
    public static final int DECK_WING = 2;

    /** A tower with a void deck half way up ({@link #voidDeck(int, double, int)}), as first drawn. */
    public static Tower voidDeck() {
        return voidDeck(DECK_FLOOR, DECK_HEIGHT, DECK_WING);
    }

    /**
     * A rectangle 48 m by 32 m on the 8 m grid, 30 floors of 3.8 m, but that its floor {@code floor}
     * is a void deck {@code height} high: open, its columns the whole grid's, but for a wing
     * {@code wing} bays wide at its west end, enclosed (none: open all through).
     */
    public static Tower voidDeck(final int floor, final double height, final int wing) {
        if (floor < 2 || floor > DECK_TOWER_FLOORS - 3 || height < 300 || height > 1500 || wing < 0 || wing > 5) {
            throw new IllegalArgumentException("a void deck at floor " + floor + ", " + height + " high, its wing "
                    + wing + " bays: not one that stands");
        }
        final Plan plan = plan(Shape.RECTANGLE, 4800, 3200, 0);
        final Plan enclosed = wing == 0 ? null
                : Plan.of("enclosed wing", Grid.of("enclosed wing", -2400, -1600, wing * BAY, 3200, BAY));
        return new Tower("void deck tower", List.of(
                new Section("lower", plan, floor, 380),
                Section.open("void deck", plan, height, enclosed),
                new Section("upper", plan, DECK_TOWER_FLOORS - floor - 1, 380)));
    }

    /** All of them. */
    public static List<Tower> all() {
        return List.of(setback(), round(), oval(), rounded(), lBlock(), bowFront(), voidDeck());
    }
}
