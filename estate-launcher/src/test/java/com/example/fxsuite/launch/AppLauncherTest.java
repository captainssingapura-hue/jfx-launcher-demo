package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.estate.AcmeCore;
import com.example.fxsuite.estate.AcmeNet;
import com.example.fxsuite.estate.AcmeText;
import com.example.fxsuite.estate.Ledger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppLauncherTest {

    /** Would need AcmeCore on lines 8 and 9 at once. */
    record Torn() implements Application {
        record V1_20261125_0() implements PhysicalVersion<Torn> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 25)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() {
                return new Dependencies(new AcmeNet.V6_20261112_1(), new AcmeText.V3_20261020_1());
            }
        }
    }

    @TempDir
    Path apps;

    private AppLauncher launcher() {
        return new AppLauncher(new AppLauncher.Settings(List.of(apps.resolve("repo")), apps));
    }

    @Test
    void preparingWorksOutWhatWouldRunWithoutTouchingTheDisk() {
        AppLauncher.Prepared prepared = launcher().prepare(new Ledger.V1_20261101_0(), false);
        assertTrue(prepared.canLaunch());
        assertEquals(1, prepared.warnings().size(), prepared.warnings().toString());
        assertTrue(prepared.releases().contains(new AcmeCore.V8_20261015_0()));
        assertEquals(20, prepared.jars().size());
        assertEquals(apps.resolve("Ledger").resolve("1.20261101.0"), prepared.dir());
        assertEquals("Ledger 1.20261101.0", prepared.label());
        assertFalse(prepared.isInstalled());
    }

    @Test
    void aNearestWinsClasspathIsKeptApartAndSaysSo() {
        AppLauncher.Prepared prepared = launcher().prepare(new Ledger.V1_20261101_0(), true);
        assertTrue(prepared.releases().contains(new AcmeCore.V8_20261001_0()));
        assertTrue(prepared.warnings().isEmpty());
        assertEquals(apps.resolve("Ledger").resolve("1.20261101.0-nearest-wins"), prepared.dir());
        assertEquals("Ledger 1.20261101.0 [nearest-wins classpath]", prepared.label());
    }

    @Test
    void aReleaseThatDoesNotResolveCannotBeInstalled() {
        AppLauncher launcher = launcher();
        AppLauncher.Prepared prepared = launcher.prepare(new Torn.V1_20261125_0(), false);
        assertFalse(prepared.canLaunch());
        assertTrue(prepared.jars().isEmpty());
        assertThrows(IllegalStateException.class, () -> launcher.install(prepared, false));
    }
}
