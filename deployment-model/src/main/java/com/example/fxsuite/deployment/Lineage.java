package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Follows a version back through its virtual bases to the physical release whose binary
 * it runs.
 */
public final class Lineage implements StatelessFunctionalObject {

    /**
     * @throws IllegalArgumentException if the chain of bases loops back on itself
     */
    public PhysicalVersion<?> physicalOf(final Version<?> version) {
        final Set<Class<?>> seen = new HashSet<>();
        Version<?> current = version;
        while (current instanceof VirtualVersion<?, ?> virtual) {
            if (!seen.add(current.getClass())) {
                throw new IllegalArgumentException("the bases of " + version.getClass().getName()
                        + " loop back to " + current.getClass().getName());
            }
            current = virtual.base();
        }
        return (PhysicalVersion<?>) current;
    }
}
