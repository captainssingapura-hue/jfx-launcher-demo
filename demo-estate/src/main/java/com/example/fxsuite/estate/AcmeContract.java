package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;

import java.time.LocalDate;

/** Message types shared by applications that talk to each other. They must be on the same line. */
public record AcmeContract() implements Library {

    /** WIRE_VERSION = 1. */
    public record V1_20261012_0() implements PhysicalVersion<AcmeContract> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 12)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** Adds a message type. */
    public record V1_20261030_0() implements PhysicalVersion<AcmeContract> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 30)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** Changes WIRE_VERSION. Consumers copy the constant into their own code, so this is breaking. */
    public record V2_20261115_0() implements PhysicalVersion<AcmeContract> {
        @Override public Monotonic monotonic() { return new Monotonic(2); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 15)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }
}
