package com.example.fxsuite.avatar.hear;

import com.example.fxsuite.avatar.audio.Outputs;
import com.example.fxsuite.avatar.audio.Space;
import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Cues;
import com.example.fxsuite.avatar.sound.Profile;
import com.example.fxsuite.avatar.sound.Profiles;
import com.example.fxsuite.avatar.sound.Sound;
import com.example.fxsuite.avatar.view.Chase;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The game's sound: what {@code look} is to the graphics. Each frame it hears the avatar
 * ({@link Cues} says what), plays it on the speakers, and steers the loops, with the listener
 * where the player is.
 *
 * <p>The listener stands at the avatar, as a third-person game's usually does, but faces the
 * way the camera does. So the avatar always sounds near, however far the camera is pulled
 * back, and left and right match the screen.
 *
 * <p>Clips are made once and kept: a few takes of each sound, each at a few strengths, so
 * nothing is made mid-frame twice, and footsteps in a row differ.
 */
public final class Hearing implements AutoCloseable {

    /** Strengths each sound is kept at: 0, a quarter, a half, three quarters, full. */
    private static final int STRENGTHS = 4;
    /** Takes kept of each sound, played in turn. */
    private static final int TAKES = 4;
    private static final double VOLUME = 0.8;
    /** How quickly the loops come back up after an explosion pushed them down: a time constant, in seconds. */
    private static final double DUCK_BACK = 0.45;

    private record Key(String profile, Sound sound, int strength, int take) {
    }

    private final Speakers speakers;
    private final List<String> notes;
    private final Map<Key, Clip> clips = new HashMap<>();
    private final Map<Sound, Integer> takes = new EnumMap<>(Sound.class);
    private final Map<Sound, Speakers.Loop> loops = new EnumMap<>(Sound.class);
    private Profile profile;
    private boolean muted;
    /** How far down the loops are pushed, by the latest explosion ({@link Sound#duck}); and when last heard. */
    private double ducked;
    private double lastTime = Double.NaN;

    Hearing(final Speakers speakers, final List<String> notes, final Profile profile) {
        this.speakers = speakers;
        this.notes = List.copyOf(notes);
        this.profile = profile;
        speakers.volume(VOLUME);
    }

    /**
     * Opens the output and profile the settings ask for ({@code -Davatar.audio},
     * {@code -Davatar.sounds}). An unattended run is silent unless an output is named.
     */
    public static Hearing open(final boolean unattended) {
        final Speakers.Kind kind = unattended && System.getProperty("avatar.audio") == null
                ? Speakers.Kind.SILENT : Speakers.Kind.fromSettings();
        final Outputs.Opened opened = Outputs.open(kind);
        opened.notes().forEach(note -> System.out.println("sound: " + note));
        return new Hearing(opened.speakers(), opened.notes(), Profiles.fromSettings());
    }

    /** As {@link #hear(Avatar, Chase, List)}, with no airships about. */
    public void hear(final Avatar avatar, final Chase chase) {
        hear(avatar, chase, List.of());
    }

    /**
     * One frame: the listener moved, this frame's sounds played, the loops steered; the nearest
     * of the {@code airships} still in the sky droning where it hangs.
     */
    public void hear(final Avatar avatar, final Chase chase, final List<Vec3> airships) {
        final double time = avatar.body().time();
        final double seconds = Double.isNaN(lastTime) ? 0 : Math.max(0, time - lastTime);
        lastTime = time;
        // the loops come back up after a blast
        ducked *= Math.exp(-seconds / DUCK_BACK);

        speakers.listen(chase.pivot(), chase.yaw());
        final Cues.Heard heard = Cues.of(avatar, profile, airships);
        for (final Cues.Play play : heard.plays()) {
            final Sound sound = play.sound();
            final Clip clip = clip(sound, play.amount(), nextTake(sound));
            if (play.at() == null) {
                speakers.play(clip, 1, 1);
            } else {
                // a blast carries further than a footstep
                speakers.play(clip, 1, 1, play.at(), sound.reach());
            }
            if (sound.duck() > 0) {
                final double heardAt = play.at() == null ? 1
                        : Space.fade(play.at().distance(chase.pivot()), sound.reach());
                ducked = Math.max(ducked, sound.duck() * heardAt);
            }
        }
        for (final Cues.Hum hum : heard.hums()) {
            final Speakers.Loop loop = loops.computeIfAbsent(hum.loop(), s -> speakers.loop(clip(s, 1, 0)));
            loop.set(hum.level().gain() * (1 - ducked), hum.level().pitch());
            loop.place(hum.at());
        }
    }

    /** How far down the loops are pushed now, 0 to 1, by the latest blast. */
    double ducked() {
        return ducked;
    }

    /** Silences everything, or brings it back. */
    public void toggleMute() {
        muted = !muted;
        speakers.volume(muted ? 0 : VOLUME);
    }

    /** Changes to the next profile; the loops start again in it on the next frame. */
    public void nextProfile() {
        final List<Profile> all = Profiles.all();
        profile = all.get((all.indexOf(profile) + 1) % all.size());
        loops.values().forEach(Speakers.Loop::stop);
        loops.clear();
    }

    public Profile profile() {
        return profile;
    }

    public boolean muted() {
        return muted;
    }

    public Speakers.Kind output() {
        return speakers.kind();
    }

    /** Why any output tried first did not open. */
    public List<String> notes() {
        return notes;
    }

    @Override
    public void close() {
        loops.values().forEach(Speakers.Loop::stop);
        loops.clear();
        speakers.close();
    }

    private int nextTake(final Sound sound) {
        final int take = takes.getOrDefault(sound, 0);
        takes.put(sound, (take + 1) % TAKES);
        return take;
    }

    /** The clip for a sound at about this strength, made the first time it is wanted. */
    private Clip clip(final Sound sound, final double amount, final int take) {
        final int strength = (int) Math.round(Math.max(0, Math.min(1, amount)) * STRENGTHS);
        return clips.computeIfAbsent(new Key(profile.id(), sound, strength, take),
                key -> profile.render(sound, (double) strength / STRENGTHS, take));
    }
}
