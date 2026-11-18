package com.example.fxsuite.avatar.worlds;

import com.example.fxsuite.avatar.build.MarinaBay;
import com.example.fxsuite.avatar.look.DrawnDemo;
import com.example.fxsuite.avatar.look.RoadScenery;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;

/**
 * Marina Bay as a demo of its own ({@link MarinaBay}): its land and its water, what stands on it, who is about, where
 * the avatar starts, its ground not ruled; its roads laid as roads are laid now, stood on, drawn in stretches as they
 * are seen ({@link RoadScenery}). Known by its name, {@code MARINA_BAY}, its runs kept and replayed.
 */
public final class Bay implements DrawnDemo {

    /** The bay. */
    public static final Bay DEMO = new Bay();

    private Bay() {
    }

    @Override
    public String name() {
        return "MARINA_BAY";
    }

    @Override
    public String title() {
        return "Marina Bay";
    }

    @Override
    public String description() {
        return "The bay as it lies, round the three towers and the park on their roofs: its land, its sea, the bay"
                + " and the river's mouth behind the barrage, the gardens' lakes, its roads and their bridges."
                + " Buildings to come.";
    }

    @Override
    public double startYaw() {
        return 90;
    }

    @Override
    public boolean recordable() {
        return true;
    }

    @Override
    public void lay(final Course.Layout world) {
        MarinaBay.lay(world);
    }

    /** Its roads' pavements and markings, each drawn once where one lies over another at one level, as they are seen. */
    @Override
    public Scenery scenery() {
        return RoadScenery.of(MarinaBay.roads());
    }
}
