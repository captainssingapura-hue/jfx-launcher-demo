package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Charts on AcmeUi: a second step in the graph, so resolution has to iterate. */
public record AcmeCharts() implements Library {

    public record V2_20261022_0() implements PhysicalVersion<AcmeCharts> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 22)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeUi.V1_20261021_0(),
                new AcmeOntology.V1_20260901_0()); }
    }

    /** Passes the security floor on. */
    public record V2_20261022_1() implements VirtualVersion<AcmeCharts, V2_20261022_0> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 22)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V2_20261022_0 base() { return new V2_20261022_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeUi.V1_20261021_1()); }
    }

    /** Passes the Guava fix on. */
    public record V2_20261022_2() implements VirtualVersion<AcmeCharts, V2_20261022_1> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 22)); }
        @Override public Patch patch() { return new Patch(2); }
        @Override public V2_20261022_1 base() { return new V2_20261022_1(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeUi.V1_20261021_2()); }
    }
}
