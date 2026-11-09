package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;

import java.time.LocalDate;

/** The base library. Line 8 is still in use after line 9 ships. */
public record AcmeCore() implements Library {

    public record V8_20261001_0() implements PhysicalVersion<AcmeCore> {
        @Override public Monotonic monotonic() { return new Monotonic(8); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** Adds API that AcmeNet needs. */
    public record V8_20261015_0() implements PhysicalVersion<AcmeCore> {
        @Override public Monotonic monotonic() { return new Monotonic(8); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 15)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    public record V8_20261106_0() implements PhysicalVersion<AcmeCore> {
        @Override public Monotonic monotonic() { return new Monotonic(8); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 6)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** Security fix backported to the latest release of the older line. A real rebuild, so physical. */
    public record V8_20261106_1() implements PhysicalVersion<AcmeCore> {
        @Override public Monotonic monotonic() { return new Monotonic(8); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 6)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** Removes deprecated API and targets Java 25: a new line. */
    public record V9_20261110_0() implements PhysicalVersion<AcmeCore> {
        @Override public Monotonic monotonic() { return new Monotonic(9); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 10)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }

    /** The security fix on the current line. */
    public record V9_20261120_0() implements PhysicalVersion<AcmeCore> {
        @Override public Monotonic monotonic() { return new Monotonic(9); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 20)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
    }
}
