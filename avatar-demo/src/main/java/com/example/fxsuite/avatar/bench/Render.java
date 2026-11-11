package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.audio.Mix;
import com.example.fxsuite.avatar.audio.Outputs;
import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Level;
import com.example.fxsuite.avatar.sound.Profile;
import com.example.fxsuite.avatar.sound.Profiles;
import com.example.fxsuite.avatar.sound.Sound;
import com.example.fxsuite.avatar.sound.Wav;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The bench without a window, or a sound card: every sound of every profile to a WAV file, a
 * folder each, with two little scenes mixed in stereo the way the Java Sound output mixes them (footsteps walking past,
 * the jets flying by), and a check of which outputs this machine can open.
 *
 * <pre>java -jar avatar-demo.jar --sounds [folder]</pre>
 */
public final class Render {

    /** Takes of each sound with a number: soft, middling, hard. */
    private static final double[] AMOUNTS = {0.2, 0.6, 1.0};

    private Render() {
    }

    public static void main(final String[] args) throws IOException {
        final Path folder = Path.of(args.length > 0 ? args[0] : "sounds");
        final List<Path> written = all(folder);
        System.out.println("wrote " + written.size() + " files to " + folder.toAbsolutePath()
                + ", a folder for each profile: " + Profiles.all().stream().map(Profile::id).toList());
        for (final Speakers.Kind kind : Speakers.Kind.values()) {
            System.out.println("  " + Outputs.probe(kind));
        }
    }

    /** Writes every profile, each into a folder of its own in {@code folder}; returns the files. */
    public static List<Path> all(final Path folder) throws IOException {
        final List<Path> written = new ArrayList<>();
        for (final Profile profile : Profiles.all()) {
            written.addAll(all(profile, folder.resolve(profile.id())));
        }
        return written;
    }

    /** Writes one profile into {@code folder}, made if need be; returns the files. */
    public static List<Path> all(final Profile profile, final Path folder) throws IOException {
        Files.createDirectories(folder);
        final List<Path> written = new ArrayList<>();
        for (final Sound sound : Sound.values()) {
            if (sound.loop()) {
                written.add(write(folder, profile.render(sound, 1, 0)));
                continue;
            }
            final double[] amounts = sound.amount() == null ? new double[] {1} : AMOUNTS;
            final int variants = sound == Sound.STEP_GROUND || sound == Sound.STEP_BLOCK ? 3 : 1;
            for (final double amount : amounts) {
                for (int v = 0; v < variants; v++) {
                    written.add(write(folder, profile.render(sound, amount, v)));
                }
            }
        }
        final Path walk = folder.resolve("scene-walk-past.wav");
        Wav.write(walk, walkPast(profile), 2);
        written.add(walk);
        final Path fly = folder.resolve("scene-fly-by.wav");
        Wav.write(fly, flyBy(profile), 2);
        written.add(fly);
        return written;
    }

    /** Someone walking past, 2 m ahead, from left to right: footsteps in stereo. */
    static float[] walkPast(final Profile profile) {
        final Mix mix = new Mix();
        final double seconds = 4.5;
        final double pace = 0.42;          // a footfall every 0.42 s, at a walk
        return scene(mix, seconds, (t, last) -> {
            final int now = (int) Math.floor(t / pace);
            final int before = (int) Math.floor(last / pace);
            if (now != before || last < 0) {
                final double x = -900 + 1800 * t / seconds;
                final double foot = now % 2 == 0 ? -12 : 12;
                mix.play(profile.render(Sound.STEP_GROUND, 0.4, now), 1, 1, new Vec3(x + foot, 0, 200));
            }
        });
    }

    /** The jets crossing from left to right, 6 m ahead, at a good clip. */
    static float[] flyBy(final Profile profile) {
        final Mix mix = new Mix();
        final double seconds = 5;
        final double thrust = 0.85;
        final List<Mix.Voice> jets = new ArrayList<>();
        final Sound[] layers = {Sound.JET_RUMBLE, Sound.JET_HISS, Sound.JET_WHINE};
        for (int i = 0; i < layers.length; i++) {
            final Mix.Voice jet = mix.loop(profile.render(layers[i], 1, 0));
            final Level level = profile.level(layers[i], thrust, 0);
            jet.set(level.gain(), level.pitch());
            jets.add(jet);
        }
        return scene(mix, seconds, (t, last) -> {
            final Vec3 at = new Vec3(-4000 + 8000 * t / seconds, -300, 600);
            jets.forEach(jet -> jet.place(at));
        });
    }

    /** What changes in a scene, each block: {@code t} now, {@code last} the block before (-1 at first). */
    private interface Step {
        void at(double t, double last);
    }

    private static float[] scene(final Mix mix, final double seconds, final Step step) {
        final int block = 512;
        final int frames = (int) Math.round(seconds * Clip.RATE);
        final float[] out = new float[frames * 2];
        final float[] chunk = new float[block * 2];
        double last = -1;
        for (int f = 0; f < frames; f += block) {
            final double t = (double) f / Clip.RATE;
            step.at(t, last);
            last = t;
            mix.render(chunk, block);
            System.arraycopy(chunk, 0, out, 2 * f, Math.min(block, frames - f) * 2);
        }
        return out;
    }

    private static Path write(final Path folder, final Clip clip) throws IOException {
        final Path file = folder.resolve(clip.name().toLowerCase(Locale.ROOT) + ".wav");
        Wav.write(file, clip);
        return file;
    }
}
