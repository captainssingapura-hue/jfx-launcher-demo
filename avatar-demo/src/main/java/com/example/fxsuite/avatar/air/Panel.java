package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.List;

/**
 * A flat face of an aircraft's skin, between lines of its skeleton ({@link Lines}): three or four
 * corners, in order round it. Drawn flat, as one facet.
 *
 * @param name    which it is: its band, and its section
 * @param paint   what it is finished in
 * @param corners its corners, round it
 */
public record Panel(String name, Paint paint, List<Point> corners) {

    public Panel {
        corners = List.copyOf(corners);
        if (corners.size() < 3 || corners.size() > 4) {
            throw new IllegalArgumentException(name + ": a panel of " + corners.size() + " corners");
        }
    }

    /** The same, mounted ({@link Mount}). */
    public Panel placed(final Mount mount) {
        return new Panel(name, paint, corners.stream().map(mount::place).toList());
    }

    /** The same on the other side: across turned to the left. */
    public Panel mirrored() {
        return new Panel(name, paint, corners.stream().map(Skin::mirror).toList());
    }
}
