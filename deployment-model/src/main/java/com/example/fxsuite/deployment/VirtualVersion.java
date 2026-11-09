package com.example.fxsuite.deployment;

/**
 * A later release that reuses the binary of an earlier release of the same unit: same
 * code, newer dependencies. It is an ordinary release in its line, so it is used wherever
 * any other version can be.
 *
 * <p>The base may itself be virtual, so a virtual version builds on whichever release it
 * follows. The binary is that of the physical release at the end of the chain.
 *
 * <p>A virtual version declares only its {@link #changes()}: new versions for some of the
 * units its base depends on. Its dependencies are its base's with those changes applied,
 * so it can neither add a dependency nor drop one.
 *
 * @param <U> the unit both releases belong to
 * @param <V> the release this one is based on
 */
public non-sealed interface VirtualVersion<U extends DeploymentUnit, V extends Version<U>>
        extends Version<U> {

    V base();

    /** The new versions, each for a unit the base already depends on. */
    Dependencies changes();

    @Override
    default Dependencies dependencies() {
        return base().dependencies().changedBy(changes());
    }
}
