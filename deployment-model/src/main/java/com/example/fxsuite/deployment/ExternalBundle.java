package com.example.fxsuite.deployment;

/**
 * An internal bundle of external libraries, grouped by purpose. It is the only kind of
 * unit that brings external code into the estate, and it aggregates its members rather
 * than copying them in.
 */
public non-sealed interface ExternalBundle extends DeploymentUnit {
}
