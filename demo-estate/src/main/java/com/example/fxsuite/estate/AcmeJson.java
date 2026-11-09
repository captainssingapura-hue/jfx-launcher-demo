package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** jsonic-core and jsonic-databind, which must always move together. */
public record AcmeJson() implements ExternalBundle {

    /** jsonic 2.15.1, derived line 1. */
    public record V1_20260915_0() implements PhysicalVersion<AcmeJson> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 9, 15)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** jsonic 2.16.0: a minor upstream release that removed a method, so a new derived line. */
    public record V2_20261005_0() implements PhysicalVersion<AcmeJson> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 5)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** jsonic 3.0.0: a major upstream release that passed every compatibility check, so it stays on line 2. */
    public record V2_20261105_0() implements VirtualVersion<AcmeJson, V2_20261005_0> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 5)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V2_20261005_0 base() { return new V2_20261005_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
    }

    /** jsonic 3.0.1, the fix for an advisory, updated in place on the release applications pin. */
    public record V2_20261105_1() implements VirtualVersion<AcmeJson, V2_20261105_0> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 5)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V2_20261105_0 base() { return new V2_20261105_0(); }
        @Override public Dependencies changes() { return new Dependencies(); }
    }
}
