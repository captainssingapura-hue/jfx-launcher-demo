package com.example.fxsuite.deployment;

/**
 * A later release whose binary is exactly that of the physical release {@code V}: same
 * code, newer dependencies. It is an ordinary release in its line, so it is used wherever
 * any other version can be.
 *
 * <p>The base is always physical, so the build behind any release is one step away.
 *
 * @param <U> the unit both releases belong to
 * @param <V> the physical release whose binary this one reuses
 */
public non-sealed interface VirtualVersion<U extends DeploymentUnit, V extends PhysicalVersion<U>>
        extends Version<U> {
}
