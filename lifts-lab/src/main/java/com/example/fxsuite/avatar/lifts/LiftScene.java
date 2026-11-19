package com.example.fxsuite.avatar.lifts;

import java.util.List;
import java.util.Map;

/**
 * A scene of the lifts bench: a lift of one kind (or all of them) in a building made for it, built as its knobs are
 * set: so many floors, so high; its cabins or its car or its steps so big; so fast.
 */
interface LiftScene {

    /** What it is called, to choose it by. */
    String name();

    /** What can be set about it, in order. */
    List<Knob> knobs();

    /** It, built as {@code set} says (by each knob's key; each knob not set as its own value). */
    Built built(Map<String, Double> set);

    /** It, built as its knobs are. */
    default Built built() {
        return built(Map.of());
    }

    /** The scenes there are: one of each kind of lift, and the yard with all of them. */
    static List<LiftScene> all() {
        return List.of(new HighRiseScene(), new PaternosterScene(), new ShuttleScene(), new EscalatorScene(),
                new YardScene());
    }
}
