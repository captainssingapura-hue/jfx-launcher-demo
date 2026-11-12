package com.example.fxsuite.avatar.sound;

import java.util.List;
import java.util.Locale;

/** The sound profiles there are, and the one the settings ask for. */
public final class Profiles {

    public static final Profile ROBOT = new Robot();
    public static final Profile ARCADE = new Arcade();

    private static final List<Profile> ALL = List.of(ROBOT, ARCADE);

    private Profiles() {
    }

    public static List<Profile> all() {
        return ALL;
    }

    /** By its id, ignoring case; the robot if there is none by that name. */
    public static Profile named(final String id) {
        final String wanted = id == null ? "" : id.strip().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(p -> p.id().equals(wanted)).findFirst().orElse(ROBOT);
    }

    /** From {@code -Davatar.sounds=robot|arcade}; the robot if unset. */
    public static Profile fromSettings() {
        return named(System.getProperty("avatar.sounds"));
    }
}
