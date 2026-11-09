package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Networking on AcmeCore, whose types it exposes. The other side of the diamond. */
public record AcmeNet() implements Library {

    /** Needs the API AcmeCore added on 2026-10-15. */
    public record V5_20261016_0() implements PhysicalVersion<AcmeNet> {
        @Override public Monotonic monotonic() { return new Monotonic(5); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 16)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeCore.V8_20261015_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /** Carries the AcmeCore line-8 security floor. */
    public record V5_20261016_1() implements VirtualVersion<AcmeNet, V5_20261016_0> {
        @Override public Monotonic monotonic() { return new Monotonic(5); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 16)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V5_20261016_0 base() { return new V5_20261016_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeCore.V8_20261106_1()); }
    }

    /** On AcmeCore line 9, so a new line. */
    public record V6_20261112_0() implements PhysicalVersion<AcmeNet> {
        @Override public Monotonic monotonic() { return new Monotonic(6); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 12)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeCore.V9_20261110_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /** Carries the AcmeCore line-9 security floor. */
    public record V6_20261112_1() implements VirtualVersion<AcmeNet, V6_20261112_0> {
        @Override public Monotonic monotonic() { return new Monotonic(6); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 12)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V6_20261112_0 base() { return new V6_20261112_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeCore.V9_20261120_0()); }
    }
}
