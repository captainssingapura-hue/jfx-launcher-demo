package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionOrder;
import com.example.fxsuite.deployment.VersionText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The releases an estate declares, found by name, such as {@code Ledger 1.20261101.1}.
 */
public final class Catalogue {

    private final List<Class<? extends DeploymentUnit>> units;

    public Catalogue(final List<Class<? extends DeploymentUnit>> units) {
        this.units = List.copyOf(units);
    }

    public Optional<Version<?>> find(final String unit, final String number) {
        final String named = unit + " " + number;
        final VersionText text = new VersionText();
        return units.stream().flatMap(u -> releasesOf(u).stream()).filter(v -> text.named(v).equals(named)).findFirst();
    }

    public List<Class<? extends DeploymentUnit>> applications() {
        return units.stream().filter(Application.class::isAssignableFrom).toList();
    }

    /** A unit's releases, oldest first. */
    public List<Version<?>> releasesOf(final Class<?> unit) {
        final List<Version<?>> versions = new ArrayList<>();
        for (final Class<?> type : unit.getDeclaredClasses()) {
            if (Version.class.isAssignableFrom(type)) {
                try {
                    final var constructor = type.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    versions.add((Version<?>) constructor.newInstance());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("cannot create " + type.getName(), e);
                }
            }
        }
        versions.sort(this::compare);
        return versions;
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }
}
