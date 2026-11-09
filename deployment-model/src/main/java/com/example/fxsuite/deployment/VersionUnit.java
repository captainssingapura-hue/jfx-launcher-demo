package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Reads which unit a version belongs to, from the {@link PhysicalVersion} or
 * {@link VirtualVersion} its record implements. The type argument is kept in the class
 * file, so each version states its unit exactly once, in its declaration.
 */
public final class VersionUnit implements StatelessFunctionalObject {

    /** Empty when the type does not directly implement a version interface for a concrete unit. */
    public Optional<Class<?>> of(final Class<?> versionType) {
        for (final Type implemented : versionType.getGenericInterfaces()) {
            if (implemented instanceof ParameterizedType p
                    && (p.getRawType() == PhysicalVersion.class || p.getRawType() == VirtualVersion.class)
                    && p.getActualTypeArguments()[0] instanceof Class<?> unit) {
                return Optional.of(unit);
            }
        }
        return Optional.empty();
    }
}
