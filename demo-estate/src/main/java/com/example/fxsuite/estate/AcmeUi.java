package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Shared JavaFX controls. */
public record AcmeUi() implements Library {

    public record V1_20261018_0() implements PhysicalVersion<AcmeUi> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 18)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeText.V3_20261005_0(),
                new AcmeFx.V1_20261002_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /** Needs the newer AcmeText, which raises AcmeText for anything using this release. */
    public record V1_20261021_0() implements PhysicalVersion<AcmeUi> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 21)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeText.V3_20261020_0(),
                new AcmeFx.V1_20261002_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /** Passes the security floor on, so its users do not pull in the unfixed AcmeText. */
    public record V1_20261021_1() implements VirtualVersion<AcmeUi, V1_20261021_0> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 21)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V1_20261021_0 base() { return new V1_20261021_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeText.V3_20261020_1()); }
    }
}
