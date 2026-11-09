package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/** Runs on AcmeCore line 9 and Jackson 3, beside Ledger on the same desktop. */
public record Atlas() implements Application {

    public record V1_20261112_0() implements PhysicalVersion<Atlas> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 12)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeNet.V6_20261112_0(),
                new AcmeJson.V2_20261105_0(),
                new AcmeJsonGuava.V1_20261105_0(),
                new AcmeGuava.V1_20261103_0(),
                new AcmeFx.V1_20261102_0()); }
    }

    /** Starts using AcmeText. Code changed, so physical. */
    public record V1_20261121_0() implements PhysicalVersion<Atlas> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 21)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeNet.V6_20261112_0(),
                new AcmeText.V4_20261113_0(),
                new AcmeJson.V2_20261105_0(),
                new AcmeJsonGuava.V1_20261105_0(),
                new AcmeGuava.V1_20261103_0(),
                new AcmeFx.V1_20261102_0()); }
    }

    /** The AcmeCore line-9 security floor. */
    public record V1_20261121_1() implements VirtualVersion<Atlas, V1_20261121_0> {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 21)); }
        @Override public Patch patch() { return new Patch(1); }
        @Override public V1_20261121_0 base() { return new V1_20261121_0(); }
        @Override public Dependencies changes() { return new Dependencies(
                new AcmeNet.V6_20261112_1(),
                new AcmeText.V4_20261113_1()); }
    }
}
