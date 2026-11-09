package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** The smallest app: JavaFX only. */
public record Hello() implements Application {

    public record V1_20261003_0() implements PhysicalVersion<Hello> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 3)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeFx.V1_20261002_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /** Dependency-only update: same binary, newer JavaFX. */
    public record V1_20261003_1() implements VirtualVersion<Hello, V1_20261003_0> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 3)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V1_20261003_0 base() { return new V1_20261003_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeFx.V1_20261102_0()); }
    }
}
