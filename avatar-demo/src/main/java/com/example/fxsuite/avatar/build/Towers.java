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
     * a top 24 m square, 8 more. 154 m to its roof. Held up by a square core a bay across in its middle,
     * and its podium's grid of columns, each standing as high as the sections reach over it.
     */
    public static Tower setback() {
        final Plan podium = plan(Shape.SQUARE, 5600, 0, 0);
        return new Tower("setback tower", List.of(
                new Section("podium", podium, 2, 500),
                new Section("shaft", plan(Shape.SQUARE, 4000, 0, 0), 28, 400),
                new Section("top", plan(Shape.SQUARE, 2400, 0, 0), 8, 400)),
                Frame.with(podium, 90, List.of(Frame.rectangle(0, 0, BAY, BAY))));
    }

    /**
     * Round: 40 m across, 45 floors of 3.8 m. Held up by a square core 12 m across in its middle and a
     * ring of sixteen round columns in behind its glass: nothing between them, open all round.
     */
    public static Tower round() {
        final Plan plan = plan(Shape.CIRCLE, 4000, 0, 0);
        return new Tower("round tower", List.of(new Section("round tower", plan, 45, 380)),
                new Frame(List.of(Frame.rectangle(0, 0, 1200, 1200)), Frame.ring(0, 0, 1720, 16, 110)));
    }

    /** Oval: 48 m by 32 m, a little fuller than an ellipse, 40 floors of 3.8 m; an octagonal core, its grid's columns. */
    public static Tower oval() {
        final Plan plan = plan(Shape.OVAL, 4800, 3200, 2.5);
        return new Tower("oval tower", List.of(new Section("oval tower", plan, 40, 380)),
                Frame.with(plan, 80, List.of(Frame.octagon(0, 0, 1600, 1000, 250))));
    }

    /** A rectangle 48 m by 32 m, its corners rounded 8 m, 36 floors of 3.8 m; two cores side by side, its grid's columns. */
    public static Tower rounded() {
        final Plan plan = plan(Shape.ROUNDED, 4800, 3200, 800);
        return new Tower("rounded tower", List.of(new Section("rounded tower", plan, 36, 380)),
                Frame.with(plan, 80, List.of(Frame.rectangle(-1200, 0, BAY, 1200), Frame.rectangle(1200, 0, BAY, 1200))));
    }

    /**
     * An L-shaped block, 48 m by 40 m, its arms 16 m, 16 floors of 3.6 m, on a podium a bay wider all
     * round, 64 m by 56 m (a bay, not half of one: its lines are the block's), two floors of 5 m. Its
     * core in the bay in the inside corner where its arms meet, on the grid; its podium's grid of columns.
     */
    public static Tower lBlock() {
        final Plan podium = plan(Shape.RECTANGLE, 6400, 5600, 0);
        return new Tower("L block on a podium", List.of(
                new Section("podium", podium, 2, 500),
                new Section("block", plan(Shape.L_SHAPE, 4800, 4000, 1600), 16, 360)),
                Frame.with(podium, 70, List.of(Frame.rectangle(-1200, -800, BAY, BAY))));
    }

    /** A rectangle 48 m by 32 m, a half round bow of 12 m out of its front, 30 floors of 3.8 m; a core in its middle. */
    public static Tower bowFront() {
        final Plan plan = plan(Shape.BOW, 4800, 3200, 1200);
        return new Tower("bow-front tower", List.of(new Section("bow-front tower", plan, 30, 380)),
                Frame.with(plan, 80, List.of(Frame.rectangle(0, 0, 2 * BAY, BAY))));
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
                new Section("upper", plan, DECK_TOWER_FLOORS - floor - 1, 380)),
                Frame.with(plan, 70, List.of(Frame.rectangle(0, 0, 2 * BAY, BAY))));
    }

    /**
     * {@code tower} with a void deck in its section {@code s}, over that section's floor
     * {@code floor} (counted from 1): the section split there, its floors under the deck keeping its
     * name, those over it named so ({@code "... above"}), the deck {@code height} high between them,
     * on the same plan; open all through ({@code enclosed} null, a full deck) or but for
     * {@code enclosed}, a plan of its own on the section's grid (a partial one). Its frame the
     * tower's: the same columns and cores, carried on through the deck.
     */
    public static Tower withDeck(final Tower tower, final int s, final int floor, final double height,
                                 final Plan enclosed) {
        final Section section = tower.sections().get(s);
        if (section.open() || floor < 1 || floor > section.floors() - 2) {
            throw new IllegalArgumentException(tower.name() + ": no void deck over floor " + floor + " of its "
                    + section.name() + ", " + section.floors() + " floors");
        }
        final List<Section> sections = new java.util.ArrayList<>(tower.sections().subList(0, s));
        sections.add(new Section(section.name(), section.plan(), floor, section.height()));
        sections.add(Section.open("void deck", section.plan(), height, enclosed));
        sections.add(new Section(section.name() + " above", section.plan(), section.floors() - floor - 1,
                section.height()));
        sections.addAll(tower.sections().subList(s + 1, tower.sections().size()));
        return new Tower(tower.name(), sections, tower.structure());
    }

    /** An enclosed part for a void deck: a rectangle of the grid, its corner at ({@code x}, {@code y}), so wide and deep. */
    public static Plan enclosed(final double x, final double y, final double width, final double depth) {
        return Plan.of("enclosed part", Grid.of("enclosed part", x, y, width, depth, BAY));
    }

    /** All of them. */
    public static List<Tower> all() {
        return List.of(setback(), round(), oval(), rounded(), lBlock(), bowFront(), voidDeck());
    }
}
