package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;

import java.util.List;

/**
 * What a bench builds to be played in ({@link Played}): its land, what stands on it and is stood on, its water, what it
 * draws besides; and where guests can be put in it ({@link Spot}): where the avatar starts, where vehicles are parked,
 * where people stand. Who comes is not its to say: the bench's guests are chosen apart from it and put on its spots.
 */
public interface PlayWorld {

    /** What it is called, in the game window's title. */
    String title();

    /** What there is in it, in a line. */
    String description();

    /** Its world laid into a course: its land, what stands there, what is stood on, its water. Not who comes, nor where. */
    void lay(Course.Layout world);

    /** What it draws besides its pieces, its land and its water. */
    Scenery scenery();

    /** Where the avatar starts, unless asked to start elsewhere. */
    Spot start();

    /**
     * Where vehicles so long ({@code lengths}, cm, nose to tail, in order) are parked: one spot each, in order, with
     * room between them.
     */
    List<Spot> parked(List<Double> lengths);

    /** Where {@code count} people stand about. */
    List<Spot> standing(int count);
}
