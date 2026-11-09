package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Stays on AcmeCore line 8 and Jackson 2. Talks to Courier through AcmeContract. */
public record Ledger() implements Application {

    /**
     * The diamond: AcmeText asks for AcmeCore 8.20261001.0 and AcmeNet for 8.20261015.0.
     * Resolution uses the higher and warns.
     */
    public record V1_20261101_0() implements PhysicalVersion<Ledger> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 1)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeText.V3_20261020_0(),
                new AcmeNet.V5_20261016_0(),
                new AcmeCharts.V2_20261022_0(),
                new AcmeContract.V1_20261030_0(),
                new AcmeJson.V1_20260915_0(),
                new AcmeFx.V1_20261002_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /**
     * The AcmeCore security floor, reached through virtual versions of the libraries in between,
     * and the fixed Jackson 2. Every path now asks for the same AcmeCore, so the diamond warning is gone.
     * Still runs the affected Guava, through AcmeText.
     */
    public record V1_20261101_1() implements VirtualVersion<Ledger, V1_20261101_0> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 1)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V1_20261101_0 base() { return new V1_20261101_0(); }
        @Override public Dependencies changes() { return new Dependencies(
                new AcmeText.V3_20261020_1(),
                new AcmeNet.V5_20261016_1(),
                new AcmeCharts.V2_20261022_1(),
                new AcmeJson.V1_20261020_0()); }
    }

    /** The fixed Guava, through virtual releases of AcmeText, AcmeUi and AcmeCharts. Same binary. */
    public record V1_20261101_2() implements VirtualVersion<Ledger, V1_20261101_1> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 1)); }
        @Override public Patch patch() { return new Patch(2); }
        @Override public V1_20261101_1 base() { return new V1_20261101_1(); }
        @Override public Dependencies changes() { return new Dependencies(
                new AcmeText.V3_20261020_2(),
                new AcmeCharts.V2_20261022_2()); }
    }
}
