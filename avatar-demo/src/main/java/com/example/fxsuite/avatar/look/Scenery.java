package com.example.fxsuite.avatar.look;

import javafx.scene.Node;

/**
 * What a demo draws besides the course's pieces ({@link DrawnDemo#scenery}): its node, in the world's frame; and, each
 * frame, told what the camera sees, so it may draw only what is in view and haze what is far off.
 */
public interface Scenery {

    /** What is drawn. */
    Node node();

    /** What {@code view} sees, each frame: nothing to do, unless it does something with it. */
    default void seenFrom(final View view) {
    }

    /** {@code node}, as it is, whatever is seen. */
    static Scenery still(final Node node) {
        return () -> node;
    }
}
