package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.bench.Render;
import com.example.fxsuite.avatar.bench.SoundBench;

import java.util.Arrays;

/**
 * Plain entry point. Launching the JavaFX application class directly from the classpath
 * fails with "JavaFX runtime components are missing", so the jar starts here.
 *
 * <p>With no arguments, the demo. {@code --bench} opens the sound bench instead, and
 * {@code --sounds [folder]} writes every sound to WAV files, with no window at all.
 */
public final class Main {

    public static void main(final String[] args) throws Exception {
        final String[] rest = args.length > 0 ? Arrays.copyOfRange(args, 1, args.length) : args;
        if (args.length > 0 && args[0].equals("--bench")) {
            javafx.application.Application.launch(SoundBench.class, rest);
        } else if (args.length > 0 && args[0].equals("--sounds")) {
            Render.main(rest);
        } else {
            javafx.application.Application.launch(AvatarDemo.class, args);
        }
    }
}
