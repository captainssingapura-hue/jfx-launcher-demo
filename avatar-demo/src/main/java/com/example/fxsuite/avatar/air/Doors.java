package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Door;
import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A door ({@link Door}) on an aircraft: each of its leaves, its frame and its infill, on a hinge of
 * its own ({@link Hinge}) at its jamb, turned by one control, so a pair opens together; its skeleton
 * its frame's, turning with it.
 */
public final class Doors {

    private Doors() {
    }

    /**
     * {@code door}, in its own frame (across its width, out of its face along, up), shut at 0°, opened
     * by {@code control} up to its most: its frame {@code frame}, its infill {@code infill}.
     */
    public static Craft of(final Door door, final String control, final Paint frame, final Paint infill) {
        final Craft.Builder built = Craft.named(door.name());
        for (int k = 0; k < door.leaves(); k++) {
            final List<Piece> pieces = new ArrayList<>();
            for (final Door.Member member : door.members(k)) {
                pieces.add(new Piece(member.name(), frame, door.corners(k, member, door.thick(), 0)));
            }
            int panel = 0;
            for (final List<Point> corners : door.infills(k)) {
                pieces.add(new Piece("infill " + panel++, infill, door.corners(k, corners, Door.INFILL * door.thick(), 0)));
            }
            final Craft leaf = new Craft(door.name() + " leaf " + k, List.of(), pieces, List.of(), List.of(), List.of(),
                    List.of(door.skeleton(k, 0)));
            final Hinge hinge = new Hinge(control, new Point(door.hinge(k), 0, 0), new Point(0, 0, 1), door.sense(k), 0,
                    door.most(), leaf);
            built.with(new Craft(leaf.name(), List.of(), List.of(), List.of(), List.of(hinge)), Mount.at(Point.ZERO));
        }
        return built.build();
    }
}
