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

/**
 * Jackson. Line 1 is Jackson 2; line 2 is Jackson 3, which renamed its packages to
 * tools.jackson: a real break, so a new line. Both share jackson-annotations.
 */
public record AcmeJson() implements ExternalBundle {

    /**
     * Jackson 2.22.2, which four HIGH advisories affect (CVE-2026-89407, CVE-2026-89425,
     * CVE-2026-91776, CVE-2026-91777), all fixed in 2.22.3.
     */
    public record V1_20260915_0() implements PhysicalVersion<AcmeJson>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 9, 15)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-core", "2.22.2"),
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-databind", "2.22.2"),
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.22")); }
    }

    /** Jackson 2.22.4: the fix, as a virtual release. Nothing that uses it is rebuilt. */
    public record V1_20261020_0() implements VirtualVersion<AcmeJson, V1_20260915_0>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 20)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20260915_0 base() { return new V1_20260915_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-core", "2.22.4"),
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-databind", "2.22.4"),
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.22")); }
    }

    /** Jackson 3.2.3. */
    public record V2_20261005_0() implements PhysicalVersion<AcmeJson>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 5)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("tools.jackson.core", "jackson-core", "3.2.3"),
                new ExternalArtifact("tools.jackson.core", "jackson-databind", "3.2.3"),
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.22")); }
    }

    /** Jackson 3.2.4. */
    public record V2_20261105_0() implements VirtualVersion<AcmeJson, V2_20261005_0>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 5)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V2_20261005_0 base() { return new V2_20261005_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
        @Override public Members members() { return new Members(
                new ExternalArtifact("tools.jackson.core", "jackson-core", "3.2.4"),
                new ExternalArtifact("tools.jackson.core", "jackson-databind", "3.2.4"),
                new ExternalArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.22")); }
    }
}
