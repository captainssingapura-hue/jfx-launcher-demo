package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.sim.Place;

/**
 * Somewhere in a world played from a bench a guest can be put ({@link PlayWorld}): a lane's start, a building's door, a
 * floor of a tower; where it is, how high what is stood on there is, and which way it faces ({@code place}: its
 * height that of what is stood on, not of who stands there).
 *
 * @param name  what it is called, to choose it by
 * @param place where it is, how high its footing, which way it faces
 */
public record Spot(String name, Place place) {
}
