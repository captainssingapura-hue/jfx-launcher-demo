package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;

import java.time.LocalDate;

/**
 * An annotation processor and its runtime. Its users bake generated code into their binary,
 * so a dependency-only update is not safe for them; they need a rebuild.
 */
public record AcmeCodegen() implements Library {

    public record V1_20261009_0() implements PhysicalVersion<AcmeCodegen> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 9)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(new AcmeOntology.V1_20260901_0()); }
    }
}
