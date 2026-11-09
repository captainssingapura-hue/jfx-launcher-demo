package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Members;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;

import java.time.LocalDate;

/** jOntology: the marker types and rules the estate uses for quality control. */
public record AcmeOntology() implements ExternalBundle {

    /** jOntology core 0.1.0. */
    public record V1_20260901_0() implements PhysicalVersion<AcmeOntology>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 9, 1)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(); }
        @Override public Members members() { return new Members(new ExternalArtifact("io.github.captainssingapura-hue.lang.ontology", "core", "0.1.0")); }
    }
}
