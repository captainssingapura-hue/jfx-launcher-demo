package com.example.fxsuite.deployment;

/**
 * A release with its own build: the binary was compiled for this release.
 */
public non-sealed interface PhysicalVersion<U extends DeploymentUnit> extends Version<U> {
}
