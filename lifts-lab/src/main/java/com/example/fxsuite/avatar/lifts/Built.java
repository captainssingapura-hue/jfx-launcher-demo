package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.List;

/**
 * What a scene comes to, set as it is: its frame (all of it that is drawn, and what of it is in the way), what moves in
 * it, what each key it is drawn by draws ({@code keyed}: the frame first, then each kind of moving part, in the order
 * their movers are keyed from {@link SceneWorld#KEY}); its figures read out; where the avatar starts, where vehicles are
 * parked from (in a row across, facing across) and where people stand from (in a row across, facing as it says); how
 * far its ground reaches; and where to look at it from: its middle, and how big it is.
 *
 * @param title    what it is called
 * @param figures  what it comes to, read out
 * @param drawn    its frame, all of it drawn
 * @param standing what of its frame is in the way
 * @param movers   what moves in it
 * @param keyed    what each of its keys draws, from {@link SceneWorld#KEY} on
 * @param start    where the avatar starts
 * @param parking  where the first vehicle is parked, the rest in a row across from it
 * @param people   where the first person stands, the rest in a row across from them
 * @param ground   how far its ground reaches every way from its middle
 * @param middle   where its middle is: across, up, along
 * @param size     how big it is, at most, any way
 * @param yaw      which way round it is best seen from, degrees: its front toward the camera
 */
record Built(String title, String figures, List<Solid> drawn, List<Solid> standing, List<Mover> movers,
             List<List<Solid>> keyed, Spot start, Place parking, Place people, double ground, double[] middle,
             double size, double yaw) {

    Built {
        drawn = List.copyOf(drawn);
        standing = List.copyOf(standing);
        movers = List.copyOf(movers);
        keyed = List.copyOf(keyed);
    }
}
