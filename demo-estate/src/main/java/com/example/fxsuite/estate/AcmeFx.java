package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** JavaFX (base, graphics, controls), with a native jar per platform. */
public record AcmeFx() implements ExternalBundle {

    /** JavaFX 25.0.0. */
    public record V1_20261002_0() implements PhysicalVersion<AcmeFx> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 2)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /**
     * JavaFX 25.0.1: members moved within their derived line, so no code of the bundle changed.
     * A routine refresh takes a new date but is still virtual.
     */
    public record V1_20261102_0() implements VirtualVersion<AcmeFx, V1_20261002_0> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 2)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20261002_0 base() { return new V1_20261002_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
    }
}
