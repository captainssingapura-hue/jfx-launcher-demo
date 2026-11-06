package com.example.fxsuite.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A handler written to the registry must not name a folder the next JDK update removes. */
class StableJavaTest {

    @TempDir Path java;

    private Path javaw(String... folders) throws Exception {
        Path bin = java;
        for (String f : folders) bin = bin.resolve(f);
        bin = bin.resolve("bin");
        Files.createDirectories(bin);
        return Files.createFile(bin.resolve("javaw.exe"));
    }

    @Test
    void prefersTheLinkThatFollowsUpdates() throws Exception {
        Path versioned = javaw("jdk-25.0.5").getParent().getParent();
        Path stable = javaw("latest", "jdk-25");
        assertEquals(stable, StableJava.javaw(versioned, 25));
    }

    @Test
    void neverCrossesToADifferentFeatureRelease() throws Exception {
        Path versioned = javaw("jdk-25.0.5").getParent().getParent();
        javaw("latest", "jdk-26");
        assertEquals(versioned.resolve("bin").resolve("javaw.exe"), StableJava.javaw(versioned, 25));
    }

    @Test
    void fallsBackToTheRunningJavaWhenThereIsNoLink() throws Exception {
        Path home = javaw("temurin-25").getParent().getParent();
        assertEquals(home.resolve("bin").resolve("javaw.exe"), StableJava.javaw(home, 25));
    }

    @Test
    void aJavaAlreadyStartedThroughTheLinkStaysOnIt() throws Exception {
        Path stable = javaw("latest", "jdk-25");
        Path home = stable.getParent().getParent();
        assertEquals(stable, StableJava.javaw(home, 25));
    }
}
