package com.example.fxsuite.avatar;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Keeps the layers apart, by what each file imports: the simulation, the viewpoint and the
 * sound are pure; only the device layer touches a sound library, and only one file of it
 * touches OpenAL, so the rest runs without it.
 */
class LayersTest {

    private static final Path ROOT = Path.of("src/main/java/com/example/fxsuite/avatar");
    private static final String BASE = "com.example.fxsuite.avatar.";

    @Test
    void eachLayerImportsOnlyWhatItMay() throws IOException {
        final List<String> wrong = new ArrayList<>();
        forbid(wrong, "sim", "javafx.", "javax.sound", "org.lwjgl", BASE + "look", BASE + "view",
                BASE + "sound", BASE + "audio", BASE + "bench");
        forbid(wrong, "view", "javafx.", "javax.sound", "org.lwjgl", BASE + "look", BASE + "sound",
                BASE + "audio", BASE + "bench");
        forbid(wrong, "sound", "javafx.", "javax.sound", "org.lwjgl", BASE + "look", BASE + "view",
                BASE + "audio", BASE + "bench");
        forbid(wrong, "audio", "javafx.", BASE + "look", BASE + "view", BASE + "bench");
        forbid(wrong, "look", "javax.sound", "org.lwjgl", BASE + "sound", BASE + "audio", BASE + "bench");
        assertEquals(List.of(), wrong);
    }

    @Test
    void onlyTheOpenAlOutputTouchesOpenAl() throws IOException {
        final List<String> users = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT)) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (Files.readString(file).contains("import org.lwjgl")) {
                    users.add(file.getFileName().toString());
                }
            }
        }
        assertEquals(List.of("OpenAlSpeakers.java"), users);
    }

    private static void forbid(final List<String> wrong, final String layer, final String... prefixes)
            throws IOException {
        try (Stream<Path> files = Files.list(ROOT.resolve(layer))) {
            for (final Path file : files.toList()) {
                for (final String line : Files.readAllLines(file)) {
                    final String text = line.strip();
                    if (!text.startsWith("import ")) {
                        continue;
                    }
                    final String imported = text.replaceFirst("^import (static )?", "");
                    for (final String prefix : prefixes) {
                        if (imported.startsWith(prefix)) {
                            wrong.add(layer + "/" + file.getFileName() + " imports " + imported);
                        }
                    }
                }
            }
        }
    }
}
