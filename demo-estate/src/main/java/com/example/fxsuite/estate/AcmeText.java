package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Text utilities on AcmeCore and Guava. One side of the diamond. */
public record AcmeText() implements Library {

    public record V3_20261005_0() implements PhysicalVersion<AcmeText> {
        @Override public Monotonic monotonic() { return new Monotonic(3); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 5)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeCore.V8_20261001_0(),
                new AcmeGuava.V1_20261001_0()); }
    }

    /** Adds API. */
    public record V3_20261020_0() implements PhysicalVersion<AcmeText> {
        @Override public Monotonic monotonic() { return new Monotonic(3); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 20)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeCore.V8_20261001_0(),
                new AcmeGuava.V1_20261001_0()); }
    }

    /** Carries the AcmeCore line-8 security floor, without a rebuild. */
    public record V3_20261020_1() implements VirtualVersion<AcmeText, V3_20261020_0> {
        @Override public Monotonic monotonic() { return new Monotonic(3); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 20)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V3_20261020_0 base() { return new V3_20261020_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeCore.V8_20261106_1()); }
    }

    /** Moving to AcmeCore line 9 is a change of dependency line, so AcmeText needs a new line too. */
    public record V4_20261113_0() implements PhysicalVersion<AcmeText> {
        @Override public Monotonic monotonic() { return new Monotonic(4); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 13)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeCore.V9_20261110_0(),
                new AcmeGuava.V1_20261103_0()); }
    }

    /** Carries the AcmeCore line-9 security floor. */
    public record V4_20261113_1() implements VirtualVersion<AcmeText, V4_20261113_0> {
        @Override public Monotonic monotonic() { return new Monotonic(4); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 13)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V4_20261113_0 base() { return new V4_20261113_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeCore.V9_20261120_0()); }
    }
}
