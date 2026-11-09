package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;

import java.time.LocalDate;

/**
 * Talks to Ledger through AcmeContract line 1. Uses AcmeCodegen, so it needs a rebuild for
 * dependency updates.
 */
public record Courier() implements Application {

    public record V1_20261102_0() implements PhysicalVersion<Courier> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 2)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeContract.V1_20261030_0(),
                new AcmeCodegen.V1_20261009_0(),
                new AcmeJson.V2_20261005_0(),
                new AcmeJsonGuava.V1_20261006_0(),
                new AcmeFx.V1_20261002_0()); }
    }

    /** Moves to the newer Jackson and Guava. Rebuilt because of AcmeCodegen, so a new physical release. */
    public record V1_20261107_0() implements PhysicalVersion<Courier> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 7)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeContract.V1_20261030_0(),
                new AcmeCodegen.V1_20261009_0(),
                new AcmeJson.V2_20261105_0(),
                new AcmeJsonGuava.V1_20261105_0(),
                new AcmeFx.V1_20261102_0()); }
    }
}
