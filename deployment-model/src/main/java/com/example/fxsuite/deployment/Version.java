package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * One release of a deployment unit, numbered {@code MONOTONIC.YYYYMMDD.PATCH} for every
 * kind of unit.
 *
 * <p>Each valid release is its own type: a record with no components, nested inside its
 * unit, so a release that does not exist cannot be named. A release is either physical
 * (its own build) or virtual (a later number for the binary of a physical release).
 *
 * <p>What the monotonic part promises depends on the kind of unit. For a library or a
 * bundle it is the compatibility line consumers rely on. For an application, which
 * nothing depends on, it marks a major change in behaviour or design for its users.
 */
public sealed interface Version<U extends DeploymentUnit> extends Scalar, Stateless
        permits PhysicalVersion, VirtualVersion {

    Monotonic monotonic();

    ReleaseDate date();

    Patch patch();
}
