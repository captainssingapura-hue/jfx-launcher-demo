package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

import java.util.Locale;

/**
 * Where the sound comes out: one face over OpenAL, Java Sound, or nothing at all, so the rest
 * of the program neither knows nor cares which it got ({@link Outputs#open} picks, falling
 * back as it must).
 *
 * <p>Positions are the simulation's ({@link Vec3}: y down). A sound with no position plays at
 * the listener: in both ears equally, at full loudness. Not thread-safe: use it from one
 * thread, the one that runs the frames.
 */
public interface Speakers extends AutoCloseable {

    /** The kinds of output, in the order they are tried. */
    enum Kind {
        OPENAL("OpenAL"), JAVA_SOUND("Java Sound"), SILENT("Silent");

        private final String title;

        Kind(final String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        /** From {@code -Davatar.audio=openal|javasound|silent}; OpenAL if unset. */
        public static Kind fromSettings() {
            final String asked = System.getProperty("avatar.audio", "openal")
                    .toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            return switch (asked) {
                case "javasound", "java" -> JAVA_SOUND;
                case "silent", "none", "off" -> SILENT;
                default -> OPENAL;
            };
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /** A sound that plays round and round until stopped; it starts silent. */
    interface Loop {
        void set(double gain, double pitch);

        /** Puts it somewhere in the world; until then it plays at the listener. */
        void place(Vec3 at);

        void stop();
    }

    Kind kind();

    /** The device it plays on, as the system names it. */
    String device();

    /** Where the listener is, and which way it faces. */
    void listen(Vec3 at, double yaw);

    /** The overall loudness, 0 to 1. */
    void volume(double volume);

    /** Plays a clip once, at the listener. {@code pitch} 1 as made, 2 an octave up and twice as quick. */
    void play(Clip clip, double gain, double pitch);

    /** Plays a clip once, from a place in the world. */
    void play(Clip clip, double gain, double pitch, Vec3 at);

    Loop loop(Clip clip);

    /** Stops everything and lets the device go. */
    @Override
    void close();
}
