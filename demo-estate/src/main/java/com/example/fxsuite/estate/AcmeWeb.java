package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** tinyhttp. It needs JSON, so it depends on AcmeJson instead of declaring jsonic itself. */
public record AcmeWeb() implements ExternalBundle {

    /** tinyhttp 4.2. */
    public record V1_20261008_0() implements PhysicalVersion<AcmeWeb> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 8)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(new AcmeJson.V2_20261005_0()); }
    }

    /** Picks up the jsonic fix; tinyhttp itself unchanged. */
    public record V1_20261106_0() implements VirtualVersion<AcmeWeb, V1_20261008_0> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 6)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20261008_0 base() { return new V1_20261008_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeJson.V2_20261105_1()); }
    }

    /**
     * tinyhttp 5.0: the only release fixing its own advisory, and it breaks compatibility,
     * so applications migrate to line 2 one at a time.
     */
    public record V2_20261120_0() implements PhysicalVersion<AcmeWeb> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 20)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(new AcmeJson.V2_20261105_1()); }
    }
}
