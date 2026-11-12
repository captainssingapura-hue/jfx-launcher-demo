package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Profiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bench's files: everything written, and the scenes moving from left to right. */
class RenderTest {

    @Test
    void everythingIsWritten(@TempDir final Path folder) throws IOException {
        final List<Path> written = Render.all(folder);
        assertTrue(written.size() > 70, "files: " + written.size());
        for (final Path file : written) {
            assertTrue(Files.size(file) > 44, file + " has sound in it");
        }
        assertTrue(written.contains(folder.resolve("arcade").resolve("scene-fly-by.wav")));
    }

    @Test
    void theWalkerAndTheJetsPassFromLeftToRight() {
        for (final float[] scene : List.of(Render.walkPast(Profiles.ROBOT), Render.flyBy(Profiles.ROBOT),
                Render.walkPast(Profiles.ARCADE), Render.flyBy(Profiles.ARCADE))) {
            final int frames = scene.length / 2;
            final int second = Clip.RATE;
            assertTrue(balance(scene, 0, second) < -0.3, "on the left at first: " + balance(scene, 0, second));
            assertTrue(balance(scene, frames - second, frames) > 0.3,
                    "on the right at last: " + balance(scene, frames - second, frames));
        }
    }

    /** -1 all left to 1 all right, by energy, over frames {@code from} to {@code to}. */
    private static double balance(final float[] stereo, final int from, final int to) {
        double left = 0;
        double right = 0;
        for (int f = from; f < to; f++) {
            left += stereo[2 * f] * stereo[2 * f];
            right += stereo[2 * f + 1] * stereo[2 * f + 1];
        }
        return (right - left) / (right + left);
    }
}
