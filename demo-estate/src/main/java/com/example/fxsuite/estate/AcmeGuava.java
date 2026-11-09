package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Members;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Guava, with the jars it needs at run time pinned beside it. */
public record AcmeGuava() implements ExternalBundle {

    /** Guava 33.4.8, which GHSA-xxph-c9ww-hj94 (CVE-2026-102554, HIGH) affects: fixed in 33.7.2. */
    public record V1_20261001_0() implements PhysicalVersion<AcmeGuava>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("com.google.guava", "guava", "33.4.8-jre"),
                new ExternalArtifact("com.google.guava", "failureaccess", "1.0.3"),
                new ExternalArtifact("com.google.guava", "listenablefuture", "9999.0-empty-to-avoid-conflict-with-guava"),
                new ExternalArtifact("com.google.errorprone", "error_prone_annotations", "2.36.0"),
                new ExternalArtifact("com.google.j2objc", "j2objc-annotations", "3.0.0"),
                new ExternalArtifact("org.jspecify", "jspecify", "1.0.0")); }
    }

    /** Guava 33.5.0: still affected by GHSA-xxph-c9ww-hj94. */
    public record V1_20261103_0() implements VirtualVersion<AcmeGuava, V1_20261001_0>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 3)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20261001_0 base() { return new V1_20261001_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("com.google.guava", "guava", "33.5.0-jre"),
                new ExternalArtifact("com.google.guava", "failureaccess", "1.0.3"),
                new ExternalArtifact("com.google.guava", "listenablefuture", "9999.0-empty-to-avoid-conflict-with-guava"),
                new ExternalArtifact("com.google.errorprone", "error_prone_annotations", "2.41.0"),
                new ExternalArtifact("com.google.j2objc", "j2objc-annotations", "3.1"),
                new ExternalArtifact("org.jspecify", "jspecify", "1.0.0")); }
    }

    /** Guava 33.7.2: the fix for GHSA-xxph-c9ww-hj94, as a virtual release. */
    public record V1_20261124_0() implements VirtualVersion<AcmeGuava, V1_20261001_0>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 24)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20261001_0 base() { return new V1_20261001_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("com.google.guava", "guava", "33.7.2-jre"),
                new ExternalArtifact("com.google.guava", "failureaccess", "1.0.3"),
                new ExternalArtifact("com.google.guava", "listenablefuture", "9999.0-empty-to-avoid-conflict-with-guava"),
                new ExternalArtifact("com.google.errorprone", "error_prone_annotations", "2.50.0"),
                new ExternalArtifact("com.google.j2objc", "j2objc-annotations", "3.1"),
                new ExternalArtifact("org.jspecify", "jspecify", "1.0.1")); }
    }
}
