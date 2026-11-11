package com.example.fxsuite.avatar.hear;

import com.example.fxsuite.avatar.audio.Outputs;
import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sim.Avatar;
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

    private record Key(String profile, Sound sound, int strength, int take) {
    }

    private final Speakers speakers;
    private final List<String> notes;
    private final Map<Key, Clip> clips = new HashMap<>();
    private final Map<Sound, Integer> takes = new EnumMap<>(Sound.class);
    private final Map<Sound, Speakers.Loop> loops = new EnumMap<>(Sound.class);
    private Profile profile;
    private boolean muted;

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

    /** One frame: the listener moved, this frame's sounds played, the loops steered. */
    public void hear(final Avatar avatar, final Chase chase) {
        speakers.listen(chase.pivot(), chase.yaw());
        final Cues.Heard heard = Cues.of(avatar, profile);
        for (final Cues.Play play : heard.plays()) {
            final Clip clip = clip(play.sound(), play.amount(), nextTake(play.sound()));
            if (play.at() == null) {
                speakers.play(clip, 1, 1);
            } else {
                speakers.play(clip, 1, 1, play.at());
            }
        }
        for (final Cues.Hum hum : heard.hums()) {
            final Speakers.Loop loop = loops.computeIfAbsent(hum.loop(), s -> speakers.loop(clip(s, 1, 0)));
            loop.set(hum.level().gain(), hum.level().pitch());
            loop.place(hum.at());
        }
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
