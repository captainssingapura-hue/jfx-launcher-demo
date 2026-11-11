package com.example.fxsuite.avatar.sound;

/** How a loop plays just now: its gain, 0 to 1, and its pitch, 1 as made. */
public record Level(double gain, double pitch) {

    public static final Level OFF = new Level(0, 1);
}
