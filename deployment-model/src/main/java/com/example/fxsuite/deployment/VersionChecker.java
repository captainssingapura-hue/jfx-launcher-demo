package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Checks the rules about a unit's versions that the type system cannot. The versions are
 * the {@link Version} records declared inside the unit.
 *
 * <p>Errors ({@link #check}):
 * <ul>
 *   <li>Each version is a record with no components that directly implements
 *       {@link PhysicalVersion} or {@link VirtualVersion} for the unit it is declared in.</li>
 *   <li>Its class name spells its number: {@code V<monotonic>_<yyyyMMdd>_<patch>}.</li>
 *   <li>No two versions share a number, and no date is used by two lines.</li>
 *   <li>No version depends on its own unit, and a bundle depends only on bundles.</li>
 *   <li>A virtual version's base is declared in the same unit, is on the same line, comes
 *       before it, and the chain of bases ends at a physical release. If the virtual
 *       version includes a checksum, that physical release includes the same one.</li>
 *   <li>Each change a virtual version makes replaces a dependency its base has with a
 *       later version on the same line, and its dependencies are exactly its base's with
 *       those changes applied.</li>
 *   <li>Each version's whole dependency graph resolves: no unit is needed on two lines.</li>
 * </ul>
 *
 * <p>Warnings ({@link #warnings}): each version whose graph requests some unit at more than
 * one version, where resolution had to pick the higher.
 */
public final class VersionChecker implements StatelessFunctionalObject {

    /** A version found inside the unit, with its dependencies. */
    private record Found(Class<?> type, Version<?> version, Dependencies dependencies) implements Scalar {
    }

    public List<Violation> check(final Class<? extends DeploymentUnit> unit) {
        final List<Violation> violations = new ArrayList<>();
        final Map<Class<?>, Found> found = versions(unit, violations);

        checkNumbersAreUnique(unit, found.values(), violations);
        checkEachDateHasOneLine(unit, found.values(), violations);
        for (final Found f : found.values()) {
            checkDependencies(unit, f, violations);
            if (f.version() instanceof VirtualVersion<?, ?> virtual) {
                checkVirtual(unit, f, virtual, found.get(virtual.base().getClass()), violations);
            }
            violations.addAll(new Resolver().resolve(f.version()).conflicts().items());
        }
        return List.copyOf(violations);
    }

    /** The resolution warnings for every version of the unit that could be inspected. */
    public List<Warning> warnings(final Class<? extends DeploymentUnit> unit) {
        final List<Warning> warnings = new ArrayList<>();
        for (final Found f : versions(unit, new ArrayList<>()).values()) {
            warnings.addAll(new Resolver().resolve(f.version()).warnings().items());
        }
        return List.copyOf(warnings);
    }

    private Map<Class<?>, Found> versions(final Class<?> unit, final List<Violation> violations) {
        final Map<Class<?>, Found> found = new HashMap<>();
        for (final Class<?> type : unit.getDeclaredClasses()) {
            if (Version.class.isAssignableFrom(type)) {
                inspect(unit, type, violations).ifPresent(f -> found.put(type, f));
            }
        }
        return found;
    }

    private Optional<Found> inspect(final Class<?> unit, final Class<?> type, final List<Violation> violations) {
        final String name = type.getName();
        if (!type.isRecord() || type.getRecordComponents().length != 0) {
            violations.add(new Violation(name + ": a version must be a record with no components"));
            return Optional.empty();
        }

        final Optional<Class<?>> declaredUnit = new VersionUnit().of(type);
        if (declaredUnit.isEmpty()) {
            violations.add(new Violation(name + ": must directly implement PhysicalVersion or VirtualVersion"));
            return Optional.empty();
        }
        if (declaredUnit.get() != unit) {
            violations.add(new Violation(name + ": is a version of " + declaredUnit.get().getName()
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

        final VersionText text = new VersionText();
        if (!type.getSimpleName().equals(text.className(version))) {
            violations.add(new Violation(name + ": is numbered " + text.number(version)
                    + " so it must be named " + text.className(version)));
        }

        // Checked before anything walks the chain, which would otherwise never end.
        try {
            new Lineage().physicalOf(version);
        } catch (IllegalArgumentException e) {
            violations.add(new Violation(name + ": " + e.getMessage()));
            return Optional.empty();
        }

        final Dependencies dependencies;
        try {
            dependencies = version.dependencies();
        } catch (RuntimeException e) {
            violations.add(new Violation(name + ": its dependencies could not be worked out: " + e.getMessage()));
            return Optional.empty();
        }
        return Optional.of(new Found(type, version, dependencies));
    }

    private void checkNumbersAreUnique(final Class<?> unit, final Iterable<Found> versions,
                                       final List<Violation> violations) {
        final VersionText text = new VersionText();
        final Map<String, Class<?>> byNumber = new HashMap<>();
        for (final Found f : versions) {
            final Class<?> earlier = byNumber.putIfAbsent(text.number(f.version()), f.type());
            if (earlier != null) {
                violations.add(new Violation(unit.getName() + ": " + earlier.getSimpleName() + " and "
                        + f.type().getSimpleName() + " are both numbered " + text.number(f.version())));
            }
        }
    }

    private void checkEachDateHasOneLine(final Class<?> unit, final Iterable<Found> versions,
                                         final List<Violation> violations) {
        final Map<ReleaseDate, Monotonic> lineOfDate = new HashMap<>();
        final Set<ReleaseDate> reported = new HashSet<>();
        for (final Found f : versions) {
            final Monotonic line = f.version().monotonic();
            final Monotonic other = lineOfDate.putIfAbsent(f.version().date(), line);
            if (other != null && !other.equals(line) && reported.add(f.version().date())) {
                final int low = Math.min(other.value(), line.value());
                final int high = Math.max(other.value(), line.value());
                violations.add(new Violation(unit.getName() + ": " + new VersionText().date(f.version().date())
                        + " is used by lines " + low + " and " + high + "; each date belongs to one line"));
            }
        }
    }

    private void checkDependencies(final Class<?> unit, final Found f, final List<Violation> violations) {
        final String name = f.type().getName();
        if (f.dependencies().units().contains(unit)) {
            violations.add(new Violation(name + ": depends on its own unit"));
        }
        if (ExternalBundle.class.isAssignableFrom(unit)) {
            for (final Class<?> dependency : f.dependencies().units()) {
                if (!ExternalBundle.class.isAssignableFrom(dependency)) {
                    violations.add(new Violation(name + ": is a bundle, so it may depend only on bundles, not "
                            + dependency.getName()));
                }
            }
        }
    }

    private void checkVirtual(final Class<?> unit, final Found virtual, final VirtualVersion<?, ?> version,
                              final Found base, final List<Violation> violations) {
        final String name = virtual.type().getName();
        final VersionText text = new VersionText();
        if (base == null) {
            violations.add(new Violation(name + ": its base must be declared in " + unit.getName()));
            return;
        }
        if (!virtual.version().monotonic().equals(base.version().monotonic())) {
            violations.add(new Violation(name + ": is on line " + virtual.version().monotonic().value()
                    + " but its base " + text.number(base.version()) + " is on line "
                    + base.version().monotonic().value()));
        }
        if (compare(virtual.version(), base.version()) <= 0) {
            violations.add(new Violation(name + ": " + text.number(virtual.version())
                    + " must come after its base " + text.number(base.version())));
        }
        if (virtual.version() instanceof IncludeChecksum own) {
            final PhysicalVersion<?> physical = new Lineage().physicalOf(virtual.version());
            if (!(physical instanceof IncludeChecksum built)) {
                violations.add(new Violation(name + ": includes a checksum but the physical release "
                        + text.number(physical) + " it runs does not"));
            } else if (!own.checksum().equals(built.checksum())) {
                violations.add(new Violation(name + ": its checksum differs from the physical release "
                        + text.number(physical) + " it runs, but a virtual version reuses that binary"));
            }
        }

        final Dependencies changes = version.changes();
        boolean changesApply = true;
        for (final Version<? extends Dependable> change : changes.versions()) {
            final Class<?> changed = new VersionUnit().of(change.getClass()).orElseThrow();
            final Optional<Version<? extends Dependable>> before = base.dependencies().on(changed);
            if (before.isEmpty()) {
                changesApply = false;
                violations.add(new Violation(name + ": changes " + changed.getName()
                        + ", which its base does not depend on; a virtual version cannot add a dependency"));
            } else if (!before.get().monotonic().equals(change.monotonic())) {
                violations.add(new Violation(name + ": moves " + changed.getName() + " from line "
                        + before.get().monotonic().value() + " to line " + change.monotonic().value()
                        + "; a virtual version stays on each dependency's line"));
            } else if (compare(change, before.get()) <= 0) {
                violations.add(new Violation(name + ": changes " + changed.getName() + " to " + text.number(change)
                        + ", which is not later than its base's " + text.number(before.get())));
            }
        }
        if (changesApply && !virtual.dependencies().equals(base.dependencies().changedBy(changes))) {
            violations.add(new Violation(name + ": its dependencies are not its base's with its changes applied"));
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(final Version<?> a, final Version<?> b) {
        return new VersionOrder().compare((Version<DeploymentUnit>) a, (Version<DeploymentUnit>) b);
    }
}
