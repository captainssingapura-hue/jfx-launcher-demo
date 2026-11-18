package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Demo;

import javafx.scene.Node;

/**
 * A demo with more to draw than the course's pieces: a test section's pavements, markings and structures, drawn by the
 * roads' own drawing. What it lays is what stands in the way and what is stood on; what it draws ({@link #scenery}) is
 * what is seen. The game adds it to the world it draws, as it is, in the world's frame.
 */
public interface DrawnDemo extends Demo {

    /** What it draws besides the course's pieces, made afresh each time it is asked for. */
    Node scenery();
}
