package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks the rules about a unit's versions that the type system cannot. The versions are
 * the {@link Version} records declared inside the unit.
 *
 * <ul>
 *   <li>Each version is a record with no components that directly implements
 *       {@link PhysicalVersion} or {@link VirtualVersion} for the unit it is declared in.</li>
 *   <li>Its class name spells its number: {@code V<monotonic>_<yyyyMMdd>_<patch>}.</li>
 *   <li>No two versions share a number, and no date is used by two lines.</li>
 *   <li>A virtual version's base is declared in the same unit, is on the same line, and
 *       comes before it; if the virtual version includes a checksum, its base includes
 *       the same one.</li>
 * </ul>
 */
public final class VersionChecker implements StatelessFunctionalObject {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    /** A version found inside the unit, with the base it declares if it is virtual. */
    private record Found(Class<?> type, Version<?> version, Optional<Class<?>> base) implements Scalar {
    }

    public List<Violation> check(final Class<? extends DeploymentUnit> unit) {
        final List<Violation> violations = new ArrayList<>();
        final Map<Class<?>, Found> found = new HashMap<>();

        for (final Class<?> type : unit.getDeclaredClasses()) {
            if (Version.class.isAssignableFrom(type)) {
                inspect(unit, type, violations).ifPresent(f -> found.put(type, f));
            }
        }

        checkNumbersAreUnique(unit, found.values(), violations);
        checkEachDateHasOneLine(unit, found.values(), violations);
        for (final Found f : found.values()) {
            f.base().ifPresent(base -> checkVirtual(unit, f, found.get(base), violations));
        }
        return List.copyOf(violations);
    }

    private Optional<Found> inspect(final Class<?> unit, final Class<?> type, final List<Violation> violations) {
        final String name = type.getName();
        if (!type.isRecord() || type.getRecordComponents().length != 0) {
            violations.add(new Violation(name + ": a version must be a record with no components"));
            return Optional.empty();
        }

        final Optional<Type[]> args = versionTypeArguments(type);
        if (args.isEmpty()) {
            violations.add(new Violation(name + ": must directly implement PhysicalVersion or VirtualVersion"));
            return Optional.empty();
        }
        if (args.get()[0] != unit) {
            violations.add(new Violation(name + ": is a version of " + args.get()[0].getTypeName()
                    + " but is declared in " + unit.getName()));
            return Optional.empty();
        }

        final Version<?> version;
        try {
            final var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            version = (Version<?>) constructor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            violations.add(new Violation(name + ": could not be created: " + e));
            return Optional.empty();
        }

        final String expected = "V" + version.monotonic().value() + "_"
                + version.date().value().format(YYYYMMDD) + "_" + version.patch().value();
        if (!type.getSimpleName().equals(expected)) {
            violations.add(new Violation(name + ": is numbered " + text(version)
                    + " so it must be named " + expected));
        }

        final Optional<Class<?>> base = args.get().length == 2 && args.get()[1] instanceof Class<?> c
                ? Optional.of(c) : Optional.empty();
        return Optional.of(new Found(type, version, base));
    }

    /** The type arguments of the PhysicalVersion or VirtualVersion the type implements. */
    private Optional<Type[]> versionTypeArguments(final Class<?> type) {
        for (final Type implemented : type.getGenericInterfaces()) {
            if (implemented instanceof ParameterizedType p
                    && (p.getRawType() == PhysicalVersion.class || p.getRawType() == VirtualVersion.class)) {
                return Optional.of(p.getActualTypeArguments());
            }
        }
        return Optional.empty();
    }

    private void checkNumbersAreUnique(final Class<?> unit, final Iterable<Found> versions,
                                       final List<Violation> violations) {
        final Map<String, Class<?>> byNumber = new HashMap<>();
        for (final Found f : versions) {
            final Class<?> earlier = byNumber.putIfAbsent(text(f.version()), f.type());
            if (earlier != null) {
                violations.add(new Violation(unit.getName() + ": " + earlier.getSimpleName() + " and "
                        + f.type().getSimpleName() + " are both numbered " + text(f.version())));
            }
        }
    }

    private void checkEachDateHasOneLine(final Class<?> unit, final Iterable<Found> versions,
                                         final List<Violation> violations) {
        final Map<ReleaseDate, Monotonic> lineOfDate = new HashMap<>();
        for (final Found f : versions) {
            final Monotonic line = f.version().monotonic();
            final Monotonic other = lineOfDate.putIfAbsent(f.version().date(), line);
            if (other != null && !other.equals(line)) {
                violations.add(new Violation(unit.getName() + ": " + f.version().date().value().format(YYYYMMDD)
                        + " is used by lines " + other.value() + " and " + line.value()
                        + "; each date belongs to one line"));
            }
        }
    }

    private void checkVirtual(final Class<?> unit, final Found virtual, final Found base,
                              final List<Violation> violations) {
        final String name = virtual.type().getName();
        if (base == null) {
            violations.add(new Violation(name + ": its base must be declared in " + unit.getName()));
            return;
        }
        if (!virtual.version().monotonic().equals(base.version().monotonic())) {
            violations.add(new Violation(name + ": is on line " + virtual.version().monotonic().value()
                    + " but its base " + text(base.version()) + " is on line " + base.version().monotonic().value()));
        }
        if (compare(virtual.version(), base.version()) <= 0) {
            violations.add(new Violation(name + ": " + text(virtual.version())
                    + " must come after its base " + text(base.version())));
        }
        if (virtual.version() instanceof IncludeChecksum own) {
            if (!(base.version() instanceof IncludeChecksum baseSum)) {
                violations.add(new Violation(name + ": includes a checksum but its base "
                        + text(base.version()) + " does not"));
            } else if (!own.checksum().equals(baseSum.checksum())) {
                violations.add(new Violation(name + ": its checksum differs from its base "
                        + text(base.version()) + ", but a virtual version reuses its base's binary"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }

    private String text(final Version<?> v) {
        return v.monotonic().value() + "." + v.date().value().format(YYYYMMDD) + "." + v.patch().value();
    }
}
