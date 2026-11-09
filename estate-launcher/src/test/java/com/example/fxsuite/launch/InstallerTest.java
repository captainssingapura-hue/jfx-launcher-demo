package com.example.fxsuite.launch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerTest {

    @TempDir
    Path repository;
    @TempDir
    Path apps;

    private static final Coordinates APP = new Coordinates("com.example.acme", "ledger", "1.20261101.0", "");
    private static final Coordinates FX = new Coordinates("org.openjfx", "javafx-graphics", "25", "win");

    private void publish(Coordinates jar, String content) throws IOException {
        Path file = repository.resolve(jar.repositoryPath());
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test
    void copiesExactlyThePlanIntoTheAppsOwnDirectoryAndReusesIt() throws IOException {
        publish(APP, "ledger bytes");
        publish(FX, "javafx bytes");
        Path dir = apps.resolve("Ledger/1.20261101.0");

        Installer.Installation first = new Installer().install(List.of(repository), dir, List.of(APP, FX));
        assertEquals(List.of(dir.resolve("lib/ledger-1.20261101.0.jar"), dir.resolve("lib/javafx-graphics-25-win.jar")),
                first.jars());
        assertTrue(first.outcome().contains("first installation"), first.outcome());
        assertEquals("javafx bytes", Files.readString(dir.resolve("lib/javafx-graphics-25-win.jar")));
        assertEquals(2, Files.readAllLines(dir.resolve("installed.txt")).size());

        Installer.Installation again = new Installer().install(List.of(repository), dir, List.of(APP, FX));
        assertTrue(again.outcome().startsWith("reused"), again.outcome());
    }

    @Test
    void aModifiedJarIsReplacedFromTheRepository() throws IOException {
        publish(APP, "ledger bytes");
        Path dir = apps.resolve("Ledger/1.20261101.0");
        new Installer().install(List.of(repository), dir, List.of(APP));

        Files.writeString(dir.resolve("lib/ledger-1.20261101.0.jar"), "tampered");
        Installer.Installation repaired = new Installer().install(List.of(repository), dir, List.of(APP));
        assertTrue(repaired.outcome().contains("ledger-1.20261101.0.jar had been modified"), repaired.outcome());
        assertEquals("ledger bytes", Files.readString(dir.resolve("lib/ledger-1.20261101.0.jar")));
    }

    @Test
    void aDifferentPlanLeavesNoStaleJarsBehind() throws IOException {
        publish(APP, "ledger bytes");
        publish(FX, "javafx bytes");
        Path dir = apps.resolve("Ledger/1.20261101.0");
        new Installer().install(List.of(repository), dir, List.of(APP, FX));

        Installer.Installation smaller = new Installer().install(List.of(repository), dir, List.of(APP));
        assertTrue(smaller.outcome().contains("the plan changed"), smaller.outcome());
        try (var files = Files.list(dir.resolve("lib"))) {
            assertEquals(List.of(dir.resolve("lib/ledger-1.20261101.0.jar")), files.toList());
        }
    }

    @Test
    void eachJarComesFromTheFirstRepositoryThatHasIt(@TempDir Path estate) throws IOException {
        publish(FX, "javafx bytes");
        Path ledger = estate.resolve(APP.repositoryPath());
        Files.createDirectories(ledger.getParent());
        Files.writeString(ledger, "ledger from the estate repository");
        publish(APP, "ledger from the other repository");
        Path dir = apps.resolve("Ledger/1.20261101.0");
        new Installer().install(List.of(estate, repository), dir, List.of(APP, FX));
        assertEquals("ledger from the estate repository", Files.readString(dir.resolve("lib/ledger-1.20261101.0.jar")));
        assertEquals("javafx bytes", Files.readString(dir.resolve("lib/javafx-graphics-25-win.jar")));
    }

    @Test
    void aReleaseWhoseBytesChangedInTheRepositoryIsRefused() throws IOException {
        publish(APP, "ledger bytes");
        Path dir = apps.resolve("Ledger/1.20261101.0");
        new Installer().install(List.of(repository), dir, List.of(APP));

        publish(APP, "different bytes, same version");
        Installer.ReleaseChanged e = assertThrows(Installer.ReleaseChanged.class,
                () -> new Installer().install(List.of(repository), dir, List.of(APP)));
        assertTrue(e.getMessage().contains("com.example.acme:ledger:1.20261101.0"), e.getMessage());
        assertEquals("ledger bytes", Files.readString(dir.resolve("lib/ledger-1.20261101.0.jar")));

        Installer.Installation forced = new Installer().install(List.of(repository), dir, List.of(APP), true);
        assertTrue(forced.outcome().contains("asked to reinstall"), forced.outcome());
        assertEquals("different bytes, same version", Files.readString(dir.resolve("lib/ledger-1.20261101.0.jar")));
    }

    @Test
    void anInstallationStillRunsWhenTheRepositoryNoLongerHasTheJar() throws IOException {
        publish(APP, "ledger bytes");
        Path dir = apps.resolve("Ledger/1.20261101.0");
        new Installer().install(List.of(repository), dir, List.of(APP));
        Files.delete(repository.resolve(APP.repositoryPath()));
        assertTrue(new Installer().install(List.of(repository), dir, List.of(APP)).outcome().startsWith("reused"));
    }

    @Test
    void missingJarsAreAllReportedAndNothingIsInstalled() throws IOException {
        publish(APP, "ledger bytes");
        Path dir = apps.resolve("Ledger/1.20261101.0");
        Installer.MissingJars e = assertThrows(Installer.MissingJars.class,
                () -> new Installer().install(List.of(repository), dir, List.of(APP, FX)));
        assertTrue(e.getMessage().contains("org.openjfx:javafx-graphics:25:win"), e.getMessage());
        assertTrue(Files.notExists(dir.resolve("installed.txt")));
    }
}
