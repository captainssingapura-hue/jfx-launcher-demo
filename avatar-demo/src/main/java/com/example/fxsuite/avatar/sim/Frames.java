package com.example.fxsuite.avatar.sim;

import java.util.List;

import com.example.fxsuite.avatar.sim.Frame.Kind;
import com.example.fxsuite.avatar.sim.Frame.Node;
import com.example.fxsuite.avatar.sim.Frame.Rod;
import com.example.fxsuite.avatar.sim.Frame.Tube;

/**
 * The frames things are built on, as rods ({@link Frame}): a motorcycle's, as a real one is built,
 * of steel tubes welded together. Each in its owner's own frame: {@code across} to its right,
 * {@code along} ahead from its middle, {@code up} from the ground.
 */
public final class Frames {

    private Frames() {
    }

    // the boxer's frame's nodes, by name
    private static final int HEAD_TOP = 0;
    private static final int HEAD_FOOT = 1;
    private static final int SPINE_L = 2;
    private static final int SPINE_R = 3;
    private static final int SEAT_L = 4;
    private static final int SEAT_R = 5;
    private static final int TAIL_L = 6;
    private static final int TAIL_R = 7;
    private static final int DOWN_L = 8;
    private static final int DOWN_R = 9;
    private static final int CRADLE_FRONT_L = 10;
    private static final int CRADLE_FRONT_R = 11;
    private static final int CRADLE_BACK_L = 12;
    private static final int CRADLE_BACK_R = 13;
    private static final int PIVOT_L = 14;
    private static final int PIVOT_R = 15;
    private static final int AXLE_L = 16;
    private static final int AXLE_R = 17;

    /**
     * The big boxer's frame, a double cradle, as such a motorcycle's is: a head tube for the forks,
     * raked back as they are; a twin backbone back from it under the tank to the seat rails, which
     * run on out to the tail; two down tubes from the head, in front of the cylinders, down into a
     * cradle under the engine, low between the wheels, which rises behind it to the swing arm's
     * pivot; struts up from the pivot to the seat rails, and stays out to the tail, so the back of
     * it is braced; tubes across at each pair; and the swing arm, back from the pivot to the back
     * axle on either side of the back wheel, the right arm the thicker, the shaft's housing. The
     * frame 28 mm tube; the head tube and the swing arm heavier.
     */
    public static final Frame BOXER = new Frame("the boxer's frame", List.of(
            new Node("head tube, top", 0, 24, 80),
            new Node("head tube, foot", 0, 38, 70),
            new Node("backbone, left", -9, 4, 66),
            new Node("backbone, right", 9, 4, 66),
            new Node("seat rail, left, front", -10, -40, 66),
            new Node("seat rail, right, front", 10, -40, 66),
            new Node("tail, left", -13, -100, 68),
            new Node("tail, right", 13, -100, 68),
            new Node("down tube, left, foot", -12, 46, 34),
            new Node("down tube, right, foot", 12, 46, 34),
            new Node("cradle, left, front", -12, 40, 10),
            new Node("cradle, right, front", 12, 40, 10),
            new Node("cradle, left, back", -12, -18, 10),
            new Node("cradle, right, back", 12, -18, 10),
            new Node("swing arm pivot, left", -12, -24, 34, Kind.HINGE),
            new Node("swing arm pivot, right", 12, -24, 34, Kind.HINGE),
            new Node("back axle, left", -14, -85, 32),
            new Node("back axle, right", 14, -85, 32)),
            List.of(
                    // the head tube, for the forks
                    new Rod(HEAD_TOP, HEAD_FOOT, new Tube(2.2, 0.3)),
                    // the backbone, under the tank, and the seat rails on out to the tail
                    new Rod(HEAD_TOP, SPINE_L), new Rod(HEAD_TOP, SPINE_R),
                    new Rod(SPINE_L, SEAT_L), new Rod(SPINE_R, SEAT_R),
                    new Rod(SEAT_L, TAIL_L), new Rod(SEAT_R, TAIL_R),
                    // the down tubes, in front of the cylinders, and the cradle under the engine
                    new Rod(HEAD_FOOT, DOWN_L), new Rod(HEAD_FOOT, DOWN_R),
                    new Rod(DOWN_L, CRADLE_FRONT_L), new Rod(DOWN_R, CRADLE_FRONT_R),
                    new Rod(CRADLE_FRONT_L, CRADLE_BACK_L), new Rod(CRADLE_FRONT_R, CRADLE_BACK_R),
                    new Rod(CRADLE_BACK_L, PIVOT_L), new Rod(CRADLE_BACK_R, PIVOT_R),
                    // struts up from the pivot to the seat rails, and stays out to the tail
                    new Rod(PIVOT_L, SEAT_L), new Rod(PIVOT_R, SEAT_R),
                    new Rod(PIVOT_L, TAIL_L), new Rod(PIVOT_R, TAIL_R),
                    // tubes across, at each pair
                    new Rod(SPINE_L, SPINE_R), new Rod(SEAT_L, SEAT_R), new Rod(TAIL_L, TAIL_R),
                    new Rod(DOWN_L, DOWN_R), new Rod(CRADLE_FRONT_L, CRADLE_FRONT_R),
                    new Rod(CRADLE_BACK_L, CRADLE_BACK_R), new Rod(PIVOT_L, PIVOT_R, new Tube(1.6, 0.4)),
                    // the swing arm, either side of the back wheel: the right arm the shaft's housing
                    new Rod(PIVOT_L, AXLE_L, new Tube(2.4, 0.3)), new Rod(PIVOT_R, AXLE_R, new Tube(3.2, 0.35))),
            new Tube(1.4, 0.2));

    /** The frame a motorcycle built on {@code chassis} has, as rods; null for one not yet built so. */
    public static Frame of(final Course.Chassis chassis) {
        return chassis == Course.BOXER || chassis == Course.BOXER_SIDECAR || chassis == Course.BOXER_ICE_CREAM ? BOXER : null;
    }
}
